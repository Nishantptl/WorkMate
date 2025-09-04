package com.example.EAMS.Adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.EAMS.Model.AttendanceRecord
import com.example.EAMS.R
import java.text.SimpleDateFormat
import java.util.*

class AttendanceAdapter(
    private var attendanceList: List<AttendanceRecord>
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    inner class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtDate: TextView = itemView.findViewById(R.id.txtDate)
        val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
        val txtCheckIn: TextView = itemView.findViewById(R.id.txtCheckIn)
        val txtCheckOut: TextView = itemView.findViewById(R.id.txtCheckOut)
        val txtDuration: TextView = itemView.findViewById(R.id.txtDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance, parent, false)
        return AttendanceViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val record = attendanceList[position]

        holder.txtDate.text = "Date: ${record.date}"
        holder.txtStatus.text = "Status: ${record.status}"
        holder.txtCheckIn.text = "Check-in: ${formatTime(record.checkInTime)}"
        holder.txtCheckOut.text = ("Check-out:" + record.checkOutTime?.let {
            formatTime(it)
        })

        holder.txtDuration.text = ("Work Duration:" + record.totalWorkDuration?.let {
            formatTime(it)
        })
    }

    override fun getItemCount(): Int = attendanceList.size

    fun updateData(newList: List<AttendanceRecord>) {
        attendanceList = newList
        notifyDataSetChanged()
    }

    private fun formatTime(timeMillis: Long): String {
        if (timeMillis == 0L) return "-"
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timeMillis))
    }

    private fun formatDuration(durationMillis: Long): String {
        if (durationMillis == 0L) return "-"
        val hours = (durationMillis / (1000 * 60 * 60)).toInt()
        val minutes = ((durationMillis / (1000 * 60)) % 60).toInt()
        return "${hours}h ${minutes}m"
    }
}
