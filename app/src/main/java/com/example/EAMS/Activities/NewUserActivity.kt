package com.example.EAMS.Activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.EAMS.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NewUserActivity : AppCompatActivity() {

    private lateinit var edtName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtConfirmPassword: EditText
    private lateinit var spinnerOrganization: Spinner
    private lateinit var btnRegister: Button
    private lateinit var txtLogIn: TextView
    private lateinit var txtErrorMessage: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var selectedOrganization: String? = null
    private val companyList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_user)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        edtName = findViewById(R.id.edtName)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword)
        spinnerOrganization = findViewById(R.id.spinnerOrganization)
        btnRegister = findViewById(R.id.btnSignIn)
        txtLogIn = findViewById(R.id.txtLogIn)
        txtErrorMessage = findViewById(R.id.txtErrorMessage)

        btnRegister.text = "Register as Admin"

        loadOrganizations()

        spinnerOrganization.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                selectedOrganization = if (position > 0) companyList[position] else null
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { selectedOrganization = null }
        }

        btnRegister.setOnClickListener { registerClicked() }
        txtLogIn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("ROLE", "ADMIN")
            startActivity(intent)
            finish()
        }
    }

    private fun loadOrganizations() {
        companyList.clear()
        companyList.add("-- Select Organization --")
        firestore.collection("organizations")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val name = doc.getString("name")
                    if (!name.isNullOrEmpty()) companyList.add(name)
                }
                spinnerOrganization.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    companyList
                )
            }
            .addOnFailureListener {
                showError("Failed to load organizations: ${it.localizedMessage}")
            }
    }

    private fun registerClicked() {
        val name = edtName.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()
        val confirmPassword = edtConfirmPassword.text.toString().trim()

        when {
            name.isEmpty() -> showError("Please enter your name")
            selectedOrganization.isNullOrEmpty() -> showError("Please select your organization")
            email.isEmpty() -> showError("Please enter your email")
            password.isEmpty() -> showError("Please enter a password")
            password != confirmPassword -> showError("Passwords do not match")
            else -> registerUser(name, email, password, selectedOrganization!!)
        }
    }

    private fun registerUser(name: String, email: String, password: String, organization: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val userMap = hashMapOf(
                        "uid" to userId,
                        "name" to name,
                        "email" to email,
                        "organization" to organization,
                        "role" to "ADMIN"
                    )

                    firestore.collection("admin")
                        .document(userId)
                        .set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registered successfully as Admin!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, AdminDashboardActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            showError("Firestore error: ${e.localizedMessage}")
                        }
                } else {
                    showError("Registration failed: ${task.exception?.localizedMessage}")
                }
            }
    }

    private fun showError(message: String) {
        txtErrorMessage.text = message
        txtErrorMessage.visibility = View.VISIBLE
    }
}
