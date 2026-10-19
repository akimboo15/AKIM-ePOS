package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionWithItems
import com.example.printer.BluetoothPrinterManager
import com.example.printer.EscPosPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class PosRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val storePreferences: StorePreferences,
    val printerManager: BluetoothPrinterManager
) {
    private val productDao = database.productDao()
    private val transactionDao = database.transactionDao()
    private val storeProfileDao = database.storeProfileDao()

    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allCategories: Flow<List<String>> = productDao.getAllCategories()
    val productCount: Flow<Int> = productDao.observeProductCount()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allTransactionsWithItems: Flow<List<TransactionWithItems>> = transactionDao.getAllTransactionsWithItems()
    val storeProfile = storePreferences.storeProfile

    /**
     * Ensures that initial sample products and store profile are seeded into Room Database
     * on the very first install, while preserving all user changes across subsequent restarts.
     */
    suspend fun ensureDatabaseInitialized() = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("notapos_db_init", Context.MODE_PRIVATE)
        val alreadySeeded = prefs.getBoolean("initial_catalog_seeded", false)
        if (!alreadySeeded) {
            if (productDao.getCount() == 0) {
                productDao.insertAll(AppDatabase.getInitialProducts())
            }
            prefs.edit().putBoolean("initial_catalog_seeded", true).apply()
        }

        val dbProfile = storeProfileDao.getStoreProfile()
        if (dbProfile == null) {
            storeProfileDao.upsertStoreProfile(storePreferences.storeProfile.value)
        } else {
            storePreferences.saveProfile(dbProfile)
        }
    }

    suspend fun restoreSampleProducts() = withContext(Dispatchers.IO) {
        val sampleItems = AppDatabase.getInitialProducts()
        for (sample in sampleItems) {
            val existing = productDao.getProductBySku(sample.sku)
            if (existing == null) {
                productDao.insert(sample)
            }
        }
    }

    suspend fun clearTransactionHistory() = withContext(Dispatchers.IO) {
        transactionDao.deleteAllTransactions()
    }

    suspend fun shareSalesReportCsv() = withContext(Dispatchers.IO) {
        val transactions = transactionDao.getAllTransactionsList()
        val store = storeProfile.value
        val sb = StringBuilder()
        sb.appendLine("No.Nota,Tanggal,Kasir,Pelanggan,MetodeBayar,Subtotal,Diskon,Pajak,GrandTotal,Dibayar,Kembalian,Status")
        for (tx in transactions) {
            sb.appendLine(
                "\"${tx.invoiceNumber}\",\"${tx.formattedDate}\",\"${tx.cashierName}\",\"${tx.customerName}\",\"${tx.paymentMethod}\",${tx.subtotal.toLong()},${tx.discountAmount.toLong()},${tx.taxAmount.toLong()},${tx.grandTotal.toLong()},${tx.paidAmount.toLong()},${tx.changeAmount.toLong()},\"${tx.status}\""
            )
        }
        withContext(Dispatchers.Main) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Penjualan ${store.name}")
                putExtra(Intent.EXTRA_TEXT, sb.toString())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Ekspor Laporan Penjualan CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    // Room Database Backup & Restore (Export / Import)
    suspend fun buildBackupPayload(): DatabaseBackupPayload = withContext(Dispatchers.IO) {
        val products = productDao.getAllProductsList()
        val transactions = transactionDao.getAllTransactionsWithItemsList()
        val profile = storeProfileDao.getStoreProfile() ?: storeProfile.value
        DatabaseBackupPayload(
            version = 1,
            appName = "AKIM ePOS Cashier",
            createdAt = System.currentTimeMillis(),
            storeProfile = profile,
            products = products,
            transactions = transactions
        )
    }

    suspend fun exportDatabaseToLocalBackup(customFileName: String? = null): Result<LocalBackupFileInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildBackupPayload()
            BackupManager.saveBackupToLocalFile(context, payload, customFileName)
        }
    }

    suspend fun exportDatabaseToUri(uri: Uri): Result<LocalBackupFileInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildBackupPayload()
            BackupManager.writeBackupToUri(context, uri, payload)
            // Also save a local backup copy for quick access in app settings
            BackupManager.saveBackupToLocalFile(context, payload)
        }
    }

    suspend fun importDatabaseFromPayload(
        payload: DatabaseBackupPayload,
        replaceExisting: Boolean = false,
        restoreStoreSettings: Boolean = false
    ): Result<ImportBackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            var totalItemsImported = 0
            database.withTransaction {
                if (replaceExisting) {
                    transactionDao.deleteAllTransactions()
                    productDao.deleteAllProducts()

                    productDao.insertAll(payload.products)

                    for (txWithItems in payload.transactions) {
                        val insertedTxId = transactionDao.insertTransaction(txWithItems.transaction)
                        val mappedItems = txWithItems.items.map { item ->
                            item.copy(id = 0L, transactionId = insertedTxId)
                        }
                        transactionDao.insertTransactionItems(mappedItems)
                        totalItemsImported += mappedItems.size
                    }
                } else {
                    // Merge / Upsert mode
                    for (product in payload.products) {
                        val existingBySku = if (product.sku.isNotBlank()) {
                            productDao.getProductBySku(product.sku)
                        } else {
                            null
                        }
                        val existingById = if (product.id > 0L) {
                            productDao.getProductById(product.id)
                        } else {
                            null
                        }

                        when {
                            existingBySku != null -> {
                                productDao.insert(product.copy(id = existingBySku.id))
                            }
                            existingById == null -> {
                                productDao.insert(product)
                            }
                            existingById.name.equals(product.name, ignoreCase = true) -> {
                                productDao.insert(product.copy(id = existingById.id))
                            }
                            else -> {
                                productDao.insert(product.copy(id = 0L))
                            }
                        }
                    }

                    for (txWithItems in payload.transactions) {
                        val tx = txWithItems.transaction
                        val existingByInvoice = transactionDao.getTransactionByInvoiceNumber(tx.invoiceNumber)
                        val targetTxId = if (existingByInvoice != null) {
                            transactionDao.deleteTransaction(existingByInvoice)
                            transactionDao.insertTransaction(tx.copy(id = existingByInvoice.id))
                        } else {
                            val existingById = if (tx.id > 0L) transactionDao.getTransactionById(tx.id) else null
                            val toInsert = if (existingById == null) tx else tx.copy(id = 0L)
                            transactionDao.insertTransaction(toInsert)
                        }

                        val mappedItems = txWithItems.items.map { item ->
                            item.copy(id = 0L, transactionId = targetTxId)
                        }
                        transactionDao.insertTransactionItems(mappedItems)
                        totalItemsImported += mappedItems.size
                    }
                }
            }

            var storeRestored = false
            if (restoreStoreSettings && payload.storeProfile != null) {
                updateStoreProfile(payload.storeProfile)
                storeRestored = true
            }

            // Mark initial catalog as seeded so default sample seeding won't overwrite imported state
            context.getSharedPreferences("notapos_db_init", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("initial_catalog_seeded", true)
                .apply()

            ImportBackupSummary(
                productsImported = payload.products.size,
                transactionsImported = payload.transactions.size,
                itemsImported = totalItemsImported,
                storeProfileRestored = storeRestored
            )
        }
    }

    suspend fun importDatabaseFromLocalFile(
        filePath: String,
        replaceExisting: Boolean = false,
        restoreStoreSettings: Boolean = false
    ): Result<ImportBackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = BackupManager.readBackupFromLocalFile(filePath)
            importDatabaseFromPayload(payload, replaceExisting, restoreStoreSettings).getOrThrow()
        }
    }

    suspend fun importDatabaseFromUri(
        uri: Uri,
        replaceExisting: Boolean = false,
        restoreStoreSettings: Boolean = false
    ): Result<ImportBackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = BackupManager.readBackupFromUri(context, uri)
            importDatabaseFromPayload(payload, replaceExisting, restoreStoreSettings).getOrThrow()
        }
    }

    suspend fun getLocalBackupFiles(): List<LocalBackupFileInfo> = withContext(Dispatchers.IO) {
        BackupManager.listLocalBackups(context)
    }

    suspend fun deleteLocalBackupFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        BackupManager.deleteLocalBackup(filePath)
    }

    suspend fun shareBackupJson(filePath: String? = null) = withContext(Dispatchers.IO) {
        val payload = if (filePath != null) {
            BackupManager.readBackupFromLocalFile(filePath)
        } else {
            buildBackupPayload()
        }
        val jsonText = BackupManager.serializeToJson(payload)
        val store = storeProfile.value
        withContext(Dispatchers.Main) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "Backup Database ${store.name} (AKIM ePOS Cashier)")
                putExtra(Intent.EXTRA_TEXT, jsonText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Bagikan File Backup JSON").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    // Product Room CRUD Operations
    suspend fun getProductBySku(sku: String): Product? = withContext(Dispatchers.IO) {
        val trimmed = sku.trim()
        if (trimmed.isEmpty()) null else productDao.getProductBySku(trimmed)
    }

    suspend fun addProduct(product: Product): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        productDao.insert(product.copy(createdAt = now, updatedAt = now))
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.update(product.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateProductPrice(productId: Long, newPrice: Double) = withContext(Dispatchers.IO) {
        productDao.updatePrice(productId, newPrice, System.currentTimeMillis())
    }

    suspend fun updateProductStock(productId: Long, newStock: Int) = withContext(Dispatchers.IO) {
        productDao.updateStock(productId, newStock, System.currentTimeMillis())
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.delete(product)
    }

    suspend fun deleteProducts(productIds: List<Long>): Int = withContext(Dispatchers.IO) {
        if (productIds.isEmpty()) 0 else productDao.deleteProductsByIds(productIds)
    }

    suspend fun deleteProductsByCategory(category: String): Int = withContext(Dispatchers.IO) {
        productDao.deleteProductsByCategory(category)
    }

    suspend fun deleteAllProducts() = withContext(Dispatchers.IO) {
        productDao.deleteAllProducts()
    }

    suspend fun getProductById(id: Long): Product? = withContext(Dispatchers.IO) {
        productDao.getProductById(id)
    }

    // Checkout & Transaction Processing in Room
    suspend fun checkout(
        cartItems: List<CartItem>,
        customerName: String,
        cashierName: String,
        paymentMethod: String,
        discountAmount: Double,
        taxAmount: Double,
        paidAmount: Double,
        notes: String
    ): Result<TransactionWithItems> = withContext(Dispatchers.IO) {
        if (cartItems.isEmpty()) {
            return@withContext Result.failure(Exception("Keranjang belanja kosong."))
        }

        val subtotal = cartItems.sumOf { it.subtotal }
        val grandTotal = (subtotal - discountAmount + taxAmount).coerceAtLeast(0.0)

        if (paidAmount < grandTotal) {
            return@withContext Result.failure(Exception("Nominal bayar kurang dari total belanja."))
        }

        val changeAmount = paidAmount - grandTotal
        val invoiceNumber = generateInvoiceNumber()

        val transaction = Transaction(
            invoiceNumber = invoiceNumber,
            timestamp = System.currentTimeMillis(),
            customerName = if (customerName.isBlank()) "Pelanggan Umum" else customerName,
            cashierName = if (cashierName.isBlank()) storeProfile.value.defaultCashier else cashierName,
            paymentMethod = paymentMethod,
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            grandTotal = grandTotal,
            paidAmount = paidAmount,
            changeAmount = changeAmount,
            status = "SELESAI",
            notes = notes
        )

        val transactionId = transactionDao.insertTransaction(transaction)
        val savedTransaction = transaction.copy(id = transactionId)

        val transactionItems = cartItems.map { cartItem ->
            TransactionItem(
                transactionId = transactionId,
                productId = cartItem.product.id,
                productName = cartItem.product.name,
                originalPrice = cartItem.product.sellingPrice,
                actualPrice = cartItem.effectiveUnitPrice,
                quantity = cartItem.quantity,
                subtotal = cartItem.subtotal,
                note = cartItem.note
            )
        }

        transactionDao.insertTransactionItems(transactionItems)

        // Deduct inventory stock in Room if product ID exists in DB
        for (item in cartItems) {
            if (item.product.id > 0) {
                productDao.reduceStock(item.product.id, item.quantity)
            }
        }

        Result.success(TransactionWithItems(savedTransaction, transactionItems))
    }

    suspend fun getTransactionDetails(transactionId: Long): TransactionWithItems? = withContext(Dispatchers.IO) {
        transactionDao.getTransactionWithItemsById(transactionId)
    }

    suspend fun printReceipt(transactionWithItems: TransactionWithItems): Result<Boolean> {
        val store = storeProfile.value
        val bytes = EscPosPrinter.buildReceiptBytes(
            store = store,
            transaction = transactionWithItems.transaction,
            items = transactionWithItems.items
        )
        val textSummary = EscPosPrinter.generatePlainTextReceipt(
            store = store,
            transaction = transactionWithItems.transaction,
            items = transactionWithItems.items
        )
        return printerManager.print(bytes, textSummary)
    }

    suspend fun testPrint(): Result<Boolean> {
        val store = storeProfile.value
        val bytes = EscPosPrinter.buildTestPrintBytes(store)
        return printerManager.print(bytes, "=== TEST PRINT STRUK ===")
    }

    fun shareReceiptAsText(transactionWithItems: TransactionWithItems) {
        val store = storeProfile.value
        val receiptText = EscPosPrinter.generatePlainTextReceipt(
            store = store,
            transaction = transactionWithItems.transaction,
            items = transactionWithItems.items
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Nota Struk ${transactionWithItems.transaction.invoiceNumber}")
            putExtra(Intent.EXTRA_TEXT, receiptText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Bagikan Nota Struk").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    suspend fun updateStoreProfile(profile: StoreProfile) = withContext(Dispatchers.IO) {
        val normalized = profile.copy(id = 1)
        storePreferences.saveProfile(normalized)
        storeProfileDao.upsertStoreProfile(normalized)
    }

    private fun generateInvoiceNumber(): String {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val randomSuffix = Random.nextInt(1000, 9999)
        return "INV-$dateStr-$randomSuffix"
    }
}

