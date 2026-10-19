package com.example.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

enum class PresetLogoType(val code: String, val label: String) {
    STORE_MART("STORE_MART", "Toko / Minimarket"),
    CAFE_COFFEE("CAFE_COFFEE", "Kedai Kopi / Cafe"),
    RESTAURANT("RESTAURANT", "Resto / Kuliner"),
    FASHION("FASHION", "Butik / Fashion"),
    ELECTRONICS("ELECTRONICS", "Toko Elektronik / HP"),
    PHARMACY("PHARMACY", "Apotek / Kesehatan"),
    CUSTOM_IMAGE("CUSTOM_IMAGE", "Gambar Galeri Kustom");

    companion object {
        fun fromCode(code: String): PresetLogoType {
            return values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: STORE_MART
        }
    }
}

object LogoBitmapProcessor {

    private const val CUSTOM_LOGO_FILENAME = "store_receipt_logo.png"

    /**
     * Copies and scales an image selected via Android Photo Picker into internal storage.
     */
    fun saveCustomLogoFromUri(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return null

            val maxDim = 320
            val ratio = original.width.toFloat() / original.height.toFloat().coerceAtLeast(1f)
            val targetWidth = if (ratio >= 1f) maxDim else (maxDim * ratio).toInt().coerceAtLeast(64)
            val targetHeight = if (ratio >= 1f) (maxDim / ratio).toInt().coerceAtLeast(64) else maxDim

            val scaled = Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
            val outFile = File(context.filesDir, CUSTOM_LOGO_FILENAME)
            FileOutputStream(outFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            outFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Loads the custom logo Bitmap from internal file path, or generates a crisp monochrome
     * emblem Bitmap for the chosen preset logo type.
     */
    fun loadOrGenerateLogoBitmap(
        presetCode: String,
        customPath: String,
        targetWidthPx: Int = 184,
        storeNameInitials: String = "POS"
    ): Bitmap? {
        val safeWidth = (targetWidthPx / 8 * 8).coerceIn(64, 384)
        val type = PresetLogoType.fromCode(presetCode)

        if (type == PresetLogoType.CUSTOM_IMAGE && customPath.isNotBlank()) {
            val file = File(customPath)
            if (file.exists()) {
                val decoded = BitmapFactory.decodeFile(file.absolutePath)
                if (decoded != null) {
                    val aspect = decoded.height.toFloat() / decoded.width.toFloat().coerceAtLeast(1f)
                    val targetHeight = (safeWidth * aspect).toInt().coerceIn(48, 320)
                    return Bitmap.createScaledBitmap(decoded, safeWidth, targetHeight, true)
                }
            }
        }

        // Generate a crisp monochrome emblem bitmap for thermal printing
        val height = (safeWidth * 0.65f).toInt().coerceAtLeast(80)
        val bitmap = Bitmap.createBitmap(safeWidth, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            textSize = height * 0.34f
            isFakeBoldText = true
        }

        val cx = safeWidth / 2f
        val cy = height / 2f
        val r = (height * 0.42f)

        // Draw outer badge frame
        val rect = RectF(cx - r * 1.4f, cy - r, cx + r * 1.4f, cy + r)
        canvas.drawRoundRect(rect, 18f, 18f, paint)

        val badgeSymbol = when (type) {
            PresetLogoType.STORE_MART -> "MART"
            PresetLogoType.CAFE_COFFEE -> "CAFE"
            PresetLogoType.RESTAURANT -> "RESTO"
            PresetLogoType.FASHION -> "STYLE"
            PresetLogoType.ELECTRONICS -> "TECH"
            PresetLogoType.PHARMACY -> "MED+"
            PresetLogoType.CUSTOM_IMAGE -> storeNameInitials.take(4).uppercase()
        }

        val fontMetrics = fillPaint.fontMetrics
        val textBaseline = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(badgeSymbol, cx, textBaseline, fillPaint)

        return bitmap
    }

    /**
     * Converts a Bitmap into ESC/POS Raster Bit Image command bytes (`GS v 0`).
     * Command format: 0x1D 0x76 0x30 0x00 xL xH yL yH d1...dk
     */
    fun bitmapToEscPosRasterBytes(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val bytesPerRow = (width + 7) / 8

        val stream = ByteArrayOutputStream()
        // GS v 0 m xL xH yL yH
        stream.write(0x1D)
        stream.write(0x76)
        stream.write(0x30)
        stream.write(0x00) // Normal mode

        stream.write(bytesPerRow and 0xFF)
        stream.write((bytesPerRow shr 8) and 0xFF)
        stream.write(height and 0xFF)
        stream.write((height shr 8) and 0xFF)

        for (y in 0 until height) {
            for (byteIdx in 0 until bytesPerRow) {
                var slice = 0
                for (bit in 0 until 8) {
                    val x = byteIdx * 8 + bit
                    if (x < width) {
                        val pixel = bitmap.getPixel(x, y)
                        val alpha = Color.alpha(pixel)
                        if (alpha > 128) {
                            val r = Color.red(pixel)
                            val g = Color.green(pixel)
                            val b = Color.blue(pixel)
                            val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                            if (luminance < 160) {
                                slice = slice or (1 shl (7 - bit))
                            }
                        }
                    }
                }
                stream.write(slice)
            }
        }
        return stream.toByteArray()
    }

    /**
     * Generates a 1-bit monochrome (black on thermal paper white) Bitmap matching the exact
     * luminance threshold (< 160) used by [bitmapToEscPosRasterBytes], so the visual receipt
     * preview screen accurately reflects how the thermal printhead will render the logo.
     */
    fun createMonochromePreviewBitmap(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val paperWhite = Color.rgb(253, 251, 247)
        val thermalBlack = Color.rgb(24, 24, 27)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = source.getPixel(x, y)
                val alpha = Color.alpha(pixel)
                var isInk = false
                if (alpha > 128) {
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                    if (luminance < 160) {
                        isInk = true
                    }
                }
                out.setPixel(x, y, if (isInk) thermalBlack else paperWhite)
            }
        }
        return out
    }
}
