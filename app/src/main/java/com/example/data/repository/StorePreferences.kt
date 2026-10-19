package com.example.data.repository

import android.content.Context
import com.example.data.model.StoreProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StorePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("notapos_store_prefs", Context.MODE_PRIVATE)

    private val _storeProfile = MutableStateFlow(loadProfile())
    val storeProfile: StateFlow<StoreProfile> = _storeProfile.asStateFlow()

    private fun loadProfile(): StoreProfile {
        return StoreProfile(
            id = 1,
            name = prefs.getString("store_name", "Toko Berkah Jaya") ?: "Toko Berkah Jaya",
            slogan = prefs.getString("store_slogan", "Sahabat Belanja Anda") ?: "Sahabat Belanja Anda",
            address = prefs.getString("store_address", "Jl. Merdeka No. 45, Jakarta") ?: "Jl. Merdeka No. 45, Jakarta",
            phone = prefs.getString("store_phone", "0812-3456-7890") ?: "0812-3456-7890",
            receiptFooter = prefs.getString(
                "store_footer",
                "Terima Kasih Atas Kunjungan Anda\nBarang yang sudah dibeli tidak dapat ditukar"
            ) ?: "Terima Kasih Atas Kunjungan Anda\nBarang yang sudah dibeli tidak dapat ditukar",
            paperWidth = prefs.getInt("paper_width", 58),
            autoCut = prefs.getBoolean("auto_cut", true),
            feedLines = prefs.getInt("feed_lines", 3),
            defaultCashier = prefs.getString("default_cashier", "Kasir 01") ?: "Kasir 01",
            languageCode = prefs.getString("language_code", "id") ?: "id",
            themeOption = prefs.getString("theme_option", "SAME_AS_DEVICE") ?: "SAME_AS_DEVICE",
            currencySymbol = prefs.getString("currency_symbol", "Rp") ?: "Rp",
            defaultTaxPercent = prefs.getFloat("default_tax_percent", 0f).toDouble(),
            autoPrintOnCheckout = prefs.getBoolean("auto_print_checkout", false),
            showLogo = prefs.getBoolean("show_logo", true),
            logoPresetType = prefs.getString("logo_preset_type", "STORE_MART") ?: "STORE_MART",
            customLogoPath = prefs.getString("custom_logo_path", "") ?: "",
            logoSizePx = prefs.getInt("logo_size_px", 184),
            logoAlignment = prefs.getString("logo_alignment", "CENTER") ?: "CENTER",
            headerAlignment = prefs.getString("header_alignment", "CENTER") ?: "CENTER",
            titleFontSize = prefs.getString("title_font_size", "DOUBLE") ?: "DOUBLE",
            dividerStyle = prefs.getString("divider_style", "DASH") ?: "DASH",
            customHeaderNote = prefs.getString("custom_header_note", "") ?: "",
            socialMediaInfo = prefs.getString("social_media_info", "IG: @tokoberkahjaya") ?: "IG: @tokoberkahjaya",
            showSlogan = prefs.getBoolean("show_slogan", true),
            showAddressPhone = prefs.getBoolean("show_address_phone", true),
            showInvoiceNumber = prefs.getBoolean("show_invoice_number", true),
            showDateTime = prefs.getBoolean("show_date_time", true),
            showCashierName = prefs.getBoolean("show_cashier_name", true),
            showCustomerName = prefs.getBoolean("show_customer_name", true),
            showItemNumber = prefs.getBoolean("show_item_number", false),
            showTaxAndDiscount = prefs.getBoolean("show_tax_discount", true),
            showSocialMedia = prefs.getBoolean("show_social_media", true),
            showBarcode = prefs.getBoolean("show_barcode", true),
            showSignatureBox = prefs.getBoolean("show_signature_box", false)
        )
    }

    fun saveProfile(profile: StoreProfile) {
        prefs.edit()
            .putString("store_name", profile.name)
            .putString("store_slogan", profile.slogan)
            .putString("store_address", profile.address)
            .putString("store_phone", profile.phone)
            .putString("store_footer", profile.receiptFooter)
            .putInt("paper_width", profile.paperWidth)
            .putBoolean("auto_cut", profile.autoCut)
            .putInt("feed_lines", profile.feedLines)
            .putString("default_cashier", profile.defaultCashier)
            .putString("language_code", profile.languageCode)
            .putString("theme_option", profile.themeOption)
            .putString("currency_symbol", profile.currencySymbol)
            .putFloat("default_tax_percent", profile.defaultTaxPercent.toFloat())
            .putBoolean("auto_print_checkout", profile.autoPrintOnCheckout)
            .putBoolean("show_logo", profile.showLogo)
            .putString("logo_preset_type", profile.logoPresetType)
            .putString("custom_logo_path", profile.customLogoPath)
            .putInt("logo_size_px", profile.logoSizePx)
            .putString("logo_alignment", profile.logoAlignment)
            .putString("header_alignment", profile.headerAlignment)
            .putString("title_font_size", profile.titleFontSize)
            .putString("divider_style", profile.dividerStyle)
            .putString("custom_header_note", profile.customHeaderNote)
            .putString("social_media_info", profile.socialMediaInfo)
            .putBoolean("show_slogan", profile.showSlogan)
            .putBoolean("show_address_phone", profile.showAddressPhone)
            .putBoolean("show_invoice_number", profile.showInvoiceNumber)
            .putBoolean("show_date_time", profile.showDateTime)
            .putBoolean("show_cashier_name", profile.showCashierName)
            .putBoolean("show_customer_name", profile.showCustomerName)
            .putBoolean("show_item_number", profile.showItemNumber)
            .putBoolean("show_tax_discount", profile.showTaxAndDiscount)
            .putBoolean("show_social_media", profile.showSocialMedia)
            .putBoolean("show_barcode", profile.showBarcode)
            .putBoolean("show_signature_box", profile.showSignatureBox)
            .apply()
        _storeProfile.value = profile
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed", false)
    }

    fun setOnboardingCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
    }
}
