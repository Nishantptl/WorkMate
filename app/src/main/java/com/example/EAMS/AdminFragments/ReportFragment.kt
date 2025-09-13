package com.example.EAMS.AdminFragments

import android.content.Intent
import android.os.Bundle
import android.text.*
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.EAMS.R
import com.example.EAMS.Adapters.ReportEmployeeAdapter
import com.example.EAMS.Model.ReportEmployee
import com.example.EAMS.Activities.ViewEmployeeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ReportFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReportEmployeeAdapter
    private lateinit var searchBox: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var blurOverlay: View

    private val employeeList = mutableListOf<ReportEmployee>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var organizationName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_report, container, false)

        recyclerView = view.findViewById(R.id.recyclerEmployees)
        searchBox = view.findViewById(R.id.edtSearchEmployee)
        progressBar = view.findViewById(R.id.progressBar)
        blurOverlay = view.findViewById(R.id.blurOverlay)

        adapter = ReportEmployeeAdapter(employeeList) { employee ->
            val intent = Intent(requireContext(), ViewEmployeeActivity::class.java)
            intent.putExtra("employeeId", employee.id)
            intent.putExtra("employeeName", employee.name)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        fetchAdminOrganization()

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString().trim())
            }
        })

        return view
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        blurOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun fetchAdminOrganization() {
        showLoading(true)
        val uid = auth.currentUser?.uid ?: return
        db.collection("admin").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                organizationName = doc.getString("organization")
                if (organizationName.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "Organization not found.", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                } else {
                    loadEmployees(organizationName!!)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to get organization.", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }

    private fun loadEmployees(organization: String) {
        showLoading(true)
        db.collection("employee")
            .whereEqualTo("organization", organization)
            .get()
            .addOnSuccessListener { result ->
                employeeList.clear()
                for (doc in result) {
                    val employee = doc.toObject(ReportEmployee::class.java)
                    employee.id = doc.id
                    employeeList.add(employee)
                }
                adapter.updateList(employeeList)
                showLoading(false)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error loading employees", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }

    private fun filterList(query: String) {
        val filtered = if (query.isEmpty()) employeeList
        else employeeList.filter { it.name.contains(query, ignoreCase = true) }
        adapter.updateList(filtered)
    }
}
