package com.example.EAMS.Model

data class AttendanceRecord(
    val date: String = "",
    val status: String = "",
    val checkInTime: Long = 0,
    val checkOutTime: Long = 0,
    val totalWorkDuration: Long = 0
)
