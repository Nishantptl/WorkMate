package com.example.EAMS.Activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.*
import com.example.EAMS.R
import com.example.EAMS.Adapters.*
import com.example.EAMS.Model.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ViewEmployeeActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    private lateinit var txtName: TextView
    private lateinit var txtDepartment: TextView
    private lateinit var txtJoiningDate: TextView
    private lateinit var txtEmail: TextView
    private lateinit var recyclerAttendance: RecyclerView
    private lateinit var attendanceAdapter: AttendanceAdapter
    private val attendanceList = mutableListOf<AttendanceRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_employee)

        db = FirebaseFirestore.getInstance()

        // UI refs
        txtName = findViewById(R.id.txtEmployeeName)
        txtDepartment = findViewById(R.id.txtEmployeeDepartment)
        txtJoiningDate = findViewById(R.id.txtEmployeeJoiningDate)
        txtEmail = findViewById(R.id.txtEmployeeEmail)
        recyclerAttendance = findViewById(R.id.recyclerAttendance)

        attendanceAdapter = AttendanceAdapter(attendanceList)
        recyclerAttendance.layoutManager = LinearLayoutManager(this)
        recyclerAttendance.adapter = attendanceAdapter

        // Get ID from intent
        val employeeId = intent.getStringExtra("employeeId")

        if (employeeId != null) {
            fetchEmployeeDetails(employeeId)
            fetchAttendanceHistory(employeeId)
        } else {
            Toast.makeText(this, "Employee ID not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fetchEmployeeDetails(employeeId: String) {
        db.collection("employee").document(employeeId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    txtName.text = ("Name : " + document.getString("name"))
                    txtDepartment.text =
                        ("Department : " + document.getString("department"))
                    txtJoiningDate.text =
                        ("Joining Date : " + document.getString("joiningDate"))
                    txtEmail.text = ("Email : " + document.getString("email"))
                } else {
                    Toast.makeText(this, "No such employee", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchAttendanceHistory(employeeId: String) {
        db.collection("attendance")
            .whereEqualTo("employeeId", employeeId)
            .get()
            .addOnSuccessListener { result ->
                attendanceList.clear()
                for (document in result) {
                    val record = document.toObject(AttendanceRecord::class.java)
                    attendanceList.add(record)
                }
                attendanceAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching attendance", Toast.LENGTH_SHORT).show()
            }
    }
}
