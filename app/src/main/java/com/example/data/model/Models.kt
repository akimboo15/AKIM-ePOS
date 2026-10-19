package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["name"]),
        Index(value = ["sku"]),
        Index(value = ["category"])
    ]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sku: String = "",
    val category: String = "Umum",
    val costPrice: Double = 0.0,
    val sellingPrice: Double,
    val stock: Int = 100,
    val unit: String = "pcs",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val customPrice: Double? = null,
    val discount: Double = 0.0,
    val note: String = ""
) {
    val unitPrice: Double
        get() = customPrice ?: product.sellingPrice

    val effectiveUnitPrice: Double
        get() = (unitPrice - discount).coerceAtLeast(0.0)

    val subtotal: Double
        get() = effectiveUnitPrice * quantity

    val isPriceOverridden: Boolean
        get() = customPrice != null && customPrice != product.sellingPrice
}

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["invoiceNumber"]), Index(value = ["timestamp"])]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val customerName: String = "Pelanggan Umum",
    val cashierName: String = "Kasir Toko",
    val paymentMethod: String = "Tunai",
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double,
    val paidAmount: Double,
    val changeAmount: Double,
    val status: String = "SELESAI", // SELESAI, BATAL
    val notes: String = ""
) {
    val formattedDate: String
        get() = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("id-ID")).format(Date(timestamp))
}

@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["transactionId"])]
)
data class TransactionItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val productId: Long,
    val productName: String,
    val originalPrice: Double,
    val actualPrice: Double,
    val quantity: Int,
    val subtotal: Double,
    val note: String = ""
)

data class TransactionWithItems(
    @Embedded val transaction: Transaction,
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val items: List<TransactionItem>
)

@Entity(tableName = "store_profile")
data class StoreProfile(
    @PrimaryKey
    val id: Int = 1,
    val name: String = "TOKO AKIMBOOW",
    val slogan: String = "Sahabat Belanja Anda",
    val address: String = "Jl. Merdeka No. 45, Jakarta",
    val phone: String = "0812-3456-7890",
    val receiptFooter: String = "Terima Kasih Atas Kunjungan Anda\nBarang yang sudah dibeli tidak dapat ditukar",
    val paperWidth: Int = 58, // 58 or 80 mm
    val autoCut: Boolean = true,
    val feedLines: Int = 3,
    val defaultCashier: String = "Kasir 01",
    // Multi-Language, Theme & General POS Settings
    val languageCode: String = "id",
    val themeOption: String = "SAME_AS_DEVICE",
    val currencySymbol: String = "Rp",
    val defaultTaxPercent: Double = 0.0,
    val autoPrintOnCheckout: Boolean = false,
    // Receipt Logo Settings
    val showLogo: Boolean = true,
    val logoPresetType: String = "STORE_MART", // STORE_MART, CAFE_COFFEE, RESTAURANT, FASHION, ELECTRONICS, PHARMACY, CUSTOM_IMAGE
    val customLogoPath: String = "",
    val logoSizePx: Int = 184, // 120 (Kecil), 184 (Sedang), 256 (Besar)
    val logoAlignment: String = "CENTER", // LEFT, CENTER, RIGHT
    // Receipt Layout Editor Settings
    val headerAlignment: String = "CENTER", // LEFT, CENTER, RIGHT
    val titleFontSize: String = "DOUBLE", // NORMAL, LARGE, DOUBLE
    val dividerStyle: String = "DASH", // DASH (-), EQUAL (=), STAR (*), DOT (.)
    val customHeaderNote: String = "",
    val socialMediaInfo: String = "IG: @tokoberkahjaya",
    val showSlogan: Boolean = true,
    val showAddressPhone: Boolean = true,
    val showInvoiceNumber: Boolean = true,
    val showDateTime: Boolean = true,
    val showCashierName: Boolean = true,
    val showCustomerName: Boolean = true,
    val showItemNumber: Boolean = false,
    val showTaxAndDiscount: Boolean = true,
    val showSocialMedia: Boolean = true,
    val showBarcode: Boolean = true,
    val showSignatureBox: Boolean = false
) {
    val dividerChar: Char
        get() = when (dividerStyle) {
            "EQUAL" -> '='
            "STAR" -> '*'
            "DOT" -> '.'
            else -> '-'
        }
}

fun formatRupiah(amount: Double, currencySymbol: String = "Rp"): String {
    val format = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
    format.maximumFractionDigits = 0
    return "$currencySymbol ${format.format(amount)}"
}
