package com.example.ui.screens.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BestSellerItem
import com.example.data.model.PaymentMethodShare
import com.example.data.model.ReportPeriod
import com.example.data.model.SalesAnalyticsReport
import com.example.data.model.SalesTrendPoint
import com.example.data.model.StoreProfile
import com.example.data.model.formatRupiah
import com.example.printer.PrinterConnectionState
import com.example.ui.localization.tr
import com.example.ui.localization.trDynamic
import com.example.ui.theme.AccentEmerald

@Composable
fun SalesReportView(
    report: SalesAnalyticsReport,
    selectedPeriod: ReportPeriod,
    selectedOffset: Int,
    storeProfile: StoreProfile,
    printerState: PrinterConnectionState,
    onPeriodSelected: (ReportPeriod) -> Unit,
    onNavigateOffset: (Int) -> Unit,
    onResetOffset: () -> Unit,
    onPrintReport: () -> Unit,
    onShareReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPrinterConnected = printerState is PrinterConnectionState.Connected

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 680.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Period Selector & Date Navigator
            PeriodNavigatorCard(
                selectedPeriod = selectedPeriod,
                selectedOffset = selectedOffset,
                periodLabel = report.periodLabel,
                onPeriodSelected = onPeriodSelected,
                onNavigateOffset = onNavigateOffset,
                onResetOffset = onResetOffset
            )

            // 2. Action Header: Print & Share
            ReportActionToolbar(
                report = report,
                isPrinterConnected = isPrinterConnected,
                onPrintReport = onPrintReport,
                onShareReport = onShareReport
            )

            // 3. Top KPI Cards
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = tr("Total Omset", "Total Revenue"),
                        value = formatRupiah(report.totalRevenue, storeProfile.currencySymbol),
                        icon = Icons.Default.AttachMoney,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_total_revenue"
                    )
                    KpiCard(
                        title = tr("Total Transaksi", "Total Transactions"),
                        value = "${report.totalTransactions} " + tr("Nota", "Receipts"),
                        icon = Icons.Default.ReceiptLong,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_total_transactions"
                    )
                    KpiCard(
                        title = tr("Rata-rata Order", "Average Order"),
                        value = formatRupiah(report.averageOrderValue, storeProfile.currencySymbol),
                        icon = Icons.Default.Analytics,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_avg_order"
                    )
                    KpiCard(
                        title = tr("Item Terjual", "Units Sold"),
                        value = "${report.totalItemsSold} " + tr("Unit", "Units"),
                        icon = Icons.Default.LocalMall,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_items_sold"
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(
                            title = tr("Total Omset", "Total Revenue"),
                            value = formatRupiah(report.totalRevenue, storeProfile.currencySymbol),
                            icon = Icons.Default.AttachMoney,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                            testTag = "kpi_total_revenue"
                        )
                        KpiCard(
                            title = tr("Total Transaksi", "Total Transactions"),
                            value = "${report.totalTransactions} " + tr("Nota", "Receipts"),
                            icon = Icons.Default.ReceiptLong,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                            testTag = "kpi_total_transactions"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(
                            title = tr("Rata-rata Order", "Average Order"),
                            value = formatRupiah(report.averageOrderValue, storeProfile.currencySymbol),
                            icon = Icons.Default.Analytics,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f),
                            testTag = "kpi_avg_order"
                        )
                        KpiCard(
                            title = tr("Item Terjual", "Units Sold"),
                            value = "${report.totalItemsSold} " + tr("Unit", "Units"),
                            icon = Icons.Default.LocalMall,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            testTag = "kpi_items_sold"
                        )
                    }
                }
            }

            // 4. Sales Trend Bar Chart
            SalesTrendChartCard(
                trendPoints = report.trendPoints,
                currencySymbol = storeProfile.currencySymbol,
                period = selectedPeriod
            )

            // 5. Best Seller Products & Payment Distribution in Wide or Vertical mode
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BestSellersCard(
                        bestSellers = report.bestSellers,
                        currencySymbol = storeProfile.currencySymbol,
                        modifier = Modifier.weight(1.3f)
                    )
                    PaymentMethodDistributionCard(
                        paymentMethods = report.paymentMethods,
                        currencySymbol = storeProfile.currencySymbol,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                BestSellersCard(
                    bestSellers = report.bestSellers,
                    currencySymbol = storeProfile.currencySymbol
                )
                PaymentMethodDistributionCard(
                    paymentMethods = report.paymentMethods,
                    currencySymbol = storeProfile.currencySymbol
                )
            }
        }
    }
}

@Composable
fun PeriodNavigatorCard(
    selectedPeriod: ReportPeriod,
    selectedOffset: Int,
    periodLabel: String,
    onPeriodSelected: (ReportPeriod) -> Unit,
    onNavigateOffset: (Int) -> Unit,
    onResetOffset: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_period_navigator_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Period Tabs (Harian, Mingguan, Bulanan, Tahunan)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportPeriod.entries.forEach { period ->
                    val isSelected = period == selectedPeriod
                    FilterChip(
                        selected = isSelected,
                        onClick = { onPeriodSelected(period) },
                        label = {
                            Text(
                                text = tr(period.titleId, period.titleEn),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("report_chip_${period.name.lowercase()}")
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Navigation Arrows & Date Range Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { onNavigateOffset(-1) },
                    modifier = Modifier.testTag("report_nav_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr("Periode Sebelumnya", "Previous Period"),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = periodLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (selectedOffset != 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable { onResetOffset() }
                        ) {
                            Text(
                                text = tr("Kembali ke Saat Ini ↺", "Reset to Current ↺"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { onNavigateOffset(1) },
                    enabled = selectedOffset < 0,
                    modifier = Modifier.testTag("report_nav_next_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = tr("Periode Berikutnya", "Next Period"),
                        tint = if (selectedOffset < 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
fun ReportActionToolbar(
    report: SalesAnalyticsReport,
    isPrinterConnected: Boolean,
    onPrintReport: () -> Unit,
    onShareReport: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onPrintReport,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPrinterConnected) AccentEmerald else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .weight(1.2f)
                .height(48.dp)
                .testTag("report_print_button")
        ) {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = tr("Cetak Laporan Thermal", "Print Thermal Report"),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }

        OutlinedButton(
            onClick = onShareReport,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("report_share_button")
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = tr("Bagikan Ringkasan", "Share Summary"),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier.testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor
            )
        }
    }
}

@Composable
fun SalesTrendChartCard(
    trendPoints: List<SalesTrendPoint>,
    currencySymbol: String,
    period: ReportPeriod
) {
    val maxRevenue = remember(trendPoints) {
        trendPoints.maxOfOrNull { it.revenue }?.takeIf { it > 0 } ?: 1.0
    }
    var selectedPoint by remember { mutableStateOf<SalesTrendPoint?>(null) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sales_trend_chart_card")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = tr("Tren Grafik Penjualan", "Sales Trend Chart"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (selectedPoint != null) {
                    Text(
                        text = "${selectedPoint!!.fullLabel}: ${formatRupiah(selectedPoint!!.revenue, currencySymbol)} (${selectedPoint!!.transactionCount}x)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (trendPoints.isEmpty() || trendPoints.all { it.revenue == 0.0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tr("Belum ada omset penjualan pada periode ini", "No sales recorded in this period yet"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                // Interactive Bar Chart
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 10.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    trendPoints.forEach { point ->
                        val isSelected = selectedPoint == point
                        val heightFraction = (point.revenue / maxRevenue).toFloat().coerceIn(0.04f, 1f)
                        val isPeak = point.revenue == maxRevenue && maxRevenue > 0

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedPoint = if (isSelected) null else point }
                                .padding(horizontal = 2.dp)
                        ) {
                            // Value top label if peak or selected
                            if (isSelected || isPeak) {
                                Text(
                                    text = formatRupiah(point.revenue, currencySymbol),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPeak) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }

                            // The Visual Bar
                            Box(
                                modifier = Modifier
                                    .width(if (trendPoints.size > 8) 28.dp else 40.dp)
                                    .fillMaxHeight(heightFraction)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isPeak -> AccentEmerald
                                            point.revenue > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        }
                                    )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Bottom Bucket Label
                            Text(
                                text = point.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected || isPeak) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected || isPeak) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BestSellersCard(
    bestSellers: List<BestSellerItem>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.testTag("best_sellers_card")
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
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFEAB308), // Gold
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = tr("Produk Best Seller (Terlaris)", "Best-Selling Products"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr("Peringkat produk berdasarkan unit terjual & omset", "Ranked by units sold & total revenue"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (bestSellers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tr("Belum ada data barang terjual pada periode ini.", "No products sold in this period yet."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    bestSellers.take(10).forEachIndexed { index, item ->
                        BestSellerRowItem(
                            rank = index + 1,
                            item = item,
                            currencySymbol = currencySymbol
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BestSellerRowItem(
    rank: Int,
    item: BestSellerItem,
    currencySymbol: String
) {
    val rankBadgeBg = when (rank) {
        1 -> Color(0xFFFEF08A) // Gold
        2 -> Color(0xFFE2E8F0) // Silver
        3 -> Color(0xFFFFEDD5) // Bronze
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val rankBadgeTextColor = when (rank) {
        1 -> Color(0xFF854D0E)
        2 -> Color(0xFF334155)
        3 -> Color(0xFF9A3412)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Rank Badge (1, 2, 3...)
            Surface(
                shape = CircleShape,
                color = rankBadgeBg,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (rank == 1) "🥇" else if (rank == 2) "🥈" else if (rank == 3) "🥉" else "$rank",
                        fontWeight = FontWeight.ExtraBold,
                        color = rankBadgeTextColor,
                        fontSize = if (rank <= 3) 14.sp else 12.sp
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${item.totalQuantitySold} " + tr("terjual", "sold"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = trDynamic(item.category),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = formatRupiah(item.totalRevenue, currencySymbol),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Bar (% of total sales)
                LinearProgressIndicator(
                    progress = { (item.percentageOfTotalSales / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun PaymentMethodDistributionCard(
    paymentMethods: List<PaymentMethodShare>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.testTag("payment_distribution_card")
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
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = tr("Distribusi Metode Pembayaran", "Payment Method Breakdown"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr("Pembagian transaksi Tunai, QRIS, & Transfer", "Cash, QRIS, & Transfer transaction shares"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (paymentMethods.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tr("Belum ada data pembayaran.", "No payment data recorded yet."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    paymentMethods.forEach { pm ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = trDynamic(pm.method),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "(${pm.count}x)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "${formatRupiah(pm.totalAmount, currencySymbol)} (${String.format(java.util.Locale.US, "%.0f", pm.percentage)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            LinearProgressIndicator(
                                progress = { (pm.percentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}
