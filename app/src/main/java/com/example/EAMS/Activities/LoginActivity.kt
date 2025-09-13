package com.example.EAMS.Activities

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
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
    private lateinit var imgPasswordToggle: ImageView

    private lateinit var auth: FirebaseAuth
    private var role: String = "EMPLOYEE"
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        role = intent.getStringExtra("ROLE") ?: "EMPLOYEE"

        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogIn)
        txtCreateAccount = findViewById(R.id.txtNewAccount)
        txtResetPassword = findViewById(R.id.txtResetPassword)
        txtErrorMessage = findViewById(R.id.txtErrorMessage)
        imgPasswordToggle = findViewById(R.id.imgPasswordToggle)

        if (role == "ADMIN") {
            btnLogin.text = "Login as Admin"
            txtResetPassword.visibility = View.GONE
            txtCreateAccount.visibility = View.VISIBLE
        } else {
            btnLogin.text = "Login as Employee"
            txtCreateAccount.visibility = View.GONE
            txtResetPassword.visibility = View.VISIBLE
        }

        btnLogin.setOnClickListener { loginClicked() }
        txtCreateAccount.setOnClickListener {
            val intent = Intent(this, NewUserActivity::class.java)
            startActivity(intent)
        }
        txtResetPassword.setOnClickListener { showPasswordResetDialog() }
        imgPasswordToggle.setOnClickListener { togglePasswordVisibility() }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            edtPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            imgPasswordToggle.setImageResource(R.drawable.ic_eye_off)
        } else {
            edtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            imgPasswordToggle.setImageResource(R.drawable.ic_eye)
        }
        edtPassword.setSelection(edtPassword.text.length)
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
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reset_password, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()

        val edtResetEmail = dialogView.findViewById<EditText>(R.id.edtResetEmail)
        val btnSendLink = dialogView.findViewById<Button>(R.id.btnSendLink)
        val txtCancel = dialogView.findViewById<TextView>(R.id.txtCancel)

        btnSendLink.setOnClickListener {
            val email = edtResetEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                FirebaseFirestore.getInstance()
                    .collection("employee")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            auth.sendPasswordResetEmail(email)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Password reset link sent to $email", Toast.LENGTH_LONG).show()
                                    dialog.dismiss()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            Toast.makeText(this, "This email is not registered as an employee", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            }
        }

        txtCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showError(message: String) {
        txtErrorMessage.text = message
        txtErrorMessage.visibility = View.VISIBLE
    }

    private fun hideError() {
        txtErrorMessage.visibility = View.GONE
    }
}

