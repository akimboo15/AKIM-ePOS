package com.example.ui.screens.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import com.example.ui.screens.cashier.BarcodeScannerSheet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Product
import com.example.data.model.formatRupiah
import com.example.ui.localization.tr
import com.example.ui.localization.trDynamic
import com.example.ui.viewmodel.PosViewModel

@Composable
fun ProductListScreen(
    viewModel: PosViewModel
) {
    val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val storeProfile by viewModel.storeProfile.collectAsStateWithLifecycle()

    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    // Multi-Selection (Hapus Pilih) State
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedProductIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }

    // Delete Options Menu & Dialogs
    var showDeleteMenu by remember { mutableStateOf(false) }
    var showDeleteByCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTabletOrWide = maxWidth >= 600.dp

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val totalStockCount = allProducts.sumOf { it.stock }
                    val totalAssetValue = allProducts.sumOf { it.sellingPrice * it.stock }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = tr("Total Katalog Item", "Total Catalog Items"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "${allProducts.size} " + tr("Produk", "Products"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if (isTabletOrWide) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = tr("Total Stok Tersedia", "Total Available Stock"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "$totalStockCount " + tr("Unit", "Units"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1.3f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = tr("Estimasi Nilai Stok", "Estimated Stock Value"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = formatRupiah(totalAssetValue, storeProfile.currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Search Bar + Actions (Delete Menu & Add Product)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text(tr("Cari nama produk, SKU, barcode...", "Search product name, SKU, barcode...")) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = tr("Cari", "Search")) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = tr("Hapus", "Clear"))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inventory_search_input")
                    )

                    // Delete Options Dropdown Menu Button
                    Box {
                        FilledTonalButton(
                            onClick = { showDeleteMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("catalog_delete_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = tr("Opsi Hapus", "Delete Options"),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr("Hapus ▾", "Delete ▾"),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        DropdownMenu(
                            expanded = showDeleteMenu,
                            onDismissRequest = { showDeleteMenu = false }
                        ) {
                            // 1. Hapus Pilih
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = tr("Hapus Pilih (Pilih Beberapa)", "Delete Selected (Pick Items)"),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = tr("Pilih beberapa produk dengan centang", "Select multiple items with checkboxes"),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    showDeleteMenu = false
                                    isSelectionMode = true
                                    selectedProductIds = emptySet()
                                },
                                modifier = Modifier.testTag("menu_action_delete_selected")
                            )

                            // 2. Hapus per Category
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Category,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = tr("Hapus per Kategori...", "Delete by Category..."),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = tr("Hapus seluruh produk dalam kategori tertentu", "Delete all items inside a chosen category"),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    showDeleteMenu = false
                                    showDeleteByCategoryDialog = true
                                },
                                modifier = Modifier.testTag("menu_action_delete_by_category")
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // 3. Hapus Semua
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = tr("Hapus Semua Produk...", "Delete All Products..."),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = tr("Kosongkan seluruh database katalog", "Clear entire catalog database"),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    showDeleteMenu = false
                                    showDeleteAllDialog = true
                                },
                                modifier = Modifier.testTag("menu_action_delete_all")
                            )
                        }
                    }

                    if (isTabletOrWide) {
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Tambah Produk", "Add Product"), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Selection Mode Contextual Action Bar
                if (isSelectionMode) {
                    val allVisibleSelected = products.isNotEmpty() && products.all { selectedProductIds.contains(it.id) }

                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("catalog_selection_action_bar")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = allVisibleSelected,
                                    onCheckedChange = { checked ->
                                        selectedProductIds = if (checked) {
                                            selectedProductIds + products.map { it.id }.toSet()
                                        } else {
                                            selectedProductIds - products.map { it.id }.toSet()
                                        }
                                    },
                                    modifier = Modifier.testTag("select_all_checkbox")
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${selectedProductIds.size} " + tr("dipilih", "selected"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (selectedProductIds.isNotEmpty()) {
                                            showDeleteSelectedDialog = true
                                        }
                                    },
                                    enabled = selectedProductIds.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("trigger_delete_selected_button")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tr("Hapus", "Delete") + " (${selectedProductIds.size})")
                                }

                                OutlinedButton(
                                    onClick = {
                                        isSelectionMode = false
                                        selectedProductIds = emptySet()
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(tr("Batal", "Cancel"))
                                }
                            }
                        }
                    }
                }

                // Category Bar
                val categoryList = listOf("Semua") + categories
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoryList) { category ->
                        FilterChip(
                            selected = uiState.selectedCategory.equals(category, ignoreCase = true),
                            onClick = { viewModel.onCategorySelected(category) },
                            label = { Text(trDynamic(category)) }
                        )
                    }
                }

                // Product List / Adaptive Grid
                if (products.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = tr("Belum ada produk ditemukan", "No products found yet"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { showAddDialog = true }) {
                                Text(tr("Tambah Item Baru", "Add New Item"))
                            }
                            if (allProducts.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.restoreSampleProducts() },
                                    modifier = Modifier.testTag("restore_sample_products_button")
                                ) {
                                    Text(tr("Muat Contoh Produk Toko", "Load Sample Store Products"))
                                }
                            }
                        }
                    }
                } else if (isTabletOrWide) {
                    // Multi-column Adaptive Grid on Tablet
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 280.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("tablet_product_catalog_grid")
                    ) {
                        items(products, key = { it.id }) { product ->
                            ProductItemCard(
                                product = product,
                                currencySymbol = storeProfile.currencySymbol,
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedProductIds.contains(product.id),
                                onToggleSelect = {
                                    selectedProductIds = if (selectedProductIds.contains(product.id)) {
                                        selectedProductIds - product.id
                                    } else {
                                        selectedProductIds + product.id
                                    }
                                },
                                onEdit = { productToEdit = product },
                                onDelete = { productToDelete = product }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(products, key = { it.id }) { product ->
                            ProductItemCard(
                                product = product,
                                currencySymbol = storeProfile.currencySymbol,
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedProductIds.contains(product.id),
                                onToggleSelect = {
                                    selectedProductIds = if (selectedProductIds.contains(product.id)) {
                                        selectedProductIds - product.id
                                    } else {
                                        selectedProductIds + product.id
                                    }
                                },
                                onEdit = { productToEdit = product },
                                onDelete = { productToDelete = product }
                            )
                        }
                    }
                }
            }

            // FAB to add item (hidden during selection mode to prevent clutter)
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .testTag("add_product_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = tr("Tambah Produk", "Add Product"), tint = Color.White)
                }
            }
        }
    }

    // Add / Edit Product Dialog
    if (showAddDialog || productToEdit != null) {
        AddEditProductDialog(
            initialProduct = productToEdit,
            existingCategories = categories,
            viewModel = viewModel,
            onSave = { name, sku, category, costPrice, sellingPrice, stock, unit ->
                viewModel.saveProduct(
                    id = productToEdit?.id ?: 0L,
                    name = name,
                    sku = sku,
                    category = category,
                    costPrice = costPrice,
                    sellingPrice = sellingPrice,
                    stock = stock,
                    unit = unit
                )
                showAddDialog = false
                productToEdit = null
            },
            onDismiss = {
                showAddDialog = false
                productToEdit = null
            }
        )
    }

    // Single Product Delete Confirmation Dialog
    productToDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text(tr("Hapus Produk?", "Delete Product?")) },
            text = { Text(tr("Apakah Anda yakin ingin menghapus '${prod.name}' dari katalog?", "Are you sure you want to delete '${prod.name}' from catalog?")) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(prod)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(tr("Hapus", "Delete"))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { productToDelete = null }) {
                    Text(tr("Batal", "Cancel"))
                }
            }
        )
    }

    // 1. DIALOG HAPUS PILIH (DELETE SELECTED PRODUCTS)
    if (showDeleteSelectedDialog && selectedProductIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            icon = {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text(tr("Hapus Produk Terpilih?", "Delete Selected Products?")) },
            text = {
                val count = selectedProductIds.size
                Text(
                    text = "${tr("Apakah Anda yakin ingin menghapus", "Are you sure you want to delete")} $count ${tr("produk terpilih dari katalog toko?", "selected products from catalog?")}"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProducts(selectedProductIds.toList()) {
                            showDeleteSelectedDialog = false
                            isSelectionMode = false
                            selectedProductIds = emptySet()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_selected_button")
                ) {
                    Text("${tr("Hapus", "Delete")} (${selectedProductIds.size})")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text(tr("Batal", "Cancel"))
                }
            }
        )
    }

    // 2. DIALOG HAPUS PER KATEGORI (DELETE BY CATEGORY)
    if (showDeleteByCategoryDialog) {
        var selectedCat by remember { mutableStateOf<String?>(categories.firstOrNull()) }
        val productCountInCat = allProducts.count { it.category.equals(selectedCat, ignoreCase = true) }

        AlertDialog(
            onDismissRequest = { showDeleteByCategoryDialog = false },
            icon = {
                Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text(tr("Hapus Produk per Kategori", "Delete Products by Category")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = tr(
                            "Pilih kategori produk yang ingin dihapus dari katalog toko:",
                            "Select product category to delete from store catalog:"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (categories.isEmpty()) {
                        Text(
                            text = tr("Tidak ada kategori produk tersimpan.", "No product categories found."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            categories.forEach { cat ->
                                val count = allProducts.count { it.category.equals(cat, ignoreCase = true) }
                                val isChosen = selectedCat.equals(cat, ignoreCase = true)

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isChosen) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedCat = cat }
                                        .testTag("delete_category_item_$cat")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isChosen,
                                            onClick = { selectedCat = cat }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = trDynamic(cat),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "$count " + tr("produk", "items"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedCat != null) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "${tr("Peringatan: Seluruh", "Warning: All")} $productCountInCat ${tr("produk dalam kategori", "products in category")} '$selectedCat' ${tr("akan dihapus permanen.", "will be permanently deleted.")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedCat?.let { cat ->
                            viewModel.deleteProductsByCategory(cat) {
                                showDeleteByCategoryDialog = false
                            }
                        }
                    },
                    enabled = selectedCat != null && productCountInCat > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_by_category_button")
                ) {
                    Text(
                        tr("Hapus Kategori", "Delete Category") + if (productCountInCat > 0) " ($productCountInCat)" else ""
                    )
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteByCategoryDialog = false }) {
                    Text(tr("Batal", "Cancel"))
                }
            }
        )
    }

    // 3. DIALOG HAPUS SEMUA PRODUK (DELETE ALL PRODUCTS)
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            icon = {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text(tr("Hapus Semua Produk di Katalog?", "Delete All Products in Catalog?")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${tr("Apakah Anda yakin ingin menghapus seluruh", "Are you sure you want to delete all")} ${allProducts.size} ${tr("produk dari database katalog toko?", "products from catalog database?")}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = tr(
                            "• Seluruh data barang, stok, dan harga modal/jual akan dibersihkan.\n• Riwayat transaksi dan nota penjualan yang sudah tercatat sebelumnya tetap aman.\n• Anda dapat memuat kembali contoh produk bawaan kapan saja jika dibutuhkan.",
                            "• All items, stock, and pricing will be cleared.\n• Prior sales history and receipts remain intact.\n• You can reload sample products anytime if needed."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllProducts {
                            showDeleteAllDialog = false
                            isSelectionMode = false
                            selectedProductIds = emptySet()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_all_products_button")
                ) {
                    Text("${tr("Hapus Semua", "Delete All")} (${allProducts.size})")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteAllDialog = false }) {
                    Text(tr("Batal", "Cancel"))
                }
            }
        )
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    currencySymbol: String = "Rp",
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelectionMode) Modifier.clickable(onClick = onToggleSelect) else Modifier
            )
            .testTag("product_card_${product.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox in selection mode
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("product_checkbox_${product.id}")
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = product.category,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (product.sku.isNotBlank()) {
                        Text(
                            text = product.sku,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatRupiah(product.sellingPrice, currencySymbol),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${tr("Stok", "Stock")}: ${product.stock} ${trDynamic(product.unit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (product.stock > 10) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFDC2626),
                        fontWeight = FontWeight.Medium
                    )
                }

                if (product.costPrice > 0) {
                    Text(
                        text = "${tr("Modal", "Cost")}: ${formatRupiah(product.costPrice, currencySymbol)} | Margin: ${formatRupiah(product.sellingPrice - product.costPrice, currencySymbol)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            // Actions: Edit (Rubah Harga) and Delete (hidden in selection mode)
            if (!isSelectionMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_product_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = tr("Rubah Harga", "Change Price"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_product_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = tr("Hapus", "Delete"),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    initialProduct: Product?,
    existingCategories: List<String>,
    viewModel: PosViewModel? = null,
    onSave: (
        name: String,
        sku: String,
        category: String,
        costPrice: Double,
        sellingPrice: Double,
        stock: Int,
        unit: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var sku by remember { mutableStateOf(initialProduct?.sku ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "Umum") }
    var costPriceInput by remember { mutableStateOf(if (initialProduct != null && initialProduct.costPrice > 0) initialProduct.costPrice.toLong().toString() else "") }
    var sellingPriceInput by remember { mutableStateOf(if (initialProduct != null) initialProduct.sellingPrice.toLong().toString() else "") }
    var stockInput by remember { mutableStateOf(if (initialProduct != null) initialProduct.stock.toString() else "100") }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "pcs") }
    var showScannerForSku by remember { mutableStateOf(false) }

    val isEditing = initialProduct != null

    if (showScannerForSku && viewModel != null) {
        BarcodeScannerSheet(
            viewModel = viewModel,
            currencySymbol = "Rp",
            onDismiss = { showScannerForSku = false },
            onScanCustomBarcode = { scannedCode ->
                sku = scannedCode
                showScannerForSku = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) tr("Rubah Harga & Edit Item", "Edit Price & Item") else tr("Tambah Item Baru", "Add New Item"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr("Nama Item / Produk *", "Item / Product Name *")) },
                    placeholder = { Text(tr("cth: Kopi Susu Aren / Beras 5kg", "e.g. Latte Coffee / Rice 5kg")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text(tr("Kategori", "Category")) },
                        placeholder = { Text(tr("Makanan, Minuman...", "Food, Drinks...")) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_category_input")
                    )

                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text(tr("SKU / Barcode", "SKU / Barcode")) },
                        placeholder = { Text("PRD-001") },
                        trailingIcon = {
                            if (viewModel != null) {
                                IconButton(
                                    onClick = { showScannerForSku = true },
                                    modifier = Modifier.testTag("scan_sku_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = tr("Pindai Barcode", "Scan Barcode"),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_sku_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sellingPriceInput,
                        onValueChange = { input -> if (input.all { it.isDigit() }) sellingPriceInput = input },
                        label = { Text(tr("Harga Jual (Rp) *", "Selling Price (Rp) *")) },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_selling_price_input")
                    )

                    OutlinedTextField(
                        value = costPriceInput,
                        onValueChange = { input -> if (input.all { it.isDigit() }) costPriceInput = input },
                        label = { Text(tr("Harga Modal (Rp)", "Cost Price (Rp)")) },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_cost_price_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stockInput,
                        onValueChange = { input -> if (input.all { it.isDigit() }) stockInput = input },
                        label = { Text(tr("Stok", "Stock")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_stock_input")
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text(tr("Satuan", "Unit")) },
                        placeholder = { Text("pcs, box, kg...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_unit_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selling = sellingPriceInput.toDoubleOrNull() ?: 0.0
                    val cost = costPriceInput.toDoubleOrNull() ?: 0.0
                    val stock = stockInput.toIntOrNull() ?: 0
                    if (name.isNotBlank() && selling > 0) {
                        onSave(name.trim(), sku.trim(), category.trim(), cost, selling, stock, unit.trim())
                    }
                },
                enabled = name.isNotBlank() && (sellingPriceInput.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text(if (isEditing) tr("Simpan Perubahan", "Save Changes") else tr("Tambah Produk", "Add Product"))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(tr("Batal", "Cancel"))
            }
        }
    )
}
