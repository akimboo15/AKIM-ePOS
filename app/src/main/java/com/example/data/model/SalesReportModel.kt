package com.example.data.model

enum class ReportPeriod(val code: String, val titleId: String, val titleEn: String) {
    DAILY("DAILY", "Harian", "Daily"),
    WEEKLY("WEEKLY", "Mingguan", "Weekly"),
    MONTHLY("MONTHLY", "Bulanan", "Monthly"),
    YEARLY("YEARLY", "Tahunan", "Yearly")
}

data class BestSellerItem(
    val productId: Long,
    val productName: String,
    val category: String,
    val totalQuantitySold: Int,
    val totalRevenue: Double,
    val percentageOfTotalSales: Float // 0.0f - 100.0f
)

data class SalesTrendPoint(
    val label: String,          // e.g. "08:00", "Sen", "W1", "Jan"
    val fullLabel: String,      // e.g. "08:00 - 10:00", "Senin, 19 Okt", "Januari 2026"
    val revenue: Double,
    val transactionCount: Int
)

data class PaymentMethodShare(
    val method: String,
    val count: Int,
    val totalAmount: Double,
    val percentage: Float
)

data class SalesAnalyticsReport(
    val period: ReportPeriod,
    val periodLabel: String,
    val dateOffset: Int,
    val totalRevenue: Double,
    val totalTransactions: Int,
    val averageOrderValue: Double,
    val totalItemsSold: Int,
    val bestSellers: List<BestSellerItem>,
    val trendPoints: List<SalesTrendPoint>,
    val paymentMethods: List<PaymentMethodShare>,
    val transactions: List<TransactionWithItems>
)
