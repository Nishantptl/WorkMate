package com.example.EAMS.AdminFragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.EAMS.R
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTotalEmployees: TextView
    private lateinit var statsGrid: GridLayout
    private lateinit var attendanceChart: PieChart
    private lateinit var btnManageEmployees: MaterialButton
    private lateinit var btnGenerateReports: MaterialButton

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        tvWelcome = view.findViewById(R.id.tv_welcome)
        tvDate = view.findViewById(R.id.tv_date)
        tvTotalEmployees = view.findViewById(R.id.txtTotalEmployee)
        statsGrid = view.findViewById(R.id.statsGrid)
        attendanceChart = view.findViewById(R.id.attendance_chart)
        btnManageEmployees = view.findViewById(R.id.btn_manage_employees)
        btnGenerateReports = view.findViewById(R.id.btn_generate_reports)

        setupUI()
        fetchAttendanceData()
        setupActions()

        return view
    }

    private fun setupUI() {
        tvWelcome.text = "Welcome, Admin"
        tvDate.text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
    }

    @SuppressLint("SetTextI18n")
    private fun fetchAttendanceData() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("attendance")
            .whereEqualTo("date", today)
            .get()
            .addOnSuccessListener { snapshot ->
                var presentCount = 0
                var lateCount = 0
                var halfDayCount = 0
                var absentCount = 0
                val totalEmployees = snapshot.size()

                for (doc in snapshot) {
                    when (doc.getString("status")) {
                        "Present" -> presentCount++
                        "Late" -> lateCount++
                        "Half Day" -> halfDayCount++
                        "Absent" -> absentCount++
                    }
                }

                // Update Stat Cards
                tvTotalEmployees.text = "Total Employees : $totalEmployees"
                statsGrid.removeAllViews()
                addStatCard("Present", presentCount.toString())
                addStatCard("Late", lateCount.toString())
                addStatCard("Half Day", halfDayCount.toString())
                addStatCard("Absent", absentCount.toString())

                // Update Pie Chart
                setupChart(presentCount, lateCount, halfDayCount, absentCount)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addStatCard(title: String, value: String) {
        val card = layoutInflater.inflate(R.layout.item_stat_card, statsGrid, false)
        val tvTitle = card.findViewById<TextView>(R.id.tv_stat_title)
        val tvValue = card.findViewById<TextView>(R.id.tv_stat_value)

        tvTitle.text = title
        tvValue.text = value

        statsGrid.addView(card)
    }

    private fun setupChart(present: Int, late: Int, halfDay: Int, absent: Int) {
        val entries = mutableListOf<PieEntry>()
        if (present > 0) entries.add(PieEntry(present.toFloat(), "Present"))
        if (late > 0) entries.add(PieEntry(late.toFloat(), "Late"))
        if (halfDay > 0) entries.add(PieEntry(halfDay.toFloat(), "Half Day"))
        if (absent > 0) entries.add(PieEntry(absent.toFloat(), "Absent"))

        val dataSet = PieDataSet(entries, "Attendance Status")
        dataSet.colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.Present),   // Green
            ContextCompat.getColor(requireContext(), R.color.Late),      // Amber
            ContextCompat.getColor(requireContext(), R.color.HalfDay),   // Blue
            ContextCompat.getColor(requireContext(), R.color.Absent)     // Red
        )

        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        val data = PieData(dataSet)

        attendanceChart.data = data
        attendanceChart.description.isEnabled = false
        attendanceChart.isDrawHoleEnabled = true
        attendanceChart.setEntryLabelColor(Color.BLACK)
        attendanceChart.animateY(1000)
        attendanceChart.invalidate()
    }

    private fun setupActions() {
        btnManageEmployees.setOnClickListener {
            Toast.makeText(requireContext(), "Manage Employees clicked", Toast.LENGTH_SHORT).show()
        }

        btnGenerateReports.setOnClickListener {
            Toast.makeText(requireContext(), "Generate Reports clicked", Toast.LENGTH_SHORT).show()
        }
    }
}
