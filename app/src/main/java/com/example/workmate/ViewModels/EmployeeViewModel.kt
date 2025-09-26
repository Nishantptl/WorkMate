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
import com.google.firebase.firestore.toObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EmployeeViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val userId = auth.currentUser?.uid

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

    private val _employeeTasks = MutableLiveData<List<Task>>()
    val employeeTasks: LiveData<List<Task>> = _employeeTasks

    private val _selectedTask = MutableLiveData<Task?>()
    val selectedTask: LiveData<Task?> = _selectedTask

    private var isClockedIn = false
    private var isBreak = false
    private var checkInTime: Long = 0
    private var breakStartTime: Long = 0
    private var totalBreakTime: Long = 0
    private var workedTimeWhenPaused: Long = 0
    private var currentAttendanceId: String? = null
    private var wasLate: Boolean = false
    private var employeeName: String = "Employee"
    private var currentSelectedTaskId: String? = null
    private var currentSelectedTaskName: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable: Runnable


    companion object {
        const val OFFICIAL_START_TIME_HOUR = 10 // e.g., 9 AM
        const val OFFICIAL_START_TIME_MINUTE = 30
        const val CHECK_IN_CUTOFF_HOUR = 11 // 11 AM cutoff
        const val FULL_DAY_DURATION_HOURS = 8
        const val HALF_DAY_DURATION_HOURS = 4
        val ACTIVE_TASK_STATUSES = listOf("To Do", "In Progress")
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

    private fun isPastCheckInCutoff(): Boolean {
        val now = Calendar.getInstance()
        return now.get(Calendar.HOUR_OF_DAY) >= CHECK_IN_CUTOFF_HOUR
    }

    private fun shouldShowTaskSelectionMessage(): Boolean {
        // Show message if not clocked in, tasks are available, but none are selected
        return !isClockedIn &&
                (_employeeTasks.value?.isNotEmpty() == true && _selectedTask.value == null) &&
                _homeUiState.value?.clockInButtonText != "Work Complete" &&
                _homeUiState.value?.isMarkedAbsent != true
    }

    private fun calculateIsClockInButtonEnabled(): Boolean {
        if (isClockedIn) { // Clocked In: Button is "Check Out"
            return _homeUiState.value?.clockInButtonText != "Work Complete"
        }

        // Not Clocked In:
        if (_homeUiState.value?.isMarkedAbsent == true || _homeUiState.value?.clockInButtonText == "Work Complete") {
            return false // Already marked absent or work completed for the day
        }

        // Before clocking in, a task must be selected if tasks are available.
        val tasksAvailable = _employeeTasks.value?.isNotEmpty() == true
        if (tasksAvailable && _selectedTask.value == null) {
            return false // Tasks are available, but none selected
        }

        // Button is enabled if no tasks are available, or if tasks are available and one is selected.
        return true
    }

    private fun getClockInButtonText(): String {
        if (isClockedIn) return "Check Out"
        if (_homeUiState.value?.isMarkedAbsent == true) return "Absent"
        if (_homeUiState.value?.clockInButtonText == "Work Complete") return "Work Complete"
        return "Check In"
    }


    private fun updateHomeUiState() {
        _homeUiState.value = _homeUiState.value?.copy(
            isClockInButtonEnabled = calculateIsClockInButtonEnabled(),
            showTaskSelectionMessage = shouldShowTaskSelectionMessage(),
            clockInButtonText = getClockInButtonText()
        )
    }


    fun fetchEmployeeTasks() {
        if (userId == null) return
        _homeUiState.value = _homeUiState.value?.copy(isLoading = true)
        firestore.collection("tasks")
            .whereEqualTo("assignedToId", userId)
            .whereIn("status", ACTIVE_TASK_STATUSES)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("EmployeeVM", "Error fetching tasks", error)
                    _employeeTasks.postValue(emptyList())
                } else {
                    val tasksWithProjectNames = mutableListOf<Task>()
                    if (snapshots == null || snapshots.isEmpty) {
                        _employeeTasks.postValue(emptyList())
                    } else {
                        val totalTasksToProcess = snapshots.size()
                        var tasksProcessedCount = 0
                        snapshots.documents.forEach { taskDoc ->
                            val task = taskDoc.toObject(Task::class.java)?.apply { id = taskDoc.id }
                            if (task != null) {
                                firestore.collection("projects").document(task.projectId).get()
                                    .addOnCompleteListener { projectTask ->
                                        if (projectTask.isSuccessful && projectTask.result?.exists() == true) {
                                            task.projectName =
                                                projectTask.result.getString("projectName").toString()
                                        }
                                        tasksWithProjectNames.add(task)
                                        tasksProcessedCount++
                                        if (tasksProcessedCount == totalTasksToProcess) {
                                            _employeeTasks.postValue(tasksWithProjectNames.sortedByDescending { it.createdAt })
                                        }
                                    }
                            } else {
                                tasksProcessedCount++
                                if (tasksProcessedCount == totalTasksToProcess) {
                                    _employeeTasks.postValue(tasksWithProjectNames.sortedByDescending { it.createdAt })
                                }
                            }
                        }
                    }
                }
                _homeUiState.value = _homeUiState.value?.copy(isLoading = false)
                updateHomeUiState() // Recalculate button state and message
            }
    }

    // MODIFIED: This now also updates the task status
    fun setSelectedTask(task: Task?) {
        _selectedTask.value = task
        currentSelectedTaskId = task?.id
        currentSelectedTaskName = task?.taskName

        // When a task is selected, update its status to "In Progress"
        task?.let {
            if (it.status != "In Progress") {
                updateTaskStatus(it.id, "In Progress")
            }
        }
        updateHomeUiState()
    }

    // NEW: Function to mark a task as completed
    fun markTaskAsCompleted(task: Task) {
        updateTaskStatus(task.id, "Completed")
    }

    // NEW: Private helper function to handle Firestore updates
    private fun updateTaskStatus(taskId: String, newStatus: String) {
        firestore.collection("tasks").document(taskId)
            .update("status", newStatus)
            .addOnSuccessListener {
                Log.d("EmployeeVM", "Task $taskId status updated to $newStatus")
            }
            .addOnFailureListener { e ->
                Log.e("EmployeeVM", "Error updating task status for $taskId", e)
            }
    }


    fun submitLeaveRequest(request: LeaveRequest) {
        val newLeaveRef = firestore.collection("leave_requests").document()
        request.leaveId = newLeaveRef.id
        newLeaveRef.set(request)
            .addOnSuccessListener { _operationStatus.postValue(Pair(true, "Request successfully submitted!")) }
            .addOnFailureListener { _operationStatus.postValue(Pair(false, "Submission failed: ${it.message}")) }
    }

    fun fetchMyLeaveRequests() {
        if (userId == null) return
        firestore.collection("leave_requests").whereEqualTo("userId", userId)
            .orderBy("requestedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) { Log.e("EmployeeVM", "Error fetching leave requests", error); return@addSnapshotListener }
                _myLeaveRequests.postValue(snapshots?.toObjects(LeaveRequest::class.java) ?: emptyList())
            }
    }

    fun refreshLeaveRequests() { fetchMyLeaveRequests() }

    // CHANGE: Main logic updated here
    fun handleClockInOut() {
        if (!isClockedIn) {
            // Step 1: Enforce task selection if tasks are available.
            if (_employeeTasks.value?.isNotEmpty() == true && _selectedTask.value == null) {
                // This state should be prevented by the disabled button, but as a safeguard:
                updateHomeUiState() // Ensure the "select a task" message is visible
                return
            }

            // Step 2: AFTER task selection is confirmed, check the time.
            if (isPastCheckInCutoff()) {
                // If past cutoff, mark as absent and stop.
                markAbsent()
                return
            }

            // Step 3: If all conditions pass, clock in.
            clockIn()
        } else {
            clockOut()
        }
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
                    val profile = doc.toObject<Employee>()?.copy(uid = userId, email = auth.currentUser?.email ?: "")
                    _employeeProfile.value = profile
                    employeeName = profile?.name ?: "Employee"
                }
                checkTodayAttendance()
                fetchEmployeeTasks() // fetch tasks after initial attendance check
            }
            .addOnFailureListener {
                checkTodayAttendance()
                fetchEmployeeTasks()
            }
    }

    fun loadAttendanceHistory() {
        if (userId == null) return
        firestore.collection("attendance").whereEqualTo("employeeId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) { Log.e("ViewModel", "Error listening to history", error); return@addSnapshotListener }
                _attendanceHistory.postValue(snapshots?.toObjects(AttendanceRecord::class.java) ?: emptyList())
            }
    }

    private fun checkTodayAttendance() {
        if (userId == null) {
            _homeUiState.value = _homeUiState.value?.copy(isLoading = false)
            updateHomeUiState()
            return
        }
        val today = getTodayDateString()
        firestore.collection("attendance").whereEqualTo("employeeId", userId).whereEqualTo("date", today).limit(1).get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    // CHANGE: Removed logic that automatically marks absent on load.
                    // The app will now always start fresh and wait for user action.
                    resetStateForNewDay()
                } else {
                    val att = docs.documents[0].toObject(AttendanceRecord::class.java)!!
                    currentAttendanceId = docs.documents[0].id
                    wasLate = att.status == "Late"
                    currentSelectedTaskId = att.selectedTaskId
                    currentSelectedTaskName = att.selectedTaskName
                    // If a task was selected in the record, try to sync it with _selectedTask LiveData
                    if (currentSelectedTaskId != null) {
                        _employeeTasks.value?.find { it.id == currentSelectedTaskId }?.let { _selectedTask.postValue(it) }
                            ?: firestore.collection("tasks").document(currentSelectedTaskId!!).get().addOnSuccessListener { taskDoc ->
                                if (taskDoc.exists()){
                                    val task = taskDoc.toObject(Task::class.java)?.apply{id = taskDoc.id}
                                    task?.let{
                                        firestore.collection("projects").document(it.projectId).get().addOnSuccessListener{ projDoc ->
                                            if(projDoc.exists()){
                                                it.projectName = projDoc.getString("projectName").toString()
                                            }
                                            _selectedTask.postValue(it)
                                            updateHomeUiState()
                                        }
                                    }
                                }
                            }
                    }

                    when {
                        att.checkOutTime != null -> showCompletedState(att)
                        att.status == "Absent" -> showAbsentState(true) // Pass true to indicate it's from existing record
                        att.checkInTime != null -> restoreInProgressState(att)
                    }
                }
                _homeUiState.value = _homeUiState.value?.copy(isLoading = false)
                updateHomeUiState()
            }.addOnFailureListener {
                _homeUiState.value = _homeUiState.value?.copy(isLoading = false)
                resetStateForNewDay()
                updateHomeUiState()
            }
    }

    private fun clockIn() {
        val now = System.currentTimeMillis()
        isClockedIn = true
        checkInTime = now
        handler.post(timerRunnable)
        saveCheckInToFirebase(now, _selectedTask.value?.id, _selectedTask.value?.taskName)

        _homeUiState.value = _homeUiState.value?.copy(
            checkInTimeText = formatTime(now),
            isCheckInVisible = true,
            isBreakButtonVisible = true,
            isCheckOutVisible = false,
            isTotalWorkVisible = false,
            timerText = "00:00:00",
            isMarkedAbsent = false
        )
        updateHomeUiState()
    }

    private fun clockOut() {
        if (isBreak) resumeFromBreak()
        isClockedIn = false
        handler.removeCallbacks(timerRunnable)
        val checkOutTime = System.currentTimeMillis()
        val workDuration = (checkOutTime - checkInTime) - totalBreakTime
        saveCheckOutToFirebase(checkOutTime, workDuration)

        _homeUiState.value = _homeUiState.value?.copy(
            checkOutTimeText = formatTime(checkOutTime),
            isCheckOutVisible = true,
            totalWorkTimeText = formatDuration(workDuration),
            isTotalWorkVisible = true,
            isBreakButtonVisible = false,
            isOnBreak = false
        )
        updateTimer(workDuration)
        updateHomeUiState()
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
        updateHomeUiState()
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
        updateHomeUiState()
    }

    private fun resetStateForNewDay() {
        isClockedIn = false; isBreak = false; checkInTime = 0; totalBreakTime = 0
        currentAttendanceId = null; wasLate = false

        _homeUiState.value = HomeUiState(
            isLoading = _homeUiState.value?.isLoading ?: false,
            employeeName = employeeName,
            formattedDate = SimpleDateFormat("dd MMM yyyy, EEEE", Locale.getDefault()).format(Date()),
            clockInButtonText = getClockInButtonText(),
            isClockInButtonEnabled = calculateIsClockInButtonEnabled(),
            showTaskSelectionMessage = shouldShowTaskSelectionMessage()
        )
    }

    private fun showCompletedState(att: AttendanceRecord) {
        val workDuration = att.totalWorkDuration ?: 0
        isClockedIn = false
        _homeUiState.value = HomeUiState(
            isLoading = false, employeeName = employeeName,
            formattedDate = getTodayDateString(true),
            clockInButtonText = "Work Complete",
            checkInTimeText = formatTime(att.checkInTime), isCheckInVisible = true,
            checkOutTimeText = formatTime(att.checkOutTime), isCheckOutVisible = true,
            totalWorkTimeText = formatDuration(workDuration), isTotalWorkVisible = true,
            timerText = formatDuration(workDuration),
            isClockInButtonEnabled = false,
            isMarkedAbsent = false,
            showTaskSelectionMessage = false
        )
    }

    private fun showAbsentState(isFromExistingRecord: Boolean) {
        isClockedIn = false
        val currentIsLoading = _homeUiState.value?.isLoading ?: false
        _homeUiState.value = HomeUiState(
            isLoading = currentIsLoading, employeeName = employeeName,
            formattedDate = getTodayDateString(true),
            clockInButtonText = "Absent",
            isMarkedAbsent = true,
            isClockInButtonEnabled = false,
            showTaskSelectionMessage = false
        )
        if (!isFromExistingRecord) {
            saveAbsentToFirebase()
        }
    }

    private fun markAbsent() {
        // This function is now called explicitly from handleClockInOut after checks.
        showAbsentState(false)
    }


    private fun restoreInProgressState(att: AttendanceRecord) {
        isClockedIn = true
        checkInTime = att.checkInTime!!
        totalBreakTime = att.totalBreakTime
        isBreak = att.isOnBreak
        breakStartTime = att.breakStartTime ?: 0

        _homeUiState.value = _homeUiState.value?.copy(
            employeeName = employeeName,
            formattedDate = getTodayDateString(true),
            isCheckInVisible = true,
            checkInTimeText = formatTime(att.checkInTime),
            isBreakButtonVisible = true,
            isMarkedAbsent = false
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
        updateHomeUiState()
    }

    private fun saveCheckInToFirebase(checkIn: Long, taskId: String?, taskName: String?) {
        if (userId == null) return
        val record = AttendanceRecord(
            employeeId = userId, employeeName = employeeName, employeeOrg = _employeeProfile.value?.organization ?: "",
            date = getTodayDateString(), status = if (checkIn > getOfficialStartTimeMillis()) "Late" else "Present",
            checkInTime = checkIn, selectedTaskId = taskId, selectedTaskName = taskName
        ).also { wasLate = it.status == "Late" }

        firestore.collection("attendance").add(record)
            .addOnSuccessListener { currentAttendanceId = it.id }
            .addOnFailureListener { Log.e("ViewModel", "Failed to save Check-In", it) }
    }

    private fun saveCheckOutToFirebase(checkOut: Long, workMs: Long) {
        if (userId == null || currentAttendanceId == null) return
        val updates = mapOf(
            "checkOutTime" to checkOut, "totalWorkDuration" to workMs, "totalBreakTime" to totalBreakTime,
            "status" to getFinalStatus(workMs), "isOnBreak" to false,
            "lastUpdated" to System.currentTimeMillis()
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

        val today = getTodayDateString()
        firestore.collection("attendance")
            .whereEqualTo("employeeId", userId)
            .whereEqualTo("date", today)
            .limit(1).get() // Check for any record today
            .addOnSuccessListener { existingDocs ->
                if (existingDocs.isEmpty) {
                    val record = AttendanceRecord(
                        employeeId = userId, employeeName = employeeName, employeeOrg = _employeeProfile.value?.organization ?: "",
                        date = today, status = "Absent",
                        selectedTaskId = _selectedTask.value?.id, // Also save the task they selected
                        selectedTaskName = _selectedTask.value?.taskName
                    )
                    firestore.collection("attendance").add(record)
                        .addOnSuccessListener { currentAttendanceId = it.id }
                        .addOnFailureListener { Log.e("ViewModel", "Failed to save Absent record", it) }
                } else {
                    // A record already exists (maybe they clocked in then out quickly).
                    // We can choose to update it to "Absent" or leave it. Updating seems more correct.
                    val docId = existingDocs.documents[0].id
                    val updates = mapOf(
                        "status" to "Absent",
                        "checkInTime" to null,
                        "checkOutTime" to null,
                        "totalWorkDuration" to 0L,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    firestore.collection("attendance").document(docId).update(updates)
                        .addOnFailureListener { Log.e("ViewModel", "Failed to update to Absent", it) }
                }
            }.addOnFailureListener {
                Log.e("ViewModel", "Error checking for existing absent record", it)
            }
    }


    // --- Helper & Formatting Functions ---

    private fun updateTimer(ms: Long) { _homeUiState.value = _homeUiState.value?.copy(timerText = formatDuration(ms)) }

    private fun getOfficialStartTimeMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, OFFICIAL_START_TIME_HOUR)
        set(Calendar.MINUTE, OFFICIAL_START_TIME_MINUTE)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun getFinalStatus(workMs: Long): String {
        val hrs = workMs / (3600000)
        return when {
            hrs >= FULL_DAY_DURATION_HOURS -> if (wasLate) "Late" else "Present"
            hrs >= HALF_DAY_DURATION_HOURS -> "Half Day"
            else -> "Absent"
        }
    }

    private fun getTodayDateString(formatted: Boolean = false): String {
        val formatPattern = if (formatted) "dd MMM yyyy, EEEE" else "yyyy-MM-dd"
        return SimpleDateFormat(formatPattern, Locale.getDefault()).format(Date())
    }

    private fun formatTime(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return "--:--"
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