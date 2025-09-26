package com.example.workmate.Model

import java.util.Date

data class Task(
    var id: String = "",
    var projectId: String = "",
    var projectName: String = "",
    var taskName: String = "",
    var taskDescription: String = "",
    var assignedToId: String? = null, // Employee ID
    var assignedToName: String? = null, // Employee Name (for display convenience)
    var status: String = "To Do", // e.g., "To Do", "In Progress", "Completed"
    var createdAt: Date? = null
)