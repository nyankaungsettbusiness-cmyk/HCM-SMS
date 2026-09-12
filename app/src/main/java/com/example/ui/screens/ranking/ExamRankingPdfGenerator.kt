package com.example.ui.screens.ranking

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.ui.viewmodel.RankingResultData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExamRankingPdfGenerator {

    private const val PAGE_WIDTH = 842 // Landscape A4 for wide table or portrait A4
    private const val PAGE_HEIGHT = 595
    private const val MARGIN = 36f

    fun generatePdf(context: Context, schoolName: String, data: RankingResultData): File {
        val pdfDocument = PdfDocument()

        val headerPaint = Paint().apply {
            color = Color.rgb(26, 35, 126) // Deep Indigo
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.rgb(33, 33, 33)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val infoPaint = Paint().apply {
            color = Color.rgb(66, 66, 66)
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val tableHeaderBgPaint = Paint().apply {
            color = Color.rgb(40, 53, 147) // Dark Blue header background
            style = Paint.Style.FILL
        }

        val rowBgEvenPaint = Paint().apply {
            color = Color.rgb(245, 247, 250)
            style = Paint.Style.FILL
        }

        val rowBgOddPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val textPaint = Paint().apply {
            color = Color.rgb(33, 33, 33)
            textSize = 9.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val passTextPaint = Paint().apply {
            color = Color.rgb(46, 125, 50) // Green
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val failTextPaint = Paint().apply {
            color = Color.rgb(198, 40, 40) // Red
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.rgb(220, 224, 230)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }

        var currentPageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        var yPos = MARGIN + 20f

        // Draw Custom School Logo if present
        val logoBmp = com.example.ui.util.SchoolLogoUtils.loadSchoolLogoBitmap(context)
        if (logoBmp != null) {
            val logoWidth = 40f
            val logoHeight = 40f
            val destRect = RectF(PAGE_WIDTH - MARGIN - logoWidth, MARGIN, PAGE_WIDTH - MARGIN, MARGIN + logoHeight)
            canvas.drawBitmap(logoBmp, null, destRect, Paint().apply { isAntiAlias = true; isFilterBitmap = true })
        }

        // 1. Draw School Name & Title
        canvas.drawText(schoolName, MARGIN, yPos, headerPaint)
        yPos += 18f

        canvas.drawText("EXAM RANKING REPORT", MARGIN, yPos, titlePaint)
        yPos += 16f

        // Info bar
        val examName = data.assessment?.assessmentName ?: "Exam Ranking"
        val infoLine = "Academic Year: ${data.academicYear}   |   Grade: ${data.grade}   |   Exam: $examName"
        canvas.drawText(infoLine, MARGIN, yPos, infoPaint)
        yPos += 16f

        // Horizontal Line
        canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, borderPaint)
        yPos += 12f

        // Table Column Calculations
        val availableWidth = PAGE_WIDTH - (MARGIN * 2)
        val rankWidth = 45f
        val nameWidth = 140f
        val totalWidth = 55f
        val distWidth = 55f
        val resultWidth = 55f

        val fixedWidth = rankWidth + nameWidth + totalWidth + distWidth + resultWidth
        val subjectsCount = data.subjects.size.coerceAtLeast(1)
        val subjectColWidth = ((availableWidth - fixedWidth) / subjectsCount).coerceAtLeast(45f)

        fun drawTableHeader(c: Canvas, topY: Float): Float {
            val headerHeight = 22f
            c.drawRect(MARGIN, topY, PAGE_WIDTH - MARGIN, topY + headerHeight, tableHeaderBgPaint)

            var x = MARGIN + 6f
            c.drawText("Rank", x, topY + 15f, tableHeaderPaint)
            x += rankWidth

            c.drawText("Student Name", x, topY + 15f, tableHeaderPaint)
            x += nameWidth

            for (sub in data.subjects) {
                val shortSub = if (sub.length > 8) sub.take(7) + ".." else sub
                c.drawText(shortSub, x, topY + 15f, tableHeaderPaint)
                x += subjectColWidth
            }

            c.drawText("Total", x, topY + 15f, tableHeaderPaint)
            x += totalWidth

            c.drawText("Dist.", x, topY + 15f, tableHeaderPaint)
            x += distWidth

            c.drawText("Result", x, topY + 15f, tableHeaderPaint)

            return topY + headerHeight
        }

        yPos = drawTableHeader(canvas, yPos)

        val rowHeight = 20f
        val maxPageY = PAGE_HEIGHT - MARGIN - 30f

        for ((index, item) in data.rankingItems.withIndex()) {
            if (yPos + rowHeight > maxPageY) {
                pdfDocument.finishPage(page)
                currentPageNum++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = MARGIN + 20f
                canvas.drawText("EXAM RANKING REPORT (Contd.)", MARGIN, yPos, titlePaint)
                yPos += 16f
                yPos = drawTableHeader(canvas, yPos)
            }

            // Draw row background
            val bgPaint = if (index % 2 == 0) rowBgEvenPaint else rowBgOddPaint
            canvas.drawRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + rowHeight, bgPaint)
            canvas.drawRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + rowHeight, borderPaint)

            var x = MARGIN + 6f
            // Rank
            canvas.drawText("${item.rank}", x, yPos + 14f, textPaint)
            x += rankWidth

            // Name
            val nameText = if (item.displayName.length > 20) item.displayName.take(18) + ".." else item.displayName
            canvas.drawText(nameText, x, yPos + 14f, textPaint)
            x += nameWidth

            // Dynamic Subject Marks
            for (sub in data.subjects) {
                val markVal = item.subjectMarks[sub]
                val markStr = if (markVal != null) {
                    if (markVal % 1.0 == 0.0) markVal.toInt().toString() else String.format(Locale.US, "%.1f", markVal)
                } else "-"
                canvas.drawText(markStr, x, yPos + 14f, textPaint)
                x += subjectColWidth
            }

            // Total
            val totalStr = if (item.totalObtained % 1.0 == 0.0) item.totalObtained.toInt().toString() else String.format(Locale.US, "%.1f", item.totalObtained)
            canvas.drawText(totalStr, x, yPos + 14f, textPaint)
            x += totalWidth

            // Distinction
            canvas.drawText(item.distinctionText, x, yPos + 14f, textPaint)
            x += distWidth

            // Result
            val resPaint = if (item.isPassed) passTextPaint else failTextPaint
            canvas.drawText(item.resultStatus, x, yPos + 14f, resPaint)

            yPos += rowHeight
        }

        // Footer on last page
        yPos += 20f
        if (yPos + 40f > PAGE_HEIGHT - MARGIN) {
            pdfDocument.finishPage(page)
            currentPageNum++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPos = MARGIN + 30f
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val footerText = "Generated Date: $dateStr   |   Prepared By: School Administration"
        canvas.drawText(footerText, MARGIN, PAGE_HEIGHT - MARGIN, infoPaint)

        pdfDocument.finishPage(page)

        // Save file to cache directory
        val fileName = "Exam_Ranking_${data.grade}_${data.academicYear}.pdf"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return file
    }
}
