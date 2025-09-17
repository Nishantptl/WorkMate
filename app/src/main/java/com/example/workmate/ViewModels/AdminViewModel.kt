package com.example.workmate.ViewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.workmate.Model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    private val _organization = MutableLiveData<String?>()
    val organization: LiveData<String?> = _organization

    // --- LiveData for Leave Management ---
    private val _operationStatus = MutableLiveData<Pair<Boolean, String>>()
    val operationStatus: LiveData<Pair<Boolean, String>> = _operationStatus

    private val _allLeaveRequests = MutableLiveData<List<LeaveRequest>>()
    val allLeaveRequests: LiveData<List<LeaveRequest>> = _allLeaveRequests

    init {
        fetchAdminOrganization()
    }

    private fun fetchAdminOrganization() {
        if (uid == null) {
            _organization.value = null
            Log.w("AdminViewModel", "User is not logged in.")
            return
        }

        db.collection("admin").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val orgName = doc.getString("organization")
                    _organization.value = orgName
                    Log.d("AdminViewModel", "Fetched organization: $orgName")
                    if (orgName != null) {
                        fetchAllLeaveRequests(orgName)
                    }
                } else {
                    _organization.value = null
                    _allLeaveRequests.postValue(emptyList())
                    Log.w("AdminViewModel", "Admin document not found.")
                }
            }
            .addOnFailureListener {
                _organization.value = null
                _allLeaveRequests.postValue(emptyList())
                Log.e("AdminViewModel", "Failed to fetch admin organization", it)
            }
    }

    fun fetchAllLeaveRequests(organizationName: String) {
        db.collection("leave_requests")
            .whereEqualTo("employeeOrg", organizationName)
            .orderBy("requestedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("AdminVM", "Error fetching all leave requests", error)
                    return@addSnapshotListener
                }
                val requests = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(LeaveRequest::class.java)?.apply { leaveId = doc.id }
                }
                _allLeaveRequests.postValue(requests ?: emptyList())
            }
    }

    fun updateLeaveStatus(leaveId: String, newStatus: LeaveStatus) {
        db.collection("leave_requests").document(leaveId)
            .update("status", newStatus.name)
            .addOnSuccessListener { _operationStatus.postValue(Pair(true, "Status updated successfully.")) }
            .addOnFailureListener { _operationStatus.postValue(Pair(false, "Update failed: ${it.message}")) }
    }
}