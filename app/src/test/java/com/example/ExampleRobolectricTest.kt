package com.example

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.ParcelUuid
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.printer.BluetoothDeviceInfo
import com.example.printer.BluetoothDeviceType
import com.example.printer.BluetoothScannerUtility
import com.example.printer.EscPosPrinter
import com.example.data.model.ReportPeriod
import com.example.data.model.TransactionWithItems
import com.example.data.repository.SalesReportCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AKIM ePOS Cashier", appName)
    }

    @Test
    fun `test cart item price override and subtotal calculation`() {
        val product = Product(
            id = 1L,
            name = "Kopi Susu",
            sellingPrice = 18000.0,
            stock = 50
        )

        // Normal cart item
        val normalItem = CartItem(product = product, quantity = 2)
        assertEquals(18000.0, normalItem.effectiveUnitPrice, 0.01)
        assertEquals(36000.0, normalItem.subtotal, 0.01)
        assertEquals(false, normalItem.isPriceOverridden)

        // Rubah Harga (Price override)
        val overriddenItem = CartItem(
            product = product,
            quantity = 3,
            customPrice = 15000.0,
            discount = 1000.0
        )
        assertEquals(14000.0, overriddenItem.effectiveUnitPrice, 0.01)
        assertEquals(42000.0, overriddenItem.subtotal, 0.01)
        assertEquals(true, overriddenItem.isPriceOverridden)
    }

    @Test
    fun `test esc pos byte sequence and plain text generation`() {
        val store = StoreProfile(
            name = "TOKO AKIMBOOW",
            address = "Jl. Sudirman 10",
            phone = "0812345678",
            paperWidth = 58
        )
        val transaction = Transaction(
            id = 101L,
            invoiceNumber = "INV-20261018-001",
            subtotal = 50000.0,
            grandTotal = 50000.0,
            paidAmount = 100000.0,
            changeAmount = 50000.0
        )
        val items = listOf(
            TransactionItem(
                transactionId = 101L,
                productId = 1L,
                productName = "Kopi Susu",
                originalPrice = 18000.0,
                actualPrice = 18000.0,
                quantity = 2,
                subtotal = 36000.0
            )
        )

        val bytes = EscPosPrinter.buildReceiptBytes(store, transaction, items)
        assertTrue(bytes.isNotEmpty())

        val plainText = EscPosPrinter.generatePlainTextReceipt(store, transaction, items)
        assertTrue(plainText.contains("TOKO AKIMBOOW"))
        assertTrue(plainText.contains("INV-20261018-001"))
        assertTrue(plainText.contains("Kopi Susu"))
    }

    @Test
    fun `test bluetooth scanner utility thermal printer detection and types`() {
        // 1. Detect by name keywords (Classic & BLE printers)
        assertTrue(BluetoothScannerUtility.detectLikelyPrinter("RPP02N-Thermal", null, null))
        assertTrue(BluetoothScannerUtility.detectLikelyPrinter("MPT-II POS-58", null, null))
        assertTrue(BluetoothScannerUtility.detectLikelyPrinter("GOOJPRT PT-210", null, null))
        assertFalse(BluetoothScannerUtility.detectLikelyPrinter("Galaxy Buds Pro", null, null))

        // 2. Detect by Bluetooth Class IMAGING
        assertTrue(
            BluetoothScannerUtility.detectLikelyPrinter(
                "Unknown Device",
                BluetoothClass.Device.Major.IMAGING,
                null
            )
        )

        // 3. Detect by BLE Printer GATT Service UUID
        val blePrinterUuid = ParcelUuid.fromString("000018f0-0000-1000-8000-00805f9b34fb")
        assertTrue(
            BluetoothScannerUtility.detectLikelyPrinter(
                "BLE-Dev",
                null,
                listOf(blePrinterUuid)
            )
        )

        // 4. Device type mapping & RSSI quality label
        assertEquals(BluetoothDeviceType.LE, BluetoothScannerUtility.mapDeviceType(BluetoothDevice.DEVICE_TYPE_LE))
        assertEquals(BluetoothDeviceType.CLASSIC, BluetoothScannerUtility.mapDeviceType(BluetoothDevice.DEVICE_TYPE_CLASSIC))
        assertEquals(BluetoothDeviceType.DUAL, BluetoothScannerUtility.mapDeviceType(BluetoothDevice.DEVICE_TYPE_DUAL))

        val blePrinterInfo = BluetoothDeviceInfo(
            name = "RPP02-BLE",
            address = "AA:BB:CC:DD:EE:FF",
            deviceType = BluetoothDeviceType.LE,
            rssi = -52,
            isLikelyPrinter = true
        )
        assertTrue(blePrinterInfo.signalQualityLabel.contains("Sinyal Kuat"))
    }

    @Test
    fun `test room database persistent product and transaction storage`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val productDao = db.productDao()
            val transactionDao = db.transactionDao()
            val storeProfileDao = db.storeProfileDao()

            // 1. Insert products & observe via Flow
            val id1 = productDao.insert(
                Product(
                    name = "Indomie Goreng",
                    sku = "MKN-101",
                    category = "Makanan",
                    costPrice = 2800.0,
                    sellingPrice = 3500.0,
                    stock = 40,
                    unit = "bungkus"
                )
            )
            assertTrue(id1 > 0)

            val allProducts = productDao.getAllProducts().first()
            assertEquals(1, allProducts.size)
            assertEquals("Indomie Goreng", allProducts[0].name)

            // 2. Update price and reduce stock in Room
            productDao.updatePrice(id1, 4000.0)
            productDao.reduceStock(id1, 5)

            val updated = productDao.getProductById(id1)
            assertNotNull(updated)
            assertEquals(4000.0, updated!!.sellingPrice, 0.01)
            assertEquals(35, updated.stock)

            // 3. Persist StoreProfile in Room
            storeProfileDao.upsertStoreProfile(
                StoreProfile(name = "TOKO AKIMBOOW", paperWidth = 80)
            )
            val savedProfile = storeProfileDao.getStoreProfile()
            assertNotNull(savedProfile)
            assertEquals("TOKO AKIMBOOW", savedProfile!!.name)
            assertEquals(80, savedProfile.paperWidth)

            // 4. Insert Transaction & TransactionItem and read relational TransactionWithItems
            val txId = transactionDao.insertTransaction(
                Transaction(
                    invoiceNumber = "INV-TEST-001",
                    subtotal = 20000.0,
                    grandTotal = 20000.0,
                    paidAmount = 20000.0,
                    changeAmount = 0.0
                )
            )
            transactionDao.insertTransactionItems(
                listOf(
                    TransactionItem(
                        transactionId = txId,
                        productId = id1,
                        productName = "Indomie Goreng",
                        originalPrice = 4000.0,
                        actualPrice = 4000.0,
                        quantity = 5,
                        subtotal = 20000.0
                    )
                )
            )

            val txWithItems = transactionDao.getTransactionWithItemsById(txId)
            assertNotNull(txWithItems)
            assertEquals("INV-TEST-001", txWithItems!!.transaction.invoiceNumber)
            assertEquals(1, txWithItems.items.size)
            assertEquals("Indomie Goreng", txWithItems.items[0].productName)

            // 5. Delete product
            productDao.deleteById(id1)
            assertNull(productDao.getProductById(id1))
        } finally {
            db.close()
        }
    }

    @Test
    fun `test multi language localization and receipt logo layout customization`() {
        // Verify all 22 languages return valid localized strings
        for (lang in com.example.ui.localization.AppLanguage.values()) {
            val localized = com.example.ui.localization.AppLocalization.getStrings(lang.code)
            assertTrue(localized.navCashier.isNotBlank())
            assertTrue(localized.receiptTotal.isNotBlank())
            assertTrue(localized.saveSettingsButton.isNotBlank())
        }

        // Verify Javanese, Sundanese, Cantonese, Arabic, and Spanish receipt labels
        assertEquals("Susuk", com.example.ui.localization.AppLocalization.getStrings("jv").receiptChange)
        assertEquals("Angsulan", com.example.ui.localization.AppLocalization.getStrings("su").receiptChange)
        assertEquals("找贖", com.example.ui.localization.AppLocalization.getStrings("yue").receiptChange)
        assertEquals("الباقي", com.example.ui.localization.AppLocalization.getStrings("ar").receiptChange)

        // Verify LogoBitmapProcessor generates bitmap and ESC/POS raster bytes (GS v 0)
        val bitmap = com.example.printer.LogoBitmapProcessor.loadOrGenerateLogoBitmap(
            presetCode = "CAFE_COFFEE",
            customPath = "",
            targetWidthPx = 184,
            storeNameInitials = "KOP"
        )
        assertNotNull(bitmap)
        val rasterBytes = com.example.printer.LogoBitmapProcessor.bitmapToEscPosRasterBytes(bitmap!!)
        assertTrue(rasterBytes.size > 8)
        assertEquals(0x1D.toByte(), rasterBytes[0])
        assertEquals(0x76.toByte(), rasterBytes[1])
        assertEquals(0x30.toByte(), rasterBytes[2])

        // Verify receipt layout editor options (Item numbering, Signature Box, Custom Currency, Star Divider)
        val customStore = StoreProfile(
            name = "Cafe Senja",
            languageCode = "en",
            currencySymbol = "$",
            dividerStyle = "STAR",
            showItemNumber = true,
            showSignatureBox = true,
            showSocialMedia = true,
            socialMediaInfo = "IG: @akimboow"
        )
        val tx = Transaction(
            id = 55L,
            invoiceNumber = "INV-999",
            subtotal = 25.0,
            grandTotal = 25.0,
            paidAmount = 30.0,
            changeAmount = 5.0
        )
        val items = listOf(
            TransactionItem(
                transactionId = 55L,
                productId = 1L,
                productName = "Espresso",
                originalPrice = 25.0,
                actualPrice = 25.0,
                quantity = 1,
                subtotal = 25.0
            )
        )

        val receiptText = EscPosPrinter.generatePlainTextReceipt(customStore, tx, items)
        assertTrue(receiptText.contains("1. Espresso"))
        assertTrue(receiptText.contains("Authorized Signature"))
        assertTrue(receiptText.contains("IG: @cafesenja"))
        assertTrue(receiptText.contains("*****"))

        // Verify all 11 theme options resolve to distinct valid Material 3 ColorSchemes
        val allThemes = com.example.ui.theme.AppThemeOption.values()
        assertEquals(11, allThemes.size)
        for (theme in allThemes) {
            val scheme = com.example.ui.theme.getColorSchemeForThemeOption(theme.code, systemInDarkTheme = false)
            assertNotNull(scheme.primary)
            assertNotNull(scheme.background)
        }
    }

    @Test
    fun `test room database export to local backup file and import restore`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = com.example.data.repository.StorePreferences(context)
        val printerManager = com.example.printer.BluetoothPrinterManager(context)
        val repo = com.example.data.repository.PosRepository(context, db, prefs, printerManager)

        try {
            // 1. Add products and a transaction to Room
            val p1Id = repo.addProduct(
                Product(
                    name = "Kopi Arabika Gayo",
                    sku = "KOP-001",
                    category = "Minuman",
                    costPrice = 15000.0,
                    sellingPrice = 25000.0,
                    stock = 50,
                    unit = "cup"
                )
            )
            val p2Id = repo.addProduct(
                Product(
                    name = "Roti Bakar Coklat",
                    sku = "MKN-002",
                    category = "Makanan",
                    costPrice = 8000.0,
                    sellingPrice = 15000.0,
                    stock = 30,
                    unit = "porsi"
                )
            )
            val p1 = repo.getProductById(p1Id)!!
            val p2 = repo.getProductById(p2Id)!!

            val checkoutRes = repo.checkout(
                cartItems = listOf(
                    CartItem(product = p1, quantity = 2),
                    CartItem(product = p2, quantity = 1)
                ),
                customerName = "Budi Santoso",
                cashierName = "Kasir Utama",
                paymentMethod = "QRIS",
                discountAmount = 5000.0,
                taxAmount = 0.0,
                paidAmount = 60000.0,
                notes = "Meja 4"
            )
            assertTrue(checkoutRes.isSuccess)
            val completedTx = checkoutRes.getOrThrow()

            // 2. Export Room database to a local backup file
            val exportRes = repo.exportDatabaseToLocalBackup("test_backup_akim_epos.json")
            assertTrue(exportRes.isSuccess)
            val backupInfo = exportRes.getOrThrow()
            assertEquals(2, backupInfo.productCount)
            assertEquals(1, backupInfo.transactionCount)

            val savedBackups = repo.getLocalBackupFiles()
            assertTrue(savedBackups.any { it.fileName == "test_backup_akim_epos.json" })

            // 3. Wipe database completely and verify empty state
            repo.clearTransactionHistory()
            db.productDao().deleteAllProducts()
            assertEquals(0, db.productDao().getCount())
            assertEquals(0, db.transactionDao().getAllTransactionsList().size)

            // 4. Import backup back in Replace mode
            val importRes = repo.importDatabaseFromLocalFile(
                filePath = backupInfo.filePath,
                replaceExisting = true,
                restoreStoreSettings = true
            )
            assertTrue(importRes.isSuccess)
            val summary = importRes.getOrThrow()
            assertEquals(2, summary.productsImported)
            assertEquals(1, summary.transactionsImported)
            assertEquals(2, summary.itemsImported)

            // Verify products and transactions with items are restored in Room
            val restoredProducts = db.productDao().getAllProductsList()
            assertEquals(2, restoredProducts.size)
            assertTrue(restoredProducts.any { it.name == "Kopi Arabika Gayo" && it.stock == 48 })

            val restoredTransactions = db.transactionDao().getAllTransactionsWithItemsList()
            assertEquals(1, restoredTransactions.size)
            assertEquals(completedTx.transaction.invoiceNumber, restoredTransactions[0].transaction.invoiceNumber)
            assertEquals("Budi Santoso", restoredTransactions[0].transaction.customerName)
            assertEquals(2, restoredTransactions[0].items.size)

            // 5. Clean up test backup file
            assertTrue(repo.deleteLocalBackupFile(backupInfo.filePath))
        } finally {
            db.close()
        }
    }

    @Test
    fun `test adaptive window size class for phone foldable and tablet`() {
        val compact = com.example.ui.common.calculateAppWindowSizeClass(androidx.compose.ui.unit.Dp(412f))
        assertEquals(com.example.ui.common.AppWindowSizeClass.COMPACT, compact)
        assertFalse(compact.isTabletOrWide)

        val mediumTablet = com.example.ui.common.calculateAppWindowSizeClass(androidx.compose.ui.unit.Dp(720f))
        assertEquals(com.example.ui.common.AppWindowSizeClass.MEDIUM, mediumTablet)
        assertTrue(mediumTablet.isTabletOrWide)
        assertFalse(mediumTablet.isExpanded)

        val expandedTablet = com.example.ui.common.calculateAppWindowSizeClass(androidx.compose.ui.unit.Dp(1024f))
        assertEquals(com.example.ui.common.AppWindowSizeClass.EXPANDED, expandedTablet)
        assertTrue(expandedTablet.isTabletOrWide)
        assertTrue(expandedTablet.isExpanded)
    }

    @Test
    fun `test real time receipt visual preview state and 1 bit thermal logo simulation`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.ui.viewmodel.PosViewModel(context)

        // 1. Add item to cart and open live print preview before checkout
        val itemProduct = Product(
            id = 77L,
            name = "Matcha Latte Dingin",
            sku = "MTC-01",
            category = "Minuman",
            sellingPrice = 24000.0,
            stock = 25
        )
        viewModel.addToCart(itemProduct)
        viewModel.openLiveCartPrintPreview(
            overrideCustomerName = "Ibu Dewi",
            overridePaymentMethod = "QRIS",
            overrideDiscount = 2000.0,
            overrideTaxRate = 11.0,
            overridePaidAmount = 25000.0,
            overrideNotes = "Tanpa Sedotan"
        )

        val previewState = viewModel.uiState.value
        assertNotNull(previewState.printPreviewTransaction)
        assertTrue(previewState.isDraftPrintPreview)
        assertEquals("Ibu Dewi", previewState.printPreviewTransaction!!.transaction.customerName)
        assertEquals("QRIS", previewState.printPreviewTransaction!!.transaction.paymentMethod)
        assertEquals(1, previewState.printPreviewTransaction!!.items.size)
        assertEquals("Matcha Latte Dingin", previewState.printPreviewTransaction!!.items[0].productName)

        // 2. Verify 1-bit monochrome thermal preview bitmap generation
        val rawBitmap = com.example.printer.LogoBitmapProcessor.loadOrGenerateLogoBitmap(
            presetCode = "STORE_MART",
            customPath = "",
            targetWidthPx = 184,
            storeNameInitials = "AKM"
        )
        assertNotNull(rawBitmap)
        val monoPreview = com.example.printer.LogoBitmapProcessor.createMonochromePreviewBitmap(rawBitmap!!)
        assertEquals(rawBitmap.width, monoPreview.width)
        assertEquals(rawBitmap.height, monoPreview.height)

        // 3. Verify real-time 58mm (32 char) vs 80mm (48 char) matrix & ESC/POS byte recalculation
        val store58 = StoreProfile(name = "AKIM ePOS", paperWidth = 58, dividerStyle = "DASH")
        val store80 = store58.copy(paperWidth = 80)
        val matrix58 = EscPosPrinter.generatePlainTextReceipt(
            store58,
            previewState.printPreviewTransaction!!.transaction,
            previewState.printPreviewTransaction!!.items
        )
        val matrix80 = EscPosPrinter.generatePlainTextReceipt(
            store80,
            previewState.printPreviewTransaction!!.transaction,
            previewState.printPreviewTransaction!!.items
        )
        assertTrue(matrix58.contains("-".repeat(32)))
        assertTrue(matrix80.contains("-".repeat(48)))

        // 4. Close preview screen
        viewModel.closeThermalPrintPreview()
        assertNull(viewModel.uiState.value.printPreviewTransaction)
    }

    @Test
    fun `test onboarding welcome popup shown on fresh install and persists dismissed state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Clear prefs to simulate fresh install
        val prefs = context.getSharedPreferences("notapos_store_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("onboarding_completed").apply()

        val storePreferences = com.example.data.repository.StorePreferences(context)
        assertFalse(storePreferences.isOnboardingCompleted())

        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.ui.viewmodel.PosViewModel(app)

        // Fresh install -> showOnboarding is true
        assertTrue(viewModel.showOnboarding.value)

        // User finishes/dismisses onboarding
        viewModel.dismissOnboarding()
        assertFalse(viewModel.showOnboarding.value)
        assertTrue(storePreferences.isOnboardingCompleted())

        // Create new viewModel instance (simulate next app launch)
        val nextLaunchViewModel = com.example.ui.viewmodel.PosViewModel(app)
        assertFalse(nextLaunchViewModel.showOnboarding.value)

        // Manual reopen from Settings/Help
        nextLaunchViewModel.openOnboardingGuide()
        assertTrue(nextLaunchViewModel.showOnboarding.value)
    }

    @Test
    fun `test catalog deletion options delete selected by category and delete all`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.ui.viewmodel.PosViewModel(app)

        // Ensure database has sample products loaded
        viewModel.restoreSampleProducts()
        val initialProducts = viewModel.allProducts.first { it.isNotEmpty() }
        assertTrue("Initial products should not be empty", initialProducts.size >= 5)

        // 1. TEST HAPUS PILIH (Delete selected products)
        val toDelete = initialProducts.take(2).map { it.id }
        var callbackExecuted = false
        viewModel.deleteProducts(toDelete) {
            callbackExecuted = true
        }
        val remainingAfterSelected = viewModel.allProducts.first { it.size == initialProducts.size - 2 }
        assertEquals(initialProducts.size - 2, remainingAfterSelected.size)
        assertTrue(callbackExecuted)
        toDelete.forEach { deletedId ->
            assertFalse(remainingAfterSelected.any { it.id == deletedId })
        }

        // 2. TEST HAPUS PER KATEGORI (Delete by Category)
        val categoryToDelete = remainingAfterSelected.first().category
        val countInCategory = remainingAfterSelected.count { it.category.equals(categoryToDelete, ignoreCase = true) }
        var catCallbackExecuted = false
        viewModel.deleteProductsByCategory(categoryToDelete) {
            catCallbackExecuted = true
        }
        val remainingAfterCat = viewModel.allProducts.first { it.none { p -> p.category.equals(categoryToDelete, ignoreCase = true) } }
        assertTrue(catCallbackExecuted)
        assertEquals(remainingAfterSelected.size - countInCategory, remainingAfterCat.size)

        // 3. TEST HAPUS SEMUA (Delete All Products)
        var allCallbackExecuted = false
        viewModel.deleteAllProducts {
            allCallbackExecuted = true
        }
        val emptyProducts = viewModel.allProducts.first { it.isEmpty() }
        assertTrue(allCallbackExecuted)
        assertEquals(0, emptyProducts.size)

        // 4. TEST RESTORE SAMPLE PRODUCTS (Reload after clearing)
        viewModel.restoreSampleProducts()
        val restoredProducts = viewModel.allProducts.first { it.isNotEmpty() }
        assertTrue(restoredProducts.isNotEmpty())
    }

    @Test
    fun `test sales report and analytics daily weekly monthly yearly and best seller calculation`() = runBlocking {
        val product1 = Product(id = 1L, name = "Kopi Susu Gula Aren", category = "Minuman", sellingPrice = 18000.0)
        val product2 = Product(id = 2L, name = "Croissant Cokelat", category = "Makanan", sellingPrice = 25000.0)
        val product3 = Product(id = 3L, name = "Air Mineral Dingin", category = "Minuman", sellingPrice = 5000.0)
        val products = listOf(product1, product2, product3)

        val now = System.currentTimeMillis()
        val tx1 = Transaction(
            id = 1L,
            invoiceNumber = "INV-001",
            timestamp = now,
            subtotal = 36000.0,
            grandTotal = 36000.0,
            paidAmount = 50000.0,
            changeAmount = 14000.0,
            paymentMethod = "Tunai"
        )
        val items1 = listOf(
            TransactionItem(id = 1L, transactionId = 1L, productId = 1L, productName = "Kopi Susu Gula Aren", originalPrice = 18000.0, actualPrice = 18000.0, quantity = 2, subtotal = 36000.0)
        )

        val tx2 = Transaction(
            id = 2L,
            invoiceNumber = "INV-002",
            timestamp = now,
            subtotal = 55000.0,
            grandTotal = 55000.0,
            paidAmount = 55000.0,
            changeAmount = 0.0,
            paymentMethod = "QRIS"
        )
        val items2 = listOf(
            TransactionItem(id = 2L, transactionId = 2L, productId = 1L, productName = "Kopi Susu Gula Aren", originalPrice = 18000.0, actualPrice = 18000.0, quantity = 1, subtotal = 18000.0),
            TransactionItem(id = 3L, transactionId = 2L, productId = 2L, productName = "Croissant Cokelat", originalPrice = 25000.0, actualPrice = 25000.0, quantity = 1, subtotal = 25000.0),
            TransactionItem(id = 4L, transactionId = 2L, productId = 3L, productName = "Air Mineral Dingin", originalPrice = 5000.0, actualPrice = 5000.0, quantity = 2, subtotal = 10000.0)
        )

        val sampleTransactionsWithItems = listOf(
            TransactionWithItems(tx1, items1),
            TransactionWithItems(tx2, items2)
        )

        // 1. DAILY REPORT
        val dailyReport = SalesReportCalculator.calculateReport(
            transactionsWithItems = sampleTransactionsWithItems,
            period = ReportPeriod.DAILY,
            dateOffset = 0,
            products = products
        )
        assertEquals(91000.0, dailyReport.totalRevenue, 0.01)
        assertEquals(2, dailyReport.totalTransactions)
        assertEquals(45500.0, dailyReport.averageOrderValue, 0.01)
        assertEquals(6, dailyReport.totalItemsSold)

        // Verify Best Sellers:
        // Kopi Susu Gula Aren: qty = 2 + 1 = 3 (Rank 1)
        // Air Mineral Dingin: qty = 2 (Rank 2)
        // Croissant Cokelat: qty = 1 (Rank 3)
        assertEquals(3, dailyReport.bestSellers.size)
        assertEquals("Kopi Susu Gula Aren", dailyReport.bestSellers[0].productName)
        assertEquals(3, dailyReport.bestSellers[0].totalQuantitySold)
        assertEquals(54000.0, dailyReport.bestSellers[0].totalRevenue, 0.01)
        assertEquals("Air Mineral Dingin", dailyReport.bestSellers[1].productName)
        assertEquals(2, dailyReport.bestSellers[1].totalQuantitySold)
        assertEquals("Croissant Cokelat", dailyReport.bestSellers[2].productName)
        assertEquals(1, dailyReport.bestSellers[2].totalQuantitySold)

        // Verify Payment distribution
        assertEquals(2, dailyReport.paymentMethods.size)
        assertTrue(dailyReport.paymentMethods.any { it.method == "QRIS" && it.count == 1 && it.totalAmount == 55000.0 })
        assertTrue(dailyReport.paymentMethods.any { it.method == "Tunai" && it.count == 1 && it.totalAmount == 36000.0 })

        // 2. WEEKLY REPORT
        val weeklyReport = SalesReportCalculator.calculateReport(
            transactionsWithItems = sampleTransactionsWithItems,
            period = ReportPeriod.WEEKLY,
            dateOffset = 0,
            products = products
        )
        assertEquals(91000.0, weeklyReport.totalRevenue, 0.01)
        assertEquals(7, weeklyReport.trendPoints.size)

        // 3. MONTHLY REPORT
        val monthlyReport = SalesReportCalculator.calculateReport(
            transactionsWithItems = sampleTransactionsWithItems,
            period = ReportPeriod.MONTHLY,
            dateOffset = 0,
            products = products
        )
        assertEquals(91000.0, monthlyReport.totalRevenue, 0.01)
        assertEquals(4, monthlyReport.trendPoints.size)

        // 4. YEARLY REPORT
        val yearlyReport = SalesReportCalculator.calculateReport(
            transactionsWithItems = sampleTransactionsWithItems,
            period = ReportPeriod.YEARLY,
            dateOffset = 0,
            products = products
        )
        assertEquals(91000.0, yearlyReport.totalRevenue, 0.01)
        assertEquals(12, yearlyReport.trendPoints.size)

        // 5. THERMAL SALES REPORT RECEIPT GENERATION
        val store = StoreProfile(name = "TOKO AKIMBOOW", paperWidth = 58)
        val thermalText = SalesReportCalculator.buildThermalSalesReportText(dailyReport, store)
        assertTrue(thermalText.contains("LAPORAN PENJUALAN HARIAN"))
        assertTrue(thermalText.contains("TOKO AKIMBOOW"))
        assertTrue(thermalText.contains("Total Omset"))
        assertTrue(thermalText.contains("Kopi Susu Gula Aren"))

        val thermalBytes = SalesReportCalculator.buildThermalSalesReportBytes(dailyReport, store)
        assertTrue(thermalBytes.isNotEmpty())
    }

    @Test
    fun `test barcode scanning adds product to cart and handles unknown barcode`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.ui.viewmodel.PosViewModel(app)

        // Ensure sample products are loaded
        viewModel.restoreSampleProducts()
        val products = viewModel.allProducts.first { it.isNotEmpty() }
        assertTrue(products.isNotEmpty())

        viewModel.clearCart()
        assertEquals(0, viewModel.uiState.value.cartItems.size)

        // 1. Scan known barcode MNM-001 (Kopi Susu Gula Aren)
        var scan1Result: com.example.ui.viewmodel.BarcodeScanResult? = null
        viewModel.scanBarcodeAndAddToCart("MNM-001") { result ->
            scan1Result = result
        }

        // Wait for coroutine to process
        kotlinx.coroutines.delay(100)

        assertTrue(scan1Result is com.example.ui.viewmodel.BarcodeScanResult.Success)
        val success1 = scan1Result as com.example.ui.viewmodel.BarcodeScanResult.Success
        assertEquals("MNM-001", success1.product.sku)
        assertEquals(1, viewModel.uiState.value.cartItems.size)
        assertEquals(1, viewModel.uiState.value.cartItems[0].quantity)

        // 2. Scan same barcode again -> should increment quantity
        var scan2Result: com.example.ui.viewmodel.BarcodeScanResult? = null
        viewModel.scanBarcodeAndAddToCart("MNM-001") { result ->
            scan2Result = result
        }
        kotlinx.coroutines.delay(100)

        assertTrue(scan2Result is com.example.ui.viewmodel.BarcodeScanResult.Success)
        assertEquals(1, viewModel.uiState.value.cartItems.size)
        assertEquals(2, viewModel.uiState.value.cartItems[0].quantity)

        // 3. Scan second product MKN-001 (Mie Goreng Spesial Telur)
        var scan3Result: com.example.ui.viewmodel.BarcodeScanResult? = null
        viewModel.scanBarcodeAndAddToCart("MKN-001") { result ->
            scan3Result = result
        }
        kotlinx.coroutines.delay(100)

        assertTrue(scan3Result is com.example.ui.viewmodel.BarcodeScanResult.Success)
        assertEquals(2, viewModel.uiState.value.cartItems.size)
        assertEquals(3, viewModel.uiState.value.totalItemCount)

        // 4. Scan non-existent barcode
        var notFoundResult: com.example.ui.viewmodel.BarcodeScanResult? = null
        viewModel.scanBarcodeAndAddToCart("UNKNOWN-BARCODE-999") { result ->
            notFoundResult = result
        }
        kotlinx.coroutines.delay(100)

        assertTrue(notFoundResult is com.example.ui.viewmodel.BarcodeScanResult.NotFound)
        assertEquals("UNKNOWN-BARCODE-999", (notFoundResult as com.example.ui.viewmodel.BarcodeScanResult.NotFound).barcode)
        // Cart item count unchanged
        assertEquals(2, viewModel.uiState.value.cartItems.size)
        assertEquals(3, viewModel.uiState.value.totalItemCount)

        // 5. Add custom item with barcode and verify it can then be scanned
        viewModel.addCustomItem(
            name = "Roti Cokelat Keju",
            price = 15000.0,
            quantity = 1,
            saveToCatalog = true,
            category = "Snack",
            sku = "SNK-999"
        )
        kotlinx.coroutines.delay(200)

        var newProductScanResult: com.example.ui.viewmodel.BarcodeScanResult? = null
        viewModel.scanBarcodeAndAddToCart("SNK-999") { result ->
            newProductScanResult = result
        }
        kotlinx.coroutines.delay(150)

        assertTrue(newProductScanResult is com.example.ui.viewmodel.BarcodeScanResult.Success)
        val successNew = newProductScanResult as com.example.ui.viewmodel.BarcodeScanResult.Success
        assertEquals("Roti Cokelat Keju", successNew.product.name)
        assertEquals("SNK-999", successNew.product.sku)
    }
}
