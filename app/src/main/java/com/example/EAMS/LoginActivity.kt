package com.example.EAMS

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var spinnerRole: Spinner
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var txtCreateAccount: TextView

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        try {
            FirebaseApp.initializeApp(this)
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            showToast("Firebase initialization failed: ${e.message}")
            return
        }

        spinnerRole = findViewById(R.id.spinnerRole)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogIn)
        txtCreateAccount = findViewById(R.id.txtNewAccount)

        val roles = listOf("Select Role", "Admin", "Employee")
        val adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item, roles)
        spinnerRole.adapter = adapter

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            val role = spinnerRole.selectedItem.toString()

            if (role == "Select Role") {
                showToast("Please select a role")
            } else if (email.isEmpty()) {
                showToast("Please enter your email")
            } else if (password.isEmpty()) {
                showToast("Please enter your password")
            } else {
                loginUser(email, password, role)
            }
        }

        txtCreateAccount.setOnClickListener {
            startActivity(Intent(this, NewUserActivity::class.java))
            finish()
        }
    }

    private fun loginUser(email: String, password: String, role: String) {
        val collectionName = when (role.lowercase()) {
            "admin" -> "admins"
            "employee" -> "employees"
            else -> return
        }

        firestore.collection(collectionName)
            .whereEqualTo("email", email)
            .whereEqualTo("password", password)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val user = documents.documents[0]
                    val userName = user.getString("name") ?: "User"

                    showToast("Login successful! Welcome $userName")

                    val intent = Intent(this, HomeActivity::class.java).apply {
                        putExtra("USER_ID", user.id)
                        putExtra("USER_NAME", userName)
                        putExtra("USER_EMAIL", email)
                        putExtra("USER_ROLE", role)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    showToast("Invalid credentials. Please check your email, password, and role.")
                }
            }
            .addOnFailureListener { exception ->
                Log.d("A1", exception.message.toString())
                showToast("Login failed: ${exception.message}")
            }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}