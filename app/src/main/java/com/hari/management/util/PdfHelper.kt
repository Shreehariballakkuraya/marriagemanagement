package com.hari.management.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object PdfHelper {
    private const val PDF_FOLDER = "pdfs"
    private const val PDF_FILE_NAME = "invitation.pdf"

    fun savePdfTemplate(context: Context, uri: Uri): Boolean {
        return try {
            // Create pdfs directory if it doesn't exist
            val pdfDir = File(context.filesDir, PDF_FOLDER).apply {
                if (!exists()) mkdirs()
            }
            
            val pdfFile = File(pdfDir, PDF_FILE_NAME)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(pdfFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getPdfFile(context: Context): File {
        val pdfDir = File(context.filesDir, PDF_FOLDER)
        return File(pdfDir, PDF_FILE_NAME)
    }

    fun hasPdfTemplate(context: Context): Boolean {
        return getPdfFile(context).exists()
    }
} 