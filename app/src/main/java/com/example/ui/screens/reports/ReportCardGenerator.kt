package com.example.ui.screens.reports

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.report.GeneratedReportCardData
import java.io.File

/**
 * ReportCardGenerator class using Android's PdfDocument API to draw A4 portrait content on a canvas,
 * starting with the School Header and Student Info structure.
 * Renders directly to a File object rather than a Compose view.
 */
class ReportCardGenerator(private val context: Context) {

    /**
     * Generates a fixed A4 Portrait PDF file for the given student report card data.
     */
    fun generateReportCardPdf(data: GeneratedReportCardData): File {
        return PdfReportCardGenerator.generatePdf(context, data)
    }

    /**
     * Saves the generated PDF file physically inside /Download/HCM Report Cards/ folder.
     * File name format: AcademicYear_Grade_StudentName_Month.pdf
     */
    fun saveToDownloads(sourcePdf: File, data: GeneratedReportCardData): Pair<Boolean, String> {
        return try {
            val sanitizedStudentName = data.student.name.replace("\\s+".toRegex(), "").replace("/", "_")
            val fileName = "${data.academicYear}_${data.gradeName}_${sanitizedStudentName}_${data.selectedMonth}.pdf"
            val subFolder = "HCM Report Cards"

            var mediaStoreSaved = false

            // On Android 10+ (API 29+), use ContentResolver / MediaStore to save explicitly inside the subfolder
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val resolver = context.contentResolver
                    val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$subFolder/"
                    val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                    // Delete existing entry if present to overwrite cleanly inside the subfolder
                    try {
                        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
                        val selectionArgs = arrayOf(fileName, relativePath)
                        resolver.delete(collection, selection, selectionArgs)
                    } catch (_: Exception) {}

                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }

                    val uri = resolver.insert(collection, values)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outStream ->
                            sourcePdf.inputStream().use { inStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                        mediaStoreSaved = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Always write physically to public Download/HCM Report Cards/ folder as well
            val downloadsFolder = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                subFolder
            )

            if (!downloadsFolder.exists()) {
                downloadsFolder.mkdirs()
            }

            val targetFile = File(downloadsFolder, fileName)
            sourcePdf.copyTo(targetFile, overwrite = true)

            // Trigger MediaScannerConnection for the file path so system file managers show it inside the folder
            MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                arrayOf("application/pdf")
            ) { _, _ -> }

            if ((targetFile.exists() && targetFile.length() > 0) || mediaStoreSaved) {
                Pair(true, "PDF saved successfully in 'Download/$subFolder/$fileName'")
            } else {
                Pair(false, "Failed to save PDF: File could not be written to '$subFolder'")
            }
        } catch (e: Exception) {
            Pair(false, "Error saving PDF file: ${e.localizedMessage}")
        }
    }

    companion object {
        fun generate(context: Context, data: GeneratedReportCardData): File {
            return ReportCardGenerator(context).generateReportCardPdf(data)
        }
    }
}
