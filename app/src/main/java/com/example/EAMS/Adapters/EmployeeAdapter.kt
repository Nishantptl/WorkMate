package com.example.EAMS.Adapters

import android.media.Image
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
    private val onDelete: (Employee) -> Unit
) : RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder>() {

    class EmployeeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtEmployeeName)
        val txtEmail: TextView = itemView.findViewById(R.id.txtEmployeeEmail)
        val txtRole: TextView = itemView.findViewById(R.id.txtEmployeeRole)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
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
        holder.txtRole.text = employee.role

        holder.btnEdit.setOnClickListener { onEdit(employee) }
        holder.btnDelete.setOnClickListener { onDelete(employee) }
    }

    override fun getItemCount(): Int = employeeList.size
}
