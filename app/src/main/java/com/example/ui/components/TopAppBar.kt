package com.example.ui.components

import android.graphics.BitmapFactory
import java.io.File
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HcmTopAppBar(
    schoolName: String,
    academicYear: String,
    availableYears: List<String> = emptyList(),
    currentUser: UserEntity?,
    schoolLogoUri: String? = null,
    onOpenDrawer: () -> Unit,
    onAcademicYearChanged: (String) -> Unit,
    onLogoutClicked: () -> Unit,
    onRoleSwitchClicked: (UserRole) -> Unit
) {
    var yearMenuExpanded by remember { mutableStateOf(false) }
    var roleMenuExpanded by remember { mutableStateOf(false) }
    var showConnectionDialog by remember { mutableStateOf(false) }

    val connectionMode by com.example.data.sync.SyncManager.connectionMode.collectAsState()
    val isNetworkAvailable by com.example.data.sync.SyncManager.isNetworkAvailable.collectAsState()
    val lastSyncTime by com.example.data.sync.SyncManager.lastSyncTime.collectAsState()
    val syncState by com.example.data.sync.SyncManager.syncState.collectAsState()
    val pendingChangesCount by com.example.data.sync.SyncManager.pendingChangesCount.collectAsState()
    val isOffline = connectionMode == com.example.data.sync.SupabaseConnectionMode.OFFLINE || !isNetworkAvailable

    val isSuperAdmin = currentUser?.role == UserRole.SUPER_ADMIN

    val yearsList = (if (availableYears.isNotEmpty()) {
        (availableYears + academicYear).filter { it.isNotBlank() }.distinct()
    } else {
        listOf(academicYear).filter { it.isNotBlank() }
    }).filterNot { it == "2024-2025" || it == "2025-2026" }
        .ifEmpty { listOf("2026-2027") }

    // Format academic year to abbreviated form (e.g. "2025-2026" -> "25-26")
    fun formatShortYear(yr: String): String {
        val trimmed = yr.trim()
        val parts = trimmed.split("-", "/")
        return if (parts.size == 2 && parts[0].length >= 2 && parts[1].length >= 2) {
            "${parts[0].takeLast(2)}-${parts[1].takeLast(2)}"
        } else {
            trimmed
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 4.dp
    ) {
        Column {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Drawer"
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // School Logo Badge (Elevated crisp white container with ContentScale.Fit)
                        val logoBitmap = com.example.ui.util.rememberSchoolLogo(schoolLogoUri)

                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.5.dp),
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
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // School Name & Subtitle hierarchy
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = schoolName.ifEmpty { "Hein Chan Myae" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    letterSpacing = 0.2.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "School Management System",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    val context = LocalContext.current

                    // Single Unified Cloud Sync & Connection Status Badge (Compact & space-saving)
                    Surface(
                        onClick = { showConnectionDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            connectionMode == com.example.data.sync.SupabaseConnectionMode.SYNCING -> Color(0xFF1565C0).copy(alpha = 0.90f)
                            pendingChangesCount > 0 -> Color(0xFFE65100).copy(alpha = 0.95f)
                            isOffline -> Color(0xFF757575).copy(alpha = 0.90f)
                            else -> Color(0xFF2E7D32).copy(alpha = 0.90f)
                        },
                        contentColor = Color.White,
                        modifier = Modifier.padding(end = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            when {
                                connectionMode == com.example.data.sync.SupabaseConnectionMode.SYNCING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(11.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                }
                                pendingChangesCount > 0 -> {
                                    Icon(
                                        imageVector = Icons.Default.CloudQueue,
                                        contentDescription = "Pending changes",
                                        modifier = Modifier.size(13.dp),
                                        tint = Color.White
                                    )
                                    Text(
                                        text = "$pendingChangesCount",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                isOffline -> {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = "Offline Mode",
                                        modifier = Modifier.size(13.dp),
                                        tint = Color.White
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = "Cloud Synced",
                                        modifier = Modifier.size(13.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Academic Year Chip (Abbreviated, e.g. "25-26")
                    Box {
                        Surface(
                            onClick = {
                                if (isSuperAdmin) {
                                    yearMenuExpanded = true
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Academic year can only be modified by Super Admin.",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Year",
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = formatShortYear(academicYear),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (isSuperAdmin) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Year",
                                        modifier = Modifier.size(13.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Super Admin Only",
                                        modifier = Modifier.size(9.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        if (isSuperAdmin) {
                            DropdownMenu(
                                expanded = yearMenuExpanded,
                                onDismissRequest = { yearMenuExpanded = false }
                            ) {
                                yearsList.forEach { year ->
                                    DropdownMenuItem(
                                        text = { Text(year) },
                                        onClick = {
                                            android.util.Log.d("AcademicYearDebug", "UI_CLICK: TopAppBar item clicked for year='$year'")
                                            onAcademicYearChanged(year)
                                            yearMenuExpanded = false
                                        },
                                        leadingIcon = {
                                            if (year == academicYear) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(3.dp))

                    // User Profile / Role Selector Dropdown
                    Box {
                        IconButton(
                            onClick = { roleMenuExpanded = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentUser?.fullName?.take(1) ?: "U",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = roleMenuExpanded,
                            onDismissRequest = { roleMenuExpanded = false }
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
                                            roleMenuExpanded = false
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
                                    onLogoutClicked()
                                    roleMenuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            )
                        }
                    }
                }
            )

            // Offline-Only Warning Banner Strip (When disconnected)
            if (isOffline) {
                Surface(
                    color = Color(0xFFD84315),
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showConnectionDialog = true }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Offline Mode Warning",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Offline-Only Mode: Data is saved to local storage & will auto-sync when online.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Details",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54F)
                            )
                        )
                    }
                }
            }
        }
    }

    // Supabase Connection & Sync Diagnostic Dialog
    if (showConnectionDialog) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showConnectionDialog = false },
            icon = {
                Icon(
                    imageVector = when (connectionMode) {
                        com.example.data.sync.SupabaseConnectionMode.ONLINE -> Icons.Default.CloudDone
                        com.example.data.sync.SupabaseConnectionMode.SYNCING -> Icons.Default.CloudSync
                        com.example.data.sync.SupabaseConnectionMode.OFFLINE -> Icons.Default.CloudOff
                    },
                    contentDescription = null,
                    tint = when (connectionMode) {
                        com.example.data.sync.SupabaseConnectionMode.ONLINE -> Color(0xFF2E7D32)
                        com.example.data.sync.SupabaseConnectionMode.SYNCING -> Color(0xFF1565C0)
                        com.example.data.sync.SupabaseConnectionMode.OFFLINE -> Color(0xFFE65100)
                    },
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Supabase Cloud Status",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Status Badge Card
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (connectionMode) {
                            com.example.data.sync.SupabaseConnectionMode.ONLINE -> Color(0xFFE8F5E9)
                            com.example.data.sync.SupabaseConnectionMode.SYNCING -> Color(0xFFE3F2FD)
                            com.example.data.sync.SupabaseConnectionMode.OFFLINE -> Color(0xFFFFF3E0)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
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
                                    com.example.data.sync.SupabaseConnectionMode.ONLINE -> "Cloud Connected (Live Realtime)"
                                    com.example.data.sync.SupabaseConnectionMode.SYNCING -> "Synchronizing with Supabase..."
                                    com.example.data.sync.SupabaseConnectionMode.OFFLINE -> "Offline-Only Mode Active"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = when (connectionMode) {
                                    com.example.data.sync.SupabaseConnectionMode.ONLINE -> Color(0xFF1B5E20)
                                    com.example.data.sync.SupabaseConnectionMode.SYNCING -> Color(0xFF65558F)
                                    com.example.data.sync.SupabaseConnectionMode.OFFLINE -> Color(0xFFBF360C)
                                }
                            )
                        }
                    }

                    Text(
                        text = if (isOffline) {
                            "⚠️ The application is operating in offline mode. The built-in offline-first architecture allows full functionality (creating/editing students, teachers, marks, and attendance). All changes are stored locally in SQLite and will synchronize automatically when requested."
                        } else {
                            "⚡ Smart On-Demand Sync is active: Conserves Supabase API quotas and battery by eliminating redundant polling. Full synchronization runs on-demand or seamlessly batches local changes."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sync Mode:", fontSize = 11.sp, color = Color.Gray)
                        Text("Smart On-Demand", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cloud Endpoint:", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = com.example.data.remote.SupabaseClientManager.supabaseUrl
                                .removePrefix("https://")
                                .removePrefix("http://")
                                .trimEnd('/'),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Last Synced:", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = if (lastSyncTime > 0) {
                                val elapsedSec = (System.currentTimeMillis() - lastSyncTime) / 1000
                                if (elapsedSec < 60) "Just now" else "${elapsedSec / 60}m ago"
                            } else "On app launch",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pendingChangesCount > 0) {
                        OutlinedButton(
                            onClick = {
                                com.example.data.sync.SyncManager.clearAllPendingChanges(context)
                                android.widget.Toast.makeText(
                                    context,
                                    "Pending စာရင်း ($pendingChangesCount) ခုကို ရှင်းလင်းပြီးပါပြီ",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                showConnectionDialog = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFE65100)
                            )
                        ) {
                            Text("Pending ရှင်းမည်")
                        }
                    }
                    Button(
                        onClick = {
                            com.example.data.sync.SyncManager.triggerSyncAsync(context, forceImmediate = true)
                            showConnectionDialog = false
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Now")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
