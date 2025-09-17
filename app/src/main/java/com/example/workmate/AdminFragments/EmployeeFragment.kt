package com.example.workmate.AdminFragments

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.workmate.R
import com.google.firebase.firestore.FirebaseFirestore
import com.example.workmate.Model.*
import com.example.workmate.Adapters.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.example.workmate.BuildConfig
import com.example.workmate.ViewModels.AdminViewModel
import com.google.firebase.firestore.ListenerRegistration
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException
import java.util.Calendar

class EmployeeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var employeeAdapter: AddEmployeeAdapter
    private lateinit var employeeList: ArrayList<Employee>
    private lateinit var db: FirebaseFirestore
    private lateinit var btnAddEmployee: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private lateinit var blurOverlay: View

    private val adminViewModel: AdminViewModel by activityViewModels()
    private var firestoreListener: ListenerRegistration? = null
    private var apiKey: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_employee, container, false)
        apiKey = BuildConfig.SENDGRID_API_KEY

        recyclerView = view.findViewById(R.id.recyclerEmployees)
        btnAddEmployee = view.findViewById(R.id.btnAddEmployee)
        progressBar = view.findViewById(R.id.progressBar)
        blurOverlay = view.findViewById(R.id.blurOverlay)
        db = FirebaseFirestore.getInstance()

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        employeeList = arrayListOf()
        employeeAdapter = AddEmployeeAdapter(employeeList)
        recyclerView.adapter = employeeAdapter

        btnAddEmployee.setOnClickListener { showAddDialog() }
        observeViewModel()

        return view
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        blurOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun observeViewModel() {
        adminViewModel.organization.observe(viewLifecycleOwner) { orgName ->
            if (!orgName.isNullOrEmpty()) {
                fetchEmployees(orgName)
            } else {
                showLoading(false)
                Toast.makeText(requireContext(), "Organization not found.", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchEmployees(organizationName: String) { // 👈 CHANGED: Takes orgName as parameter
        showLoading(true)
        // Remove previous listener to avoid duplicates
        firestoreListener?.remove()

        firestoreListener = db.collection("employee")
            .whereEqualTo("organization", organizationName)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    showLoading(false)
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
                } else {
                    employeeList.clear()
                    employeeAdapter.notifyDataSetChanged()
                    Log.d("Firestore", "No employee data found")
                }
                showLoading(false)
            }
    }

    @SuppressLint("SetTextI18n")
    private fun showAddDialog() {

        val currentOrganization = adminViewModel.organization.value
        if (currentOrganization.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Organization not loaded, cannot add employee.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_employee, null)

        val edtName = dialogView.findViewById<EditText>(R.id.edtName)
        val edtDepartment = dialogView.findViewById<EditText>(R.id.edtDepartment)
        val edtEmail = dialogView.findViewById<EditText>(R.id.edtEmail)
        val edtPassword = dialogView.findViewById<EditText>(R.id.edtPassword)
        val layoutJoiningDate = dialogView.findViewById<View>(R.id.layoutJoiningDate)
        val txtJoiningDate = dialogView.findViewById<TextView>(R.id.txtJoiningDate)
        val ivPasswordToggle = dialogView.findViewById<ImageView>(R.id.ivPasswordToggle)

        var isPasswordVisible = false
        ivPasswordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                edtPassword.transformationMethod = null // Show password
                ivPasswordToggle.setImageResource(R.drawable.ic_eye)
            } else {
                edtPassword.transformationMethod = PasswordTransformationMethod.getInstance() // Hide password
                ivPasswordToggle.setImageResource(R.drawable.ic_eye_off)
            }
            edtPassword.setSelection(edtPassword.text.length) // Move cursor to end
        }

        layoutJoiningDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    txtJoiningDate.text = "$selectedDay-${selectedMonth + 1}-$selectedYear"
                }, year, month, day
            )
            datePicker.show()
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = edtName.text.toString()
                val department = edtDepartment.text.toString()
                val email = edtEmail.text.toString()
                val password = edtPassword.text.toString()
                val joiningDate = txtJoiningDate.text.toString()
                val role = "EMPLOYEE"
                val status = "active"

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
                                "joiningDate" to joiningDate,
                                "status" to status,
                                "organization" to currentOrganization
                            )
                            db.collection("employee")
                                .document(userId)
                                .set(userMap)
                            sendEmailToEmployee(name, email, password, department, joiningDate, currentOrganization)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(),
                                "Auth error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                }
                else {
                    Toast.makeText(requireContext(),
                        "Email and password are required", Toast.LENGTH_SHORT).show()
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
        joiningDate: String,
        adminOrganization: String?
    ) {
        val url = "https://api.sendgrid.com/v3/mail/send"

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
              <tr><td>Organization</td><td>$adminOrganization</td></tr>
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
                val responseBody = response.body.string()
                Log.i("SendGrid", "Response: ${response.code} - ${response.message}")
                Log.i("SendGrid", "Response Body: $responseBody")
            }
        })
    }
}
