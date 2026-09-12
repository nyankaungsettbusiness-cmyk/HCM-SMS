package com.example.ui.screens.reports

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Renders an actual generated PDF file using native Android PdfRenderer.
 * Converts PDF pages directly into high-DPI Bitmaps for crisp preview.
 */
@Composable
fun PdfFilePreview(
    pdfFile: File,
    modifier: Modifier = Modifier
) {
    val fileKey = remember(pdfFile) { "${pdfFile.absolutePath}_${if (pdfFile.exists()) pdfFile.lastModified() else 0L}" }
    var pageBitmaps by remember(pdfFile.absolutePath) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember(pdfFile.absolutePath) { mutableStateOf(pageBitmaps.isEmpty()) }
    var errorMessage by remember(pdfFile.absolutePath) { mutableStateOf<String?>(null) }

    LaunchedEffect(fileKey) {
        if (pageBitmaps.isEmpty()) {
            isLoading = true
        }
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                if (!pdfFile.exists() || pdfFile.length() == 0L) {
                    withContext(Dispatchers.Main) {
                        if (pageBitmaps.isEmpty()) {
                            errorMessage = "PDF file is empty or missing"
                        }
                        isLoading = false
                    }
                    return@withContext
                }

                val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fileDescriptor)
                val pageCount = renderer.pageCount
                val bitmaps = mutableListOf<Bitmap>()

                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)
                    // Render at 1.5x scale (892 x 1263) for high-DPI paper look while keeping memory light
                    val width = (page.width * 1.5f).toInt()
                    val height = (page.height * 1.5f).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    
                    // Fill white background before rendering PDF
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps.add(bitmap)
                }

                renderer.close()
                fileDescriptor.close()

                withContext(Dispatchers.Main) {
                    pageBitmaps = bitmaps
                    isLoading = false
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    if (pageBitmaps.isEmpty()) {
                        errorMessage = e.localizedMessage ?: "Failed to render PDF file"
                    }
                    isLoading = false
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF374151)),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = Color.White)
                Text("Rendering PDF pages from document...", color = Color.White, fontSize = 12.sp)
            }
        } else if (errorMessage != null) {
            Text("Error loading PDF: $errorMessage", color = Color.Red, fontSize = 12.sp)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(pageBitmaps) { index, bitmap ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(2.dp)),
                        shape = RoundedCornerShape(2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column {
                            // Badge header on page top
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1F2937))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PDF Page ${index + 1} of ${pageBitmaps.size} (A4 Portrait)",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "595 x 842 pt",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 10.sp
                                )
                            }

                            // Crisp PDF Page Image
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "PDF Page ${index + 1}",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Triggers native Android system print job directly from the generated PDF file.
 */
fun printPdfFile(context: Context, pdfFile: File, documentTitle: String) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: return

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val info = PrintDocumentInfo.Builder(documentTitle)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()

                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (destination == null) {
                    callback?.onWriteFailed("No destination file descriptor")
                    return
                }

                runCatching {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                }.onFailure {
                    callback?.onWriteFailed(it.localizedMessage)
                }
            }
        }

        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .build()

        printManager.print(documentTitle, printAdapter, attributes)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
