package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.BuildConfig
import com.example.data.util.AppUpdateInfo
import com.example.data.util.AppUpdateManager
import com.example.ui.components.AppUpdateDialog
import com.example.ui.util.rememberSchoolLogo
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(
    schoolName: String = "Hein Chan Myae",
    schoolLogoUri: String? = null
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var activeUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val logoBitmap = rememberSchoolLogo(schoolLogoUri)

        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 3.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (logoBitmap != null) {
                    Image(
                        bitmap = logoBitmap,
                        contentDescription = "School Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = schoolName.ifBlank { "Hein Chan Myae" },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Student Management System",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Academic & Institutional Portal",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }

            FilledTonalButton(
                onClick = {
                    if (!isCheckingUpdate) {
                        isCheckingUpdate = true
                        coroutineScope.launch {
                            try {
                                val result = AppUpdateManager.checkForUpdates(
                                    currentVersion = BuildConfig.VERSION_NAME
                                )
                                if (result.hasUpdate) {
                                    activeUpdateInfo = result
                                } else {
                                    Toast.makeText(
                                        context,
                                        "You are using the latest version (v${BuildConfig.VERSION_NAME})",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Failed to check updates: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                isCheckingUpdate = false
                            }
                        }
                    }
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(30.dp),
                enabled = !isCheckingUpdate
            ) {
                if (isCheckingUpdate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Checking...", fontSize = 11.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Check for Updates", fontSize = 11.sp)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "System Overview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "HCM-SMS is an offline-first management system built specifically for school administration at Hein Chan Myae Private High School, Myanmar. It enables secure local management of student records, academic assessments, attendance, holistic progress, and report card generation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = "Active Application Modules",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                val modules = listOf(
                    "Student Information & Class Enrollment",
                    "Teacher Profiles & Subject Assignments",
                    "Attendance Tracking & Absence Reports",
                    "Academic Exam Management & Examination Policy",
                    "Marks Entry, Automated Ranking & Grading Rules",
                    "HCM Assessment (Holistic Pillars & Remarks)",
                    "Report Card Generator & PDF Export",
                    "Academic Year Management & Class Promotion",
                    "User Authentication & Role-Based Permissions",
                    "School Policy & System Settings Center"
                )

                modules.forEach { moduleName ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = moduleName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = "Technical Architecture",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• Native Android (Kotlin & Jetpack Compose)\n• Architecture: Clean Architecture + MVVM\n• Local Storage: SQLite via Room ORM\n• Synchronization: Multi-Device Supabase Cloud Sync Engine",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = "Hein Chan Myae Education Group",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    activeUpdateInfo?.let { info ->
        AppUpdateDialog(
            updateInfo = info,
            onDismiss = { activeUpdateInfo = null }
        )
    }
}

