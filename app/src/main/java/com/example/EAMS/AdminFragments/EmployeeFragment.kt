package com.example.EAMS.AdminFragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.EAMS.R
import com.google.firebase.firestore.FirebaseFirestore
import com.example.EAMS.Model.*
import com.example.EAMS.Adapters.*
import com.google.firebase.auth.FirebaseAuth
import org.w3c.dom.Text

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
            onDelete = { employee -> deleteEmployee(employee.id) })

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
            .get()
            .addOnSuccessListener { snapshot ->
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
                    Log.d("Firestore", "Fetched employees: ${employeeList.size}")
                } else {
                    Log.d("Firestore", "No employee data found")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching employees", e)
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

    private fun showUpdateDialog(employee: Employee) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_employee, null)

        val edtName = dialogView.findViewById<EditText>(R.id.edtName)
        val edtDepartment = dialogView.findViewById<EditText>(R.id.edtDepartment)
        val edtEmail = dialogView.findViewById<EditText>(R.id.edtEmail)
        val edtJoiningDate = dialogView.findViewById<LinearLayout>(R.id.layoutJoiningDate)
        val txtJoiningDate = dialogView.findViewById<TextView>(R.id.txtJoiningDate)

        edtName.setText(employee.name)
        edtDepartment.setText(employee.department)
        edtEmail.setText(employee.email)
        txtJoiningDate.setText(employee.joiningDate)

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
                val updates = mapOf(
                    "name" to edtName.text.toString(),
                    "department" to edtDepartment.text.toString(),
                    "email" to edtEmail.text.toString(),
                    "joiningDate" to txtJoiningDate.text.toString()
                )
                db.collection("employee")
                    .document(employee.id)
                    .update(updates)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEmployee(employeeId: String) {
        db.collection("employee")
            .document(employeeId)
            .delete()
    }

}
