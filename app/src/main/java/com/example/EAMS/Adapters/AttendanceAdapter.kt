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
import java.util.concurrent.TimeUnit

class AttendanceAdapter(
    private var attendanceList: List<AttendanceRecord>
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

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

    @SuppressLint("SetTextI18n", "DefaultLocale")
    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val record = attendanceList[position]

        holder.txtDate.text = "Date: ${record.date}"
        holder.txtStatus.text = "Status: ${record.status}"
        holder.txtStatus.setTextColor(
            when (record.status) {
                "Present" -> holder.itemView.context.getColor(R.color.Present)
                "Absent" -> holder.itemView.context.getColor(R.color.Absent)
                "Half Day" -> holder.itemView.context.getColor(R.color.HalfDay)
                "Late" -> holder.itemView.context.getColor(R.color.Late)
                else -> holder.itemView.context.getColor(R.color.black)
            }
        )

        // Conditional visibility logic
        if (record.status == "Absent") {
            holder.txtCheckIn.visibility = View.GONE
            holder.txtCheckOut.visibility = View.GONE
            holder.txtDuration.visibility = View.GONE
        } else {
            // Make sure views are visible for other statuses
            holder.txtCheckIn.visibility = View.VISIBLE
            holder.txtCheckOut.visibility = View.VISIBLE
            holder.txtDuration.visibility = View.VISIBLE

            holder.txtCheckIn.text = record.checkInTime?.let {
                "In: ${timeFormat.format(Date(it))}"
            } ?: "In: -"

            holder.txtCheckOut.text = record.checkOutTime?.let {
                "Out: ${timeFormat.format(Date(it))}"
            } ?: "Out: -"

            holder.txtDuration.text = record.totalWorkDuration?.let { duration ->
                val hours = TimeUnit.MILLISECONDS.toHours(duration)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60
                String.format("Total: %02dh %02dm", hours, minutes)
            } ?: "Total: --h --m"
        }
    }

    override fun getItemCount(): Int = attendanceList.size
}
