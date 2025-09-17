package com.example.workmate.EmployeeFragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.workmate.Model.*
import com.example.workmate.R
import com.example.workmate.ViewModels.EmployeeViewModel

class HomeFragment : Fragment() {

    // --- UI Elements ---
    private lateinit var txtEmployeeName: TextView
    private lateinit var txtDate: TextView
    private lateinit var txtTime: TextView
    private lateinit var btnClockIn: Button
    private lateinit var txtCheckInTime: TextView
    private lateinit var txtCheckOutTime: TextView
    private lateinit var txtTotalWorkTime: TextView
    private lateinit var txtMarkedAbsent: TextView
    private lateinit var txtOnBreak: TextView
    private lateinit var txtBreak: TextView
    private lateinit var blurOverlay: View
    private lateinit var progressBar: ProgressBar

    // 👈 Get the shared ViewModel
    private val viewModel: EmployeeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        bindViews(view)
        setupClickListeners()
        observeViewModel() // 👈 Observe for UI state changes
        return view
    }

    private fun bindViews(view: View) {
        txtEmployeeName = view.findViewById(R.id.txtEmployeeName)
        txtDate = view.findViewById(R.id.txtDate)
        // ... find all other views ...
        txtTime = view.findViewById(R.id.txtTime)
        btnClockIn = view.findViewById(R.id.btnClockIn)
        txtCheckInTime = view.findViewById(R.id.txtCheckInTime)
        txtCheckOutTime = view.findViewById(R.id.txtCheckOutTime)
        txtTotalWorkTime = view.findViewById(R.id.txtTotalWorkTime)
        txtMarkedAbsent = view.findViewById(R.id.txtMarkedAbsent)
        txtOnBreak = view.findViewById(R.id.txtOnBreak)
        txtBreak = view.findViewById(R.id.txtBreak)
        blurOverlay = view.findViewById(R.id.blurOverlay)
        progressBar  = view.findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        btnClockIn.setOnClickListener { viewModel.handleClockInOut() }
        txtBreak.setOnClickListener { viewModel.handleBreakResume() }
    }

    private fun observeViewModel() {
        viewModel.homeUiState.observe(viewLifecycleOwner) { state ->
            updateUi(state)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUi(state: HomeUiState) {
        progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        blurOverlay.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        txtEmployeeName.text = "Hi, ${state.employeeName}"
        txtDate.text = state.formattedDate
        txtTime.text = state.timerText
        btnClockIn.text = state.clockInButtonText
        btnClockIn.isEnabled = state.isClockInButtonEnabled
        txtCheckInTime.text = state.checkInTimeText
        txtCheckOutTime.text = state.checkOutTimeText
        txtTotalWorkTime.text = state.totalWorkTimeText
        txtBreak.text = state.breakButtonText

        // Update visibility based on state
        txtCheckInTime.visibility = if (state.isCheckInVisible) View.VISIBLE else View.GONE
        txtCheckOutTime.visibility = if (state.isCheckOutVisible) View.VISIBLE else View.GONE
        txtTotalWorkTime.visibility = if (state.isTotalWorkVisible) View.VISIBLE else View.GONE
        txtBreak.visibility = if (state.isBreakButtonVisible) View.VISIBLE else View.GONE
        txtOnBreak.visibility = if (state.isOnBreak) View.VISIBLE else View.GONE
        txtMarkedAbsent.visibility = if (state.isMarkedAbsent) View.VISIBLE else View.GONE
    }
}