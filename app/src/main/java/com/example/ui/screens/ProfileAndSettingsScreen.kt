package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.AuditLogEntity
import com.example.data.preferences.LockType
import com.example.data.preferences.ShopSettings
import com.example.localization.*
import com.example.ui.components.AmarDokanTopBar
import com.example.ui.theme.*
import com.example.utils.ShareAndMediaHelper
import java.io.File

@Composable
fun ProfileAndSettingsScreen(
    settings: ShopSettings,
    auditLogs: List<AuditLogEntity>,
    onUpdateShopProfile: (name: String, owner: String, phone: String, address: String, logoUri: String) -> Unit,
    onUpdateCurrency: (AppCurrency) -> Unit,
    onUpdateLanguage: (AppLanguage) -> Unit,
    onUpdateDateFormat: (DateFormatOption) -> Unit,
    onUpdateDarkMode: (Boolean) -> Unit,
    onUpdateSecurity: (LockType, String?, Boolean) -> Unit,
    onUpdateBackupSettings: (Boolean, Boolean) -> Unit,
    onNavigateToFinancialOverview: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onLockAppNow: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isBn = strings.isBengali
    val context = LocalContext.current

    var showProfileDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showAuditDialog by remember { mutableStateOf(false) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* handled gracefully */ }

    Scaffold(
        topBar = {
            AmarDokanTopBar(title = strings.navProfile)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Shop Profile Card
            Card(
                onClick = { showProfileDialog = true },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (settings.shopLogoUri.isNotBlank() && File(settings.shopLogoUri).exists()) {
                        AsyncImage(
                            model = File(settings.shopLogoUri),
                            contentDescription = settings.shopName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(60.dp).clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(60.dp).clip(CircleShape).background(TealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(settings.shopName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            settings.ownerName.ifBlank { strings.shopProfile },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (settings.shopPhone.isNotBlank()) {
                            Text(settings.shopPhone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.Edit, contentDescription = strings.edit, tint = TealPrimary)
                }
            }

            // Quick Actions: Financial Overview, Backup, Security, Audit
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text(strings.businessFinancialOverview, fontWeight = FontWeight.SemiBold) },
                        leadingContent = { Icon(Icons.Default.Analytics, contentDescription = null, tint = TealPrimary) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable(onClick = onNavigateToFinancialOverview)
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(strings.backupAndRestore, fontWeight = FontWeight.SemiBold) },
                        leadingContent = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = TileCyan) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable(onClick = onNavigateToBackup)
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(strings.securityLock, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(settings.lockType.name) },
                        leadingContent = { Icon(Icons.Default.Lock, contentDescription = null, tint = TilePurple) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable { showSecurityDialog = true }
                    )
                    if (settings.lockType != LockType.NONE) {
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text(if (isBn) "অ্যাপ এখনই লক করুন" else "Lock App Now", fontWeight = FontWeight.SemiBold) },
                            leadingContent = { Icon(Icons.Default.LockClock, contentDescription = null, tint = WarningAmber) },
                            modifier = Modifier.clickable(onClick = onLockAppNow)
                        )
                    }
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(strings.auditHistory, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("${Formatters.toLocalizedDigits(auditLogs.size.toString(), isBn)}") },
                        leadingContent = { Icon(Icons.Default.History, contentDescription = null, tint = TileBlue) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable { showAuditDialog = true }
                    )
                }
            }

            // Preferences
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings.settings, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // Language
                    Text(strings.language, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = settings.language == lang,
                                onClick = { onUpdateLanguage(lang) },
                                label = { Text(lang.displayName) },
                                modifier = Modifier.testTag("lang_${lang.code}")
                            )
                        }
                    }

                    // Currency
                    Text(strings.currency, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppCurrency.entries.forEach { cur ->
                            FilterChip(
                                selected = settings.currency == cur,
                                onClick = { onUpdateCurrency(cur) },
                                label = { Text(cur.displayName) },
                                modifier = Modifier.testTag("currency_${cur.code}")
                            )
                        }
                    }

                    // Date Format
                    Text(strings.dateFormat, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateFormatOption.entries.forEach { fmt ->
                            FilterChip(
                                selected = settings.dateFormat == fmt,
                                onClick = { onUpdateDateFormat(fmt) },
                                label = { Text(fmt.label) }
                            )
                        }
                    }

                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(strings.darkMode, style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = settings.darkMode, onCheckedChange = onUpdateDarkMode)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isBn) "অটো ব্যাকআপ" else "Auto Backup", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = settings.autoBackupEnabled,
                            onCheckedChange = { onUpdateBackupSettings(it, settings.lowStockNotificationsEnabled) }
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isBn) "কম স্টক নোটিফিকেশন" else "Low Stock Notifications", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = settings.lowStockNotificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                onUpdateBackupSettings(settings.autoBackupEnabled, enabled)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        var name by remember { mutableStateOf(settings.shopName) }
        var owner by remember { mutableStateOf(settings.ownerName) }
        var phone by remember { mutableStateOf(settings.shopPhone) }
        var address by remember { mutableStateOf(settings.shopAddress) }
        var logoUri by remember { mutableStateOf(settings.shopLogoUri) }
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                ShareAndMediaHelper.copyPickedImageToInternalStorage(context, uri)?.let { logoUri = it }
            }
        }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text(strings.shopProfile) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (isBn) "দোকানের লোগো বাছাই করুন" else "Pick Shop Logo")
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.shopName) }, singleLine = true)
                    OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text(strings.ownerName) }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(strings.phone) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(strings.address) }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateShopProfile(name.trim(), owner.trim(), phone.trim(), address.trim(), logoUri)
                    showProfileDialog = false
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showSecurityDialog) {
        var lockType by remember { mutableStateOf(settings.lockType) }
        var secret by remember { mutableStateOf("") }
        var bio by remember { mutableStateOf(settings.biometricEnabled) }
        AlertDialog(
            onDismissRequest = { showSecurityDialog = false },
            title = { Text(strings.securityLock) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = lockType == LockType.NONE, onClick = { lockType = LockType.NONE }, label = { Text(if (isBn) "বন্ধ" else "Off") })
                        FilterChip(selected = lockType == LockType.PIN, onClick = { lockType = LockType.PIN }, label = { Text("PIN") })
                        FilterChip(selected = lockType == LockType.PASSWORD, onClick = { lockType = LockType.PASSWORD }, label = { Text(if (isBn) "পাসওয়ার্ড" else "Password") })
                    }
                    if (lockType != LockType.NONE) {
                        OutlinedTextField(
                            value = secret,
                            onValueChange = { secret = it },
                            label = { Text(if (lockType == LockType.PIN) strings.pinLock else strings.passwordLock) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (lockType == LockType.PIN) KeyboardType.NumberPassword else KeyboardType.Password
                            ),
                            singleLine = true
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(strings.biometricUnlock)
                            Switch(checked = bio, onCheckedChange = { bio = it })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateSecurity(lockType, secret.ifBlank { null }, bio)
                        showSecurityDialog = false
                    },
                    enabled = lockType == LockType.NONE || secret.length >= 4 || settings.lockSecretHash.isNotEmpty()
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showSecurityDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showAuditDialog) {
        AlertDialog(
            onDismissRequest = { showAuditDialog = false },
            title = { Text(strings.auditHistory) },
            text = {
                if (auditLogs.isEmpty()) Text(strings.noData)
                else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(auditLogs, key = { it.id }) { log ->
                            Column {
                                Text("${log.entityType} • ${log.action}", style = MaterialTheme.typography.labelMedium, color = TealPrimary, fontWeight = FontWeight.Bold)
                                Text(log.summary, style = MaterialTheme.typography.bodySmall)
                                Text(Formatters.formatDateTime(log.timestamp, isBn), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAuditDialog = false }) { Text(strings.close) }
            }
        )
    }
}
