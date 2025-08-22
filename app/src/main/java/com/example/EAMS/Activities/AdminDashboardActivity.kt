package com.example.EAMS.Activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.EAMS.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.EAMS.AdminFragments.*

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
                R.id.nav_search -> loadFragment(SearchFragment())
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