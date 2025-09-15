package com.example.workmate.Model

data class HomeUiState(
    val employeeName: String = "",
    val formattedDate: String = "",
    val timerText: String = "00:00:00",
    val clockInButtonText: String = "Check In",
    val isClockInButtonEnabled: Boolean = true,
    val checkInTimeText: String = "",
    val isCheckInVisible: Boolean = false,
    val checkOutTimeText: String = "",
    val isCheckOutVisible: Boolean = false,
    val totalWorkTimeText: String = "",
    val isTotalWorkVisible: Boolean = false,
    val breakButtonText: String = "I'm taking a break",
    val isBreakButtonVisible: Boolean = false,
    val isOnBreak: Boolean = false,
    val isMarkedAbsent: Boolean = false,
    val isLoading: Boolean = true
)