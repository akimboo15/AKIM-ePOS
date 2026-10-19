package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionWithItems
import com.example.data.repository.LocalBackupFileInfo
import com.example.data.repository.PosRepository
import com.example.data.repository.StorePreferences
import com.example.printer.BluetoothDeviceInfo
import com.example.printer.BluetoothPrinterManager
import com.example.printer.PrinterConnectionState
import com.example.printer.ScanMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.Intent
import com.example.data.model.ReportPeriod
import com.example.data.model.SalesAnalyticsReport
import com.example.data.repository.SalesReportCalculator
import java.util.Calendar

sealed class BarcodeScanResult {
    data class Success(val product: Product, val quantityInCart: Int) : BarcodeScanResult()
    data class NotFound(val barcode: String) : BarcodeScanResult()
}

data class PosUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "Semua",
    val cartItems: List<CartItem> = emptyList(),
    val overallDiscount: Double = 0.0,
    val taxRatePercent: Double = 0.0, // 0%, 11%, etc.
    val customerName: String = "",
    val paymentMethod: String = "Tunai",
    val paidAmountText: String = "",
    val notes: String = "",
    val lastCompletedTransaction: TransactionWithItems? = null,
    val selectedHistoryTransaction: TransactionWithItems? = null,
    val printPreviewTransaction: TransactionWithItems? = null,
    val isDraftPrintPreview: Boolean = false,
    val isCheckingOut: Boolean = false,
    val snackbarMessage: String? = null,
    val showCartBottomSheet: Boolean = false,
    val showBarcodeScanner: Boolean = false,
    val lastScannedBarcode: String? = null,
    val lastScannedProduct: Product? = null
) {
    val subtotal: Double
        get() = cartItems.sumOf { it.subtotal }

    val taxAmount: Double
        get() = if (taxRatePercent > 0) (subtotal - overallDiscount).coerceAtLeast(0.0) * (taxRatePercent / 100.0) else 0.0

    val grandTotal: Double
        get() = (subtotal - overallDiscount + taxAmount).coerceAtLeast(0.0)

    val paidAmount: Double
        get() = paidAmountText.toDoubleOrNull() ?: 0.0

    val changeAmount: Double
        get() = (paidAmount - grandTotal).coerceAtLeast(0.0)

    val totalItemCount: Int
        get() = cartItems.sumOf { it.quantity }
}

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val storePreferences = StorePreferences(application)
    val printerManager = BluetoothPrinterManager(application)
    val repository = PosRepository(application, database, storePreferences, printerManager)

    val storeProfile: StateFlow<StoreProfile> = repository.storeProfile
    val printerConnectionState: StateFlow<PrinterConnectionState> = printerManager.connectionState
    val lastPrintedSummary: StateFlow<String?> = printerManager.lastPrintedText

    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<String>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactionsWithItems: StateFlow<List<TransactionWithItems>> = repository.allTransactionsWithItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedReportPeriod = MutableStateFlow(ReportPeriod.DAILY)
    val selectedReportPeriod: StateFlow<ReportPeriod> = _selectedReportPeriod.asStateFlow()

    private val _selectedReportOffset = MutableStateFlow(0)
    val selectedReportOffset: StateFlow<Int> = _selectedReportOffset.asStateFlow()

    val salesAnalyticsReport: StateFlow<SalesAnalyticsReport> = combine(
        allTransactionsWithItems,
        _selectedReportPeriod,
        _selectedReportOffset,
        allProducts
    ) { txList, period, offset, products ->
        SalesReportCalculator.calculateReport(
            transactionsWithItems = txList,
            period = period,
            dateOffset = offset,
            products = products
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SalesReportCalculator.calculateReport(emptyList(), ReportPeriod.DAILY, 0, emptyList())
    )

    fun setReportPeriod(period: ReportPeriod) {
        _selectedReportPeriod.value = period
        _selectedReportOffset.value = 0
    }

    fun navigateReportPeriod(delta: Int) {
        _selectedReportOffset.value = _selectedReportOffset.value + delta
    }

    fun resetReportPeriodOffset() {
        _selectedReportOffset.value = 0
    }

    fun printThermalSalesReport(report: SalesAnalyticsReport) {
        viewModelScope.launch {
            val text = SalesReportCalculator.buildThermalSalesReportText(report, storeProfile.value)
            val bytes = SalesReportCalculator.buildThermalSalesReportBytes(report, storeProfile.value)
            val result = printerManager.print(bytes, text)
            if (result.isSuccess && result.getOrDefault(false)) {
                showSnackbar("Laporan penjualan ${report.period.titleId.lowercase()} berhasil dicetak ke printer!")
            } else {
                showSnackbar("Gagal mencetak laporan ke printer thermal. Pastikan printer terhubung.")
            }
        }
    }

    fun shareSalesReportText(report: SalesAnalyticsReport) {
        val text = SalesReportCalculator.buildThermalSalesReportText(report, storeProfile.value)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Penjualan - ${report.periodLabel}")
            type = "text/plain"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val shareIntent = Intent.createChooser(sendIntent, "Bagikan Laporan Penjualan").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        getApplication<Application>().startActivity(shareIntent)
    }

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = _pairedDevices.asStateFlow()

    private val _showOnboarding = MutableStateFlow(!storePreferences.isOnboardingCompleted())
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()

    fun dismissOnboarding() {
        storePreferences.setOnboardingCompleted(true)
        _showOnboarding.value = false
    }

    fun openOnboardingGuide() {
        _showOnboarding.value = true
    }

    val isScanningBluetooth: StateFlow<Boolean> = printerManager.scanner.isScanning
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = printerManager.scanner.discoveredDevices
    val scanStatusMessage: StateFlow<String?> = printerManager.scanner.scanStatusMessage

    private val _selectedScanMode = MutableStateFlow(ScanMode.DUAL_ALL)
    val selectedScanMode: StateFlow<ScanMode> = _selectedScanMode.asStateFlow()

    private val _filterPrintersOnly = MutableStateFlow(false)
    val filterPrintersOnly: StateFlow<Boolean> = _filterPrintersOnly.asStateFlow()

    val filteredProducts: StateFlow<List<Product>> = combine(
        allProducts,
        _uiState
    ) { products, state ->
        products.filter { product ->
            val matchesCategory = state.selectedCategory == "Semua" || product.category.equals(state.selectedCategory, ignoreCase = true)
            val matchesSearch = state.searchQuery.isBlank() ||
                    product.name.contains(state.searchQuery, ignoreCase = true) ||
                    product.sku.contains(state.searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today's Sales Calculation
    val todaySummary = allTransactions.combine(_uiState) { transactions, _ ->
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val todayTx = transactions.filter { it.timestamp >= startOfDay && it.status == "SELESAI" }
        val totalRevenue = todayTx.sumOf { it.grandTotal }
        val count = todayTx.size
        Triple(totalRevenue, count, if (count > 0) totalRevenue / count else 0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(0.0, 0, 0.0))

    private val _localBackupFiles = MutableStateFlow<List<LocalBackupFileInfo>>(emptyList())
    val localBackupFiles: StateFlow<List<LocalBackupFileInfo>> = _localBackupFiles.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDatabaseInitialized()
            refreshLocalBackups()
        }
        refreshPairedDevices()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onCategorySelected(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    // Cart Operations
    fun addToCart(product: Product) {
        val currentItems = _uiState.value.cartItems.toMutableList()
        val existingIndex = currentItems.indexOfFirst {
            it.product.id == product.id && it.customPrice == null && it.discount == 0.0
        }

        if (existingIndex >= 0) {
            val item = currentItems[existingIndex]
            currentItems[existingIndex] = item.copy(quantity = item.quantity + 1)
        } else {
            currentItems.add(CartItem(product = product, quantity = 1))
        }

        _uiState.value = _uiState.value.copy(cartItems = currentItems)
    }

    fun updateCartItemQuantity(index: Int, newQty: Int) {
        if (index !in _uiState.value.cartItems.indices) return
        val currentItems = _uiState.value.cartItems.toMutableList()
        if (newQty <= 0) {
            currentItems.removeAt(index)
        } else {
            currentItems[index] = currentItems[index].copy(quantity = newQty)
        }
        _uiState.value = _uiState.value.copy(cartItems = currentItems)
    }

    /**
     * Rubah Harga (Price override) & Diskon untuk item di keranjang.
     * Kasir dapat memasukkan harga khusus untuk transaksi ini.
     */
    fun updateCartItemPriceAndDetails(
        index: Int,
        customPrice: Double?,
        discount: Double,
        quantity: Int,
        note: String,
        savePriceToCatalog: Boolean = false
    ) {
        if (index !in _uiState.value.cartItems.indices) return
        val currentItems = _uiState.value.cartItems.toMutableList()
        val item = currentItems[index]
        val finalUnitPrice = customPrice ?: item.product.sellingPrice

        if (savePriceToCatalog && item.product.id > 0 && customPrice != null) {
            viewModelScope.launch {
                repository.updateProductPrice(item.product.id, customPrice)
            }
            val updatedProduct = item.product.copy(sellingPrice = customPrice)
            currentItems[index] = item.copy(
                product = updatedProduct,
                customPrice = null,
                discount = discount,
                quantity = quantity.coerceAtLeast(1),
                note = note
            )
            _uiState.value = _uiState.value.copy(cartItems = currentItems)
            showSnackbar("Harga katalog & keranjang disimpan: Rp ${finalUnitPrice.toLong()}")
        } else {
            currentItems[index] = item.copy(
                customPrice = customPrice,
                discount = discount,
                quantity = quantity.coerceAtLeast(1),
                note = note
            )
            _uiState.value = _uiState.value.copy(cartItems = currentItems)
            showSnackbar("Harga item diperbarui: Rp ${finalUnitPrice.toLong()}")
        }
    }

    fun removeCartItem(index: Int) {
        if (index in _uiState.value.cartItems.indices) {
            val currentItems = _uiState.value.cartItems.toMutableList()
            currentItems.removeAt(index)
            _uiState.value = _uiState.value.copy(cartItems = currentItems)
        }
    }

    fun clearCart() {
        _uiState.value = _uiState.value.copy(
            cartItems = emptyList(),
            overallDiscount = 0.0,
            customerName = "",
            paidAmountText = "",
            notes = ""
        )
    }

    fun addCustomItem(
        name: String,
        price: Double,
        quantity: Int = 1,
        saveToCatalog: Boolean = true,
        category: String = "Umum",
        sku: String = ""
    ) {
        viewModelScope.launch {
            val cleanName = if (name.isBlank()) "Item Bebas" else name.trim()
            val cleanCategory = if (category.isBlank()) "Umum" else category.trim()
            val cleanSku = if (sku.isNotBlank()) sku.trim() else "ITM-${System.currentTimeMillis() % 10000}"

            val product = if (saveToCatalog) {
                val toInsert = Product(
                    name = cleanName,
                    sku = cleanSku,
                    category = cleanCategory,
                    sellingPrice = price,
                    stock = 100,
                    unit = "pcs"
                )
                val insertedId = repository.addProduct(toInsert)
                toInsert.copy(id = insertedId)
            } else {
                Product(
                    id = -System.currentTimeMillis(),
                    name = cleanName,
                    category = "Kustom",
                    sellingPrice = price,
                    stock = 999,
                    unit = "pcs"
                )
            }

            val newItem = CartItem(
                product = product,
                quantity = quantity.coerceAtLeast(1),
                customPrice = if (saveToCatalog) null else price
            )
            _uiState.value = _uiState.value.copy(
                cartItems = _uiState.value.cartItems + newItem
            )
            showSnackbar(
                if (saveToCatalog) {
                    "Item '$cleanName' disimpan ke database & ditambah ke keranjang"
                } else {
                    "Item kustom ditambahkan ke keranjang"
                }
            )
        }
    }

    fun setOverallDiscount(amount: Double) {
        _uiState.value = _uiState.value.copy(overallDiscount = amount.coerceAtLeast(0.0))
    }

    fun setTaxRatePercent(percent: Double) {
        _uiState.value = _uiState.value.copy(taxRatePercent = percent.coerceAtLeast(0.0))
    }

    fun setCustomerName(name: String) {
        _uiState.value = _uiState.value.copy(customerName = name)
    }

    fun setPaymentMethod(method: String) {
        _uiState.value = _uiState.value.copy(paymentMethod = method)
    }

    fun setPaidAmountText(amount: String) {
        _uiState.value = _uiState.value.copy(paidAmountText = amount)
    }

    fun setNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun setCartBottomSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCartBottomSheet = show)
    }

    fun setBarcodeScannerVisible(show: Boolean) {
        _uiState.value = _uiState.value.copy(showBarcodeScanner = show)
    }

    /**
     * Pindai barcode menggunakan kamera atau input manual dan langsung masukkan item ke keranjang.
     * Mengembalikan status apakah barcode ditemukan di katalog toko atau tidak.
     */
    fun scanBarcodeAndAddToCart(
        rawBarcode: String,
        onResult: (BarcodeScanResult) -> Unit = {}
    ) {
        val barcode = rawBarcode.trim()
        if (barcode.isBlank()) return
        viewModelScope.launch {
            // 1. Coba pencarian SKU persis
            var matchedProduct = repository.getProductBySku(barcode)

            // 2. Jika tidak ada SKU persis, cari di daftar produk berdasarkan kesamaan SKU atau nama
            if (matchedProduct == null) {
                val allProds = allProducts.value
                matchedProduct = allProds.firstOrNull { it.sku.equals(barcode, ignoreCase = true) }
                    ?: allProds.firstOrNull { it.sku.contains(barcode, ignoreCase = true) && it.sku.isNotBlank() }
                    ?: allProds.firstOrNull { it.name.equals(barcode, ignoreCase = true) }
            }

            if (matchedProduct != null) {
                addToCart(matchedProduct)
                val currentQty = _uiState.value.cartItems.find { it.product.id == matchedProduct.id }?.quantity ?: 1
                _uiState.value = _uiState.value.copy(
                    lastScannedBarcode = barcode,
                    lastScannedProduct = matchedProduct
                )
                onResult(BarcodeScanResult.Success(matchedProduct, currentQty))
            } else {
                _uiState.value = _uiState.value.copy(
                    lastScannedBarcode = barcode,
                    lastScannedProduct = null
                )
                onResult(BarcodeScanResult.NotFound(barcode))
            }
        }
    }

    // Checkout
    fun processCheckout(onSuccess: (TransactionWithItems) -> Unit) {
        val state = _uiState.value
        if (state.cartItems.isEmpty()) {
            showSnackbar("Keranjang belanja masih kosong!")
            return
        }

        val paid = state.paidAmountText.toDoubleOrNull() ?: state.grandTotal
        if (paid < state.grandTotal) {
            showSnackbar("Uang bayar kurang! Total: Rp ${state.grandTotal.toLong()}, Bayar: Rp ${paid.toLong()}")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingOut = true)
            val result = repository.checkout(
                cartItems = state.cartItems,
                customerName = state.customerName,
                cashierName = storeProfile.value.defaultCashier,
                paymentMethod = state.paymentMethod,
                discountAmount = state.overallDiscount,
                taxAmount = state.taxAmount,
                paidAmount = paid,
                notes = state.notes
            )

            _uiState.value = _uiState.value.copy(isCheckingOut = false)
            result.onSuccess { transactionWithItems ->
                _uiState.value = _uiState.value.copy(
                    lastCompletedTransaction = transactionWithItems,
                    printPreviewTransaction = transactionWithItems,
                    isDraftPrintPreview = false,
                    cartItems = emptyList(),
                    overallDiscount = 0.0,
                    customerName = "",
                    paidAmountText = "",
                    notes = "",
                    showCartBottomSheet = false
                )
                if (storeProfile.value.autoPrintOnCheckout) {
                    repository.printReceipt(transactionWithItems)
                }
                showSnackbar("Transaksi ${transactionWithItems.transaction.invoiceNumber} siap dicetak!")
                onSuccess(transactionWithItems)
            }.onFailure { e ->
                showSnackbar("Gagal checkout: ${e.message}")
            }
        }
    }

    // Thermal Printer Actions
    fun refreshPairedDevices() {
        _pairedDevices.value = printerManager.getPairedDevices()
    }

    fun setScanMode(mode: ScanMode) {
        _selectedScanMode.value = mode
    }

    fun setFilterPrintersOnly(printersOnly: Boolean) {
        _filterPrintersOnly.value = printersOnly
    }

    fun startBluetoothScan(mode: ScanMode = _selectedScanMode.value) {
        refreshPairedDevices()
        printerManager.startDiscoveryScan(mode)
    }

    fun stopBluetoothScan() {
        printerManager.stopDiscoveryScan()
        refreshPairedDevices()
    }

    fun pairBluetoothDevice(address: String) {
        val result = printerManager.pairDevice(address)
        result.onSuccess {
            showSnackbar("Permintaan pairing Bluetooth dikirim...")
        }.onFailure { e ->
            showSnackbar("Gagal pairing: ${e.message}")
        }
    }

    fun connectPrinter(deviceAddress: String) {
        viewModelScope.launch {
            showSnackbar("Menyambungkan ke printer...")
            val result = printerManager.connect(deviceAddress)
            result.onSuccess {
                showSnackbar("Printer berhasil terhubung!")
                refreshPairedDevices()
            }.onFailure { e ->
                showSnackbar("Gagal terhubung: ${e.message}")
            }
        }
    }

    fun disconnectPrinter() {
        printerManager.disconnect()
        refreshPairedDevices()
        showSnackbar("Printer terputus")
    }

    fun printCurrentReceipt(transactionWithItems: TransactionWithItems) {
        viewModelScope.launch {
            val result = repository.printReceipt(transactionWithItems)
            result.onSuccess {
                showSnackbar("Struk ${transactionWithItems.transaction.invoiceNumber} berhasil dicetak!")
            }.onFailure { e ->
                showSnackbar("Printer tidak terhubung: ${e.localizedMessage}")
            }
        }
    }

    fun testPrint() {
        viewModelScope.launch {
            val result = repository.testPrint()
            result.onSuccess {
                showSnackbar("Test cetak struk berhasil!")
            }.onFailure { e ->
                showSnackbar("Gagal mencetak: ${e.localizedMessage}")
            }
        }
    }

    fun shareReceipt(transactionWithItems: TransactionWithItems) {
        repository.shareReceiptAsText(transactionWithItems)
    }

    fun openThermalPrintPreview(transactionWithItems: TransactionWithItems, isDraft: Boolean = false) {
        refreshPairedDevices()
        _uiState.value = _uiState.value.copy(
            printPreviewTransaction = transactionWithItems,
            isDraftPrintPreview = isDraft
        )
    }

    fun openLiveCartPrintPreview(
        overrideCustomerName: String? = null,
        overridePaymentMethod: String? = null,
        overrideDiscount: Double? = null,
        overrideTaxRate: Double? = null,
        overridePaidAmount: Double? = null,
        overrideNotes: String? = null
    ) {
        refreshPairedDevices()
        val state = _uiState.value
        val profile = storeProfile.value
        val cart = state.cartItems

        val txItems = if (cart.isNotEmpty()) {
            cart.mapIndexed { idx, cartItem ->
                TransactionItem(
                    id = (idx + 1).toLong(),
                    transactionId = 0L,
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    originalPrice = cartItem.product.sellingPrice,
                    actualPrice = cartItem.effectiveUnitPrice,
                    quantity = cartItem.quantity,
                    subtotal = cartItem.subtotal,
                    note = cartItem.note
                )
            }
        } else {
            listOf(
                TransactionItem(
                    id = 1L,
                    transactionId = 0L,
                    productId = 1L,
                    productName = "Kopi Susu Gula Aren",
                    originalPrice = 18000.0,
                    actualPrice = 18000.0,
                    quantity = 2,
                    subtotal = 36000.0,
                    note = "Less Ice"
                ),
                TransactionItem(
                    id = 2L,
                    transactionId = 0L,
                    productId = 2L,
                    productName = "Roti Bakar Coklat Keju",
                    originalPrice = 16000.0,
                    actualPrice = 15000.0,
                    quantity = 1,
                    subtotal = 15000.0,
                    note = "Harga Promo"
                )
            )
        }

        val subtotal = txItems.sumOf { it.subtotal }
        val discount = overrideDiscount ?: state.overallDiscount
        val taxRate = overrideTaxRate ?: state.taxRatePercent
        val taxable = (subtotal - discount).coerceAtLeast(0.0)
        val taxAmount = if (taxRate > 0) taxable * (taxRate / 100.0) else 0.0
        val grandTotal = (taxable + taxAmount).coerceAtLeast(0.0)
        val paid = overridePaidAmount ?: (state.paidAmountText.toDoubleOrNull()?.takeIf { it >= grandTotal } ?: grandTotal)
        val change = (paid - grandTotal).coerceAtLeast(0.0)

        val draftTx = Transaction(
            id = 0L,
            invoiceNumber = if (cart.isNotEmpty()) "DRAFT-POS-${System.currentTimeMillis() % 10000}" else "SAMPLE-PREVIEW-001",
            timestamp = System.currentTimeMillis(),
            customerName = (overrideCustomerName ?: state.customerName).ifBlank { "Pelanggan Umum" },
            cashierName = profile.defaultCashier.ifBlank { "Kasir Utama" },
            subtotal = subtotal,
            discountAmount = discount,
            taxAmount = taxAmount,
            grandTotal = grandTotal,
            paymentMethod = (overridePaymentMethod ?: state.paymentMethod).ifBlank { "Tunai" },
            paidAmount = paid,
            changeAmount = change,
            notes = overrideNotes ?: state.notes
        )

        _uiState.value = _uiState.value.copy(
            printPreviewTransaction = TransactionWithItems(transaction = draftTx, items = txItems),
            isDraftPrintPreview = true
        )
    }

    fun closeThermalPrintPreview() {
        _uiState.value = _uiState.value.copy(
            printPreviewTransaction = null,
            lastCompletedTransaction = null,
            isDraftPrintPreview = false
        )
    }

    fun updateStoreProfileSilently(profile: StoreProfile) {
        viewModelScope.launch {
            repository.updateStoreProfile(profile)
            _uiState.value = _uiState.value.copy(taxRatePercent = profile.defaultTaxPercent)
        }
    }

    fun triggerThermalPrintFromPreview(
        transactionWithItems: TransactionWithItems,
        updatedProfile: StoreProfile,
        copyCount: Int = 1
    ) {
        viewModelScope.launch {
            repository.updateStoreProfile(updatedProfile)
            val safeCopies = copyCount.coerceIn(1, 5)
            var lastError: Throwable? = null
            for (i in 1..safeCopies) {
                val res = repository.printReceipt(transactionWithItems)
                if (res.isFailure) {
                    lastError = res.exceptionOrNull()
                    break
                }
            }
            if (lastError == null) {
                val copyLabel = if (safeCopies > 1) " ($safeCopies salinan)" else ""
                showSnackbar("Struk ${transactionWithItems.transaction.invoiceNumber}$copyLabel berhasil dikirim ke printer Bluetooth!")
            } else {
                showSnackbar("Printer Bluetooth belum terhubung: ${lastError.localizedMessage}")
            }
        }
    }

    // Product Management (Tambah Item, Rubah Harga Master, Hapus)
    fun saveProduct(
        id: Long = 0,
        name: String,
        sku: String,
        category: String,
        costPrice: Double,
        sellingPrice: Double,
        stock: Int,
        unit: String
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                showSnackbar("Nama item tidak boleh kosong!")
                return@launch
            }
            if (sellingPrice < 0) {
                showSnackbar("Harga jual tidak valid!")
                return@launch
            }

            if (id == 0L) {
                val newProduct = Product(
                    name = name.trim(),
                    sku = if (sku.isBlank()) "ITM-${System.currentTimeMillis() % 10000}" else sku.trim(),
                    category = if (category.isBlank()) "Umum" else category.trim(),
                    costPrice = costPrice,
                    sellingPrice = sellingPrice,
                    stock = stock,
                    unit = if (unit.isBlank()) "pcs" else unit.trim()
                )
                repository.addProduct(newProduct)
                showSnackbar("Item '${newProduct.name}' berhasil ditambahkan")
            } else {
                val updatedProduct = Product(
                    id = id,
                    name = name.trim(),
                    sku = sku.trim(),
                    category = if (category.isBlank()) "Umum" else category.trim(),
                    costPrice = costPrice,
                    sellingPrice = sellingPrice,
                    stock = stock,
                    unit = if (unit.isBlank()) "pcs" else unit.trim()
                )
                repository.updateProduct(updatedProduct)
                showSnackbar("Item '${updatedProduct.name}' berhasil diperbarui")
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            showSnackbar("Item '${product.name}' dihapus")
        }
    }

    fun deleteProducts(productIds: List<Long>, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val count = repository.deleteProducts(productIds)
            showSnackbar("$count produk terpilih berhasil dihapus")
            onComplete?.invoke()
        }
    }

    fun deleteProductsByCategory(category: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val count = repository.deleteProductsByCategory(category)
            showSnackbar("$count produk dalam kategori '$category' berhasil dihapus")
            onComplete?.invoke()
        }
    }

    fun deleteAllProducts(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteAllProducts()
            showSnackbar("Seluruh produk di katalog berhasil dihapus")
            onComplete?.invoke()
        }
    }

    fun selectHistoryTransaction(transactionId: Long) {
        viewModelScope.launch {
            val details = repository.getTransactionDetails(transactionId)
            _uiState.value = _uiState.value.copy(selectedHistoryTransaction = details)
        }
    }

    fun dismissHistoryDetail() {
        _uiState.value = _uiState.value.copy(selectedHistoryTransaction = null)
    }

    fun dismissLastTransactionDialog() {
        _uiState.value = _uiState.value.copy(
            lastCompletedTransaction = null,
            printPreviewTransaction = null,
            isDraftPrintPreview = false
        )
    }

    fun restoreSampleProducts() {
        viewModelScope.launch {
            repository.restoreSampleProducts()
            showSnackbar("Daftar contoh produk toko berhasil dimuat ke database!")
        }
    }

    fun updateStoreProfile(profile: StoreProfile, message: String = "Pengaturan toko & layout nota tersimpan!") {
        viewModelScope.launch {
            repository.updateStoreProfile(profile)
            _uiState.value = _uiState.value.copy(taxRatePercent = profile.defaultTaxPercent)
            showSnackbar(message)
        }
    }

    fun setAppLanguage(languageCode: String, languageName: String) {
        val updated = storeProfile.value.copy(languageCode = languageCode)
        updateStoreProfile(updated, "Bahasa diubah ke: $languageName")
    }

    fun setAppTheme(themeCode: String, themeTitle: String) {
        val updated = storeProfile.value.copy(themeOption = themeCode)
        updateStoreProfile(updated, "Tema aplikasi diubah ke: $themeTitle")
    }

    fun saveCustomLogoFromUri(uri: android.net.Uri) {
        viewModelScope.launch {
            val savedPath = com.example.printer.LogoBitmapProcessor.saveCustomLogoFromUri(getApplication(), uri)
            if (savedPath != null) {
                val updated = storeProfile.value.copy(
                    showLogo = true,
                    logoPresetType = com.example.printer.PresetLogoType.CUSTOM_IMAGE.code,
                    customLogoPath = savedPath
                )
                repository.updateStoreProfile(updated)
                showSnackbar("Logo kustom berhasil diunggah & dipasang pada nota!")
            } else {
                showSnackbar("Gagal memuat gambar logo.")
            }
        }
    }

    fun resetReceiptLayout() {
        val current = storeProfile.value
        val resetProfile = current.copy(
            paperWidth = 58,
            autoCut = true,
            feedLines = 3,
            showLogo = true,
            logoPresetType = "STORE_MART",
            logoSizePx = 184,
            logoAlignment = "CENTER",
            headerAlignment = "CENTER",
            titleFontSize = "DOUBLE",
            dividerStyle = "DASH",
            customHeaderNote = "",
            showSlogan = true,
            showAddressPhone = true,
            showInvoiceNumber = true,
            showDateTime = true,
            showCashierName = true,
            showCustomerName = true,
            showItemNumber = false,
            showTaxAndDiscount = true,
            showSocialMedia = true,
            showBarcode = true,
            showSignatureBox = false
        )
        updateStoreProfile(resetProfile, "Layout nota dikembalikan ke pengaturan awal!")
    }

    fun exportSalesReportCsv() {
        viewModelScope.launch {
            repository.shareSalesReportCsv()
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.clearTransactionHistory()
            showSnackbar("Seluruh riwayat transaksi telah dibersihkan.")
        }
    }

    // Room Database Backup & Restore (Export / Import)
    fun refreshLocalBackups() {
        viewModelScope.launch {
            _localBackupFiles.value = repository.getLocalBackupFiles()
        }
    }

    fun exportDatabaseBackupToLocal(customFileName: String? = null) {
        viewModelScope.launch {
            val result = repository.exportDatabaseToLocalBackup(customFileName)
            result.onSuccess { info ->
                refreshLocalBackups()
                showSnackbar("Backup lokal disimpan: ${info.fileName} (${info.productCount} item, ${info.transactionCount} transaksi)")
            }.onFailure { e ->
                showSnackbar("Gagal membuat backup lokal: ${e.message}")
            }
        }
    }

    fun exportDatabaseBackupToUri(uri: android.net.Uri) {
        viewModelScope.launch {
            val result = repository.exportDatabaseToUri(uri)
            result.onSuccess { info ->
                refreshLocalBackups()
                showSnackbar("Database berhasil diekspor ke file (${info.productCount} item, ${info.transactionCount} transaksi)!")
            }.onFailure { e ->
                showSnackbar("Gagal mengekspor database: ${e.message}")
            }
        }
    }

    fun importDatabaseBackupFromLocalFile(
        filePath: String,
        replaceExisting: Boolean,
        restoreStoreSettings: Boolean
    ) {
        viewModelScope.launch {
            val result = repository.importDatabaseFromLocalFile(
                filePath = filePath,
                replaceExisting = replaceExisting,
                restoreStoreSettings = restoreStoreSettings
            )
            result.onSuccess { summary ->
                refreshLocalBackups()
                showSnackbar(
                    "Impor berhasil: ${summary.productsImported} item & ${summary.transactionsImported} transaksi dipulihkan!"
                )
            }.onFailure { e ->
                showSnackbar("Gagal mengimpor backup: ${e.message}")
            }
        }
    }

    fun importDatabaseBackupFromUri(
        uri: android.net.Uri,
        replaceExisting: Boolean,
        restoreStoreSettings: Boolean
    ) {
        viewModelScope.launch {
            val result = repository.importDatabaseFromUri(
                uri = uri,
                replaceExisting = replaceExisting,
                restoreStoreSettings = restoreStoreSettings
            )
            result.onSuccess { summary ->
                refreshLocalBackups()
                showSnackbar(
                    "Impor file berhasil: ${summary.productsImported} item & ${summary.transactionsImported} transaksi dipulihkan!"
                )
            }.onFailure { e ->
                showSnackbar("Gagal membaca file backup: ${e.message}")
            }
        }
    }

    fun deleteLocalBackup(filePath: String) {
        viewModelScope.launch {
            val deleted = repository.deleteLocalBackupFile(filePath)
            refreshLocalBackups()
            if (deleted) {
                showSnackbar("File backup lokal berhasil dihapus.")
            } else {
                showSnackbar("Gagal menghapus file backup.")
            }
        }
    }

    fun shareBackupJson(filePath: String? = null) {
        viewModelScope.launch {
            repository.shareBackupJson(filePath)
        }
    }

    fun showSnackbar(msg: String) {
        _uiState.value = _uiState.value.copy(snackbarMessage = msg)
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        printerManager.stopDiscoveryScan()
    }
}
