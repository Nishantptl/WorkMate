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
    private lateinit var txtErrorMessage: TextView

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
        txtErrorMessage = findViewById(R.id.txtErrorMessage)

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
            startActivity(intent)
            finish()
        }

        txtResetPassword.setOnClickListener { showPasswordResetDialog() }
    }

    private fun loginClicked() {
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()
        when {
            email.isEmpty() -> showError("Please enter your email")
            password.isEmpty() -> showError("Please enter your password")
            else -> loginUser(email, password)
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    hideError()
                    val userId = auth.currentUser?.uid
                    if (userId == null) {
                        showError("Authentication failed. Please try again.")
                        return@addOnCompleteListener
                    }

                    val db = FirebaseFirestore.getInstance()

                    if (role == "ADMIN") {
                        db.collection("admin").document(userId).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    Toast.makeText(this, "Welcome Admin!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                                    finish()
                                } else {
                                    showError("Admin record not found. Access denied.")
                                    auth.signOut()
                                }
                            }
                            .addOnFailureListener { e ->
                                showError("Error verifying admin: ${e.localizedMessage}")
                                auth.signOut()
                            }
                    } else {
                        db.collection("employee").document(userId).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val status = document.getString("status") ?: "inactive"
                                    if (status == "inactive") {
                                        showError("Your account is deactivated. Please contact admin.")
                                        auth.signOut()
                                    } else {
                                        Toast.makeText(this, "Welcome Employee!", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, EmployeeDashboardActivity::class.java))
                                        finish()
                                    }
                                } else {
                                    showError("No employee record found. Contact admin.")
                                    auth.signOut()
                                }
                            }
                            .addOnFailureListener { e ->
                                showError("Error: ${e.localizedMessage}")
                                auth.signOut()
                            }
                    }
                } else {
                    showError("Wrong Credentials. Please try again.")
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
                    showError("Please enter your email")
                } else {
                    FirebaseFirestore.getInstance()
                        .collection("employee")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener { documents ->
                            if (!documents.isEmpty) {
                                auth.sendPasswordResetEmail(email)
                                    .addOnSuccessListener {
                                        showError("Password reset link sent to $email")
                                    }
                                    .addOnFailureListener { e ->
                                        showError("Error: ${e.localizedMessage}")
                                    }
                            } else {
                                showError("This email is not registered as an employee")
                            }
                        }
                        .addOnFailureListener { e ->
                            showError("Error checking employee record: ${e.localizedMessage}")
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showError(message: String) {
        txtErrorMessage.text = message
        txtErrorMessage.visibility = TextView.VISIBLE
    }

    private fun hideError() {
        txtErrorMessage.visibility = TextView.GONE
    }
}
