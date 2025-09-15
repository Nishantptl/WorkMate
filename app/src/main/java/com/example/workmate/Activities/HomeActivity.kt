package com.example.workmate.Activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.workmate.R

class HomeActivity : AppCompatActivity() {

    private lateinit var cardAdmin: CardView
    private lateinit var cardEmployee: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)
        cardAdmin = findViewById(R.id.cardAdmin)
        cardEmployee = findViewById(R.id.cardEmployee)

        cardAdmin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("ROLE", "ADMIN")
            startActivity(intent)
        }

        cardEmployee.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("ROLE", "EMPLOYEE")
            startActivity(intent)
        }
    }
}