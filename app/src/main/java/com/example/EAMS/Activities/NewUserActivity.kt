package com.example.EAMS.Activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.EAMS.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NewUserActivity : AppCompatActivity() {

    private lateinit var edtName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var txtLogIn: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var role: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_user)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        role = intent.getStringExtra("ROLE") ?: "EMPLOYEE"

        edtName = findViewById(R.id.edtName)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword)
        btnRegister = findViewById(R.id.btnSignIn)
        txtLogIn = findViewById(R.id.txtLogIn)

        if (role == "ADMIN") {
            btnRegister.text = "Register as Admin"
        } else {
            btnRegister.text = "Register as Employee"
        }

        btnRegister.setOnClickListener { registerClicked() }
        txtLogIn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("ROLE", role)
            startActivity(intent)
            finish()
        }
    }

    private fun registerClicked() {
        val name = edtName.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()
        val confirmPassword = edtConfirmPassword.text.toString().trim()

        when {
            name.isEmpty() -> showToast("Please enter your name")
            email.isEmpty() -> showToast("Please enter your email")
            password.isEmpty() -> showToast("Please enter a password")
            password != confirmPassword -> showToast("Passwords do not match")
            else -> registerUser(name, email, password)
        }
    }

    private fun registerUser(name: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val userMap = hashMapOf(
                        "uid" to userId, "name" to name,
                        "email" to email, "role" to role)

                    firestore.collection(role.lowercase())
                        .document(userId)
                        .set(userMap)
                        .addOnSuccessListener {
                            showToast("Registered successfully as $role!")
                            if (role == "ADMIN") {
                                startActivity(Intent(this, AdminDashboardActivity::class.java))
                            } else {
//                                startActivity(Intent(this, EmployeeDashboardActivity::class.java))
                            }
                        }
                        .addOnFailureListener { e ->
                            showToast("Firestore error: ${e.localizedMessage}")
                        }
                } else {
                    showToast("Registration failed: ${task.exception?.localizedMessage}")
                }
            }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}