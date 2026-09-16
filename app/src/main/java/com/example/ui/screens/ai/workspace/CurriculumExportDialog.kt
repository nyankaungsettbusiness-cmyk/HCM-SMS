package com.example.ui.screens.ai.workspace

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.export.*
import com.example.data.local.entity.SchoolSettingEntity
import com.example.ui.screens.reports.PdfFilePreview
import com.example.ui.screens.reports.printPdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class ExportTargetFormat(
    val title: String,
    val subtitle: String,
    val fileExt: String,
    val icon: ImageVector
) {
    STUDENT_EXAM_PDF(
        "Student Exam Paper",
        "Clean print-ready A4 test paper for students",
        "pdf",
        Icons.Default.Description
    ),
    TEACHER_GUIDE_PDF(
        "Teacher Marking Guide",
        "Full answer keys, rubrics & textbook citations",
        "pdf",
        Icons.Default.Checklist
    ),
    WORKSHEET_PDF(
        "Practice Worksheet",
        "Differentiated layout with student name & score boxes",
        "pdf",
        Icons.Default.Assignment
    ),
    EDITABLE_DOCX(
        "Microsoft Word (.docx)",
        "Fully editable OpenXML document (Word / WPS / Docs)",
        "docx",
        Icons.Default.Article
    )
}

/**
 * Compact, production-grade export modal for Hein Chan Myae curriculum outputs.
 * Provides unified generation, preview, printing, sharing, and downloads saving.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurriculumExportDialog(
    result: WorkspaceStructuredResult,
    schoolSettings: SchoolSettingEntity? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedFormat by remember { mutableStateOf(ExportTargetFormat.STUDENT_EXAM_PDF) }
    var isGenerating by remember { mutableStateOf(false) }
    var currentExportedFile by remember { mutableStateOf<File?>(null) }
    var previewPdfFile by remember { mutableStateOf<File?>(null) }
    var showPreviewModal by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Generate output file on format change
    fun generateSelectedOutput(onSuccess: (File) -> Unit = {}) {
        isGenerating = true
        errorMessage = null
        statusMessage = null

        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val fileResult: Result<File> = when (selectedFormat) {
                        ExportTargetFormat.STUDENT_EXAM_PDF -> {
                            CurriculumPdfGenerator(context).generatePdf(
                                result = result,
                                exportType = DocumentExportType.OFFICIAL_QUESTION_PAPER,
                                schoolSettings = schoolSettings
                            )
                        }
                        ExportTargetFormat.TEACHER_GUIDE_PDF -> {
                            CurriculumPdfGenerator(context).generatePdf(
                                result = result,
                                exportType = DocumentExportType.TEACHER_ANSWER_KEY,
                                schoolSettings = schoolSettings
                            )
                        }
                        ExportTargetFormat.WORKSHEET_PDF -> {
                            CurriculumPdfGenerator(context).generatePdf(
                                result = result,
                                exportType = DocumentExportType.WORKSHEET,
                                schoolSettings = schoolSettings
                            )
                        }
                        ExportTargetFormat.EDITABLE_DOCX -> {
                            CurriculumDocxExporter(context).exportToDocx(
                                result = result,
                                exportType = DocxExportType.STUDENT_QUESTION_PAPER,
                                schoolSettings = schoolSettings
                            )
                        }
                    }

                    withContext(Dispatchers.Main) {
                        isGenerating = false
                        if (fileResult.isSuccess) {
                            val file = fileResult.getOrThrow()
                            currentExportedFile = file
                            onSuccess(file)
                        } else {
                            errorMessage = fileResult.exceptionOrNull()?.localizedMessage ?: "Failed to generate document"
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isGenerating = false
                        errorMessage = e.localizedMessage ?: "An unexpected error occurred during generation."
                    }
                }
            }
        }
    }

    // Helper functions for action buttons
    fun triggerPreview() {
        if (selectedFormat == ExportTargetFormat.EDITABLE_DOCX) {
            // DOCX doesn't use PdfRenderer directly; generate student exam PDF for instant preview
            coroutineScope.launch {
                isGenerating = true
                val pdfRes = withContext(Dispatchers.IO) {
                    CurriculumPdfGenerator(context).generatePdf(
                        result = result,
                        exportType = DocumentExportType.OFFICIAL_QUESTION_PAPER,
                        schoolSettings = schoolSettings
                    )
                }
                isGenerating = false
                if (pdfRes.isSuccess) {
                    previewPdfFile = pdfRes.getOrThrow()
                    showPreviewModal = true
                } else {
                    errorMessage = pdfRes.exceptionOrNull()?.localizedMessage
                }
            }
        } else {
            generateSelectedOutput { file ->
                previewPdfFile = file
                showPreviewModal = true
            }
        }
    }

    fun triggerPrint() {
        generateSelectedOutput { file ->
            if (file.name.endsWith(".pdf", ignoreCase = true)) {
                printPdfFile(context, file, result.title)
                statusMessage = "Sent to printer adapter."
            } else {
                // If DOCX, print via PDF conversion
                coroutineScope.launch {
                    val pdfRes = withContext(Dispatchers.IO) {
                        CurriculumPdfGenerator(context).generatePdf(
                            result = result,
                            exportType = DocumentExportType.OFFICIAL_QUESTION_PAPER,
                            schoolSettings = schoolSettings
                        )
                    }
                    if (pdfRes.isSuccess) {
                        printPdfFile(context, pdfRes.getOrThrow(), result.title)
                        statusMessage = "Sent to printer adapter."
                    }
                }
            }
        }
    }

    fun triggerShare() {
        generateSelectedOutput { file ->
            try {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val mimeType = if (file.name.endsWith(".docx", ignoreCase = true)) {
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                } else {
                    "application/pdf"
                }

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "${result.title} (${result.grade} ${result.subject})")
                    clipData = ClipData.newRawUri(file.name, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share ${file.name}"))
                statusMessage = "Sharing ${file.name}..."
            } catch (e: Exception) {
                errorMessage = "Failed to share file: ${e.localizedMessage}"
            }
        }
    }

    fun triggerSaveToDownloads() {
        generateSelectedOutput { file ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val targetDir = File(downloadsDir, "HeinChanMyae_Curriculum").apply { mkdirs() }
                    val targetFile = File(targetDir, file.name)

                    FileInputStream(file).use { inStream ->
                        FileOutputStream(targetFile).use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    }

                    val mimeType = if (file.name.endsWith(".docx", ignoreCase = true)) {
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    } else {
                        "application/pdf"
                    }

                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath),
                        arrayOf(mimeType)
                    ) { _, _ -> }

                    withContext(Dispatchers.Main) {
                        statusMessage = "Saved to Downloads/HeinChanMyae_Curriculum/${file.name}"
                        Toast.makeText(context, "Saved to Downloads folder!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Could not save to Downloads: ${e.localizedMessage}"
                    }
                }
            }
        }
    }

    // Main Modal Dialog
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .testTag("curriculum_export_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Export Document",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${result.grade} • ${result.subject} • ${result.examType}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog", modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 2. Format Selection (Cards)
                Text(
                    text = "Select Document Format:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportTargetFormat.values().forEach { format ->
                        val isSelected = format == selectedFormat
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedFormat = format
                                    errorMessage = null
                                    statusMessage = null
                                }
                                .border(
                                    BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    RoundedCornerShape(10.dp)
                                ),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedFormat = format
                                        errorMessage = null
                                        statusMessage = null
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = format.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            color = if (format == ExportTargetFormat.EDITABLE_DOCX) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = ".${format.fileExt}",
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (format == ExportTargetFormat.EDITABLE_DOCX) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                    Text(
                                        text = format.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Document Summary Meta Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Document: ${result.title}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Total: ${result.totalMarks} Marks",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        val totalQuestions = result.sections.sumOf { it.questions.size }
                        Text(
                            text = "Sections: ${result.sections.size} | Questions: $totalQuestions | Duration: ${result.durationMinutes} Mins",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 4. Status / Error messages
                if (errorMessage != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Text(text = errorMessage ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                if (statusMessage != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF166534), modifier = Modifier.size(18.dp))
                            Text(text = statusMessage ?: "", style = MaterialTheme.typography.bodySmall, color = Color(0xFF166534))
                        }
                    }
                }

                // 5. Action Buttons (Preview, Print, Share, Save to Downloads)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Preview Button
                    OutlinedButton(
                        onClick = { triggerPreview() },
                        enabled = !isGenerating,
                        modifier = Modifier.testTag("btn_export_preview"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }

                    // Print Button
                    OutlinedButton(
                        onClick = { triggerPrint() },
                        enabled = !isGenerating,
                        modifier = Modifier.testTag("btn_export_print"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }

                    // Share Button
                    OutlinedButton(
                        onClick = { triggerShare() },
                        enabled = !isGenerating,
                        modifier = Modifier.testTag("btn_export_share"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }

                    // Save to Downloads Button
                    Button(
                        onClick = { triggerSaveToDownloads() },
                        enabled = !isGenerating,
                        modifier = Modifier.testTag("btn_export_save"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    // PDF Preview Fullscreen Modal (Reusing PdfFilePreview)
    if (showPreviewModal && previewPdfFile != null) {
        Dialog(
            onDismissRequest = { showPreviewModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Preview Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Document Print Preview",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "${result.title} (${result.grade} ${result.subject})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = {
                                    previewPdfFile?.let { printPdfFile(context, it, result.title) }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = "Print", tint = Color.White)
                            }
                            IconButton(
                                onClick = { showPreviewModal = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close preview", tint = Color.White)
                            }
                        }
                    }

                    // PDF Renderer
                    Box(modifier = Modifier.weight(1f)) {
                        PdfFilePreview(pdfFile = previewPdfFile!!)
                    }
                }
            }
        }
    }
}
