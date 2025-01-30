package com.hari.management.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.MessagingException
import java.util.Properties

object EmailConfig {
    private const val PREFS_NAME = "email_config"
    private const val KEY_ADMIN_EMAIL = "admin_email"
    private const val KEY_ADMIN_PASSWORD = "admin_password"
    private const val KEY_RECIPIENT = "recipient"

    fun saveAdminCredentials(context: Context, email: String, password: String) {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        
        val sharedPreferences = EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        sharedPreferences.edit().apply {
            putString(KEY_ADMIN_EMAIL, email)
            putString(KEY_ADMIN_PASSWORD, password)
            apply()
        }
    }

    fun saveRecipient(context: Context, recipient: String?) {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        
        val sharedPreferences = EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        sharedPreferences.edit().apply {
            putString(KEY_RECIPIENT, recipient)
            apply()
        }
    }

    fun getCredentials(context: Context): Triple<String?, String?, String?> {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        
        val sharedPreferences = EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val email = sharedPreferences.getString(KEY_ADMIN_EMAIL, null)
        val password = sharedPreferences.getString(KEY_ADMIN_PASSWORD, null)
        val recipient = sharedPreferences.getString(KEY_RECIPIENT, email)
        
        return Triple(email, password, recipient)
    }

    suspend fun sendTestEmail(context: Context) {
        val (email, password, recipient) = getCredentials(context)
        
        if (email == null || password == null) {
            throw IllegalStateException("Email credentials not configured")
        }

        withContext(Dispatchers.IO) {
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", "smtp.gmail.com")
                put("mail.smtp.port", "587")
                put("mail.smtp.ssl.trust", "smtp.gmail.com")
                put("mail.smtp.ssl.protocols", "TLSv1.2")
            }

            val session = Session.getInstance(props, object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(email, password)
                }
            })

            try {
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(email))
                    setRecipients(
                        Message.RecipientType.TO,
                        InternetAddress.parse(recipient)
                    )
                    subject = "Test Email"
                    setText("This is a test email from your Guest Management app.")
                }

                Transport.send(message)
            } catch (e: MessagingException) {
                val errorMessage = when {
                    e.message?.contains("535-5.7.8") == true -> 
                        "Authentication failed. Make sure you're using an App Password and not your regular Gmail password. " +
                        "Go to Google Account Settings > Security > 2-Step Verification > App passwords to generate one."
                    e.message?.contains("Network is unreachable") == true ->
                        "Network error. Please check your internet connection."
                    else -> "Failed to send email: ${e.message}"
                }
                throw RuntimeException(errorMessage)
            }
        }
    }
} 