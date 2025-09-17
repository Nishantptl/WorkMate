    package com.example.workmate.Adapters

    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.ImageButton
    import android.widget.TextView
    import androidx.recyclerview.widget.RecyclerView
    import com.example.workmate.R
    import com.example.workmate.Model.*

    class ReportEmployeeAdapter(
        private var employees: List<ReportEmployee>,
        private val onViewClick: (ReportEmployee) -> Unit
    ) : RecyclerView.Adapter<ReportEmployeeAdapter.ReportEmployeeViewHolder>() {

        class ReportEmployeeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val txtSrNo: TextView = itemView.findViewById(R.id.txtSrNo)
            val txtEmployeeName: TextView = itemView.findViewById(R.id.txtEmployeeName)
            val btnView: ImageButton = itemView.findViewById(R.id.btnView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportEmployeeViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_row_employee, parent, false)
            return ReportEmployeeViewHolder(view)
        }

        override fun onBindViewHolder(holder: ReportEmployeeViewHolder, position: Int) {
            val employee = employees[position]

            holder.txtSrNo.text = (position + 1).toString()
            holder.txtEmployeeName.text = employee.name

            holder.btnView.setOnClickListener {
                onViewClick(employee)
            }
        }

        override fun getItemCount(): Int = employees.size

        fun updateList(newList: List<ReportEmployee>) {
            employees = newList
            notifyDataSetChanged()
        }
    }
