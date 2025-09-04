package com.example.EAMS.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.EAMS.R
import com.example.EAMS.Model.AttendanceRecord
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AttendanceHistoryAdapter(private val historyList: List<AttendanceRecord>) :
    RecyclerView.Adapter<AttendanceHistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.txtHistoryDate)
        val status: TextView = itemView.findViewById(R.id.txtHistoryStatus)
        val checkIn: TextView = itemView.findViewById(R.id.txtHistoryCheckIn)
        val checkOut: TextView = itemView.findViewById(R.id.txtHistoryCheckOut)
        val totalHours: TextView = itemView.findViewById(R.id.txtHistoryTotalHours)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val record = historyList[position]

        // Formatters for time and date
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()) // e.g., 09:30 AM

        // Set Date
        // The date from Firestore is "yyyy-MM-dd", we parse and reformat it
        val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(record.date)
        holder.date.text = if (parsedDate != null) dateFormat.format(parsedDate) else record.date

        holder.status.text = record.status
        when (record.status) {
            "Present" -> holder.status.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.Present)
            )
            "Late" -> holder.status.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.Late)
            )
            "Half Day" ->holder.status.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.HalfDay)
            )
            "Absent" -> holder.status.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.Absent)
            )
            else -> holder.status.setBackgroundColor(Color.GRAY)
        }


        // Set Check-in and Check-out times
        holder.checkIn.text = "In: ${timeFormat.format(Date(record.checkInTime))}"
        holder.checkOut.text = if (record.checkOutTime != null) {
            "Out: ${timeFormat.format(Date(record.checkOutTime))}"
        } else {
            "Out: --:--"
        }

        // Set Total Work Duration
        val duration = record.totalWorkDuration
        if (duration != null) {
            val hours = TimeUnit.MILLISECONDS.toHours(duration)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60
            holder.totalHours.text = String.format("Total: %02dh %02dm", hours, minutes)
        } else {
            holder.totalHours.text = "Total: --h --m"
        }
    }

    override fun getItemCount() = historyList.size
}
