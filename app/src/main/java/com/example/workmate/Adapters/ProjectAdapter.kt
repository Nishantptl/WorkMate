package com.example.workmate.Adapters

import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.workmate.R
import com.example.workmate.Model.*

class ProjectAdapter(
    private var projectList: ArrayList<Project>,
    private val onView: (Project) -> Unit,
    private val onStatusChange: (Project) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    private var fullList: ArrayList<Project> = ArrayList(projectList)

    class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtProjectName: TextView = itemView.findViewById(R.id.txtProjectName)
        val txtProjectRequirement: TextView = itemView.findViewById(R.id.txtProjectRequirements)
        val btnView: ImageButton = itemView.findViewById(R.id.btnView)
        val btnToggleStatus: ImageButton = itemView.findViewById(R.id.btnToggleStatus)
        val txtProjectStatus: TextView = itemView.findViewById(R.id.txtProjectStatus)
        val txtProjectDeadline: TextView = itemView.findViewById(R.id.txtProjectDeadline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projectList[position]
        holder.txtProjectName.text = project.projectName
        holder.txtProjectRequirement.text = project.projectRequirements
        holder.txtProjectStatus.text = project.projectStatus
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        holder.txtProjectDeadline.text = dateFormat.format(project.projectDeadline!!.toDate())

        if (project.projectStatus == "active") {
            holder.btnToggleStatus.setImageResource(R.drawable.ic_active)
            holder.txtProjectStatus.text = "Active"
            holder.txtProjectStatus.setTextColor(holder.itemView.context.getColor(R.color.Present))
        } else {
            holder.btnToggleStatus.setImageResource(R.drawable.ic_inactive)
            holder.txtProjectStatus.text = "Inactive"
            holder.txtProjectStatus.setTextColor(holder.itemView.context.getColor(R.color.Absent))
        }

        holder.btnView.setOnClickListener { onView(project) }
        holder.btnToggleStatus.setOnClickListener { onStatusChange(project) }
    }

    override fun getItemCount(): Int = projectList.size

    fun filter(query: String) {
        projectList = if (query.isEmpty()) {
            ArrayList(fullList)
        } else {
            val filtered = fullList.filter {
                it.projectName.contains(query, ignoreCase = true)
            }
            ArrayList(filtered)
        }
        notifyDataSetChanged()
    }

    fun updateList(newList: ArrayList<Project>) {
        fullList = ArrayList(newList)
        projectList = ArrayList(newList)
        notifyDataSetChanged()
    }
}
