package com.example.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.BackupManager
import com.example.data.repository.LocalBackupFileInfo
import com.example.ui.localization.tr
import com.example.ui.theme.AccentEmerald
import com.example.ui.viewmodel.PosViewModel

@Composable
fun DatabaseBackupSectionCard(
    viewModel: PosViewModel
) {
    val localBackups by viewModel.localBackupFiles.collectAsStateWithLifecycle()
    var replaceExistingOnImport by remember { mutableStateOf(false) }
    var restoreStoreSettingsOnImport by remember { mutableStateOf(true) }
    var pendingRestoreBackup by remember { mutableStateOf<LocalBackupFileInfo?>(null) }

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportDatabaseBackupToUri(uri)
        }
    }

    val importDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importDatabaseBackupFromUri(
                uri = uri,
                replaceExisting = replaceExistingOnImport,
                restoreStoreSettings = restoreStoreSettingsOnImport
            )
        }
    }

    pendingRestoreBackup?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingRestoreBackup = null },
            title = {
                Text(
                    text = tr("Pulihkan Database dari Backup?", "Restore Database from Backup?"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${tr("File", "File")}: ${backup.fileName}")
                    Text("${tr("Isi Backup", "Backup Content")}: ${backup.productCount} ${tr("Item Produk", "Products")} & ${backup.transactionCount} ${tr("Transaksi Nota", "Receipt Transactions")}")
                    Text("${tr("Tanggal Backup", "Backup Date")}: ${backup.formattedDate}")
                    HorizontalDivider()
                    Text(
                        text = if (replaceExistingOnImport) {
                            tr("Mode: Ganti Semua Data (Data saat ini akan diganti dengan isi file backup).", "Mode: Replace All Data (Current data will be overwritten by backup content).")
                        } else {
                            tr("Mode: Gabungkan Data (Item & transaksi baru ditambahkan tanpa menghapus data lama).", "Mode: Merge Data (New items & transactions added without deleting old data).")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (replaceExistingOnImport) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importDatabaseBackupFromLocalFile(
                            filePath = backup.filePath,
                            replaceExisting = replaceExistingOnImport,
                            restoreStoreSettings = restoreStoreSettingsOnImport
                        )
                        pendingRestoreBackup = null
                    },
                    modifier = Modifier.testTag("confirm_restore_backup_button")
                ) {
                    Text(tr("Ya, Pulihkan Sekarang", "Yes, Restore Now"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreBackup = null }) {
                    Text(tr("Batal", "Cancel"))
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("database_backup_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Backup,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = tr("Backup & Restore Database Lokal (JSON)", "Local Database Backup & Restore (JSON)"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr("Ekspor daftar item toko & riwayat transaksi ke file backup lokal dan impor kembali kapan saja", "Export store product catalog & transaction history to local backup files and import back anytime"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // Export Section
            Text(
                text = tr("1. Ekspor Database (Daftar Item & Transaksi)", "1. Export Database (Products & Transactions)"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.exportDatabaseBackupToLocal() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("settings_create_local_backup_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr("Backup Lokal", "Local Backup"),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        exportDocumentLauncher.launch(BackupManager.generateDefaultBackupFileName())
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("settings_export_backup_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr("Simpan ke File", "Save to File"),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider()

            // Import Section & Mode Options
            Text(
                text = tr("2. Opsi & Impor Database dari File Backup", "2. Options & Import Database from Backup"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !replaceExistingOnImport,
                    onClick = { replaceExistingOnImport = false },
                    label = { Text(tr("Gabungkan (Merge)", "Merge")) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("import_mode_merge")
                )
                FilterChip(
                    selected = replaceExistingOnImport,
                    onClick = { replaceExistingOnImport = true },
                    label = { Text(tr("Ganti Semua (Replace)", "Replace All")) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("import_mode_replace")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = tr("Pulihkan Juga Profil & Layout Nota Toko", "Also Restore Store Profile & Receipt Layout"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = tr("Ikut menerapkan pengaturan profil toko, bahasa, tema & layout struk dari file backup", "Also apply store profile, language, theme & receipt layout settings from backup file"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = restoreStoreSettingsOnImport,
                    onCheckedChange = { restoreStoreSettingsOnImport = it },
                    modifier = Modifier.testTag("switch_restore_store_settings")
                )
            }

            Button(
                onClick = {
                    importDocumentLauncher.launch(
                        arrayOf("application/json", "text/plain", "*/*")
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_import_backup_file_button")
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr("Impor dari File Backup Perangkat (.json)", "Import from Device Backup File (.json)"),
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            // Saved Local Backups Vault List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${tr("Daftar File Backup Lokal", "Local Backup Files List")} (${localBackups.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(
                    onClick = { viewModel.refreshLocalBackups() },
                    modifier = Modifier.testTag("refresh_local_backups_button")
                ) {
                    Text(tr("Muat Ulang", "Reload"))
                }
            }

            if (localBackups.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = tr("Belum ada file backup lokal. Tekan \"Backup Lokal\" atau \"Simpan ke File\" untuk membuat cadangan database.", "No local backup files yet. Tap 'Local Backup' or 'Save to File' to create database backups."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    localBackups.forEachIndexed { index, backup ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("local_backup_item_$index"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = backup.fileName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${backup.formattedDate} • ${backup.formattedSize} • ${backup.storeName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "${backup.productCount} ${tr("Item", "Items")} • ${backup.transactionCount} ${tr("Nota", "Receipts")}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { pendingRestoreBackup = backup },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("restore_local_backup_$index")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Restore,
                                            contentDescription = tr("Pulihkan", "Restore"),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tr("Pulihkan (Import)", "Restore (Import)"),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = { viewModel.shareBackupJson(backup.filePath) },
                                        modifier = Modifier.testTag("share_local_backup_$index")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = tr("Bagikan File Backup", "Share Backup File"),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteLocalBackup(backup.filePath) },
                                        modifier = Modifier.testTag("delete_local_backup_$index")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = tr("Hapus File Backup", "Delete Backup File"),
                                            tint = MaterialTheme.colorScheme.error
                                        )
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
