package com.example.ui.common

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.StoreProfile
import com.example.data.model.TransactionWithItems
import com.example.data.model.formatRupiah
import com.example.printer.LogoBitmapProcessor
import com.example.printer.PresetLogoType
import com.example.printer.PrinterConnectionState
import com.example.ui.localization.AppLocalization
import com.example.ui.localization.tr
import com.example.ui.localization.trDynamic
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.ReceiptPaper
import com.example.ui.theme.ReceiptText
import java.io.File

@Composable
fun ReceiptPreviewDialog(
    transactionWithItems: TransactionWithItems,
    storeProfile: StoreProfile,
    printerState: PrinterConnectionState,
    onPrint: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = AppLocalization.getStrings(storeProfile.languageCode)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header with title and close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = tr("Sukses", "Success"),
                            tint = AccentEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${tr("Nota Struk Digital", "Digital Receipt")} (${storeProfile.paperWidth}mm)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_receipt_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = strings.closeButton)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Realistic Paper Receipt Container
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ReceiptPaperCard(
                        transactionWithItems = transactionWithItems,
                        storeProfile = storeProfile
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Printer status badge
                val isConnected = printerState is PrinterConnectionState.Connected
                val printerLabel = when (printerState) {
                    is PrinterConnectionState.Connected -> "${tr("Terhubung", "Connected")}: ${printerState.deviceName}"
                    is PrinterConnectionState.Connecting -> tr("Menyambungkan...", "Connecting...")
                    is PrinterConnectionState.Error -> tr("Printer error / belum konek", "Printer error / disconnected")
                    is PrinterConnectionState.Disconnected -> tr("Printer belum terhubung", "Printer not connected")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isConnected) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Thermal Printer: $printerLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isConnected) Color(0xFF166534) else Color(0xFF92400E),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${storeProfile.paperWidth}mm",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Print & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_receipt_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = strings.shareButton)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.shareButton)
                    }

                    Button(
                        onClick = onPrint,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("print_receipt_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) AccentEmerald else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Print, contentDescription = strings.printButton)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.printButton)
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptPaperCard(
    transactionWithItems: TransactionWithItems,
    storeProfile: StoreProfile,
    modifier: Modifier = Modifier,
    showThermalHardwareSimulation: Boolean = false
) {
    val strings = AppLocalization.getStrings(storeProfile.languageCode)
    val cur = storeProfile.currencySymbol

    val headerTextAlign = when (storeProfile.headerAlignment.uppercase()) {
        "LEFT" -> TextAlign.Start
        "RIGHT" -> TextAlign.End
        else -> TextAlign.Center
    }

    val logoBoxAlignment = when (storeProfile.logoAlignment.uppercase()) {
        "LEFT" -> Alignment.CenterStart
        "RIGHT" -> Alignment.CenterEnd
        else -> Alignment.Center
    }

    val titleFontSizeSp = when (storeProfile.titleFontSize.uppercase()) {
        "NORMAL" -> 15.sp
        "LARGE" -> 18.sp
        else -> 21.sp
    }

    val logoDisplayDp = when {
        storeProfile.logoSizePx <= 130 -> 48.dp
        storeProfile.logoSizePx >= 240 -> 84.dp
        else -> 64.dp
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = ReceiptPaper),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (showThermalHardwareSimulation) {
                ReceiptTearEdgeGraphic(isTopEdge = true)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 1. Store Logo (Custom Image, 1-Bit Monochrome Thermal Simulation, or Preset Emblem)
                if (storeProfile.showLogo) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = logoBoxAlignment
                    ) {
                        val monochromeThermalBitmap = remember(
                            showThermalHardwareSimulation,
                            storeProfile.logoPresetType,
                            storeProfile.customLogoPath,
                            storeProfile.logoSizePx,
                            storeProfile.name
                        ) {
                            if (showThermalHardwareSimulation) {
                                try {
                                    val raw = LogoBitmapProcessor.loadOrGenerateLogoBitmap(
                                        presetCode = storeProfile.logoPresetType,
                                        customPath = storeProfile.customLogoPath,
                                        targetWidthPx = storeProfile.logoSizePx,
                                        storeNameInitials = storeProfile.name.take(3)
                                    )
                                    raw?.let { LogoBitmapProcessor.createMonochromePreviewBitmap(it) }
                                } catch (_: Exception) {
                                    null
                                }
                            } else {
                                null
                            }
                        }

                        val customBitmap = remember(storeProfile.logoPresetType, storeProfile.customLogoPath) {
                            if (storeProfile.logoPresetType == PresetLogoType.CUSTOM_IMAGE.code &&
                                storeProfile.customLogoPath.isNotBlank()
                            ) {
                                val file = File(storeProfile.customLogoPath)
                                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                            } else {
                                null
                            }
                        }

                        if (monochromeThermalBitmap != null) {
                            Image(
                                bitmap = monochromeThermalBitmap.asImageBitmap(),
                                contentDescription = "Logo Thermal 1-Bit",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .height(logoDisplayDp)
                                    .widthIn(min = logoDisplayDp, max = logoDisplayDp * 1.8f)
                                    .border(1.dp, ReceiptText.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(2.dp)
                            )
                        } else if (customBitmap != null) {
                            Image(
                                bitmap = customBitmap.asImageBitmap(),
                                contentDescription = "Logo Nota",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(logoDisplayDp)
                                    .border(1.dp, ReceiptText.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .padding(4.dp)
                            )
                        } else {
                            val preset = PresetLogoType.fromCode(storeProfile.logoPresetType)
                            val iconVec = when (preset) {
                                PresetLogoType.STORE_MART -> Icons.Default.Storefront
                                PresetLogoType.CAFE_COFFEE -> Icons.Default.LocalCafe
                                PresetLogoType.RESTAURANT -> Icons.Default.Restaurant
                                PresetLogoType.FASHION -> Icons.Default.Checkroom
                                PresetLogoType.ELECTRONICS -> Icons.Default.Devices
                                PresetLogoType.PHARMACY -> Icons.Default.LocalPharmacy
                                PresetLogoType.CUSTOM_IMAGE -> Icons.Default.Storefront
                            }
                            Box(
                                modifier = Modifier
                                    .size(logoDisplayDp)
                                    .border(2.dp, ReceiptText, RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVec,
                                    contentDescription = preset.label,
                                    tint = ReceiptText,
                                    modifier = Modifier.size(logoDisplayDp * 0.6f)
                                )
                            }
                        }
                    }
                }

            // 2. Store Header
            Text(
                text = storeProfile.name.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = titleFontSizeSp,
                    color = ReceiptText
                ),
                textAlign = headerTextAlign,
                modifier = Modifier.fillMaxWidth()
            )

            if (storeProfile.showSlogan && storeProfile.slogan.isNotBlank()) {
                Text(
                    text = storeProfile.slogan,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = ReceiptText.copy(alpha = 0.85f)
                    ),
                    textAlign = headerTextAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (storeProfile.showAddressPhone) {
                if (storeProfile.address.isNotBlank()) {
                    Text(
                        text = storeProfile.address,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = ReceiptText.copy(alpha = 0.8f)
                        ),
                        textAlign = headerTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (storeProfile.phone.isNotBlank()) {
                    Text(
                        text = "${tr("Telp", "Phone")}: ${storeProfile.phone}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = ReceiptText.copy(alpha = 0.8f)
                        ),
                        textAlign = headerTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (storeProfile.customHeaderNote.isNotBlank()) {
                Text(
                    text = storeProfile.customHeaderNote,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = ReceiptText
                    ),
                    textAlign = headerTextAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            ReceiptStyledDivider(storeProfile.dividerChar)

            // 3. Metadata (Invoice, Date, Cashier, Customer)
            if (storeProfile.showInvoiceNumber) {
                ReceiptRow(strings.receiptInvoiceNo, transactionWithItems.transaction.invoiceNumber)
            }
            if (storeProfile.showDateTime) {
                ReceiptRow(strings.receiptDate, transactionWithItems.transaction.formattedDate)
            }
            if (storeProfile.showCashierName) {
                ReceiptRow(strings.receiptCashier, transactionWithItems.transaction.cashierName)
            }
            if (storeProfile.showCustomerName) {
                ReceiptRow(strings.receiptCustomer, transactionWithItems.transaction.customerName)
            }

            if (storeProfile.showInvoiceNumber || storeProfile.showDateTime ||
                storeProfile.showCashierName || storeProfile.showCustomerName
            ) {
                ReceiptStyledDivider(storeProfile.dividerChar)
            }

            // 4. Items List
            transactionWithItems.items.forEachIndexed { index, item ->
                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                    val prefix = if (storeProfile.showItemNumber) "${index + 1}. " else ""
                    Text(
                        text = "$prefix${item.productName}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ReceiptText
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "  ${item.quantity} x ${formatRupiah(item.actualPrice, cur)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = ReceiptText.copy(alpha = 0.8f)
                            )
                        )
                        Text(
                            text = formatRupiah(item.subtotal, cur),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = ReceiptText
                            )
                        )
                    }
                    if (item.note.isNotBlank()) {
                        Text(
                            text = "  *${item.note}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF6B7280)
                            )
                        )
                    }
                }
            }

            ReceiptStyledDivider(storeProfile.dividerChar)

            // 5. Totals
            ReceiptRow(strings.receiptSubtotal, formatRupiah(transactionWithItems.transaction.subtotal, cur))
            if (storeProfile.showTaxAndDiscount) {
                if (transactionWithItems.transaction.discountAmount > 0) {
                    ReceiptRow(strings.receiptDiscount, "-${formatRupiah(transactionWithItems.transaction.discountAmount, cur)}")
                }
                if (transactionWithItems.transaction.taxAmount > 0) {
                    ReceiptRow(strings.receiptTax, formatRupiah(transactionWithItems.transaction.taxAmount, cur))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.receiptTotal,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = ReceiptText
                    )
                )
                Text(
                    text = formatRupiah(transactionWithItems.transaction.grandTotal, cur),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = ReceiptText
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            ReceiptRow(strings.receiptPaymentMethod, trDynamic(transactionWithItems.transaction.paymentMethod))
            ReceiptRow(strings.receiptPaid, formatRupiah(transactionWithItems.transaction.paidAmount, cur))
            ReceiptRow(strings.receiptChange, formatRupiah(transactionWithItems.transaction.changeAmount, cur))

            ReceiptStyledDivider(storeProfile.dividerChar)

            // 6. Signature Box if enabled
            if (storeProfile.showSignatureBox) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.receiptSignature,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = ReceiptText
                        )
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = "( .................... )",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = ReceiptText
                        )
                    )
                }
                ReceiptStyledDivider(storeProfile.dividerChar)
            }

            // 7. Footer message & Social Media
            val footerLines = storeProfile.receiptFooter.split("\n")
            for (line in footerLines) {
                if (line.isNotBlank()) {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = ReceiptText.copy(alpha = 0.75f)
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (storeProfile.showSocialMedia && storeProfile.socialMediaInfo.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = storeProfile.socialMediaInfo,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = ReceiptText.copy(alpha = 0.85f)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 8. Barcode graphic at bottom if enabled
            if (storeProfile.showBarcode) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ReceiptBarcodeGraphic(
                        code = transactionWithItems.transaction.invoiceNumber,
                        modifier = Modifier
                            .width(180.dp)
                            .height(34.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transactionWithItems.transaction.invoiceNumber,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = ReceiptText,
                            letterSpacing = 1.5.sp
                        )
                    )
                }
            }

            // 9. Simulated Feed Lines & Auto-Cut Indicator when in Thermal Hardware Simulation mode
            if (showThermalHardwareSimulation) {
                val safeFeed = storeProfile.feedLines.coerceIn(1, 10)
                Spacer(modifier = Modifier.height((safeFeed * 8).dp))
                Text(
                    text = "[ ${tr("Feed Kertas", "Paper Feed")}: $safeFeed ${tr("baris", "lines")} (ESC d $safeFeed) ]",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (storeProfile.autoCut) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "✂ - - - AUTO-CUTTER (GS V A 0) - - - ✂",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B7280)
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            }

            if (showThermalHardwareSimulation) {
                ReceiptTearEdgeGraphic(isTopEdge = false)
            }
        }
    }
}

@Composable
fun ReceiptTearEdgeGraphic(
    isTopEdge: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        val teethCount = 28
        val toothWidth = size.width / teethCount
        val path = Path()
        if (isTopEdge) {
            path.moveTo(0f, 0f)
            for (i in 0 until teethCount) {
                val xMid = (i + 0.5f) * toothWidth
                val xEnd = (i + 1f) * toothWidth
                path.lineTo(xMid, size.height)
                path.lineTo(xEnd, 0f)
            }
            path.close()
        } else {
            path.moveTo(0f, size.height)
            for (i in 0 until teethCount) {
                val xMid = (i + 0.5f) * toothWidth
                val xEnd = (i + 1f) * toothWidth
                path.lineTo(xMid, 0f)
                path.lineTo(xEnd, size.height)
            }
            path.close()
        }
        drawPath(
            path = path,
            color = Color(0xFFE5E7EB)
        )
    }
}

@Composable
fun ReceiptBarcodeGraphic(
    code: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barCount = 38
        val step = size.width / barCount
        for (i in 0 until barCount) {
            val charSeed = code.getOrElse(i % code.length.coerceAtLeast(1)) { 'A' }.code
            val isThick = (i + charSeed) % 3 == 0
            val isDrawn = i % 2 == 0 || (charSeed + i) % 5 == 0
            if (isDrawn) {
                val barW = if (isThick) step * 0.75f else step * 0.38f
                drawRect(
                    color = ReceiptText,
                    topLeft = Offset(i * step, 0f),
                    size = Size(barW, size.height)
                )
            }
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = ReceiptText.copy(alpha = 0.7f)
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = ReceiptText
            )
        )
    }
}

@Composable
fun ReceiptStyledDivider(dividerChar: Char = '-') {
    val pattern = remember(dividerChar) {
        (1..22).joinToString(" ") { dividerChar.toString() }
    }
    Text(
        text = pattern,
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF9CA3AF)
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}
