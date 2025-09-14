package com.example.EAMS.EmployeeFragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.EAMS.Activities.*
import com.example.EAMS.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountFragment : Fragment() {

    private lateinit var txtUserName: TextView
    private lateinit var txtUserEmail: TextView
    private lateinit var txtUserOrganization: TextView

    private lateinit var layoutEditProfile: LinearLayout
    private lateinit var layoutCheckInOut: LinearLayout
    private lateinit var layoutAttendanceHistory: LinearLayout
    private lateinit var layoutLogout: LinearLayout

    private lateinit var blurOverlay: FrameLayout

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        // Initialize Views
        txtUserName = view.findViewById(R.id.txtUserName)
        txtUserEmail = view.findViewById(R.id.txtUserEmail)
        txtUserOrganization = view.findViewById(R.id.txtUserOrganization)

        blurOverlay = view.findViewById(R.id.blurOverlay)

        layoutEditProfile = view.findViewById(R.id.layoutEditProfile)
        layoutCheckInOut = view.findViewById(R.id.layoutCheckInOut)
        layoutAttendanceHistory = view.findViewById(R.id.layoutAttendanceHistory)
        layoutLogout = view.findViewById(R.id.layoutLogout)

        loadUserProfile()
        setupClickListeners()

        return view
    }

    private fun showLoading(show: Boolean) {
        blurOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun loadUserProfile() {
        showLoading(true)
        val currentUser = auth.currentUser
        if (currentUser != null) {
            txtUserEmail.text = currentUser.email

            firestore.collection("employee")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        txtUserName.text = document.getString("name") ?: "Employee"
                        txtUserOrganization.text = document.getString("organization") ?: "Organization"
                    }
                    showLoading(false)
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load profile.", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
        }else showLoading(false)
    }

    private fun setupClickListeners() {
        layoutEditProfile.setOnClickListener {
            Toast.makeText(context, "Edit Profile clicked", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to EditProfileFragment or Activity
        }

        layoutCheckInOut.setOnClickListener {
            (activity as? EmployeeDashboardActivity)?.setSelectedTab(R.id.nav_home)
        }

        layoutAttendanceHistory.setOnClickListener {
            (activity as? EmployeeDashboardActivity)?.setSelectedTab(R.id.nav_history)
        }

        layoutLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
