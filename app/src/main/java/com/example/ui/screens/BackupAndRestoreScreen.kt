package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.preferences.ShopSettings
import com.example.localization.Formatters
import com.example.localization.LocalAppStrings
import com.example.ui.components.AmarDokanTopBar
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TileCyan

@Composable
fun BackupAndRestoreScreen(
    settings: ShopSettings,
    hasAutoBackup: Boolean,
    onExportBackupUri: (Uri) -> Unit,
    onImportBackupUri: (Uri) -> Unit,
    onSaveAutoBackupNow: () -> Unit,
    onRestoreAutoBackup: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isBn = strings.isBengali

    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackupUri(uri)
    }

    val openJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportBackupUri(uri)
    }

    Scaffold(
        topBar = {
            AmarDokanTopBar(title = strings.backupAndRestore, onBack = onBack)
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
            // Cloud Sync Status Notice
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = InfoBlue.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = InfoBlue)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(strings.cloudSyncTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(strings.cloudSyncDeclinedMsg, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Local Auto-Backup Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = ProfitGreen)
                        Text(
                            text = if (isBn) "অফলাইন SQLite ও অটো-ব্যাকআপ" else "Offline SQLite & Auto-Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (settings.lastBackupTimestamp > 0L) {
                        Text(
                            text = "${if (isBn) "সর্বশেষ ব্যাকআপ:" else "Last backup:"} ${Formatters.formatDateTime(settings.lastBackupTimestamp, isBn)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onSaveAutoBackupNow,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isBn) "ব্যাকআপ নিন" else "Backup Now")
                        }
                        OutlinedButton(
                            onClick = onRestoreAutoBackup,
                            enabled = hasAutoBackup,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.restoreLastAutoBackup)
                        }
                    }
                }
            }

            // JSON Export / Import Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.FileCopy, contentDescription = null, tint = TileCyan)
                        Text(
                            text = if (isBn) "JSON ফাইল এক্সপোর্ট ও রিস্টোর" else "JSON File Export & Restore",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (isBn) "সব পণ্য, বিক্রয়, ক্রয়, বাকি খাতা, ক্যাশবুক ও খরচের সম্পূর্ণ ব্যাকআপ JSON ফাইল হিসেবে ফোনে বা Google Drive-এ সেভ করুন।"
                        else "Export all products, sales, purchases, due ledger, cashbook, and expenses as a portable JSON file to your device or Google Drive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { createJsonLauncher.launch("amar_dokan_backup_${System.currentTimeMillis()}.json") },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.exportJsonBackup)
                    }
                    OutlinedButton(
                        onClick = { openJsonLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.importJsonBackup)
                    }
                }
            }
        }
    }
}
