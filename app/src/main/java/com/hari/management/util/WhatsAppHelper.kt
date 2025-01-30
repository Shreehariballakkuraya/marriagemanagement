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
    private fun getMessageTemplate(context: Context): String {
        return context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("message_template", 
                """Dear {name},

You are cordially invited to our wedding celebration.

We would be honored by your presence.

Please confirm your attendance.

Best regards."""
            ) ?: ""
    }

    fun sendInvitation(context: Context, phoneNumber: String, guestName: String) {
        if (!PdfHelper.hasPdfTemplate(context) && !ImageHelper.hasImageTemplate(context)) {
            Toast.makeText(context, "Please add an invitation template first", Toast.LENGTH_LONG).show()
            return
        }

        try {
            // Format phone number to WhatsApp format
            val formattedNumber = phoneNumber.filter { it.isDigit() }.let {
                if (it.length == 10) "91$it" else it
            }

            // Get and format the message template
            val messageTemplate = getMessageTemplate(context)
            val message = messageTemplate.replace("{name}", guestName)

            // Check if a PDF template exists
            val pdfFile = PdfHelper.getPdfFile(context)
            if (pdfFile.exists() && pdfFile.canRead() && pdfFile.length() > 0) {
                sendPdf(context, pdfFile, formattedNumber, message)
                return
            }

            // Check if an image template exists
            val imageFile = ImageHelper.getImageFile(context)
            if (imageFile.exists() && imageFile.canRead() && imageFile.length() > 0) {
                sendImage(context, imageFile, formattedNumber, message)
                return
            }

            Toast.makeText(context, "Error: No valid template found", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun sendPdf(context: Context, pdfFile: File, formattedNumber: String, message: String) {
        val pdfUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

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
    }

    private fun sendImage(context: Context, imageFile: File, formattedNumber: String, message: String) {
        if (!imageFile.exists() || !imageFile.canRead()) {
            Toast.makeText(context, "Error: Image file not accessible", Toast.LENGTH_LONG).show()
            return
        }

        val imageUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        val imageIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra("jid", "$formattedNumber@s.whatsapp.net")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(imageIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error sending image", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }

        val guestId = "some_guest_id" // Replace with the actual guest ID
        val database = GuestDatabase.getDatabase(context)
        database.updateInvitationStatus(guestId, "Confirmed") // Update status to "Confirmed"
    }

    fun sendWhatsAppMessage(context: Context, phoneNumber: String, message: String) {
        try {
            val formattedNumber = if (phoneNumber.startsWith("+")) {
                phoneNumber.substring(1)
            } else {
                phoneNumber
            }
            
            val uri = Uri.parse(
                "https://api.whatsapp.com/send?phone=$formattedNumber&text=${Uri.encode(message)}"
            )
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.whatsapp")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            throw e
        }
    }
}