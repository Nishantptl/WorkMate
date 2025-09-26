package com.example.workmate.Adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.workmate.Model.Task
import com.example.workmate.R

class EmployeeTaskAdapter(
    private var tasks: List<Task>,
    private val listener: OnTaskInteractionListener
) : RecyclerView.Adapter<EmployeeTaskAdapter.TaskViewHolder>() {

    private var selectedTask: Task? = null

    interface OnTaskInteractionListener {
        fun onTaskSelected(task: Task)
        fun onTaskCompleted(task: Task)
        fun onTaskDeselected()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_employee_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task, task == selectedTask, listener)
    }

    override fun getItemCount(): Int = tasks.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        if (selectedTask != null && !newTasks.contains(selectedTask)) {
            selectedTask = null
        }
        notifyDataSetChanged()
    }

    fun setSelectedTask(task: Task?) {
        val oldSelectedTaskPosition = if (selectedTask != null) tasks.indexOf(selectedTask) else -1
        selectedTask = task
        val newSelectedTaskPosition = if (task != null) tasks.indexOf(task) else -1

        if (oldSelectedTaskPosition != -1) {
            notifyItemChanged(oldSelectedTaskPosition)
        }
        if (newSelectedTaskPosition != -1) {
            notifyItemChanged(newSelectedTaskPosition)
        }
    }

    fun clearSelection() {
        val oldSelectedTaskPosition = if (selectedTask != null) tasks.indexOf(selectedTask) else -1
        selectedTask = null
        if (oldSelectedTaskPosition != -1) {
            notifyItemChanged(oldSelectedTaskPosition)
        }
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskNameTextView: TextView = itemView.findViewById(R.id.tvTaskName)
        private val projectNameTextView: TextView = itemView.findViewById(R.id.tvProjectNameForTask)
        private val taskRadioButton: RadioButton = itemView.findViewById(R.id.rbTaskSelected)
        private val taskCheckBox: CheckBox = itemView.findViewById(R.id.cbTaskCompleted)

        fun bind(task: Task, isSelected: Boolean, listener: OnTaskInteractionListener) {
            taskNameTextView.text = task.taskName
            projectNameTextView.text = "Project: ${task.projectName ?: "N/A"}"
            taskRadioButton.isChecked = isSelected

            taskCheckBox.setOnCheckedChangeListener(null)
            taskCheckBox.isChecked = task.status == "Completed"

            itemView.setOnClickListener {
                listener.onTaskSelected(task)
            }

            taskCheckBox.setOnClickListener {
                listener.onTaskCompleted(task)
                if (isSelected) {
                    listener.onTaskDeselected()
                }
            }
        }
    }
}