package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Modern Jetpack Compose Bar Chart Component with animation and gradient fill.
 */
@Composable
fun BarChart(
    data: Map<String, Float>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    maxValue: Float? = null,
    valueFormatter: (Float) -> String = { "%.1f".format(it) }
) {
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No chart data available", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        return
    }

    val calculatedMax = maxValue ?: (data.values.maxOrNull()?.takeIf { it > 0f } ?: 100f)
    var animationPlayed by remember { mutableStateOf(false) }
    val animateProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "barAnimation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    val primaryColor = barColor
    val gradientColor = primaryColor.copy(alpha = 0.4f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 16.dp, bottom = 8.dp)
        ) {
            val width = size.width
            val height = size.height
            val barCount = data.size
            val spacePerBar = width / barCount
            val barWidth = (spacePerBar * 0.5f).coerceAtMost(48.dp.toPx())

            // Grid Lines (3 background lines)
            for (i in 0..3) {
                val y = height - (height * (i / 3f))
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw Bars
            var index = 0
            data.forEach { (_, value) ->
                val barHeight = ((value / calculatedMax) * height * animateProgress).coerceAtMost(height)
                val x = (index * spacePerBar) + (spacePerBar - barWidth) / 2f
                val y = height - barHeight

                val brush = Brush.verticalGradient(
                    colors = listOf(primaryColor, gradientColor)
                )

                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                index++
            }
        }

        // Labels underneath
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.forEach { (label, value) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = valueFormatter(value),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Smooth Line Chart component with gradient area fill and data points.
 */
@Composable
fun LineChart(
    data: Map<String, Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.secondary,
    valueSuffix: String = "%"
) {
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No trend data available", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        return
    }

    val keys = data.keys.toList()
    val values = data.values.toList()
    val maxVal = (values.maxOrNull()?.takeIf { it > 0f } ?: 100f) * 1.1f
    val minVal = (values.minOrNull() ?: 0f) * 0.8f

    var animationPlayed by remember { mutableStateOf(false) }
    val animateProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "lineAnimation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    val primary = lineColor
    val fillGradient = Brush.verticalGradient(
        colors = listOf(primary.copy(alpha = 0.35f), Color.Transparent)
    )
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
        ) {
            val width = size.width
            val height = size.height
            val count = values.size

            if (count < 2) return@Canvas

            val stepX = width / (count - 1)

            // Horizontal Gridlines
            for (i in 0..2) {
                val y = height - (height * (i / 2f))
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val strokePath = Path()
            val fillPath = Path()

            val points = mutableListOf<Offset>()

            for (i in 0 until count) {
                val x = i * stepX
                val rawY = height - ((values[i] - minVal) / (maxVal - minVal)) * height
                val y = height - (height - rawY) * animateProgress
                points.add(Offset(x, y))

                if (i == 0) {
                    strokePath.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    val prev = points[i - 1]
                    val controlX1 = prev.x + (x - prev.x) / 2f
                    val controlY1 = prev.y
                    val controlX2 = prev.x + (x - prev.x) / 2f
                    val controlY2 = y
                    strokePath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                }
            }

            fillPath.lineTo(points.last().x, height)
            fillPath.close()

            drawPath(path = fillPath, brush = fillGradient)
            drawPath(path = strokePath, color = primary, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

            // Points
            points.forEach { point ->
                drawCircle(color = primary, radius = 5.dp.toPx(), center = point)
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = point)
            }
        }

        // Labels Below
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            keys.forEachIndexed { idx, label ->
                Text(
                    text = label,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Donut / Pie Chart with high resolution and legend.
 */
@Composable
fun PieDonutChart(
    data: Map<String, Int>,
    colors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primaryContainer
    ),
    modifier: Modifier = Modifier
) {
    if (data.isEmpty() || data.values.sum() == 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        return
    }

    val total = data.values.sum().toFloat()
    var animationPlayed by remember { mutableStateOf(false) }
    val animateProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "donutAnimation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                var colorIdx = 0

                data.forEach { (_, value) ->
                    val sweepAngle = (value / total) * 360f * animateProgress
                    val color = colors[colorIdx % colors.size]

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt)
                    )

                    startAngle += sweepAngle
                    colorIdx++
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${total.toInt()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Total",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }

        // Legend
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            var colorIdx = 0
            data.forEach { (label, value) ->
                val color = colors[colorIdx % colors.size]
                val pct = if (total > 0) (value / total) * 100f else 0f
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "$value (%.0f%%)".format(pct),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                colorIdx++
            }
        }
    }
}

/**
 * Circular Arc Progress Indicator for percentages (e.g. Today's Attendance or HCM Score).
 */
@Composable
fun CircularGaugeCard(
    percentage: Float,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animateProgress by animateFloatAsState(
        targetValue = if (animationPlayed) (percentage / 100f).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "gaugeAnimation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                val trackColor = color.copy(alpha = 0.2f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 10.dp.toPx()

                    drawArc(
                        color = trackColor,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    drawArc(
                        color = color,
                        startAngle = 135f,
                        sweepAngle = 270f * animateProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%.1f%%".format(percentage),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
