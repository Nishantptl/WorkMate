package com.example.EAMS.EmployeeFragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.*
import android.widget.*
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
    private lateinit var txtNoHistory: TextView
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
        txtNoHistory = view.findViewById(R.id.txtNoHistory)

        historyRecyclerView.layoutManager = LinearLayoutManager(context)
        historyAdapter = AttendanceHistoryAdapter(historyList)
        historyRecyclerView.adapter = historyAdapter

        loadAttendanceHistory()

        return view
    }

    private fun loadAttendanceHistory() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("attendance")
            .whereEqualTo("employeeId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->

                if (documents.isEmpty) {
                    historyList.clear()
                    historyAdapter.notifyDataSetChanged()

                    historyRecyclerView.visibility = View.GONE
                    txtNoHistory.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                historyList.clear() // Clear previous data
                for (document in documents) {
                    val record = AttendanceRecord(
                        date = document.getString("date") ?: "",
                        status = document.getString("status") ?: "N/A",
                        checkInTime = document.getLong("checkInTime"),
                        checkOutTime = document.getLong("checkOutTime"),
                        totalWorkDuration = document.getLong("totalWorkDuration")
                    )
                    historyList.add(record)
                }
                historyAdapter.notifyDataSetChanged()

                historyRecyclerView.visibility = View.VISIBLE
                txtNoHistory.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading history: ${e.message}", Toast.LENGTH_LONG).show()
                Log.d("HistoryFragment", "Error loading history: ${e.message}")
            }
    }
}
