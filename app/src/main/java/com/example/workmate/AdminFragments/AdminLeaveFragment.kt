package com.example.workmate.AdminFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.workmate.Adapters.AdminLeaveAdapter
import com.example.workmate.Model.LeaveRequest
import com.example.workmate.Model.LeaveStatus
import com.example.workmate.R
import com.example.workmate.ViewModels.AdminViewModel

class AdminLeaveFragment : Fragment(), AdminLeaveAdapter.OnActionClickListener {

    private lateinit var adminViewModel: AdminViewModel
    private lateinit var leaveAdapter: AdminLeaveAdapter
    private lateinit var rvAllLeaveRequests: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var blurOverlay: FrameLayout // Changed to FrameLayout for clarity
    private lateinit var tvNoRequests: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_leave, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adminViewModel = ViewModelProvider(this)[AdminViewModel::class.java]

        rvAllLeaveRequests = view.findViewById(R.id.rvAllLeaveRequests)
        progressBar = view.findViewById(R.id.progressBar)
        blurOverlay = view.findViewById(R.id.blurOverlay)
        tvNoRequests = view.findViewById(R.id.tvNoRequestsAdmin)

        setupRecyclerView()
        observeViewModel()
    }

    private fun showLoading(show: Boolean) {
        blurOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setupRecyclerView() {
        leaveAdapter = AdminLeaveAdapter(this)
        rvAllLeaveRequests.adapter = leaveAdapter
        rvAllLeaveRequests.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeViewModel() {
        showLoading(true) // Show loading for initial data fetch
        adminViewModel.allLeaveRequests.observe(viewLifecycleOwner) { requests ->
            showLoading(false) // Hide loading after data is fetched
            if (requests.isNullOrEmpty()) {
                tvNoRequests.visibility = View.VISIBLE
                rvAllLeaveRequests.visibility = View.GONE
            } else {
                tvNoRequests.visibility = View.GONE
                rvAllLeaveRequests.visibility = View.VISIBLE
                leaveAdapter.setData(requests)
            }
        }

        adminViewModel.operationStatus.observe(viewLifecycleOwner) { (success, message) ->
            showLoading(false) // Hide loading after an operation (approve/reject) is complete
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onApproveClicked(leaveRequest: LeaveRequest) {
        showConfirmationDialog("Approve", leaveRequest) {
            adminViewModel.updateLeaveStatus(leaveRequest.leaveId, LeaveStatus.APPROVED)
        }
    }

    override fun onRejectClicked(leaveRequest: LeaveRequest) {
        showConfirmationDialog("Reject", leaveRequest) {
            adminViewModel.updateLeaveStatus(leaveRequest.leaveId, LeaveStatus.REJECTED)
        }
    }

    private fun showConfirmationDialog(action: String, request: LeaveRequest, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("$action Leave Request")
            .setMessage("Are you sure you want to $action this leave request from ${request.employeeName}?")
            .setPositiveButton(action) { dialog, _ ->
                showLoading(true) // Show loading before starting the update operation
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
}