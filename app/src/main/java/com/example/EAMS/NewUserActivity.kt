package com.example.EAMS

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class NewUserActivity : AppCompatActivity() {

    private lateinit var spinnerRole: Spinner
    private lateinit var edtName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var txtLogIn: TextView

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_user)

        firestore = FirebaseFirestore.getInstance()

        spinnerRole = findViewById(R.id.spinnerRole)
        edtName = findViewById(R.id.edtName)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword)
        btnRegister = findViewById(R.id.btnSignIn)
        txtLogIn = findViewById(R.id.txtLogIn)

        val roles = listOf("Select Role", "Admin", "Employee")
        val adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item, roles)
        spinnerRole.adapter = adapter

        btnRegister.setOnClickListener {
            val role = spinnerRole.selectedItem.toString()
            val name = edtName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            val confirmPassword = edtConfirmPassword.text.toString().trim()

            if (role == "Select Role") {
                showToast("Please select a role")
            } else if (name.isEmpty()) {
                showToast("Please enter your name")
            } else if (email.isEmpty()) {
                showToast("Please enter your email")
            } else if (password.isEmpty()) {
                showToast("Please enter a password")
            } else if (password != confirmPassword) {
                showToast("Passwords do not match")
            } else {
                registerUser(name, email, role, password)
            }
        }

        txtLogIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser(name: String, email: String, role: String, password: String) {
        val userId = generateUserId()
        saveUserToRoleBasedCollection(userId, name, email, role, password)
    }

    private fun generateUserId(): String {
        return firestore.collection("users").document().id
    }

    private fun saveUserToRoleBasedCollection(userId: String, name: String, email: String,
                                              role: String, password: String) {
        val userMap = mapOf(
            "uid" to userId,
            "name" to name,
            "email" to email,
            "role" to role,
            "password" to password,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        val collectionName = when (role.lowercase()) {
            "admin" -> "admins"
            "employee" -> "employees"
            else -> "users"
        }

        Log.d("A2", collectionName);
        firestore.collection(collectionName)
            .document(userId)
            .set(userMap)
            .addOnSuccessListener {
                showToast("Registration successful!")
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
            .addOnFailureListener { exception ->
                Log.d("A2", exception.message.toString())
                showToast("Failed to save user: ${exception.message}")
            }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}