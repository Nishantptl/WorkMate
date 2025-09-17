package com.example.workmate.ViewModels

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.workmate.Model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EmployeeViewModel : ViewModel() {

    // --- Dependencies & Setup ---
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val userId = auth.currentUser?.uid

    // --- LiveData for UI Observation ---
    private val _employeeProfile = MutableLiveData<Employee?>()
    val employeeProfile: LiveData<Employee?> = _employeeProfile

    private val _homeUiState = MutableLiveData<HomeUiState>()
    val homeUiState: LiveData<HomeUiState> = _homeUiState

    private val _attendanceHistory = MutableLiveData<List<AttendanceRecord>>()
    val attendanceHistory: LiveData<List<AttendanceRecord>> = _attendanceHistory

    private val _operationStatus = MutableLiveData<Pair<Boolean, String>>()
    val operationStatus: LiveData<Pair<Boolean, String>> = _operationStatus

    private val _myLeaveRequests = MutableLiveData<List<LeaveRequest>>()
    val myLeaveRequests: LiveData<List<LeaveRequest>> = _myLeaveRequests

    // --- Internal State Management for HomeFragment Logic ---
    private var isClockedIn = false
    private var isBreak = false
    private var checkInTime: Long = 0
    private var breakStartTime: Long = 0
    private var totalBreakTime: Long = 0
    private var workedTimeWhenPaused: Long = 0
    private var currentAttendanceId: String? = null
    private var wasLate: Boolean = false
    private var employeeName: String = "Employee"

    // --- Timer Handler ---
    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable: Runnable

    companion object {
        const val OFFICIAL_START_TIME_HOUR = 10
        const val OFFICIAL_START_TIME_MINUTE = 30
        const val FULL_DAY_DURATION_HOURS = 8
        const val HALF_DAY_DURATION_HOURS = 4
    }

    init {
        _homeUiState.value = HomeUiState() // Initial state
        timerRunnable = object : Runnable {
            override fun run() {
                if (isClockedIn && !isBreak) {
                    val elapsed = (System.currentTimeMillis() - checkInTime) - totalBreakTime
                    updateTimer(elapsed)
                    handler.postDelayed(this, 1000)
                }
            }
        }
        loadEmployeeDetails()
        loadAttendanceHistory()
        fetchMyLeaveRequests()
    }

    fun submitLeaveRequest(request: LeaveRequest) {
        val newLeaveRef = firestore.collection("leave_requests").document()
        request.leaveId = newLeaveRef.id
        newLeaveRef.set(request)
            .addOnSuccessListener { _operationStatus.postValue(Pair(true, "Request submitted successfully!")) }
            .addOnFailureListener { _operationStatus.postValue(Pair(false, "Submission failed: ${it.message}")) }
    }

    fun fetchMyLeaveRequests() {
        if (userId == null) return
        firestore.collection("leave_requests").whereEqualTo("userId", userId)
            .orderBy("requestedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("EmployeeVM", "Error fetching leave requests", error)
                    return@addSnapshotListener
                }
                val requests = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(LeaveRequest::class.java)?.apply { leaveId = doc.id }
                }
                _myLeaveRequests.postValue(requests ?: emptyList())
            }
    }

    fun refreshLeaveRequests() {
        fetchMyLeaveRequests()
    }

    fun handleClockInOut() {
        if (!isClockedIn) clockIn() else clockOut()
    }

    fun handleBreakResume() {
        if (!isBreak) startBreak() else resumeFromBreak()
    }

    fun loadEmployeeDetails() {
        if (userId == null) return
        _homeUiState.value = _homeUiState.value?.copy(isLoading = true)
        firestore.collection("employee").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val profile = Employee(
                        uid = userId,
                        name = doc.getString("name") ?: "Employee",
                        email = auth.currentUser?.email ?: "",
                        organization = doc.getString("organization") ?: "Organization"
                    )
                    _employeeProfile.value = profile
                    employeeName = profile.name
                }
                checkTodayAttendance()
            }
            .addOnFailureListener {
                checkTodayAttendance()
            }
    }

    fun loadAttendanceHistory() {
        if (userId == null) return
        firestore.collection("attendance")
            .whereEqualTo("employeeId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("ViewModel", "Error listening to history", error)
                    return@addSnapshotListener
                }
                val history = snapshots?.toObjects(AttendanceRecord::class.java)
                _attendanceHistory.postValue(history ?: emptyList())
            }
    }

    private fun checkTodayAttendance() {
        if (userId == null) return
        val today = getTodayDateString()
        firestore.collection("attendance")
            .whereEqualTo("employeeId", userId).whereEqualTo("date", today).limit(1)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    resetState()
                } else {
                    val doc = docs.documents[0]
                    currentAttendanceId = doc.id
                    val att = doc.toObject(AttendanceRecord::class.java)!!
                    wasLate = att.status == "Late"
                    when {
                        att.checkOutTime != null -> showCompletedState(att)
                        att.status == "Absent" -> showAbsentState()
                        att.checkInTime != null -> restoreInProgressState(att)
                    }
                }
                _homeUiState.value = _homeUiState.value?.copy(isLoading = false) // Hide loading here
            }.addOnFailureListener {
                resetState()
                _homeUiState.value = _homeUiState.value?.copy(isLoading = false) // Hide loading on failure
            }
    }

    private fun clockIn() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 11); set(Calendar.MINUTE, 0) }
        if (now > cal.timeInMillis) {
            markAbsent(); return
        }

        isClockedIn = true
        checkInTime = now
        handler.post(timerRunnable)
        saveCheckInToFirebase(now)

        _homeUiState.value = _homeUiState.value?.copy(
            clockInButtonText = "Check Out",
            checkInTimeText = formatTime(now),
            isCheckInVisible = true,
            isBreakButtonVisible = true,
            isCheckOutVisible = false,
            isTotalWorkVisible = false,
            timerText = "00:00:00"
        )
    }

    private fun clockOut() {
        if (isBreak) resumeFromBreak()
        isClockedIn = false
        handler.removeCallbacks(timerRunnable)

        val checkOutTime = System.currentTimeMillis()
        val workDuration = (checkOutTime - checkInTime) - totalBreakTime
        saveCheckOutToFirebase(checkOutTime, workDuration)

        _homeUiState.value = _homeUiState.value?.copy(
            clockInButtonText = "Work Complete",
            isClockInButtonEnabled = false,
            checkOutTimeText = formatTime(checkOutTime),
            isCheckOutVisible = true,
            totalWorkTimeText = formatDuration(workDuration),
            isTotalWorkVisible = true,
            isBreakButtonVisible = false,
            isOnBreak = false
        )
        updateTimer(workDuration)
    }

    private fun startBreak() {
        isBreak = true
        breakStartTime = System.currentTimeMillis()
        workedTimeWhenPaused = (breakStartTime - checkInTime) - totalBreakTime
        handler.removeCallbacks(timerRunnable)
        updateBreakStatusInFirebase(true)

        _homeUiState.value = _homeUiState.value?.copy(
            isOnBreak = true,
            breakButtonText = "Click here to continue"
        )
    }

    private fun resumeFromBreak() {
        isBreak = false
        totalBreakTime += System.currentTimeMillis() - breakStartTime
        handler.post(timerRunnable)
        updateBreakStatusInFirebase(false)

        _homeUiState.value = _homeUiState.value?.copy(
            isOnBreak = false,
            breakButtonText = "I'm taking a break"
        )
    }

    // --- State Update Functions ---

    private fun resetState() {
        isClockedIn = false; isBreak = false; checkInTime = 0; totalBreakTime = 0
        currentAttendanceId = null; wasLate = false
        _homeUiState.value = HomeUiState(
            isLoading = false,
            employeeName = employeeName,
            formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        )
    }

    private fun showCompletedState(att: AttendanceRecord) {
        val workDuration = att.totalWorkDuration ?: 0
        _homeUiState.value = HomeUiState(
            isLoading = false, employeeName = employeeName,
            formattedDate = getTodayDateString(true),
            clockInButtonText = "Work Complete", isClockInButtonEnabled = false,
            checkInTimeText = formatTime(att.checkInTime), isCheckInVisible = true,
            checkOutTimeText = formatTime(att.checkOutTime), isCheckOutVisible = true,
            totalWorkTimeText = formatDuration(workDuration), isTotalWorkVisible = true,
            timerText = formatDuration(workDuration)
        )
    }

    private fun showAbsentState() {
        _homeUiState.value = HomeUiState(
            isLoading = false, employeeName = employeeName,
            formattedDate = getTodayDateString(true),
            clockInButtonText = "Absent", isClockInButtonEnabled = false,
            isMarkedAbsent = true
        )
    }

    private fun restoreInProgressState(att: AttendanceRecord) {
        isClockedIn = true
        checkInTime = att.checkInTime!!
        totalBreakTime = att.totalBreakTime
        isBreak = att.isOnBreak
        breakStartTime = att.breakStartTime ?: 0

        _homeUiState.value = HomeUiState(
            isLoading = false, employeeName = employeeName,
            formattedDate = getTodayDateString(true),
            clockInButtonText = "Check Out", isCheckInVisible = true,
            checkInTimeText = formatTime(att.checkInTime), isBreakButtonVisible = true
        )
        if (isBreak) {
            workedTimeWhenPaused = (breakStartTime - checkInTime) - totalBreakTime
            updateTimer(workedTimeWhenPaused)
            _homeUiState.value = _homeUiState.value?.copy(
                isOnBreak = true,
                breakButtonText = "Click here to continue"
            )
        } else {
            handler.post(timerRunnable)
        }
    }

    private fun markAbsent() {
        saveAbsentToFirebase()
        showAbsentState()
    }

    private fun saveCheckInToFirebase(checkIn: Long) {
        if (userId == null) return
        val organizationName = _employeeProfile.value?.organization ?: ""
        val initialStatus = if (checkIn > getOfficialStartTime()) "Late" else "Present"
        wasLate = initialStatus == "Late"
        val record = AttendanceRecord(
            employeeId = userId, employeeName = employeeName, employeeOrg = organizationName,
            date = getTodayDateString(), status = initialStatus, checkInTime = checkIn
        )
        firestore.collection("attendance").add(record)
            .addOnSuccessListener { ref -> currentAttendanceId = ref.id }
            .addOnFailureListener { Log.e("ViewModel", "Failed to save Check-In", it) }
    }

    private fun saveCheckOutToFirebase(checkOut: Long, workMs: Long) {
        if (userId == null || currentAttendanceId == null) return
        val finalStatus = getFinalStatus(workMs)
        val updates = mapOf(
            "checkOutTime" to checkOut, "totalWorkDuration" to workMs,
            "totalBreakTime" to totalBreakTime, "status" to finalStatus,
            "isOnBreak" to false, "lastUpdated" to System.currentTimeMillis()
        )
        firestore.collection("attendance").document(currentAttendanceId!!).update(updates)
            .addOnFailureListener { Log.e("ViewModel", "Failed to save Check-Out", it) }
    }

    private fun updateBreakStatusInFirebase(onBreak: Boolean) {
        if (userId == null || currentAttendanceId == null) return
        val updates = mapOf(
            "isOnBreak" to onBreak,
            "breakStartTime" to if(onBreak) breakStartTime else null,
            "totalBreakTime" to totalBreakTime,
            "lastUpdated" to System.currentTimeMillis()
        )
        firestore.collection("attendance").document(currentAttendanceId!!).update(updates)
            .addOnFailureListener { Log.e("ViewModel", "Failed to update break status", it) }
    }

    private fun saveAbsentToFirebase() {
        if (userId == null) return
        val organizationName = _employeeProfile.value?.organization ?: ""
        val record = AttendanceRecord(
            employeeId = userId, employeeName = employeeName, employeeOrg = organizationName,
            date = getTodayDateString(), status = "Absent"
        )
        firestore.collection("attendance").add(record)
            .addOnFailureListener { Log.e("ViewModel", "Failed to save Absent record", it) }
    }


    // --- Helper & Formatting Functions ---

    private fun updateTimer(ms: Long) {
        _homeUiState.value = _homeUiState.value?.copy(timerText = formatDuration(ms))
    }

    private fun getOfficialStartTime(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, OFFICIAL_START_TIME_HOUR)
            set(Calendar.MINUTE, OFFICIAL_START_TIME_MINUTE)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }

    private fun getFinalStatus(workMs: Long): String {
        val hrs = workMs / (1000 * 60 * 60)
        return when {
            hrs >= FULL_DAY_DURATION_HOURS -> if (wasLate) "Late" else "Present"
            hrs >= HALF_DAY_DURATION_HOURS -> "Half Day"
            else -> "Absent"
        }
    }

    private fun getTodayDateString(formatted: Boolean = false): String {
        val format = if (formatted) "dd MMM yyyy" else "yyyy-MM-dd"
        return SimpleDateFormat(format, Locale.getDefault()).format(Date())
    }

    private fun formatTime(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return ""
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatDuration(ms: Long): String {
        val seconds = (ms / 1000).toInt().coerceAtLeast(0)
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacks(timerRunnable)
    }
}