package com.example.workmate.Adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.workmate.Model.Task
import com.example.workmate.R

class TaskAdapter(private var tasks: List<Task>) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged() // Or use DiffUtil for better performance
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTaskName: TextView = itemView.findViewById(R.id.tvTaskName)
        private val tvTaskAssignedToName: TextView = itemView.findViewById(R.id.tvTaskAssignedToName)
        private val tvTaskStatus: TextView = itemView.findViewById(R.id.tvTaskStatus)

        fun bind(task: Task) {
            tvTaskName.text = task.taskName
            tvTaskAssignedToName.text = task.assignedToName ?: "Not Assigned"
            tvTaskStatus.text = task.status

            // Set status background
            val context = itemView.context
            when (task.status.lowercase()) { // Use toLowerCase() for case-insensitive comparison
                "to do" -> tvTaskStatus.background = ContextCompat.getDrawable(context, R.drawable.bg_status_todo)
                "in progress" -> tvTaskStatus.background = ContextCompat.getDrawable(context, R.drawable.bg_status_inprogress)
                "completed" -> tvTaskStatus.background = ContextCompat.getDrawable(context, R.drawable.bg_status_completed)
                else -> tvTaskStatus.background = ContextCompat.getDrawable(context, R.drawable.bg_status_todo) // Default
            }
        }
    }
}
