package com.example.workmate.EmployeeFragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.workmate.Activities.*
import com.example.workmate.Model.*
import com.example.workmate.R
import com.google.firebase.auth.FirebaseAuth

class AccountFragment : Fragment() {

    // --- UI Elements ---
    private lateinit var txtUserName: TextView
    private lateinit var txtUserEmail: TextView
    private lateinit var txtUserOrganization: TextView
    private lateinit var layoutEditProfile: LinearLayout
    private lateinit var layoutCheckInOut: LinearLayout
    private lateinit var layoutAttendanceHistory: LinearLayout
    private lateinit var layoutLogout: LinearLayout
    private lateinit var blurOverlay: FrameLayout

    // 👈 Get the shared EmployeeViewModel
    private val employeeViewModel: EmployeeViewModel by activityViewModels()
    private val auth = FirebaseAuth.getInstance()

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

        observeViewModel() // 👈 Observe the ViewModel for data
        setupClickListeners()

        return view
    }

    private fun observeViewModel() {
        showLoading(true)
        employeeViewModel.employeeProfile.observe(viewLifecycleOwner) { profile ->
            showLoading(false)
            if (profile != null) {
                txtUserName.text = profile.name
                txtUserEmail.text = profile.email
                txtUserOrganization.text = profile.organization
            } else {
                Toast.makeText(context, "Failed to load profile.", Toast.LENGTH_SHORT).show()
                // You can set default text if profile is null
                txtUserName.text = "User"
                txtUserEmail.text = "Not logged in"
                txtUserOrganization.text = "Organization Not Found"
            }
        }
    }

    private fun showLoading(show: Boolean) {
        blurOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setupClickListeners() {
        layoutEditProfile.setOnClickListener {
            Toast.makeText(context, "Edit Profile clicked", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to an EditProfile screen
        }

        layoutCheckInOut.setOnClickListener {
            // This assumes your activity has a method to switch tabs
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
            activity?.finish()
        }
    }
}