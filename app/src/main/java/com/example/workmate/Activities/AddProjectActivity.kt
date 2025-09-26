package com.example.workmate.Activities

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.workmate.Adapters.EmployeeSelectAdapter
import com.example.workmate.Model.Employee
import com.example.workmate.Model.Project
import com.example.workmate.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddProjectActivity : AppCompatActivity() {

    private lateinit var edtProjectName: EditText
    private lateinit var edtProjectRequirement: EditText
    private lateinit var layoutProjectDeadline: LinearLayout
    private lateinit var txtProjectDeadline: TextView
    private lateinit var imgDown: ImageView
    private lateinit var txtSelectedEmployees: TextView
    private lateinit var recyclerEmployees: RecyclerView
    private lateinit var btnSaveProject: FloatingActionButton
    private lateinit var header: LinearLayout
    private lateinit var content: LinearLayout

    private lateinit var employeeAdapter: EmployeeSelectAdapter
    private var allEmployeesList = mutableListOf<Employee>()
    private var selectedDeadline: Calendar = Calendar.getInstance()

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private var orgName: String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_project)

        edtProjectName = findViewById(R.id.edtProjectName)
        edtProjectRequirement = findViewById(R.id.edtProjectRequirement)
        layoutProjectDeadline = findViewById(R.id.layoutProjectDeadline)
        txtProjectDeadline = findViewById(R.id.txtProjectDeadline)
        header = findViewById(R.id.layoutSelectMembers)
        content = findViewById(R.id.layoutMembersContent)
        imgDown = findViewById(R.id.imgDown)
        txtSelectedEmployees = findViewById(R.id.txtSelectedEmployees)
        recyclerEmployees = findViewById(R.id.recyclerEmployees)
        btnSaveProject = findViewById(R.id.btnSaveProject)

        setupRecyclerView()
        setupClickListeners()
        fetchOrganization()

        header.setOnClickListener { toggleEmployeeListVisibility() }
        imgDown.setOnClickListener { toggleEmployeeListVisibility() }

        recyclerEmployees.visibility = View.GONE
        txtSelectedEmployees.visibility = View.GONE
        txtSelectedEmployees.text = "Selected: None"
    }

    @SuppressLint("SetTextI18n")
    private fun setupRecyclerView() {
        employeeAdapter = EmployeeSelectAdapter(allEmployeesList) { selectedList ->
            txtSelectedEmployees.text =
                if (selectedList.isEmpty()) "Selected: None"
                else "Selected: ${selectedList.joinToString { it.name }}"
        }
        recyclerEmployees.apply {
            layoutManager = LinearLayoutManager(this@AddProjectActivity)
            adapter = employeeAdapter
        }
    }

    private fun setupClickListeners() {
        layoutProjectDeadline.setOnClickListener { showDatePicker() }
        btnSaveProject.setOnClickListener { saveProject() }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDeadline.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                txtProjectDeadline.text = dateFormat.format(selectedDeadline.time)
            },
            selectedDeadline.get(Calendar.YEAR),
            selectedDeadline.get(Calendar.MONTH),
            selectedDeadline.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    private fun toggleEmployeeListVisibility() {
        if (content.isVisible) {
            content.visibility = View.GONE
            recyclerEmployees.visibility = View.GONE
            txtSelectedEmployees.visibility = View.GONE
            imgDown.animate().rotation(0f).start()
        } else {
            content.visibility = View.VISIBLE
            recyclerEmployees.visibility = View.VISIBLE
            txtSelectedEmployees.visibility = View.VISIBLE
            imgDown.animate().rotation(180f).start()
        }
    }

    private fun fetchOrganization() {
        db.collection("admin").document(uid!!).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    orgName = doc.getString("organization")
                    if (orgName != null) fetchEmployeesForOrganization(orgName!!)
                }
            }
    }

    private fun fetchEmployeesForOrganization(organizationName: String) {
        db.collection("employee")
            .whereEqualTo("organization", organizationName)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    allEmployeesList.clear()
                    for (doc in documents) {
                        val employee = doc.toObject(Employee::class.java).apply { id = doc.id }
                        allEmployeesList.add(employee)
                    }
                    employeeAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "No employees found for organization", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching employees: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProject() {
        val projectName = edtProjectName.text.toString().trim()
        val requirements = edtProjectRequirement.text.toString().trim()
        val selectedEmployees = employeeAdapter.getSelectedItems()

        if (projectName.isEmpty()) {
            edtProjectName.error = "Project name is required"
            return
        }
        if (txtProjectDeadline.text.isEmpty() || txtProjectDeadline.text.startsWith("Select")) {
            Toast.makeText(this, "Please select a deadline", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedEmployees.isEmpty()) {
            Toast.makeText(this, "Please select at least one team member", Toast.LENGTH_SHORT).show()
            return
        }

        val newProject = Project(
            projectName = projectName,
            projectRequirements = requirements,
            projectDeadline = Timestamp(selectedDeadline.time),
            teamMembersId = selectedEmployees.map { it.id },
            projectStatus = "active",
            projectManager = uid ?: "",
            organization = orgName.toString()
        )

        db.collection("projects").add(newProject)
            .addOnSuccessListener { documentReference ->
                val newId = documentReference.id
                db.collection("projects").document(newId)
                    .update("id", newId)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Project added successfully!", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Project created, but failed to save ID: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving project: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
