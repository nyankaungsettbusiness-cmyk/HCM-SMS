package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object StudentPhotoUtils {

    private fun getStudentPhotosDir(context: Context): File {
        val dir = File(context.filesDir, "student_photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Saves an image from a Content Uri (Photo Picker) into app internal storage as a compressed PNG.
     * Returns the relative file name (or path) that can be stored in StudentEntity.photoUrl, or null on error.
     */
    fun savePhotoFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                savePhotoFromInputStream(context, inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Resizes and saves an image input stream to internal student photos directory.
     */
    fun savePhotoFromInputStream(context: Context, inputStream: InputStream): String? {
        return try {
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            val maxDimension = 512
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = minOf(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
                val targetW = (width * ratio).toInt().coerceAtLeast(1)
                val targetH = (height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
            } else {
                originalBitmap
            }

            val fileName = "photo_${UUID.randomUUID().toString().take(12)}.png"
            val targetFile = File(getStudentPhotosDir(context), fileName)

            val byteStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, byteStream)
            val bytes = byteStream.toByteArray()

            FileOutputStream(targetFile).use { out ->
                out.write(bytes)
                out.flush()
            }

            fileName
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Loads a student photo Bitmap given a photoUrl/photoPath or Base64 string.
     */
    fun loadStudentPhotoBitmap(context: Context, photoUrl: String?): Bitmap? {
        if (photoUrl.isNullOrBlank()) return null

        // 1. Try internal student photos directory
        val fileInDir = File(getStudentPhotosDir(context), photoUrl)
        if (fileInDir.exists() && fileInDir.length() > 0) {
            try {
                val bmp = BitmapFactory.decodeFile(fileInDir.absolutePath)
                if (bmp != null) return bmp
            } catch (_: Exception) {}
        }

        // 2. Try direct absolute or relative file path
        val directFile = File(photoUrl)
        if (directFile.exists() && directFile.length() > 0) {
            try {
                val bmp = BitmapFactory.decodeFile(directFile.absolutePath)
                if (bmp != null) return bmp
            } catch (_: Exception) {}
        }

        // 3. Try base64 decoding if photoUrl contains base64 image data
        if (photoUrl.startsWith("data:image") || photoUrl.length > 100) {
            try {
                val cleanBase64 = if (photoUrl.contains(",")) photoUrl.substringAfter(",") else photoUrl.trim()
                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                if (decodedBytes != null && decodedBytes.isNotEmpty()) {
                    return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                }
            } catch (_: Exception) {}
        }

        return null
    }

    /**
     * Helper to load ImageBitmap for Compose.
     */
    fun loadStudentPhotoImageBitmap(context: Context, photoUrl: String?): ImageBitmap? {
        return loadStudentPhotoBitmap(context, photoUrl)?.asImageBitmap()
    }

    /**
     * Converts photo to Base64 for export / backup if needed.
     */
    fun getPhotoAsBase64(context: Context, photoUrl: String?): String? {
        if (photoUrl.isNullOrBlank()) return null
        val fileInDir = File(getStudentPhotosDir(context), photoUrl)
        val file = if (fileInDir.exists()) fileInDir else File(photoUrl)
        if (!file.exists() || file.length() == 0L) return null
        return try {
            val bytes = file.readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}
