package com.example.EAMS.Adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.EAMS.R
import com.example.EAMS.Model.*

class EmployeeAdapter(
    private val employeeList: ArrayList<Employee>,
    private val onEdit: (Employee) -> Unit,
    private val onStatusChange: (Employee) -> Unit
) : RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder>() {

    class EmployeeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtEmployeeName)
        val txtEmail: TextView = itemView.findViewById(R.id.txtEmployeeEmail)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnToggleStatus: ImageButton = itemView.findViewById(R.id.btnToggleStatus)
        val txtDepartment: TextView = itemView.findViewById(R.id.txtDepartment)
        val txtJoiningDate: TextView = itemView.findViewById(R.id.txtJoiningDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_employee, parent, false)
        return EmployeeViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        val employee = employeeList[position]
        holder.txtName.text = employee.name
        holder.txtEmail.text = employee.email
        holder.txtDepartment.text = employee.department
        holder.txtJoiningDate.text = employee.joiningDate

        // Set status button appearance
        if (employee.status == "active") {
            holder.btnToggleStatus.setImageResource(R.drawable.ic_active)
        } else {
            holder.btnToggleStatus.setImageResource(R.drawable.ic_inactive)
        }

        holder.btnEdit.setOnClickListener { onEdit(employee) }
        holder.btnToggleStatus.setOnClickListener { onStatusChange(employee) }
    }

    override fun getItemCount(): Int = employeeList.size
}
