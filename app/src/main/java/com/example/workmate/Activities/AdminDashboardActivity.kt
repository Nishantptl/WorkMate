package com.example.workmate.Activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.workmate.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.workmate.AdminFragments.*

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        bottomNav = findViewById(R.id.admin_bottom_nav)

        loadFragment(DashboardFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> loadFragment(DashboardFragment())
                R.id.nav_employees -> loadFragment(EmployeeFragment())
                R.id.nav_project -> loadFragment(ProjectFragment())
                R.id.nav_report -> loadFragment(ReportFragment())
                R.id.nav_leave -> loadFragment(AdminLeaveFragment())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, fragment)
            .commit()
    }
}