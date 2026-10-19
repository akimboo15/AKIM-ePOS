package com.example.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.localization.tr

/**
 * Dialog onboarding dan tutorial interaktif yang tampil saat awal aplikasi dibuka (fresh install),
 * atau saat pengguna memilih menu Bantuan & Tutorial di Pengaturan.
 */
@Composable
fun AppOnboardingDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val totalSteps = 5

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 580.dp)
                .heightIn(max = 680.dp)
                .testTag("onboarding_welcome_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Bar: Step Indicators & Skip Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Step Dots Indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(totalSteps) { step ->
                            val isActive = step == currentStep
                            val isCompleted = step < currentStep
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(if (isActive) 24.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isActive -> MaterialTheme.colorScheme.primary
                                            isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        }
                                    )
                            )
                        }
                    }

                    if (currentStep < totalSteps - 1) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("onboarding_skip_button")
                        ) {
                            Text(
                                text = tr("Lewati", "Skip"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Scrollable Content Pane with Step Transition
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "OnboardingStepAnimation"
                    ) { step ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            when (step) {
                                0 -> WelcomeStepContent()
                                1 -> CashierTutorialStepContent()
                                2 -> PrinterTutorialStepContent()
                                3 -> FeaturesTourStepContent()
                                4 -> ReadyToSellStepContent()
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("onboarding_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Kembali", "Back"))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (currentStep < totalSteps - 1) {
                        Button(
                            onClick = { currentStep++ },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("onboarding_next_button")
                        ) {
                            Text(
                                text = when (currentStep) {
                                    0 -> tr("Mulai Tutorial ➔", "Start Tutorial ➔")
                                    1 -> tr("Lanjut: Struk ➔", "Next: Receipts ➔")
                                    2 -> tr("Lanjut: Semua Fitur ➔", "Next: All Features ➔")
                                    else -> tr("Selanjutnya ➔", "Next ➔")
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("onboarding_finish_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr("Mulai Berjualan Sekarang! 🚀", "Start Selling Now! 🚀"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 0: WELCOME CONTENT
// -------------------------------------------------------------
@Composable
private fun WelcomeStepContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App Badge / Logo
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(86.dp)
            ) {}
            Image(
                painter = painterResource(id = R.drawable.img_akim_epos_logo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }

        Text(
            text = tr("Selamat Datang di AKIM ePOS!", "Welcome to AKIM ePOS!"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = tr(
                "Aplikasi Kasir Pintar & Cetak Struk Bluetooth Modern untuk Toko Retail, UMKM, Kafe, & Warung Anda.",
                "Modern Smart Cashier & Bluetooth Receipt Printing Solution for your Retail Store, SME, Cafe, & Shop."
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Feature Highlights Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OnboardingFeatureMiniCard(
                icon = Icons.Default.PointOfSale,
                title = tr("Transaksi Kasir Cepat & Mudah", "Fast & Easy POS Checkout"),
                desc = tr("Keranjang belanja responsif, kalkulator kembalian instan, & barcode scan.", "Responsive cart, instant change calculator, & camera barcode scan.")
            )
            OnboardingFeatureMiniCard(
                icon = Icons.Default.Print,
                title = tr("Cetak Struk Thermal Bluetooth", "Bluetooth Thermal Receipt Printing"),
                desc = tr("Mendukung kertas 58mm & 80mm, pratinjau nota visual, dan auto-print.", "Supports 58mm & 80mm paper, visual receipt preview, and auto-print.")
            )
            OnboardingFeatureMiniCard(
                icon = Icons.Default.Inventory,
                title = tr("Manajemen Katalog & Laporan Omset", "Catalog & Revenue Analytics"),
                desc = tr("Kelola produk & stok, riwayat nota lengkap, serta ekspor file cadangan JSON.", "Manage items & stock, complete invoice history, and JSON backup export.")
            )
            OnboardingFeatureMiniCard(
                icon = Icons.Default.Language,
                title = tr("22 Bahasa & Palet Warna Material 3", "22 Languages & Material 3 Palettes"),
                desc = tr("Pilihan bahasa daerah & dunia, serta mode gelap/terang yang nyaman.", "Regional & world languages, plus light/dark theme choices.")
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 1: TUTORIAL TRANSAKSI KASIR
// -------------------------------------------------------------
@Composable
private fun CashierTutorialStepContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PointOfSale,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column {
                Text(
                    text = tr("Tutorial 1: Transaksi Kasir", "Tutorial 1: Cashier Checkout"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tr("Cara cepat melayani pesanan pembeli:", "Quick steps to process customer orders:"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        TutorialStepItemCard(
            stepNumber = "1",
            icon = Icons.Default.QrCodeScanner,
            title = tr("Pilih Produk atau Scan Barcode", "Select Products or Scan Barcode"),
            description = tr(
                "Ketuk produk pada katalog, cari nama produk, gunakan filter kategori, atau buka kamera barcode untuk memindai kode barang secara instan.",
                "Tap products from catalog, search by name, filter by category, or open camera scanner to scan item barcodes instantly."
            )
        )

        TutorialStepItemCard(
            stepNumber = "2",
            icon = Icons.Default.Discount,
            title = tr("Kustomisasi Harga, Diskon & Item Manual", "Customize Price, Discount & Manual Item"),
            description = tr(
                "Ketuk item di keranjang belanja untuk menambah kuantitas, ubah harga khusus (fitur Rubah Harga), atau gunakan tombol 'Item Manual' untuk barang non-katalog.",
                "Tap cart items to adjust quantity, apply custom unit price / discounts, or use 'Manual Item' for unlisted goods."
            )
        )

        TutorialStepItemCard(
            stepNumber = "3",
            icon = Icons.Default.Receipt,
            title = tr("Bayar & Hitung Kembalian Otomatis", "Checkout & Automatic Change Calculation"),
            description = tr(
                "Klik tombol 'Bayar / Checkout', pilih metode pembayaran (Tunai, QRIS, Transfer, Debit), masukkan uang diterima, lalu konfirmasi pembayaran.",
                "Click 'Pay / Checkout', select payment method (Cash, QRIS, Transfer, Card), input cash received, and confirm payment."
            )
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = tr(
                        "Tips: Struk transaksi langsung tersimpan otomatis di Riwayat dan siap dicetak!",
                        "Tip: Completed receipts are saved in History and ready for printing!"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 2: TUTORIAL CETAK STRUK & PRINTER
// -------------------------------------------------------------
@Composable
private fun PrinterTutorialStepContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column {
                Text(
                    text = tr("Tutorial 2: Cetak Struk & Printer", "Tutorial 2: Receipts & Thermal Printer"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tr("Menghubungkan printer Bluetooth & cetak nota:", "Connecting Bluetooth printer & printing receipts:"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        TutorialStepItemCard(
            stepNumber = "1",
            icon = Icons.Default.Print,
            title = tr("Pindai & Sambungkan Printer POS", "Scan & Connect Bluetooth POS Printer"),
            description = tr(
                "Buka menu 'Printer', aktifkan pemindaian, lalu pilih printer thermal Anda. Aplikasi mendukung printer thermal ukuran kertas 58mm dan 80mm.",
                "Open 'Printer' tab, start scanning, and connect your thermal POS printer. Supports both 58mm and 80mm paper widths."
            )
        )

        TutorialStepItemCard(
            stepNumber = "2",
            icon = Icons.Default.Visibility,
            title = tr("Pratinjau Struk Real-Time (Live Preview)", "Real-Time Digital Receipt Preview"),
            description = tr(
                "Ketuk tombol 'Nota' di bilah atas aplikasi untuk melihat kertas struk digital, logo toko, rincian produk, dan command ESC/POS sebelum dicetak ke kertas asli.",
                "Tap 'Receipt' button on top bar to see visual receipt layout, store logo, item details, and ESC/POS preview before real printing."
            )
        )

        TutorialStepItemCard(
            stepNumber = "3",
            icon = Icons.Default.CheckCircle,
            title = tr("Auto-Print Struk Sekali Klik", "Auto-Print Struk Automatically"),
            description = tr(
                "Aktifkan opsi 'Cetak Otomatis Setelah Bayar' di menu Pengaturan. Setiap selesai transaksi, struk langsung dicetak tanpa perlu tombol tambahan!",
                "Enable 'Auto Print on Checkout' in Settings. Whenever a checkout is completed, the receipt is automatically dispatched to your printer!"
            )
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = tr(
                        "Anda juga bisa mengganti Logo Nota, Nama Toko, Alamat, dan Catatan Kaki di menu Pengaturan.",
                        "You can also customize Receipt Logo, Store Name, Address, and Footer notes in Settings."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 3: PENGENALAN SEMUA FITUR APLIKASI
// -------------------------------------------------------------
@Composable
private fun FeaturesTourStepContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column {
                Text(
                    text = tr("Pengenalan Semua Fitur Aplikasi", "Overview of All App Features"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tr("8 Fitur unggulan AKIM ePOS Cashier:", "8 powerful features built for your store:"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val allFeatures = listOf(
            FeatureItem(
                icon = Icons.Default.PointOfSale,
                title = tr("1. Menu Kasir Cepat", "1. Fast Cashier POS"),
                desc = tr("Keranjang belanja, barcode scanner kamera, custom harga/diskon, dan multi-metode bayar.", "Shopping cart, camera barcode scan, custom price/discount, and multi-payment.")
            ),
            FeatureItem(
                icon = Icons.Default.Inventory,
                title = tr("2. Katalog Produk & Stok", "2. Product Catalog & Stock"),
                desc = tr("Tambah & edit produk, modal vs jual, pemantauan stok menipis, dan kode SKU.", "Add & edit items, cost vs price, low stock alert, and SKU barcodes.")
            ),
            FeatureItem(
                icon = Icons.Default.ReceiptLong,
                title = tr("3. Riwayat Transaksi & Nota", "3. Transaction History & Receipts"),
                desc = tr("Laporan omset harian, filter pembayaran (Tunai/QRIS/Debit), cetak ulang, & ekspor CSV.", "Daily sales summary, payment filters (Cash/QRIS/Card), reprint, & CSV export.")
            ),
            FeatureItem(
                icon = Icons.Default.Print,
                title = tr("4. Printer & Scanner Bluetooth", "4. Bluetooth Printer & Scanner"),
                desc = tr("Koneksi instan ke printer thermal POS, dual scan, tes cetak struk, & indikator online.", "Instant connection to thermal POS printers, dual scan, test print, & online status.")
            ),
            FeatureItem(
                icon = Icons.Default.Visibility,
                title = tr("5. Pratinjau Struk Real-Time", "5. Real-Time Receipt Preview"),
                desc = tr("Simulasi visual kertas struk fisik dan command matrix ESC/POS sebelum dicetak.", "Visual simulation of physical paper receipt and ESC/POS matrix before printing.")
            ),
            FeatureItem(
                icon = Icons.Default.Store,
                title = tr("6. Kustomisasi Profil Toko", "6. Store Profile Customization"),
                desc = tr("Nama usaha, alamat, kontak kasir, pilihan logo thermal preset, & catatan nota.", "Business name, address, cashier contact, thermal preset logo, & footer notes.")
            ),
            FeatureItem(
                icon = Icons.Default.Language,
                title = tr("7. Dukungan 22 Bahasa & Tema", "7. 22 Languages & Material Themes"),
                desc = tr("Bahasa daerah & internasional, 11 palet Material 3, dan mode gelap/terang.", "Regional & international languages, 11 Material 3 palettes, and dark/light modes.")
            ),
            FeatureItem(
                icon = Icons.Default.Backup,
                title = tr("8. Backup & Restore Database", "8. Database Backup & Restore"),
                desc = tr("Cadangkan seluruh data produk & transaksi ke file JSON aman di perangkat Anda.", "Backup all products & transactions to a safe JSON file on your device.")
            )
        )

        allFeatures.forEach { feature ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = feature.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = feature.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = feature.desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 4: READY TO SELL CONTENT
// -------------------------------------------------------------
@Composable
private fun ReadyToSellStepContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.RocketLaunch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Text(
            text = tr("Anda Siap Memulai! 🎉", "You're All Set! 🎉"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = tr(
                "Semua fitur kasir pintar, manajemen produk, dan cetak struk thermal telah siap digunakan untuk melayani pelanggan Anda.",
                "All smart cashier, product management, and thermal receipt printing features are ready to serve your customers."
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReadyChecklistItem(
                    text = tr("Contoh katalog produk sudah tersedia untuk dicoba langsung.", "Sample products are ready to test in Cashier.")
                )
                ReadyChecklistItem(
                    text = tr("Profil toko & logo nota dapat diubah kapan saja di menu Pengaturan.", "Store profile & receipt logo can be updated in Settings.")
                )
                ReadyChecklistItem(
                    text = tr("Tutorial ini dapat dibuka kembali melalui Pengaturan > Panduan Aplikasi.", "This tutorial can be revisited anytime via Settings > App Guide.")
                )
            }
        }
    }
}

// Helper components
@Composable
private fun OnboardingFeatureMiniCard(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TutorialStepItemCard(
    stepNumber: String,
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stepNumber,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReadyChecklistItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private data class FeatureItem(
    val icon: ImageVector,
    val title: String,
    val desc: String
)
