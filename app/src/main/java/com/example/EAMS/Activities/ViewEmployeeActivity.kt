package com.example.EAMS.Activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.*
import com.example.EAMS.R
import com.example.EAMS.Adapters.AttendanceAdapter
import com.example.EAMS.Model.AttendanceRecord
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ViewEmployeeActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    private lateinit var txtName: TextView
    private lateinit var txtDepartment: TextView
    private lateinit var txtJoiningDate: TextView
    private lateinit var txtEmail: TextView
    private lateinit var btnDownload: ImageButton
    private lateinit var recyclerAttendance: RecyclerView
    private lateinit var attendanceAdapter: AttendanceAdapter
    private val attendanceList = mutableListOf<AttendanceRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_employee)

        db = FirebaseFirestore.getInstance()

        // UI refs
        txtName = findViewById(R.id.txtEmployeeName)
        txtDepartment = findViewById(R.id.txtEmployeeDepartment)
        txtJoiningDate = findViewById(R.id.txtEmployeeJoiningDate)
        txtEmail = findViewById(R.id.txtEmployeeEmail)
        btnDownload = findViewById(R.id.btnDownload)
        recyclerAttendance = findViewById(R.id.recyclerAttendance)

        attendanceAdapter = AttendanceAdapter(attendanceList)
        recyclerAttendance.layoutManager = LinearLayoutManager(this)
        recyclerAttendance.adapter = attendanceAdapter

        // Get ID from intent
        val employeeId = intent.getStringExtra("employeeId")

        if (employeeId != null) {
            fetchEmployeeDetails(employeeId)
            fetchAttendanceHistory(employeeId)
        } else {
            Toast.makeText(this, "Employee ID not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnDownload.setOnClickListener {
            showExportOptions()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fetchEmployeeDetails(employeeId: String) {
        db.collection("employee").document(employeeId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    txtName.text = ("Name : " + document.getString("name"))
                    txtDepartment.text = ("Department : " + document.getString("department"))
                    txtJoiningDate.text = ("Joining Date : " + document.getString("joiningDate"))
                    txtEmail.text = ("Email : " + document.getString("email"))
                } else {
                    Toast.makeText(this, "No such employee", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching details", Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("SimpleDateFormat")
    private fun fetchAttendanceHistory(employeeId: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("attendance")
            .whereEqualTo("employeeId", employeeId)
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING) // ✅ Sort by date DESC
            .get()
            .addOnSuccessListener { result ->
                attendanceList.clear()
                for (document in result) {
                    val record = document.toObject(AttendanceRecord::class.java)

                    // ✅ Skip today's record
                    if (record.date != today) {
                        attendanceList.add(record)
                    }
                }
                attendanceAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching attendance", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showExportOptions() {
        val options = arrayOf("Export as PDF", "Export as Excel")

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Choose Export Format")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> generateEmployeePdf()   // PDF
                1 -> generateEmployeeExcel() // Excel
            }
        }
        builder.show()
    }

    @SuppressLint("SetTextI18n")
    private fun generateEmployeePdf() {
        val employeeName = txtName.text.toString()
        val employeeEmail = txtEmail.text.toString()
        val employeeDept = txtDepartment.text.toString()
        val employeeJoinDate = txtJoiningDate.text.toString()

        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = android.graphics.Paint()
        paint.textSize = 16f

        var y = 50

        // Employee details
        canvas.drawText("Employee Profile", 230f, y.toFloat(), paint)
        y += 40
        canvas.drawText(employeeName, 50f, y.toFloat(), paint)
        y += 30
        canvas.drawText(employeeEmail, 50f, y.toFloat(), paint)
        y += 30
        canvas.drawText(employeeDept, 50f, y.toFloat(), paint)
        y += 30
        canvas.drawText(employeeJoinDate, 50f, y.toFloat(), paint)
        y += 50

        // Attendance history
        canvas.drawText("Attendance History:", 50f, y.toFloat(), paint)
        y += 30

        for (record in attendanceList) {
            canvas.drawText("Date: ${record.date} | Status: ${record.status}", 50f, y.toFloat(), paint)
            y += 25
            if (y > 800) break // avoid overflow for demo
        }

        pdfDocument.finishPage(page)

        // ✅ Write to ByteArray
        val outputStream = java.io.ByteArrayOutputStream()
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()

        val pdfBytes = outputStream.toByteArray()

        val formatter = java.text.SimpleDateFormat("dd-MM-yyyy_HH-mm", java.util.Locale.getDefault())
        val timeStamp = formatter.format(java.util.Date())
        val fileName = "Employee_$timeStamp.pdf"

        // Save in Downloads
        saveFileToDownloads(
            fileName,
            "application/pdf",
            pdfBytes
        )

        Toast.makeText(this, "PDF saved in Downloads", Toast.LENGTH_LONG).show()
    }

    private fun generateEmployeeExcel() {
        val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
        val sheet = workbook.createSheet("Employee Data")

        var rowIdx = 0
        var row = sheet.createRow(rowIdx++)
        row.createCell(0).setCellValue("Employee Profile")

        row = sheet.createRow(rowIdx++)
        row.createCell(0).setCellValue(txtName.text.toString())

        row = sheet.createRow(rowIdx++)
        row.createCell(0).setCellValue(txtEmail.text.toString())

        row = sheet.createRow(rowIdx++)
        row.createCell(0).setCellValue(txtDepartment.text.toString())

        row = sheet.createRow(rowIdx++)
        row.createCell(0).setCellValue(txtJoiningDate.text.toString())

        rowIdx++
        row = sheet.createRow(rowIdx++)
        row.createCell(0).setCellValue("Attendance History")

        for (record in attendanceList) {
            row = sheet.createRow(rowIdx++)
            row.createCell(0).setCellValue(record.date ?: "")
            row.createCell(1).setCellValue(record.status ?: "")
        }

        // Write to ByteArray
        val byteArrayOutputStream = java.io.ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)
        workbook.close()

        val excelBytes = byteArrayOutputStream.toByteArray()

        val formatter = java.text.SimpleDateFormat("dd-MM-yyyy_HH-mm", java.util.Locale.getDefault())
        val timeStamp = formatter.format(java.util.Date())
        val fileName = "Employee_$timeStamp.xlsx"

        // Save in Downloads
        saveFileToDownloads(
            fileName,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            excelBytes
        )

        Toast.makeText(this, "Excel saved in Downloads", Toast.LENGTH_LONG).show()
    }

    private fun saveFileToDownloads(fileName: String, mimeType: String, content: ByteArray): File? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Android 10 and above → use MediaStore
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, mimeType)
                put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
            }

            val resolver = contentResolver
            val collection =
                android.provider.MediaStore.Downloads.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values)

            uri?.let {
                resolver.openOutputStream(it).use { outputStream ->
                    outputStream?.write(content)
                }
                values.clear()
                values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            null
        } else {
            // For Android 9 and below → direct file write
            val downloadsDir =
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, fileName)
            java.io.FileOutputStream(file).use { it.write(content) }
            file
        }
    }
}
