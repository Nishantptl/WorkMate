package com.example.EAMS

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var txtCreateAccount: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var role: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        role = intent.getStringExtra("ROLE") ?: "EMPLOYEE"

        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogIn)
        txtCreateAccount = findViewById(R.id.txtNewAccount)

        if (role == "ADMIN") {
            btnLogin.text = "Login as Admin"
        } else {
            btnLogin.text = "Login as Employee"
        }

        btnLogin.setOnClickListener { loginClicked() }
        txtCreateAccount.setOnClickListener {
            val intent = Intent(this, NewUserActivity::class.java)
            intent.putExtra("ROLE", role)
            startActivity(intent)
            finish()
        }
    }

    private fun loginClicked() {
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()
        when {
            email.isEmpty() -> showToast("Please enter your email")
            password.isEmpty() -> showToast("Please enter your password")
            else -> loginUser(email, password)
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid

                    if (role == "ADMIN") {
                        showToast("Welcome Admin!")
//                        startActivity(Intent(this, AdminDashboardActivity::class.java))
                    } else {
                        showToast("Welcome Employee!")
//                        startActivity(Intent(this, EmployeeDashboardActivity::class.java))
                    }
                    finish()
                } else {
                    showToast("Login failed: ${task.exception?.localizedMessage}")
                }
            }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
