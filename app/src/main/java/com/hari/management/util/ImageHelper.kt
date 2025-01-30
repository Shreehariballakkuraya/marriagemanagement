package com.hari.management.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageHelper {
    private const val IMAGE_FOLDER = "images"
    private const val IMAGE_FILE_NAME = "invitation.png"

    fun saveImageTemplate(context: Context, uri: Uri): Boolean {
        return try {
            // Create images directory if it doesn't exist
            val imageDir = File(context.filesDir, IMAGE_FOLDER).apply {
                if (!exists()) mkdirs()
            }

            val imageFile = File(imageDir, IMAGE_FILE_NAME)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(imageFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getImageFile(context: Context): File {
        val imageDir = File(context.filesDir, IMAGE_FOLDER)
        return File(imageDir, IMAGE_FILE_NAME)
    }

    fun hasImageTemplate(context: Context): Boolean {
        return getImageFile(context).exists()
    }
}