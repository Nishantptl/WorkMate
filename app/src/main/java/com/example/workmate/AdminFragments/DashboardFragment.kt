package com.example.workmate.AdminFragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.workmate.R
import com.example.workmate.Model.*
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var blurOverlay: View

    private lateinit var txtWelcome: TextView
    private lateinit var txtOrganization: TextView
    private lateinit var txtDate: TextView
    private lateinit var txtTotalEmployee: TextView
    private lateinit var txtPresentCount: TextView
    private lateinit var txtAbsentCount: TextView
    private lateinit var txtHalfDayCount: TextView
    private lateinit var txtLateCount: TextView
    private lateinit var attendanceChart: PieChart

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val adminViewModel: AdminViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        progressBar = view.findViewById(R.id.progressBar)
        blurOverlay = view.findViewById(R.id.blurOverlay)
        txtWelcome = view.findViewById(R.id.txtWelcome)
        txtOrganization = view.findViewById(R.id.txtOrganization)
        txtDate = view.findViewById(R.id.txtDate)
        txtTotalEmployee = view.findViewById(R.id.txtTotalEmployee)
        attendanceChart = view.findViewById(R.id.attendance_chart)
        txtPresentCount = view.findViewById(R.id.txtPresentCount)
        txtAbsentCount = view.findViewById(R.id.txtAbsentCount)
        txtHalfDayCount = view.findViewById(R.id.txtHalfDayCount)
        txtLateCount = view.findViewById(R.id.txtLateCount)

        fetchAdminName()
        setupUI()
        observeViewModel()

        return view
    }

    private fun showLoading(show: Boolean) {
        blurOverlay.visibility = if (show) View.VISIBLE else View.GONE
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setupUI() {
        txtDate.text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
    }

    private fun observeViewModel() {
        adminViewModel.organization.observe(viewLifecycleOwner) { orgName ->
            if (!orgName.isNullOrEmpty()) {
                txtOrganization.text = orgName
                fetchAttendanceData(orgName)
            } else {
                txtOrganization.text = "Organization Not Found"
                Toast.makeText(requireContext(), "Could not verify organization.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchAdminName() {
        val user = auth.currentUser ?: return
        showLoading(true)
        db.collection("admin").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("name") ?: "Admin"
                    txtWelcome.text = "Welcome, $name"
                }
                showLoading(false)
            }
            .addOnFailureListener {
                showLoading(false)
                txtWelcome.text = "Welcome"
            }
    }


    @SuppressLint("SetTextI18n")
    private fun fetchAttendanceData(organizationName: String) {
        showLoading(true)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("employee")
            .whereEqualTo("organization", organizationName)
            .get()
            .addOnSuccessListener { employeeSnapshot ->
                if (employeeSnapshot.isEmpty) {
                    txtTotalEmployee.text = "Total Employees: 0"
                    txtPresentCount.text = "0"
                    txtAbsentCount.text = "0"
                    txtHalfDayCount.text = "0"
                    txtLateCount.text = "0"
                    setupChart(0, 0, 0, 0)
                    return@addOnSuccessListener
                }

                val employeeIds = employeeSnapshot.documents.map { it.id }
                txtTotalEmployee.text = "Total Employees: ${employeeIds.size}"

                if (employeeIds.isEmpty()) {
                    setupChart(0,0,0,0)
                    return@addOnSuccessListener
                }

                val chunkedEmployeeIds = employeeIds.chunked(10)
                val tasks = mutableListOf<Task<QuerySnapshot>>()

                for (chunk in chunkedEmployeeIds) {
                    val task = db.collection("attendance")
                        .whereEqualTo("date", today)
                        .whereIn("employeeId", chunk)
                        .get()
                    tasks.add(task)
                }

                Tasks.whenAllSuccess<QuerySnapshot>(tasks)
                    .addOnSuccessListener { results ->
                        var present = 0; var late = 0; var halfDay = 0; var absent = 0

                        for (attendanceSnapshot in results) {
                            for (doc in attendanceSnapshot) {
                                when (doc.getString("status")) {
                                    "Present" -> present++
                                    "Late" -> late++
                                    "Half Day" -> halfDay++
                                    "Absent" -> absent++
                                }
                            }
                        }

                        txtPresentCount.text = present.toString()
                        txtAbsentCount.text = absent.toString()
                        txtHalfDayCount.text = halfDay.toString()
                        txtLateCount.text = late.toString()

                        setupChart(present, late, halfDay, absent)
                        showLoading(false)
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        Toast.makeText(requireContext(), "Error fetching attendance data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(requireContext(), "Error fetching employees: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupChart(present: Int, late: Int, halfDay: Int, absent: Int) {
        val entries = mutableListOf<PieEntry>()
        if (present > 0) entries.add(PieEntry(present.toFloat(), "Present"))
        if (late > 0) entries.add(PieEntry(late.toFloat(), "Late"))
        if (halfDay > 0) entries.add(PieEntry(halfDay.toFloat(), "Half Day"))
        if (absent > 0) entries.add(PieEntry(absent.toFloat(), "Absent"))

        if (entries.isEmpty()) {
            attendanceChart.clear()
            attendanceChart.centerText = "No Data"
            attendanceChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "Attendance")
        dataSet.colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.Present),
            ContextCompat.getColor(requireContext(), R.color.Late),
            ContextCompat.getColor(requireContext(), R.color.HalfDay),
            ContextCompat.getColor(requireContext(), R.color.Absent)
        )
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        attendanceChart.data = PieData(dataSet)
        attendanceChart.description.isEnabled = false
        attendanceChart.isDrawHoleEnabled = true
        attendanceChart.setEntryLabelColor(Color.BLACK)
        attendanceChart.animateY(1000)
        attendanceChart.invalidate()
    }
}

