package com.example.ui.screens.cashier

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.data.model.CartItem
import com.example.data.model.formatRupiah
import com.example.ui.localization.tr
import com.example.ui.theme.AccentEmerald

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PriceEditDialog(
    cartItem: CartItem,
    onConfirm: (
        customPrice: Double?,
        discount: Double,
        quantity: Int,
        note: String,
        savePriceToCatalog: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var priceInput by remember {
        val initialPrice = cartItem.customPrice ?: cartItem.product.sellingPrice
        mutableStateOf(initialPrice.toLong().toString())
    }
    var discountInput by remember {
        mutableStateOf(cartItem.discount.toLong().toString())
    }
    var quantity by remember {
        mutableIntStateOf(cartItem.quantity)
    }
    var note by remember {
        mutableStateOf(cartItem.note)
    }
    var saveToCatalog by remember {
        mutableStateOf(false)
    }

    val currentInputPrice = priceInput.toDoubleOrNull() ?: cartItem.product.sellingPrice
    val currentInputDiscount = discountInput.toDoubleOrNull() ?: 0.0
    val effectivePrice = (currentInputPrice - currentInputDiscount).coerceAtLeast(0.0)
    val calculatedSubtotal = effectivePrice * quantity

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = tr("Rubah Harga", "Change Price"),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = tr("Rubah Harga Item", "Edit Item Price"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = cartItem.product.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info harga asli
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr("Harga Katalog / Asli:", "Catalog / Original Price:"),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = formatRupiah(cartItem.product.sellingPrice),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Input Harga Baru
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            priceInput = input
                        }
                    },
                    label = { Text(tr("Harga Satuan Baru (Rp)", "New Unit Price (Rp)")) },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("price_override_input")
                )

                // Quick buttons: Reset ke Normal, Potongan %
                Text(
                    text = tr("Preset Cepat:", "Quick Presets:"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AssistChip(
                        onClick = {
                            priceInput = cartItem.product.sellingPrice.toLong().toString()
                            discountInput = "0"
                        },
                        label = { Text(tr("Harga Normal", "Normal Price")) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    AssistChip(
                        onClick = {
                            val discounted = (cartItem.product.sellingPrice * 0.95).toLong()
                            priceInput = discounted.toString()
                        },
                        label = { Text("-5%") }
                    )
                    AssistChip(
                        onClick = {
                            val discounted = (cartItem.product.sellingPrice * 0.90).toLong()
                            priceInput = discounted.toString()
                        },
                        label = { Text("-10%") }
                    )
                    AssistChip(
                        onClick = {
                            val discounted = (cartItem.product.sellingPrice * 0.80).toLong()
                            priceInput = discounted.toString()
                        },
                        label = { Text("-20%") }
                    )
                }

                // Option to persist new price into Room Database catalog permanently
                if (cartItem.product.id > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("Simpan ke Database Katalog", "Save to Catalog Database"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = tr("Perbarui harga tetap item ini di toko", "Update permanent price in store"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = saveToCatalog,
                            onCheckedChange = { saveToCatalog = it },
                            modifier = Modifier.testTag("save_price_to_catalog_switch")
                        )
                    }
                }

                // Quantity Stepper
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

                // Diskon Manual per Item (Rp)
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            discountInput = input
                        }
                    },
                    label = { Text(tr("Potongan / Diskon per Item (Rp)", "Item Discount (Rp)")) },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Catatan Item
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(tr("Catatan Item (opsional)", "Item Note (optional)")) },
                    placeholder = { Text(tr("cth: Harga Grosir, Request Khusus", "e.g. Wholesale Price, Special Request")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Subtotal Preview
                Surface(
                    color = AccentEmerald.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr("Subtotal Akhir:", "Final Subtotal:"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentEmerald
                        )
                        Text(
                            text = formatRupiah(calculatedSubtotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentEmerald
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalCustomPrice = currentInputPrice
                    val customPriceToSave = if (!saveToCatalog && finalCustomPrice == cartItem.product.sellingPrice) {
                        null
                    } else {
                        finalCustomPrice
                    }
                    onConfirm(customPriceToSave, currentInputDiscount, quantity, note.trim(), saveToCatalog)
                },
                modifier = Modifier.testTag("save_price_override_button")
            ) {
                Text(tr("Simpan Perubahan", "Save Changes"))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(tr("Batal", "Cancel"))
            }
        }
    )
}
