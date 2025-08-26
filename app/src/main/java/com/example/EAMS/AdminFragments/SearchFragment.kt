package com.example.EAMS.AdminFragments

import android.os.Bundle
import android.text.*
import android.util.Log
import android.view.*
import android.widget.*
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
            onEdit = { employee ->
                Log.d("SearchFragment", "Edit: ${employee.name}")
            },
            onStatusChange = { employee ->
                Log.d("SearchFragment", "Toggle status: ${employee.name}")
            }
        )
        rvEmployees.adapter = employeeAdapter

        loadEmployees()

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

    private fun loadEmployees() {
        db.collection("employee")
            .get()
            .addOnSuccessListener { result ->
                val list = ArrayList<Employee>()
                for (doc in result) {
                    val employee = doc.toObject(Employee::class.java)
                    list.add(employee)
                }
                fullEmployeeList = list
                employeeAdapter.updateList(list)
                showEmptyMessage("Search employees by name")
            }
            .addOnFailureListener { e ->
                Log.e("SearchFragment", "Error fetching employees", e)
                showEmptyMessage("Error loading employees")
            }
    }

    private fun filterEmployees(query: String) {
        if (query.isEmpty()) {
            rvEmployees.visibility = View.GONE
            showEmptyMessage("Search employees by name")
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
}
