package com.example.EAMS.Adapters

import android.annotation.SuppressLint
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

    private val fullDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val serverDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.txtHistoryDate)
        val status: TextView = itemView.findViewById(R.id.txtHistoryStatus)
        val checkIn: TextView = itemView.findViewById(R.id.txtHistoryCheckIn)
        val checkOut: TextView = itemView.findViewById(R.id.txtHistoryCheckOut)
        val totalHours: TextView = itemView.findViewById(R.id.txtHistoryTotalHours)
        val divider: View = itemView.findViewById(R.id.divider)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_history, parent, false)
        return HistoryViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val record = historyList[position]

        try {
            val parsedDate = serverDateFormat.parse(record.date)
            holder.date.text = parsedDate?.let { fullDateFormat.format(it) } ?: record.date
        } catch (e: Exception) {
            holder.date.text = record.date
        }

        holder.status.text = record.status
        val statusColor = when (record.status) {
            "Present" -> R.color.Present
            "Late" -> R.color.Late
            "Half Day" -> R.color.HalfDay
            "Absent" -> R.color.Absent
            else -> android.R.color.black
        }
        holder.status.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, statusColor))

        if (record.status.equals("Absent", ignoreCase = true)) {
            holder.divider.visibility = View.GONE
            holder.checkIn.visibility = View.GONE
            holder.checkOut.visibility = View.GONE
            holder.totalHours.visibility = View.GONE
        } else {
            holder.divider.visibility = View.VISIBLE
            holder.checkIn.visibility = View.VISIBLE
            holder.checkOut.visibility = View.VISIBLE
            holder.totalHours.visibility = View.VISIBLE

            holder.checkIn.text = record.checkInTime?.let {
                "In: ${timeFormat.format(Date(it))}"
            } ?: "In: -"

            holder.checkOut.text = record.checkOutTime?.let {
                "Out: ${timeFormat.format(Date(it))}"
            } ?: "Out: -"

            holder.totalHours.text = record.totalWorkDuration?.let { duration ->
                val hours = TimeUnit.MILLISECONDS.toHours(duration)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60
                String.format("Total: %02dh %02dm", hours, minutes)
            } ?: "Total: --h --m"
        }
    }

    override fun getItemCount() = historyList.size
}