package com.example.workmate.AdminFragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.workmate.R
import com.example.workmate.Model.*
import com.example.workmate.Adapters.*
import com.example.workmate.Activities.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ProjectFragment : Fragment() {

    private lateinit var edtSearchProject: EditText
    private lateinit var txtEmptyState: TextView
    private lateinit var recyclerProjects: RecyclerView
    private lateinit var btnAddProject: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private lateinit var blurOverlay: View

    private val db = FirebaseFirestore.getInstance()
    private var firestoreListener: ListenerRegistration? = null
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    private var projectList = ArrayList<Project>()
    private lateinit var projectAdapter: ProjectAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_project, container, false)

        edtSearchProject = view.findViewById(R.id.edtSearchProject)
        txtEmptyState = view.findViewById(R.id.txtEmptyState)
        recyclerProjects = view.findViewById(R.id.recyclerProjects)
        btnAddProject = view.findViewById(R.id.btnAddProject)
        progressBar = view.findViewById(R.id.progressBar)
        blurOverlay = view.findViewById(R.id.blurOverlay)

        fetchOrg()
        setupRecyclerView()
        setupListeners()

        return view
    }

    private fun fetchOrg() {
        showLoading(true)
        db.collection("admin").document(uid!!)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val orgName = doc.getString("organization")
                    if (orgName != null) fetchProjects(orgName)
                }
            }
    }

    private fun fetchProjects(organizationName: String) {
        showLoading(true)
        firestoreListener = db.collection("projects") // Assign to firestoreListener
            .whereEqualTo("organization", organizationName)
            .addSnapshotListener { snapshots, error ->
                showLoading(false) // Hide loading indicator once data is fetched or error occurs
                if (error != null) {
                    Log.e("ProjectFragment", "Error fetching projects", error)
                    return@addSnapshotListener
                }
                val fetchedProjects = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(Project::class.java)?.apply { id = doc.id }
                }
                projectList = ArrayList(fetchedProjects ?: emptyList())
                projectAdapter.updateList(projectList)
                toggleEmptyState()
            }
    }

    private fun setupRecyclerView() {
        recyclerProjects.layoutManager = LinearLayoutManager(requireContext())
        projectAdapter = ProjectAdapter(arrayListOf(),
            onView = { project -> viewProject(project) },
            onStatusChange = { project -> toggleProjectStatus(project) }
        )
        recyclerProjects.adapter = projectAdapter
    }

    private fun setupListeners() {
        btnAddProject.setOnClickListener { addNewProject() }
        edtSearchProject.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterProjects(s.toString().trim())
            }
        })
    }

    private fun filterProjects(query: String) {
        val filteredList = if (query.isEmpty()) {
            projectList
        } else {
            projectList.filter { it.projectName.contains(query, ignoreCase = true) }
        }
        projectAdapter.updateList(ArrayList(filteredList))
        toggleEmptyState()
    }

    private fun toggleEmptyState() {
        if (projectAdapter.itemCount == 0) {
            recyclerProjects.visibility = View.GONE
            txtEmptyState.visibility = View.VISIBLE
        } else {
            recyclerProjects.visibility = View.VISIBLE
            txtEmptyState.visibility = View.GONE
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        blurOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun viewProject(project: Project) {
        val intent = Intent(requireContext(), ViewProjectActivity::class.java)
        intent.putExtra("PROJECT_ID", project.id)
        intent.putExtra("PROJECT_NAME", project.projectName) // Ensure your Project model has projectName
        startActivity(intent)
    }

    private fun addNewProject() {
        val intent = Intent(requireContext(), AddProjectActivity::class.java)
        startActivity(intent)
    }

    private fun toggleProjectStatus(project: Project) {
        val newStatus = if (project.projectStatus == "active") "inactive" else "active"
        db.collection("projects").document(project.id).update("projectStatus", newStatus)
            .addOnSuccessListener {
                // Update local list and notify adapter for immediate UI update
                val index = projectList.indexOfFirst { it.id == project.id }
                if (index != -1) {
                    projectList[index].projectStatus = newStatus
                    projectAdapter.notifyItemChanged(index) // More efficient than notifyDataSetChanged()
                }
                Toast.makeText(requireContext(), "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error updating status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove() // Remove listener to prevent memory leaks
        // If using ViewBinding, set binding to null: _binding = null
    }
}
