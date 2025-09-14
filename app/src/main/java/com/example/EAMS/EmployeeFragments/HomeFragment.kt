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
    private lateinit var txtDate: TextView
    private lateinit var txtTime: TextView
    private lateinit var btnClockIn: Button
    private lateinit var txtCheckInTime: TextView
    private lateinit var txtCheckOutTime: TextView
    private lateinit var txtTotalWorkTime: TextView
    private lateinit var txtMarkedAbsent: TextView
    private lateinit var txtOnBreak: TextView
    private lateinit var txtBreak: TextView
    private lateinit var blurOverlay: View
    private lateinit var progressBar: ProgressBar

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var employeeName: String? = null

    private var isClockedIn = false
    private var isBreak = false
    private var checkInTime: Long = 0
    private var breakStartTime: Long = 0
    private var totalBreakTime: Long = 0
    private var workedTimeWhenPaused: Long = 0
    private var currentAttendanceId: String? = null
    private var wasLate: Boolean = false

    companion object {
        const val OFFICIAL_START_TIME_HOUR = 10
        const val OFFICIAL_START_TIME_MINUTE = 30
        const val FULL_DAY_DURATION_HOURS = 8
        const val HALF_DAY_DURATION_HOURS = 4
    }

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        @SuppressLint("DefaultLocale")
        override fun run() {
            if (isClockedIn) {
                val elapsed = (System.currentTimeMillis() - checkInTime) - totalBreakTime
                val effectiveElapsed = if (isBreak) workedTimeWhenPaused else elapsed

                val seconds = (effectiveElapsed / 1000).toInt()
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                val secs = seconds % 60

                txtTime.text = String.format("%02d:%02d:%02d", hours, minutes, secs)

                if (!isBreak) handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        txtEmployeeName = view.findViewById(R.id.txtEmployeeName)
        txtDate = view.findViewById(R.id.txtDate)
        txtTime = view.findViewById(R.id.txtTime)
        btnClockIn = view.findViewById(R.id.btnClockIn)
        txtCheckInTime = view.findViewById(R.id.txtCheckInTime)
        txtCheckOutTime = view.findViewById(R.id.txtCheckOutTime)
        txtTotalWorkTime = view.findViewById(R.id.txtTotalWorkTime)
        txtMarkedAbsent = view.findViewById(R.id.txtMarkedAbsent)
        txtOnBreak = view.findViewById(R.id.txtOnBreak)
        txtBreak = view.findViewById(R.id.txtBreak)
        blurOverlay = view.findViewById(R.id.blurOverlay)
        progressBar  = view.findViewById(R.id.progressBar)

        loadEmployeeDetails()
        setDate()
        checkTodayAttendance()

        btnClockIn.setOnClickListener { handleClockInOut() }
        txtBreak.setOnClickListener { handleBreakResume() }

        return view
    }

    private fun showLoading(show: Boolean) {
        blurOverlay.visibility = if (show) View.VISIBLE else View.GONE
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    @SuppressLint("SetTextI18n")
    private fun loadEmployeeDetails() {
        showLoading(true)
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("employee")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") ?: "Employee"
                        employeeName = name
                        txtEmployeeName.text = "Hi, $name"
                    }
                    showLoading(false)
                }
                .addOnFailureListener {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setDate() {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        txtDate.text = sdf.format(Date())
    }

    // -------- Attendance check & UI update --------
    @SuppressLint("SetTextI18n")
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
                    val doc = documents.documents[0]
                    currentAttendanceId = doc.id
                    val att = doc.toObject(AttendanceRecord::class.java) ?: return@addOnSuccessListener
                    wasLate = att.status == "Late"

                    when {
                        att.checkInTime != null && att.checkOutTime != null -> {
                            showCompletedWorkState(
                                att.checkInTime, att.checkOutTime,
                                att.totalBreakTime, att.status
                            )
                        }
                        att.status == "Absent" -> {
                            btnClockIn.text = "Absent"
                            btnClockIn.isEnabled = false
                            btnClockIn.alpha = 0.5f
                            txtCheckInTime.visibility = View.VISIBLE
                            txtBreak.visibility = View.GONE
                            txtTime.text = "00:00:00"
                            txtMarkedAbsent.visibility = View.VISIBLE
                        }
                        att.checkInTime != null -> {
                            restoreClockInState(
                                att.checkInTime,
                                att.totalBreakTime,
                                att.isOnBreak,
                                att.breakStartTime ?: 0L,
                                att.lastUpdated
                            )
                        }
                        else -> resetUI()
                    }
                } else resetUI()
            }
            .addOnFailureListener {
                resetUI()
                Toast.makeText(requireContext(), "Failed to load attendance data", Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("SetTextI18n")
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

        val tf = SimpleDateFormat("HH:mm", Locale.getDefault())
        btnClockIn.text = "Check Out"
        txtCheckInTime.visibility = View.VISIBLE
        txtCheckInTime.text = tf.format(Date(checkInTimestamp))
        txtBreak.visibility = View.VISIBLE
        txtCheckOutTime.visibility = View.GONE
        txtTotalWorkTime.visibility = View.GONE
        txtMarkedAbsent.visibility = View.GONE

        if (isOnBreak) {
            txtBreak.text = "Click here to continue"
            breakStartTime = breakStart.takeIf { it > 0 } ?: lastUpdated
            workedTimeWhenPaused = (breakStartTime - checkInTime) - totalBreakTime
            updateTimerText(workedTimeWhenPaused)
            handler.removeCallbacks(timerRunnable)
        } else {
            txtBreak.text = "I'm taking a break"
            handler.post(timerRunnable)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showCompletedWorkState(
        checkInTimestamp: Long,
        checkOutTimestamp: Long,
        totalBreakDuration: Long,
        status: String? = null
    ) {
        val tf = SimpleDateFormat("HH:mm", Locale.getDefault())
        btnClockIn.text = "Work Complete"
        btnClockIn.isEnabled = false
        btnClockIn.alpha = 0.5f

        txtCheckInTime.visibility = View.VISIBLE
        txtCheckInTime.text = tf.format(Date(checkInTimestamp))

        txtCheckOutTime.visibility = View.VISIBLE
        txtCheckOutTime.text = tf.format(Date(checkOutTimestamp))

        val workDuration = (checkOutTimestamp - checkInTimestamp) - totalBreakDuration
        updateTimerText(workDuration)

        txtTotalWorkTime.visibility = View.VISIBLE
        txtTotalWorkTime.text = formatDuration(workDuration)
        txtBreak.visibility = View.GONE

        txtMarkedAbsent.visibility = if (status == "Absent") View.VISIBLE else View.GONE
    }

    @SuppressLint("SetTextI18n")
    private fun resetUI() {
        isClockedIn = false
        isBreak = false
        checkInTime = 0
        totalBreakTime = 0
        workedTimeWhenPaused = 0
        currentAttendanceId = null
        wasLate = false

        txtTime.text = "00:00:00"
        btnClockIn.text = "Check In"
        txtCheckInTime.visibility = View.GONE
        txtCheckOutTime.visibility = View.GONE
        txtTotalWorkTime.visibility = View.GONE
        txtBreak.visibility = View.GONE
        txtMarkedAbsent.visibility = View.GONE

        btnClockIn.isEnabled = true
        btnClockIn.alpha = 1f
    }

    private fun getTodayDateString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // -------- Timer helper --------
    @SuppressLint("DefaultLocale")
    private fun updateTimerText(ms: Long) {
        val seconds = (ms / 1000).toInt().coerceAtLeast(0)
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        txtTime.text = String.format("%02d:%02d:%02d", h, m, s)
    }

    @SuppressLint("DefaultLocale")
    private fun formatDuration(ms: Long): String {
        val h = (ms / (1000 * 60 * 60)).toInt()
        val m = ((ms / (1000 * 60)) % 60).toInt()
        val s = ((ms / 1000) % 60).toInt()
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    // -------- Break handling --------
    @SuppressLint("SetTextI18n")
    private fun handleBreakResume() {
        if (!isBreak) {
            isBreak = true
            breakStartTime = System.currentTimeMillis()
            workedTimeWhenPaused = (breakStartTime - checkInTime) - totalBreakTime
            handler.removeCallbacks(timerRunnable)
            txtOnBreak.visibility = View.VISIBLE
            txtBreak.text = "Click here to continue"
            Toast.makeText(requireContext(), "Break Started", Toast.LENGTH_SHORT).show()
            updateBreakStatus(true)
        } else {
            isBreak = false
            totalBreakTime += System.currentTimeMillis() - breakStartTime
            handler.post(timerRunnable)
            txtOnBreak.visibility = View.GONE
            txtBreak.text = "I'm taking a break"
            Toast.makeText(requireContext(), "Resumed", Toast.LENGTH_SHORT).show()
            updateBreakStatus(false)
        }
    }

    // -------- Clock in/out --------
    @SuppressLint("SetTextI18n")
    private fun handleClockInOut() {
        val now = System.currentTimeMillis()
        val tf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val userId = auth.currentUser?.uid ?: return

        if (!isClockedIn) {
            // Cutoff 11:00
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 11)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            if (now > cal.timeInMillis) {
                markAbsent(userId, tf)
                return
            }

            isClockedIn = true
            checkInTime = now
            totalBreakTime = 0
            isBreak = false
            workedTimeWhenPaused = 0

            btnClockIn.text = "Check Out"
            txtCheckInTime.visibility = View.VISIBLE
            txtBreak.visibility = View.VISIBLE
            txtBreak.text = "I'm taking a break"
            txtCheckInTime.text = tf.format(Date(now))
            txtCheckOutTime.visibility = View.GONE
            txtTotalWorkTime.visibility = View.GONE
            txtTime.text = "00:00:00"
            handler.post(timerRunnable)

            saveCheckInToFirebase(userId, now)
            Toast.makeText(requireContext(), "Clocked In", Toast.LENGTH_SHORT).show()
        } else {
            if (isBreak) handleBreakResume()

            isClockedIn = false
            val checkOutTime = now
            handler.removeCallbacks(timerRunnable)

            btnClockIn.text = "Work Complete"
            btnClockIn.isEnabled = false
            btnClockIn.alpha = 0.5f
            txtCheckOutTime.visibility = View.VISIBLE
            txtCheckOutTime.text = tf.format(Date(checkOutTime))
            txtBreak.visibility = View.GONE

            val diff = (checkOutTime - checkInTime) - totalBreakTime
            txtTotalWorkTime.visibility = View.VISIBLE
            txtTotalWorkTime.text = formatDuration(diff)
            updateTimerText(diff)

            saveCheckOutToFirebase(checkOutTime, diff)
            Toast.makeText(requireContext(), "Clocked Out", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun markAbsent(userId: String, tf: SimpleDateFormat) {
        btnClockIn.text = "Absent"
        btnClockIn.isEnabled = false
        btnClockIn.alpha = 0.5f
        txtBreak.visibility = View.GONE
        txtTime.text = "00:00:00"
        txtMarkedAbsent.visibility = View.VISIBLE
        txtMarkedAbsent.text = "Marked Absent due to late check-in"

        val attendance = AttendanceRecord(
            employeeId = userId,
            employeeName = employeeName ?: "",
            date = getTodayDateString(),
            status = "Absent",
            checkInTime = null,
            checkOutTime = null,
            totalWorkDuration = null,
            totalBreakTime = 0L,
            isOnBreak = false,
            breakStartTime = null,
            createdAt = System.currentTimeMillis(),
            lastUpdated = System.currentTimeMillis()
        )
        firestore.collection("attendance").add(attendance)
    }

    // -------- Firestore helpers --------
    private fun updateBreakStatus(onBreak: Boolean) {
        currentAttendanceId?.let { id ->
            firestore.collection("attendance").document(id).get()
                .addOnSuccessListener { snap ->
                    val record = snap.toObject(AttendanceRecord::class.java) ?: return@addOnSuccessListener
                    val updated = record.copy(
                        isOnBreak = onBreak,
                        totalBreakTime = totalBreakTime,
                        lastUpdated = System.currentTimeMillis(),
                        breakStartTime = if (onBreak) breakStartTime else null
                    )
                    firestore.collection("attendance").document(id).set(updated)
                }
        }
    }

    private fun getInitialStatus(time: Long): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, OFFICIAL_START_TIME_HOUR)
        cal.set(Calendar.MINUTE, OFFICIAL_START_TIME_MINUTE)
        cal.set(Calendar.SECOND, 0)
        wasLate = time > cal.timeInMillis
        return if (wasLate) "Late" else "Present"
    }

    private fun getFinalStatus(workMs: Long): String {
        val hrs = workMs / (1000 * 60 * 60)
        return when {
            hrs >= FULL_DAY_DURATION_HOURS -> if (wasLate) "Late" else "Present"
            hrs >= HALF_DAY_DURATION_HOURS -> "Half Day"
            else -> "Absent"
        }
    }

    private fun saveCheckInToFirebase(userId: String, checkIn: Long) {
        val initialStatus = getInitialStatus(checkIn)
        val record = AttendanceRecord(
            employeeId = userId,
            employeeName = employeeName ?: "",
            date = getTodayDateString(),
            status = initialStatus,
            checkInTime = checkIn,
            checkOutTime = null,
            totalWorkDuration = null,
            totalBreakTime = 0L,
            isOnBreak = false,
            breakStartTime = null,
            createdAt = System.currentTimeMillis(),
            lastUpdated = System.currentTimeMillis()
        )
        firestore.collection("attendance").add(record)
            .addOnSuccessListener { ref -> currentAttendanceId = ref.id }
    }

    private fun saveCheckOutToFirebase(checkOut: Long, workMs: Long) {
        currentAttendanceId?.let { id ->
            firestore.collection("attendance").document(id).get()
                .addOnSuccessListener { snap ->
                    val record = snap.toObject(AttendanceRecord::class.java) ?: return@addOnSuccessListener
                    val finalStatus = getFinalStatus(workMs)
                    val updated = record.copy(
                        checkOutTime = checkOut,
                        totalWorkDuration = workMs,
                        totalBreakTime = totalBreakTime,
                        status = finalStatus,
                        isOnBreak = false,
                        breakStartTime = null,
                        lastUpdated = System.currentTimeMillis()
                    )
                    firestore.collection("attendance").document(id).set(updated)
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timerRunnable)
    }
}
