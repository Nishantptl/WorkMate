package com.example.workmate.Activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.workmate.Adapters.TaskAdapter
import com.example.workmate.Model.Task
import com.example.workmate.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Date

class ViewProjectActivity : AppCompatActivity() {

    private lateinit var tvProjectName: TextView
    private lateinit var tvProjectRequirements: TextView
    private lateinit var tvProjectDeadline: TextView
    private lateinit var tvTeamMembers: TextView
    private lateinit var tvNoTasks: TextView

    private lateinit var recyclerTasks: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private var taskList = mutableListOf<Task>()
    private var projectTeamMembersList = mutableListOf<EmployeeSpinnerItem>()

    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var toolbar: Toolbar
    private lateinit var imgBackButton: ImageButton

    private lateinit var db: FirebaseFirestore
    private var projectId: String? = null
    private var projectName: String? = null
    private var tasksListener: ListenerRegistration? = null
    private var addTaskDialog: AlertDialog? = null // To manage dialog instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_project)

        db = FirebaseFirestore.getInstance()
        projectId = intent.getStringExtra("PROJECT_ID")
        projectName = intent.getStringExtra("PROJECT_NAME")

        initializeViews()
        setupToolbar()
        setupRecyclerView()

        if (projectId != null) {
            fetchProjectDetails(projectId!!)
            listenForTasks(projectId!!)
        } else {
            handleProjectLoadError("Project ID not found")
        }

        fabAddTask.setOnClickListener {
            if (projectId != null) {
                showAddTaskDialog()
            } else {
                Toast.makeText(this, "Project ID not available to add task.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbarViewProject)
        imgBackButton = findViewById(R.id.imgBackProjectDetails)
        tvProjectName = findViewById(R.id.tvProjectName)
        tvProjectRequirements = findViewById(R.id.tvProjectRequirements)
        tvProjectDeadline = findViewById(R.id.tvProjectDeadline)
        tvTeamMembers = findViewById(R.id.tvTeamMembers)
        recyclerTasks = findViewById(R.id.recyclerTasks)
        tvNoTasks = findViewById(R.id.tvNoTasks)
        fabAddTask = findViewById(R.id.fabAddTask)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false) // Using custom title TextView
        imgBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed() // More robust than finish()
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(taskList)
        recyclerTasks.layoutManager = LinearLayoutManager(this)
        recyclerTasks.adapter = taskAdapter
        recyclerTasks.isNestedScrollingEnabled = false
    }

    private fun fetchProjectDetails(currentProjectId: String) {
        db.collection("projects").document(currentProjectId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    tvProjectName.text = document.getString("projectName") ?: "N/A"
                    tvProjectRequirements.text = document.getString("projectRequirements") ?: "N/A"
                    val deadline = document.getDate("projectDeadline")
                    val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    tvProjectDeadline.text = deadline?.let { dateFormat.format(it) } ?: "N/A"

                    val teamMemberIds = document.get("teamMembersId") as? List<String>
                    if (teamMemberIds != null && teamMemberIds.isNotEmpty()) {
                        fetchTeamMemberNames(teamMemberIds)
                    } else {
                        tvTeamMembers.text = "No members assigned"
                        projectTeamMembersList.clear() // Clear if no members
                    }
                } else {
                    handleProjectLoadError("Project details not found.")
                }
            }
            .addOnFailureListener { exception ->
                handleProjectLoadError("Error fetching project details: ${exception.message}", exception)
            }
    }

    @SuppressLint("SetTextI18n")
    private fun fetchTeamMemberNames(memberIds: List<String>) {
        val fetchedEmployeeItems = mutableListOf<EmployeeSpinnerItem>()
        val employeeDisplayNames = mutableListOf<String>()
        val totalMembers = memberIds.size
        var membersFetchedCount = 0
        tvTeamMembers.text = "Loading members..."
        projectTeamMembersList.clear() // Clear previous list

        if (memberIds.isEmpty()) {
            tvTeamMembers.text = "N/A"
            return
        }

        memberIds.forEach { employeeId ->
            db.collection("employee").document(employeeId).get()
                .addOnSuccessListener { employeeDoc ->
                    if (employeeDoc != null && employeeDoc.exists()) {
                        val name = employeeDoc.getString("name") ?: "Unknown Member"
                        fetchedEmployeeItems.add(EmployeeSpinnerItem(employeeId, name))
                        employeeDisplayNames.add(name)
                    } else {
                        fetchedEmployeeItems.add(EmployeeSpinnerItem(employeeId, "Unknown ($employeeId)"))
                        employeeDisplayNames.add("Unknown ($employeeId)")
                        Log.d("ViewProjectActivity", "No such employee: $employeeId")
                    }
                    membersFetchedCount++
                    if (membersFetchedCount == totalMembers) {
                        projectTeamMembersList.addAll(fetchedEmployeeItems.sortedBy { it.name })
                        tvTeamMembers.text = if (employeeDisplayNames.isNotEmpty()) employeeDisplayNames.joinToString(", ") else "N/A"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ViewProjectActivity", "Error fetching name for $employeeId", e)
                    employeeDisplayNames.add("Error") // Add error to display list
                    membersFetchedCount++
                    if (membersFetchedCount == totalMembers) {
                        tvTeamMembers.text = employeeDisplayNames.joinToString(", ")
                    }
                }
        }
    }

    private fun listenForTasks(currentProjectId: String) {
        tasksListener?.remove()
        tasksListener = db.collection("tasks")
            .whereEqualTo("projectId", currentProjectId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("ViewProjectActivity", "Error listening for tasks", error)
                    Toast.makeText(this, "Error loading tasks: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                val fetchedTasks = mutableListOf<Task>()
                snapshots?.forEach { document ->
                    val task = document.toObject(Task::class.java).apply { id = document.id }
                    fetchedTasks.add(task)
                }
                taskList.clear()
                taskList.addAll(fetchedTasks)
                taskAdapter.updateTasks(taskList)
                toggleNoTasksView()
            }
    }

    private fun toggleNoTasksView() {
        if (taskList.isEmpty()) {
            tvNoTasks.visibility = View.VISIBLE
            recyclerTasks.visibility = View.GONE
        } else {
            tvNoTasks.visibility = View.GONE
            recyclerTasks.visibility = View.VISIBLE
        }
    }

    data class EmployeeSpinnerItem(val id: String, val name: String) {
        override fun toString(): String = name // This is what will be shown in the Spinner
    }

    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val edtTaskName = dialogView.findViewById<EditText>(R.id.edtTaskName)
        val edtTaskDescription = dialogView.findViewById<EditText>(R.id.edtTaskDescription)
        val spinnerAssignTo = dialogView.findViewById<Spinner>(R.id.spinnerAssignTo)
        val spinnerTaskStatus = dialogView.findViewById<Spinner>(R.id.spinnerTaskStatus)
        val btnDialogCancelTask = dialogView.findViewById<Button>(R.id.btnDialogCancelTask)
        val btnDialogAddTask = dialogView.findViewById<Button>(R.id.btnDialogAddTask)

        // Populate Assign To spinner
        if (projectTeamMembersList.isEmpty()) {
            val noMembersList = listOf(EmployeeSpinnerItem("", "No team members available"))
            val assignToAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, noMembersList)
            spinnerAssignTo.adapter = assignToAdapter
            spinnerAssignTo.isEnabled = false
        } else {
            val assignToAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, projectTeamMembersList)
            assignToAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerAssignTo.adapter = assignToAdapter
            spinnerAssignTo.isEnabled = true
        }

        // Populate Status spinner
        val statusOptions = listOf("To Do", "In Progress", "Completed")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTaskStatus.adapter = statusAdapter

        addTaskDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnDialogCancelTask.setOnClickListener {
            addTaskDialog?.dismiss()
        }

        btnDialogAddTask.setOnClickListener { 
            val taskName = edtTaskName.text.toString().trim()
            val taskDescription = edtTaskDescription.text.toString().trim()

            if (taskName.isEmpty()) {
                edtTaskName.error = "Task name cannot be empty"
                return@setOnClickListener
            }
             if (taskDescription.isEmpty()) {
                 edtTaskDescription.error = "Task description cannot be empty"
                 return@setOnClickListener
             }

            val selectedEmployeeItem = spinnerAssignTo.selectedItem as? EmployeeSpinnerItem
            val assignedToId = selectedEmployeeItem?.id
            val assignedToName = selectedEmployeeItem?.name
            val taskStatus = spinnerTaskStatus.selectedItem.toString()

            if (projectId == null) {
                Toast.makeText(this, "Error: Project ID is missing.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (projectTeamMembersList.isNotEmpty() && (assignedToId == null || assignedToId.isEmpty())){
                 Toast.makeText(this, "Please select a team member to assign the task.", Toast.LENGTH_SHORT).show()
                 return@setOnClickListener
            }

            val newTaskId = db.collection("tasks").document().id // Generate ID client-side
            val newTask = Task(
                id = newTaskId, 
                projectId = projectId!!,
                projectName = projectName!!,
                taskName = taskName,
                taskDescription = taskDescription,
                assignedToId = if(projectTeamMembersList.isEmpty() || assignedToId.isNullOrEmpty()) null else assignedToId, 
                assignedToName = if(projectTeamMembersList.isEmpty() || assignedToId.isNullOrEmpty()) null else assignedToName, 
                status = taskStatus,
                createdAt = Date()
            )

            db.collection("tasks").document(newTaskId).set(newTask)
                .addOnSuccessListener {
                    Toast.makeText(this, "Task added successfully", Toast.LENGTH_SHORT).show()
                    addTaskDialog?.dismiss()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error adding task: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ViewProjectActivity", "Error adding task", e)
                }
        }
        addTaskDialog?.show()
    }


    private fun handleProjectLoadError(message: String, exception: Exception? = null) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e("ViewProjectActivity", message, exception)
        tvProjectName.text = "Error"
        tvProjectRequirements.text = "Error"
        tvProjectDeadline.text = "Error"
        tvTeamMembers.text = "Error"
        tvNoTasks.text = "Could not load project tasks"
        tvNoTasks.visibility = View.VISIBLE
        recyclerTasks.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        tasksListener?.remove()
        addTaskDialog?.dismiss() // Dismiss dialog to prevent leaks
    }
}
