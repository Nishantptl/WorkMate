package com.example.workmate.Adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.workmate.Model.LeaveRequest
import com.example.workmate.Model.LeaveStatus
import com.example.workmate.R
import java.text.SimpleDateFormat
import java.util.*

class LeaveRequestAdapter : RecyclerView.Adapter<LeaveRequestAdapter.LeaveViewHolder>() {

    private var leaveList: List<LeaveRequest> = emptyList()

    // Public method to update the data in the adapter
    fun setData(newLeaveList: List<LeaveRequest>) {
        this.leaveList = newLeaveList
        notifyDataSetChanged() // Inefficient, but the "old way"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leave_request_employee, parent, false)
        return LeaveViewHolder(view)
    }

    override fun getItemCount(): Int {
        return leaveList.size
    }

    override fun onBindViewHolder(holder: LeaveViewHolder, position: Int) {
        val currentItem = leaveList[position]
        holder.bind(currentItem)
    }

    class LeaveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val context: Context = itemView.context
        private val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())

        // Find views by their ID
        private val txtLeaveType: TextView = itemView.findViewById(R.id.txtLeaveType)
        private val txtLeaveStatus: TextView = itemView.findViewById(R.id.txtLeaveStatus)
        private val txtLeaveDates: TextView = itemView.findViewById(R.id.txtLeaveDates)
        private val txtLeaveReason: TextView = itemView.findViewById(R.id.txtLeaveReason)


        @SuppressLint("SetTextI18n")
        fun bind(leaveRequest: LeaveRequest) {
            txtLeaveType.text = leaveRequest.leaveType
            txtLeaveReason.text = leaveRequest.reason

            val startDateStr = leaveRequest.startDate?.let { dateFormat.format(it) } ?: "N/A"
            val endDateStr = leaveRequest.endDate?.let { dateFormat.format(it) } ?: "N/A"
            txtLeaveDates.text = "$startDateStr to $endDateStr"

            txtLeaveStatus.text = leaveRequest.status
            setStatusAppearance(leaveRequest.status)
        }

        private fun setStatusAppearance(status: String) {
            val statusEnum = LeaveStatus.valueOf(status)
            val (backgroundColor, textColor) = when (statusEnum) {
                LeaveStatus.PENDING -> R.color.status_pending to R.color.white
                LeaveStatus.APPROVED -> R.color.status_approved to R.color.white
                LeaveStatus.REJECTED -> R.color.status_rejected to R.color.white
            }
            txtLeaveStatus.setBackgroundColor(ContextCompat.getColor(context, backgroundColor))
            txtLeaveStatus.setTextColor(ContextCompat.getColor(context, textColor))
        }
    }
}