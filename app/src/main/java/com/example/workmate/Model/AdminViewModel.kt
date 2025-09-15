package com.example.workmate.Model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    // This LiveData will hold the organization name. Fragments can observe it.
    private val _organization = MutableLiveData<String?>()
    val organization: LiveData<String?> = _organization

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
                } else {
                    _organization.value = null
                    Log.w("AdminViewModel", "Admin document not found.")
                }
            }
            .addOnFailureListener {
                _organization.value = null
                Log.e("AdminViewModel", "Failed to fetch admin organization", it)
            }
    }
}