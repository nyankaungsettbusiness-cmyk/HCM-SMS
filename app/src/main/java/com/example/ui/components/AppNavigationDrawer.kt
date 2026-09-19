package com.example.ui.components

import android.graphics.BitmapFactory
import java.io.File
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserEntity

import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import com.example.data.local.entity.UserRole

sealed class ScreenRoute(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : ScreenRoute("dashboard", "Dashboard & Analytics", Icons.Default.Dashboard)
    object Students : ScreenRoute("students", "Students", Icons.Default.People)
    object Teachers : ScreenRoute("teachers", "Teachers", Icons.Default.Badge)
    object Attendance : ScreenRoute("attendance", "Attendance", Icons.Default.HowToReg)
    object Assessment : ScreenRoute("assessment", "Academic Exam Management", Icons.Default.Assignment)
    object MarksEntry : ScreenRoute("marks_entry", "Marks Entry", Icons.Default.FactCheck)
    object ExamRanking : ScreenRoute("exam_ranking", "Exam Ranking & Results", Icons.Default.Leaderboard)
    object HolisticAssessment : ScreenRoute("holistic_assessment", "HCM Assessment", Icons.Default.Psychology)
    object Reports : ScreenRoute("reports", "Report Cards", Icons.Default.Description)
    object AcademicYear : ScreenRoute("academic_year", "Academic Year & Promotion", Icons.Default.CalendarMonth)
    object AiAssistant : ScreenRoute("ai_assistant", "AI Teacher Assistant", Icons.Default.AutoAwesome)
    object PolicyCenter : ScreenRoute("policy_center", "School Policy Center", Icons.Default.Policy)
    object Users : ScreenRoute("users", "User & Role Management", Icons.Default.AdminPanelSettings)
    object Settings : ScreenRoute("settings", "System Settings", Icons.Default.Settings)
    object About : ScreenRoute("about", "About HCM-SMS", Icons.Default.Info)
}

@Composable
fun AppNavigationDrawerContent(
    currentRoute: String,
    currentUser: UserEntity?,
    schoolName: String,
    schoolLogoUri: String? = null,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    onLogoutClicked: () -> Unit = {},
    onRoleSwitchClicked: (UserRole) -> Unit = {}
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val context = LocalContext.current
                        val logoBitmap = com.example.ui.util.rememberSchoolLogo(schoolLogoUri)

                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp),
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
                                        contentDescription = "School Logo",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "HCM-SMS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            Text(
                                text = schoolName.ifEmpty { "Hein Chan Myae" },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    var userMenuExpanded by remember { mutableStateOf(false) }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { userMenuExpanded = true }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // User Icon Avatar Badge
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentUser?.fullName?.take(1) ?: "U",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Column {
                                    Text(
                                        text = currentUser?.fullName ?: "Guest",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = currentUser?.role?.displayName ?: "",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                }
                            }

                            Box {
                                IconButton(
                                    onClick = { userMenuExpanded = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "User Options",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = userMenuExpanded,
                                    onDismissRequest = { userMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = currentUser?.fullName ?: "Guest",
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = currentUser?.role?.displayName ?: "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = {},
                                        leadingIcon = {
                                            Icon(Icons.Default.AccountCircle, contentDescription = null)
                                        }
                                    )

                                    if (currentUser?.role == UserRole.SUPER_ADMIN) {
                                        HorizontalDivider()

                                        Text(
                                            text = " Switch Demo Role:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )

                                        UserRole.values().forEach { role ->
                                            DropdownMenuItem(
                                                text = { Text(role.displayName) },
                                                onClick = {
                                                    onRoleSwitchClicked(role)
                                                    userMenuExpanded = false
                                                },
                                                leadingIcon = {
                                                    if (currentUser?.role == role) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                                    } else {
                                                        Icon(Icons.Default.Badge, contentDescription = null)
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    HorizontalDivider()

                                    DropdownMenuItem(
                                        text = { Text("Logout", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            userMenuExpanded = false
                                            onCloseDrawer()
                                            onLogoutClicked()
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Items
            val navItems = listOf(
                ScreenRoute.Dashboard,
                ScreenRoute.Students,
                ScreenRoute.Teachers,
                ScreenRoute.Attendance,
                ScreenRoute.Assessment,
                ScreenRoute.MarksEntry,
                ScreenRoute.ExamRanking,
                ScreenRoute.HolisticAssessment,
                ScreenRoute.Reports,
                ScreenRoute.AcademicYear,
                ScreenRoute.AiAssistant,
                ScreenRoute.PolicyCenter,
                ScreenRoute.Users,
                ScreenRoute.Settings,
                ScreenRoute.About
            )

            val filteredItems = navItems.filter { screen ->
                when (currentUser?.role) {
                    com.example.data.local.entity.UserRole.SUPER_ADMIN,
                    com.example.data.local.entity.UserRole.ADMIN -> true
                    com.example.data.local.entity.UserRole.TEACHER -> screen !in listOf(
                        ScreenRoute.Users,
                        ScreenRoute.PolicyCenter,
                        ScreenRoute.Settings,
                        ScreenRoute.AcademicYear
                    )
                    com.example.data.local.entity.UserRole.OFFICE_STAFF -> screen !in listOf(
                        ScreenRoute.Users,
                        ScreenRoute.PolicyCenter,
                        ScreenRoute.Settings,
                        ScreenRoute.AiAssistant,
                        ScreenRoute.AcademicYear
                    )
                    null -> screen in listOf(
                        ScreenRoute.Dashboard,
                        ScreenRoute.Students,
                        ScreenRoute.About
                    )
                }
            }

            filteredItems.forEach { screen ->
                val isSelected = currentRoute == screen.route
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            text = screen.title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selected = isSelected,
                    onClick = {
                        onNavigate(screen.route)
                        onCloseDrawer()
                    },
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sidebar Supabase Cloud Connection & Sync Status Card
            val context = LocalContext.current
            val connectionMode by com.example.data.sync.SyncManager.connectionMode.collectAsState()
            val isNetworkAvailable by com.example.data.sync.SyncManager.isNetworkAvailable.collectAsState()
            val lastSyncTime by com.example.data.sync.SyncManager.lastSyncTime.collectAsState()
            val isOffline = connectionMode == com.example.data.sync.SupabaseConnectionMode.OFFLINE || !isNetworkAvailable

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = when (connectionMode) {
                    com.example.data.sync.SupabaseConnectionMode.ONLINE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    com.example.data.sync.SupabaseConnectionMode.SYNCING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    com.example.data.sync.SupabaseConnectionMode.OFFLINE -> Color(0xFFFFF3E0)
                },
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = when (connectionMode) {
                        com.example.data.sync.SupabaseConnectionMode.ONLINE -> Color(0xFF81C784).copy(alpha = 0.5f)
                        com.example.data.sync.SupabaseConnectionMode.SYNCING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        com.example.data.sync.SupabaseConnectionMode.OFFLINE -> Color(0xFFFFB74D)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when (connectionMode) {
                                        com.example.data.sync.SupabaseConnectionMode.ONLINE -> Color(0xFF2E7D32)
                                        com.example.data.sync.SupabaseConnectionMode.SYNCING -> Color(0xFF1565C0)
                                        com.example.data.sync.SupabaseConnectionMode.OFFLINE -> Color(0xFFE65100)
                                    }
                                )
                        )
                        Text(
                            text = when (connectionMode) {
                                com.example.data.sync.SupabaseConnectionMode.ONLINE -> "Supabase Live"
                                com.example.data.sync.SupabaseConnectionMode.SYNCING -> "Syncing with Cloud..."
                                com.example.data.sync.SupabaseConnectionMode.OFFLINE -> "Offline-Only Mode"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = when (connectionMode) {
                                com.example.data.sync.SupabaseConnectionMode.ONLINE -> Color(0xFF1B5E20)
                                com.example.data.sync.SupabaseConnectionMode.SYNCING -> Color(0xFF65558F)
                                com.example.data.sync.SupabaseConnectionMode.OFFLINE -> Color(0xFFBF360C)
                            }
                        )
                    }

                    Text(
                        text = if (isOffline) {
                            "Offline mode active. All operations are saved locally to Room SQLite and will auto-sync once connected."
                        } else {
                            "Real-time cloud synchronization active with automatic periodic updates."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lastSyncTime > 0) {
                                val elapsedSec = (System.currentTimeMillis() - lastSyncTime) / 1000
                                if (elapsedSec < 60) "Synced just now" else "Synced ${elapsedSec / 60}m ago"
                            } else "Synced on start",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )

                        FilledTonalButton(
                            onClick = {
                                com.example.data.sync.SyncManager.triggerSyncAsync(context, forceImmediate = true)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

