package com.example.workmate.Model

data class Employee(
    var id: String = "",
    var uid: String = "",
    var name: String = "",
    var email: String = "",
    var role: String = "EMPLOYEE",
    var department: String = "",
    var joiningDate: String = "",
    var status: String? = "active",
    var organization: String = ""
)
