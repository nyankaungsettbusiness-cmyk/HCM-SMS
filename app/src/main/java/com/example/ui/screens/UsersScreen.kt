package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.*
import com.example.ui.viewmodel.AuthViewModel
import com.example.util.PasswordHasher
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    authViewModel: AuthViewModel,
    teachers: List<TeacherEntity> = emptyList(),
    onOpenDrawer: (() -> Unit)? = null
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val allUsers by authViewModel.allUsers.collectAsState()
    val filteredUsers by authViewModel.filteredUsers.collectAsState()
    val rolePermissions by authViewModel.rolePermissions.collectAsState()
    val loginHistory by authViewModel.loginHistory.collectAsState()
    val securityPolicy by authViewModel.securityPolicy.collectAsState()
    val auditLogs by authViewModel.auditLogs.collectAsState()

    val isAuthorized = currentUser?.role == UserRole.SUPER_ADMIN || currentUser?.role == UserRole.ADMIN
    if (!isAuthorized) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Access Denied",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "Access Restricted",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "User & Role Management is restricted to System Administrators only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Dialog States
    var showCreateUserDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserEntity?>(null) }
    var userForStatusChange by remember { mutableStateOf<UserEntity?>(null) }
    var userForResetPassword by remember { mutableStateOf<UserEntity?>(null) }
    var userForDelete by remember { mutableStateOf<UserEntity?>(null) }
    var showRemoveDefaultAdminDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Stats
    val totalUsersCount = allUsers.size
    val activeCount = allUsers.count { it.status == UserStatus.ACTIVE }
    val inactiveCount = allUsers.count { it.status == UserStatus.INACTIVE || it.status == UserStatus.SUSPENDED }
    val lockedCount = allUsers.count { it.status == UserStatus.LOCKED }
    val syncedCount = allUsers.count { !it.isDirty }
    val pendingCount = allUsers.count { it.isDirty }
    val syncStatus by authViewModel.syncStatus.collectAsState()
    val isSyncingUsers by authViewModel.isSyncingUsers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. Compact Module Header Bar (Responsive & overflow-safe)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "User & Roles",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            ),
                            maxLines = 1
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$totalUsersCount",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Accounts, Passwords & Access Control",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Cloud Sync Button with dynamic status indicator
                OutlinedButton(
                    onClick = {
                        authViewModel.syncUsersOnly(context) { (pushed, pulled) ->
                            Toast.makeText(
                                context,
                                "Account Sync complete: $pushed pushed, $pulled pulled from cloud",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    if (isSyncingUsers || syncStatus is com.example.data.sync.SyncStatus.Syncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Syncing", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium, maxLines = 1)
                    } else {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = "Sync Accounts to Supabase",
                            tint = if (pendingCount > 0) Color(0xFFE65100) else Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (pendingCount > 0) "$pendingCount" else "Sync",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (pendingCount > 0) Color(0xFFE65100) else Color(0xFF2E7D32),
                            maxLines = 1
                        )
                    }
                }

                if (pendingCount > 0) {
                    IconButton(
                        onClick = {
                            authViewModel.clearPendingSyncFlags(context)
                            Toast.makeText(context, "Pending flags reset to Synced", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = "Mark all as synced",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Add / Create Account Primary Action Button
                Button(
                    onClick = { showCreateUserDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.5.dp, pressedElevation = 3.dp)
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = "Create Account",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Create",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp,
                        maxLines = 1
                    )
                }
            }
        }

        // Default Admin Notice & Removal Card (if default 'admin' exists alongside custom admin)
        val defaultAdminExists = allUsers.any { it.username.equals("admin", ignoreCase = true) }
        val customAdminsCount = allUsers.count { 
            !it.username.equals("admin", ignoreCase = true) && (it.role == UserRole.ADMIN || it.role == UserRole.SUPER_ADMIN) 
        }
        if (defaultAdminExists && customAdminsCount > 0) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "First-Time Setup Admin ('admin') is still active alongside your account.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Button(
                        onClick = { showRemoveDefaultAdminDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Delete 'admin'", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Compact Header Stats Strip
        CompactUserStatsHeader(
            totalCount = totalUsersCount,
            activeCount = activeCount,
            syncedCount = syncedCount,
            pendingCount = pendingCount,
            onClearPending = {
                authViewModel.clearPendingSyncFlags(context)
                Toast.makeText(context, "Pending sync status cleared", Toast.LENGTH_SHORT).show()
            }
        )

        // 3. Compact Module Tabs
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Accounts (${filteredUsers.size})", fontSize = 11.5.sp, maxLines = 1) },
                icon = { Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Permissions", fontSize = 11.5.sp, maxLines = 1) },
                icon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("History", fontSize = 11.5.sp, maxLines = 1) },
                icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedTabIndex == 3,
                onClick = { selectedTabIndex = 3 },
                text = { Text("Policy", fontSize = 11.5.sp, maxLines = 1) },
                icon = { Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        // 4. Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTabIndex) {
                0 -> UserAccountsTabContent(
                    authViewModel = authViewModel,
                    currentUser = currentUser,
                    users = filteredUsers,
                    onCreateUser = { showCreateUserDialog = true },
                    onEditUser = { editingUser = it },
                    onChangeStatus = { userForStatusChange = it },
                    onResetPassword = { userForResetPassword = it },
                    onDeleteUser = { userForDelete = it }
                )
                1 -> RolePermissionsTabContent(
                    authViewModel = authViewModel,
                    rolePermissions = rolePermissions
                )
                2 -> LoginHistoryTabContent(
                    loginHistory = loginHistory,
                    auditLogs = auditLogs
                )
                3 -> SecurityPolicyTabContent(
                    securityPolicy = securityPolicy,
                    onSavePolicy = { newPolicy ->
                        authViewModel.saveSecurityPolicy(newPolicy)
                        Toast.makeText(context, "Security Policy Updated Successfully!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // --- DIALOGS ---

    // 1. Create User Dialog
    if (showCreateUserDialog) {
        CreateUserDialog(
            allUsers = allUsers,
            teachers = teachers,
            minPasswordLength = securityPolicy?.minPasswordLength ?: 6,
            onDismiss = { showCreateUserDialog = false },
            onCreate = { username, fullName, role, status, email, phone, teacherName, staffName, pass, mustChange ->
                authViewModel.createUser(
                    username = username,
                    fullName = fullName,
                    role = role,
                    email = email,
                    phone = phone,
                    status = status,
                    linkedTeacherName = teacherName,
                    linkedStaffName = staffName,
                    initialPassword = pass,
                    mustChangePassword = mustChange
                ) { result ->
                    result.onSuccess {
                        Toast.makeText(context, "Account '@$username' created successfully!", Toast.LENGTH_LONG).show()
                        showCreateUserDialog = false
                    }.onFailure { err ->
                        Toast.makeText(context, err.message ?: "Failed to create account", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // 2. Edit User Dialog
    editingUser?.let { user ->
        EditUserDialog(
            user = user,
            allUsers = allUsers,
            teachers = teachers,
            currentUser = currentUser,
            minPasswordLength = securityPolicy?.minPasswordLength ?: 6,
            onDismiss = { editingUser = null },
            onSave = { updatedUser, newPassword, forceChange ->
                authViewModel.updateUser(updatedUser) { updateRes ->
                    updateRes.onSuccess {
                        if (!newPassword.isNullOrBlank()) {
                            authViewModel.resetPassword(user.id, newPassword, forceChange) { passRes ->
                                passRes.onSuccess {
                                    Toast.makeText(context, "Account '@${updatedUser.username}' and password updated!", Toast.LENGTH_SHORT).show()
                                }.onFailure { err ->
                                    Toast.makeText(context, "Account updated, but password error: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Updated account '@${updatedUser.username}'", Toast.LENGTH_SHORT).show()
                        }
                        editingUser = null
                    }.onFailure { err ->
                        Toast.makeText(context, err.message ?: "Failed to update account", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // 3. Change Status Dialog
    userForStatusChange?.let { user ->
        ChangeStatusDialog(
            user = user,
            onDismiss = { userForStatusChange = null },
            onConfirmStatus = { newStatus ->
                authViewModel.setUserStatus(user.id, newStatus) { res ->
                    res?.onSuccess {
                        Toast.makeText(context, "Changed status of '@${user.username}' to ${newStatus.displayName}", Toast.LENGTH_SHORT).show()
                    }?.onFailure { err ->
                        Toast.makeText(context, err.message ?: "Failed to update status", Toast.LENGTH_LONG).show()
                    }
                }
                userForStatusChange = null
            }
        )
    }

    // 4. Reset Password Dialog
    userForResetPassword?.let { user ->
        ResetPasswordDialog(
            user = user,
            minPasswordLength = securityPolicy?.minPasswordLength ?: 6,
            onDismiss = { userForResetPassword = null },
            onConfirmReset = { newPass, forceChange ->
                authViewModel.resetPassword(user.id, newPass, forceChange) { res ->
                    res.onSuccess {
                        Toast.makeText(context, "Password for '@${user.username}' successfully changed!", Toast.LENGTH_LONG).show()
                    }.onFailure { err ->
                        Toast.makeText(context, err.message ?: "Failed to reset password", Toast.LENGTH_LONG).show()
                    }
                }
                userForResetPassword = null
            }
        )
    }

    // 5. Delete User Confirmation Dialog
    userForDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userForDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Account (@${user.username})", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete the account for '${user.fullName}' (@${user.username})?\n\n" +
                            "This account will be deactivated and marked as deleted. Historical marks and logs remain preserved."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.softDeleteUser(user.id) { res ->
                            res?.onSuccess { verification ->
                                val msg = when (verification) {
                                    is com.example.data.sync.DeletionVerificationResult.VerifiedPurged ->
                                        "✅ Account '@${user.username}' permanently deleted from Supabase Cloud"
                                    is com.example.data.sync.DeletionVerificationResult.VerifiedTombstoned ->
                                        "✅ Account '@${user.username}' soft-deleted & verified on Supabase Cloud"
                                    is com.example.data.sync.DeletionVerificationResult.ServerRejectedOrIgnored ->
                                        "⚠️ Supabase Warning: Server rejected or ignored deletion (${verification.reason})"
                                    is com.example.data.sync.DeletionVerificationResult.OfflineQueued ->
                                        "📶 Account deleted locally. Queued for Supabase sync."
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }?.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed to delete account", Toast.LENGTH_LONG).show()
                            }
                        }
                        userForDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Account")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userForDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 6. Remove Default Admin Account Confirmation Dialog
    if (showRemoveDefaultAdminDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDefaultAdminDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Default 'admin' Account", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to completely remove the setup 'admin' account?\n\n" +
                    "Your personal admin account is confirmed active. Once deleted, login using default 'admin' / 'Password123!' will be permanently disabled on both this device and Supabase Cloud."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.removeDefaultAdmin { res ->
                            res.onSuccess { removed ->
                                if (removed) {
                                    Toast.makeText(context, "✅ Default 'admin' account deleted successfully.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Default 'admin' was not found.", Toast.LENGTH_SHORT).show()
                                }
                            }.onFailure { err ->
                                Toast.makeText(context, "Failed: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        showRemoveDefaultAdminDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete 'admin'")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRemoveDefaultAdminDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =========================================================
// COMPACT STATS HEADER
// =========================================================

@Composable
private fun CompactUserStatsHeader(
    totalCount: Int,
    activeCount: Int,
    syncedCount: Int,
    pendingCount: Int,
    onClearPending: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactStatPill(label = "Total", value = totalCount.toString(), color = MaterialTheme.colorScheme.primary)
            VerticalDivider(modifier = Modifier.height(16.dp))
            CompactStatPill(label = "Active", value = activeCount.toString(), color = Color(0xFF2E7D32))
            VerticalDivider(modifier = Modifier.height(16.dp))
            CompactStatPill(label = "Synced", value = syncedCount.toString(), color = Color(0xFF1565C0))
            VerticalDivider(modifier = Modifier.height(16.dp))
            CompactStatPill(
                label = "Pending",
                value = pendingCount.toString(),
                color = if (pendingCount > 0) Color(0xFFE65100) else Color(0xFF757575),
                modifier = if (pendingCount > 0) {
                    Modifier.clickable {
                        onClearPending()
                    }
                } else Modifier
            )
        }
    }
}

@Composable
private fun CompactStatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Text(value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = color, fontSize = 12.sp)
    }
}

// =========================================================
// TAB 1: USER ACCOUNTS (COMPACT DENSE LIST)
// =========================================================

@Composable
private fun UserAccountsTabContent(
    authViewModel: AuthViewModel,
    currentUser: UserEntity?,
    users: List<UserEntity>,
    onCreateUser: () -> Unit,
    onEditUser: (UserEntity) -> Unit,
    onChangeStatus: (UserEntity) -> Unit,
    onResetPassword: (UserEntity) -> Unit,
    onDeleteUser: (UserEntity) -> Unit
) {
    val searchQuery by authViewModel.searchQuery.collectAsState()
    val selectedRoleFilter by authViewModel.selectedRoleFilter.collectAsState()
    val selectedStatusFilter by authViewModel.selectedStatusFilter.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compact Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { authViewModel.setSearchQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by name, @username, or email...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { authViewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
        )

        // Compact Horizontal Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedRoleFilter == null && selectedStatusFilter == null,
                onClick = {
                    authViewModel.setRoleFilter(null)
                    authViewModel.setStatusFilter(null)
                },
                label = { Text("All (${users.size})", fontSize = 11.sp) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(30.dp)
            )

            UserRole.values().forEach { role ->
                FilterChip(
                    selected = selectedRoleFilter == role,
                    onClick = {
                        authViewModel.setRoleFilter(if (selectedRoleFilter == role) null else role)
                    },
                    label = { Text(role.displayName, fontSize = 11.sp) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                )
            }

            UserStatus.values().forEach { status ->
                FilterChip(
                    selected = selectedStatusFilter == status,
                    onClick = {
                        authViewModel.setStatusFilter(if (selectedStatusFilter == status) null else status)
                    },
                    label = { Text(status.displayName, fontSize = 11.sp) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                )
            }
        }

        // Users List (Small compact dense cards)
        if (users.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.PersonOff,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                    Text(
                        "No accounts found matching search/filter.",
                        textAlign = TextAlign.Center,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onCreateUser,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 76.dp)
            ) {
                items(users, key = { it.id }) { user ->
                    CompactUserAccountCard(
                        user = user,
                        currentUser = currentUser,
                        onEdit = { onEditUser(user) },
                        onChangeStatus = { onChangeStatus(user) },
                        onResetPassword = { onResetPassword(user) },
                        onDelete = { onDeleteUser(user) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactUserAccountCard(
    user: UserEntity,
    currentUser: UserEntity?,
    onEdit: () -> Unit,
    onChangeStatus: () -> Unit,
    onResetPassword: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (user.status) {
        UserStatus.ACTIVE -> Color(0xFF2E7D32)
        UserStatus.INACTIVE -> Color(0xFF757575)
        UserStatus.LOCKED -> Color(0xFFD32F2F)
        UserStatus.SUSPENDED -> Color(0xFFE65100)
    }

    val roleBg = when (user.role) {
        UserRole.SUPER_ADMIN -> MaterialTheme.colorScheme.primaryContainer
        UserRole.ADMIN -> MaterialTheme.colorScheme.secondaryContainer
        UserRole.TEACHER -> MaterialTheme.colorScheme.tertiaryContainer
        UserRole.OFFICE_STAFF -> MaterialTheme.colorScheme.surfaceVariant
    }

    val roleTextColor = when (user.role) {
        UserRole.SUPER_ADMIN -> MaterialTheme.colorScheme.onPrimaryContainer
        UserRole.ADMIN -> MaterialTheme.colorScheme.onSecondaryContainer
        UserRole.TEACHER -> MaterialTheme.colorScheme.onTertiaryContainer
        UserRole.OFFICE_STAFF -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val canDelete = remember(currentUser, user) {
        if (currentUser == null) false
        else if (currentUser.role != UserRole.SUPER_ADMIN && currentUser.role != UserRole.ADMIN) false
        else if (currentUser.id == user.id) false // cannot delete own account
        else if (user.role == UserRole.SUPER_ADMIN && currentUser.role != UserRole.SUPER_ADMIN) false // only Super Admin can delete Super Admin
        else true
    }

    var showMenu by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Profile Header with Role, Status & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Avatar (34dp)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.fullName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Name, Role & Status
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = user.fullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = roleBg
                        ) {
                            Text(
                                text = user.role.displayName,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = roleTextColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }

                        // Status dot & text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = user.status.displayName,
                                fontSize = 9.sp,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (user.linkedTeacherName.isNotBlank() || user.linkedStaffName.isNotBlank() || user.email.isNotBlank()) {
                        val subText = when {
                            user.linkedTeacherName.isNotBlank() -> "Teacher: ${user.linkedTeacherName}"
                            user.linkedStaffName.isNotBlank() -> "Staff: ${user.linkedStaffName}"
                            else -> user.email
                        }
                        Text(
                            text = subText,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Quick Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Account",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onChangeStatus,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "Change Status",
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Account",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Row 2: Clean Credentials & Password Management Card
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Username & Password Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Username
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text("Username:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "@${user.username}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(user.username))
                                    Toast.makeText(context, "Copied username '@${user.username}'", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy Username",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }

                        // Password Field
                        val currentPass = user.managedPassword.ifBlank { "Password123!" }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text("Password:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (isPasswordVisible) currentPass else "••••••••",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (isPasswordVisible) androidx.compose.ui.text.font.FontFamily.Monospace else androidx.compose.ui.text.font.FontFamily.Default,
                                color = if (isPasswordVisible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                            )
                            if (user.managedPassword.isBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text("Default", fontSize = 8.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(horizontal = 3.dp))
                                }
                            }

                            // Eye toggle button
                            IconButton(
                                onClick = { isPasswordVisible = !isPasswordVisible },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isPasswordVisible) "Hide Password" else "Show Password",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            // Copy Password button
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(currentPass))
                                    Toast.makeText(context, "Copied password for '@${user.username}'", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy Password",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }

                    // Credentials Actions Row: Quick Password Reset & Slip Copy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = onResetPassword,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset / Set Password", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                val slip = "Hein Chan Myae Login Credentials:\nUsername: ${user.username}\nPassword: ${user.managedPassword.ifBlank { "Password123!" }}\nRole: ${user.role.displayName}"
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(slip))
                                Toast.makeText(context, "Copied login slip for '@${user.username}' to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy Login Slip", fontSize = 9.5.sp)
                        }
                    }
                }
            }

            // Row 3: Cloud Sync status badge & UUID identifier
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val isSynced = !user.isDirty && user.uuid.isNotBlank()
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isSynced) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (isSynced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = if (isSynced) Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = if (isSynced) "Cloud Synced" else "Local / Pending Sync",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSynced) Color(0xFF1B5E20) else Color(0xFFBF360C)
                        )
                    }
                }

                if (user.uuid.isNotBlank()) {
                    Text(
                        text = "UUID: #${user.uuid.take(8)}",
                        fontSize = 8.5.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// =========================================================
// CREATE USER DIALOG (CLEAR & FUNCTIONAL)
// =========================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateUserDialog(
    allUsers: List<UserEntity>,
    teachers: List<TeacherEntity> = emptyList(),
    minPasswordLength: Int = 6,
    onDismiss: () -> Unit,
    onCreate: (username: String, fullName: String, role: UserRole, status: UserStatus, email: String, phone: String, teacherName: String, staffName: String, pass: String, mustChange: Boolean) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.TEACHER) }
    var selectedStatus by remember { mutableStateOf(UserStatus.ACTIVE) }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var linkedTeacherName by remember { mutableStateOf("") }
    var linkedStaffName by remember { mutableStateOf("") }
    var mustChangePassword by remember { mutableStateOf(false) }
    var teacherDropdownExpanded by remember { mutableStateOf(false) }

    val cleanUsername = username.trim().lowercase()
    val isDuplicate = remember(cleanUsername, allUsers) {
        cleanUsername.isNotBlank() && allUsers.any { it.username.equals(cleanUsername, ignoreCase = true) }
    }

    val passwordMismatch = password != confirmPassword
    val passwordTooShort = password.length < minPasswordLength
    val isFormValid = cleanUsername.isNotBlank() && !isDuplicate && fullName.isNotBlank() && !passwordMismatch && !passwordTooShort

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Name Field
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Display Name / Full Name *") },
                    placeholder = { Text("e.g. Tr. Daw Nu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Username Field (Must be visible and validated)
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.filter { char -> char.isLetterOrDigit() || char == '_' || char == '.' } },
                    label = { Text("Username *") },
                    placeholder = { Text("e.g. dawnu") },
                    isError = isDuplicate || (username.isNotBlank() && cleanUsername.isBlank()),
                    supportingText = {
                        if (isDuplicate) {
                            Text("Username already taken!", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                        } else {
                            Text("Letters, numbers, _ or . (case-insensitive login)", fontSize = 10.sp)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Password Helper Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account Password", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                password = "Password123!"
                                confirmPassword = "Password123!"
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Password123!", fontSize = 10.5.sp)
                        }
                        TextButton(
                            onClick = {
                                val generated = PasswordHasher.generateTempPassword()
                                password = generated
                                confirmPassword = generated
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("🎲 Random", fontSize = 10.5.sp)
                        }
                    }
                }

                // Password Field (Masked with Visibility Toggle)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password *") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    singleLine = true,
                    isError = passwordTooShort && password.isNotEmpty(),
                    supportingText = {
                        if (passwordTooShort && password.isNotEmpty()) {
                            Text("Minimum $minPasswordLength characters required", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Confirm Password Field (Masked with Visibility Toggle)
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password *") },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    singleLine = true,
                    isError = passwordMismatch && confirmPassword.isNotEmpty(),
                    supportingText = {
                        if (passwordMismatch && confirmPassword.isNotEmpty()) {
                            Text("Passwords do not match!", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Role Selector (Compact Row)
                Text("Role *", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    UserRole.values().forEach { r ->
                        FilterChip(
                            selected = selectedRole == r,
                            onClick = { selectedRole = r },
                            label = { Text(r.displayName, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }

                // Status Selector (Active vs Inactive)
                Text("Status *", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStatus == UserStatus.ACTIVE,
                        onClick = { selectedStatus = UserStatus.ACTIVE },
                        label = { Text("Active", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    )
                    FilterChip(
                        selected = selectedStatus == UserStatus.INACTIVE,
                        onClick = { selectedStatus = UserStatus.INACTIVE },
                        label = { Text("Inactive", fontSize = 11.sp) },
                        leadingIcon = {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF757575)))
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    )
                }

                // Linked Profile if TEACHER
                if (selectedRole == UserRole.TEACHER) {
                    Text("Linked Teacher Profile", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    ExposedDropdownMenuBox(
                        expanded = teacherDropdownExpanded,
                        onExpandedChange = { teacherDropdownExpanded = !teacherDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = linkedTeacherName.ifEmpty { "Select Teacher..." },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teacherDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = teacherDropdownExpanded,
                            onDismissRequest = { teacherDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None (Unlinked)") },
                                onClick = {
                                    linkedTeacherName = ""
                                    teacherDropdownExpanded = false
                                }
                            )
                            teachers.forEach { teacher ->
                                val label = "${teacher.fullName} (${teacher.teacherCode})"
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        linkedTeacherName = label
                                        if (fullName.isBlank()) {
                                            fullName = teacher.fullName
                                        }
                                        teacherDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else if (selectedRole == UserRole.OFFICE_STAFF) {
                    OutlinedTextField(
                        value = linkedStaffName,
                        onValueChange = { linkedStaffName = it },
                        label = { Text("Linked Staff Name (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Optional Email & Phone
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    placeholder = { Text(if (cleanUsername.isNotBlank()) "$cleanUsername@hcm.edu.mm" else "user@hcm.edu.mm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Require Password Change on First Login
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Checkbox(
                        checked = mustChangePassword,
                        onCheckedChange = { mustChangePassword = it }
                    )
                    Text("Require password change on first login", fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isFormValid) {
                        val effectiveEmail = email.ifBlank { if (cleanUsername.isNotBlank()) "$cleanUsername@hcm.edu.mm" else "" }
                        onCreate(
                            cleanUsername, fullName, selectedRole, selectedStatus, effectiveEmail, phone,
                            linkedTeacherName, linkedStaffName, password, mustChangePassword
                        )
                    }
                },
                enabled = isFormValid
            ) {
                Text("Create Account")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// =========================================================
// EDIT USER DIALOG (WITH SEPARATE PASSWORD CHANGE & USERNAME VISIBILITY)
// =========================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditUserDialog(
    user: UserEntity,
    allUsers: List<UserEntity>,
    teachers: List<TeacherEntity> = emptyList(),
    currentUser: UserEntity?,
    minPasswordLength: Int = 6,
    onDismiss: () -> Unit,
    onSave: (UserEntity, String?, Boolean) -> Unit
) {
    var username by remember { mutableStateOf(user.username) }
    var fullName by remember { mutableStateOf(user.fullName) }
    var email by remember { mutableStateOf(user.email) }
    var phone by remember { mutableStateOf(user.phone) }
    var selectedRole by remember { mutableStateOf(user.role) }
    var selectedStatus by remember { mutableStateOf(user.status) }
    var linkedTeacherName by remember { mutableStateOf(user.linkedTeacherName) }
    var linkedStaffName by remember { mutableStateOf(user.linkedStaffName) }
    var teacherDropdownExpanded by remember { mutableStateOf(false) }

    // Password Change Section
    var changePasswordNow by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var forceChangePasswordOnLogin by remember { mutableStateOf(true) }

    val cleanUsername = username.trim().lowercase()
    val isDuplicate = remember(cleanUsername, allUsers, user.id) {
        cleanUsername.isNotBlank() && allUsers.any { it.id != user.id && it.username.equals(cleanUsername, ignoreCase = true) }
    }

    val passwordMismatch = changePasswordNow && (newPassword != confirmNewPassword)
    val passwordTooShort = changePasswordNow && (newPassword.length < minPasswordLength)
    val isFormValid = cleanUsername.isNotBlank() && !isDuplicate && fullName.isNotBlank() && (!changePasswordNow || (!passwordMismatch && !passwordTooShort))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Account (@${user.username})", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Username Field (Visible and Editable)
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.filter { char -> char.isLetterOrDigit() || char == '_' || char == '.' } },
                    label = { Text("Username *") },
                    isError = isDuplicate,
                    supportingText = {
                        if (isDuplicate) {
                            Text("Username already taken by another account!", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Full Name / Display Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Display Name / Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Role Selector
                Text("Account Role", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    UserRole.values().forEach { r ->
                        FilterChip(
                            selected = selectedRole == r,
                            onClick = { selectedRole = r },
                            label = { Text(r.displayName, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }

                // Status Selector
                Text("Account Status", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    UserStatus.values().forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = st },
                            label = { Text(st.displayName, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }

                // Linked Profile if TEACHER
                if (selectedRole == UserRole.TEACHER) {
                    Text("Linked Teacher Profile", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    ExposedDropdownMenuBox(
                        expanded = teacherDropdownExpanded,
                        onExpandedChange = { teacherDropdownExpanded = !teacherDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = linkedTeacherName.ifEmpty { "Select Teacher Profile..." },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teacherDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = teacherDropdownExpanded,
                            onDismissRequest = { teacherDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None (Unlinked)") },
                                onClick = {
                                    linkedTeacherName = ""
                                    teacherDropdownExpanded = false
                                }
                            )
                            teachers.forEach { teacher ->
                                val label = "${teacher.fullName} (${teacher.teacherCode})"
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        linkedTeacherName = label
                                        teacherDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else if (selectedRole == UserRole.OFFICE_STAFF) {
                    OutlinedTextField(
                        value = linkedStaffName,
                        onValueChange = { linkedStaffName = it },
                        label = { Text("Linked Staff Profile") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Email & Phone
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Separate Change Password Action
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { changePasswordNow = !changePasswordNow }
                        ) {
                            Checkbox(
                                checked = changePasswordNow,
                                onCheckedChange = { changePasswordNow = it }
                            )
                            Text("Change / Reset Password", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        if (changePasswordNow) {
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = { Text("New Password *") },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    Row {
                                        IconButton(onClick = { newPassword = PasswordHasher.generateTempPassword(); confirmNewPassword = newPassword }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Generate Random", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                isError = passwordTooShort && newPassword.isNotEmpty(),
                                supportingText = {
                                    if (passwordTooShort && newPassword.isNotEmpty()) {
                                        Text("Minimum $minPasswordLength characters required", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = confirmNewPassword,
                                onValueChange = { confirmNewPassword = it },
                                label = { Text("Confirm New Password *") },
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                singleLine = true,
                                isError = passwordMismatch && confirmNewPassword.isNotEmpty(),
                                supportingText = {
                                    if (passwordMismatch && confirmNewPassword.isNotEmpty()) {
                                        Text("Passwords do not match!", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Checkbox(
                                    checked = forceChangePasswordOnLogin,
                                    onCheckedChange = { forceChangePasswordOnLogin = it }
                                )
                                Text("Require password change on next login", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isFormValid) {
                        onSave(
                            user.copy(
                                username = cleanUsername,
                                fullName = fullName.trim(),
                                email = email.trim(),
                                phone = phone.trim(),
                                role = selectedRole,
                                status = selectedStatus,
                                isActive = (selectedStatus == UserStatus.ACTIVE),
                                linkedTeacherName = linkedTeacherName.trim(),
                                linkedStaffName = linkedStaffName.trim()
                            ),
                            if (changePasswordNow && newPassword.isNotBlank()) newPassword else null,
                            forceChangePasswordOnLogin
                        )
                    }
                },
                enabled = isFormValid
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// =========================================================
// RESET PASSWORD DIALOG (WITH MASKING & CONFIRMATION)
// =========================================================

@Composable
private fun ResetPasswordDialog(
    user: UserEntity,
    minPasswordLength: Int = 6,
    onDismiss: () -> Unit,
    onConfirmReset: (newPassword: String, forceChange: Boolean) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var newPassword by remember { mutableStateOf(PasswordHasher.generateTempPassword()) }
    var confirmPassword by remember { mutableStateOf(newPassword) }
    var passwordVisible by remember { mutableStateOf(true) }
    var confirmPasswordVisible by remember { mutableStateOf(true) }
    var forceChange by remember { mutableStateOf(false) }

    val passwordMismatch = newPassword != confirmPassword
    val passwordTooShort = newPassword.length < minPasswordLength
    val isValid = newPassword.isNotBlank() && !passwordMismatch && !passwordTooShort

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Manage Password", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("@${user.username} • ${user.fullName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Current Credential Summary Card
                val currentPass = user.managedPassword.ifBlank { "Password123!" }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Current Active Credentials", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Username: @${user.username}", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Password: $currentPass",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(currentPass))
                                    Toast.makeText(context, "Copied current password!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(11.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Copy Password", fontSize = 9.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    newPassword = currentPass
                                    confirmPassword = currentPass
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(11.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Use As Template", fontSize = 9.sp)
                            }
                        }
                    }
                }

                // Quick Password Presets Row
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Quick Presets", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                newPassword = "Password123!"
                                confirmPassword = "Password123!"
                            },
                            label = { Text("Password123!", fontSize = 10.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                newPassword = "Admin@2026"
                                confirmPassword = "Admin@2026"
                            },
                            label = { Text("Admin@2026", fontSize = 10.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                newPassword = "HCM#2026"
                                confirmPassword = "HCM#2026"
                            },
                            label = { Text("HCM#2026", fontSize = 10.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                val generated = PasswordHasher.generateTempPassword()
                                newPassword = generated
                                confirmPassword = generated
                            },
                            label = { Text("🎲 Random", fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password *") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = {
                                val generated = PasswordHasher.generateTempPassword()
                                newPassword = generated
                                confirmPassword = generated
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Generate Random", modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    isError = passwordTooShort && newPassword.isNotEmpty(),
                    supportingText = {
                        if (passwordTooShort && newPassword.isNotEmpty()) {
                            Text("Minimum $minPasswordLength characters required", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password *") },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    singleLine = true,
                    isError = passwordMismatch && confirmPassword.isNotEmpty(),
                    supportingText = {
                        if (passwordMismatch && confirmPassword.isNotEmpty()) {
                            Text("Passwords do not match!", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Checkbox(checked = forceChange, onCheckedChange = { forceChange = it })
                    Text("Require password change on next login", fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirmReset(newPassword, forceChange)
                    }
                },
                enabled = isValid
            ) {
                Text("Reset Password")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// =========================================================
// CHANGE STATUS DIALOG
// =========================================================

@Composable
private fun ChangeStatusDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onConfirmStatus: (UserStatus) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(user.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Change Status: @${user.username}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Select new account status for ${user.fullName}:", fontSize = 12.sp)
                UserStatus.values().forEach { st ->
                    val color = when (st) {
                        UserStatus.ACTIVE -> Color(0xFF2E7D32)
                        UserStatus.INACTIVE -> Color(0xFF757575)
                        UserStatus.LOCKED -> Color(0xFFD32F2F)
                        UserStatus.SUSPENDED -> Color(0xFFE65100)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { selectedStatus = st }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = selectedStatus == st, onClick = { selectedStatus = st })
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${st.displayName} ${if (st == UserStatus.LOCKED) "(Clears lock counter on unlock)" else ""}",
                            fontSize = 13.sp,
                            fontWeight = if (selectedStatus == st) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirmStatus(selectedStatus) }) {
                Text("Apply Status")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// =========================================================
// TAB 2: ROLE & PERMISSION MATRIX (COMPACT)
// =========================================================

@Composable
private fun RolePermissionsTabContent(
    authViewModel: AuthViewModel,
    rolePermissions: List<RolePermissionEntity>
) {
    var selectedRole by remember { mutableStateOf(UserRole.ADMIN) }

    val modules = listOf(
        "DASHBOARD" to "1. Dashboard",
        "STUDENTS" to "2. Students Management",
        "TEACHERS" to "3. Teachers Management",
        "ATTENDANCE" to "4. Attendance System",
        "ASSESSMENT" to "5. Assessment Module",
        "MARKS_ENTRY" to "6. Marks Entry",
        "HCM_ASSESSMENT" to "7. HCM Assessment (Holistic)",
        "SGI" to "8. SGI Module",
        "REPORT_CARDS" to "9. Report Cards",
        "ACADEMIC_YEAR" to "10. Academic Year & Term",
        "PROMOTION" to "11. Promotion Management",
        "SCHOOL_POLICY" to "12. School Policy Center",
        "AI_ASSISTANT" to "13. AI Teacher Assistant",
        "USER_MANAGEMENT" to "14. User & Role Management",
        "SYSTEM_SETTINGS" to "15. System Settings"
    )

    val actions = listOf(
        "VIEW" to "View",
        "CREATE" to "Create",
        "EDIT" to "Edit",
        "DELETE" to "Delete",
        "PRINT" to "Print",
        "EXPORT" to "Export"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Compact Role Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            UserRole.values().forEach { role ->
                val isSelected = selectedRole == role
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedRole = role },
                    label = { Text(role.displayName, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        if (selectedRole == UserRole.SUPER_ADMIN) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Super Admin Master Access", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Super Admin has full unrestricted access to all 15 modules. Permissions cannot be restricted for this role.", textAlign = TextAlign.Center, fontSize = 11.sp)
                }
            }
        } else {
            // Permission Matrix Grid/List (Compact)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(modules) { (moduleKey, moduleTitle) ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(moduleTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                actions.forEach { (actionKey, actionTitle) ->
                                    val permKey = "${moduleKey}_${actionKey}"
                                    val isAllowed = rolePermissions.find { it.role == selectedRole && it.permissionKey == permKey }?.isAllowed ?: false

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Switch(
                                            checked = isAllowed,
                                            onCheckedChange = { checked ->
                                                authViewModel.updatePermission(
                                                    RolePermissionEntity(
                                                        role = selectedRole,
                                                        permissionKey = permKey,
                                                        isAllowed = checked
                                                    )
                                                )
                                            },
                                            modifier = Modifier.scale(0.7f)
                                        )
                                        Text(actionTitle, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// TAB 3: LOGIN HISTORY & AUDIT LOGS (COMPACT)
// =========================================================

@Composable
private fun LoginHistoryTabContent(
    loginHistory: List<LoginHistoryEntity>,
    auditLogs: List<AuditLogEntity>
) {
    var subTab by remember { mutableIntStateOf(0) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                label = { Text("Logins (${loginHistory.size})", fontSize = 11.sp) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(30.dp)
            )
            FilterChip(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                label = { Text("Audit Logs (${auditLogs.size})", fontSize = 11.sp) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(30.dp)
            )
        }

        if (subTab == 0) {
            if (loginHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No login history records found.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(loginHistory, key = { it.id }) { log ->
                        val isSuccess = log.status == "SUCCESS"
                        val isLogout = log.status == "LOGOUT"
                        val badgeColor = if (isSuccess) Color(0xFF2E7D32) else if (isLogout) MaterialTheme.colorScheme.primary else Color(0xFFD32F2F)

                        Card(
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text("${log.displayName} (@${log.username})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("IP: ${log.ipAddress} • ${dateFormat.format(Date(log.loginTime))}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (log.failureReason.isNotBlank()) {
                                        Text(log.failureReason, fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }

                                Surface(
                                    color = badgeColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = log.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (auditLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No audit log records recorded yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(auditLogs, key = { it.id }) { log ->
                        Card(
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(log.action, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                                    Text(dateFormat.format(Date(log.timestamp)), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("User: ${log.userName} (${log.roleName})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text(log.details, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// TAB 4: SECURITY POLICY (COMPACT)
// =========================================================

@Composable
private fun SecurityPolicyTabContent(
    securityPolicy: SecurityPolicyEntity?,
    onSavePolicy: (SecurityPolicyEntity) -> Unit
) {
    val current = securityPolicy ?: SecurityPolicyEntity()

    var maxAttempts by remember(current) { mutableIntStateOf(current.maxFailedAttempts) }
    var forceFirstLoginChange by remember(current) { mutableStateOf(current.forcePasswordChangeOnFirstLogin) }
    var sessionTimeout by remember(current) { mutableIntStateOf(current.sessionTimeoutMinutes) }
    var minPassLength by remember(current) { mutableIntStateOf(current.minPasswordLength) }
    var requireSpecial by remember(current) { mutableStateOf(current.requireSpecialChar) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Max Failed Attempts
                Column {
                    Text("Max Failed Login Attempts: $maxAttempts", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Slider(
                        value = maxAttempts.toFloat(),
                        onValueChange = { maxAttempts = it.toInt() },
                        valueRange = 3f..10f,
                        steps = 6
                    )
                }

                HorizontalDivider()

                // Session Timeout
                Column {
                    Text("Session Timeout: $sessionTimeout minutes", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Slider(
                        value = sessionTimeout.toFloat(),
                        onValueChange = { sessionTimeout = it.toInt() },
                        valueRange = 10f..120f,
                        steps = 10
                    )
                }

                HorizontalDivider()

                // Min Password Length
                Column {
                    Text("Minimum Password Length: $minPassLength characters", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Slider(
                        value = minPassLength.toFloat(),
                        onValueChange = { minPassLength = it.toInt() },
                        valueRange = 4f..16f,
                        steps = 11
                    )
                }

                HorizontalDivider()

                // Force Change On First Login
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Force Password Change on First Login", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Users with temp passwords must set a new password on login.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = forceFirstLoginChange,
                        onCheckedChange = { forceFirstLoginChange = it },
                        modifier = Modifier.scale(0.8f)
                    )
                }

                HorizontalDivider()

                // Require Special Characters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Require Special Characters (!@#$)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("New passwords must include symbols.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = requireSpecial,
                        onCheckedChange = { requireSpecial = it },
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }
        }

        // Save Button
        Button(
            onClick = {
                onSavePolicy(
                    SecurityPolicyEntity(
                        id = 1,
                        maxFailedAttempts = maxAttempts,
                        forcePasswordChangeOnFirstLogin = forceFirstLoginChange,
                        sessionTimeoutMinutes = sessionTimeout,
                        minPasswordLength = minPassLength,
                        requireSpecialChar = requireSpecial
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save Security Policy", fontSize = 13.sp)
        }
    }
}
