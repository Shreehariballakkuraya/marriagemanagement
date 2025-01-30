package com.hari.management.util

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hari.management.data.GuestDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class EmailExportWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            exportAndSendEmail(applicationContext)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    companion object {
        suspend fun exportAndSendEmail(context: Context) {
            try {
                val (email, password, recipient) = EmailConfig.getCredentials(context)
                
                // Check if admin email is configured
                if (email == null || password == null) {
                    throw IllegalStateException("Admin email not configured. Please set up admin email in Settings > Admin Settings first.")
                }

                // Check if there are any guests to export
                val guests = GuestDatabase.getDatabase(context).guestDao().getAllGuests().first()
                if (guests.isEmpty()) {
                    throw IllegalStateException("No guests to export. Please add some guests first.")
                }

                // Export guest data to Excel
                val uri = GuestDataManager.exportGuestsToExcel(context, guests)
                    ?: throw IllegalStateException("Failed to create export file. Please try again.")

                // Verify file exists and is readable
                val file = File(uri.path!!)
                if (!file.exists() || !file.canRead()) {
                    throw IllegalStateException("Cannot access export file. Please try again.")
                }

                // Send email with attachment
                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                    put("mail.smtp.ssl.trust", "smtp.gmail.com")
                    put("mail.smtp.ssl.protocols", "TLSv1.2")
                    put("mail.debug", "true") // Enable debug mode
                }

                val session = Session.getInstance(props, object : javax.mail.Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(email, password)
                    }
                })

                withContext(Dispatchers.IO) {
                    try {
                        val message = MimeMessage(session).apply {
                            setFrom(InternetAddress(email))
                            setRecipients(
                                Message.RecipientType.TO,
                                InternetAddress.parse(recipient ?: email)
                            )
                            subject = "Guest List Export - ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}"

                            // Create the message body part
                            val messageBodyPart = MimeBodyPart().apply {
                                setText("Please find attached the guest list export in Excel format.\n\nTotal Guests: ${guests.size}")
                            }

                            // Create the attachment part
                            val attachmentPart = MimeBodyPart().apply {
                                dataHandler = DataHandler(FileDataSource(file))
                                fileName = "guests_export_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.xls"
                            }

                            // Create the multipart and add parts
                            val multipart = MimeMultipart("mixed").apply {
                                addBodyPart(messageBodyPart)
                                addBodyPart(attachmentPart)
                            }

                            // Set the content
                            setContent(multipart)
                            saveChanges() // Ensure all changes are saved
                        }

                        // Send the message
                        Transport.send(message)
                    } catch (e: MessagingException) {
                        val errorMessage = when {
                            e.message?.contains("535-5.7.8") == true -> 
                                "Authentication failed. Please check admin email settings."
                            e.message?.contains("Network is unreachable") == true ->
                                "Network error. Please check your internet connection."
                            e.message?.contains("Invalid Addresses") == true ->
                                "Invalid email address. Please check the recipient email."
                            else -> "Email error: ${e.message}"
                        }
                        e.printStackTrace() // Log the full stack trace
                        throw MessagingException(errorMessage)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace() // Log the full stack trace
                when (e) {
                    is MessagingException -> throw e
                    is IllegalStateException -> throw e
                    is SecurityException -> throw IllegalStateException("Permission denied while accessing files.")
                    is OutOfMemoryError -> throw IllegalStateException("Not enough memory to process the export.")
                    else -> throw IllegalStateException("System error: ${e.javaClass.simpleName} - ${e.message}")
                }
            }
        }
    }
} 