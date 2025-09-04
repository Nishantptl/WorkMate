package com.example.EAMS.EmployeeFragments

import android.annotation.SuppressLint
import android.os.*
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.EAMS.R
import com.example.EAMS.Model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    // --- UI Elements ---
    private lateinit var txtEmployeeName: TextView
    private lateinit var txtEmployeeDepartment: TextView
    private lateinit var txtDate: TextView
    private lateinit var txtTime: TextView
    private lateinit var txtCheck: TextView
    private lateinit var btnClockIn: LinearLayout
    private lateinit var txtCheckInTime: TextView
    private lateinit var txtCheckOutTime: TextView
    private lateinit var txtTotalWorkTime: TextView
    private lateinit var txtOnBreak: TextView
    private lateinit var txtBreak: TextView

    // --- Firebase ---
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var employeeName: String? = null

    // --- State variables for time tracking ---
    private var isClockedIn = false
    private var isBreak = false
    private var checkInTime: Long = 0
    private var breakStartTime: Long = 0
    private var totalBreakTime: Long = 0
    private var workedTimeWhenPaused: Long = 0
    private var currentAttendanceId: String? = null
    private var wasLate: Boolean = false // To remember if the initial clock-in was late

    // --- Attendance Rules Constants ---
    companion object {
        const val OFFICIAL_START_TIME_HOUR = 10    //  10 AM
        const val OFFICIAL_START_TIME_MINUTE = 30 // 30 minutes
        const val FULL_DAY_DURATION_HOURS = 8     // 8 hours for a full day
        const val HALF_DAY_DURATION_HOURS = 4     // 4 hours for a half day
    }

    // --- Handler for the main work timer ---
    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isClockedIn) {
                val elapsed = (System.currentTimeMillis() - checkInTime) - totalBreakTime
                val effectiveElapsed = if (isBreak) workedTimeWhenPaused else elapsed

                val seconds = (effectiveElapsed / 1000).toInt()
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                val secs = seconds % 60

                txtTime.text = String.format("%02d:%02d:%02d", hours, minutes, secs)

                if (!isBreak)    {
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize UI components
        txtEmployeeName = view.findViewById(R.id.txtEmployeeName)
        txtEmployeeDepartment = view.findViewById(R.id.txtEmployeeDepartment)
        txtDate = view.findViewById(R.id.txtDate)
        txtTime = view.findViewById(R.id.txtTime)
        txtCheck = view.findViewById(R.id.txtCheck)
        btnClockIn = view.findViewById(R.id.btnClockIn)
        txtCheckInTime = view.findViewById(R.id.txtCheckInTime)
        txtCheckOutTime = view.findViewById(R.id.txtCheckOutTime)
        txtTotalWorkTime = view.findViewById(R.id.txtTotalWorkTime)
        txtOnBreak = view.findViewById(R.id.txtOnBreak)
        txtBreak = view.findViewById(R.id.txtBreak)

        loadEmployeeDetails()
        setDate()
        checkTodayAttendance()

        btnClockIn.setOnClickListener {
            handleClockInOut()
        }

        txtBreak.setOnClickListener {
            handleBreakResume()
        }

        return view
    }

    // --- UI and Data Loading ---
    private fun loadEmployeeDetails() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("employee")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") ?: "Employee"
                        val department = document.getString("department") ?: "Unknown Dept"
                        employeeName = name

                        txtEmployeeName.text = "Hi, $name"
                        txtEmployeeDepartment.text = department
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setDate() {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        txtDate.text = sdf.format(Date())
    }

    // --- Attendance State Management ---
    private fun checkTodayAttendance() {
        val userId = auth.currentUser?.uid ?: return
        val today = getTodayDateString()

        firestore.collection("attendance")
            .whereEqualTo("employeeId", userId)
            .whereEqualTo("date", today)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    currentAttendanceId = document.id

                    // Convert directly into data class
                    val attendance = document.toObject(AttendanceRecord::class.java)

                    if (attendance != null) {
                        wasLate = attendance.status == "Late"

                        when {
                            // Case 1: Already Absent → disable button
                            attendance.status == "Absent" -> {
                                txtCheck.text = "Absent"
                                btnClockIn.isEnabled = false
                                btnClockIn.alpha = 0.5f
                                txtCheckInTime.visibility = View.VISIBLE
                                txtCheckInTime.text = "Marked absent today"
                                txtBreak.visibility = View.GONE
                                txtTime.text = "00:00:00"
                            }

                            // Case 2: Checked in but not yet out
                            attendance.checkInTime > 0 && attendance.checkOutTime == null -> {
                                restoreClockInState(
                                    checkInTimestamp = attendance.checkInTime,
                                    totalBreakDuration = attendance.totalBreakTime,
                                    isOnBreak = attendance.isOnBreak,
                                    breakStart = attendance.breakStartTime ?: 0L,
                                    lastUpdated = attendance.lastUpdated
                                )
                            }

                            // Case 3: Work already completed
                            attendance.checkInTime > 0 && attendance.checkOutTime != null -> {
                                showCompletedWorkState(
                                    checkInTimestamp = attendance.checkInTime,
                                    checkOutTimestamp = attendance.checkOutTime,
                                    totalBreakDuration = attendance.totalBreakTime
                                )
                            }
                            else -> {
                                resetUI()
                            }
                        }
                    } else {
                        resetUI()
                    }
                } else {
                    resetUI()
                }
            }
            .addOnFailureListener {
                resetUI()
                Toast.makeText(requireContext(), "Failed to load attendance data", Toast.LENGTH_SHORT).show()
            }
    }


    private fun restoreClockInState(
        checkInTimestamp: Long,
        totalBreakDuration: Long,
        isOnBreak: Boolean,
        breakStart: Long,
        lastUpdated: Long
    ) {
        isClockedIn = true
        checkInTime = checkInTimestamp
        totalBreakTime = totalBreakDuration
        this.isBreak = isOnBreak

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        txtCheck.text = "Check Out"
        txtCheckInTime.visibility = View.VISIBLE
        txtCheckInTime.text = "Checked in at " + timeFormat.format(Date(checkInTimestamp))
        txtBreak.visibility = View.VISIBLE
        txtCheckOutTime.visibility = View.GONE
        txtTotalWorkTime.visibility = View.GONE

        if (isOnBreak) {
            txtBreak.text = "Resume"
            breakStartTime = breakStart.takeIf { it > 0 } ?: lastUpdated

            workedTimeWhenPaused = (breakStartTime - checkInTime) - totalBreakTime

            val seconds = (workedTimeWhenPaused / 1000).toInt().coerceAtLeast(0)
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            txtTime.text = String.format("%02d:%02d:%02d", hours, minutes, secs)

            handler.removeCallbacks(timerRunnable)
        } else {
            txtBreak.text = "Break"
            handler.post(timerRunnable)
        }
    }

    private fun showCompletedWorkState(checkInTimestamp: Long, checkOutTimestamp: Long, totalBreakDuration: Long) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        txtCheck.text = "Work Complete"
        btnClockIn.isEnabled = false
        btnClockIn.alpha = 0.5f

        txtCheckInTime.visibility = View.VISIBLE
        txtCheckInTime.text = "Checked in at " + timeFormat.format(Date(checkInTimestamp))

        txtCheckOutTime.visibility = View.VISIBLE
        txtCheckOutTime.text = "Checked out at " + timeFormat.format(Date(checkOutTimestamp))

        val workDuration = (checkOutTimestamp - checkInTimestamp) - totalBreakDuration
        val hours = (workDuration / (1000 * 60 * 60)).toInt()
        val minutes = ((workDuration / (1000 * 60)) % 60).toInt()
        val seconds = ((workDuration / 1000) % 60).toInt()

        txtTotalWorkTime.visibility = View.VISIBLE
        txtTotalWorkTime.text = "Total working hours " + String.format("%02d:%02d:%02d", hours, minutes, seconds)
        txtTime.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        txtBreak.visibility = View.GONE
    }

    private fun resetUI() {
        isClockedIn = false
        isBreak = false
        checkInTime = 0
        totalBreakTime = 0
        workedTimeWhenPaused = 0
        currentAttendanceId = null
        wasLate = false

        txtTime.text = "00:00:00"
        txtCheck.text = "Check In"
        txtCheckInTime.visibility = View.GONE
        txtCheckOutTime.visibility = View.GONE
        txtTotalWorkTime.visibility = View.GONE
        txtBreak.visibility = View.GONE

        btnClockIn.isEnabled = true
        btnClockIn.alpha = 1.0f
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // --- Main Logic: Clock-In/Out and Breaks ---
    @SuppressLint("SetTextI18n")
    private fun handleBreakResume() {
        if (!isBreak) {
            isBreak = true
            breakStartTime = System.currentTimeMillis()
            workedTimeWhenPaused = (breakStartTime - checkInTime) - totalBreakTime

            handler.removeCallbacks(timerRunnable)
            txtOnBreak.visibility = View.VISIBLE
            txtBreak.text = "Resume"
            Toast.makeText(requireContext(), "Break Started", Toast.LENGTH_SHORT).show()
            updateBreakStatus(true)
        } else {
            isBreak = false
            totalBreakTime += System.currentTimeMillis() - breakStartTime
            handler.post(timerRunnable)
            txtOnBreak.visibility = View.GONE
            txtBreak.text = "Break"
            Toast.makeText(requireContext(), "Resumed", Toast.LENGTH_SHORT).show()
            updateBreakStatus(false)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun handleClockInOut() {
        val currentTime = System.currentTimeMillis()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val userId = auth.currentUser?.uid ?: return

        if (!isClockedIn) {
            // --- Check cutoff time (11:00 AM) ---
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 11)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val cutoffTime = calendar.timeInMillis

            if (currentTime > cutoffTime) {
                // Past 11:00 AM → Mark Absent, disable check-in
                txtCheck.text = "Absent"
                btnClockIn.isEnabled = false
                btnClockIn.alpha = 0.5f
                txtCheckInTime.visibility = View.VISIBLE
                txtCheckInTime.text = "Attendance closed at " + timeFormat.format(Date(currentTime))
                txtBreak.visibility = View.GONE
                txtTime.text = "00:00:00"

                // Save directly as Absent record
                val attendanceRecord = AttendanceRecord(
                    employeeId = userId,
                    employeeName = employeeName ?: "",
                    date = getTodayDateString(),
                    status = "Absent",
                    checkInTime = 0L,
                    checkOutTime = null,
                    totalWorkDuration = null,
                    totalBreakTime = 0L,
                    isOnBreak = false,
                    breakStartTime = null,
                    createdAt = System.currentTimeMillis(),
                    lastUpdated = System.currentTimeMillis()
                )

                firestore.collection("attendance")
                    .add(attendanceRecord)
                    .addOnSuccessListener { documentReference ->
                        currentAttendanceId = documentReference.id
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to mark Absent", Toast.LENGTH_SHORT).show()
                    }

                Toast.makeText(requireContext(), "Check-in closed after 11:00 AM", Toast.LENGTH_SHORT).show()
                return
            }

            // --- Normal Clock In (before 11 AM) ---
            isClockedIn = true
            checkInTime = currentTime
            totalBreakTime = 0
            isBreak = false
            workedTimeWhenPaused = 0

            txtCheck.text = "Check Out"
            txtCheckInTime.visibility = View.VISIBLE
            txtBreak.visibility = View.VISIBLE
            txtBreak.text = "Break"
            txtCheckInTime.text = "Checked in at " + timeFormat.format(Date(currentTime))
            txtCheckOutTime.visibility = View.GONE
            txtTotalWorkTime.visibility = View.GONE
            txtTime.text = "00:00:00"
            handler.post(timerRunnable)

            saveCheckInToFirebase(userId, currentTime)
            Toast.makeText(requireContext(), "Clocked In", Toast.LENGTH_SHORT).show()
        } else {
            // --- Clock Out ---
            if (isBreak) {
                handleBreakResume()
            }

            isClockedIn = false
            val checkOutTime = currentTime
            handler.removeCallbacks(timerRunnable)

            txtCheck.text = "Work Complete"
            btnClockIn.isEnabled = false
            btnClockIn.alpha = 0.5f
            txtCheckOutTime.visibility = View.VISIBLE
            txtCheckOutTime.text = "Checked out at " + timeFormat.format(Date(checkOutTime))
            txtBreak.visibility = View.GONE

            val diff = (checkOutTime - checkInTime) - totalBreakTime
            val hours = (diff / (1000 * 60 * 60)).toInt()
            val minutes = ((diff / (1000 * 60)) % 60).toInt()
            val seconds = ((diff / 1000) % 60).toInt()

            txtTotalWorkTime.visibility = View.VISIBLE
            txtTotalWorkTime.text = "Total working hours " + String.format("%02d:%02d:%02d", hours, minutes, seconds)

            saveCheckOutToFirebase(checkOutTime, diff)
            Toast.makeText(requireContext(), "Clocked Out", Toast.LENGTH_SHORT).show()
        }
    }


    // --- Firebase Data Handling ---
    private fun updateBreakStatus(isOnBreak: Boolean) {
        currentAttendanceId?.let { attendanceId ->
            firestore.collection("attendance")
                .document(attendanceId)
                .get()
                .addOnSuccessListener { snapshot ->
                    val record = snapshot.toObject(AttendanceRecord::class.java)
                    if (record != null) {
                        val updatedRecord = record.copy(
                            isOnBreak = isOnBreak,
                            totalBreakTime = totalBreakTime,
                            lastUpdated = System.currentTimeMillis(),
                            breakStartTime = if (isOnBreak) breakStartTime else null
                        )

                        firestore.collection("attendance")
                            .document(attendanceId)
                            .set(updatedRecord) // overwrite with updated data class
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Failed to update break status", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to fetch record for update", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun getInitialStatus(checkInTime: Long): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, OFFICIAL_START_TIME_HOUR)
        calendar.set(Calendar.MINUTE, OFFICIAL_START_TIME_MINUTE)
        calendar.set(Calendar.SECOND, 0)
        val officialStartTime = calendar.timeInMillis

        wasLate = checkInTime > officialStartTime
        return if (wasLate) "Late" else "Present"
    }

    private fun getFinalStatus(totalWorkDuration: Long): String {
        val totalWorkHours = totalWorkDuration / (1000 * 60 * 60)
        return when {
            totalWorkHours >= FULL_DAY_DURATION_HOURS -> if (wasLate) "Late" else "Present"
            totalWorkHours >= HALF_DAY_DURATION_HOURS -> "Half Day"
            else -> "Absent"
        }
    }

    private fun saveCheckInToFirebase(userId: String, checkInTime: Long) {
        val initialStatus = getInitialStatus(checkInTime)

        val attendanceRecord = AttendanceRecord(
            employeeId = userId,
            employeeName = employeeName ?: "",
            date = getTodayDateString(),
            status = initialStatus,
            checkInTime = checkInTime,
            checkOutTime = null,                // not set yet
            totalWorkDuration = null,           // not set yet
            totalBreakTime = 0L,
            isOnBreak = false,
            breakStartTime = null,              // not set yet
            createdAt = System.currentTimeMillis(),
            lastUpdated = System.currentTimeMillis()
        )

        firestore.collection("attendance")
            .add(attendanceRecord)
            .addOnSuccessListener { documentReference ->
                currentAttendanceId = documentReference.id
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to save check-in data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveCheckOutToFirebase(checkOutTime: Long, totalWorkDuration: Long) {
        currentAttendanceId?.let { attendanceId ->
            firestore.collection("attendance")
                .document(attendanceId)
                .get()
                .addOnSuccessListener { snapshot ->
                    val record = snapshot.toObject(AttendanceRecord::class.java)
                    if (record != null) {
                        val finalStatus = getFinalStatus(totalWorkDuration)

                        val updatedRecord = record.copy(
                            checkOutTime = checkOutTime,
                            totalWorkDuration = totalWorkDuration,
                            totalBreakTime = totalBreakTime,
                            status = finalStatus,
                            isOnBreak = false,
                            breakStartTime = null, // clear break start
                            lastUpdated = System.currentTimeMillis()
                        )

                        firestore.collection("attendance")
                            .document(attendanceId)
                            .set(updatedRecord)
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Failed to save check-out data", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to fetch record for checkout", Toast.LENGTH_SHORT).show()
                }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timerRunnable)
    }
}
