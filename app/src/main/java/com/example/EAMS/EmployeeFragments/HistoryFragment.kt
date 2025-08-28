package com.example.EAMS.EmployeeFragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.EAMS.R
import com.example.EAMS.Adapters.AttendanceHistoryAdapter
import com.example.EAMS.Model.AttendanceRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoryFragment : Fragment() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyAdapter: AttendanceHistoryAdapter
    private val historyList = mutableListOf<AttendanceRecord>()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        // Initialize RecyclerView
        historyRecyclerView = view.findViewById(R.id.historyRecyclerView)
        historyRecyclerView.layoutManager = LinearLayoutManager(context)
        historyAdapter = AttendanceHistoryAdapter(historyList)
        historyRecyclerView.adapter = historyAdapter

        // Load data from Firestore
        loadAttendanceHistory()

        return view
    }

    private fun loadAttendanceHistory() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("attendance")
            .whereEqualTo("employeeId", userId)
            .orderBy("date", Query.Direction.DESCENDING) // Show most recent first
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(context, "No attendance history found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                historyList.clear() // Clear previous data
                for (document in documents) {
                    val record = AttendanceRecord(
                        date = document.getString("date") ?: "",
                        status = document.getString("status") ?: "N/A",
                        checkInTime = document.getLong("checkInTime") ?: 0,
                        checkOutTime = document.getLong("checkOutTime") ?: 0,
                        totalWorkDuration = document.getLong("totalWorkDuration") ?: 0
                    )
                    historyList.add(record)
                }
                historyAdapter.notifyDataSetChanged() // Refresh the list
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading history: ${e.message}", Toast.LENGTH_LONG).show()
                Log.d("HistoryFragment", "Error loading history: ${e.message}")
            }
    }
}
