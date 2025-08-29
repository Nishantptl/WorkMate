package com.example.EAMS.AdminFragments

import android.os.Bundle
import android.text.*
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import com.example.EAMS.Adapters.EmployeeAdapter
import com.example.EAMS.Model.Employee
import com.example.EAMS.R
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment() {

    private lateinit var edtSearch: EditText
    private lateinit var rvEmployees: RecyclerView
    private lateinit var txtEmptyState: TextView
    private lateinit var employeeAdapter: EmployeeAdapter
    private val db = FirebaseFirestore.getInstance()
    private var fullEmployeeList = ArrayList<Employee>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        edtSearch = view.findViewById(R.id.edtSearchEmployee)
        rvEmployees = view.findViewById(R.id.rvEmployees)
        txtEmptyState = view.findViewById(R.id.txtEmptyState)

        rvEmployees.layoutManager = LinearLayoutManager(requireContext())
        employeeAdapter = EmployeeAdapter(arrayListOf(),
            onEdit = { employee -> showUpdateDialog(employee) },
            onStatusChange = { employee -> toggleEmployeeStatus(employee) }
        )
        rvEmployees.adapter = employeeAdapter

        fetchEmployees()

        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                filterEmployees(query)
            }
        })

        return view
    }

    private fun fetchEmployees() {
        db.collection("employee")
            .get()
            .addOnSuccessListener { result ->
                val list = ArrayList<Employee>()
                for (doc in result) {
                    val employee = doc.toObject(Employee::class.java)
                    employee.id = doc.id
                    list.add(employee)
                }
                fullEmployeeList = list

                if (list.isEmpty()) {
                    showEmptyMessage("No employees available")
                    rvEmployees.visibility = View.GONE
                } else {
                    rvEmployees.visibility = View.VISIBLE
                    txtEmptyState.visibility = View.GONE
                    employeeAdapter.updateList(list)
                }
            }
            .addOnFailureListener { e ->
                Log.e("SearchFragment", "Error fetching employees", e)
                showEmptyMessage("Error loading employees")
                rvEmployees.visibility = View.GONE
            }
    }

    private fun filterEmployees(query: String) {
        if (query.isEmpty()) {
            if (fullEmployeeList.isEmpty()) {
                showEmptyMessage("No employees available")
                rvEmployees.visibility = View.GONE
            } else {
                rvEmployees.visibility = View.VISIBLE
                txtEmptyState.visibility = View.GONE
                employeeAdapter.updateList(fullEmployeeList) // reset full list
            }
            return
        }

        val filtered = fullEmployeeList.filter {
            it.name.contains(query, ignoreCase = true)
        }

        if (filtered.isEmpty()) {
            rvEmployees.visibility = View.GONE
            showEmptyMessage("No employees found.\nTry using different letters.")
        } else {
            rvEmployees.visibility = View.VISIBLE
            txtEmptyState.visibility = View.GONE
            employeeAdapter.updateList(ArrayList(filtered))
        }
    }


    private fun showEmptyMessage(message: String) {
        txtEmptyState.text = message
        txtEmptyState.visibility = View.VISIBLE
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
                var name = edtName.text.toString()
                var department = edtDepartment.text.toString()
                var email = edtEmail.text.toString()
                var joiningDate = txtJoiningDate.text.toString()
                val updates = mapOf(
                    "name" to name,
                    "department" to department,
                    "email" to email,
                    "joiningDate" to joiningDate
                )
                db.collection("employee")
                    .document(employee.id)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Employee updated", Toast.LENGTH_SHORT).show()

                        // Update local list
                        val index = fullEmployeeList.indexOfFirst { it.id == employee.id }
                        if (index != -1) {
                            fullEmployeeList[index].apply {
                                name = updates["name"] as String
                                department = updates["department"] as String
                                email = updates["email"] as String
                                joiningDate = updates["joiningDate"] as String
                            }
                        }

                        // Reapply current filter so UI refreshes immediately
                        val query = edtSearch.text.toString().trim()
                        filterEmployees(query)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

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

                // Update local list
                val index = fullEmployeeList.indexOfFirst { it.id == employee.id }
                if (index != -1) {
                    fullEmployeeList[index].status = newStatus
                }

                // Reapply current filter
                val query = edtSearch.text.toString().trim()
                filterEmployees(query)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
