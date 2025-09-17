package com.example.workmate.AdminFragments

import android.os.Bundle
import android.text.*
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class SearchFragment : Fragment() {

    private lateinit var edtSearch: EditText
    private lateinit var rvEmployees: RecyclerView
    private lateinit var txtEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var blurOverlay: View
    private lateinit var employeeAdapter: EmployeeAdapter

    private val db = FirebaseFirestore.getInstance()
    private var fullEmployeeList = ArrayList<Employee>()
    private val adminViewModel: AdminViewModel by activityViewModels()
    private var employeeListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        edtSearch = view.findViewById(R.id.edtSearchEmployee)
        rvEmployees = view.findViewById(R.id.rvEmployees)
        txtEmptyState = view.findViewById(R.id.txtEmptyState)
        progressBar = view.findViewById(R.id.progressBar)
        blurOverlay = view.findViewById(R.id.blurOverlay)

        rvEmployees.layoutManager = LinearLayoutManager(requireContext())
        employeeAdapter = EmployeeAdapter(arrayListOf(),
            onEdit = { employee -> showUpdateDialog(employee) },
            onStatusChange = { employee -> toggleEmployeeStatus(employee) }
        )
        rvEmployees.adapter = employeeAdapter

        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterEmployees(s.toString().trim())
            }
        })
        observeViewModel()

        return view
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        blurOverlay.visibility = if (show) View.VISIBLE else View.GONE
        rvEmployees.alpha = if (show) 0.6f else 1.0f
    }

    private fun observeViewModel() {
        showLoading(true)
        adminViewModel.organization.observe(viewLifecycleOwner) { orgName ->
            if (!orgName.isNullOrEmpty()) {
                fetchEmployeesOfOrg(orgName)
            } else {
                showLoading(false)
                showEmptyMessage("Organization not found.")
            }
        }
    }

    // Replace your existing fetchEmployeesOfOrg function with this one

    private fun fetchEmployeesOfOrg(organization: String) {
        showLoading(true)
        // Clean up any old listener to prevent duplicates
        employeeListener?.remove()

        employeeListener = db.collection("employee")
            .whereEqualTo("organization", organization)
            .addSnapshotListener { snapshot, error ->
                showLoading(false)
                if (error != null) {
                    Log.e("SearchFragment", "Error listening for employees", error)
                    showEmptyMessage("Error loading employees")
                    return@addSnapshotListener
                }

                val list = ArrayList<Employee>()
                if (snapshot != null) {
                    for (doc in snapshot) {
                        val employee = doc.toObject(Employee::class.java)
                        employee.id = doc.id
                        list.add(employee)
                    }
                }

                fullEmployeeList = list
                // Re-apply the current search text to the new data
                filterEmployees(edtSearch.text.toString().trim())
            }
    }

    private fun filterEmployees(query: String) {
        val filtered = if (query.isEmpty()) fullEmployeeList
        else fullEmployeeList.filter { it.name.contains(query, ignoreCase = true) }

        if (filtered.isEmpty()) {
            rvEmployees.visibility = View.GONE
            showEmptyMessage("No employees found.")
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
        val edtPassword = dialogView.findViewById<EditText>(R.id.edtPassword)
        val layoutJoiningDate = dialogView.findViewById<LinearLayout>(R.id.layoutJoiningDate)

        edtName.setText(employee.name)
        edtDepartment.setText(employee.department)
        edtEmail.setText(employee.email)

        edtPassword.visibility = View.GONE
        layoutJoiningDate.visibility = View.GONE

        AlertDialog.Builder(requireContext())
            .setTitle("Update Employee")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val updates = mapOf(
                    "name" to edtName.text.toString(),
                    "department" to edtDepartment.text.toString(),
                    "email" to edtEmail.text.toString()
                )

                db.collection("employee")
                    .document(employee.id)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Employee updated", Toast.LENGTH_SHORT).show()
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

        db.collection("employee")
            .document(employee.id)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        employeeListener?.remove()
    }
}
