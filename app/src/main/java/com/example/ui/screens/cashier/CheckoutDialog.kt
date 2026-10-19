package com.example.ui.screens.cashier

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.formatRupiah
import com.example.ui.localization.tr
import com.example.ui.localization.trDynamic
import com.example.ui.theme.AccentEmerald

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckoutDialog(
    subtotal: Double,
    initialDiscount: Double,
    initialTaxRate: Double,
    initialCustomerName: String,
    initialPaymentMethod: String,
    onConfirm: (
        customerName: String,
        paymentMethod: String,
        discount: Double,
        taxRate: Double,
        paidAmount: Double,
        notes: String
    ) -> Unit,
    onDismiss: () -> Unit,
    onPreviewReceipt: ((
        customerName: String,
        paymentMethod: String,
        discount: Double,
        taxRate: Double,
        paidAmount: Double,
        notes: String
    ) -> Unit)? = null
) {
    var discountInput by remember { mutableStateOf(if (initialDiscount > 0) initialDiscount.toLong().toString() else "0") }
    var applyTax by remember { mutableStateOf(initialTaxRate > 0) }
    var taxRate by remember { mutableDoubleStateOf(if (initialTaxRate > 0) initialTaxRate else 11.0) }
    var customerName by remember { mutableStateOf(initialCustomerName) }
    var selectedMethod by remember { mutableStateOf(initialPaymentMethod) }
    var notes by remember { mutableStateOf("") }

    val discountAmount = discountInput.toDoubleOrNull() ?: 0.0
    val taxableBase = (subtotal - discountAmount).coerceAtLeast(0.0)
    val taxAmount = if (applyTax) taxableBase * (taxRate / 100.0) else 0.0
    val grandTotal = taxableBase + taxAmount

    var paidInput by remember {
        mutableStateOf(grandTotal.toLong().toString())
    }

    val paidAmount = paidInput.toDoubleOrNull() ?: 0.0
    val changeAmount = (paidAmount - grandTotal).coerceAtLeast(0.0)
    val isPaymentSufficient = paidAmount >= grandTotal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = tr("Pembayaran Kasir", "Cashier Checkout"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tr("Lengkapi detail pembayaran dan nota", "Complete payment and receipt details"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Grand Total Banner
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tr("TOTAL TAGIHAN", "GRAND TOTAL"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = formatRupiah(grandTotal),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Payment Method Selector
                Text(
                    text = tr("Metode Pembayaran:", "Payment Method:"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val methods = listOf(
                        Pair("Tunai", Icons.Default.Payments),
                        Pair("QRIS", Icons.Default.QrCode2),
                        Pair("Transfer", Icons.Default.CreditCard),
                        Pair("Debit", Icons.Default.Money)
                    )
                    for ((method, icon) in methods) {
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = {
                                selectedMethod = method
                                if (method != "Tunai") {
                                    paidInput = grandTotal.toLong().toString()
                                }
                            },
                            label = { Text(trDynamic(method), fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(icon, contentDescription = method, modifier = Modifier.height(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Uang Diterima & Kembalian (Especially important for Cash)
                OutlinedTextField(
                    value = paidInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            paidInput = input
                        }
                    },
                    label = { Text(tr("Uang Diterima / Bayar (Rp)", "Cash Received / Paid (Rp)")) },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("paid_amount_input")
                )

                // Quick Cash Buttons
                Text(
                    text = tr("Pecahan Cepat:", "Quick Cash:"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AssistChip(
                        onClick = { paidInput = grandTotal.toLong().toString() },
                        label = { Text(tr("Uang Pas", "Exact Cash")) }
                    )
                    val denominations = listOf(10000L, 20000L, 50000L, 100000L, 200000L)
                    for (denom in denominations) {
                        if (denom >= grandTotal || denominations.indexOf(denom) >= denominations.size - 2) {
                            AssistChip(
                                onClick = { paidInput = denom.toString() },
                                label = { Text(formatRupiah(denom.toDouble())) }
                            )
                        }
                    }
                }

                // Kembalian Card
                Surface(
                    color = if (isPaymentSufficient) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPaymentSufficient) tr("Kembalian:", "Change:") else tr("Kurang Bayar:", "Insufficient:"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPaymentSufficient) Color(0xFF166534) else Color(0xFF991B1B)
                        )
                        Text(
                            text = formatRupiah(if (isPaymentSufficient) changeAmount else grandTotal - paidAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPaymentSufficient) Color(0xFF166534) else Color(0xFF991B1B)
                        )
                    }
                }

                // Diskon Nota Keseluruhan
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            discountInput = input
                        }
                    },
                    label = { Text(tr("Potongan / Diskon Nota (Rp)", "Receipt Discount (Rp)")) },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Pajak / PPN Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = tr("Kenakan Pajak / PPN (11%)", "Apply Tax / VAT (11%)"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (applyTax) {
                            Text(
                                text = "+ ${formatRupiah(taxAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Switch(
                        checked = applyTax,
                        onCheckedChange = { applyTax = it }
                    )
                }

                // Customer Name
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text(tr("Nama Pelanggan (opsional)", "Customer Name (optional)")) },
                    placeholder = { Text(tr("cth: Pelanggan Umum / Bpk. Rudi", "e.g. Walk-in Customer / Mr. John")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(tr("Catatan Transaksi (opsional)", "Transaction Notes (optional)")) },
                    placeholder = { Text(tr("cth: Meja 03 / Takeaway", "e.g. Table 03 / Takeaway")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (onPreviewReceipt != null) {
                    OutlinedButton(
                        onClick = {
                            onPreviewReceipt(
                                customerName.trim(),
                                selectedMethod,
                                discountAmount,
                                if (applyTax) taxRate else 0.0,
                                paidAmount,
                                notes.trim()
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_preview_receipt_button")
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Pratinjau Visual Struk Real-Time", "Real-Time Visual Receipt Preview"), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        customerName.trim(),
                        selectedMethod,
                        discountAmount,
                        if (applyTax) taxRate else 0.0,
                        paidAmount,
                        notes.trim()
                    )
                },
                enabled = isPaymentSufficient,
                modifier = Modifier.testTag("confirm_checkout_button")
            ) {
                Text(tr("Bayar & Proses Nota", "Pay & Process Receipt"))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(tr("Kembali", "Back"))
            }
        }
    )
}
