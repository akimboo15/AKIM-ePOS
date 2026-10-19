package com.example.data.repository

import com.example.data.model.BestSellerItem
import com.example.data.model.PaymentMethodShare
import com.example.data.model.Product
import com.example.data.model.ReportPeriod
import com.example.data.model.SalesAnalyticsReport
import com.example.data.model.SalesTrendPoint
import com.example.data.model.StoreProfile
import com.example.data.model.TransactionWithItems
import com.example.data.model.formatRupiah
import com.example.printer.EscPosPrinter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object SalesReportCalculator {

    fun calculateReport(
        transactionsWithItems: List<TransactionWithItems>,
        period: ReportPeriod,
        dateOffset: Int,
        products: List<Product>
    ): SalesAnalyticsReport {
        val (startTime, endTime, periodLabel, trendBuckets) = computePeriodBoundariesAndBuckets(period, dateOffset)

        val filteredTransactions = transactionsWithItems.filter { txWithItems ->
            val tx = txWithItems.transaction
            tx.timestamp in startTime..endTime && tx.status == "SELESAI"
        }

        val totalRevenue = filteredTransactions.sumOf { it.transaction.grandTotal }
        val totalTransactions = filteredTransactions.size
        val averageOrderValue = if (totalTransactions > 0) totalRevenue / totalTransactions else 0.0

        val allItems = filteredTransactions.flatMap { it.items }
        val totalItemsSold = allItems.sumOf { it.quantity }

        val productCategoryMap = products.associate { it.id to it.category }

        // 1. Best Sellers Ranking
        val bestSellers = allItems.groupBy { it.productId to it.productName }
            .map { (key, items) ->
                val (productId, productName) = key
                val totalQty = items.sumOf { it.quantity }
                val totalRev = items.sumOf { it.subtotal }
                val category = productCategoryMap[productId] ?: "Umum"
                val percentage = if (totalRevenue > 0) {
                    ((totalRev / totalRevenue) * 100).toFloat()
                } else 0f
                BestSellerItem(
                    productId = productId,
                    productName = productName,
                    category = category,
                    totalQuantitySold = totalQty,
                    totalRevenue = totalRev,
                    percentageOfTotalSales = percentage
                )
            }
            .sortedByDescending { it.totalQuantitySold }

        // 2. Trend Points Calculation
        val populatedTrendPoints = trendBuckets.map { bucket ->
            val bucketTransactions = filteredTransactions.filter { it.transaction.timestamp in bucket.startTime..bucket.endTime }
            val rev = bucketTransactions.sumOf { it.transaction.grandTotal }
            val count = bucketTransactions.size
            SalesTrendPoint(
                label = bucket.shortLabel,
                fullLabel = bucket.fullLabel,
                revenue = rev,
                transactionCount = count
            )
        }

        // 3. Payment Method Distribution
        val paymentGroups = filteredTransactions.groupBy { it.transaction.paymentMethod }
        val paymentMethods = paymentGroups.map { (method, txList) ->
            val count = txList.size
            val amount = txList.sumOf { it.transaction.grandTotal }
            val pct = if (totalRevenue > 0) ((amount / totalRevenue) * 100).toFloat() else 0f
            PaymentMethodShare(
                method = method,
                count = count,
                totalAmount = amount,
                percentage = pct
            )
        }.sortedByDescending { it.totalAmount }

        return SalesAnalyticsReport(
            period = period,
            periodLabel = periodLabel,
            dateOffset = dateOffset,
            totalRevenue = totalRevenue,
            totalTransactions = totalTransactions,
            averageOrderValue = averageOrderValue,
            totalItemsSold = totalItemsSold,
            bestSellers = bestSellers,
            trendPoints = populatedTrendPoints,
            paymentMethods = paymentMethods,
            transactions = filteredTransactions.sortedByDescending { it.transaction.timestamp }
        )
    }

    private data class TrendBucket(
        val shortLabel: String,
        val fullLabel: String,
        val startTime: Long,
        val endTime: Long
    )

    private data class BoundariesResult(
        val startTime: Long,
        val endTime: Long,
        val periodLabel: String,
        val buckets: List<TrendBucket>
    )

    private fun computePeriodBoundariesAndBuckets(period: ReportPeriod, offset: Int): BoundariesResult {
        val localeId = Locale.forLanguageTag("id-ID")
        val now = Calendar.getInstance()

        return when (period) {
            ReportPeriod.DAILY -> {
                val cal = (now.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, offset)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis

                val endCal = (cal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val end = endCal.timeInMillis

                val dayNameFormat = SimpleDateFormat("EEEE, dd MMM yyyy", localeId)
                val label = when (offset) {
                    0 -> "Hari Ini (${dayNameFormat.format(Date(start))})"
                    -1 -> "Kemarin (${dayNameFormat.format(Date(start))})"
                    else -> dayNameFormat.format(Date(start))
                }

                // 2-hour interval trend buckets (12 slots)
                val buckets = mutableListOf<TrendBucket>()
                val hourSlots = listOf(
                    0 to 2, 2 to 4, 4 to 6, 6 to 8, 8 to 10, 10 to 12,
                    12 to 14, 14 to 16, 16 to 18, 18 to 20, 20 to 22, 22 to 24
                )
                for ((hStart, hEnd) in hourSlots) {
                    val bStartCal = (cal.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, hStart) }
                    val bEndCal = (cal.clone() as Calendar).apply {
                        if (hEnd == 24) {
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        } else {
                            set(Calendar.HOUR_OF_DAY, hEnd)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                    }
                    buckets.add(
                        TrendBucket(
                            shortLabel = String.format(Locale.US, "%02d:00", hStart),
                            fullLabel = String.format(Locale.US, "%02d:00 - %02d:00", hStart, hEnd),
                            startTime = bStartCal.timeInMillis,
                            endTime = bEndCal.timeInMillis
                        )
                    )
                }
                BoundariesResult(start, end, label, buckets)
            }

            ReportPeriod.WEEKLY -> {
                val cal = (now.clone() as Calendar).apply {
                    add(Calendar.WEEK_OF_YEAR, offset)
                    // Move to Monday of this week
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis

                val endCal = (cal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 6)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val end = endCal.timeInMillis

                val dFmt = SimpleDateFormat("dd MMM", localeId)
                val fullFmt = SimpleDateFormat("dd MMM yyyy", localeId)
                val label = when (offset) {
                    0 -> "Minggu Ini (${dFmt.format(Date(start))} - ${fullFmt.format(Date(end))})"
                    -1 -> "Minggu Lalu (${dFmt.format(Date(start))} - ${fullFmt.format(Date(end))})"
                    else -> "${dFmt.format(Date(start))} - ${fullFmt.format(Date(end))}"
                }

                // 7 days of the week (Sen to Min)
                val buckets = mutableListOf<TrendBucket>()
                val dayShortNames = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                for (i in 0 until 7) {
                    val dayStart = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
                    val dayEnd = (dayStart.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    val fullDayFmt = SimpleDateFormat("EEEE, dd MMM", localeId)
                    buckets.add(
                        TrendBucket(
                            shortLabel = dayShortNames[i],
                            fullLabel = fullDayFmt.format(Date(dayStart.timeInMillis)),
                            startTime = dayStart.timeInMillis,
                            endTime = dayEnd.timeInMillis
                        )
                    )
                }
                BoundariesResult(start, end, label, buckets)
            }

            ReportPeriod.MONTHLY -> {
                val cal = (now.clone() as Calendar).apply {
                    add(Calendar.MONTH, offset)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis

                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val endCal = (cal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, maxDay)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val end = endCal.timeInMillis

                val monthYearFmt = SimpleDateFormat("MMMM yyyy", localeId)
                val label = when (offset) {
                    0 -> "Bulan Ini (${monthYearFmt.format(Date(start))})"
                    -1 -> "Bulan Lalu (${monthYearFmt.format(Date(start))})"
                    else -> monthYearFmt.format(Date(start))
                }

                // Split into 4 weekly blocks: Day 1-7, 8-14, 15-21, 22-end
                val buckets = mutableListOf<TrendBucket>()
                val intervals = listOf(
                    Triple("W1", 1, 7),
                    Triple("W2", 8, 14),
                    Triple("W3", 15, 21),
                    Triple("W4", 22, maxDay)
                )
                for ((tag, startD, endD) in intervals) {
                    val bStart = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, startD) }
                    val bEnd = (cal.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, endD)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    buckets.add(
                        TrendBucket(
                            shortLabel = tag,
                            fullLabel = "Tgl $startD - $endD ${monthYearFmt.format(Date(start))}",
                            startTime = bStart.timeInMillis,
                            endTime = bEnd.timeInMillis
                        )
                    )
                }
                BoundariesResult(start, end, label, buckets)
            }

            ReportPeriod.YEARLY -> {
                val cal = (now.clone() as Calendar).apply {
                    add(Calendar.YEAR, offset)
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis

                val maxDaysYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
                val endCal = (cal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_YEAR, maxDaysYear)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val end = endCal.timeInMillis

                val year = cal.get(Calendar.YEAR)
                val label = when (offset) {
                    0 -> "Tahun Ini ($year)"
                    -1 -> "Tahun Lalu ($year)"
                    else -> "Tahun $year"
                }

                // 12 Months
                val buckets = mutableListOf<TrendBucket>()
                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
                for (m in 0 until 12) {
                    val mStart = (cal.clone() as Calendar).apply {
                        set(Calendar.MONTH, m)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    val mMaxDay = mStart.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val mEnd = (mStart.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, mMaxDay)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    buckets.add(
                        TrendBucket(
                            shortLabel = monthNames[m],
                            fullLabel = "${monthNames[m]} $year",
                            startTime = mStart.timeInMillis,
                            endTime = mEnd.timeInMillis
                        )
                    )
                }
                BoundariesResult(start, end, label, buckets)
            }
        }
    }

    /**
     * Generates a thermal-printable plain text receipt summary for the selected report period.
     */
    fun buildThermalSalesReportText(report: SalesAnalyticsReport, store: StoreProfile): String {
        val width = if (store.paperWidth == 80) 48 else 32
        val sb = StringBuilder()
        val divChar = store.dividerChar

        fun center(text: String): String {
            if (text.length >= width) return text.take(width)
            val pad = (width - text.length) / 2
            return " ".repeat(pad) + text
        }

        fun twoCols(left: String, right: String): String {
            val space = width - left.length - right.length
            return if (space > 0) left + " ".repeat(space) + right else "$left $right".take(width)
        }

        sb.appendLine(divChar.toString().repeat(width))
        val periodHeader = when (report.period) {
            ReportPeriod.DAILY -> "LAPORAN PENJUALAN HARIAN"
            ReportPeriod.WEEKLY -> "LAPORAN PENJUALAN MINGGUAN"
            ReportPeriod.MONTHLY -> "LAPORAN PENJUALAN BULANAN"
            ReportPeriod.YEARLY -> "LAPORAN PENJUALAN TAHUNAN"
        }
        sb.appendLine(center(periodHeader))
        sb.appendLine(center(store.name.uppercase(Locale.getDefault())))
        sb.appendLine(divChar.toString().repeat(width))

        sb.appendLine("Periode : ${report.periodLabel}")
        sb.appendLine("Waktu   : ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
        sb.appendLine("-".repeat(width))

        sb.appendLine(twoCols("Total Omset", formatRupiah(report.totalRevenue, store.currencySymbol)))
        sb.appendLine(twoCols("Transaksi", "${report.totalTransactions} Transaksi"))
        sb.appendLine(twoCols("Rata-rata/Tx", formatRupiah(report.averageOrderValue, store.currencySymbol)))
        sb.appendLine(twoCols("Total Item Terjual", "${report.totalItemsSold} Unit"))
        sb.appendLine("-".repeat(width))

        // Best Sellers
        if (report.bestSellers.isNotEmpty()) {
            sb.appendLine("PRODUK BEST SELLER:")
            val topItems = report.bestSellers.take(5)
            topItems.forEachIndexed { idx, item ->
                val rank = idx + 1
                sb.appendLine("$rank. ${item.productName}")
                val revStr = formatRupiah(item.totalRevenue, store.currencySymbol)
                sb.appendLine(twoCols("   ${item.totalQuantitySold}x terjual", "$revStr (${String.format(Locale.US, "%.1f", item.percentageOfTotalSales)}%)"))
            }
            sb.appendLine("-".repeat(width))
        }

        // Payment Methods
        if (report.paymentMethods.isNotEmpty()) {
            sb.appendLine("METODE PEMBAYARAN:")
            report.paymentMethods.forEach { pm ->
                val amtStr = formatRupiah(pm.totalAmount, store.currencySymbol)
                sb.appendLine(twoCols("${pm.method} (${pm.count}x)", "$amtStr (${String.format(Locale.US, "%.0f", pm.percentage)}%)"))
            }
            sb.appendLine(divChar.toString().repeat(width))
        }

        sb.appendLine(center("Dicetak oleh: ${store.defaultCashier}"))
        sb.appendLine(center("AKIM ePOS Cashier System"))
        sb.appendLine("\n\n\n")

        return sb.toString()
    }

    /**
     * Builds ESC/POS thermal printer bytes for the report summary.
     */
    fun buildThermalSalesReportBytes(report: SalesAnalyticsReport, store: StoreProfile): ByteArray {
        val text = buildThermalSalesReportText(report, store)
        val baos = java.io.ByteArrayOutputStream()
        baos.write(EscPosPrinter.CMD_INIT)
        baos.write(text.toByteArray(Charsets.ISO_8859_1))
        if (store.autoCut) {
            baos.write(EscPosPrinter.CMD_CUT_PAPER)
        }
        return baos.toByteArray()
    }
}
