package com.example.workmate.AdminFragments

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.*
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.*
import com.example.workmate.Adapters.EmployeeAdapter
import com.example.workmate.Model.*
import com.example.workmate.R
import com.example.workmate.ViewModels.AdminViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.example.workmate.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException
import java.util.Calendar

class EmployeeFragment : Fragment() {

    private lateinit var edtSearch: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAddEmployee: FloatingActionButton
    private lateinit var txtEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var blurOverlay: View

    private val db = FirebaseFirestore.getInstance()
    private val adminViewModel: AdminViewModel by activityViewModels()
    private var firestoreListener: ListenerRegistration? = null

    private lateinit var employeeAdapter: EmployeeAdapter
    // Use one list as the "source of truth"
    private var fullEmployeeList = ArrayList<Employee>()

    private var apiKey: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_employee, container, false)
        apiKey = BuildConfig.SENDGRID_API_KEY

        // Initialize Views
        edtSearch = view.findViewById(R.id.edtSearchEmployee)
        recyclerView = view.findViewById(R.id.recyclerEmployees)
        btnAddEmployee = view.findViewById(R.id.btnAddEmployee)
        txtEmptyState = view.findViewById(R.id.txtEmptyState)
        progressBar = view.findViewById(R.id.progressBar)
        blurOverlay = view.findViewById(R.id.blurOverlay)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        return view
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        employeeAdapter = EmployeeAdapter(arrayListOf(),
            onEdit = { employee -> showUpdateDialog(employee) },
            onStatusChange = { employee -> toggleEmployeeStatus(employee) }
        )
        recyclerView.adapter = employeeAdapter
    }

    private fun setupListeners() {
        btnAddEmployee.setOnClickListener { showAddDialog() }

        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterEmployees(s.toString().trim())
            }
        })
    }

    private fun observeViewModel() {
        adminViewModel.organization.observe(viewLifecycleOwner) { orgName ->
            if (!orgName.isNullOrEmpty()) {
                fetchEmployees(orgName)
            } else {
                showLoading(false)
                showEmptyMessage("Organization not found.")
            }
        }
    }

    // --- IMPROVED DATA HANDLING ---

    private fun fetchEmployees(organizationName: String) {
        showLoading(true)
        firestoreListener?.remove()

        firestoreListener = db.collection("employee")
            .whereEqualTo("organization", organizationName)
            .addSnapshotListener { snapshot, e ->
                showLoading(false)
                if (e != null) {
                    Log.e("Firestore", "Error fetching employees", e)
                    showEmptyMessage("Error fetching employees")
                    return@addSnapshotListener
                }

                val list = ArrayList<Employee>()
                snapshot?.documents?.forEach { doc ->
                    doc.toObject(Employee::class.java)?.let { employee ->
                        employee.id = doc.id
                        list.add(employee)
                    }
                }

                // 1. Update the main list
                fullEmployeeList = list
                // 2. Apply the current filter to the new data
                filterEmployees(edtSearch.text.toString().trim())
            }
    }

    private fun filterEmployees(query: String) {
        val filteredList = if (query.isEmpty()) {
            fullEmployeeList
        } else {
            fullEmployeeList.filter { it.name.contains(query, ignoreCase = true) }
        }

        if (filteredList.isEmpty()) {
            recyclerView.visibility = View.GONE
            showEmptyMessage("No employees found.")
        } else {
            recyclerView.visibility = View.VISIBLE
            txtEmptyState.visibility = View.GONE
            employeeAdapter.updateList(ArrayList(filteredList))
        }
    }

    // --- UI and Dialog Functions (No changes below this line) ---

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        blurOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmptyMessage(message: String) {
        txtEmptyState.text = message
        txtEmptyState.visibility = View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    private fun showAddDialog() {
        val currentOrganization = adminViewModel.organization.value
        if (currentOrganization.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Organization not loaded.", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_employee, null)
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
                edtPassword.transformationMethod = null
                ivPasswordToggle.setImageResource(R.drawable.ic_eye)
            } else {
                edtPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                ivPasswordToggle.setImageResource(R.drawable.ic_eye_off)
            }
            edtPassword.setSelection(edtPassword.text.length)
        }
        layoutJoiningDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day -> txtJoiningDate.text = "$day-${month + 1}-$year" },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = edtName.text.toString().trim()
                val department = edtDepartment.text.toString().trim()
                val email = edtEmail.text.toString().trim()
                val password = edtPassword.text.toString().trim()
                val joiningDate = txtJoiningDate.text.toString()
                if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty() && department.isNotEmpty()) {
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { result ->
                            val userId = result.user?.uid ?: ""
                            val userMap = hashMapOf(
                                "uid" to userId, "name" to name, "department" to department,
                                "email" to email, "role" to "EMPLOYEE", "joiningDate" to joiningDate,
                                "status" to "active", "organization" to currentOrganization
                            )
                            db.collection("employee").document(userId).set(userMap)
                            sendEmailToEmployee(name, email, password, department, joiningDate, currentOrganization)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Auth error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendEmailToEmployee(name: String, email: String, password: String, department: String, joiningDate: String, adminOrganization: String?) {
        val htmlContent = """
        <html><body style='font-family: Arial, sans-serif; color:#333;'>
            <h2 style='color:#4CAF50;'>Welcome to EAMS 🎉</h2><p>Hi <b>$name</b>,</p>
            <p>Your account has been created. Here are your details:</p>
            <table border='1' cellpadding='6' cellspacing='0'>
              <tr><td>Email</td><td>$email</td></tr><tr><td>Password</td><td>$password</td></tr>
              <tr><td>Organization</td><td>$adminOrganization</td></tr><tr><td>Department</td><td>$department</td></tr>
              <tr><td>Joining Date</td><td>$joiningDate</td></tr>
            </table><p><b>Please change your password after first login.</b></p>
            <p>Best regards,<br/>EAMS Admin</p></body></html>
        """.trimIndent()

        val json = org.json.JSONObject().apply {
            put("personalizations", org.json.JSONArray().put(
                org.json.JSONObject().apply {
                    put("to", org.json.JSONArray().put(org.json.JSONObject().put("email", email)))
                    put("subject", "Welcome to EAMS 🎉")
                }))
            put("from", org.json.JSONObject().put("email", "nishantpatel2810@gmail.com"))
            put("content", org.json.JSONArray().put(
                org.json.JSONObject().apply {
                    put("type", "text/html")
                    put("value", htmlContent)
                }))
        }
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.sendgrid.com/v3/mail/send")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), json.toString()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SendGrid", "Error: ${e.message}", e)
            }
            override fun onResponse(call: Call, response: Response) {
                Log.i("SendGrid", "Response: ${response.code} - ${response.message}")
                Log.i("SendGrid", "Response Body: ${response.body.string()}")
                response.close()
            }
        })
    }

    private fun showUpdateDialog(employee: Employee) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_employee, null)
        val edtName = dialogView.findViewById<EditText>(R.id.edtName)
        val edtDepartment = dialogView.findViewById<EditText>(R.id.edtDepartment)
        val edtEmail = dialogView.findViewById<EditText>(R.id.edtEmail)

        edtName.setText(employee.name)
        edtDepartment.setText(employee.department)
        edtEmail.setText(employee.email)
        edtEmail.isEnabled = false // Email should not be editable

        dialogView.findViewById<EditText>(R.id.edtPassword).visibility = View.GONE
        dialogView.findViewById<LinearLayout>(R.id.layoutJoiningDate).visibility = View.GONE

        AlertDialog.Builder(requireContext())
            .setTitle("Update Employee")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val updates = mapOf(
                    "name" to edtName.text.toString().trim(),
                    "department" to edtDepartment.text.toString().trim()
                )
                db.collection("employee").document(employee.id).update(updates)
                    .addOnSuccessListener { Toast.makeText(requireContext(), "Employee updated", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { e -> Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleEmployeeStatus(employee: Employee) {
        val newStatus = if (employee.status == "active") "inactive" else "active"
        db.collection("employee").document(employee.id).update("status", newStatus)
            .addOnSuccessListener { Toast.makeText(requireContext(), "Status updated to $newStatus", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
    }
}