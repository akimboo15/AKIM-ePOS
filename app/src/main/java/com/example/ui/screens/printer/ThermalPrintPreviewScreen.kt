package com.example.ui.screens.printer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StoreProfile
import com.example.data.model.TransactionWithItems
import com.example.data.model.formatRupiah
import com.example.printer.BluetoothDeviceInfo
import com.example.printer.EscPosPrinter
import com.example.printer.PrinterConnectionState
import com.example.ui.common.ReceiptPaperCard
import com.example.ui.localization.AppLocalization
import com.example.ui.localization.tr
import com.example.ui.localization.trDynamic
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.ReceiptPaper
import com.example.ui.theme.ReceiptText

enum class ReceiptPreviewMode {
    VISUAL_PAPER,
    ESCPOS_MATRIX
}

@Composable
fun ThermalPrintPreviewScreen(
    transactionWithItems: TransactionWithItems,
    storeProfile: StoreProfile,
    printerState: PrinterConnectionState,
    pairedDevices: List<BluetoothDeviceInfo>,
    isDraftPreview: Boolean = false,
    onUpdateStoreProfile: (StoreProfile) -> Unit,
    onConnectPrinter: (String) -> Unit,
    onOpenPrinterScanner: () -> Unit,
    onTriggerPrint: (StoreProfile, Int) -> Unit,
    onShareReceipt: (TransactionWithItems) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    // Local real-time state synchronized with StoreProfile so adjustments reflect immediately on the receipt preview
    var liveProfile by remember(storeProfile) { mutableStateOf(storeProfile) }
    var previewMode by remember { mutableStateOf(ReceiptPreviewMode.VISUAL_PAPER) }
    var showHardwareSim by remember { mutableStateOf(true) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var copyCount by remember { mutableIntStateOf(1) }
    var showCompactControls by remember { mutableStateOf(false) }
    var lastPrintTriggeredMessage by remember { mutableStateOf<String?>(null) }

    val strings = AppLocalization.getStrings(liveProfile.languageCode)

    // Real-time ESC/POS payload & character matrix calculation
    val escPosBytes = remember(liveProfile, transactionWithItems) {
        EscPosPrinter.buildReceiptBytes(
            store = liveProfile,
            transaction = transactionWithItems.transaction,
            items = transactionWithItems.items
        )
    }

    val plainTextMatrix = remember(liveProfile, transactionWithItems) {
        EscPosPrinter.generatePlainTextReceipt(
            store = liveProfile,
            transaction = transactionWithItems.transaction,
            items = transactionWithItems.items
        )
    }

    val charColumns = if (liveProfile.paperWidth == 80) 48 else 32
    val printHeadDots = if (liveProfile.paperWidth == 80) 576 else 384

    fun applyLiveProfileUpdate(updated: StoreProfile) {
        liveProfile = updated
        onUpdateStoreProfile(updated)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("thermal_print_preview_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar with Close/Back, Invoice Info & Real-Time Payload Telemetry
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("close_receipt_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.closeButton
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = tr("Pratinjau Cetak Struk Real-Time", "Real-Time Thermal Print Preview"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    color = if (isDraftPreview) {
                                        AccentAmber.copy(alpha = 0.2f)
                                    } else {
                                        AccentEmerald.copy(alpha = 0.2f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isDraftPreview) tr("DRAFT KASIR", "CASHIER DRAFT") else tr("SIAP CETAK", "READY TO PRINT"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDraftPreview) Color(0xFFB45309) else Color(0xFF047857),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${transactionWithItems.transaction.invoiceNumber} • ${tr("Kertas", "Paper")} ${liveProfile.paperWidth}mm ($charColumns ${tr("Kolom", "Cols")} / $printHeadDots Dots) • ${escPosBytes.size} Bytes ESC/POS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("dismiss_print_preview_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.closeButton
                        )
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val isTabletOrWide = maxWidth >= 600.dp

                if (isTabletOrWide) {
                    // Canonical Two-Column Split Layout for Tablets & Wide Screens
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("tablet_print_preview_split_layout")
                    ) {
                        // Left Pane: Real-Time Visual Paper / Matrix Preview Stage
                        Column(
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxHeight()
                        ) {
                            PreviewToolbarStrip(
                                previewMode = previewMode,
                                onSelectMode = { previewMode = it },
                                showHardwareSim = showHardwareSim,
                                onToggleHardwareSim = { showHardwareSim = it },
                                zoomScale = zoomScale,
                                onZoomIn = { zoomScale = (zoomScale + 0.1f).coerceAtMost(1.25f) },
                                onZoomOut = { zoomScale = (zoomScale - 0.1f).coerceAtLeast(0.8f) },
                                paperWidthMm = liveProfile.paperWidth,
                                charColumns = charColumns
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                LiveReceiptPreviewCanvas(
                                    transactionWithItems = transactionWithItems,
                                    storeProfile = liveProfile,
                                    previewMode = previewMode,
                                    showHardwareSim = showHardwareSim,
                                    zoomScale = zoomScale,
                                    plainTextMatrix = plainTextMatrix,
                                    escPosByteCount = escPosBytes.size,
                                    charColumns = charColumns
                                )
                            }
                        }

                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                        // Right Pane: Real-Time Print Controls, Bluetooth Readiness & Print Trigger
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    BluetoothReadinessCard(
                                        printerState = printerState,
                                        pairedDevices = pairedDevices,
                                        onConnectPrinter = onConnectPrinter,
                                        onOpenPrinterScanner = onOpenPrinterScanner
                                    )

                                    RealTimePrintControlsCard(
                                        liveProfile = liveProfile,
                                        copyCount = copyCount,
                                        onCopyCountChange = { copyCount = it },
                                        onUpdateProfile = { applyLiveProfileUpdate(it) }
                                    )

                                    EscPosTelemetrySummaryCard(
                                        storeProfile = liveProfile,
                                        byteCount = escPosBytes.size,
                                        lineCount = plainTextMatrix.lines().size,
                                        charColumns = charColumns,
                                        printHeadDots = printHeadDots
                                    )

                                    lastPrintTriggeredMessage?.let { msg ->
                                        PrintDispatchedBanner(message = msg)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                PrintActionBottomButtons(
                                    printerState = printerState,
                                    copyCount = copyCount,
                                    shareLabel = strings.shareButton,
                                    printLabel = strings.printButton,
                                    onShare = { onShareReceipt(transactionWithItems) },
                                    onPrint = {
                                        onTriggerPrint(liveProfile, copyCount)
                                        lastPrintTriggeredMessage =
                                            "Perintah cetak Bluetooth (${escPosBytes.size} bytes × ${copyCount}x) dikirim ke printer!"
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Compact Smartphone Layout: Preview Stage + Collapsible Live Controls + Sticky Bottom Bar
                    Column(modifier = Modifier.fillMaxSize()) {
                        PreviewToolbarStrip(
                            previewMode = previewMode,
                            onSelectMode = { previewMode = it },
                            showHardwareSim = showHardwareSim,
                            onToggleHardwareSim = { showHardwareSim = it },
                            zoomScale = zoomScale,
                            onZoomIn = { zoomScale = (zoomScale + 0.1f).coerceAtMost(1.25f) },
                            onZoomOut = { zoomScale = (zoomScale - 0.1f).coerceAtLeast(0.8f) },
                            paperWidthMm = liveProfile.paperWidth,
                            charColumns = charColumns
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LiveReceiptPreviewCanvas(
                                transactionWithItems = transactionWithItems,
                                storeProfile = liveProfile,
                                previewMode = previewMode,
                                showHardwareSim = showHardwareSim,
                                zoomScale = zoomScale,
                                plainTextMatrix = plainTextMatrix,
                                escPosByteCount = escPosBytes.size,
                                charColumns = charColumns
                            )

                            // Collapsible Real-Time Controls Card on Compact Phones
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showCompactControls = !showCompactControls }
                                            .testTag("toggle_realtime_print_controls"),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = tr("Pengaturan Cetak & Kertas Real-Time", "Real-Time Print & Paper Settings"),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${liveProfile.paperWidth}mm • ${copyCount}x ${tr("Salinan", "Copies")} • Feed ${liveProfile.feedLines} ${tr("baris", "lines")}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = if (showCompactControls) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null
                                        )
                                    }

                                    AnimatedVisibility(visible = showCompactControls) {
                                        Column(
                                            modifier = Modifier.padding(top = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            RealTimePrintControlsCard(
                                                liveProfile = liveProfile,
                                                copyCount = copyCount,
                                                onCopyCountChange = { copyCount = it },
                                                onUpdateProfile = { applyLiveProfileUpdate(it) }
                                            )
                                            BluetoothReadinessCard(
                                                printerState = printerState,
                                                pairedDevices = pairedDevices,
                                                onConnectPrinter = onConnectPrinter,
                                                onOpenPrinterScanner = onOpenPrinterScanner
                                            )
                                        }
                                    }
                                }
                            }

                            lastPrintTriggeredMessage?.let { msg ->
                                PrintDispatchedBanner(message = msg)
                            }
                        }

                        // Sticky Bottom Bar on Compact Screen
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CompactPrinterStatusStrip(
                                    printerState = printerState,
                                    paperWidth = liveProfile.paperWidth,
                                    onOpenPrinterScanner = onOpenPrinterScanner
                                )

                                PrintActionBottomButtons(
                                    printerState = printerState,
                                    copyCount = copyCount,
                                    shareLabel = strings.shareButton,
                                    printLabel = strings.printButton,
                                    onShare = { onShareReceipt(transactionWithItems) },
                                    onPrint = {
                                        onTriggerPrint(liveProfile, copyCount)
                                        lastPrintTriggeredMessage =
                                            "Perintah cetak Bluetooth (${escPosBytes.size} bytes × ${copyCount}x) dikirim ke printer!"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewToolbarStrip(
    previewMode: ReceiptPreviewMode,
    onSelectMode: (ReceiptPreviewMode) -> Unit,
    showHardwareSim: Boolean,
    onToggleHardwareSim: (Boolean) -> Unit,
    zoomScale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    paperWidthMm: Int,
    charColumns: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = previewMode == ReceiptPreviewMode.VISUAL_PAPER,
                onClick = { onSelectMode(ReceiptPreviewMode.VISUAL_PAPER) },
                label = { Text("${tr("Visual Kertas", "Visual Paper")} (${paperWidthMm}mm)", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("preview_mode_visual_paper")
            )

            FilterChip(
                selected = previewMode == ReceiptPreviewMode.ESCPOS_MATRIX,
                onClick = { onSelectMode(ReceiptPreviewMode.ESCPOS_MATRIX) },
                label = { Text("${tr("Matriks ESC/POS", "ESC/POS Matrix")} ($charColumns ${tr("Kolom", "Cols")})", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("preview_mode_escpos_matrix")
            )

            if (previewMode == ReceiptPreviewMode.VISUAL_PAPER) {
                FilterChip(
                    selected = showHardwareSim,
                    onClick = { onToggleHardwareSim(!showHardwareSim) },
                    label = { Text(tr("Simulasi Thermal 1-Bit", "1-Bit Thermal Simulation"), fontSize = 12.sp) },
                    modifier = Modifier.testTag("toggle_thermal_hardware_sim")
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 4.dp)
            ) {
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = tr("Perkecil", "Zoom Out"), modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "${(zoomScale * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = tr("Perbesar", "Zoom In"), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun LiveReceiptPreviewCanvas(
    transactionWithItems: TransactionWithItems,
    storeProfile: StoreProfile,
    previewMode: ReceiptPreviewMode,
    showHardwareSim: Boolean,
    zoomScale: Float,
    plainTextMatrix: String,
    escPosByteCount: Int,
    charColumns: Int
) {
    val basePaperWidthDp = if (storeProfile.paperWidth == 80) 396.dp else 308.dp
    val scaledPaperWidthDp = basePaperWidthDp * zoomScale

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Thermal Roll Dimension Ruler Header
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Text(
                text = " ${tr("Lebar Kertas Thermal", "Thermal Paper Width")}: ${storeProfile.paperWidth}mm • $charColumns ${tr("Karakter/Baris", "Chars/Line")} ",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        if (previewMode == ReceiptPreviewMode.VISUAL_PAPER) {
            ReceiptPaperCard(
                transactionWithItems = transactionWithItems,
                storeProfile = storeProfile,
                showThermalHardwareSimulation = showHardwareSim,
                modifier = Modifier
                    .widthIn(max = scaledPaperWidthDp)
                    .testTag("live_visual_receipt_paper_card")
            )
        } else {
            // ESC/POS Character Matrix & Command Stream View
            Card(
                modifier = Modifier
                    .widthIn(max = (scaledPaperWidthDp + 32.dp))
                    .fillMaxWidth()
                    .testTag("live_escpos_matrix_card"),
                colors = CardDefaults.cardColors(containerColor = ReceiptPaper),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFD1D5DB))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "[0x1B 0x40] ESC/POS INIT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        )
                        Text(
                            text = "$escPosByteCount Bytes",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4B5563)
                            )
                        )
                    }

                    if (storeProfile.showLogo) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "[0x1D 0x76 0x30] GS v 0 RASTER LOGO (${storeProfile.logoSizePx}px, align=${storeProfile.logoAlignment})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color(0xFF1D4ED8)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Column Ruler (1..32 or 1..48)
                    val rulerDigits = (1..charColumns).joinToString("") { (it % 10).toString() }
                    Text(
                        text = rulerDigits,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = (11f * zoomScale).sp,
                            color = Color(0xFF9CA3AF)
                        ),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color(0xFFD1D5DB)
                    )

                    Text(
                        text = plainTextMatrix,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = (11.5f * zoomScale).sp,
                            lineHeight = (15f * zoomScale).sp,
                            color = ReceiptText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color(0xFFD1D5DB)
                    )

                    Text(
                        text = "[0x1B 0x64 0x0${storeProfile.feedLines}] FEED ${storeProfile.feedLines} LINES" +
                            if (storeProfile.autoCut) "  •  [0x1D 0x56 0x41] AUTO-CUT" else "",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B7280)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RealTimePrintControlsCard(
    liveProfile: StoreProfile,
    copyCount: Int,
    onCopyCountChange: (Int) -> Unit,
    onUpdateProfile: (StoreProfile) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tr("Penyesuaian Cepat Sebelum Cetak", "Quick Adjustments Before Print"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // 1. Paper Width (58mm vs 80mm)
            Text(
                text = tr("Lebar Kertas Printer Thermal:", "Thermal Printer Paper Width:"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = liveProfile.paperWidth == 58,
                    onClick = { onUpdateProfile(liveProfile.copy(paperWidth = 58)) },
                    label = { Text(tr("58mm (32 Kolom)", "58mm (32 Cols)")) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preview_paper_58mm_chip")
                )
                FilterChip(
                    selected = liveProfile.paperWidth == 80,
                    onClick = { onUpdateProfile(liveProfile.copy(paperWidth = 80)) },
                    label = { Text(tr("80mm (48 Kolom)", "80mm (48 Cols)")) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preview_paper_80mm_chip")
                )
            }

            // 2. Number of Copies to Print (1x, 2x, 3x)
            Text(
                text = tr("Jumlah Salinan Cetak (Copies):", "Print Copies:"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    1 to "1x (${tr("Pelanggan", "Customer")})",
                    2 to "2x (+${tr("Arsip", "Archive")})",
                    3 to "3x ${tr("Salinan", "Copies")}"
                ).forEach { (count, label) ->
                    FilterChip(
                        selected = copyCount == count,
                        onClick = { onCopyCountChange(count) },
                        label = { Text(label, fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("print_copy_count_$count")
                    )
                }
            }

            // 3. Quick Toggles: Logo, Barcode, Auto-Cut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = liveProfile.showLogo,
                    onClick = { onUpdateProfile(liveProfile.copy(showLogo = !liveProfile.showLogo)) },
                    label = { Text("Logo", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(15.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = liveProfile.showBarcode,
                    onClick = { onUpdateProfile(liveProfile.copy(showBarcode = !liveProfile.showBarcode)) },
                    label = { Text("Barcode", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(15.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = liveProfile.autoCut,
                    onClick = { onUpdateProfile(liveProfile.copy(autoCut = !liveProfile.autoCut)) },
                    label = { Text("Auto-Cut", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(15.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 4. Feed Lines Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = tr("Spasi Kertas Keluar (Feed Lines)", "Paper Feed Lines"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${tr("Perintah ESC d", "ESC d command")} ${liveProfile.feedLines} ${tr("baris di akhir nota", "lines at end of receipt")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = {
                            val next = (liveProfile.feedLines - 1).coerceAtLeast(1)
                            onUpdateProfile(liveProfile.copy(feedLines = next))
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = tr("Kurangi Feed", "Reduce Feed"), modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "${liveProfile.feedLines}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(
                        onClick = {
                            val next = (liveProfile.feedLines + 1).coerceAtMost(8)
                            onUpdateProfile(liveProfile.copy(feedLines = next))
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = tr("Tambah Feed", "Increase Feed"), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BluetoothReadinessCard(
    printerState: PrinterConnectionState,
    pairedDevices: List<BluetoothDeviceInfo>,
    onConnectPrinter: (String) -> Unit,
    onOpenPrinterScanner: () -> Unit
) {
    val isConnected = printerState is PrinterConnectionState.Connected
    val statusText = when (printerState) {
        is PrinterConnectionState.Connected -> "${tr("Terhubung ke", "Connected to")} ${printerState.deviceName} (${printerState.address})"
        is PrinterConnectionState.Connecting -> "${tr("Sedang menyambungkan ke", "Connecting to")} ${printerState.deviceName}..."
        is PrinterConnectionState.Error -> "${tr("Koneksi bermasalah", "Connection error")}: ${printerState.message}"
        is PrinterConnectionState.Disconnected -> tr("Printer Bluetooth belum terhubung", "Bluetooth printer not connected")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) {
                Color(0xFFDCFCE7).copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isConnected) AccentEmerald.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) AccentEmerald else MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = if (isConnected) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isConnected) tr("Printer Thermal Siap Cetak", "Thermal Printer Ready") else tr("Status Printer Thermal", "Thermal Printer Status"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) Color(0xFF065F46) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isConnected) Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenPrinterScanner,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("preview_open_printer_scanner_button")
                ) {
                    Text(tr("Kelola", "Manage"), fontSize = 11.sp)
                }
            }

            // Quick 1-Tap Connect for Paired Printers if currently disconnected
            if (!isConnected && pairedDevices.isNotEmpty()) {
                Text(
                    text = tr("Sambung Cepat Printer Tersimpan:", "Quick Connect Saved Printers:"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pairedDevices.take(4).forEach { device ->
                        FilterChip(
                            selected = false,
                            onClick = { onConnectPrinter(device.address) },
                            label = { Text(device.name, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EscPosTelemetrySummaryCard(
    storeProfile: StoreProfile,
    byteCount: Int,
    lineCount: Int,
    charColumns: Int,
    printHeadDots: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = tr("Spesifikasi Payload ESC/POS Bluetooth", "Bluetooth ESC/POS Payload Specs"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "• ${tr("Resolusi Printhead", "Printhead Resolution")}: $printHeadDots dots/${tr("baris", "line")} ($charColumns ${tr("karakter Font A", "Font A characters")})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "• ${tr("Total Baris Teks", "Total Text Lines")}: $lineCount ${tr("baris", "lines")} • ${tr("Ukuran Data", "Data Size")}: $byteCount bytes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "• ${tr("Komando Hardware", "Hardware Commands")}: ESC @ (Init)" +
                    (if (storeProfile.showLogo) " + GS v 0 (Bitmap Logo)" else "") +
                    " + ESC d ${storeProfile.feedLines} (Feed)" +
                    (if (storeProfile.autoCut) " + GS V A (Auto-Cut)" else ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompactPrinterStatusStrip(
    printerState: PrinterConnectionState,
    paperWidth: Int,
    onOpenPrinterScanner: () -> Unit
) {
    val isConnected = printerState is PrinterConnectionState.Connected
    val label = when (printerState) {
        is PrinterConnectionState.Connected -> "${tr("Terhubung", "Connected")}: ${printerState.deviceName}"
        is PrinterConnectionState.Connecting -> tr("Menyambungkan...", "Connecting...")
        is PrinterConnectionState.Error -> tr("Printer error / belum konek", "Printer error / disconnected")
        is PrinterConnectionState.Disconnected -> tr("Printer belum terhubung (Ketuk untuk atur)", "Printer disconnected (Tap to manage)")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isConnected) Color(0xFFDCFCE7) else Color(0xFFFEF3C7))
            .clickable { if (!isConnected) onOpenPrinterScanner() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                contentDescription = null,
                tint = if (isConnected) Color(0xFF166534) else Color(0xFF92400E),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (isConnected) Color(0xFF166534) else Color(0xFF92400E),
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = "${paperWidth}mm",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isConnected) Color(0xFF166534) else Color(0xFF92400E)
        )
    }
}

@Composable
private fun PrintDispatchedBanner(message: String) {
    Surface(
        color = Color(0xFFDCFCE7),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("print_dispatched_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF15803D),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF166534)
            )
        }
    }
}

@Composable
private fun PrintActionBottomButtons(
    printerState: PrinterConnectionState,
    copyCount: Int,
    shareLabel: String,
    printLabel: String,
    onShare: () -> Unit,
    onPrint: () -> Unit
) {
    val isConnected = printerState is PrinterConnectionState.Connected

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onShare,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("share_receipt_button"),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = shareLabel, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(shareLabel, fontWeight = FontWeight.SemiBold)
        }

        Button(
            onClick = onPrint,
            modifier = Modifier
                .weight(1.45f)
                .height(48.dp)
                .testTag("print_receipt_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isConnected) AccentEmerald else MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Print, contentDescription = printLabel, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (copyCount > 1) "$printLabel (${copyCount}x)" else "$printLabel Bluetooth",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
