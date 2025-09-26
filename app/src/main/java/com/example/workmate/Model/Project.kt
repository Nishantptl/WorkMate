package com.example.workmate.Model

import com.google.firebase.Timestamp
import java.util.Date

data class Project (
    var id: String = "",
    val projectName: String = "",
    var projectStatus: String = "active",
    val projectRequirements: String = "",
    val projectDeadline: Timestamp? = null,
    val projectManager: String = "",
    val organization: String = "",
    val teamMembersId: List<String> = emptyList()
)