package com.example.workmate.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.workmate.Model.Employee
import com.example.workmate.R

class EmployeeSelectAdapter(
    private val employeeList: List<Employee>,
    private val onSelectionChanged: (List<Employee>) -> Unit
) : RecyclerView.Adapter<EmployeeSelectAdapter.EmployeeViewHolder>() {

    private val selectedEmployeeIds = mutableSetOf<String>()

    fun getSelectedItems(): List<Employee> {
        return employeeList.filter { selectedEmployeeIds.contains(it.id) }
    }

    class EmployeeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.txtEmployeeName)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxEmployee)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_select_employee, parent, false)
        return EmployeeViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        val employee = employeeList[position]
        holder.nameTextView.text = employee.name
        holder.checkBox.isChecked = selectedEmployeeIds.contains(employee.id)

        holder.checkBox.setOnClickListener {
            if (selectedEmployeeIds.contains(employee.id)) {
                selectedEmployeeIds.remove(employee.id)
            } else {
                selectedEmployeeIds.add(employee.id)
            }
            onSelectionChanged(getSelectedItems())
            notifyItemChanged(holder.adapterPosition)
        }
    }

    override fun getItemCount() = employeeList.size
}