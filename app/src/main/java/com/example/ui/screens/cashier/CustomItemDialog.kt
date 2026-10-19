package com.example.ui.screens.cashier

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.localization.tr

@Composable
fun CustomItemDialog(
    initialBarcode: String = "",
    onConfirm: (
        name: String,
        price: Double,
        quantity: Int,
        saveToCatalog: Boolean,
        category: String,
        sku: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf(initialBarcode) }
    var category by remember { mutableStateOf("Umum") }
    var priceInput by remember { mutableStateOf("") }
    var quantity by remember { mutableIntStateOf(1) }
    var saveToCatalog by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = tr("Tambah Item Baru / Bebas", "Add New / Custom Item"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr("Nama Item / Produk *", "Item / Product Name *")) },
                    placeholder = { Text(tr("cth: Es Campur Spesial / Ongkir", "e.g. Special Dessert / Shipping")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_item_name_input")
                )

                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            priceInput = input
                        }
                    },
                    label = { Text(tr("Harga Satuan (Rp) *", "Unit Price (Rp) *")) },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_item_price_input")
                )

                if (saveToCatalog) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text(tr("Kategori Katalog", "Catalog Category")) },
                        placeholder = { Text(tr("Umum, Makanan, Minuman...", "General, Food, Drinks...")) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_item_category_input")
                    )

                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text(tr("SKU / Barcode", "SKU / Barcode")) },
                        placeholder = { Text(tr("cth: MNM-005 atau kode batang", "e.g. MNM-005 or barcode")) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_item_sku_input")
                    )
                }

                // Toggle to persist into Room Database
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("Simpan ke Katalog Toko (Database)", "Save to Store Catalog (Database)"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = tr("Agar item tetap tersedia saat aplikasi dibuka kembali", "So item remains available next time app opens"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = saveToCatalog,
                        onCheckedChange = { saveToCatalog = it },
                        modifier = Modifier.testTag("save_custom_item_to_db_switch")
                    )
                }

                Text(
                    text = tr("Jumlah Kuantitas:", "Quantity:"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledIconButton(
                        onClick = { if (quantity > 1) quantity-- },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = tr("Kurang", "Decrease"))
                    }

                    Text(
                        text = "$quantity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    FilledIconButton(
                        onClick = { quantity++ },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = tr("Tambah", "Add"))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceInput.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && price > 0) {
                        onConfirm(name.trim(), price, quantity, saveToCatalog, category.trim(), sku.trim())
                    }
                },
                enabled = name.isNotBlank() && (priceInput.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.testTag("confirm_custom_item_button")
            ) {
                Text(tr("Tambah & Simpan", "Add & Save"))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(tr("Batal", "Cancel"))
            }
        }
    )
}
