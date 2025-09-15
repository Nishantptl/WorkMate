package com.example.workmate.Adapters

import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.workmate.R
import com.example.workmate.Model.*

class AddEmployeeAdapter(
    private var employeeList: ArrayList<Employee>
) : RecyclerView.Adapter<AddEmployeeAdapter.EmployeeViewHolder>() {

    class EmployeeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtEmployeeName)
        val txtEmail: TextView = itemView.findViewById(R.id.txtEmployeeEmail)
        val txtDepartment: TextView = itemView.findViewById(R.id.txtDepartment)
        val txtJoiningDate: TextView = itemView.findViewById(R.id.txtJoiningDate)
        val txtOrganization: TextView = itemView.findViewById(R.id.txtOrganization)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_add_employee, parent, false)
        return EmployeeViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        val employee = employeeList[position]
        holder.txtName.text = employee.name
        holder.txtEmail.text = employee.email
        holder.txtDepartment.text = employee.department
        holder.txtJoiningDate.text = employee.joiningDate
        holder.txtOrganization.text = employee.organization
    }

    override fun getItemCount(): Int = employeeList.size
}
