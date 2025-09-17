package com.example.workmate.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.workmate.Model.LeaveRequest
import com.example.workmate.Model.LeaveStatus
import com.example.workmate.R
import java.text.SimpleDateFormat
import java.util.*

class AdminLeaveAdapter(
    private val listener: OnActionClickListener
) : RecyclerView.Adapter<AdminLeaveAdapter.LeaveViewHolder>() {

    private var leaveList = emptyList<LeaveRequest>()
    private val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())

    interface OnActionClickListener {
        fun onApproveClicked(leaveRequest: LeaveRequest)
        fun onRejectClicked(leaveRequest: LeaveRequest)
    }

    inner class LeaveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val employeeName: TextView = itemView.findViewById(R.id.tvEmployeeName)
        val leaveType: TextView = itemView.findViewById(R.id.tvLeaveType)
        val dates: TextView = itemView.findViewById(R.id.tvLeaveDates)
        val reason: TextView = itemView.findViewById(R.id.tvLeaveReason)
        val status: TextView = itemView.findViewById(R.id.tvLeaveStatus)
        val actionButtonsLayout: LinearLayout = itemView.findViewById(R.id.llActionButtons)
        val approveButton: Button = itemView.findViewById(R.id.btnApprove)
        val rejectButton: Button = itemView.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leave_request_admin, parent, false)
        return LeaveViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaveViewHolder, position: Int) {
        val currentItem = leaveList[position]

        holder.employeeName.text = currentItem.employeeName
        holder.leaveType.text = currentItem.leaveType
        holder.reason.text = "Reason: ${currentItem.reason}"

        val startDate = dateFormat.format(currentItem.startDate!!)
        val endDate = dateFormat.format(currentItem.endDate!!)
        holder.dates.text = "$startDate to $endDate"

        holder.status.text = currentItem.status

        // Set status color and button visibility
        when (currentItem.status) {
            LeaveStatus.PENDING.name -> {
                holder.status.backgroundTintList = ContextCompat.getColorStateList(holder.itemView.context, R.color.status_pending)
                holder.actionButtonsLayout.visibility = View.VISIBLE
            }
            LeaveStatus.APPROVED.name -> {
                holder.status.backgroundTintList = ContextCompat.getColorStateList(holder.itemView.context, R.color.status_approved) // Or a green color
                holder.actionButtonsLayout.visibility = View.GONE
            }
            LeaveStatus.REJECTED.name -> {
                holder.status.backgroundTintList = ContextCompat.getColorStateList(holder.itemView.context, R.color.status_rejected)
                holder.actionButtonsLayout.visibility = View.GONE
            }
        }

        holder.approveButton.setOnClickListener {
            listener.onApproveClicked(currentItem)
        }

        holder.rejectButton.setOnClickListener {
            listener.onRejectClicked(currentItem)
        }
    }

    override fun getItemCount() = leaveList.size

    fun setData(requests: List<LeaveRequest>) {
        this.leaveList = requests
        notifyDataSetChanged()
    }
}