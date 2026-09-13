package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AiSettingEntity
import com.example.data.local.entity.ReportSettingEntity
import com.example.data.report.ReportAssessmentVisibilityManager
import com.example.data.local.entity.SchoolSettingEntity
import com.example.data.local.entity.SecurityPolicyEntity
import com.example.data.local.entity.UserRole
import com.example.ui.viewmodel.SettingsCategory
import com.example.ui.viewmodel.SystemSettingsViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SystemSettingsViewModel,
    currentUserRole: UserRole = UserRole.SUPER_ADMIN,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val schoolSettings by viewModel.schoolSettings.collectAsState()
    val reportSettings by viewModel.reportSettings.collectAsState()
    val aiSettings by viewModel.aiSettings.collectAsState()
    val securityPolicy by viewModel.securityPolicy.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val dbSize by viewModel.dbSize.collectAsState()
    val isTestingAi by viewModel.isTestingAi.collectAsState()
    val aiTestResult by viewModel.aiTestResult.collectAsState()

    val isAuthorized = currentUserRole == UserRole.SUPER_ADMIN || currentUserRole == UserRole.ADMIN
    if (!isAuthorized) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Access Restricted",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "System Settings & Backup is restricted to Administrators only.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        return
    }

    // Show status toasts
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Column {
                    Text(
                        "System Settings & Backup",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    )
                    Text(
                        "Configuration, Integrations & Audit",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }

        // Search Settings Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search settings (e.g. Logo, Theme, Pass Mark, Backup, API Key)...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Category Navigation Tabs / Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(SettingsCategory.values()) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { viewModel.setSelectedCategory(category) },
                    label = { Text(category.title, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = getCategoryIcon(category),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        HorizontalDivider()

        // Content Area by Category
        Box(modifier = Modifier.weight(1f)) {
            when (selectedCategory) {
                SettingsCategory.SCHOOL_INFO -> SchoolInfoSettingsSection(
                    schoolSettings = schoolSettings ?: SchoolSettingEntity(),
                    onSave = { viewModel.saveSchoolSettings(it) },
                    isEditable = currentUserRole == UserRole.SUPER_ADMIN || currentUserRole == UserRole.ADMIN
                )
                SettingsCategory.GENERAL -> GeneralSettingsSection(
                    schoolSettings = schoolSettings ?: SchoolSettingEntity(),
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = onToggleDarkTheme,
                    onSave = { viewModel.saveSchoolSettings(it) }
                )
                SettingsCategory.REPORT_SETTINGS -> ReportSettingsSection(
                    reportSettings = reportSettings ?: ReportSettingEntity(),
                    onSave = { viewModel.saveReportSettings(it) },
                    isEditable = currentUserRole == UserRole.SUPER_ADMIN || currentUserRole == UserRole.ADMIN
                )
                SettingsCategory.ASSESSMENT -> AssessmentSettingsSection(
                    schoolSettings = schoolSettings ?: SchoolSettingEntity(),
                    onSave = { viewModel.saveSchoolSettings(it) },
                    isEditable = currentUserRole == UserRole.SUPER_ADMIN || currentUserRole == UserRole.ADMIN
                )
                SettingsCategory.AI_SETTINGS -> AiSettingsSection(
                    aiSettings = aiSettings ?: AiSettingEntity(),
                    isTesting = isTestingAi,
                    testResult = aiTestResult,
                    onTestConnection = { key, provider -> viewModel.testAiConnection(key, provider) },
                    onSave = { viewModel.saveAiSettings(it) },
                    isEditable = currentUserRole == UserRole.SUPER_ADMIN || currentUserRole == UserRole.ADMIN
                )
                SettingsCategory.BACKUP_RESTORE -> BackupRestoreSection(
                    onCreateBackup = { viewModel.createBackup() },
                    onRestoreBackup = { jsonStr -> viewModel.restoreBackup(jsonStr) },
                    isEditable = currentUserRole == UserRole.SUPER_ADMIN || currentUserRole == UserRole.ADMIN,
                    onResetTestData = { clearStudents, clearTeachers, clearAssessments ->
                        viewModel.resetTestingData(clearStudents, clearTeachers, clearAssessments)
                    },
                    onFactoryReset = {
                        viewModel.resetEntireSystemToDefaults()
                    }
                )
                SettingsCategory.IMPORT_EXPORT -> ImportExportSection(
                    onImport = { entity, content, isJson -> viewModel.importData(entity, content, isJson) },
                    onExport = { entity, isJson -> viewModel.exportData(entity, isJson) },
                    isEditable = currentUserRole == UserRole.SUPER_ADMIN || currentUserRole == UserRole.ADMIN
                )
                SettingsCategory.SYSTEM_LOGS -> SystemLogsSection(
                    auditLogs = auditLogs,
                    searchQuery = searchQuery
                )
                SettingsCategory.MAINTENANCE -> DatabaseMaintenanceSection(
                    dbSize = dbSize,
                    onOptimize = { viewModel.optimizeDatabase() },
                    onCleanTemp = { viewModel.cleanTempFiles() },
                    onRefresh = { viewModel.refreshDatabaseSize() },
                    isSuperAdmin = currentUserRole == UserRole.SUPER_ADMIN || currentUserRole == UserRole.ADMIN,
                    onResetTestData = { clearStudents, clearTeachers, clearAssessments ->
                        viewModel.resetTestingData(clearStudents, clearTeachers, clearAssessments)
                    },
                    onFactoryReset = {
                        viewModel.resetEntireSystemToDefaults()
                    }
                )
                SettingsCategory.NOTIFICATIONS -> NotificationsSection(
                    schoolSettings = schoolSettings ?: SchoolSettingEntity(),
                    onSave = { viewModel.saveSchoolSettings(it) }
                )
                SettingsCategory.SECURITY -> SecurityAndPermissionsSection(
                    securityPolicy = securityPolicy ?: SecurityPolicyEntity(),
                    currentUserRole = currentUserRole,
                    onSavePolicy = { viewModel.saveSecurityPolicy(it) }
                )
            }
        }
    }
}

private fun getCategoryIcon(category: SettingsCategory) = when (category) {
    SettingsCategory.SCHOOL_INFO -> Icons.Default.School
    SettingsCategory.GENERAL -> Icons.Default.Tune
    SettingsCategory.REPORT_SETTINGS -> Icons.Default.Assessment
    SettingsCategory.ASSESSMENT -> Icons.Default.Rule
    SettingsCategory.AI_SETTINGS -> Icons.Default.Psychology
    SettingsCategory.BACKUP_RESTORE -> Icons.Default.Backup
    SettingsCategory.IMPORT_EXPORT -> Icons.Default.ImportExport
    SettingsCategory.SYSTEM_LOGS -> Icons.Default.ReceiptLong
    SettingsCategory.MAINTENANCE -> Icons.Default.Build
    SettingsCategory.NOTIFICATIONS -> Icons.Default.Notifications
    SettingsCategory.SECURITY -> Icons.Default.Security
}

// =========================================================
// 1. SCHOOL INFORMATION SETTINGS
// =========================================================
@Composable
fun SchoolInfoSettingsSection(
    schoolSettings: SchoolSettingEntity,
    onSave: (SchoolSettingEntity) -> Unit,
    isEditable: Boolean
) {
    var schoolName by remember(schoolSettings) { mutableStateOf(schoolSettings.schoolName) }
    var address by remember(schoolSettings) { mutableStateOf(schoolSettings.address) }
    var contactPhone by remember(schoolSettings) { mutableStateOf(schoolSettings.contactPhone) }
    var email by remember(schoolSettings) { mutableStateOf(schoolSettings.email) }
    var website by remember(schoolSettings) { mutableStateOf(schoolSettings.website) }
    var principalName by remember(schoolSettings) { mutableStateOf(schoolSettings.principalName) }
    var academicYear by remember(schoolSettings) { mutableStateOf(schoolSettings.academicYear) }
    var motto by remember(schoolSettings) { mutableStateOf(schoolSettings.motto) }
    var logoText by remember(schoolSettings) { mutableStateOf(schoolSettings.logoText) }
    var schoolSeal by remember(schoolSettings) { mutableStateOf(schoolSettings.schoolSeal) }
    var logoUri by remember(schoolSettings) { mutableStateOf(schoolSettings.logoUri) }
    var showBase64Dialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(selectedUri)
                val base64Logo = inputStream?.use { stream ->
                    com.example.ui.util.SchoolLogoUtils.saveLogoFromInputStream(context, stream)
                }
                if (!base64Logo.isNullOrBlank()) {
                    logoUri = base64Logo
                    onSave(
                        schoolSettings.copy(
                            schoolName = schoolName,
                            address = address,
                            contactPhone = contactPhone,
                            email = email,
                            website = website,
                            principalName = principalName,
                            academicYear = academicYear,
                            motto = motto,
                            logoText = logoText,
                            logoUri = base64Logo,
                            schoolSeal = schoolSeal
                        )
                    )
                    com.example.data.sync.SyncManager.triggerSyncAsync(context)
                    Toast.makeText(context, "School logo updated and synced across devices!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to encode image.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to process logo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "School Information updated here will automatically reflect across all Report Cards, PDF Headers, Print Templates, and Transcripts.",
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Official School Logo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("The saved logo is automatically used across all student report cards and exported PDFs.", fontSize = 12.sp, color = Color.Gray)

                    val logoBitmap = com.example.ui.util.rememberSchoolLogo(schoolSettings.logoUri)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (logoBitmap != null) {
                                    Image(
                                        bitmap = logoBitmap,
                                        contentDescription = "School Logo Preview",
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text("No Logo", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (logoBitmap == null) {
                                Button(
                                    onClick = { logoLauncher.launch("image/*") },
                                    enabled = isEditable,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Upload Image File")
                                }

                                OutlinedButton(
                                    onClick = { showBase64Dialog = true },
                                    enabled = isEditable,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Upload as Base64")
                                }
                            } else {
                                Button(
                                    onClick = { logoLauncher.launch("image/*") },
                                    enabled = isEditable,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Replace Image File")
                                }

                                OutlinedButton(
                                    onClick = { showBase64Dialog = true },
                                    enabled = isEditable,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Replace as Base64")
                                }

                                OutlinedButton(
                                    onClick = {
                                        val b64 = logoUri?.ifBlank { null } ?: com.example.ui.util.SchoolLogoUtils.getSchoolLogoAsBase64(context)
                                        if (!b64.isNullOrBlank()) {
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("School Logo Base64", b64)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Base64 logo copied to clipboard (${b64.length} characters)!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "No Base64 data found.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Copy Base64 String")
                                }

                                OutlinedButton(
                                    onClick = {
                                        logoUri = null
                                        val logoFile = com.example.ui.util.SchoolLogoUtils.getSchoolLogoFile(context)
                                        if (logoFile.exists()) logoFile.delete()
                                        onSave(
                                            schoolSettings.copy(
                                                schoolName = schoolName,
                                                address = address,
                                                contactPhone = contactPhone,
                                                email = email,
                                                website = website,
                                                principalName = principalName,
                                                academicYear = academicYear,
                                                motto = motto,
                                                logoText = logoText,
                                                logoUri = null,
                                                schoolSeal = schoolSeal
                                            )
                                        )
                                        com.example.data.sync.SyncManager.triggerTableSyncAsync("school_settings", forceImmediate = true)
                                        Toast.makeText(context, "School logo removed.", Toast.LENGTH_SHORT).show()
                                    },
                                    enabled = isEditable,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Remove Logo")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("School Branding & Identity", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { schoolName = it },
                        label = { Text("School Name") },
                        enabled = isEditable,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = logoText,
                            onValueChange = { logoText = it },
                            label = { Text("Logo Abbreviation") },
                            enabled = isEditable,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = schoolSeal,
                            onValueChange = { schoolSeal = it },
                            label = { Text("School Seal Status") },
                            enabled = isEditable,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = motto,
                        onValueChange = { motto = it },
                        label = { Text("School Motto") },
                        enabled = isEditable,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Contact & Administration", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = principalName,
                        onValueChange = { principalName = it },
                        label = { Text("Principal Name") },
                        enabled = isEditable,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Phone Number") },
                            enabled = isEditable,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            enabled = isEditable,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = website,
                            onValueChange = { website = it },
                            label = { Text("Website URL") },
                            enabled = isEditable,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = academicYear,
                            onValueChange = { academicYear = it },
                            label = { Text("Current Academic Year") },
                            enabled = isEditable,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("School Physical Address") },
                        enabled = isEditable,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isEditable) {
                        Button(
                            onClick = {
                                onSave(
                                    schoolSettings.copy(
                                        schoolName = schoolName,
                                        address = address,
                                        contactPhone = contactPhone,
                                        email = email,
                                        website = website,
                                        principalName = principalName,
                                        academicYear = academicYear,
                                        motto = motto,
                                        logoText = logoText,
                                        logoUri = logoUri,
                                        schoolSeal = schoolSeal
                                    )
                                )
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save School Info")
                        }
                    }
                }
            }
        }
    }

    if (showBase64Dialog) {
        UploadBase64LogoDialog(
            initialBase64 = logoUri ?: com.example.ui.util.SchoolLogoUtils.getSchoolLogoAsBase64(context),
            onDismiss = { showBase64Dialog = false },
            onConfirmBase64 = { base64Data ->
                showBase64Dialog = false
                val saved = com.example.ui.util.SchoolLogoUtils.saveLogoFromBase64(context, base64Data)
                if (saved) {
                    val cleanBase64 = com.example.ui.util.SchoolLogoUtils.getSchoolLogoAsBase64(context) ?: base64Data
                    logoUri = cleanBase64
                    val updated = schoolSettings.copy(
                        schoolName = schoolName,
                        address = address,
                        contactPhone = contactPhone,
                        email = email,
                        website = website,
                        principalName = principalName,
                        academicYear = academicYear,
                        motto = motto,
                        logoText = logoText,
                        logoUri = cleanBase64,
                        schoolSeal = schoolSeal
                    )
                    onSave(updated)
                    com.example.data.sync.SyncManager.triggerTableSyncAsync("school_settings", forceImmediate = true)
                    com.example.data.sync.SyncManager.triggerSyncAsync(context, forceImmediate = true)
                    Toast.makeText(context, "School logo saved and synced to database! All users will now see this logo.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to decode or save Base64 logo.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

// =========================================================
// 2. GENERAL SETTINGS
// =========================================================
@Composable
fun GeneralSettingsSection(
    schoolSettings: SchoolSettingEntity,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSave: (SchoolSettingEntity) -> Unit
) {
    var selectedLanguage by remember(schoolSettings) { mutableStateOf(schoolSettings.defaultLanguage) }
    var dateFormat by remember(schoolSettings) { mutableStateOf(schoolSettings.dateFormat) }
    var timeFormat by remember(schoolSettings) { mutableStateOf(schoolSettings.timeFormat) }
    var currency by remember(schoolSettings) { mutableStateOf(schoolSettings.currency) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfoState by remember { mutableStateOf<com.example.data.util.AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val languages = listOf("English", "Myanmar (Burmese)", "Mon", "Karen", "Chinese")
    val dateFormats = listOf("yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy")
    val timeFormats = listOf("12-Hour (AM/PM)", "24-Hour (HH:mm)")
    val currencies = listOf("MMK (Ks)", "USD ($)", "EUR (€)", "THB (฿)")

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Display Theme", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text("Dark Mode", fontWeight = FontWeight.SemiBold)
                                Text("Switch system layout theme color scheme", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Switch(checked = isDarkTheme, onCheckedChange = onToggleDarkTheme)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Application Updates", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Current Version: v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})", fontSize = 11.5.sp, color = Color.Gray)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "GitHub Releases",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "When changes are pushed to GitHub, GitHub Actions automatically compiles and publishes the new APK. Tap below to check for available updates.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isCheckingUpdate = true
                                    val info = com.example.data.util.AppUpdateManager.checkForUpdates()
                                    isCheckingUpdate = false
                                    updateInfoState = info
                                    if (info.hasUpdate) {
                                        showUpdateDialog = true
                                    } else {
                                        Toast.makeText(context, "You are using the latest version (v${com.example.BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isCheckingUpdate,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                                Text("Checking...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Check For Updates", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Locale & Localization", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Text("Default UI Language", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(languages) { lang ->
                            FilterChip(
                                selected = selectedLanguage == lang,
                                onClick = {
                                    selectedLanguage = lang
                                    onSave(schoolSettings.copy(defaultLanguage = lang))
                                },
                                label = { Text(lang, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text("Date Format", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(dateFormats) { fmt ->
                            FilterChip(
                                selected = dateFormat == fmt,
                                onClick = {
                                    dateFormat = fmt
                                    onSave(schoolSettings.copy(dateFormat = fmt))
                                },
                                label = { Text(fmt, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text("Time & Currency", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Time Format", fontSize = 11.sp, color = Color.Gray)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(timeFormats) { tf ->
                                    FilterChip(
                                        selected = timeFormat == tf,
                                        onClick = {
                                            timeFormat = tf
                                            onSave(schoolSettings.copy(timeFormat = tf))
                                        },
                                        label = { Text(tf, fontSize = 11.sp) },
                                        leadingIcon = null,
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Currency Quick Select", fontSize = 11.sp, color = Color.Gray)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(currencies) { c ->
                                    FilterChip(
                                        selected = currency == c,
                                        onClick = {
                                            currency = c
                                            onSave(schoolSettings.copy(currency = c))
                                        },
                                        label = { Text(c, fontSize = 11.sp) },
                                        leadingIcon = null,
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = currency,
                        onValueChange = {
                            currency = it
                            onSave(schoolSettings.copy(currency = it))
                        },
                        label = { Text("School Base Currency / Unit", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                }
            }
        }
    }

    if (showUpdateDialog && updateInfoState != null) {
        com.example.ui.components.AppUpdateDialog(
            updateInfo = updateInfoState!!,
            onDismiss = { showUpdateDialog = false }
        )
    }
}

// =========================================================
// 3. REPORT SETTINGS
// =========================================================
@Composable
fun ReportSettingsSection(
    reportSettings: ReportSettingEntity,
    onSave: (ReportSettingEntity) -> Unit,
    isEditable: Boolean
) {
    var showStudentPhoto by remember(reportSettings) { mutableStateOf(reportSettings.showStudentPhoto) }
    var showSchoolLogo by remember(reportSettings) { mutableStateOf(reportSettings.showSchoolLogo) }
    var showAttendance by remember(reportSettings) { mutableStateOf(reportSettings.showAttendance) }
    var showSgi by remember(reportSettings) { mutableStateOf(reportSettings.showSgi) }
    var showHcm by remember(reportSettings) { mutableStateOf(reportSettings.showHcm) }
    var showExtracurricular by remember(reportSettings) { mutableStateOf(reportSettings.showExtracurricular) }
    var showTeacherComment by remember(reportSettings) { mutableStateOf(reportSettings.showTeacherComment) }
    var showRecommendation by remember(reportSettings) { mutableStateOf(reportSettings.showRecommendation) }
    var showParentSignature by remember(reportSettings) { mutableStateOf(reportSettings.showParentSignature) }
    var showPrincipalSignature by remember(reportSettings) { mutableStateOf(reportSettings.showPrincipalSignature) }
    var showClassTeacherSignature by remember(reportSettings) { mutableStateOf(reportSettings.showClassTeacherSignature) }
    var showSchoolSeal by remember(reportSettings) { mutableStateOf(reportSettings.showSchoolSeal) }
    var pdfNamingPattern by remember(reportSettings) { mutableStateOf(reportSettings.pdfNamingPattern) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Report Card Field Visibility", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Enable or disable specific sections on generated student report cards.", fontSize = 12.sp, color = Color.Gray)

                    SettingSwitchRow("School Logo & Seal", "Display official school emblem and validated stamp", showSchoolLogo) { showSchoolLogo = it; showSchoolSeal = it }
                    SettingSwitchRow("Attendance Summary", "Include days present, absent, and leave counts", showAttendance) { showAttendance = it }
                    SettingSwitchRow("SGI (Student Growth Index)", "Include physical & psychological growth ratings", showSgi) { showSgi = it }
                    SettingSwitchRow("HCM Assessment", "Include 5 Holistic Pillars assessment scores", showHcm) { showHcm = it }
                    SettingSwitchRow("Extracurricular Achievements", "Include sports, clubs, and competition awards", showExtracurricular) { showExtracurricular = it }
                    SettingSwitchRow("Teacher Remarks & Recommendations", "Include principal and class teacher comments", showTeacherComment) { showTeacherComment = it; showRecommendation = it }
                    SettingSwitchRow("Parent & Staff Signatures", "Include signature lines for Parent, Teacher & Principal", showParentSignature) {
                        showParentSignature = it
                        showPrincipalSignature = it
                        showClassTeacherSignature = it
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("PDF Export Configuration", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = pdfNamingPattern,
                        onValueChange = { pdfNamingPattern = it },
                        label = { Text("PDF File Naming Pattern") },
                        enabled = isEditable,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Variables: {Template}, {Grade}, {StudentName}, {Period}, {Year}", fontSize = 11.sp, color = Color.Gray)

                    if (isEditable) {
                        Button(
                            onClick = {
                                onSave(
                                    reportSettings.copy(
                                        showSchoolLogo = showSchoolLogo,
                                        showAttendance = showAttendance,
                                        showSgi = showSgi,
                                        showHcm = showHcm,
                                        showExtracurricular = showExtracurricular,
                                        showTeacherComment = showTeacherComment,
                                        showRecommendation = showRecommendation,
                                        showParentSignature = showParentSignature,
                                        showPrincipalSignature = showPrincipalSignature,
                                        showClassTeacherSignature = showClassTeacherSignature,
                                        showSchoolSeal = showSchoolSeal,
                                        pdfNamingPattern = pdfNamingPattern
                                    )
                                )
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Apply Settings To All Templates")
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// 4. ASSESSMENT SETTINGS
// =========================================================
@Composable
fun AssessmentSettingsSection(
    schoolSettings: SchoolSettingEntity,
    onSave: (SchoolSettingEntity) -> Unit,
    isEditable: Boolean
) {
    var passMark by remember { mutableStateOf("40") }
    var distinctionMark by remember { mutableStateOf("75") }
    var periodVisibility by remember { mutableStateOf(true) }
    var enableAcademicRanking by remember { mutableStateOf(true) }
    var selectedCalculation by remember { mutableStateOf("Weighted Average (SGI + Exams)") }

    val calcOptions = listOf("Weighted Average (SGI + Exams)", "Percentage Total", "Raw Sum Total")

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Default Passing Thresholds", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = passMark,
                            onValueChange = { passMark = it },
                            label = { Text("Default Pass Mark") },
                            enabled = isEditable,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = distinctionMark,
                            onValueChange = { distinctionMark = it },
                            label = { Text("Default Distinction Mark") },
                            enabled = isEditable,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SettingSwitchRow("Assessment Period Visibility", "Show past period results to teachers and parents", periodVisibility) { periodVisibility = it }
                    SettingSwitchRow("Academic Ranking & Percentiles", "Calculate and display class rank (#1, #2...) on report cards", enableAcademicRanking) { enableAcademicRanking = it }

                    Text("Report Calculation Engine Mode", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(calcOptions) { opt ->
                            FilterChip(
                                selected = selectedCalculation == opt,
                                onClick = { selectedCalculation = opt },
                                label = { Text(opt, fontSize = 12.sp) }
                            )
                        }
                    }

                    if (isEditable) {
                        Button(
                            onClick = { onSave(schoolSettings) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save Assessment Preferences")
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// 5. AI SETTINGS
// =========================================================
@Composable
fun AiSettingsSection(
    aiSettings: AiSettingEntity,
    isTesting: Boolean,
    testResult: String?,
    onTestConnection: (String, String) -> Unit,
    onSave: (AiSettingEntity) -> Unit,
    isEditable: Boolean
) {
    var preferredProvider by remember(aiSettings) { mutableStateOf(aiSettings.preferredProvider) }
    var apiKey by remember(aiSettings) { mutableStateOf(aiSettings.apiKeyOverride) }
    var showApiKey by remember { mutableStateOf(false) }
    var defaultLanguage by remember(aiSettings) { mutableStateOf(aiSettings.defaultLanguage) }
    var maxOutputLength by remember(aiSettings) { mutableStateOf(aiSettings.maxOutputLength.toString()) }
    var isEnabled by remember(aiSettings) { mutableStateOf(aiSettings.isAiModuleEnabled) }

    val providers = listOf("Gemini 1.5 Flash", "Gemini 1.5 Pro", "Gemini 2.0 Flash", "OpenAI GPT-4o", "Claude 3.5 Sonnet")
    val lengths = listOf("1000", "2000", "4000")

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AI Module Activation", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                    }

                    Text("Preferred AI Model Provider", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(providers) { provider ->
                            FilterChip(
                                selected = preferredProvider == provider,
                                onClick = { preferredProvider = provider },
                                label = { Text(provider, fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key Override (Optional)") },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                            }
                        },
                        enabled = isEditable,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("API keys are stored securely in encrypted local storage and never exposed to standard users.", fontSize = 11.sp, color = Color.Gray)

                    Text("Maximum Response Tokens", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(lengths) { len ->
                            FilterChip(
                                selected = maxOutputLength == len,
                                onClick = { maxOutputLength = len },
                                label = { Text("$len tokens", fontSize = 12.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onTestConnection(apiKey, preferredProvider) },
                            enabled = !isTesting
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                                Text("Testing...")
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Test AI Connection")
                            }
                        }

                        if (isEditable) {
                            Button(
                                onClick = {
                                    onSave(
                                        aiSettings.copy(
                                            preferredProvider = preferredProvider,
                                            apiKeyOverride = apiKey,
                                            defaultLanguage = defaultLanguage,
                                            maxOutputLength = maxOutputLength.toIntOrNull() ?: 2000,
                                            isAiModuleEnabled = isEnabled
                                        )
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Save AI Settings")
                            }
                        }
                    }

                    testResult?.let {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (it.startsWith("Success")) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        ) {
                            Text(
                                it,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp),
                                color = if (it.startsWith("Success")) Color(0xFF2E7D32) else Color(0xFFE65100)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// 6. BACKUP & RESTORE
// =========================================================
@Composable
fun BackupRestoreSection(
    onCreateBackup: () -> Unit,
    onRestoreBackup: (String) -> Unit,
    isEditable: Boolean,
    onResetTestData: (clearStudents: Boolean, clearTeachers: Boolean, clearAssessments: Boolean) -> Unit = { _, _, _ -> },
    onFactoryReset: () -> Unit = {}
) {
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingBackupContent by remember { mutableStateOf<String?>(null) }
    var pendingFileName by remember { mutableStateOf<String?>(null) }
    var autoBackupSchedule by remember { mutableStateOf("Weekly") }
    val schedules = listOf("Off", "Daily", "Weekly", "Monthly")
    val context = LocalContext.current

    // Post-Testing Reset state variables
    var showResetTestDialog by remember { mutableStateOf(false) }
    var showFactoryResetDialog by remember { mutableStateOf(false) }
    var clearStudentsCheck by remember { mutableStateOf(false) }
    var clearTeachersCheck by remember { mutableStateOf(false) }
    var clearAssessmentsCheck by remember { mutableStateOf(false) }
    var factoryResetConfirmText by remember { mutableStateOf("") }
    var showSupabaseSchemaGuideDialog by remember { mutableStateOf(false) }

    val backupFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.use { stream ->
                    stream.bufferedReader().readText()
                }
                if (!content.isNullOrBlank()) {
                    pendingBackupContent = content
                    pendingFileName = it.lastPathSegment ?: "Selected Backup Package"
                    showRestoreConfirmDialog = true
                } else {
                    Toast.makeText(context, "Selected file is empty", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read backup file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Manual & Automatic Backup Engine", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (!isEditable) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "Admin Only",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text("Full backup includes Students, Teachers, Users, Grades, Classes, Subjects, Academic Years, Assessments, Marks, Attendance, HCM Assessment, SGI, and School Settings.", fontSize = 12.sp, color = Color.Gray)

                    if (!isEditable) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Backup and database restoration privileges are restricted to Administrator and Super Admin roles.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text("Automatic Backup Schedule", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(schedules) { sch ->
                            FilterChip(
                                selected = autoBackupSchedule == sch,
                                onClick = { if (isEditable) autoBackupSchedule = sch },
                                enabled = isEditable,
                                label = { Text(sch, fontSize = 12.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onCreateBackup,
                            modifier = Modifier.weight(1f),
                            enabled = isEditable
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create Backup Now")
                        }

                        OutlinedButton(
                            onClick = { backupFilePicker.launch("*/*") },
                            modifier = Modifier.weight(1f),
                            enabled = isEditable
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Restore Backup")
                        }
                    }
                }
            }
        }

        // Supabase Cloud Configuration & Schema Status Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "Supabase Cloud Sync Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                com.example.data.remote.SupabaseClientManager.supabaseUrl
                                    .removePrefix("https://")
                                    .removePrefix("http://")
                                    .trimEnd('/'),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        "The app runs offline-first: all student profiles, marks, assessments, and attendance records persist securely in your device's local database. To synchronize bidirectional changes with the connected Supabase cloud backend, ensure your Supabase database schema has the required table columns and RLS sync policies configured.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "💡 Supabase Setup Tip",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "If sync fails with column cache errors (e.g. 'closed_by' or 'education_level'), run the generated SQL script 'supabase_migration_fix.sql' in your Supabase SQL Editor to add the full column definitions and enable sync RLS policies.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                com.example.data.sync.SyncManager.triggerSyncAsync(context, forceImmediate = true)
                                Toast.makeText(context, "Initiated cloud sync...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sync Now")
                        }

                        OutlinedButton(
                            onClick = { showSupabaseSchemaGuideDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Schema Setup")
                        }
                    }
                }
            }
        }

        // Prominent Red Post-Testing Reset Card (connected to local Room & Cloud Supabase)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "Database Reset (Post-Testing Run)",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 16.sp
                            )
                            Text(
                                "Purges test marks, results, attendance & logs from Phone & Cloud Database.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.35f))

                    Text(
                        "Done with testing? Tap the red button below to wipe simulated student marks, assessment results, attendance records, holistic evaluations, teacher remarks, and temporary report files so your database is clean for official school run. Deletions propagate to the connected Cloud Database.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showResetTestDialog = true },
                            enabled = isEditable,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reset Test Data", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                factoryResetConfirmText = ""
                                showFactoryResetDialog = true
                            },
                            enabled = isEditable,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Factory Reset", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showRestoreConfirmDialog && pendingBackupContent != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingBackupContent = null
            },
            title = { Text("Restore System Database Backup?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("File: ${pendingFileName ?: "Selected Package"}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Warning: Restoring a backup will overwrite/update existing records in the database. All school settings, student records, teacher profiles, marks, and attendance data will be synchronized.")
                    Text("Are you sure you want to proceed with database restoration?", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        val content = pendingBackupContent
                        showRestoreConfirmDialog = false
                        pendingBackupContent = null
                        if (content != null) {
                            onRestoreBackup(content)
                        }
                    }
                ) { Text("Confirm & Restore") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showRestoreConfirmDialog = false
                    pendingBackupContent = null
                }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Reset Test Run Data
    if (showResetTestDialog) {
        AlertDialog(
            onDismissRequest = { showResetTestDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Reset Post-Testing Run Data?", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "This will permanently erase all testing transactions from your phone database and synchronize the purge with the connected Cloud Database.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Always cleared automatically:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Text("• Student marks & assessment results", fontSize = 11.sp)
                            Text("• Attendance daily check-ins & records", fontSize = 11.sp)
                            Text("• Holistic/SGI evaluation ratings & comments", fontSize = 11.sp)
                            Text("• Report card generation & PDF histories", fontSize = 11.sp)
                            Text("• AI assistant chats & temporary preview files", fontSize = 11.sp)
                        }
                    }

                    Text("Optional test roster purges:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = clearStudentsCheck,
                            onCheckedChange = { clearStudentsCheck = it }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Also delete enrolled test Students", fontSize = 12.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = clearTeachersCheck,
                            onCheckedChange = { clearTeachersCheck = it }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Also delete test Teacher staff accounts", fontSize = 12.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = clearAssessmentsCheck,
                            onCheckedChange = { clearAssessmentsCheck = it }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Also delete created Assessment schedules", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetTestData(clearStudentsCheck, clearTeachersCheck, clearAssessmentsCheck)
                        showResetTestDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm & Wipe Test Data")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetTestDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Full Factory Reset
    if (showFactoryResetDialog) {
        AlertDialog(
            onDismissRequest = { showFactoryResetDialog = false },
            icon = {
                Icon(
                    Icons.Default.Dangerous,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("Full Factory Reset", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "⚠️ CAUTION: This will wipe ALL custom school configurations, grading policies, student and teacher profiles, marks, and attendance from phone and cloud, resetting the database back to standard Myanmar curriculum seed defaults.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "To proceed with full re-initialization, type 'RESET' below:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    OutlinedTextField(
                        value = factoryResetConfirmText,
                        onValueChange = { factoryResetConfirmText = it },
                        placeholder = { Text("RESET") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onFactoryReset()
                        showFactoryResetDialog = false
                    },
                    enabled = factoryResetConfirmText.trim().uppercase() == "RESET",
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Execute Factory Reset")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showFactoryResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Supabase Schema Configuration Guide
    if (showSupabaseSchemaGuideDialog) {
        AlertDialog(
            onDismissRequest = { showSupabaseSchemaGuideDialog = false },
            icon = {
                Icon(
                    Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Supabase Cloud Setup Guide", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Your mobile app is currently connected to Supabase project:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = com.example.data.remote.SupabaseClientManager.supabaseUrl,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Text(
                        "Why do schema cache errors happen?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "PostgREST caches database table columns in Supabase. If tables are created with default placeholder columns (e.g. only uuid, name), pushing full entities with specific school columns (like 'closed_by', 'education_level', or 'academic_year_reminder_enabled') will report 'Could not find column in schema cache'.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Text(
                        "How to resolve with 1 click in Supabase:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "1. Open your Supabase Dashboard for this project.\n2. Navigate to SQL Editor.\n3. Run the script 'supabase_migration_fix.sql' (located in the project root).\n4. This script safely adds all missing columns with 'IF NOT EXISTS' and configures Row Level Security (RLS) sync policies.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Text(
                        "Local data is 100% safe: All operations continue in offline-first mode without data loss.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2E7D32)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showSupabaseSchemaGuideDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }
}

// =========================================================
// 7. IMPORT & EXPORT
// =========================================================
@Composable
fun ImportExportSection(
    onImport: (String, String, Boolean) -> Unit,
    onExport: (String, Boolean) -> Unit,
    isEditable: Boolean
) {
    var selectedEntity by remember { mutableStateOf("Students") }
    var selectedFormat by remember { mutableStateOf("CSV") }
    var pasteDataText by remember { mutableStateOf("") }

    val entities = listOf("Students", "Teachers", "Subjects", "Assessment Settings", "Academic Years")

    val sampleCsv = """
        Student ID,Full Name,Gender,Grade,Class,Roll No,Parent Name,Parent Phone
        ST-1001,Aung Aung,Male,G1,A,1,U Ba,0912345678
        ST-1002,Su Su,Female,G1,A,2,Daw Hla,0987654321
    """.trimIndent()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Data Import & Batch Onboarding", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Text("Target Entity Type", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(entities) { ent ->
                            FilterChip(
                                selected = selectedEntity == ent,
                                onClick = { selectedEntity = ent },
                                label = { Text(ent, fontSize = 12.sp) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedFormat == "CSV",
                            onClick = { selectedFormat = "CSV" },
                            label = { Text("CSV Format") }
                        )
                        FilterChip(
                            selected = selectedFormat == "JSON",
                            onClick = { selectedFormat = "JSON" },
                            label = { Text("JSON Format") }
                        )
                    }

                    OutlinedTextField(
                        value = pasteDataText,
                        onValueChange = { pasteDataText = it },
                        placeholder = { Text("Paste CSV or JSON content here, or use sample batch below...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(onClick = { pasteDataText = sampleCsv }) { Text("Load Sample Batch", fontSize = 12.sp) }
                        Button(
                            onClick = { onImport(selectedEntity, pasteDataText.ifBlank { sampleCsv }, selectedFormat == "JSON") },
                            enabled = isEditable
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Execute Import")
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Data Export Center", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Export records to Documents/HCM_SMS_Exports/ for reporting and compliance.", fontSize = 12.sp, color = Color.Gray)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onExport(selectedEntity, false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export CSV")
                        }

                        OutlinedButton(
                            onClick = { onExport(selectedEntity, true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export JSON")
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// 8. SYSTEM LOGS
// =========================================================
@Composable
fun SystemLogsSection(
    auditLogs: List<com.example.data.local.entity.AuditLogEntity>,
    searchQuery: String
) {
    val filteredLogs = remember(auditLogs, searchQuery) {
        if (searchQuery.isBlank()) auditLogs
        else auditLogs.filter {
            it.action.contains(searchQuery, ignoreCase = true) ||
                    it.details.contains(searchQuery, ignoreCase = true) ||
                    it.userName.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(filteredLogs) { log ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    log.action,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(log.userName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("(${log.roleName})", fontSize = 11.sp, color = Color.Gray)
                        }
                        Text(log.details, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Text(
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// =========================================================
// 9. DATABASE MAINTENANCE & POST-TESTING RESET
// =========================================================
@Composable
fun DatabaseMaintenanceSection(
    dbSize: String,
    onOptimize: () -> Unit,
    onCleanTemp: () -> Unit,
    onRefresh: () -> Unit,
    isSuperAdmin: Boolean = false,
    onResetTestData: (clearStudents: Boolean, clearTeachers: Boolean, clearAssessments: Boolean) -> Unit = { _, _, _ -> },
    onFactoryReset: () -> Unit = {}
) {
    var showResetTestDialog by remember { mutableStateOf(false) }
    var showFactoryResetDialog by remember { mutableStateOf(false) }

    var clearStudentsCheck by remember { mutableStateOf(false) }
    var clearTeachersCheck by remember { mutableStateOf(false) }
    var clearAssessmentsCheck by remember { mutableStateOf(false) }
    var factoryResetConfirmText by remember { mutableStateOf("") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("SQLite Database Maintenance", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Database File Size", fontSize = 12.sp, color = Color.Gray)
                            Text(dbSize, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh database file size")
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(onClick = onOptimize, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Optimize Database")
                        }

                        OutlinedButton(onClick = onCleanTemp, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Clean Temp Cache")
                        }
                    }
                }
            }
        }

        // SuperAdmin Post-Testing System Reset Panel
        if (isSuperAdmin) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    "SuperAdmin Post-Testing Reset",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Wipe test marks, pilot evaluations, and staging data before live academic run.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f))

                        Text(
                            "Finished testing the system? Use the buttons below to clean out mock exam marks, attendance records, holistic evaluations, and temporary logs so your database is clean for real-world school operations.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showResetTestDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Reset Test Data", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    factoryResetConfirmText = ""
                                    showFactoryResetDialog = true
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Factory Reset", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Reset Test Run Data
    if (showResetTestDialog) {
        AlertDialog(
            onDismissRequest = { showResetTestDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Reset Post-Testing Run Data?", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "This operation will permanently erase all testing transactions across marks, assessments, attendance sheets, holistic ratings, AI logs, and generated report histories.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Always cleared automatically:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Text("• Student marks & assessment results", fontSize = 11.sp)
                            Text("• Attendance daily check-ins & records", fontSize = 11.sp)
                            Text("• Holistic/SGI evaluation ratings & comments", fontSize = 11.sp)
                            Text("• Report card generation & PDF histories", fontSize = 11.sp)
                            Text("• AI assistant chats & temporary preview files", fontSize = 11.sp)
                        }
                    }

                    Text("Optional test roster purges:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = clearStudentsCheck,
                            onCheckedChange = { clearStudentsCheck = it }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Also delete enrolled test Students", fontSize = 12.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = clearTeachersCheck,
                            onCheckedChange = { clearTeachersCheck = it }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Also delete test Teacher staff accounts", fontSize = 12.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = clearAssessmentsCheck,
                            onCheckedChange = { clearAssessmentsCheck = it }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Also delete created Assessment schedules", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetTestData(clearStudentsCheck, clearTeachersCheck, clearAssessmentsCheck)
                        showResetTestDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm & Wipe Test Data")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetTestDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Full Factory Reset
    if (showFactoryResetDialog) {
        AlertDialog(
            onDismissRequest = { showFactoryResetDialog = false },
            icon = {
                Icon(
                    Icons.Default.Dangerous,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("Full Factory Reset", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "⚠️ CAUTION: This will wipe ALL custom school configurations, grading policies, student and teacher profiles, marks, and attendance, resetting the database back to standard Myanmar curriculum seed defaults.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "To proceed with full re-initialization, type 'RESET' below:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    OutlinedTextField(
                        value = factoryResetConfirmText,
                        onValueChange = { factoryResetConfirmText = it },
                        placeholder = { Text("RESET") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onFactoryReset()
                        showFactoryResetDialog = false
                    },
                    enabled = factoryResetConfirmText.trim().uppercase() == "RESET",
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Execute Factory Reset")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showFactoryResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =========================================================
// 10. NOTIFICATIONS
// =========================================================
@Composable
fun NotificationsSection(
    schoolSettings: SchoolSettingEntity,
    onSave: (SchoolSettingEntity) -> Unit
) {
    var sysNotif by remember(schoolSettings) { mutableStateOf(schoolSettings.systemNotificationsEnabled) }
    var backupRemind by remember(schoolSettings) { mutableStateOf(schoolSettings.backupRemindersEnabled) }
    var academicRemind by remember(schoolSettings) { mutableStateOf(schoolSettings.academicYearReminderEnabled) }
    var assessmentRemind by remember(schoolSettings) { mutableStateOf(schoolSettings.assessmentReminderEnabled) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Notification Rules & Reminders", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    SettingSwitchRow("Master System Notifications", "Enable banners & alerts for critical operations", sysNotif) {
                        sysNotif = it
                        onSave(schoolSettings.copy(systemNotificationsEnabled = it))
                    }
                    SettingSwitchRow("Weekly Backup Reminders", "Alert admins when no backup is created for 7 days", backupRemind) {
                        backupRemind = it
                        onSave(schoolSettings.copy(backupRemindersEnabled = it))
                    }
                    SettingSwitchRow("Academic Year Transition Alert", "Notify admins at year-end for promotion rollover", academicRemind) {
                        academicRemind = it
                        onSave(schoolSettings.copy(academicYearReminderEnabled = it))
                    }
                    SettingSwitchRow("Assessment Schedule & Lock Alert", "Remind teachers before mark entry locking dates", assessmentRemind) {
                        assessmentRemind = it
                        onSave(schoolSettings.copy(assessmentReminderEnabled = it))
                    }
                }
            }
        }
    }
}

// =========================================================
// 11. SECURITY & PERMISSIONS
// =========================================================
@Composable
fun SecurityAndPermissionsSection(
    securityPolicy: SecurityPolicyEntity,
    currentUserRole: UserRole,
    onSavePolicy: (SecurityPolicyEntity) -> Unit
) {
    var maxAttempts by remember(securityPolicy) { mutableStateOf(securityPolicy.maxFailedAttempts.toString()) }
    var sessionTimeout by remember(securityPolicy) { mutableStateOf(securityPolicy.sessionTimeoutMinutes.toString()) }
    var minPassLen by remember(securityPolicy) { mutableStateOf(securityPolicy.minPasswordLength.toString()) }
    var forcePassChange by remember(securityPolicy) { mutableStateOf(securityPolicy.forcePasswordChangeOnFirstLogin) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Authentication & Security Policy", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = maxAttempts,
                            onValueChange = { maxAttempts = it },
                            label = { Text("Max Failed Login Attempts") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sessionTimeout,
                            onValueChange = { sessionTimeout = it },
                            label = { Text("Session Timeout (Min)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = minPassLen,
                            onValueChange = { minPassLen = it },
                            label = { Text("Min Password Length") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SettingSwitchRow("Force Password Change", "Require initial password reset on first login", forcePassChange) { forcePassChange = it }

                    if (currentUserRole == UserRole.SUPER_ADMIN) {
                        Button(
                            onClick = {
                                onSavePolicy(
                                    securityPolicy.copy(
                                        maxFailedAttempts = maxAttempts.toIntOrNull() ?: 5,
                                        sessionTimeoutMinutes = sessionTimeout.toIntOrNull() ?: 30,
                                        minPasswordLength = minPassLen.toIntOrNull() ?: 6,
                                        forcePasswordChangeOnFirstLogin = forcePassChange
                                    )
                                )
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save Security Policy")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun UploadBase64LogoDialog(
    initialBase64: String? = null,
    onDismiss: () -> Unit,
    onConfirmBase64: (base64String: String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var inputText by remember { mutableStateOf(initialBase64 ?: "") }

    val decodedBitmap = remember(inputText) {
        com.example.ui.util.SchoolLogoUtils.decodeBase64ToBitmap(inputText)
    }
    val isValid = decodedBitmap != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Upload Logo as Base64", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Paste a Base64-encoded image string (data:image/...;base64,... or raw Base64 data). Once saved, it will be uploaded to the database and synced so all users see your logo across the app, top bar, and login screen.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Paste from Clipboard Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                inputText = clipText
                                Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Paste from Clipboard", fontSize = 10.sp)
                    }
                }

                // Text Input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Base64 String *") },
                    placeholder = { Text("e.g. data:image/png;base64,iVBORw0KGgo...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp, max = 150.dp),
                    trailingIcon = {
                        if (inputText.isNotEmpty()) {
                            IconButton(onClick = { inputText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    isError = inputText.isNotBlank() && !isValid,
                    supportingText = {
                        if (inputText.isNotBlank() && !isValid) {
                            Text("Invalid Base64 image data. Please verify your string.", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                        }
                    }
                )

                // Decoded Live Preview
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isValid) Color(0xFFF1F8E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (isValid) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isValid && decodedBitmap != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                Text(
                                    text = "Valid Image (${decodedBitmap.width} x ${decodedBitmap.height} px)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                            Surface(
                                modifier = Modifier.size(80.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                shadowElevation = 2.dp,
                                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                            ) {
                                Image(
                                    bitmap = decodedBitmap.asImageBitmap(),
                                    contentDescription = "Decoded Preview",
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } else {
                            Text(
                                text = "Preview will appear here once valid Base64 is entered",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirmBase64(inputText.trim())
                    }
                },
                enabled = isValid
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save & Upload to Database")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
