package com.example.workmate.Model

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

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
    }

    // --- Public methods for Fragments to call ---
    fun handleClockInOut() {
        if (!isClockedIn) clockIn() else clockOut()
    }

    fun handleBreakResume() {
        if (!isBreak) startBreak() else resumeFromBreak()
    }

    // --- Data Loading Logic ---
    private fun loadEmployeeDetails() {
        if (userId == null) return
        _homeUiState.value = _homeUiState.value?.copy(isLoading = true)
        firestore.collection("employee").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val profile = Employee(
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
            .get()
            .addOnSuccessListener { documents ->
                _attendanceHistory.value = documents.toObjects(AttendanceRecord::class.java)
            }
            .addOnFailureListener { Log.e("ViewModel", "Error loading history", it) }
    }


    // --- Clock In/Out and Break Logic (moved from Fragment) ---

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


    // --- Firestore Operations ---

    private fun saveCheckInToFirebase(checkIn: Long) {
        if (userId == null) return
        val initialStatus = if (checkIn > getOfficialStartTime()) "Late" else "Present"
        wasLate = initialStatus == "Late"
        val record = AttendanceRecord(
            employeeId = userId, employeeName = employeeName, date = getTodayDateString(),
            status = initialStatus, checkInTime = checkIn
        )
        firestore.collection("attendance").add(record)
            .addOnSuccessListener { ref -> currentAttendanceId = ref.id }
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
    }

    private fun saveAbsentToFirebase() {
        if (userId == null) return
        val record = AttendanceRecord(
            employeeId = userId, employeeName = employeeName,
            date = getTodayDateString(), status = "Absent"
        )
        firestore.collection("attendance").add(record)
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
        handler.removeCallbacks(timerRunnable) // Important: clean up handler to prevent memory leaks
    }
}