package com.example.workmate.EmployeeFragments

import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.workmate.R
import com.example.workmate.Adapters.AttendanceHistoryAdapter
import com.example.workmate.Model.*
import com.example.workmate.ViewModels.EmployeeViewModel

class HistoryFragment : Fragment() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var txtNoHistory: TextView
    private lateinit var historyAdapter: AttendanceHistoryAdapter
    private lateinit var blurOverlay: FrameLayout

    private val viewModel: EmployeeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        historyRecyclerView = view.findViewById(R.id.historyRecyclerView)
        txtNoHistory = view.findViewById(R.id.txtNoHistory)
        blurOverlay = view.findViewById(R.id.blurOverlay)

        setupRecyclerView()
        observeViewModel() // 👈 Observe for data changes

        return view
    }

    private fun setupRecyclerView() {
        historyAdapter = AttendanceHistoryAdapter(emptyList()) // Start with empty list
        historyRecyclerView.layoutManager = LinearLayoutManager(context)
        historyRecyclerView.adapter = historyAdapter
    }

    private fun observeViewModel() {
        showLoading(true)
        viewModel.attendanceHistory.observe(viewLifecycleOwner) { historyList ->
            showLoading(false)
            if (historyList.isNullOrEmpty()) {
                historyRecyclerView.visibility = View.GONE
                txtNoHistory.visibility = View.VISIBLE
            } else {
                historyAdapter.updateList(historyList) // Update adapter with new data
                historyRecyclerView.visibility = View.VISIBLE
                txtNoHistory.visibility = View.GONE
            }
        }
    }

    private fun showLoading(show: Boolean) {
        blurOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }
}