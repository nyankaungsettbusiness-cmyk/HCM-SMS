package com.example.ui.screens.holistic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.HolisticCategoryEntity
import com.example.data.local.entity.SgiCategoryEntity
import com.example.data.local.entity.StudentEntity
import com.example.ui.components.StarRatingWidget

@Composable
fun StudentProfileView(
    student: StudentEntity,
    period: String,
    academicYear: String,
    holisticCategories: List<HolisticCategoryEntity>,
    holisticRatings: Map<Long, Int>,
    sgiCategories: List<SgiCategoryEntity>,
    sgiRatings: Map<Long, Int>,
    positiveComments: String,
    areasForImprovement: String,
    generalComment: String,
    futureRecommendation: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val normGrade = student.gradeName.trim().uppercase()
    val studentLevel = when {
        normGrade == "KG" || normGrade.contains("KINDERGARTEN") -> "KINDERGARTEN"
        normGrade in listOf("G1", "G2", "G3", "G4", "G5", "GRADE 1", "GRADE 2", "GRADE 3", "GRADE 4", "GRADE 5", "PRIMARY 1", "PRIMARY 2", "PRIMARY 3", "PRIMARY 4", "PRIMARY 5") || normGrade.startsWith("PRIMARY") -> "PRIMARY"
        normGrade in listOf("G6", "G7", "G8", "G9", "GRADE 6", "GRADE 7", "GRADE 8", "GRADE 9") || normGrade.startsWith("SECONDARY") -> "SECONDARY"
        normGrade in listOf("G10", "G11", "G12", "GRADE 10", "GRADE 11", "GRADE 12") || normGrade.startsWith("HIGH") -> "HIGH_SCHOOL"
        else -> "PRIMARY"
    }

    val displayCategories = holisticCategories.filter { it.educationLevel.equals(studentLevel, ignoreCase = true) }

    val holisticRatedCount = holisticRatings.values.count { it > 0 }
    val holisticAvgStars = if (holisticRatedCount > 0) holisticRatings.values.filter { it > 0 }.average() else 0.0

    val sgiRatedCount = sgiRatings.values.count { it > 0 }
    val sgiAvgStars = if (sgiRatedCount > 0) sgiRatings.values.filter { it > 0 }.average() else 0.0

    val avatarColors = listOf(
        Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFE53935),
        Color(0xFF8E24AA), Color(0xFFFB8C00), Color(0xFF00ACC1)
    )
    val avatarBg = avatarColors[(student.photoAvatarIndex.toInt() - 1).coerceIn(0, avatarColors.size - 1)]

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(10.dp)
            .testTag("student_profile_view")
    ) {
        // 1. Header Student Card
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(10.dp)
            ) {
                // Photo / Avatar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(avatarBg)
                ) {
                    Text(
                        text = student.name.take(2).uppercase(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = student.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Roll #${student.rollNumber}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Code: ${student.studentCode} • Gender: ${student.gender}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Grade: ${student.gradeName} (${student.className}) • Year: $academicYear",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Assessment Period: $period",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // 2. Growth Summary Stats
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "HCM Holistic Assessment",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$holisticRatedCount / ${displayCategories.size} Evaluated",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Text(
                    text = String.format("%.1f ★", holisticAvgStars),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 3. Holistic Assessment Traits Breakdown
        Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Holistic Traits",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Holistic Trait Assessment",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                if (displayCategories.isEmpty()) {
                    Text("No active holistic categories configured for this level.", fontSize = 11.sp, color = Color.Gray)
                } else {
                    displayCategories.forEach { cat ->
                        val rating = holisticRatings[cat.id] ?: 0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.categoryName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (cat.description.isNotBlank()) {
                                    Text(
                                        text = cat.description,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            StarRatingWidget(
                                currentRating = rating,
                                maxStars = cat.maxStars,
                                readOnly = true,
                                showLabel = true
                            )
                        }
                    }
                }
            }
        }

        // 5. Teacher Comments
        Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "Teacher Remarks",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Teacher Evaluation Remarks",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                CommentBlock(title = "🌟 Positive Comments", content = positiveComments)
                Spacer(modifier = Modifier.height(6.dp))
                CommentBlock(title = "🎯 Areas for Improvement", content = areasForImprovement)
                Spacer(modifier = Modifier.height(6.dp))
                CommentBlock(title = "📝 General Comment", content = generalComment)
                Spacer(modifier = Modifier.height(6.dp))
                CommentBlock(title = "🚀 Future Recommendation", content = futureRecommendation)
            }
        }

        // 6. Academic Marks Placeholder Summary (Future Integration)
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Grade,
                    contentDescription = "Academic Marks",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "Academic Marks Integration",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Consolidated academic marks and term position will appear automatically in future Report Card generation.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentBlock(title: String, content: String) {
    Column {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (content.isNotBlank()) content else "No comments provided yet.",
                fontSize = 11.sp,
                color = if (content.isNotBlank()) MaterialTheme.colorScheme.onSurface else Color.Gray,
                modifier = Modifier.padding(6.dp)
            )
        }
    }
}
