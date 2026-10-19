package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TabletMac
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.printer.PrinterConnectionState
import com.example.ui.common.AppOnboardingDialog
import com.example.ui.common.LocalAppWindowSizeClass
import com.example.ui.common.calculateAppWindowSizeClass
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppLocalization
import com.example.ui.localization.LocalAppLanguageCode
import com.example.ui.localization.tr
import com.example.ui.screens.cashier.CashierScreen
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.printer.PrinterScreen
import com.example.ui.screens.printer.ThermalPrintPreviewScreen
import com.example.ui.screens.products.ProductListScreen
import com.example.ui.screens.settings.StoreSettingsScreen
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PosViewModel

enum class NavScreen(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    CASHIER(Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale, "nav_cashier"),
    PRODUCTS(Icons.Filled.Inventory, Icons.Outlined.Inventory, "nav_products"),
    HISTORY(Icons.Filled.History, Icons.Outlined.History, "nav_history"),
    PRINTER(Icons.Filled.Print, Icons.Outlined.Print, "nav_printer"),
    SETTINGS(Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {

    private val posViewModel: PosViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val storeProfile by posViewModel.storeProfile.collectAsStateWithLifecycle()

            MyApplicationTheme(themeOptionCode = storeProfile.themeOption) {
                var currentScreen by remember { mutableStateOf(NavScreen.CASHIER) }
                val printerState by posViewModel.printerConnectionState.collectAsStateWithLifecycle()
                val pairedDevices by posViewModel.pairedDevices.collectAsStateWithLifecycle()
                val uiState by posViewModel.uiState.collectAsStateWithLifecycle()
                val showOnboarding by posViewModel.showOnboarding.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                val strings = AppLocalization.getStrings(storeProfile.languageCode)
                val currentLang = AppLanguage.fromCode(storeProfile.languageCode)

                // Handle back press to return to Kasir
                BackHandler(enabled = uiState.printPreviewTransaction == null && currentScreen != NavScreen.CASHIER) {
                    currentScreen = NavScreen.CASHIER
                }

                // Show snackbars when triggered in ViewModel
                LaunchedEffect(uiState.snackbarMessage) {
                    uiState.snackbarMessage?.let { msg ->
                        snackbarHostState.showSnackbar(
                            AppLocalization.translateDynamic(storeProfile.languageCode, msg)
                        )
                        posViewModel.clearSnackbar()
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val windowSizeClass = calculateAppWindowSizeClass(maxWidth)
                    val isTabletOrWide = windowSizeClass.isTabletOrWide

                    CompositionLocalProvider(
                        LocalAppWindowSizeClass provides windowSizeClass,
                        LocalAppLanguageCode provides storeProfile.languageCode
                    ) {
                        Scaffold(
                            contentWindowInsets = WindowInsets.systemBars,
                            topBar = {
                                CenterAlignedTopAppBar(
                                    navigationIcon = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .padding(start = 12.dp)
                                                .clickable { currentScreen = NavScreen.SETTINGS }
                                                .testTag("top_app_logo_badge")
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.img_akim_epos_logo),
                                                contentDescription = stringResource(id = R.string.app_name),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                        }
                                    },
                                    title = {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = storeProfile.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = currentLang.flagEmoji,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Text(
                                                text = "${stringResource(id = R.string.app_name)} • " + when (currentScreen) {
                                                    NavScreen.CASHIER -> strings.subtitleCashier
                                                    NavScreen.PRODUCTS -> strings.subtitleCatalog
                                                    NavScreen.HISTORY -> strings.subtitleHistory
                                                    NavScreen.PRINTER -> strings.subtitlePrinter
                                                    NavScreen.SETTINGS -> strings.subtitleSettings
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    actions = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(end = 12.dp)
                                        ) {
                                            if (isTabletOrWide) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(20.dp),
                                                    modifier = Modifier
                                                        .padding(end = 8.dp)
                                                        .testTag("tablet_mode_badge")
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.TabletMac,
                                                            contentDescription = "Mode Tablet",
                                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Tablet POS",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                }
                                            }

                                            // Quick Live Receipt Print Preview Chip in top bar
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(20.dp),
                                                modifier = Modifier
                                                    .padding(end = 6.dp)
                                                    .clickable { posViewModel.openLiveCartPrintPreview() }
                                                    .testTag("top_bar_receipt_preview_button")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ReceiptLong,
                                                        contentDescription = tr("Pratinjau Nota", "Receipt Preview"),
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = tr("Nota", "Receipt"),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }

                                            // Status chip for Bluetooth printer in top bar
                                            val isConnected = printerState is PrinterConnectionState.Connected
                                            Surface(
                                                color = if (isConnected) AccentEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(20.dp),
                                                modifier = Modifier
                                                    .clickable { currentScreen = NavScreen.PRINTER }
                                                    .testTag("top_printer_status_indicator")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(
                                                                if (isConnected) AccentEmerald else Color(0xFF94A3B8),
                                                                CircleShape
                                                            )
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                                                        contentDescription = "Printer Status",
                                                        tint = if (isConnected) AccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isConnected) "Print OK" else strings.navPrinter,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isConnected) AccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(4.dp))

                                            // Quick Tutorial / Guide button in top bar
                                            IconButton(
                                                onClick = { posViewModel.openOnboardingGuide() },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("top_bar_tutorial_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.HelpOutline,
                                                    contentDescription = tr("Panduan & Tutorial", "Guide & Tutorial"),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    },
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            },
                            bottomBar = {
                                if (!isTabletOrWide) {
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 8.dp,
                                        modifier = Modifier.testTag("mobile_navigation_bar")
                                    ) {
                                        NavScreen.values().forEach { screen ->
                                            val selected = currentScreen == screen
                                            val labelText = when (screen) {
                                                NavScreen.CASHIER -> strings.navCashier
                                                NavScreen.PRODUCTS -> strings.navCatalog
                                                NavScreen.HISTORY -> strings.navHistory
                                                NavScreen.PRINTER -> strings.navPrinter
                                                NavScreen.SETTINGS -> strings.navSettings
                                            }
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = { currentScreen = screen },
                                                icon = {
                                                    if (screen == NavScreen.CASHIER && uiState.totalItemCount > 0) {
                                                        BadgedBox(
                                                            badge = {
                                                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                                                    Text("${uiState.totalItemCount}")
                                                                }
                                                            }
                                                        ) {
                                                            Icon(
                                                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                                                contentDescription = labelText
                                                            )
                                                        }
                                                    } else {
                                                        Icon(
                                                            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                                            contentDescription = labelText
                                                        )
                                                    }
                                                },
                                                label = {
                                                    Text(
                                                        text = labelText,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 11.sp,
                                                        maxLines = 1
                                                    )
                                                },
                                                modifier = Modifier.testTag(screen.testTag)
                                            )
                                        }
                                    }
                                }
                            },
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            modifier = Modifier.fillMaxSize()
                        ) { innerPadding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                // Tablet & Wide-Screen NavigationRail
                                if (isTabletOrWide) {
                                    NavigationRail(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .testTag("tablet_navigation_rail")
                                    ) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        NavScreen.values().forEach { screen ->
                                            val selected = currentScreen == screen
                                            val labelText = when (screen) {
                                                NavScreen.CASHIER -> strings.navCashier
                                                NavScreen.PRODUCTS -> strings.navCatalog
                                                NavScreen.HISTORY -> strings.navHistory
                                                NavScreen.PRINTER -> strings.navPrinter
                                                NavScreen.SETTINGS -> strings.navSettings
                                            }
                                            NavigationRailItem(
                                                selected = selected,
                                                onClick = { currentScreen = screen },
                                                icon = {
                                                    if (screen == NavScreen.CASHIER && uiState.totalItemCount > 0) {
                                                        BadgedBox(
                                                            badge = {
                                                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                                                    Text("${uiState.totalItemCount}")
                                                                }
                                                            }
                                                        ) {
                                                            Icon(
                                                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                                                contentDescription = labelText
                                                            )
                                                        }
                                                    } else {
                                                        Icon(
                                                            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                                            contentDescription = labelText
                                                        )
                                                    }
                                                },
                                                label = {
                                                    Text(
                                                        text = labelText,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 11.sp,
                                                        maxLines = 1
                                                    )
                                                },
                                                modifier = Modifier
                                                    .padding(vertical = 4.dp)
                                                    .testTag(screen.testTag)
                                            )
                                        }
                                    }
                                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    val activePreviewTx = uiState.printPreviewTransaction
                                    if (activePreviewTx != null) {
                                        ThermalPrintPreviewScreen(
                                            transactionWithItems = activePreviewTx,
                                            storeProfile = storeProfile,
                                            printerState = printerState,
                                            pairedDevices = pairedDevices,
                                            isDraftPreview = uiState.isDraftPrintPreview,
                                            onUpdateStoreProfile = { updated ->
                                                posViewModel.updateStoreProfileSilently(updated)
                                            },
                                            onConnectPrinter = { addr ->
                                                posViewModel.connectPrinter(addr)
                                            },
                                            onOpenPrinterScanner = {
                                                posViewModel.closeThermalPrintPreview()
                                                currentScreen = NavScreen.PRINTER
                                            },
                                            onTriggerPrint = { updatedProfile, copies ->
                                                posViewModel.triggerThermalPrintFromPreview(
                                                    transactionWithItems = activePreviewTx,
                                                    updatedProfile = updatedProfile,
                                                    copyCount = copies
                                                )
                                            },
                                            onShareReceipt = { tx ->
                                                posViewModel.shareReceipt(tx)
                                            },
                                            onClose = {
                                                posViewModel.closeThermalPrintPreview()
                                            }
                                        )
                                    } else {
                                        Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                                            when (screen) {
                                                NavScreen.CASHIER -> CashierScreen(viewModel = posViewModel)
                                                NavScreen.PRODUCTS -> ProductListScreen(viewModel = posViewModel)
                                                NavScreen.HISTORY -> HistoryScreen(viewModel = posViewModel)
                                                NavScreen.PRINTER -> PrinterScreen(viewModel = posViewModel)
                                                NavScreen.SETTINGS -> StoreSettingsScreen(
                                                    viewModel = posViewModel,
                                                    onNavigateToPrinter = { currentScreen = NavScreen.PRINTER },
                                                    onOpenTutorial = { posViewModel.openOnboardingGuide() }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Fresh install & help Onboarding Dialog
                        if (showOnboarding) {
                            AppOnboardingDialog(
                                onDismiss = {
                                    posViewModel.dismissOnboarding()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
