package com.hari.management.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import com.hari.management.data.GuestDatabase

object WhatsAppHelper {
    fun sendInvitation(context: Context, phoneNumber: String, guestName: String) {
        if (!PdfHelper.hasPdfTemplate(context)) {
            Toast.makeText(context, "Please add an invitation template first", Toast.LENGTH_LONG).show()
            return
        }

        try {
            // Format phone number to WhatsApp format
            val formattedNumber = phoneNumber.filter { it.isDigit() }.let {
                if (it.length == 10) "91$it" else it
            }

            // Create the message
            val message = """Dear $guestName,

You are cordially invited to our wedding celebration.

We would be honored by your presence.

Please confirm your attendance.

Best regards."""

            // Verify PDF exists and is readable
            val pdfFile = PdfHelper.getPdfFile(context)
            if (!verifyPdfFile(pdfFile)) {
                Toast.makeText(context, "Error: PDF file not accessible", Toast.LENGTH_LONG).show()
                return
            }

            val pdfUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            // First, send the message
            val messageIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedNumber&text=${Uri.encode(message)}")
                setPackage("com.whatsapp")
            }

            try {
                context.startActivity(messageIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening WhatsApp for message", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }

            // Use a Handler to delay sending the PDF
            Handler(Looper.getMainLooper()).postDelayed({
                // Then, send the PDF with the message as a caption
                val pdfIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    setPackage("com.whatsapp")
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    putExtra(Intent.EXTRA_TEXT, message) // Include the message as a caption
                    putExtra("jid", "$formattedNumber@s.whatsapp.net")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                try {
                    context.startActivity(pdfIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Error sending PDF", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }

                val guestId = "some_guest_id" // Replace with the actual guest ID
                val database = GuestDatabase.getDatabase(context)
                database.updateInvitationStatus(guestId, "Confirmed") // Update status to "Confirmed"
            }, 2000) // Delay for 2 seconds

        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
    }

    private fun verifyPdfFile(file: File): Boolean {
        return file.exists() && file.canRead() && file.length() > 0
    }
} 