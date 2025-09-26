package com.example.workmate.EmployeeFragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.workmate.Model.*
import com.example.workmate.R
import com.example.workmate.ViewModels.EmployeeViewModel
import com.example.workmate.Adapters.EmployeeTaskAdapter

class HomeFragment : Fragment(), EmployeeTaskAdapter.OnTaskInteractionListener {

    private lateinit var txtEmployeeName: TextView
    private lateinit var txtDate: TextView
    private lateinit var txtTime: TextView
    private lateinit var btnClockIn: Button
    private lateinit var txtCheckInTime: TextView
    private lateinit var txtCheckOutTime: TextView
    private lateinit var txtTotalWorkTime: TextView
    private lateinit var txtMarkedAbsent: TextView
    private lateinit var txtOnBreak: TextView
    private lateinit var txtBreak: TextView
    private lateinit var blurOverlay: View
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerEmployeeTasks: RecyclerView
    private lateinit var txtNoTasksAssigned: TextView
    private lateinit var txtSelectedTaskInfo: TextView
    private lateinit var txtTaskSelectionMessage: TextView
    private lateinit var employeeTaskAdapter: EmployeeTaskAdapter
    private var selectedTaskForUI: Task? = null
    private val viewModel: EmployeeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        bindViews(view)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        viewModel.fetchEmployeeTasks()
        return view
    }

    private fun bindViews(view: View) {
        txtEmployeeName = view.findViewById(R.id.txtEmployeeName)
        txtDate = view.findViewById(R.id.txtDate)
        txtTime = view.findViewById(R.id.txtTime)
        btnClockIn = view.findViewById(R.id.btnClockIn)
        txtCheckInTime = view.findViewById(R.id.txtCheckInTime)
        txtCheckOutTime = view.findViewById(R.id.txtCheckOutTime)
        txtTotalWorkTime = view.findViewById(R.id.txtTotalWorkTime)
        txtMarkedAbsent = view.findViewById(R.id.txtMarkedAbsent)
        txtOnBreak = view.findViewById(R.id.txtOnBreak)
        txtBreak = view.findViewById(R.id.txtBreak)
        blurOverlay = view.findViewById(R.id.blurOverlay)
        progressBar = view.findViewById(R.id.progressBar)

        recyclerEmployeeTasks = view.findViewById(R.id.recyclerEmployeeTasks)
        txtNoTasksAssigned = view.findViewById(R.id.txtNoTasksAssigned)
        txtSelectedTaskInfo = view.findViewById(R.id.txtSelectedTaskInfo)
        txtTaskSelectionMessage = view.findViewById(R.id.txtTaskSelectionMessage)
    }

    private fun setupRecyclerView() {
        employeeTaskAdapter = EmployeeTaskAdapter(emptyList(), this)
        recyclerEmployeeTasks.layoutManager = LinearLayoutManager(requireContext())
        recyclerEmployeeTasks.adapter = employeeTaskAdapter
    }

    private fun setupClickListeners() {
        btnClockIn.setOnClickListener {
            viewModel.handleClockInOut()
        }
        txtBreak.setOnClickListener { viewModel.handleBreakResume() }
    }

    private fun observeViewModel() {
        viewModel.homeUiState.observe(viewLifecycleOwner) { state ->
            updateUi(state)
        }

        viewModel.employeeTasks.observe(viewLifecycleOwner) { tasks ->
            employeeTaskAdapter.updateTasks(tasks)
            txtNoTasksAssigned.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
            recyclerEmployeeTasks.visibility = if (tasks.isEmpty()) View.GONE else View.VISIBLE

            if (selectedTaskForUI != null && !tasks.contains(selectedTaskForUI)) {
                clearTaskSelectionDisplay()
            }
            if (tasks.size == 1 && selectedTaskForUI == null) {
                onTaskSelected(tasks[0])
                employeeTaskAdapter.setSelectedTask(tasks[0])
            }
        }

        viewModel.selectedTask.observe(viewLifecycleOwner) { task ->
            selectedTaskForUI = task
            employeeTaskAdapter.setSelectedTask(task)
            if (task != null) {
                txtSelectedTaskInfo.text = "Task: ${task.taskName}"
                txtSelectedTaskInfo.visibility = View.VISIBLE
            } else {
                txtSelectedTaskInfo.visibility = View.GONE
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUi(state: HomeUiState) {
        progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        blurOverlay.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        txtEmployeeName.text = "Hi, ${state.employeeName}"
        txtDate.text = state.formattedDate
        txtTime.text = state.timerText
        btnClockIn.text = state.clockInButtonText
        btnClockIn.isEnabled = state.isClockInButtonEnabled

        txtCheckInTime.text = state.checkInTimeText
        txtCheckOutTime.text = state.checkOutTimeText
        txtTotalWorkTime.text = state.totalWorkTimeText
        txtBreak.text = state.breakButtonText

        txtCheckInTime.visibility = if (state.isCheckInVisible) View.VISIBLE else View.GONE
        txtCheckOutTime.visibility = if (state.isCheckOutVisible) View.VISIBLE else View.GONE
        txtTotalWorkTime.visibility = if (state.isTotalWorkVisible) View.VISIBLE else View.GONE
        txtBreak.visibility = if (state.isBreakButtonVisible) View.VISIBLE else View.GONE
        txtOnBreak.visibility = if (state.isOnBreak) View.VISIBLE else View.GONE
        txtMarkedAbsent.visibility = if (state.isMarkedAbsent) View.VISIBLE else View.GONE
        txtTaskSelectionMessage.visibility = if (state.showTaskSelectionMessage) View.VISIBLE else View.GONE
    }

    override fun onTaskSelected(task: Task) {
        viewModel.setSelectedTask(task)
    }

    override fun onTaskCompleted(task: Task) {
        viewModel.markTaskAsCompleted(task)
    }

    override fun onTaskDeselected() {
        viewModel.setSelectedTask(null)
    }

    private fun clearTaskSelectionDisplay() {
        selectedTaskForUI = null
        employeeTaskAdapter.clearSelection()
    }
}