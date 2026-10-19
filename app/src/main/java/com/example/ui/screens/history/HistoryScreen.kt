package com.example.ui.screens.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Transaction
import com.example.data.model.formatRupiah
import com.example.printer.PrinterConnectionState
import com.example.ui.common.ReceiptPaperCard
import com.example.ui.common.ReceiptPreviewDialog
import com.example.ui.localization.AppLocalization
import com.example.ui.localization.tr
import com.example.ui.localization.trDynamic
import com.example.ui.theme.AccentEmerald
import com.example.ui.viewmodel.PosViewModel

@Composable
fun HistoryScreen(
    viewModel: PosViewModel
) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val salesReport by viewModel.salesAnalyticsReport.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedReportPeriod.collectAsStateWithLifecycle()
    val selectedOffset by viewModel.selectedReportOffset.collectAsStateWithLifecycle()
    val todaySummary by viewModel.todaySummary.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val storeProfile by viewModel.storeProfile.collectAsStateWithLifecycle()
    val printerState by viewModel.printerConnectionState.collectAsStateWithLifecycle()
    val strings = AppLocalization.getStrings(storeProfile.languageCode)

    val (todayTotalRevenue, todayTxCount, todayAvgOrder) = todaySummary

    var selectedHistoryTab by rememberSaveable { mutableIntStateOf(0) } // 0: Laporan & Analisis, 1: Riwayat Nota

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTabletOrWide = maxWidth >= 600.dp

        // Automatically select the newest transaction on tablet so the detail pane is populated
        LaunchedEffect(isTabletOrWide, transactions.firstOrNull()?.id) {
            if (isTabletOrWide && uiState.selectedHistoryTransaction == null && transactions.isNotEmpty()) {
                viewModel.selectHistoryTransaction(transactions.first().id)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Primary Navigation Tab: Laporan & Analisis vs Riwayat Nota
            TabRow(
                selectedTabIndex = selectedHistoryTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_screen_tab_row")
            ) {
                Tab(
                    selected = selectedHistoryTab == 0,
                    onClick = { selectedHistoryTab = 0 },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    text = { Text(tr("Laporan & Analisis", "Reports & Analytics"), fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_sales_report")
                )
                Tab(
                    selected = selectedHistoryTab == 1,
                    onClick = { selectedHistoryTab = 1 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    text = {
                        Text(
                            "${tr("Riwayat Nota", "Receipt History")} (${transactions.size})",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("tab_receipt_history")
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (selectedHistoryTab == 0) {
                    // TAB 0: Sales Reports & Analytics Dashboard
                    SalesReportView(
                        report = salesReport,
                        selectedPeriod = selectedPeriod,
                        selectedOffset = selectedOffset,
                        storeProfile = storeProfile,
                        printerState = printerState,
                        onPeriodSelected = { viewModel.setReportPeriod(it) },
                        onNavigateOffset = { viewModel.navigateReportPeriod(it) },
                        onResetOffset = { viewModel.resetReportPeriodOffset() },
                        onPrintReport = { viewModel.printThermalSalesReport(salesReport) },
                        onShareReport = { viewModel.shareSalesReportText(salesReport) }
                    )
                } else {
                    // TAB 1: Receipt History
                    if (isTabletOrWide) {
                        // Canonical List-Detail Split View for Tablets
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left Pane: Daily Sales Summary & Transaction List
                            Column(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .fillMaxHeight()
                            ) {
                                DailySalesBannerCard(
                                    todayTxCount = todayTxCount,
                                    todayTotalRevenue = todayTotalRevenue,
                                    todayAvgOrder = todayAvgOrder,
                                    currencySymbol = storeProfile.currencySymbol
                                )

                                Text(
                                    text = "${tr("Daftar Riwayat Nota", "Receipt History List")} (${transactions.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )

                                if (transactions.isEmpty()) {
                                    EmptyHistoryState(modifier = Modifier.weight(1f))
                                } else {
                                    LazyColumn(
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(transactions, key = { it.id }) { tx ->
                                            val isSelected = uiState.selectedHistoryTransaction?.transaction?.id == tx.id
                                            TransactionItemCard(
                                                transaction = tx,
                                                currencySymbol = storeProfile.currencySymbol,
                                                isSelected = isSelected,
                                                onClick = { viewModel.selectHistoryTransaction(tx.id) }
                                            )
                                        }
                                    }
                                }
                            }

                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                            // Right Detail Pane: Live Receipt Preview & Print/Share Actions
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .testTag("tablet_history_detail_pane"),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp
                            ) {
                                val selectedDetails = uiState.selectedHistoryTransaction
                                if (selectedDetails == null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.ReceiptLong,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(56.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = tr("Pilih Transaksi di Sebelah Kiri", "Select Transaction on the Left"),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = tr("Rincian nota struk digital dan tombol cetak ulang akan tampil di panel ini.", "Digital receipt details and reprint actions will appear in this panel."),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${tr("Pratinjau Nota", "Receipt Preview")} (${selectedDetails.transaction.invoiceNumber})",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    text = "${selectedDetails.transaction.formattedDate} • ${tr("Kertas", "Paper")} ${storeProfile.paperWidth}mm",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = { viewModel.openThermalPrintPreview(selectedDetails, isDraft = false) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("history_open_full_preview_button")
                                            ) {
                                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(tr("Layar Penuh", "Full Screen"))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider()
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .verticalScroll(rememberScrollState()),
                                            contentAlignment = Alignment.TopCenter
                                        ) {
                                            ReceiptPaperCard(
                                                transactionWithItems = selectedDetails,
                                                storeProfile = storeProfile,
                                                showThermalHardwareSimulation = true,
                                                modifier = Modifier.widthIn(max = 420.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider()
                                        Spacer(modifier = Modifier.height(12.dp))

                                        val isConnected = printerState is PrinterConnectionState.Connected
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { viewModel.shareReceipt(selectedDetails) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .testTag("share_receipt_button")
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = strings.shareButton)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(strings.shareButton, fontWeight = FontWeight.SemiBold)
                                            }

                                            Button(
                                                onClick = { viewModel.openThermalPrintPreview(selectedDetails, isDraft = false) },
                                                modifier = Modifier
                                                    .weight(1.3f)
                                                    .height(48.dp)
                                                    .testTag("print_receipt_button"),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isConnected) AccentEmerald else MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                Icon(Icons.Default.Print, contentDescription = strings.printButton)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("${tr("Pratinjau", "Preview")} & ${strings.printButton}", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Compact Smartphone Layout
                        Column(modifier = Modifier.fillMaxSize()) {
                            DailySalesBannerCard(
                                todayTxCount = todayTxCount,
                                todayTotalRevenue = todayTotalRevenue,
                                todayAvgOrder = todayAvgOrder,
                                currencySymbol = storeProfile.currencySymbol
                            )

                            Text(
                                text = "${tr("Daftar Riwayat Nota", "Receipt History List")} (${transactions.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )

                            if (transactions.isEmpty()) {
                                EmptyHistoryState(modifier = Modifier.weight(1f))
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(transactions, key = { it.id }) { tx ->
                                        TransactionItemCard(
                                            transaction = tx,
                                            currencySymbol = storeProfile.currencySymbol,
                                            isSelected = false,
                                            onClick = { viewModel.selectHistoryTransaction(tx.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // Detail Receipt & Reprint Dialog on Compact Smartphone
                        uiState.selectedHistoryTransaction?.let { txDetails ->
                            ReceiptPreviewDialog(
                                transactionWithItems = txDetails,
                                storeProfile = storeProfile,
                                printerState = printerState,
                                onPrint = {
                                    viewModel.dismissHistoryDetail()
                                    viewModel.openThermalPrintPreview(txDetails, isDraft = false)
                                },
                                onShare = { viewModel.shareReceipt(txDetails) },
                                onDismiss = { viewModel.dismissHistoryDetail() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailySalesBannerCard(
    todayTxCount: Int,
    todayTotalRevenue: Double,
    todayAvgOrder: Double,
    currencySymbol: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr("Ringkasan Penjualan Hari Ini", "Today's Sales Summary"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Surface(
                    color = AccentEmerald.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$todayTxCount ${tr("Transaksi", "Transactions")}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = tr("Total Omset Hari Ini", "Today's Total Revenue"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = formatRupiah(todayTotalRevenue, currencySymbol),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = tr("Rata-rata Order", "Average Order"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = formatRupiah(todayAvgOrder, currencySymbol),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tr("Belum ada transaksi tersimpan", "No transactions saved yet"),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = tr("Lakukan transaksi pada menu Kasir", "Make transactions in Cashier menu"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun TransactionItemCard(
    transaction: Transaction,
    currencySymbol: String = "Rp",
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("transaction_item_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = transaction.invoiceNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = trDynamic(transaction.paymentMethod),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${transaction.formattedDate} • ${transaction.customerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatRupiah(transaction.grandTotal, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = AccentEmerald
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = tr("Buka Detail", "Open Details"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
