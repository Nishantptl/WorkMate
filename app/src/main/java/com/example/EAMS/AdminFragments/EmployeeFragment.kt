package com.example.EAMS.AdminFragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.EAMS.R
import com.google.firebase.firestore.FirebaseFirestore
import com.example.EAMS.Model.*
import com.example.EAMS.Adapters.*
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException


class EmployeeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var employeeAdapter: EmployeeAdapter
    private lateinit var employeeList: ArrayList<Employee>
    private lateinit var db: FirebaseFirestore
    private lateinit var btnAddEmployee: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_employee, container, false)

        recyclerView = view.findViewById(R.id.recyclerEmployees)
        btnAddEmployee = view.findViewById(R.id.btnAddEmployee)
        db = FirebaseFirestore.getInstance()

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        employeeList = arrayListOf()

        employeeAdapter = EmployeeAdapter(employeeList,
            onEdit = { employee -> showUpdateDialog(employee) },
            onStatusChange = { employee -> toggleEmployeeStatus(employee) })

        recyclerView.adapter = employeeAdapter
        btnAddEmployee.setOnClickListener {
            showAddDialog()
        }

        fetchEmployees()
        return view
    }

    private fun fetchEmployees() {
        FirebaseFirestore.getInstance()
            .collection("employee")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Firestore", "Error fetching employees", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    employeeList.clear()
                    for (doc in snapshot.documents) {
                        val employee = doc.toObject(Employee::class.java)
                        if (employee != null) {
                            employee.id = doc.id
                            employeeList.add(employee)
                        }
                    }
                    employeeAdapter.notifyDataSetChanged()
                    Log.d("Firestore", "Realtime update: ${employeeList.size} employees")
                } else {
                    employeeList.clear()
                    employeeAdapter.notifyDataSetChanged()
                    Log.d("Firestore", "No employee data found")
                }
            }
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_employee, null)

        val edtName = dialogView.findViewById<EditText>(R.id.edtName)
        val edtDepartment = dialogView.findViewById<EditText>(R.id.edtDepartment)
        val edtEmail = dialogView.findViewById<EditText>(R.id.edtEmail)
        val edtPassword = dialogView.findViewById<EditText>(R.id.edtPassword)
        val edtJoiningDate = dialogView.findViewById<LinearLayout>(R.id.layoutJoiningDate)
        val txtJoiningDate = dialogView.findViewById<TextView>(R.id.txtJoiningDate)

        // DatePicker for joining date
        edtJoiningDate.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

            val datePicker = android.app.DatePickerDialog(requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    txtJoiningDate.setText("$selectedDay-${selectedMonth + 1}-$selectedYear")
                }, year, month, day
            )
            datePicker.show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Employee")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = edtName.text.toString()
                val department = edtDepartment.text.toString()
                val email = edtEmail.text.toString()
                val password = edtPassword.text.toString()
                val joiningDate = txtJoiningDate.text.toString()
                val role = "EMPLOYEE"

                if (email.isNotEmpty() && password.isNotEmpty()) {
                    val auth = FirebaseAuth.getInstance()
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { result ->
                            val userId = result.user?.uid ?: ""
                            val userMap = hashMapOf(
                                "uid" to userId,
                                "name" to name,
                                "department" to department,
                                "email" to email,
                                "role" to role,
                                "joiningDate" to joiningDate
                            )
                            db.collection("employee")
                                .document(userId)
                                .set(userMap)
                            sendEmailToEmployee(name, email, password, department, joiningDate)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(),
                                "Auth error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendEmailToEmployee(
        name: String,
        email: String,
        password: String,
        department: String,
        joiningDate: String
    ) {
        val url = "https://api.sendgrid.com/v3/mail/send"
        val apiKey = requireContext().getString(R.string.api_key)

        // Build HTML email body
        val htmlContent = """
        <html>
          <body style='font-family: Arial, sans-serif; color:#333;'>
            <h2 style='color:#4CAF50;'>Welcome to Employee Attendance Management System 🎉</h2>
            <p>Hi <b>$name</b>,</p>
            <p>Your account has been created. Here are your details:</p>
            <table border='1' cellpadding='6' cellspacing='0'>
              <tr><td>Email</td><td>$email</td></tr>
              <tr><td>Password</td><td>$password</td></tr>
              <tr><td>Department</td><td>$department</td></tr>
              <tr><td>Joining Date</td><td>$joiningDate</td></tr>
            </table>
            <p><b>Please change your password after first login.</b></p>
            <p>Best regards,<br/>EAMS Admin</p>
          </body>
        </html>
    """.trimIndent()

        val json = org.json.JSONObject().apply {
            put("personalizations", org.json.JSONArray().put(
                org.json.JSONObject().apply {
                    put("to", org.json.JSONArray().put(org.json.JSONObject().put("email", email)))
                    put("subject", "Welcome to EAMS 🎉")
                }
            ))
            put("from", org.json.JSONObject().put("email", "nishantpatel2810@gmail.com")) // must be VERIFIED in SendGrid
            put("content", org.json.JSONArray().put(
                org.json.JSONObject().apply {
                    put("type", "text/html")
                    put("value", htmlContent)
                }
            ))
        }

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), json.toString()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SendGrid", "Error: ${e.message}", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.i("SendGrid", "Response: ${response.code} - ${response.message}")
                Log.i("SendGrid", "Response Body: $responseBody")
            }
        })
    }

    private fun showUpdateDialog(employee: Employee) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_employee, null)

        val edtName = dialogView.findViewById<EditText>(R.id.edtName)
        val edtDepartment = dialogView.findViewById<EditText>(R.id.edtDepartment)
        val edtEmail = dialogView.findViewById<EditText>(R.id.edtEmail)
        val edtJoiningDate = dialogView.findViewById<LinearLayout>(R.id.layoutJoiningDate)
        val txtJoiningDate = dialogView.findViewById<TextView>(R.id.txtJoiningDate)
        val edtPassword = dialogView.findViewById<EditText>(R.id.edtPassword)

        edtName.setText(employee.name)
        edtDepartment.setText(employee.department)
        edtEmail.setText(employee.email)
        txtJoiningDate.setText(employee.joiningDate)
        edtPassword.visibility = View.GONE

        edtJoiningDate.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

            val datePicker = android.app.DatePickerDialog(requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    txtJoiningDate.setText("$selectedDay-${selectedMonth + 1}-$selectedYear")
                }, year, month, day
            )
            datePicker.show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Update Employee")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val name = edtName.text.toString()
                val department = edtDepartment.text.toString()
                val email = edtEmail.text.toString()
                val joiningDate = txtJoiningDate.text.toString()
                    val updates = mapOf(
                        "name" to name,
                        "department" to department,
                        "email" to email,
                        "joiningDate" to joiningDate
                    )
                    db.collection("employee")
                        .document(employee.id)
                        .update(updates)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun toggleEmployeeStatus(employee: Employee) {
        val newStatus = if (employee.status == "active") "inactive" else "active"

        FirebaseFirestore.getInstance()
            .collection("employee")
            .document(employee.id)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
                fetchEmployees()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
