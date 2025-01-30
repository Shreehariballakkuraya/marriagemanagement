package com.hari.management.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.hari.management.data.GuestEntity
import com.hari.management.data.InvitationStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import jxl.Workbook
import jxl.write.*
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

object GuestDataManager {
    private const val EXCEL_FOLDER = "excel"
    private const val TAG = "GuestDataManager"

    fun exportGuestsToJson(context: Context, guests: List<GuestEntity>): Uri? {
        try {
            val jsonArray = JSONArray()
            guests.forEach { guest ->
                val jsonGuest = JSONObject().apply {
                    put("name", guest.name)
                    put("phoneNumber", guest.phoneNumber)
                    put("status", guest.invitationStatus.name)
                    put("categoryId", guest.categoryId)
                    put("reminderDate", guest.reminderDate)
                }
                jsonArray.put(jsonGuest)
            }

            val fileName = "guests_export_${System.currentTimeMillis()}.json"
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use {
                it.write(jsonArray.toString().toByteArray())
            }

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportGuestsToExcel(context: Context, guests: List<GuestEntity>): Uri? {
        var workbook: WritableWorkbook? = null
        var excelFile: File? = null

        try {
            // Create excel directory if it doesn't exist
            val excelDir = File(context.filesDir, EXCEL_FOLDER)
            if (!excelDir.exists() && !excelDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory: ${excelDir.absolutePath}")
                throw IOException("Failed to create excel directory")
            }

            // Clean up old files
            try {
                excelDir.listFiles()?.forEach { it.delete() }
            } catch (e: Exception) {
                Log.w(TAG, "Error cleaning up old files: ${e.message}")
            }

            // Create Excel file with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            excelFile = File(excelDir, "guests_export_$timestamp.xls")

            // Create a new workbook
            workbook = Workbook.createWorkbook(excelFile)
            val sheet = workbook.createSheet("Guest List", 0)

            // Define cell formats
            val headerFormat = WritableCellFormat().apply {
                setBackground(Colour.GRAY_25)
                setBorder(Border.ALL, BorderLineStyle.THIN)
                setAlignment(Alignment.CENTRE)
            }

            val cellFormat = WritableCellFormat().apply {
                setBorder(Border.ALL, BorderLineStyle.THIN)
            }

            // Add headers
            val headers = listOf(
                "Name",
                "Phone Number",
                "Category",
                "Invitation Status",
                "Reminder Date",
                "Interaction Status"
            )

            headers.forEachIndexed { index, header ->
                sheet.addCell(Label(index, 0, header, headerFormat))
                sheet.setColumnView(index, 20)
            }

            // Add data
            guests.forEachIndexed { index, guest ->
                val row = index + 1
                sheet.addCell(Label(0, row, guest.name, cellFormat))
                sheet.addCell(Label(1, row, guest.phoneNumber, cellFormat))
                sheet.addCell(Label(2, row, guest.categoryId?.toString() ?: "None", cellFormat))
                sheet.addCell(Label(3, row, guest.invitationStatus.name, cellFormat))
                sheet.addCell(Label(4, row, guest.reminderDate?.let { 
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                } ?: "Not Set", cellFormat))
                sheet.addCell(Label(5, row, if (guest.hasInteracted) "Interacted" else "Not Interacted", cellFormat))
            }

            // Write and close the workbook
            workbook.write()
            workbook.close()
            workbook = null

            Log.d(TAG, "Excel file created successfully: ${excelFile.absolutePath}")
            return Uri.fromFile(excelFile)

        } catch (e: Exception) {
            Log.e(TAG, "Error exporting to Excel", e)
            workbook?.close()
            excelFile?.delete()
            return null
        }
    }

    fun importGuestsFromJson(context: Context, uri: Uri): List<GuestEntity> {
        val guests = mutableListOf<GuestEntity>()
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use {
                it?.readText()
            } ?: return emptyList()

            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonGuest = jsonArray.getJSONObject(i)
                guests.add(
                    GuestEntity(
                        name = jsonGuest.getString("name"),
                        phoneNumber = jsonGuest.getString("phoneNumber"),
                        invitationStatus = InvitationStatus.valueOf(jsonGuest.getString("status")),
                        categoryId = if (jsonGuest.has("categoryId")) jsonGuest.getInt("categoryId") else null,
                        reminderDate = if (jsonGuest.has("reminderDate")) jsonGuest.getLong("reminderDate") else null
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return guests
    }

    fun importGuestsFromExcel(context: Context, uri: Uri): List<GuestEntity> {
        val guests = mutableListOf<GuestEntity>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = Workbook.getWorkbook(inputStream)
                
                // Read from all sheets
                workbook.sheets.forEach { sheet ->
                    for (row in 1 until sheet.rows) { // Start from 1 to skip header
                        try {
                            val name = sheet.getCell(0, row).contents.trim()
                            val phoneNumber = sheet.getCell(1, row).contents.trim()
                            
                            // Skip empty rows
                            if (name.isNotEmpty() && phoneNumber.isNotEmpty()) {
                                // Determine status based on sheet name
                                val status = when (sheet.name.uppercase()) {
                                    "INVITED" -> InvitationStatus.INVITED
                                    "PENDING" -> InvitationStatus.PENDING
                                    else -> InvitationStatus.NOT_INVITED
                                }
                                
                                // Get category if exists
                                val categoryId = try {
                                    sheet.getCell(2, row).contents.toIntOrNull()
                                } catch (e: Exception) {
                                    null
                                }
                                
                                // Get reminder date if exists
                                val reminderDate = try {
                                    sheet.getCell(3, row).contents.toLongOrNull()
                                } catch (e: Exception) {
                                    null
                                }
                                
                                guests.add(
                                    GuestEntity(
                                        name = name,
                                        phoneNumber = phoneNumber,
                                        invitationStatus = status,
                                        categoryId = categoryId,
                                        reminderDate = reminderDate
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Continue to next row if there's an error
                            continue
                        }
                    }
                }
                workbook.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return guests
    }
} 