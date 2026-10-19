package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StoreProfile
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppLocalization
import com.example.ui.localization.tr
import com.example.ui.localization.trDynamic
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AppThemeOption
import com.example.ui.viewmodel.PosViewModel

private data class SettingsMenuItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String
)

@Composable
fun StoreSettingsScreen(
    viewModel: PosViewModel,
    onNavigateToPrinter: () -> Unit = {},
    onOpenTutorial: () -> Unit = {}
) {
    val storeProfile by viewModel.storeProfile.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val strings = AppLocalization.getStrings(storeProfile.languageCode)
    val currentLanguage = AppLanguage.fromCode(storeProfile.languageCode)
    val currentTheme = AppThemeOption.fromCode(storeProfile.themeOption)

    val menuItems = listOf(
        SettingsMenuItem(
            id = 0,
            title = strings.settingsGeneralAndLang,
            subtitle = tr("Tema tampilan, 22 bahasa daerah & mancanegara, mata uang, pajak, cetak otomatis", "Appearance theme, 22 regional & world languages, currency, tax, auto print"),
            icon = Icons.Default.Language,
            badge = "${currentLanguage.flagEmoji} ${currentLanguage.nativeName}"
        ),
        SettingsMenuItem(
            id = 1,
            title = strings.settingsStoreProfile,
            subtitle = tr("Identitas toko, nama kasir, alamat toko, kontak kasir, catatan nota", "Store identity, cashier name, address, contact phone, receipt footer notes"),
            icon = Icons.Default.Store,
            badge = storeProfile.name
        ),
        SettingsMenuItem(
            id = 2,
            title = strings.settingsLogoAndLayout,
            subtitle = tr("Pratinjau struk real-time, logo thermal, lebar 58/80mm, perataan & detail cetak", "Real-time receipt preview, thermal logo, 58/80mm paper width, alignment & print elements"),
            icon = Icons.Default.ReceiptLong,
            badge = "${storeProfile.paperWidth}mm"
        ),
        SettingsMenuItem(
            id = 3,
            title = strings.settingsDataAndPos,
            subtitle = tr("Koneksi printer Bluetooth, backup & restore database JSON, reload katalog", "Bluetooth printer connection, JSON database backup & restore, reload catalog"),
            icon = Icons.Default.Inventory,
            badge = "${allProducts.size} " + tr("produk", "products")
        )
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 768.dp

        if (isWideScreen) {
            // Tablet / Desktop Layout: Master-Detail Two Panes Side-by-Side
            var selectedSection by rememberSaveable { mutableIntStateOf(0) }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Pane: Vertical Menu List
                Card(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header inside left pane
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = strings.subtitleSettings,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${currentLanguage.flagEmoji} ${currentLanguage.nativeName} • ${storeProfile.currencySymbol}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = tr("Daftar Menu Pengaturan", "Settings Menu List"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Vertical Menu Items for Wide Screen
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            menuItems.forEach { item ->
                                val isSelected = selectedSection == item.id
                                VerticalSettingsMenuItemCard(
                                    item = item,
                                    isSelected = isSelected,
                                    onClick = { selectedSection = item.id },
                                    modifier = Modifier.testTag("settings_section_tab_${item.id}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f, fill = false))

                        // Quick Printer Shortcut in Left Pane
                        FilledTonalButton(
                            onClick = onNavigateToPrinter,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tr("Pengaturan Printer", "Printer Settings"), fontSize = 12.sp)
                        }

                        // App Guide & Tutorial Button in Left Pane
                        OutlinedButton(
                            onClick = onOpenTutorial,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tr("Panduan Aplikasi", "App Tutorial"), fontSize = 12.sp)
                        }
                    }
                }

                // Right Pane: Active Section Content
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val activeItem = menuItems.firstOrNull { it.id == selectedSection } ?: menuItems[0]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = activeItem.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = activeItem.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = activeItem.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider()

                        when (selectedSection) {
                            0 -> GeneralAndLanguageSection(storeProfile = storeProfile, viewModel = viewModel)
                            1 -> StoreProfileEditorSection(storeProfile = storeProfile, viewModel = viewModel)
                            2 -> ReceiptLayoutEditorTab(storeProfile = storeProfile, viewModel = viewModel)
                            3 -> PosAndDataManagementSection(
                                storeProfile = storeProfile,
                                productCount = allProducts.size,
                                transactionCount = allTransactions.size,
                                viewModel = viewModel,
                                onNavigateToPrinter = onNavigateToPrinter
                            )
                        }
                    }
                }
            }
        } else {
            // Mobile / Portrait Layout: Vertical Menu List -> Detail Navigation
            var selectedSection by rememberSaveable { mutableStateOf<Int?>(null) }

            BackHandler(enabled = selectedSection != null) {
                selectedSection = null
            }

            if (selectedSection == null) {
                // Screen 1: The Vertical List Menu of Settings
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = strings.subtitleSettings,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${currentLanguage.flagEmoji} ${currentLanguage.nativeName} • ${trDynamic(currentTheme.title)} • ${storeProfile.currencySymbol} • ${storeProfile.paperWidth}mm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    // Section Title
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tr("Daftar Menu Pengaturan", "Settings Menu List"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = tr("Pilih kategori", "Select category"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Vertical List of Menu Items
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        menuItems.forEach { item ->
                            VerticalSettingsMenuItemCard(
                                item = item,
                                isSelected = false,
                                onClick = { selectedSection = item.id },
                                modifier = Modifier.testTag("settings_section_tab_${item.id}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Quick Action Shortcut Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToPrinter() },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Print,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = tr("Pengaturan Printer Thermal & Bluetooth", "Thermal & Bluetooth Printer Settings"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = tr("Pindai perangkat bluetooth, status koneksi & tes cetak", "Scan bluetooth devices, connection status & test print"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // App Tutorial & Feature Tour Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTutorial() },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = tr("Bantuan & Panduan Aplikasi (Tutorial)", "Help & App Tutorial Guide"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = tr("Buka kembali sambutan, tutorial kasir, nota & semua fitur", "Reopen welcome popup, cashier tutorial & all features tour"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Screen 2: Active Section Detail View
                val currentSectionIndex = selectedSection ?: 0
                val activeItem = menuItems.firstOrNull { it.id == currentSectionIndex } ?: menuItems[0]
                var showSectionDropdown by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Navigation Bar to return to Vertical Menu List or Switch Menu
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Back to Menu Button
                            FilledTonalButton(
                                onClick = { selectedSection = null },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("settings_back_to_menu")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = tr("Kembali ke Daftar Menu", "Back to Menu List"),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr("Daftar Menu", "Menu List"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Dropdown to quickly switch section
                            Box {
                                OutlinedButton(
                                    onClick = { showSectionDropdown = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = activeItem.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = activeItem.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSectionDropdown,
                                    onDismissRequest = { showSectionDropdown = false }
                                ) {
                                    menuItems.forEach { item ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = item.icon,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp),
                                                        tint = if (item.id == currentSectionIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = item.title,
                                                        fontWeight = if (item.id == currentSectionIndex) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (item.id == currentSectionIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedSection = item.id
                                                showSectionDropdown = false
                                            },
                                            trailingIcon = {
                                                if (item.id == currentSectionIndex) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section Content
                    when (currentSectionIndex) {
                        0 -> GeneralAndLanguageSection(storeProfile = storeProfile, viewModel = viewModel)
                        1 -> StoreProfileEditorSection(storeProfile = storeProfile, viewModel = viewModel)
                        2 -> ReceiptLayoutEditorTab(storeProfile = storeProfile, viewModel = viewModel)
                        3 -> PosAndDataManagementSection(
                            storeProfile = storeProfile,
                            productCount = allProducts.size,
                            transactionCount = allTransactions.size,
                            viewModel = viewModel,
                            onNavigateToPrinter = onNavigateToPrinter
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalSettingsMenuItemCard(
    item: SettingsMenuItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                },
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (item.badge.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            }
                        ) {
                            Text(
                                text = item.badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeneralAndLanguageSection(
    storeProfile: StoreProfile,
    viewModel: PosViewModel
) {
    val strings = AppLocalization.getStrings(storeProfile.languageCode)
    var languageSearch by remember { mutableStateOf("") }
    var currencySymbol by remember(storeProfile) { mutableStateOf(storeProfile.currencySymbol) }
    var defaultTaxPercent by remember(storeProfile) { mutableStateOf(storeProfile.defaultTaxPercent) }
    var autoPrintOnCheckout by remember(storeProfile) { mutableStateOf(storeProfile.autoPrintOnCheckout) }

    val filteredLanguages = remember(languageSearch) {
        AppLanguage.values().filter { lang ->
            languageSearch.isBlank() ||
                lang.nativeName.contains(languageSearch, ignoreCase = true) ||
                lang.indonesianName.contains(languageSearch, ignoreCase = true) ||
                lang.code.contains(languageSearch, ignoreCase = true)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 0. Theme Selector Card (Dark, Same as Device, Light, and 8 Material Palettes)
        AppThemeSelectorCard(
            selectedThemeCode = storeProfile.themeOption,
            onSelectTheme = { theme ->
                viewModel.setAppTheme(theme.code, "${theme.title} (${theme.subtitle})")
            }
        )

        // 1. Multi-Language Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = strings.selectLanguageTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr("22 Bahasa Daerah & Mancanegara (Menu & Struk Nota)", "22 Regional & World Languages (Menu & Receipts)"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = languageSearch,
                    onValueChange = { languageSearch = it },
                    placeholder = { Text(tr("Cari bahasa (Inggris, Jawa, Sunda, Arab, China, Cantonese, dll)...", "Search language (English, Javanese, Sundanese, Arabic, Chinese, Cantonese, etc)...")) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = tr("Cari Bahasa", "Search Language")) },
                    trailingIcon = {
                        if (languageSearch.isNotEmpty()) {
                            IconButton(onClick = { languageSearch = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = tr("Hapus", "Clear"))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("language_search_input")
                )

                // Grid of Languages (2 columns)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredLanguages.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { lang ->
                                val isSelected = storeProfile.languageCode.equals(lang.code, ignoreCase = true)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.setAppLanguage(
                                                lang.code,
                                                "${lang.nativeName} (${lang.indonesianName})"
                                            )
                                        }
                                        .testTag("lang_option_${lang.code}"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = lang.flagEmoji,
                                                fontSize = 20.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = lang.nativeName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = trDynamic(lang.indonesianName),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = tr("Aktif", "Active"),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 2. Currency, Tax & Automatic Checkout Printing Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = tr("Mata Uang, Pajak & Otomatisasi Kasir", "Currency, Tax & Cashier Automation"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr("Simbol mata uang nota, PPN default, dan cetak otomatis", "Receipt currency symbol, default tax, and auto-print"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = tr("Pilih Simbol Mata Uang (Currency):", "Select Currency Symbol:"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                val currencies = listOf("Rp", "$", "€", "¥", "฿", "₱", "RM", "﷼", "£", "₩", "₫", "₹")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    currencies.forEach { sym ->
                        FilterChip(
                            selected = currencySymbol == sym,
                            onClick = { currencySymbol = sym },
                            label = { Text(sym, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                OutlinedTextField(
                    value = currencySymbol,
                    onValueChange = { if (it.length <= 5) currencySymbol = it },
                    label = { Text(tr("Simbol Mata Uang Kustom", "Custom Currency Symbol")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("currency_symbol_input")
                )

                Text(
                    text = tr("Pajak / PPN Default Transaksi:", "Default Transaction Tax / VAT:"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.0 to tr("Tanpa PPN (0%)", "No Tax (0%)"), 10.0 to "10%", 11.0 to "11%", 12.0 to "12%").forEach { (rate, label) ->
                        FilterChip(
                            selected = defaultTaxPercent == rate,
                            onClick = { defaultTaxPercent = rate },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = tr("Cetak Nota Otomatis Saat Checkout", "Auto-Print Receipt on Checkout"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tr("Langsung kirim perintah print ke printer thermal begitu pembayaran selesai", "Automatically dispatch print command to thermal printer upon payment completion"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoPrintOnCheckout,
                        onCheckedChange = { autoPrintOnCheckout = it },
                        modifier = Modifier.testTag("switch_auto_print_checkout")
                    )
                }

                Button(
                    onClick = {
                        val updated = storeProfile.copy(
                            currencySymbol = if (currencySymbol.isBlank()) "Rp" else currencySymbol.trim(),
                            defaultTaxPercent = defaultTaxPercent,
                            autoPrintOnCheckout = autoPrintOnCheckout
                        )
                        viewModel.updateStoreProfile(updated, "Pengaturan mata uang & kasir disimpan!")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_general_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.saveSettingsButton, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StoreProfileEditorSection(
    storeProfile: StoreProfile,
    viewModel: PosViewModel
) {
    val strings = AppLocalization.getStrings(storeProfile.languageCode)
    var name by remember(storeProfile) { mutableStateOf(storeProfile.name) }
    var slogan by remember(storeProfile) { mutableStateOf(storeProfile.slogan) }
    var address by remember(storeProfile) { mutableStateOf(storeProfile.address) }
    var phone by remember(storeProfile) { mutableStateOf(storeProfile.phone) }
    var defaultCashier by remember(storeProfile) { mutableStateOf(storeProfile.defaultCashier) }
    var customHeaderNote by remember(storeProfile) { mutableStateOf(storeProfile.customHeaderNote) }
    var socialMediaInfo by remember(storeProfile) { mutableStateOf(storeProfile.socialMediaInfo) }
    var footer by remember(storeProfile) { mutableStateOf(storeProfile.receiptFooter) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = tr("Identitas & Profil Toko", "Store Profile & Identity"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr("Informasi ini dicetak pada header dan footer struk nota", "This information is printed on receipt header and footer"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(tr("Nama Toko / Usaha *", "Store / Business Name *")) },
                placeholder = { Text(tr("cth: Toko Berkah Jaya / Cafe Kopi Senja", "e.g. Berkah Jaya Store / Sunset Cafe")) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_name_input")
            )

            OutlinedTextField(
                value = slogan,
                onValueChange = { slogan = it },
                label = { Text(tr("Slogan / Subjudul Toko", "Store Slogan / Subtitle")) },
                placeholder = { Text(tr("cth: Sahabat Belanja Anda / Murah & Lengkap", "e.g. Your Shopping Partner / Affordable & Complete")) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_slogan_input")
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(tr("Alamat Lengkap Toko", "Complete Store Address")) },
                placeholder = { Text(tr("cth: Jl. Merdeka No. 45, Jakarta", "e.g. 45 Merdeka St, Jakarta")) },
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_address_input")
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(tr("Nomor Telepon / WhatsApp", "Phone Number / WhatsApp")) },
                placeholder = { Text(tr("cth: 0812-3456-7890", "e.g. 0812-3456-7890")) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_phone_input")
            )

            OutlinedTextField(
                value = defaultCashier,
                onValueChange = { defaultCashier = it },
                label = { Text(tr("Nama Kasir Default", "Default Cashier Name")) },
                placeholder = { Text(tr("cth: Kasir 01 / Rina", "e.g. Cashier 01 / Sarah")) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_cashier_input")
            )

            OutlinedTextField(
                value = customHeaderNote,
                onValueChange = { customHeaderNote = it },
                label = { Text(tr("Catatan Tambahan Header (NPWP / Cabang / Info)", "Additional Header Note (Tax ID / Branch / Info)")) },
                placeholder = { Text(tr("cth: Cabang Pusat • Buka 08:00 - 22:00", "e.g. Main Branch • Open 08:00 - 22:00")) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_header_note_input")
            )

            OutlinedTextField(
                value = socialMediaInfo,
                onValueChange = { socialMediaInfo = it },
                label = { Text(tr("Info Sosial Media / Password WiFi (Di Bawah Nota)", "Social Media / WiFi Password (Bottom of Receipt)")) },
                placeholder = { Text(tr("cth: IG: @tokoberkahjaya • WiFi: Berkah123", "e.g. IG: @store_name • WiFi: Store123")) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_social_media_input")
            )

            OutlinedTextField(
                value = footer,
                onValueChange = { footer = it },
                label = { Text(tr("Ucapan Terima Kasih / Catatan Kaki (Footer Nota)", "Thank You Note / Receipt Footer")) },
                placeholder = { Text(tr("Terima Kasih Atas Kunjungan Anda\nBarang yang sudah dibeli tidak dapat ditukar", "Thank You For Your Visit\nItems purchased cannot be returned")) },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_footer_input")
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val updated = storeProfile.copy(
                        name = if (name.isBlank()) "Toko Kasir" else name.trim(),
                        slogan = slogan.trim(),
                        address = address.trim(),
                        phone = phone.trim(),
                        defaultCashier = if (defaultCashier.isBlank()) "Kasir" else defaultCashier.trim(),
                        customHeaderNote = customHeaderNote.trim(),
                        socialMediaInfo = socialMediaInfo.trim(),
                        receiptFooter = footer.trim()
                    )
                    viewModel.updateStoreProfile(updated, "Profil toko berhasil disimpan!")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_store_profile_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = "Simpan")
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.saveSettingsButton, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PosAndDataManagementSection(
    storeProfile: StoreProfile,
    productCount: Int,
    transactionCount: Int,
    viewModel: PosViewModel,
    onNavigateToPrinter: () -> Unit
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(tr("Hapus Seluruh Riwayat Transaksi?", "Clear All Transaction History?")) },
            text = {
                Text(
                    tr(
                        "Tindakan ini akan menghapus semua catatan riwayat transaksi penjualan. Daftar produk katalog dan pengaturan toko TIDAK akan terhapus.",
                        "This action will delete all recorded sales transaction history. Product catalog and store settings will NOT be deleted."
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllTransactions()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(tr("Ya, Hapus Riwayat", "Yes, Clear History"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(tr("Batal", "Cancel"))
                }
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isTwoColumnTablet = maxWidth >= 700.dp

        val statusAndActionsCard: @Composable () -> Unit = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = tr("Status Database & Perangkat Kasir", "Database & POS Device Status"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "$productCount ${tr("Item", "Items")}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = tr("Katalog Produk Tersimpan", "Saved Product Catalog"),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "$transactionCount ${tr("Nota", "Receipts")}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = tr("Transaksi Tersimpan", "Saved Transactions"),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Quick actions
                    OutlinedButton(
                        onClick = onNavigateToPrinter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_open_printer_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("Buka Pengaturan & Scanner Printer Bluetooth", "Open Bluetooth Printer Settings & Scanner"))
                    }

                    Button(
                        onClick = { viewModel.exportSalesReportCsv() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_export_csv_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("Ekspor & Bagikan Laporan Penjualan (CSV)", "Export & Share Sales Report (CSV)"))
                    }

                    OutlinedButton(
                        onClick = { viewModel.restoreSampleProducts() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_restore_samples_button")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("Muat Ulang Produk Contoh Toko ke Katalog", "Reload Sample Products into Catalog"))
                    }

                    OutlinedButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_clear_transactions_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("Bersihkan Seluruh Riwayat Transaksi", "Clear All Transaction History"))
                    }
                }
            }
        }

        if (isTwoColumnTablet) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tablet_settings_data_two_column"),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    statusAndActionsCard()
                }
                Box(modifier = Modifier.weight(1.1f)) {
                    DatabaseBackupSectionCard(viewModel = viewModel)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                statusAndActionsCard()
                DatabaseBackupSectionCard(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun AppThemeSelectorCard(
    selectedThemeCode: String,
    onSelectTheme: (AppThemeOption) -> Unit
) {
    val systemModes = listOf(
        Triple(AppThemeOption.SAME_AS_DEVICE, Icons.Default.BrightnessAuto, "Same as Device"),
        Triple(AppThemeOption.LIGHT, Icons.Default.LightMode, "Light"),
        Triple(AppThemeOption.DARK, Icons.Default.DarkMode, "Dark")
    )

    val materialPalettes = listOf(
        AppThemeOption.MATERIAL_BLUE,
        AppThemeOption.MATERIAL_GREEN,
        AppThemeOption.MATERIAL_RED,
        AppThemeOption.MATERIAL_GREY,
        AppThemeOption.MATERIAL_PINK,
        AppThemeOption.MATERIAL_YELLOW,
        AppThemeOption.MATERIAL_SAGE_GREEN,
        AppThemeOption.MATERIAL_VIOLET
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = tr("Opsi Tema & Warna Aplikasi", "App Theme & Color Options"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr("Pilih Mode Gelap/Terang atau Palet Warna Material 3", "Choose Dark/Light Mode or Material 3 Color Palette"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            Text(
                text = tr("Mode Tampilan Dasar:", "Basic Display Mode:"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            // 3 Mode Cards: Same as Device, Light, Dark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                systemModes.forEach { (themeOption, iconVec, shortTitle) ->
                    val isSelected = selectedThemeCode.equals(themeOption.code, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectTheme(themeOption) }
                            .testTag("theme_option_${themeOption.code.lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = iconVec,
                                contentDescription = shortTitle,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = trDynamic(shortTitle),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = trDynamic(themeOption.subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Text(
                text = tr("Palet Warna Material Design 3:", "Material Design 3 Color Palettes:"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            // 2-Column Grid of 8 Material Color Themes
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                materialPalettes.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { themeOption ->
                            val isSelected = selectedThemeCode.equals(themeOption.code, ignoreCase = true)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectTheme(themeOption) }
                                    .testTag("theme_option_${themeOption.code.lowercase()}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Dual Swatch Circle Badge
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(themeOption.containerSwatch)
                                                .border(1.dp, themeOption.primarySwatch.copy(alpha = 0.5f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(themeOption.primarySwatch)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = trDynamic(themeOption.title),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = trDynamic(themeOption.subtitle),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = tr("Aktif", "Active"),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

