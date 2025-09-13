package com.example.EAMS.AdminFragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.EAMS.R
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var txtWelcome: TextView
    private lateinit var txtOrganization: TextView
    private lateinit var txtDate: TextView
    private lateinit var txtTotalEmployees: TextView
    private lateinit var statsGrid: GridLayout
    private lateinit var attendanceChart: PieChart
    private lateinit var btnManageEmployees: MaterialButton
    private lateinit var btnGenerateReports: MaterialButton

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Match IDs with your XML
        txtWelcome = view.findViewById(R.id.txtWelcome)
        txtOrganization = view.findViewById(R.id.txtOrganization)
        txtDate = view.findViewById(R.id.txtDate)
        txtTotalEmployees = view.findViewById(R.id.txtTotalEmployee)
        statsGrid = view.findViewById(R.id.statsGrid)
        attendanceChart = view.findViewById(R.id.attendance_chart)
        btnManageEmployees = view.findViewById(R.id.btn_manage_employees)
        btnGenerateReports = view.findViewById(R.id.btn_generate_reports)

        setupUI()
        fetchAdminDetails()
        fetchAttendanceData()
        setupActions()

        return view
    }

    private fun setupUI() {
        txtDate.text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
    }

    private fun fetchAdminDetails() {
        val user = auth.currentUser ?: return
        db.collection("admin").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("name") ?: "Admin"
                    val organization = doc.getString("organization") ?: "Organization"
                    txtWelcome.text = "Welcome, $name"
                    txtOrganization.text = organization
                } else {
                    Toast.makeText(requireContext(),
                        "Admin record not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    "Failed to load admin info: ${it.localizedMessage}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("SetTextI18n")
    private fun fetchAttendanceData() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("attendance")
            .whereEqualTo("date", today)
            .get()
            .addOnSuccessListener { snapshot ->
                var present = 0; var late = 0; var halfDay = 0; var absent = 0
                val totalEmployees = snapshot.size()

                for (doc in snapshot) {
                    when (doc.getString("status")) {
                        "Present" -> present++
                        "Late" -> late++
                        "Half Day" -> halfDay++
                        "Absent" -> absent++
                    }
                }

                txtTotalEmployees.text = "Total Employees : $totalEmployees"
                statsGrid.removeAllViews()
                addStatCard("Present", present.toString())
                addStatCard("Late", late.toString())
                addStatCard("Half Day", halfDay.toString())
                addStatCard("Absent", absent.toString())

                setupChart(present, late, halfDay, absent)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addStatCard(title: String, value: String) {
        val card = layoutInflater.inflate(R.layout.item_stat_card, statsGrid, false)
        card.findViewById<TextView>(R.id.tv_stat_title).text = title
        card.findViewById<TextView>(R.id.tv_stat_value).text = value
        statsGrid.addView(card)
    }

    private fun setupChart(present: Int, late: Int, halfDay: Int, absent: Int) {
        val entries = mutableListOf<PieEntry>()
        if (present > 0) entries.add(PieEntry(present.toFloat(), "Present"))
        if (late > 0) entries.add(PieEntry(late.toFloat(), "Late"))
        if (halfDay > 0) entries.add(PieEntry(halfDay.toFloat(), "Half Day"))
        if (absent > 0) entries.add(PieEntry(absent.toFloat(), "Absent"))

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

    private fun setupActions() {
        btnManageEmployees.setOnClickListener {
            Toast.makeText(requireContext(), "Manage Employees clicked", Toast.LENGTH_SHORT).show()
        }
        btnGenerateReports.setOnClickListener {
            Toast.makeText(requireContext(), "Generate Reports clicked", Toast.LENGTH_SHORT).show()
        }
    }
}
