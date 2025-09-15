package com.example.workmate.Model

data class AttendanceRecord(
    val employeeId: String = "",
    val employeeName: String = "",
    val date: String = "",
    val status: String = "",
    val checkInTime: Long? = null,
    val checkOutTime: Long? = null,
    val totalWorkDuration: Long? = null,    // Nullable
    val totalBreakTime: Long = 0L,
    val isOnBreak: Boolean = false,
    val breakStartTime: Long? = null,       // Nullable
    val createdAt: Long = 0L,
    val lastUpdated: Long = 0L
)
