package com.example.workmate.EmployeeFragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.workmate.Activities.*
import com.example.workmate.Model.*
import com.example.workmate.R
import com.example.workmate.ViewModels.EmployeeViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class AccountFragment : Fragment() {

    // --- UI Elements ---
    private lateinit var txtUserName: TextView
    private lateinit var txtUserEmail: TextView
    private lateinit var txtUserOrganization: TextView
    private lateinit var layoutChangePassword: LinearLayout
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
        layoutChangePassword = view.findViewById(R.id.layoutChangePassword)
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
        layoutChangePassword.setOnClickListener {
            showChangePasswordDialog()
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
            activity?.finish()
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)

        val edtOldPassword = dialogView.findViewById<EditText>(R.id.edtOldPassword)
        val edtNewPassword = dialogView.findViewById<EditText>(R.id.edtNewPassword)
        val edtConfirmPassword = dialogView.findViewById<EditText>(R.id.edtConfirmNewPassword)

        val oldPasswordToggle = dialogView.findViewById<ImageView>(R.id.imgOldPasswordToggle)
        val newPasswordToggle = dialogView.findViewById<ImageView>(R.id.imgNewPasswordToggle)
        val confirmPasswordToggle = dialogView.findViewById<ImageView>(R.id.imgConfirmPasswordToggle)

        setupPasswordToggle(edtOldPassword, oldPasswordToggle)
        setupPasswordToggle(edtNewPassword, newPasswordToggle)
        setupPasswordToggle(edtConfirmPassword, confirmPasswordToggle)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Update", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val oldPass = edtOldPassword.text.toString().trim()
                val newPass = edtNewPassword.text.toString().trim()
                val confirmPass = edtConfirmPassword.text.toString().trim()

                if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                    Toast.makeText(context, "All fields are required.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (newPass.length < 6) {
                    Toast.makeText(context, "New password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (newPass != confirmPass) {
                    Toast.makeText(context, "New passwords do not match.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                positiveButton.isEnabled = false
                positiveButton.text = "Updating..."

                val user = auth.currentUser ?: return@setOnClickListener
                val credential = EmailAuthProvider.getCredential(user.email!!, oldPass)
                user.reauthenticate(credential)
                    .addOnSuccessListener {
                        user.updatePassword(newPass)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }
                            .addOnFailureListener { e ->
                                positiveButton.isEnabled = true
                                positiveButton.text = "Update"
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        positiveButton.isEnabled = true
                        positiveButton.text = "Update"
                        Toast.makeText(context, "Authentication failed: Incorrect old password.", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        dialog.show()
    }

    private fun setupPasswordToggle(editText: EditText, imageView: ImageView) {
        imageView.setOnClickListener {
            if (editText.transformationMethod == null) {
                editText.transformationMethod = PasswordTransformationMethod.getInstance()
                imageView.setImageResource(R.drawable.ic_eye_off)
            } else {
                editText.transformationMethod = null
                imageView.setImageResource(R.drawable.ic_eye)
            }
            editText.setSelection(editText.text.length)
        }
    }
}