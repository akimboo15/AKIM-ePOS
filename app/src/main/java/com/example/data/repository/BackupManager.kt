package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.model.Product
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionWithItems
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DatabaseBackupPayload(
    val version: Int = 1,
    val appName: String = "AKIM ePOS Cashier",
    val createdAt: Long = System.currentTimeMillis(),
    val storeProfile: StoreProfile? = null,
    val products: List<Product> = emptyList(),
    val transactions: List<TransactionWithItems> = emptyList()
)

data class LocalBackupFileInfo(
    val fileName: String,
    val filePath: String,
    val createdAt: Long,
    val sizeBytes: Long,
    val productCount: Int,
    val transactionCount: Int,
    val storeName: String
) {
    val formattedDate: String
        get() = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("id-ID")).format(Date(createdAt))

    val formattedSize: String
        get() = if (sizeBytes < 1024) {
            "$sizeBytes B"
        } else {
            String.format(Locale.US, "%.1f KB", sizeBytes / 1024.0)
        }
}

data class ImportBackupSummary(
    val productsImported: Int,
    val transactionsImported: Int,
    val itemsImported: Int,
    val storeProfileRestored: Boolean
)

object BackupManager {

    private const val BACKUP_DIR_NAME = "backups"

    fun getBackupDirectory(context: Context): File {
        val dir = File(context.filesDir, BACKUP_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun generateDefaultBackupFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "akim_epos_backup_$timestamp.json"
    }

    fun serializeToJson(payload: DatabaseBackupPayload): String {
        val root = JSONObject()
        root.put("version", payload.version)
        root.put("appName", payload.appName)
        root.put("createdAt", payload.createdAt)

        // StoreProfile
        payload.storeProfile?.let { profile ->
            val sp = JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("slogan", profile.slogan)
                put("address", profile.address)
                put("phone", profile.phone)
                put("receiptFooter", profile.receiptFooter)
                put("paperWidth", profile.paperWidth)
                put("autoCut", profile.autoCut)
                put("feedLines", profile.feedLines)
                put("defaultCashier", profile.defaultCashier)
                put("languageCode", profile.languageCode)
                put("themeOption", profile.themeOption)
                put("currencySymbol", profile.currencySymbol)
                put("defaultTaxPercent", profile.defaultTaxPercent)
                put("autoPrintOnCheckout", profile.autoPrintOnCheckout)
                put("showLogo", profile.showLogo)
                put("logoPresetType", profile.logoPresetType)
                put("customLogoPath", profile.customLogoPath)
                put("logoSizePx", profile.logoSizePx)
                put("logoAlignment", profile.logoAlignment)
                put("headerAlignment", profile.headerAlignment)
                put("titleFontSize", profile.titleFontSize)
                put("dividerStyle", profile.dividerStyle)
                put("customHeaderNote", profile.customHeaderNote)
                put("socialMediaInfo", profile.socialMediaInfo)
                put("showSlogan", profile.showSlogan)
                put("showAddressPhone", profile.showAddressPhone)
                put("showInvoiceNumber", profile.showInvoiceNumber)
                put("showDateTime", profile.showDateTime)
                put("showCashierName", profile.showCashierName)
                put("showCustomerName", profile.showCustomerName)
                put("showItemNumber", profile.showItemNumber)
                put("showTaxAndDiscount", profile.showTaxAndDiscount)
                put("showSocialMedia", profile.showSocialMedia)
                put("showBarcode", profile.showBarcode)
                put("showSignatureBox", profile.showSignatureBox)
            }
            root.put("storeProfile", sp)
        }

        // Products
        val productsArray = JSONArray()
        for (p in payload.products) {
            val pObj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("sku", p.sku)
                put("category", p.category)
                put("costPrice", p.costPrice)
                put("sellingPrice", p.sellingPrice)
                put("stock", p.stock)
                put("unit", p.unit)
                put("createdAt", p.createdAt)
                put("updatedAt", p.updatedAt)
            }
            productsArray.put(pObj)
        }
        root.put("products", productsArray)

        // Transactions with Items
        val txArray = JSONArray()
        for (txWithItems in payload.transactions) {
            val tx = txWithItems.transaction
            val txObj = JSONObject().apply {
                put("id", tx.id)
                put("invoiceNumber", tx.invoiceNumber)
                put("timestamp", tx.timestamp)
                put("customerName", tx.customerName)
                put("cashierName", tx.cashierName)
                put("paymentMethod", tx.paymentMethod)
                put("subtotal", tx.subtotal)
                put("discountAmount", tx.discountAmount)
                put("taxAmount", tx.taxAmount)
                put("grandTotal", tx.grandTotal)
                put("paidAmount", tx.paidAmount)
                put("changeAmount", tx.changeAmount)
                put("status", tx.status)
                put("notes", tx.notes)

                val itemsArray = JSONArray()
                for (item in txWithItems.items) {
                    val itemObj = JSONObject().apply {
                        put("id", item.id)
                        put("transactionId", item.transactionId)
                        put("productId", item.productId)
                        put("productName", item.productName)
                        put("originalPrice", item.originalPrice)
                        put("actualPrice", item.actualPrice)
                        put("quantity", item.quantity)
                        put("subtotal", item.subtotal)
                        put("note", item.note)
                    }
                    itemsArray.put(itemObj)
                }
                put("items", itemsArray)
            }
            txArray.put(txObj)
        }
        root.put("transactions", txArray)

        return root.toString(2)
    }

    fun deserializeFromJson(jsonString: String): DatabaseBackupPayload {
        val root = JSONObject(jsonString.trim())
        val version = root.optInt("version", 1)
        val appName = root.optString("appName", "AKIM ePOS Cashier")
        val createdAt = root.optLong("createdAt", System.currentTimeMillis())

        val storeProfile = root.optJSONObject("storeProfile")?.let { sp ->
            val defaults = StoreProfile()
            StoreProfile(
                id = 1,
                name = sp.optString("name", defaults.name),
                slogan = sp.optString("slogan", defaults.slogan),
                address = sp.optString("address", defaults.address),
                phone = sp.optString("phone", defaults.phone),
                receiptFooter = sp.optString("receiptFooter", defaults.receiptFooter),
                paperWidth = sp.optInt("paperWidth", defaults.paperWidth),
                autoCut = sp.optBoolean("autoCut", defaults.autoCut),
                feedLines = sp.optInt("feedLines", defaults.feedLines),
                defaultCashier = sp.optString("defaultCashier", defaults.defaultCashier),
                languageCode = sp.optString("languageCode", defaults.languageCode),
                themeOption = sp.optString("themeOption", defaults.themeOption),
                currencySymbol = sp.optString("currencySymbol", defaults.currencySymbol),
                defaultTaxPercent = sp.optDouble("defaultTaxPercent", defaults.defaultTaxPercent),
                autoPrintOnCheckout = sp.optBoolean("autoPrintOnCheckout", defaults.autoPrintOnCheckout),
                showLogo = sp.optBoolean("showLogo", defaults.showLogo),
                logoPresetType = sp.optString("logoPresetType", defaults.logoPresetType),
                customLogoPath = sp.optString("customLogoPath", defaults.customLogoPath),
                logoSizePx = sp.optInt("logoSizePx", defaults.logoSizePx),
                logoAlignment = sp.optString("logoAlignment", defaults.logoAlignment),
                headerAlignment = sp.optString("headerAlignment", defaults.headerAlignment),
                titleFontSize = sp.optString("titleFontSize", defaults.titleFontSize),
                dividerStyle = sp.optString("dividerStyle", defaults.dividerStyle),
                customHeaderNote = sp.optString("customHeaderNote", defaults.customHeaderNote),
                socialMediaInfo = sp.optString("socialMediaInfo", defaults.socialMediaInfo),
                showSlogan = sp.optBoolean("showSlogan", defaults.showSlogan),
                showAddressPhone = sp.optBoolean("showAddressPhone", defaults.showAddressPhone),
                showInvoiceNumber = sp.optBoolean("showInvoiceNumber", defaults.showInvoiceNumber),
                showDateTime = sp.optBoolean("showDateTime", defaults.showDateTime),
                showCashierName = sp.optBoolean("showCashierName", defaults.showCashierName),
                showCustomerName = sp.optBoolean("showCustomerName", defaults.showCustomerName),
                showItemNumber = sp.optBoolean("showItemNumber", defaults.showItemNumber),
                showTaxAndDiscount = sp.optBoolean("showTaxAndDiscount", defaults.showTaxAndDiscount),
                showSocialMedia = sp.optBoolean("showSocialMedia", defaults.showSocialMedia),
                showBarcode = sp.optBoolean("showBarcode", defaults.showBarcode),
                showSignatureBox = sp.optBoolean("showSignatureBox", defaults.showSignatureBox)
            )
        }

        val products = mutableListOf<Product>()
        val productsArray = root.optJSONArray("products")
        if (productsArray != null) {
            for (i in 0 until productsArray.length()) {
                val pObj = productsArray.getJSONObject(i)
                val name = pObj.optString("name", "").trim()
                if (name.isNotEmpty()) {
                    products.add(
                        Product(
                            id = pObj.optLong("id", 0L),
                            name = name,
                            sku = pObj.optString("sku", ""),
                            category = pObj.optString("category", "Umum"),
                            costPrice = pObj.optDouble("costPrice", 0.0),
                            sellingPrice = pObj.optDouble("sellingPrice", 0.0),
                            stock = pObj.optInt("stock", 0),
                            unit = pObj.optString("unit", "pcs"),
                            createdAt = pObj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = pObj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }

        val transactions = mutableListOf<TransactionWithItems>()
        val txArray = root.optJSONArray("transactions")
        if (txArray != null) {
            for (i in 0 until txArray.length()) {
                val txObj = txArray.getJSONObject(i)
                val invoiceNumber = txObj.optString("invoiceNumber", "").trim()
                if (invoiceNumber.isNotEmpty()) {
                    val txId = txObj.optLong("id", 0L)
                    val transaction = Transaction(
                        id = txId,
                        invoiceNumber = invoiceNumber,
                        timestamp = txObj.optLong("timestamp", System.currentTimeMillis()),
                        customerName = txObj.optString("customerName", "Pelanggan Umum"),
                        cashierName = txObj.optString("cashierName", "Kasir Toko"),
                        paymentMethod = txObj.optString("paymentMethod", "Tunai"),
                        subtotal = txObj.optDouble("subtotal", 0.0),
                        discountAmount = txObj.optDouble("discountAmount", 0.0),
                        taxAmount = txObj.optDouble("taxAmount", 0.0),
                        grandTotal = txObj.optDouble("grandTotal", 0.0),
                        paidAmount = txObj.optDouble("paidAmount", 0.0),
                        changeAmount = txObj.optDouble("changeAmount", 0.0),
                        status = txObj.optString("status", "SELESAI"),
                        notes = txObj.optString("notes", "")
                    )

                    val items = mutableListOf<TransactionItem>()
                    val itemsArray = txObj.optJSONArray("items")
                    if (itemsArray != null) {
                        for (j in 0 until itemsArray.length()) {
                            val itemObj = itemsArray.getJSONObject(j)
                            items.add(
                                TransactionItem(
                                    id = itemObj.optLong("id", 0L),
                                    transactionId = itemObj.optLong("transactionId", txId),
                                    productId = itemObj.optLong("productId", 0L),
                                    productName = itemObj.optString("productName", "Item"),
                                    originalPrice = itemObj.optDouble("originalPrice", 0.0),
                                    actualPrice = itemObj.optDouble("actualPrice", 0.0),
                                    quantity = itemObj.optInt("quantity", 1),
                                    subtotal = itemObj.optDouble("subtotal", 0.0),
                                    note = itemObj.optString("note", "")
                                )
                            )
                        }
                    }

                    transactions.add(TransactionWithItems(transaction = transaction, items = items))
                }
            }
        }

        return DatabaseBackupPayload(
            version = version,
            appName = appName,
            createdAt = createdAt,
            storeProfile = storeProfile,
            products = products,
            transactions = transactions
        )
    }

    fun saveBackupToLocalFile(
        context: Context,
        payload: DatabaseBackupPayload,
        customFileName: String? = null
    ): LocalBackupFileInfo {
        val dir = getBackupDirectory(context)
        val rawName = customFileName?.takeIf { it.isNotBlank() } ?: generateDefaultBackupFileName()
        val safeName = if (rawName.endsWith(".json", ignoreCase = true)) rawName else "$rawName.json"
        val file = File(dir, safeName)
        val jsonContent = serializeToJson(payload)
        file.writeText(jsonContent, Charsets.UTF_8)

        return LocalBackupFileInfo(
            fileName = file.name,
            filePath = file.absolutePath,
            createdAt = payload.createdAt,
            sizeBytes = file.length(),
            productCount = payload.products.size,
            transactionCount = payload.transactions.size,
            storeName = payload.storeProfile?.name ?: "AKIM ePOS Cashier"
        )
    }

    fun writeBackupToUri(
        context: Context,
        uri: Uri,
        payload: DatabaseBackupPayload
    ) {
        val jsonContent = serializeToJson(payload)
        val outputStream = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Tidak dapat membuka lokasi file untuk menyimpan backup.")
        outputStream.use { stream ->
            stream.write(jsonContent.toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }

    fun readBackupFromUri(
        context: Context,
        uri: Uri
    ): DatabaseBackupPayload {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Tidak dapat membaca file backup yang dipilih.")
        val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return deserializeFromJson(jsonString)
    }

    fun readBackupFromLocalFile(filePath: String): DatabaseBackupPayload {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File backup lokal tidak ditemukan: $filePath")
        }
        val jsonString = file.readText(Charsets.UTF_8)
        return deserializeFromJson(jsonString)
    }

    fun listLocalBackups(context: Context): List<LocalBackupFileInfo> {
        val dir = getBackupDirectory(context)
        val files = dir.listFiles { file ->
            file.isFile && file.name.endsWith(".json", ignoreCase = true)
        } ?: return emptyList()

        return files.mapNotNull { file ->
            try {
                val root = JSONObject(file.readText(Charsets.UTF_8))
                val createdAt = root.optLong("createdAt", file.lastModified())
                val productCount = root.optJSONArray("products")?.length() ?: 0
                val transactionCount = root.optJSONArray("transactions")?.length() ?: 0
                val storeName = root.optJSONObject("storeProfile")?.optString("name", "AKIM ePOS") ?: "AKIM ePOS"
                LocalBackupFileInfo(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    createdAt = createdAt,
                    sizeBytes = file.length(),
                    productCount = productCount,
                    transactionCount = transactionCount,
                    storeName = storeName
                )
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.createdAt }
    }

    fun deleteLocalBackup(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.exists()) file.delete() else false
    }
}
