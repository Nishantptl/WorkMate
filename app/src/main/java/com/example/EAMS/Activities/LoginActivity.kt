package com.example.EAMS.Activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.EAMS.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var txtCreateAccount: TextView
    private lateinit var txtResetPassword: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var role: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        auth.signOut()
        role = intent.getStringExtra("ROLE") ?: "EMPLOYEE"

        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogIn)
        txtCreateAccount = findViewById(R.id.txtNewAccount)
        txtResetPassword = findViewById(R.id.txtResetPassword)

        if (role == "ADMIN") {
            btnLogin.text = "Login as Admin"
            txtResetPassword.visibility = TextView.GONE
            txtCreateAccount.visibility = TextView.VISIBLE
        } else {
            btnLogin.text = "Login as Employee"
            txtCreateAccount.visibility = TextView.GONE
            txtResetPassword.visibility = TextView.VISIBLE
        }

        btnLogin.setOnClickListener { loginClicked() }
        txtCreateAccount.setOnClickListener {
            val intent = Intent(this, NewUserActivity::class.java)
            intent.putExtra("ROLE", role)
            startActivity(intent)
            finish()
        }

        txtResetPassword.setOnClickListener {
            showPasswordResetDialog()
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
        // Start Progress Dialog here if you have one
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid

                    if (userId == null) {
                        showToast("Authentication failed. Please try again.")
                        return@addOnCompleteListener
                    }

                    val db = FirebaseFirestore.getInstance()

                    if (role == "ADMIN") {

                        db.collection("admin").document(userId).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    showToast("Welcome Admin!")
                                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                                    finish()
                                } else {
                                    // User is authenticated but not in the employee collection
                                    showToast("Admin record not found. Access denied.")
                                    auth.signOut()
                                }
                            }
                            .addOnFailureListener { e ->
                                showToast("Error verifying admin: ${e.localizedMessage}")
                                auth.signOut()
                            }
                        // ▲▲▲ MODIFICATION END ▲▲▲
                    } else { // Employee Logic
                        db.collection("employee").document(userId).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val status = document.getString("status") ?: "inactive"
                                    if (status == "inactive") {
                                        showToast("Your account is deactivated. Please contact admin.")
                                        auth.signOut()
                                    } else {
                                        showToast("Welcome Employee!")
                                        startActivity(Intent(this, EmployeeDashboardActivity::class.java))
                                        finish() // Close LoginActivity
                                    }
                                } else {
                                    showToast("No employee record found. Contact admin.")
                                    auth.signOut()
                                }
                            }
                            .addOnFailureListener { e ->
                                showToast("Error: ${e.localizedMessage}")
                                auth.signOut()
                            }
                    }
                } else {
                    // Stop Progress Dialog here
                    showToast("Login failed: ${task.exception?.localizedMessage}")
                }
            }
    }


    private fun showPasswordResetDialog() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_reset_password, null)

        val edtEmail = dialogView.findViewById<EditText>(R.id.edtResetEmail)

        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setView(dialogView)
            .setPositiveButton("Reset") { _, _ ->
                val email = edtEmail.text.toString().trim()
                if (email.isEmpty()) {
                    showToast("Please enter your email")
                } else {
                    FirebaseFirestore.getInstance()
                        .collection("employee")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener { documents ->
                            if (!documents.isEmpty) {
                                auth.sendPasswordResetEmail(email)
                                    .addOnSuccessListener {
                                        showToast("Password reset link sent to $email")
                                    }
                                    .addOnFailureListener { e ->
                                        showToast("Error: ${e.localizedMessage}")
                                    }
                            } else {
                                showToast("This email is not registered as an employee")
                            }
                        }
                        .addOnFailureListener { e ->
                            showToast("Error checking employee record: ${e.localizedMessage}")
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}