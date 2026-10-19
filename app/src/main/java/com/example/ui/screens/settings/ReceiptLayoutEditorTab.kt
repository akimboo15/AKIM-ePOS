package com.example.ui.screens.settings

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionWithItems
import com.example.printer.PresetLogoType
import com.example.ui.common.ReceiptPaperCard
import com.example.ui.localization.AppLocalization
import com.example.ui.localization.tr
import com.example.ui.localization.trDynamic
import com.example.ui.theme.AccentEmerald
import com.example.ui.viewmodel.PosViewModel
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReceiptLayoutEditorTab(
    storeProfile: StoreProfile,
    viewModel: PosViewModel
) {
    val strings = AppLocalization.getStrings(storeProfile.languageCode)

    // Local editable state synced with storeProfile
    var showLogo by remember(storeProfile) { mutableStateOf(storeProfile.showLogo) }
    var logoPresetType by remember(storeProfile) { mutableStateOf(storeProfile.logoPresetType) }
    var logoSizePx by remember(storeProfile) { mutableIntStateOf(storeProfile.logoSizePx) }
    var logoAlignment by remember(storeProfile) { mutableStateOf(storeProfile.logoAlignment) }

    var paperWidth by remember(storeProfile) { mutableIntStateOf(storeProfile.paperWidth) }
    var headerAlignment by remember(storeProfile) { mutableStateOf(storeProfile.headerAlignment) }
    var titleFontSize by remember(storeProfile) { mutableStateOf(storeProfile.titleFontSize) }
    var dividerStyle by remember(storeProfile) { mutableStateOf(storeProfile.dividerStyle) }
    var autoCut by remember(storeProfile) { mutableStateOf(storeProfile.autoCut) }
    var feedLines by remember(storeProfile) { mutableIntStateOf(storeProfile.feedLines) }

    var showSlogan by remember(storeProfile) { mutableStateOf(storeProfile.showSlogan) }
    var showAddressPhone by remember(storeProfile) { mutableStateOf(storeProfile.showAddressPhone) }
    var showInvoiceNumber by remember(storeProfile) { mutableStateOf(storeProfile.showInvoiceNumber) }
    var showDateTime by remember(storeProfile) { mutableStateOf(storeProfile.showDateTime) }
    var showCashierName by remember(storeProfile) { mutableStateOf(storeProfile.showCashierName) }
    var showCustomerName by remember(storeProfile) { mutableStateOf(storeProfile.showCustomerName) }
    var showItemNumber by remember(storeProfile) { mutableStateOf(storeProfile.showItemNumber) }
    var showTaxAndDiscount by remember(storeProfile) { mutableStateOf(storeProfile.showTaxAndDiscount) }
    var showSocialMedia by remember(storeProfile) { mutableStateOf(storeProfile.showSocialMedia) }
    var showBarcode by remember(storeProfile) { mutableStateOf(storeProfile.showBarcode) }
    var showSignatureBox by remember(storeProfile) { mutableStateOf(storeProfile.showSignatureBox) }

    var showLivePreview by remember { mutableStateOf(true) }

    // Zero-permission Android Photo Picker for custom receipt logo
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            logoPresetType = PresetLogoType.CUSTOM_IMAGE.code
            showLogo = true
            viewModel.saveCustomLogoFromUri(uri)
        }
    }

    // Build live preview StoreProfile reflecting current unsaved toggles
    val previewProfile = storeProfile.copy(
        showLogo = showLogo,
        logoPresetType = logoPresetType,
        logoSizePx = logoSizePx,
        logoAlignment = logoAlignment,
        paperWidth = paperWidth,
        headerAlignment = headerAlignment,
        titleFontSize = titleFontSize,
        dividerStyle = dividerStyle,
        autoCut = autoCut,
        feedLines = feedLines,
        showSlogan = showSlogan,
        showAddressPhone = showAddressPhone,
        showInvoiceNumber = showInvoiceNumber,
        showDateTime = showDateTime,
        showCashierName = showCashierName,
        showCustomerName = showCustomerName,
        showItemNumber = showItemNumber,
        showTaxAndDiscount = showTaxAndDiscount,
        showSocialMedia = showSocialMedia,
        showBarcode = showBarcode,
        showSignatureBox = showSignatureBox
    )

    val sampleTransaction = remember(storeProfile.defaultCashier) {
        TransactionWithItems(
            transaction = Transaction(
                id = 1,
                invoiceNumber = "INV-20261019-8821",
                customerName = "Budi Santoso",
                cashierName = storeProfile.defaultCashier,
                paymentMethod = "QRIS / Tunai",
                subtotal = 53000.0,
                discountAmount = 3000.0,
                taxAmount = 5500.0,
                grandTotal = 55500.0,
                paidAmount = 60000.0,
                changeAmount = 4500.0
            ),
            items = listOf(
                TransactionItem(
                    id = 1,
                    transactionId = 1,
                    productId = 1,
                    productName = "Kopi Susu Gula Aren",
                    originalPrice = 18000.0,
                    actualPrice = 18000.0,
                    quantity = 2,
                    subtotal = 36000.0,
                    note = "Less Ice"
                ),
                TransactionItem(
                    id = 2,
                    transactionId = 1,
                    productId = 2,
                    productName = "Roti Bakar Cokelat Keju",
                    originalPrice = 17000.0,
                    actualPrice = 17000.0,
                    quantity = 1,
                    subtotal = 17000.0
                )
            )
        )
    }

    val livePreviewCard: @Composable () -> Unit = {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLivePreview = !showLivePreview }
                        .testTag("toggle_live_receipt_preview"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = strings.previewReceiptButton,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${tr("Kertas", "Paper")} ${paperWidth}mm • ${tr("Bahasa", "Language")}: ${storeProfile.languageCode.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = if (showLivePreview) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = tr("Buka/Tutup Pratinjau", "Toggle Preview")
                    )
                }

                AnimatedVisibility(visible = showLivePreview) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        ReceiptPaperCard(
                            transactionWithItems = sampleTransaction,
                            storeProfile = previewProfile
                        )
                    }
                }
            }
        }
    }

    val editorControlsContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 2. Receipt Logo Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = tr("Logo Nota / Struk Toko", "Store Receipt Logo"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr("Cetak grafik logo bitmap pada bagian atas struk", "Print bitmap graphic logo on top of receipt"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = showLogo,
                        onCheckedChange = { showLogo = it },
                        modifier = Modifier.testTag("switch_show_logo")
                    )
                }

                AnimatedVisibility(visible = showLogo) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HorizontalDivider()

                        Text(
                            text = tr("Pilih Ikon Logo Bawaan atau Unggah Gambar Galeri:", "Choose Built-in Logo Icon or Upload from Gallery:"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Preset Logo Selector Grid
                        val presets = listOf(
                            Triple(PresetLogoType.STORE_MART, Icons.Default.Storefront, tr("Minimarket", "Minimarket")),
                            Triple(PresetLogoType.CAFE_COFFEE, Icons.Default.LocalCafe, tr("Kedai Kopi", "Coffee Shop")),
                            Triple(PresetLogoType.RESTAURANT, Icons.Default.Restaurant, tr("Resto / Kuliner", "Restaurant / Food")),
                            Triple(PresetLogoType.FASHION, Icons.Default.Checkroom, tr("Fashion / Butik", "Fashion / Boutique")),
                            Triple(PresetLogoType.ELECTRONICS, Icons.Default.Devices, tr("Elektronik", "Electronics")),
                            Triple(PresetLogoType.PHARMACY, Icons.Default.LocalPharmacy, tr("Apotek", "Pharmacy"))
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEach { (preset, iconVec, shortLabel) ->
                                val isSelected = logoPresetType == preset.code
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { logoPresetType = preset.code },
                                    label = { Text(shortLabel) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = iconVec,
                                            contentDescription = shortLabel,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("logo_preset_${preset.code.lowercase()}")
                                )
                            }
                        }

                        // Upload Custom Image from Gallery Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("upload_custom_logo_button")
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = tr("Pilih Gambar", "Select Image"))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(tr("Pilih Gambar Logo dari Galeri", "Choose Logo Image from Gallery"))
                            }

                            if (storeProfile.customLogoPath.isNotBlank()) {
                                val thumb = remember(storeProfile.customLogoPath) {
                                    val f = File(storeProfile.customLogoPath)
                                    if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                                }
                                if (thumb != null) {
                                    Image(
                                        bitmap = thumb.asImageBitmap(),
                                        contentDescription = tr("Thumbnail Logo Kustom", "Custom Logo Thumbnail"),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                2.dp,
                                                if (logoPresetType == PresetLogoType.CUSTOM_IMAGE.code) AccentEmerald else Color.Gray,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { logoPresetType = PresetLogoType.CUSTOM_IMAGE.code }
                                    )
                                }
                            }
                        }

                        // Logo Size & Alignment
                        Text(
                            text = tr("Ukuran Logo Thermal:", "Thermal Logo Size:"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(120 to tr("Kecil", "Small"), 184 to tr("Sedang", "Medium"), 256 to tr("Besar", "Large")).forEach { (px, label) ->
                                FilterChip(
                                    selected = logoSizePx == px,
                                    onClick = { logoSizePx = px },
                                    label = { Text("$label (${px}px)") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Text(
                            text = tr("Posisi Perataan Logo:", "Logo Alignment Position:"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        AlignmentSelectorRow(
                            selectedAlignment = logoAlignment,
                            onSelect = { logoAlignment = it }
                        )
                    }
                }
            }
        }

        // 3. Complete Receipt Layout Editor Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = tr("Editor Tata Letak (Layout) Nota", "Receipt Layout Editor"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr("Kustomisasi format kertas, tipografi, garis, dan elemen struk", "Customize paper format, typography, divider lines, and receipt elements"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                // Paper Width
                Text(
                    text = tr("Ukuran Lebar Kertas Thermal:", "Thermal Paper Width:"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = paperWidth == 58,
                        onClick = { paperWidth = 58 },
                        label = { Text(tr("58mm (32 Kolom Standar)", "58mm (Standard 32 Cols)")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("layout_paper_58")
                    )
                    FilterChip(
                        selected = paperWidth == 80,
                        onClick = { paperWidth = 80 },
                        label = { Text(tr("80mm (48 Kolom Lebar)", "80mm (Wide 48 Cols)")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("layout_paper_80")
                    )
                }

                // Header Text Alignment
                Text(
                    text = tr("Perataan Teks Header Toko:", "Store Header Text Alignment:"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                AlignmentSelectorRow(
                    selectedAlignment = headerAlignment,
                    onSelect = { headerAlignment = it }
                )

                // Store Title Font Size
                Text(
                    text = tr("Ukuran Font Nama Toko:", "Store Title Font Size:"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "NORMAL" to tr("Normal", "Normal"),
                        "LARGE" to tr("Tinggi (Large)", "Tall (Large)"),
                        "DOUBLE" to tr("Besar (2x)", "Large (2x)")
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = titleFontSize == code,
                            onClick = { titleFontSize = code },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Divider Character Style
                Text(
                    text = tr("Gaya Garis Pemisah Nota:", "Receipt Divider Line Style:"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "DASH" to tr("- - - (Strip)", "- - - (Dash)"),
                        "EQUAL" to tr("= = = (Ganda)", "= = = (Double)"),
                        "STAR" to tr("* * * (Bintang)", "* * * (Star)"),
                        "DOT" to tr(". . . (Titik)", ". . . (Dot)")
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = dividerStyle == code,
                            onClick = { dividerStyle = code },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = tr("Komponen yang Ditampilkan pada Nota:", "Components Displayed on Receipt:"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                LayoutToggleRow(
                    title = tr("Tampilkan Slogan Toko", "Show Store Slogan"),
                    subtitle = tr("Cetak subjudul/slogan di bawah nama toko", "Print subtitle/slogan below store name"),
                    checked = showSlogan,
                    onCheckedChange = { showSlogan = it }
                )
                LayoutToggleRow(
                    title = tr("Tampilkan Alamat & Telepon", "Show Address & Phone"),
                    subtitle = tr("Cetak lokasi toko dan nomor kontak di header", "Print store location and contact number in header"),
                    checked = showAddressPhone,
                    onCheckedChange = { showAddressPhone = it }
                )
                LayoutToggleRow(
                    title = tr("Tampilkan Nomor Nota (Invoice)", "Show Receipt Invoice Number"),
                    subtitle = tr("Cetak kode unik transaksi (cth: INV-2026...)", "Print unique transaction code (e.g. INV-2026...)"),
                    checked = showInvoiceNumber,
                    onCheckedChange = { showInvoiceNumber = it }
                )
                LayoutToggleRow(
                    title = tr("Tampilkan Tanggal & Jam", "Show Date & Time"),
                    subtitle = tr("Cetak waktu transaksi berlangsung", "Print transaction timestamp"),
                    checked = showDateTime,
                    onCheckedChange = { showDateTime = it }
                )
                LayoutToggleRow(
                    title = tr("Tampilkan Nama Kasir", "Show Cashier Name"),
                    subtitle = tr("Cetak nama operator/kasir yang bertugas", "Print cashier operator on duty"),
                    checked = showCashierName,
                    onCheckedChange = { showCashierName = it }
                )
                LayoutToggleRow(
                    title = tr("Tampilkan Nama Pelanggan", "Show Customer Name"),
                    subtitle = tr("Cetak nama pembeli pada informasi nota", "Print customer name on receipt info"),
                    checked = showCustomerName,
                    onCheckedChange = { showCustomerName = it }
                )
                LayoutToggleRow(
                    title = tr("Tampilkan Nomor Urut Item (1, 2, 3...)", "Show Item Row Numbers (1, 2, 3...)"),
                    subtitle = tr("Beri penomoran di depan setiap nama barang", "Add item numbers before item names"),
                    checked = showItemNumber,
                    onCheckedChange = { showItemNumber = it }
                )
                LayoutToggleRow(
                    title = tr("Tampilkan Rincian Diskon & Pajak", "Show Discount & Tax Breakdown"),
                    subtitle = tr("Cetak baris potongan diskon dan PPN jika ada", "Print discount and tax lines if applied"),
                    checked = showTaxAndDiscount,
                    onCheckedChange = { showTaxAndDiscount = it }
                )
                LayoutToggleRow(
                    title = tr("Tampilkan Info Sosial Media / WiFi", "Show Social Media / WiFi Info"),
                    subtitle = tr("Cetak akun IG, WhatsApp, atau password WiFi di bawah footer", "Print IG, WhatsApp, or WiFi password below footer"),
                    checked = showSocialMedia,
                    onCheckedChange = { showSocialMedia = it }
                )
                LayoutToggleRow(
                    title = tr("Tampilkan Kolom Tanda Tangan", "Show Signature Box"),
                    subtitle = tr("Tambahkan area tanda tangan / Hormat Kami untuk nota resmi", "Add signature / authorized by area for formal receipts"),
                    checked = showSignatureBox,
                    onCheckedChange = { showSignatureBox = it }
                )
                LayoutToggleRow(
                    title = tr("Tampilkan Barcode Nomor Nota", "Show Invoice Barcode"),
                    subtitle = tr("Cetak grafik barcode invoice di bagian paling bawah struk", "Print invoice barcode graphic at the bottom"),
                    checked = showBarcode,
                    onCheckedChange = { showBarcode = it }
                )

                HorizontalDivider()

                // Paper Feed & Auto Cut
                Text(
                    text = "${tr("Spasi Kertas Akhir Setelah Cetak", "End Paper Feed After Print")}: $feedLines ${tr("Baris", "Lines")}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = feedLines.toFloat(),
                    onValueChange = { feedLines = it.toInt() },
                    valueRange = 1f..6f,
                    steps = 4
                )

                LayoutToggleRow(
                    title = tr("Potong Kertas Otomatis (Auto-Cut ESC/POS)", "Automatic Paper Cut (Auto-Cut ESC/POS)"),
                    subtitle = tr("Kirim perintah pemotong kertas untuk printer yang mendukung cutter", "Send cutter command for printers equipped with auto-cutter"),
                    checked = autoCut,
                    onCheckedChange = { autoCut = it }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Action Buttons: Save Layout, Test Print, Reset
                Button(
                    onClick = {
                        viewModel.updateStoreProfile(
                            previewProfile,
                            "Pengaturan logo & layout nota berhasil disimpan!"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_receipt_layout_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.saveSettingsButton, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.updateStoreProfile(previewProfile)
                            viewModel.printCurrentReceipt(sampleTransaction)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_print_layout_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Cetak Contoh Nota", "Print Sample Receipt"))
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetReceiptLayout() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reset_receipt_layout_button")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Reset Default", "Reset to Default"))
                    }
                }
            }
        }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isTwoColumnTablet = maxWidth >= 700.dp
        if (isTwoColumnTablet) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tablet_receipt_editor_two_column"),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1.2f)) {
                    editorControlsContent()
                }
                Box(modifier = Modifier.weight(1f)) {
                    livePreviewCard()
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                livePreviewCard()
                editorControlsContent()
            }
        }
    }
}

@Composable
private fun AlignmentSelectorRow(
    selectedAlignment: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val options = listOf(
            Triple("LEFT", tr("Rata Kiri", "Left"), Icons.Default.FormatAlignLeft),
            Triple("CENTER", tr("Tengah", "Center"), Icons.Default.FormatAlignCenter),
            Triple("RIGHT", tr("Rata Kanan", "Right"), Icons.Default.FormatAlignRight)
        )
        options.forEach { (code, label, icon) ->
            FilterChip(
                selected = selectedAlignment.equals(code, ignoreCase = true),
                onClick = { onSelect(code) },
                label = { Text(label) },
                leadingIcon = {
                    Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LayoutToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
