package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object SchoolLogoUtils {
    const val LOGO_FILE_NAME = "school_logo.png"

    fun getSchoolLogoFile(context: Context): File {
        return File(context.filesDir, LOGO_FILE_NAME)
    }

    /**
     * Converts a local school logo file into a Base64 data string for cloud synchronization across devices.
     */
    fun getSchoolLogoAsBase64(context: Context): String? {
        val file = getSchoolLogoFile(context)
        if (!file.exists() || file.length() == 0L) return null
        return try {
            val bytes = file.readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resizes and compresses an input stream image into local file cache and returns the Base64 representation.
     */
    fun saveLogoFromInputStream(context: Context, inputStream: InputStream): String? {
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

            val byteStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, byteStream)
            val bytes = byteStream.toByteArray()

            // Save to local internal storage
            val file = getSchoolLogoFile(context)
            FileOutputStream(file).use { out ->
                out.write(bytes)
                out.flush()
            }

            // Return Base64 encoded string
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes Base64 image data and writes it to the local school logo file.
     */
    fun saveLogoFromBase64(context: Context, base64Data: String): Boolean {
        if (base64Data.isBlank()) return false
        return try {
            val cleanBase64 = if (base64Data.contains(",")) {
                base64Data.substringAfter(",")
            } else {
                base64Data.trim()
            }
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            if (decodedBytes == null || decodedBytes.isEmpty()) return false

            val file = getSchoolLogoFile(context)
            FileOutputStream(file).use { out ->
                out.write(decodedBytes)
                out.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Attempts to decode Base64 string directly into a Bitmap for instant UI validation & preview.
     */
    fun decodeBase64ToBitmap(base64Data: String): Bitmap? {
        if (base64Data.isBlank()) return null
        return try {
            val cleanBase64 = if (base64Data.contains(",")) {
                base64Data.substringAfter(",")
            } else {
                base64Data.trim()
            }
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT) ?: return null
            if (decodedBytes.isEmpty()) return null
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    fun deleteLogo(context: Context): Boolean {
        val file = getSchoolLogoFile(context)
        return if (file.exists()) file.delete() else true
    }

    fun loadSchoolLogoBitmap(context: Context, fallbackBase64: String? = null): Bitmap? {
        val file = getSchoolLogoFile(context)
        if (file.exists() && file.length() > 0) {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) return bitmap
            } catch (e: Exception) {
                // fallback to base64
            }
        }
        if (!fallbackBase64.isNullOrBlank()) {
            if (saveLogoFromBase64(context, fallbackBase64)) {
                return try {
                    BitmapFactory.decodeFile(file.absolutePath)
                } catch (e: Exception) {
                    null
                }
            }
        }
        return null
    }

    fun loadSchoolLogoImageBitmap(context: Context, fallbackBase64: String? = null): ImageBitmap? {
        return loadSchoolLogoBitmap(context, fallbackBase64)?.asImageBitmap()
    }
}

@Composable
fun rememberSchoolLogo(fallbackBase64: String? = null): ImageBitmap? {
    val context = LocalContext.current
    val logoFile = remember { SchoolLogoUtils.getSchoolLogoFile(context) }
    return remember(logoFile.exists(), logoFile.lastModified(), fallbackBase64) {
        SchoolLogoUtils.loadSchoolLogoImageBitmap(context, fallbackBase64)
    }
}
