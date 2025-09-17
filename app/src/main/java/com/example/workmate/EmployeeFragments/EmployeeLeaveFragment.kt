package com.example.workmate.EmployeeFragments

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.workmate.Adapters.*
import com.example.workmate.Model.*
import com.example.workmate.R
import com.example.workmate.ViewModels.*
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class EmployeeLeaveFragment : Fragment() {

    // ViewModels and Auth
    private lateinit var employeeViewModel: EmployeeViewModel
    private val auth = FirebaseAuth.getInstance()

    // Class-level variable to hold the employee profile
    private var currentEmployee: Employee? = null

    // Adapter for the list
    private lateinit var leaveAdapter: LeaveRequestAdapter

    // Main fragment UI Views
    private lateinit var rvLeaveHistory: RecyclerView
    private lateinit var tvNoRequests: TextView
    private lateinit var fabRequestLeave: FloatingActionButton
    private lateinit var blurOverlay: FrameLayout
    private lateinit var progressBar: ProgressBar // Re-added ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_employee_leave, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        employeeViewModel = ViewModelProvider(this)[EmployeeViewModel::class.java]

        // Find main fragment views
        rvLeaveHistory = view.findViewById(R.id.rvLeaveHistory)
        tvNoRequests = view.findViewById(R.id.tvNoRequests)
        fabRequestLeave = view.findViewById(R.id.fabRequestLeave)
        blurOverlay = view.findViewById(R.id.blurOverlay)
        progressBar = view.findViewById(R.id.progressBar) // Initialized ProgressBar

        setupRecyclerView()
        observeLeaveRequests()
        observeEmployeeProfile()
        observeOperationStatus()

        // Fetch data
        employeeViewModel.loadEmployeeDetails()

        fabRequestLeave.setOnClickListener {
            currentEmployee?.let {
                showRequestLeaveDialog(it)
            } ?: Toast.makeText(context, "Profile is still loading...", Toast.LENGTH_SHORT).show()
        }
    }

    // A helper function to control the blur overlay during submission
    private fun showSubmitLoading(show: Boolean) {
        blurOverlay.visibility = if (show) View.VISIBLE else View.GONE
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun observeEmployeeProfile() {
        employeeViewModel.employeeProfile.observe(viewLifecycleOwner) { profile ->
            currentEmployee = profile
        }
    }

    private fun observeOperationStatus() {
        employeeViewModel.operationStatus.observe(viewLifecycleOwner) { (success, message) ->
            showSubmitLoading(false) // Hide blur overlay after operation completes
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            if (success) {
                employeeViewModel.refreshLeaveRequests()
            }
        }
    }

    private fun showRequestLeaveDialog(employee: Employee) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_request_leave, null)
        val builder = AlertDialog.Builder(requireContext()).setView(dialogView).setCancelable(false)
        val dialog = builder.create()
        dialog.show()

        var startDate: Date? = null
        var endDate: Date? = null
        val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())

        val etStartDate = dialogView.findViewById<EditText>(R.id.etStartDate)
        val etEndDate = dialogView.findViewById<EditText>(R.id.etEndDate)
        val spinnerLeaveType = dialogView.findViewById<Spinner>(R.id.spinnerLeaveType)
        val etReason = dialogView.findViewById<EditText>(R.id.etReason)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmit)

        val leaveTypes = resources.getStringArray(R.array.leave_types)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, leaveTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLeaveType.adapter = adapter

        etStartDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker().build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                startDate = calendar.time
                etStartDate.setText(dateFormat.format(startDate!!))
            }
            datePicker.show(childFragmentManager, "DATE_PICKER_START")
        }

        etEndDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker().build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                endDate = calendar.time
                etEndDate.setText(dateFormat.format(endDate!!))
            }
            datePicker.show(childFragmentManager, "DATE_PICKER_END")
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            val leaveType = spinnerLeaveType.selectedItem.toString()
            val reason = etReason.text.toString().trim()

            if (startDate == null || endDate == null || reason.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (endDate!!.before(startDate)) {
                Toast.makeText(context, "End date cannot be before start date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val leaveRequest = LeaveRequest(
                userId = currentEmployee!!.uid,
                employeeName = currentEmployee!!.name,
                employeeOrg = currentEmployee!!.organization,
                startDate = startDate,
                endDate = endDate,
                leaveType = leaveType,
                reason = reason,
                status = LeaveStatus.PENDING.name
            )

            showSubmitLoading(true) // Show blur overlay before submitting
            employeeViewModel.submitLeaveRequest(leaveRequest)
            dialog.dismiss()
        }
    }

    private fun setupRecyclerView() {
        leaveAdapter = LeaveRequestAdapter()
        rvLeaveHistory.adapter = leaveAdapter
        rvLeaveHistory.layoutManager = LinearLayoutManager(requireContext())
    }

    // ** CORRECTED LOGIC FOR INITIAL LOAD **
    private fun observeLeaveRequests() {
        // 1. Before fetching, show the progress bar and hide everything else.
        progressBar.visibility = View.VISIBLE
        rvLeaveHistory.visibility = View.GONE
        tvNoRequests.visibility = View.GONE

        employeeViewModel.myLeaveRequests.observe(viewLifecycleOwner) { leaveList ->
            // 2. As soon as data arrives, hide the progress bar.
            progressBar.visibility = View.GONE

            // 3. Now, show either the list or the "no requests" text.
            if (leaveList.isNullOrEmpty()) {
                tvNoRequests.visibility = View.VISIBLE
            } else {
                rvLeaveHistory.visibility = View.VISIBLE
                (rvLeaveHistory.adapter as? LeaveRequestAdapter)?.setData(leaveList)
            }
        }
    }
}