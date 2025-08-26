package com.example.EAMS.Activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.EAMS.EmployeeFragments.*
import com.example.EAMS.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class EmployeeDashboardActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employee_dashboard)

        bottomNav = findViewById(R.id.employee_bottom_nav)

        loadFragment(DashboardFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> loadFragment(DashboardFragment())
                R.id.nav_history -> loadFragment(HistoryFragment())
                R.id.nav_account -> loadFragment(AccountFragment())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.employee_fragment_container, fragment)
            .commit()
    }
}