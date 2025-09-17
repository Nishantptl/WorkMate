package com.example.workmate.Model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class LeaveRequest(
    var leaveId: String = "",
    val userId: String = "",
    val employeeName: String = "",
    val employeeOrg: String = "",
    val startDate: Date? = null,
    val endDate: Date? = null,
    val leaveType: String = "",
    val reason: String = "",
    var status: String = LeaveStatus.PENDING.name,
    @ServerTimestamp
    val requestedAt: Date? = null
)

enum class LeaveStatus {
    PENDING,
    APPROVED,
    REJECTED
}