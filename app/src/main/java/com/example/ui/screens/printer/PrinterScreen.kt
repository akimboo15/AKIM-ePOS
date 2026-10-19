package com.example.ui.screens.printer

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.StoreProfile
import com.example.printer.BluetoothDeviceInfo
import com.example.printer.BluetoothDeviceType
import com.example.printer.PrinterConnectionState
import com.example.printer.ScanMode
import com.example.ui.localization.tr
import com.example.ui.localization.trDynamic
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrinterScreen(
    viewModel: PosViewModel
) {
    val printerState by viewModel.printerConnectionState.collectAsStateWithLifecycle()
    val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanningBluetooth.collectAsStateWithLifecycle()
    val scanStatusMessage by viewModel.scanStatusMessage.collectAsStateWithLifecycle()
    val selectedScanMode by viewModel.selectedScanMode.collectAsStateWithLifecycle()
    val filterPrintersOnly by viewModel.filterPrintersOnly.collectAsStateWithLifecycle()
    val storeProfile by viewModel.storeProfile.collectAsStateWithLifecycle()

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val hasAnyBtPermission = results.entries.any { it.value }
        if (hasAnyBtPermission) {
            viewModel.startBluetoothScan(selectedScanMode)
        } else {
            viewModel.refreshPairedDevices()
        }
    }

    val displayedDiscoveredDevices = if (filterPrintersOnly) {
        discoveredDevices.filter { it.isLikelyPrinter }
    } else {
        discoveredDevices
    }

    val displayedPairedDevices = if (filterPrintersOnly) {
        pairedDevices.filter { it.isLikelyPrinter }
    } else {
        pairedDevices
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTwoColumnTablet = maxWidth >= 700.dp

        if (isTwoColumnTablet) {
            // Tablet 2-Column Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .testTag("tablet_printer_two_column"),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Connection Status, Scanner Controls & Paper Settings
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PrinterConnectionBannerCard(
                        printerState = printerState,
                        onDisconnect = { viewModel.disconnectPrinter() }
                    )

                    BluetoothScannerControlCard(
                        isScanning = isScanning,
                        selectedScanMode = selectedScanMode,
                        filterPrintersOnly = filterPrintersOnly,
                        scanStatusMessage = scanStatusMessage,
                        onSelectScanMode = { viewModel.setScanMode(it) },
                        onToggleFilterPrinters = { viewModel.setFilterPrintersOnly(it) },
                        onStartScan = {
                            permissionLauncher.launch(permissionsToRequest)
                            viewModel.startBluetoothScan(selectedScanMode)
                        },
                        onStopScan = { viewModel.stopBluetoothScan() },
                        onTestPrint = { viewModel.testPrint() }
                    )

                    PaperAndEscPosConfigCard(
                        storeProfile = storeProfile,
                        onUpdateStoreProfile = { viewModel.updateStoreProfile(it) },
                        onOpenPrintPreview = { viewModel.openLiveCartPrintPreview() }
                    )
                }

                // Right Column: Discovered & Paired Devices + Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PrinterDevicesListSection(
                        printerState = printerState,
                        isScanning = isScanning,
                        filterPrintersOnly = filterPrintersOnly,
                        displayedDiscoveredDevices = displayedDiscoveredDevices,
                        displayedPairedDevices = displayedPairedDevices,
                        onConnectDevice = { viewModel.connectPrinter(it) },
                        onPairDevice = { viewModel.pairBluetoothDevice(it) }
                    )

                    PrinterCompatibilityInfoCard()
                }
            }
        } else {
            // Compact Single-Column Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PrinterConnectionBannerCard(
                    printerState = printerState,
                    onDisconnect = { viewModel.disconnectPrinter() }
                )

                BluetoothScannerControlCard(
                    isScanning = isScanning,
                    selectedScanMode = selectedScanMode,
                    filterPrintersOnly = filterPrintersOnly,
                    scanStatusMessage = scanStatusMessage,
                    onSelectScanMode = { viewModel.setScanMode(it) },
                    onToggleFilterPrinters = { viewModel.setFilterPrintersOnly(it) },
                    onStartScan = {
                        permissionLauncher.launch(permissionsToRequest)
                        viewModel.startBluetoothScan(selectedScanMode)
                    },
                    onStopScan = { viewModel.stopBluetoothScan() },
                    onTestPrint = { viewModel.testPrint() }
                )

                PrinterDevicesListSection(
                    printerState = printerState,
                    isScanning = isScanning,
                    filterPrintersOnly = filterPrintersOnly,
                    displayedDiscoveredDevices = displayedDiscoveredDevices,
                    displayedPairedDevices = displayedPairedDevices,
                    onConnectDevice = { viewModel.connectPrinter(it) },
                    onPairDevice = { viewModel.pairBluetoothDevice(it) }
                )

                PaperAndEscPosConfigCard(
                    storeProfile = storeProfile,
                    onUpdateStoreProfile = { viewModel.updateStoreProfile(it) },
                    onOpenPrintPreview = { viewModel.openLiveCartPrintPreview() }
                )

                PrinterCompatibilityInfoCard()
            }
        }
    }
}

@Composable
private fun PrinterConnectionBannerCard(
    printerState: PrinterConnectionState,
    onDisconnect: () -> Unit
) {
    val isConnected = printerState is PrinterConnectionState.Connected

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isConnected) AccentEmerald else MaterialTheme.colorScheme.primary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                    contentDescription = tr("Status Bluetooth", "Bluetooth Status"),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isConnected) tr("Printer Terhubung", "Printer Connected") else tr("Printer Belum Terhubung", "Printer Not Connected"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) Color(0xFF166534) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                when (val state = printerState) {
                    is PrinterConnectionState.Connected -> {
                        Text(
                            text = "${state.deviceName} (${state.address}) • ${state.protocol.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF15803D)
                        )
                    }
                    is PrinterConnectionState.Connecting -> {
                        Text(
                            text = "${tr("Menyambungkan ke", "Connecting to")} ${state.deviceName}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is PrinterConnectionState.Error -> {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    PrinterConnectionState.Disconnected -> {
                        Text(
                            text = tr("Pindai atau pilih printer thermal Bluetooth LE / Classic", "Scan or select a Bluetooth LE / Classic thermal printer"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isConnected) {
                OutlinedButton(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF991B1B)),
                    modifier = Modifier.testTag("disconnect_printer_button")
                ) {
                    Text(tr("Putus", "Disconnect"), fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BluetoothScannerControlCard(
    isScanning: Boolean,
    selectedScanMode: ScanMode,
    filterPrintersOnly: Boolean,
    scanStatusMessage: String?,
    onSelectScanMode: (ScanMode) -> Unit,
    onToggleFilterPrinters: (Boolean) -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onTestPrint: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr("Pemindai Bluetooth LE & Classic", "Bluetooth LE & Classic Scanner"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isScanning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("Memindai...", "Scanning..."),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = tr("Pilih protokol pemindaian untuk mendeteksi printer thermal Bluetooth Classic (SPP) maupun Bluetooth Low Energy (BLE) di sekitar:", "Select scan protocol to detect nearby Bluetooth Classic (SPP) and Bluetooth Low Energy (BLE) thermal printers:"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ScanMode.values().forEach { mode ->
                    FilterChip(
                        selected = selectedScanMode == mode,
                        onClick = { onSelectScanMode(mode) },
                        label = { Text(trDynamic(mode.label), fontSize = 12.sp) },
                        modifier = Modifier.testTag("scan_mode_${mode.name.lowercase()}")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = tr("Hanya Tampilkan Printer Thermal", "Only Show Thermal Printers"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = tr("Sembunyikan perangkat non-printer dari daftar", "Hide non-printer devices from list"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = filterPrintersOnly,
                    onCheckedChange = onToggleFilterPrinters,
                    modifier = Modifier.testTag("filter_printers_only_switch")
                )
            }

            AnimatedVisibility(visible = !scanStatusMessage.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = scanStatusMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isScanning) {
                    Button(
                        onClick = onStopScan,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stop_scan_bluetooth_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = tr("Berhenti", "Stop"))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Hentikan Pindai", "Stop Scan"))
                    }
                } else {
                    Button(
                        onClick = onStartScan,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scan_bluetooth_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = tr("Pindai", "Scan"))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Cari Printer", "Find Printers"))
                    }
                }

                Button(
                    onClick = onTestPrint,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_print_button")
                ) {
                    Icon(Icons.Default.Print, contentDescription = tr("Uji Cetak", "Test Print"))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr("Tes Cetak", "Test Print"))
                }
            }
        }
    }
}

@Composable
private fun PrinterDevicesListSection(
    printerState: PrinterConnectionState,
    isScanning: Boolean,
    filterPrintersOnly: Boolean,
    displayedDiscoveredDevices: List<BluetoothDeviceInfo>,
    displayedPairedDevices: List<BluetoothDeviceInfo>,
    onConnectDevice: (String) -> Unit,
    onPairDevice: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (displayedDiscoveredDevices.isNotEmpty() || isScanning) {
            Text(
                text = "${tr("Hasil Pemindaian Sekitar", "Nearby Scan Results")} (${displayedDiscoveredDevices.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (displayedDiscoveredDevices.isEmpty() && isScanning) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = tr("Mencari printer Bluetooth LE & Classic di sekitar...", "Searching for Bluetooth LE & Classic printers nearby..."),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (device in displayedDiscoveredDevices) {
                        val isThisDeviceConnected =
                            (printerState as? PrinterConnectionState.Connected)?.address == device.address
                        BluetoothDeviceCard(
                            device = device,
                            isConnected = isThisDeviceConnected,
                            onConnect = { onConnectDevice(device.address) },
                            onPair = { onPairDevice(device.address) }
                        )
                    }
                }
            }
        }

        Text(
            text = "${tr("Perangkat Bluetooth Terpasang", "Paired Bluetooth Devices")} (${displayedPairedDevices.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (displayedPairedDevices.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (filterPrintersOnly) {
                            tr("Tidak ada printer thermal terpasang", "No thermal printers paired")
                        } else {
                            tr("Tidak ada perangkat Bluetooth yang terpasang", "No Bluetooth devices paired")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tr("Tekan tombol 'Cari Printer' untuk memindai printer Bluetooth LE maupun Classic secara langsung.", "Tap 'Find Printers' to scan for Bluetooth LE and Classic printers directly."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (device in displayedPairedDevices) {
                    val isThisDeviceConnected =
                        (printerState as? PrinterConnectionState.Connected)?.address == device.address
                    BluetoothDeviceCard(
                        device = device,
                        isConnected = isThisDeviceConnected,
                        onConnect = { onConnectDevice(device.address) },
                        onPair = { onPairDevice(device.address) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaperAndEscPosConfigCard(
    storeProfile: StoreProfile,
    onUpdateStoreProfile: (StoreProfile) -> Unit,
    onOpenPrintPreview: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = tr("Pengaturan Kertas & Format ESC/POS", "Paper & ESC/POS Settings"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = tr("Lebar Kertas Thermal:", "Thermal Paper Width:"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = storeProfile.paperWidth == 58,
                        onClick = { onUpdateStoreProfile(storeProfile.copy(paperWidth = 58)) },
                        label = { Text(tr("58 mm (32 Kolom)", "58 mm (32 Cols)")) }
                    )
                    FilterChip(
                        selected = storeProfile.paperWidth == 80,
                        onClick = { onUpdateStoreProfile(storeProfile.copy(paperWidth = 80)) },
                        label = { Text(tr("80 mm (48 Kolom)", "80 mm (48 Cols)")) }
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("Kirim Perintah Potong Kertas (Auto-Cut)", "Send Paper Auto-Cut Command"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = tr("Untuk printer yang memiliki pemotong otomatis", "For printers equipped with automatic cutter"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = storeProfile.autoCut,
                        onCheckedChange = { onUpdateStoreProfile(storeProfile.copy(autoCut = it)) }
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${tr("Baris Kosong Akhir (Feed)", "End Feed Blank Lines")}: ${storeProfile.feedLines} ${tr("baris", "lines")}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row {
                        OutlinedButton(
                            onClick = {
                                if (storeProfile.feedLines > 1) {
                                    onUpdateStoreProfile(storeProfile.copy(feedLines = storeProfile.feedLines - 1))
                                }
                            }
                        ) {
                            Text("-")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedButton(
                            onClick = {
                                if (storeProfile.feedLines < 8) {
                                    onUpdateStoreProfile(storeProfile.copy(feedLines = storeProfile.feedLines + 1))
                                }
                            }
                        ) {
                            Text("+")
                        }
                    }
                }

                HorizontalDivider()

                OutlinedButton(
                    onClick = onOpenPrintPreview,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("printer_open_visual_preview_button")
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr("Buka Layar Pratinjau Visual Struk Real-Time", "Open Real-Time Receipt Visual Preview"), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PrinterCompatibilityInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = tr("Info Kompatibilitas Thermal Printer Universal (Classic & BLE):", "Universal Thermal Printer Compatibility Info (Classic & BLE):"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tr("Aplikasi ini mendukung pemindaian dan pencetakan dual-mode: Bluetooth Classic (RFCOMM SPP) dan Bluetooth Low Energy (BLE GATT). Kompatibel dengan printer kasir portable Panda, GOOJPRT, Iware, Eppos, VSC, BellaV, Zjiang, MPT, RPP02, Xprinter, dan merk universal lainnya.", "This app supports dual-mode scanning and printing: Bluetooth Classic (RFCOMM SPP) and Bluetooth Low Energy (BLE GATT). Compatible with portable receipt printers like Panda, GOOJPRT, Iware, Eppos, VSC, BellaV, Zjiang, MPT, RPP02, Xprinter, and other universal brands."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BluetoothDeviceCard(
    device: BluetoothDeviceInfo,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onPair: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConnect() }
            .testTag("device_card_${device.address.replace(":", "")}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surface
        ),
        border = when {
            isConnected -> BorderStroke(1.5.dp, AccentEmerald)
            device.isLikelyPrinter -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            else -> null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when {
                            isConnected -> Icons.Default.CheckCircle
                            device.isLikelyPrinter -> Icons.Default.Print
                            else -> Icons.Default.Bluetooth
                        },
                        contentDescription = null,
                        tint = when {
                            isConnected -> AccentEmerald
                            device.isLikelyPrinter -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = device.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isConnected) {
                    Surface(
                        color = AccentEmerald.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = tr("Aktif", "Active"),
                            fontWeight = FontWeight.Bold,
                            color = AccentEmerald,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!device.isBonded && device.deviceType != BluetoothDeviceType.LE) {
                            OutlinedButton(
                                onClick = onPair,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("pair_btn_${device.address.replace(":", "")}")
                            ) {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = "Pair",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(tr("Pair", "Pair"), fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = onConnect,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("connect_btn_${device.address.replace(":", "")}")
                        ) {
                            Text(tr("Konek", "Connect"), fontSize = 12.sp)
                        }
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val protocolBg = when (device.deviceType) {
                    BluetoothDeviceType.LE -> Color(0xFFE0F2FE)
                    BluetoothDeviceType.DUAL -> Color(0xFFF3E8FF)
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
                val protocolTextCol = when (device.deviceType) {
                    BluetoothDeviceType.LE -> Color(0xFF0369A1)
                    BluetoothDeviceType.DUAL -> Color(0xFF6B21A8)
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                }
                Surface(
                    color = protocolBg,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = device.deviceType.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = protocolTextCol,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (device.isLikelyPrinter) {
                    Surface(
                        color = AccentEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = tr("Printer Thermal", "Thermal Printer"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentEmerald,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    color = if (device.isBonded) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        AccentAmber.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (device.isBonded) tr("Terpasang", "Paired") else tr("Belum Dipasangkan", "Not Paired"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (device.isBonded) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            AccentAmber
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (device.rssi != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SignalCellularAlt,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = trDynamic(device.signalQualityLabel),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
