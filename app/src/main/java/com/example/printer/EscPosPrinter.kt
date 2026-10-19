package com.example.printer

import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.data.model.formatRupiah
import com.example.ui.localization.AppLocalization
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

object EscPosPrinter {

    // ESC/POS Command Constants
    private const val ESC: Byte = 0x1B
    private const val GS: Byte = 0x1D

    val CMD_INIT: ByteArray = byteArrayOf(ESC, 0x40)
    val CMD_ALIGN_LEFT: ByteArray = byteArrayOf(ESC, 0x61, 0x00)
    val CMD_ALIGN_CENTER: ByteArray = byteArrayOf(ESC, 0x61, 0x01)
    val CMD_ALIGN_RIGHT: ByteArray = byteArrayOf(ESC, 0x61, 0x02)

    val CMD_BOLD_ON: ByteArray = byteArrayOf(ESC, 0x45, 0x01)
    val CMD_BOLD_OFF: ByteArray = byteArrayOf(ESC, 0x45, 0x00)

    val CMD_FONT_NORMAL: ByteArray = byteArrayOf(GS, 0x21, 0x00)
    val CMD_FONT_DOUBLE_HEIGHT: ByteArray = byteArrayOf(GS, 0x21, 0x01)
    val CMD_FONT_DOUBLE_WIDTH: ByteArray = byteArrayOf(GS, 0x21, 0x10)
    val CMD_FONT_DOUBLE_ALL: ByteArray = byteArrayOf(GS, 0x21, 0x11)

    val CMD_CUT_PAPER: ByteArray = byteArrayOf(GS, 0x56, 0x41, 0x00)
    val CMD_FEED_LINE: ByteArray = byteArrayOf(0x0A)

    fun feedLines(count: Int): ByteArray {
        val safeCount = count.coerceIn(1, 10).toByte()
        return byteArrayOf(ESC, 0x64, safeCount)
    }

    private fun getAlignmentCmd(alignment: String): ByteArray {
        return when (alignment.uppercase()) {
            "LEFT" -> CMD_ALIGN_LEFT
            "RIGHT" -> CMD_ALIGN_RIGHT
            else -> CMD_ALIGN_CENTER
        }
    }

    private fun getTitleFontCmd(fontSize: String): ByteArray {
        return when (fontSize.uppercase()) {
            "NORMAL" -> CMD_FONT_NORMAL
            "LARGE" -> CMD_FONT_DOUBLE_HEIGHT
            else -> CMD_FONT_DOUBLE_ALL
        }
    }

    /**
     * Builds ESC/POS byte sequence for a receipt respecting all logo, layout, and language settings.
     */
    fun buildReceiptBytes(
        store: StoreProfile,
        transaction: Transaction,
        items: List<TransactionItem>
    ): ByteArray {
        val stream = ByteArrayOutputStream()
        val width = if (store.paperWidth == 80) 48 else 32
        val charset = Charset.forName("windows-1252")
        val strings = AppLocalization.getStrings(store.languageCode)
        val cur = store.currencySymbol
        val divider = store.dividerChar.toString().repeat(width)

        fun write(bytes: ByteArray) = stream.write(bytes)
        fun writeText(text: String) = stream.write(text.toByteArray(charset))
        fun writeLine(text: String = "") {
            writeText(text)
            write(CMD_FEED_LINE)
        }

        // 1. Initialize
        write(CMD_INIT)

        // 2. Store Logo (Raster Bit Image GS v 0) if enabled
        if (store.showLogo) {
            try {
                val logoBitmap = LogoBitmapProcessor.loadOrGenerateLogoBitmap(
                    presetCode = store.logoPresetType,
                    customPath = store.customLogoPath,
                    targetWidthPx = store.logoSizePx,
                    storeNameInitials = store.name.take(3)
                )
                if (logoBitmap != null) {
                    write(getAlignmentCmd(store.logoAlignment))
                    write(LogoBitmapProcessor.bitmapToEscPosRasterBytes(logoBitmap))
                    write(CMD_FEED_LINE)
                }
            } catch (_: Exception) {
                // Continue gracefully if bitmap generation fails in headless unit test
            }
        }

        // 3. Header (Store Info with configurable alignment & font size)
        write(getAlignmentCmd(store.headerAlignment))
        write(CMD_BOLD_ON)
        write(getTitleFontCmd(store.titleFontSize))
        writeLine(store.name)
        write(CMD_FONT_NORMAL)
        write(CMD_BOLD_OFF)

        if (store.showSlogan && store.slogan.isNotBlank()) {
            writeLine(store.slogan)
        }
        if (store.showAddressPhone) {
            if (store.address.isNotBlank()) {
                writeLine(store.address)
            }
            if (store.phone.isNotBlank()) {
                writeLine("Telp: ${store.phone}")
            }
        }
        if (store.customHeaderNote.isNotBlank()) {
            writeLine(store.customHeaderNote)
        }

        // Divider
        write(CMD_ALIGN_LEFT)
        writeLine(divider)

        // 4. Transaction Metadata (Respecting visibility toggles & language)
        val invText = if (store.showInvoiceNumber) "${strings.receiptInvoiceNo}: ${transaction.invoiceNumber}" else ""
        val dateText = if (store.showDateTime) transaction.formattedDate else ""
        if (invText.isNotEmpty() && dateText.isNotEmpty()) {
            if (invText.length + dateText.length + 1 > width) {
                writeLine(invText)
                writeLine("${strings.receiptDate}: $dateText")
            } else {
                writeLine(formatTwoColumns(invText, dateText, width))
            }
        } else if (invText.isNotEmpty()) {
            writeLine(invText)
        } else if (dateText.isNotEmpty()) {
            writeLine("${strings.receiptDate}: $dateText")
        }

        val cashierStr = if (store.showCashierName) "${strings.receiptCashier}: ${transaction.cashierName}" else ""
        val custStr = if (store.showCustomerName) "${strings.receiptCustomer}: ${transaction.customerName}" else ""
        if (cashierStr.isNotEmpty() && custStr.isNotEmpty()) {
            if (cashierStr.length + custStr.length + 1 > width) {
                writeLine(cashierStr)
                writeLine(custStr)
            } else {
                writeLine(formatTwoColumns(cashierStr, custStr, width))
            }
        } else if (cashierStr.isNotEmpty()) {
            writeLine(cashierStr)
        } else if (custStr.isNotEmpty()) {
            writeLine(custStr)
        }

        if (invText.isNotEmpty() || dateText.isNotEmpty() || cashierStr.isNotEmpty() || custStr.isNotEmpty()) {
            writeLine(divider)
        }

        // 5. Items Table
        items.forEachIndexed { index, item ->
            write(CMD_BOLD_ON)
            val prefix = if (store.showItemNumber) "${index + 1}. " else ""
            writeLine("$prefix${item.productName}")
            write(CMD_BOLD_OFF)

            val qtyPrice = "${item.quantity} x ${formatRupiah(item.actualPrice, cur)}"
            val subtotalStr = formatRupiah(item.subtotal, cur)
            writeLine(formatTwoColumns("  $qtyPrice", subtotalStr, width))

            if (item.note.isNotBlank()) {
                writeLine("  *${item.note}")
            }
        }

        writeLine(divider)

        // 6. Totals & Payment
        writeLine(formatTwoColumns(strings.receiptSubtotal, formatRupiah(transaction.subtotal, cur), width))

        if (store.showTaxAndDiscount) {
            if (transaction.discountAmount > 0) {
                writeLine(formatTwoColumns(strings.receiptDiscount, "-${formatRupiah(transaction.discountAmount, cur)}", width))
            }
            if (transaction.taxAmount > 0) {
                writeLine(formatTwoColumns(strings.receiptTax, formatRupiah(transaction.taxAmount, cur), width))
            }
        }

        write(CMD_BOLD_ON)
        write(CMD_FONT_DOUBLE_HEIGHT)
        writeLine(formatTwoColumns(strings.receiptTotal, formatRupiah(transaction.grandTotal, cur), width))
        write(CMD_FONT_NORMAL)
        write(CMD_BOLD_OFF)

        writeLine(formatTwoColumns(strings.receiptPaymentMethod, transaction.paymentMethod, width))
        writeLine(formatTwoColumns(strings.receiptPaid, formatRupiah(transaction.paidAmount, cur), width))
        writeLine(formatTwoColumns(strings.receiptChange, formatRupiah(transaction.changeAmount, cur), width))

        if (transaction.notes.isNotBlank()) {
            writeLine("Catatan: ${transaction.notes}")
        }

        writeLine("=".repeat(width))

        // 7. Signature Box if enabled
        if (store.showSignatureBox) {
            write(CMD_ALIGN_CENTER)
            writeLine(strings.receiptSignature)
            writeLine("")
            writeLine("")
            writeLine("( .................... )")
            write(CMD_ALIGN_LEFT)
            writeLine(divider)
        }

        // 8. Footer, Social Media & Barcode
        write(CMD_ALIGN_CENTER)
        val footerLines = store.receiptFooter.split("\n")
        for (line in footerLines) {
            if (line.isNotBlank()) {
                writeLine(line)
            }
        }

        if (store.showSocialMedia && store.socialMediaInfo.isNotBlank()) {
            writeLine(store.socialMediaInfo)
        }

        if (store.showBarcode) {
            writeLine("|| ||| | |||| || | ||| ||")
            writeLine(transaction.invoiceNumber)
        }

        // Feed & Cut
        write(feedLines(store.feedLines))
        if (store.autoCut) {
            write(CMD_CUT_PAPER)
        }

        return stream.toByteArray()
    }

    /**
     * Builds ESC/POS bytes for a quick hardware test print.
     */
    fun buildTestPrintBytes(store: StoreProfile): ByteArray {
        val stream = ByteArrayOutputStream()
        val width = if (store.paperWidth == 80) 48 else 32
        val charset = Charset.forName("windows-1252")
        val divider = store.dividerChar.toString().repeat(width)

        fun write(bytes: ByteArray) = stream.write(bytes)
        fun writeText(text: String) = stream.write(text.toByteArray(charset))
        fun writeLine(text: String = "") {
            writeText(text)
            write(CMD_FEED_LINE)
        }

        write(CMD_INIT)
        write(CMD_ALIGN_CENTER)
        write(CMD_BOLD_ON)
        write(CMD_FONT_DOUBLE_ALL)
        writeLine("TEST PRINT OK")
        write(CMD_FONT_NORMAL)
        write(CMD_BOLD_OFF)
        writeLine(store.name)
        writeLine("Thermal Bluetooth Printer")
        writeLine(divider)
        write(CMD_ALIGN_LEFT)
        writeLine("Karakter Width: $width cols")
        writeLine("Kertas: ${store.paperWidth}mm")
        writeLine("Bahasa: ${store.languageCode.uppercase()}")
        writeLine(formatTwoColumns("Item Sample", "${store.currencySymbol} 25.000", width))
        writeLine(divider)
        write(CMD_BOLD_ON)
        writeLine(formatTwoColumns("TOTAL TEST", "${store.currencySymbol} 25.000", width))
        write(CMD_BOLD_OFF)
        write(CMD_ALIGN_CENTER)
        writeLine("Printer Siap Digunakan!")
        write(feedLines(store.feedLines))
        if (store.autoCut) {
            write(CMD_CUT_PAPER)
        }

        return stream.toByteArray()
    }

    /**
     * Generates plain text receipt for sharing (WhatsApp, copy, share sheet).
     */
    fun generatePlainTextReceipt(
        store: StoreProfile,
        transaction: Transaction,
        items: List<TransactionItem>
    ): String {
        val width = if (store.paperWidth == 80) 48 else 32
        val sb = StringBuilder()
        val sep = store.dividerChar.toString().repeat(width)
        val doubleSep = "=".repeat(width)
        val strings = AppLocalization.getStrings(store.languageCode)
        val cur = store.currencySymbol

        fun alignText(text: String, align: String): String {
            if (text.length >= width) return text
            return when (align.uppercase()) {
                "LEFT" -> text
                "RIGHT" -> " ".repeat(width - text.length) + text
                else -> {
                    val pad = (width - text.length) / 2
                    " ".repeat(pad) + text
                }
            }
        }

        if (store.showLogo) {
            val badge = "[ LOGO: ${store.name.take(12).uppercase()} ]"
            sb.appendLine(alignText(badge, store.logoAlignment))
        }

        sb.appendLine(alignText(store.name.uppercase(), store.headerAlignment))
        if (store.showSlogan && store.slogan.isNotBlank()) {
            sb.appendLine(alignText(store.slogan, store.headerAlignment))
        }
        if (store.showAddressPhone) {
            if (store.address.isNotBlank()) sb.appendLine(alignText(store.address, store.headerAlignment))
            if (store.phone.isNotBlank()) sb.appendLine(alignText("Telp: ${store.phone}", store.headerAlignment))
        }
        if (store.customHeaderNote.isNotBlank()) {
            sb.appendLine(alignText(store.customHeaderNote, store.headerAlignment))
        }
        sb.appendLine(sep)

        if (store.showInvoiceNumber || store.showDateTime) {
            val invStr = if (store.showInvoiceNumber) "${strings.receiptInvoiceNo}: ${transaction.invoiceNumber}" else ""
            val dtStr = if (store.showDateTime) transaction.formattedDate else ""
            if (invStr.isNotEmpty() && dtStr.isNotEmpty()) {
                if (invStr.length + dtStr.length + 1 > width) {
                    sb.appendLine(invStr)
                    sb.appendLine("${strings.receiptDate}: $dtStr")
                } else {
                    sb.appendLine(formatTwoColumns(invStr, dtStr, width))
                }
            } else if (invStr.isNotEmpty()) {
                sb.appendLine(invStr)
            } else if (dtStr.isNotEmpty()) {
                sb.appendLine("${strings.receiptDate}: $dtStr")
            }
        }

        if (store.showCashierName || store.showCustomerName) {
            val cashierStr = if (store.showCashierName) "${strings.receiptCashier}: ${transaction.cashierName}" else ""
            val custStr = if (store.showCustomerName) "${strings.receiptCustomer}: ${transaction.customerName}" else ""
            if (cashierStr.isNotEmpty() && custStr.isNotEmpty()) {
                if (cashierStr.length + custStr.length + 1 > width) {
                    sb.appendLine(cashierStr)
                    sb.appendLine(custStr)
                } else {
                    sb.appendLine(formatTwoColumns(cashierStr, custStr, width))
                }
            } else if (cashierStr.isNotEmpty()) {
                sb.appendLine(cashierStr)
            } else if (custStr.isNotEmpty()) {
                sb.appendLine(custStr)
            }
        }
        sb.appendLine(sep)

        items.forEachIndexed { index, item ->
            val prefix = if (store.showItemNumber) "${index + 1}. " else ""
            sb.appendLine("$prefix${item.productName}")
            val qtyPrice = "  ${item.quantity} x ${formatRupiah(item.actualPrice, cur)}"
            sb.appendLine(formatTwoColumns(qtyPrice, formatRupiah(item.subtotal, cur), width))
            if (item.note.isNotBlank()) {
                sb.appendLine("  *${item.note}")
            }
        }
        sb.appendLine(sep)

        sb.appendLine(formatTwoColumns(strings.receiptSubtotal, formatRupiah(transaction.subtotal, cur), width))
        if (store.showTaxAndDiscount) {
            if (transaction.discountAmount > 0) {
                sb.appendLine(formatTwoColumns(strings.receiptDiscount, "-${formatRupiah(transaction.discountAmount, cur)}", width))
            }
            if (transaction.taxAmount > 0) {
                sb.appendLine(formatTwoColumns(strings.receiptTax, formatRupiah(transaction.taxAmount, cur), width))
            }
        }
        sb.appendLine(formatTwoColumns(strings.receiptTotal, formatRupiah(transaction.grandTotal, cur), width))
        sb.appendLine(formatTwoColumns(strings.receiptPaymentMethod, transaction.paymentMethod, width))
        sb.appendLine(formatTwoColumns(strings.receiptPaid, formatRupiah(transaction.paidAmount, cur), width))
        sb.appendLine(formatTwoColumns(strings.receiptChange, formatRupiah(transaction.changeAmount, cur), width))
        sb.appendLine(doubleSep)

        if (store.showSignatureBox) {
            sb.appendLine(alignText(strings.receiptSignature, "CENTER"))
            sb.appendLine("")
            sb.appendLine(alignText("( .................... )", "CENTER"))
            sb.appendLine(sep)
        }

        val footerLines = store.receiptFooter.split("\n")
        for (line in footerLines) {
            if (line.isNotBlank()) sb.appendLine(alignText(line, "CENTER"))
        }
        if (store.showSocialMedia && store.socialMediaInfo.isNotBlank()) {
            sb.appendLine(alignText(store.socialMediaInfo, "CENTER"))
        }
        if (store.showBarcode) {
            sb.appendLine(alignText("|| ||| | |||| || | ||| ||", "CENTER"))
            sb.appendLine(alignText(transaction.invoiceNumber, "CENTER"))
        }

        return sb.toString()
    }

    /**
     * Formats two columns aligned Left and Right.
     */
    fun formatTwoColumns(left: String, right: String, totalWidth: Int): String {
        val totalAvailable = totalWidth - right.length
        if (totalAvailable <= 0) {
            return "$left $right"
        }
        return if (left.length > totalAvailable - 1) {
            val truncatedLeft = left.take(totalAvailable - 1)
            truncatedLeft + " ".repeat((totalWidth - truncatedLeft.length - right.length).coerceAtLeast(1)) + right
        } else {
            val spaces = totalWidth - left.length - right.length
            left + " ".repeat(spaces.coerceAtLeast(1)) + right
        }
    }
}
