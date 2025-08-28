package com.example.EAMS.Activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.EAMS.EmployeeFragments.*
import com.example.EAMS.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class EmployeeDashboardActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employee_dashboard)

        bottomNavigation = findViewById(R.id.employee_bottom_nav)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.employee_fragment_container, HomeFragment())
                        .commit()
                    true
                }
                R.id.nav_history -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.employee_fragment_container, HistoryFragment())
                        .commit()
                    true
                }
                R.id.nav_account -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.employee_fragment_container, AccountFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }

        // Load default fragment
        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    fun setSelectedTab(tabId: Int) {
        bottomNavigation.selectedItemId = tabId
    }
}
