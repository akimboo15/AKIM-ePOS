package com.example.ui.screens.cashier

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.formatRupiah
import com.example.ui.common.ReceiptPreviewDialog
import com.example.ui.localization.tr
import com.example.ui.localization.trDynamic
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierScreen(
    viewModel: PosViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val storeProfile by viewModel.storeProfile.collectAsStateWithLifecycle()
    val printerState by viewModel.printerConnectionState.collectAsStateWithLifecycle()

    var showPriceEditDialogForIndex by remember { mutableStateOf<Int?>(null) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showCustomItemDialog by remember { mutableStateOf(false) }
    var showBarcodeScannerSheet by remember { mutableStateOf(false) }
    var customItemInitialBarcode by remember { mutableStateOf("") }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTabletOrWide = maxWidth >= 600.dp
        val isExpandedTablet = maxWidth >= 840.dp

        if (isTabletOrWide) {
            // Canonical Split-Pane POS Register Layout for Tablets & Foldables
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Pane: Search, Categories & Product Grid
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    CashierCatalogSection(
                        searchQuery = uiState.searchQuery,
                        selectedCategory = uiState.selectedCategory,
                        categories = categories,
                        products = products,
                        currencySymbol = storeProfile.currencySymbol,
                        minGridCellWidth = if (isExpandedTablet) 170.dp else 155.dp,
                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        onCategorySelected = { viewModel.onCategorySelected(it) },
                        onOpenCustomItemDialog = {
                            customItemInitialBarcode = ""
                            showCustomItemDialog = true
                        },
                        onOpenBarcodeScanner = { showBarcodeScannerSheet = true },
                        onAddToCart = { viewModel.addToCart(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                // Right Supporting Pane: Live POS Cart & Checkout Panel
                TabletCartSupportingPane(
                    uiState = uiState,
                    currencySymbol = storeProfile.currencySymbol,
                    onUpdateQuantity = { idx, qty -> viewModel.updateCartItemQuantity(idx, qty) },
                    onEditPrice = { idx -> showPriceEditDialogForIndex = idx },
                    onRemoveItem = { idx -> viewModel.removeCartItem(idx) },
                    onClearCart = { viewModel.clearCart() },
                    onOpenCartSheet = { viewModel.openLiveCartPrintPreview() },
                    onOpenCustomItem = {
                        customItemInitialBarcode = ""
                        showCustomItemDialog = true
                    },
                    onCheckout = { showCheckoutDialog = true },
                    modifier = Modifier
                        .width(if (isExpandedTablet) 380.dp else 320.dp)
                        .fillMaxHeight()
                        .testTag("tablet_cart_pane")
                )
            }
        } else {
            // Compact Smartphone Layout: Product Grid + Sticky Bottom Cart Bar
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (uiState.cartItems.isNotEmpty()) 80.dp else 0.dp)
                ) {
                    CashierCatalogSection(
                        searchQuery = uiState.searchQuery,
                        selectedCategory = uiState.selectedCategory,
                        categories = categories,
                        products = products,
                        currencySymbol = storeProfile.currencySymbol,
                        minGridCellWidth = 150.dp,
                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        onCategorySelected = { viewModel.onCategorySelected(it) },
                        onOpenCustomItemDialog = {
                            customItemInitialBarcode = ""
                            showCustomItemDialog = true
                        },
                        onOpenBarcodeScanner = { showBarcodeScannerSheet = true },
                        onAddToCart = { viewModel.addToCart(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Sticky Bottom Bar for Cart Checkout on Compact Screen
                if (uiState.cartItems.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable { viewModel.setCartBottomSheet(true) }
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text("${uiState.totalItemCount}")
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = tr("Keranjang", "Cart"),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = tr("Total", "Total") + " (${uiState.totalItemCount} " + tr("item", "items") + ")",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatRupiah(uiState.grandTotal, storeProfile.currencySymbol),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.setCartBottomSheet(true) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("view_cart_button")
                                ) {
                                    Text(tr("Rincian", "Details"))
                                }

                                Button(
                                    onClick = { showCheckoutDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("checkout_button")
                                ) {
                                    Text(tr("Bayar (POS)", "Pay (POS)"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Full Cart View with "Rubah Harga" & Quantity adjustment
    if (uiState.showCartBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setCartBottomSheet(false) },
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Keranjang Penjualan", "Sales Cart") + " (${uiState.totalItemCount} " + tr("item", "items") + ")",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { viewModel.clearCart() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(tr("Kosongkan", "Clear"), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cart Item List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.cartItems.forEachIndexed { index, cartItem ->
                        CartItemRow(
                            cartItem = cartItem,
                            currencySymbol = storeProfile.currencySymbol,
                            onQuantityChange = { newQty -> viewModel.updateCartItemQuantity(index, newQty) },
                            onEditPrice = { showPriceEditDialogForIndex = index },
                            onDelete = { viewModel.removeCartItem(index) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Subtotal and Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = tr("Subtotal Belanja:", "Shopping Subtotal:"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatRupiah(uiState.subtotal, storeProfile.currencySymbol),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.setCartBottomSheet(false)
                        viewModel.openLiveCartPrintPreview()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("cart_sheet_preview_receipt_button")
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr("Pratinjau Visual Struk Real-Time", "Real-Time Visual Receipt Preview"), fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.setCartBottomSheet(false)
                        showCheckoutDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("proceed_to_payment_button")
                ) {
                    Text(
                        text = tr("Lanjut Pembayaran", "Proceed to Payment") + " • ${formatRupiah(uiState.grandTotal, storeProfile.currencySymbol)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Rubah Harga Dialog
    showPriceEditDialogForIndex?.let { index ->
        val cartItem = uiState.cartItems.getOrNull(index)
        if (cartItem != null) {
            PriceEditDialog(
                cartItem = cartItem,
                onConfirm = { customPrice, discount, quantity, note, savePriceToCatalog ->
                    viewModel.updateCartItemPriceAndDetails(
                        index = index,
                        customPrice = customPrice,
                        discount = discount,
                        quantity = quantity,
                        note = note,
                        savePriceToCatalog = savePriceToCatalog
                    )
                    showPriceEditDialogForIndex = null
                },
                onDismiss = { showPriceEditDialogForIndex = null }
            )
        }
    }

    // Checkout Dialog
    if (showCheckoutDialog) {
        CheckoutDialog(
            subtotal = uiState.subtotal,
            initialDiscount = uiState.overallDiscount,
            initialTaxRate = uiState.taxRatePercent,
            initialCustomerName = uiState.customerName,
            initialPaymentMethod = uiState.paymentMethod,
            onConfirm = { customerName, paymentMethod, discount, taxRate, paidAmount, notes ->
                viewModel.setCustomerName(customerName)
                viewModel.setPaymentMethod(paymentMethod)
                viewModel.setOverallDiscount(discount)
                viewModel.setTaxRatePercent(taxRate)
                viewModel.setPaidAmountText(paidAmount.toLong().toString())
                viewModel.setNotes(notes)

                viewModel.processCheckout {
                    showCheckoutDialog = false
                }
            },
            onDismiss = { showCheckoutDialog = false },
            onPreviewReceipt = { customerName, paymentMethod, discount, taxRate, paidAmount, notes ->
                viewModel.setCustomerName(customerName)
                viewModel.setPaymentMethod(paymentMethod)
                viewModel.setOverallDiscount(discount)
                viewModel.setTaxRatePercent(taxRate)
                viewModel.setPaidAmountText(paidAmount.toLong().toString())
                viewModel.setNotes(notes)
                showCheckoutDialog = false
                viewModel.openLiveCartPrintPreview(
                    overrideCustomerName = customerName,
                    overridePaymentMethod = paymentMethod,
                    overrideDiscount = discount,
                    overrideTaxRate = taxRate,
                    overridePaidAmount = paidAmount,
                    overrideNotes = notes
                )
            }
        )
    }

    // Custom Item Dialog
    if (showCustomItemDialog) {
        CustomItemDialog(
            initialBarcode = customItemInitialBarcode,
            onConfirm = { name, price, qty, saveToCatalog, category, sku ->
                viewModel.addCustomItem(
                    name = name,
                    price = price,
                    quantity = qty,
                    saveToCatalog = saveToCatalog,
                    category = category,
                    sku = sku
                )
                customItemInitialBarcode = ""
                showCustomItemDialog = false
            },
            onDismiss = {
                customItemInitialBarcode = ""
                showCustomItemDialog = false
            }
        )
    }

    // Barcode & QR Scanner Camera Bottom Sheet
    if (showBarcodeScannerSheet || uiState.showBarcodeScanner) {
        BarcodeScannerSheet(
            viewModel = viewModel,
            currencySymbol = storeProfile.currencySymbol,
            onDismiss = {
                showBarcodeScannerSheet = false
                viewModel.setBarcodeScannerVisible(false)
            },
            onOpenCustomItem = { barcode ->
                showBarcodeScannerSheet = false
                viewModel.setBarcodeScannerVisible(false)
                customItemInitialBarcode = barcode
                showCustomItemDialog = true
            }
        )
    }

    // Fallback Receipt Preview Dialog if full-screen preview is not active
    if (uiState.printPreviewTransaction == null) {
        uiState.lastCompletedTransaction?.let { txWithItems ->
            ReceiptPreviewDialog(
                transactionWithItems = txWithItems,
                storeProfile = storeProfile,
                printerState = printerState,
                onPrint = { viewModel.printCurrentReceipt(txWithItems) },
                onShare = { viewModel.shareReceipt(txWithItems) },
                onDismiss = { viewModel.dismissLastTransactionDialog() }
            )
        }
    }
}

@Composable
private fun CashierCatalogSection(
    searchQuery: String,
    selectedCategory: String,
    categories: List<String>,
    products: List<Product>,
    currencySymbol: String,
    minGridCellWidth: androidx.compose.ui.unit.Dp,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onOpenCustomItemDialog: () -> Unit,
    onOpenBarcodeScanner: () -> Unit,
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Search & Quick Action Header with Barcode Scanner & Custom Item
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text(tr("Cari produk atau barcode...", "Search product or barcode...")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = tr("Cari", "Search")) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = tr("Hapus", "Clear"))
                            }
                        }
                        IconButton(
                            onClick = onOpenBarcodeScanner,
                            modifier = Modifier.testTag("search_barcode_scanner_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = tr("Pindai Barcode", "Scan Barcode"),
                                tint = AccentEmerald
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_product_input")
            )

            // Primary Scan Barcode Button
            Button(
                onClick = onOpenBarcodeScanner,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("scan_barcode_header_button")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = tr("Pindai Barcode", "Scan Barcode"),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = tr("Scan", "Scan"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quick Custom Ad-hoc Item Button
            OutlinedButton(
                onClick = onOpenCustomItemDialog,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("add_custom_item_button")
            ) {
                Icon(
                    Icons.Default.PostAdd,
                    contentDescription = tr("Item Bebas", "Custom Item"),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(tr("Item Bebas", "Custom Item"), fontSize = 12.sp)
            }
        }

        // Categories horizontal bar
        val categoryList = listOf("Semua") + categories
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categoryList) { category ->
                FilterChip(
                    selected = selectedCategory.equals(category, ignoreCase = true),
                    onClick = { onCategorySelected(category) },
                    label = { Text(trDynamic(category)) },
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        // Product Grid
        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tr("Tidak ada produk yang cocok", "No matching products found"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onOpenCustomItemDialog) {
                        Text(tr("Tambah Item Bebas", "Add Custom Item"))
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = minGridCellWidth),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(products, key = { it.id }) { product ->
                    ProductPosCard(
                        product = product,
                        currencySymbol = currencySymbol,
                        onAddToCart = { onAddToCart(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabletCartSupportingPane(
    uiState: PosUiState,
    currencySymbol: String,
    onUpdateQuantity: (Int, Int) -> Unit,
    onEditPrice: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onClearCart: () -> Unit,
    onOpenCartSheet: () -> Unit,
    onOpenCustomItem: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BadgedBox(
                        badge = {
                            if (uiState.totalItemCount > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text("${uiState.totalItemCount}")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = tr("Keranjang Kasir", "Cashier Cart"),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = tr("Keranjang Kasir", "Cashier Cart"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${uiState.totalItemCount} " + tr("item dipilih", "items selected"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (uiState.cartItems.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearCart,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(tr("Kosongkan", "Clear"), fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            // Cart Content or Empty State
            if (uiState.cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = tr("Keranjang Belanja Kosong", "Shopping Cart is Empty"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tr("Ketuk produk di katalog sebelah kiri untuk menambahkan ke nota transaksi.", "Tap products on the left catalog to add them to the receipt."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onOpenCustomItem) {
                            Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Tambah Item Bebas", "Add Custom Item"))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(uiState.cartItems) { index, cartItem ->
                        CartItemRow(
                            cartItem = cartItem,
                            currencySymbol = currencySymbol,
                            onQuantityChange = { newQty -> onUpdateQuantity(index, newQty) },
                            onEditPrice = { onEditPrice(index) },
                            onDelete = { onRemoveItem(index) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            // Totals Summary Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tr("Subtotal", "Subtotal") + " (${uiState.totalItemCount} " + tr("item", "items") + ")",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatRupiah(uiState.subtotal, currencySymbol),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (uiState.overallDiscount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tr("Diskon Nota", "Receipt Discount"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "-${formatRupiah(uiState.overallDiscount, currencySymbol)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (uiState.taxAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tr("Pajak / PPN", "Tax / VAT") + " (${uiState.taxRatePercent.toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatRupiah(uiState.taxAmount, currencySymbol),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr("Total Tagihan", "Grand Total"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatRupiah(uiState.grandTotal, currencySymbol),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenCartSheet,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("open_live_print_preview_button")
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr("Pratinjau", "Preview"))
                }

                Button(
                    onClick = onCheckout,
                    enabled = uiState.cartItems.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("checkout_button")
                ) {
                    Text(
                        text = tr("Bayar (POS)", "Pay (POS)"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ProductPosCard(
    product: Product,
    currencySymbol: String = "Rp",
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddToCart() }
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Category badge
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = product.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatRupiah(product.sellingPrice, currencySymbol),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${tr("Stok", "Stock")}: ${product.stock} ${trDynamic(product.unit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.stock > 10) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFDC2626)
                )

                FilledIconButton(
                    onClick = onAddToCart,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = tr("Tambah", "Add") + " ${product.name}",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    currencySymbol: String = "Rp",
    onQuantityChange: (Int) -> Unit,
    onEditPrice: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cartItem.product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = formatRupiah(cartItem.effectiveUnitPrice, currencySymbol),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (cartItem.isPriceOverridden) {
                            Surface(
                                color = AccentAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = tr("Harga Dirubah", "Price Changed"),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentAmber,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    if (cartItem.note.isNotBlank()) {
                        Text(
                            text = "*${cartItem.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Button to "Rubah Harga"
                OutlinedButton(
                    onClick = onEditPrice,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("edit_price_btn_${cartItem.product.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = tr("Rubah Harga", "Change Price"), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr("Ubah Harga", "Change Price"), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quantity stepper & Subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledIconButton(
                        onClick = { onQuantityChange(cartItem.quantity - 1) },
                        modifier = Modifier.size(28.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Icon(
                            if (cartItem.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                            contentDescription = tr("Hapus", "Delete"),
                            modifier = Modifier.size(16.dp),
                            tint = if (cartItem.quantity == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "${cartItem.quantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    FilledIconButton(
                        onClick = { onQuantityChange(cartItem.quantity + 1) },
                        modifier = Modifier.size(28.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = tr("Tambah", "Add"), modifier = Modifier.size(16.dp))
                    }
                }

                Text(
                    text = formatRupiah(cartItem.subtotal, currencySymbol),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
