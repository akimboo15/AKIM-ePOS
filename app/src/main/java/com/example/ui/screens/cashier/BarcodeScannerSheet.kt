@file:kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.cashier

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.Product
import com.example.data.model.formatRupiah
import com.example.ui.localization.tr
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.viewmodel.BarcodeScanResult
import com.example.ui.viewmodel.PosViewModel
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerSheet(
    viewModel: PosViewModel,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onOpenCustomItem: (String) -> Unit = {},
    onScanCustomBarcode: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxSize()
            .testTag("barcode_scanner_sheet")
    ) {
        if (!hasCameraPermission) {
            CameraPermissionRequiredContent(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onDismiss = onDismiss,
                onManualInputSelected = { /* handled inside */ },
                viewModel = viewModel,
                currencySymbol = currencySymbol
            )
        } else {
            ActiveBarcodeScannerContent(
                viewModel = viewModel,
                currencySymbol = currencySymbol,
                onDismiss = onDismiss,
                onOpenCustomItem = onOpenCustomItem,
                onScanCustomBarcode = onScanCustomBarcode
            )
        }
    }
}

@Composable
private fun ActiveBarcodeScannerContent(
    viewModel: PosViewModel,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onOpenCustomItem: (String) -> Unit,
    onScanCustomBarcode: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isContinuousMode by remember { mutableStateOf(true) }

    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var lastScannedTimestamp by remember { mutableLongStateOf(0L) }
    var scannedNotification by remember { mutableStateOf<BarcodeScanResult?>(null) }
    var showManualInput by remember { mutableStateOf(false) }
    var manualCodeInput by remember { mutableStateOf("") }

    // Auto-hide scan notification after 3.5 seconds
    LaunchedEffect(scannedNotification) {
        if (scannedNotification != null) {
            delay(3500)
            scannedNotification = null
        }
    }

    // Audio and vibration feedback
    val playSuccessFeedback = remember {
        {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            } catch (_: Exception) { }

            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(100)
                }
            } catch (_: Exception) { }
        }
    }

    val playNotFoundFeedback = remember {
        {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
            } catch (_: Exception) { }

            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 80, 80, 80)
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                }
            } catch (_: Exception) { }
        }
    }

    val handleBarcodeDetected: (String) -> Unit = { rawCode ->
        val now = System.currentTimeMillis()
        val isSameCode = rawCode == lastScannedCode
        val isDebounced = isSameCode && (now - lastScannedTimestamp < 1800L)

        if (!isDebounced) {
            lastScannedCode = rawCode
            lastScannedTimestamp = now

            if (onScanCustomBarcode != null) {
                playSuccessFeedback()
                onScanCustomBarcode(rawCode)
                onDismiss()
            } else {
                viewModel.scanBarcodeAndAddToCart(rawCode) { result ->
                    scannedNotification = result
                    when (result) {
                        is BarcodeScanResult.Success -> {
                            playSuccessFeedback()
                            if (!isContinuousMode) {
                                onDismiss()
                            }
                        }
                        is BarcodeScanResult.NotFound -> {
                            playNotFoundFeedback()
                        }
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val scanBoxSize = (minOf(screenWidth, screenHeight) * 0.72f).coerceIn(220.dp, 320.dp)

        // 1. CameraX Preview Layer
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val barcodeScannerOptions = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                            Barcode.FORMAT_EAN_13,
                            Barcode.FORMAT_EAN_8,
                            Barcode.FORMAT_UPC_A,
                            Barcode.FORMAT_UPC_E,
                            Barcode.FORMAT_CODE_128,
                            Barcode.FORMAT_CODE_39,
                            Barcode.FORMAT_CODE_93,
                            Barcode.FORMAT_QR_CODE
                        )
                        .build()

                    val barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions)

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val executor = ContextCompat.getMainExecutor(ctx)
                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        processBarcodeImageProxy(barcodeScanner, imageProxy) { detectedCode ->
                            handleBarcodeDetected(detectedCode)
                        }
                    }

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        val camera: Camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        cameraControl = camera.cameraControl
                    } catch (e: Exception) {
                        Log.e("BarcodeScanner", "Camera binding failed: ${e.message}")
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Viewfinder Overlay with Dark Scrim & Transparent Cutout
        val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
        val laserProgress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "laser_progress"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val boxPx = scanBoxSize.toPx()
            val left = (canvasWidth - boxPx) / 2f
            val top = (canvasHeight - boxPx) / 2f - 40.dp.toPx()

            // Semi-transparent dark overlay around the scan window
            drawRect(
                color = Color.Black.copy(alpha = 0.55f),
                size = Size(canvasWidth, canvasHeight)
            )

            // Clear viewfinder viewport
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(boxPx, boxPx),
                cornerRadius = CornerRadius(20.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            // Viewfinder border
            drawRoundRect(
                color = AccentEmerald.copy(alpha = 0.8f),
                topLeft = Offset(left, top),
                size = Size(boxPx, boxPx),
                cornerRadius = CornerRadius(20.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )

            // Animated Laser scanning line
            val laserY = top + (boxPx * laserProgress)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        AccentEmerald.copy(alpha = 0.1f),
                        AccentEmerald.copy(alpha = 0.95f),
                        AccentEmerald.copy(alpha = 0.1f)
                    )
                ),
                start = Offset(left + 8.dp.toPx(), laserY),
                end = Offset(left + boxPx - 8.dp.toPx(), laserY),
                strokeWidth = 3.dp.toPx()
            )
        }

        // 3. Top Header Bar
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            color = Color.Black.copy(alpha = 0.65f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("close_scanner_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = tr("Tutup", "Close"))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = tr("Pindai Barcode Produk", "Scan Product Barcode"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = tr("Arahkan kamera ke barcode / QR", "Point camera at barcode or QR"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Flashlight toggle
                    IconButton(
                        onClick = {
                            isTorchOn = !isTorchOn
                            cameraControl?.enableTorch(isTorchOn)
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isTorchOn) AccentAmber else Color.White.copy(alpha = 0.15f),
                            contentColor = if (isTorchOn) Color.Black else Color.White
                        ),
                        modifier = Modifier.testTag("toggle_torch_button")
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = tr("Lampu Kilat", "Torch")
                        )
                    }

                    // Flip camera
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("flip_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = tr("Ganti Kamera", "Switch Camera")
                        )
                    }
                }
            }
        }

        // 4. Live Scan Result Card / Toast (Floating above controls)
        AnimatedVisibility(
            visible = scannedNotification != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp, start = 16.dp, end = 16.dp)
        ) {
            when (val result = scannedNotification) {
                is BarcodeScanResult.Success -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AccentEmerald),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scan_success_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = result.product.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Surface(
                                        color = Color.White.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "+1",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${formatRupiah(result.product.sellingPrice, currencySymbol)} • ${tr("Total di keranjang", "In cart")}: ${result.quantityInCart} ${result.product.unit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
                is BarcodeScanResult.NotFound -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scan_not_found_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tr("Barcode Tidak Dikenal", "Unknown Barcode"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "${tr("Kode", "Code")}: ${result.barcode} ${tr("belum terdaftar di katalog", "not found in catalog")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onOpenCustomItem(result.barcode)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = tr("Tambah Item Bebas", "Add Custom Item"),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
                null -> {}
            }
        }

        // 5. Bottom Controls & Quick Simulation Barcodes
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.Black.copy(alpha = 0.82f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Settings Row: Continuous Scan toggle & Manual Input toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Switch(
                            checked = isContinuousMode,
                            onCheckedChange = { isContinuousMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentEmerald
                            ),
                            modifier = Modifier.testTag("continuous_scan_toggle")
                        )
                        Text(
                            text = tr("Pindai Cepat Beruntun", "Continuous Fast Scan"),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = { showManualInput = !showManualInput },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showManualInput) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("manual_input_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showManualInput) tr("Tutup Manual", "Close Manual") else tr("Ketik Kode", "Type Code"),
                            fontSize = 12.sp
                        )
                    }
                }

                // Expandable Manual Input Section
                AnimatedVisibility(visible = showManualInput) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = manualCodeInput,
                                onValueChange = { manualCodeInput = it },
                                placeholder = {
                                    Text(
                                        tr("Ketik barcode / SKU...", "Type barcode / SKU..."),
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = AccentEmerald,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                                    cursorColor = AccentEmerald
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_barcode_input")
                            )

                            Button(
                                onClick = {
                                    if (manualCodeInput.isNotBlank()) {
                                        handleBarcodeDetected(manualCodeInput.trim())
                                        manualCodeInput = ""
                                    }
                                },
                                enabled = manualCodeInput.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("submit_manual_barcode_button")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = tr("Cari", "Search"))
                            }
                        }

                        // Quick Test Chips (Emulator & Demo friendly)
                        Text(
                            text = tr("Barcode Contoh Toko (Klik untuk uji coba):", "Sample Barcodes (Click to test):"),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )

                        val sampleCodes = listOf(
                            "MNM-001" to "Kopi Susu",
                            "MNM-002" to "Es Teh",
                            "MKN-001" to "Mie Goreng",
                            "MKN-002" to "Ayam Geprek",
                            "SBK-001" to "Minyak 2L",
                            "SBK-002" to "Beras 5Kg",
                            "SNK-001" to "Keripik",
                            "MNM-003" to "Air Mineral"
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(sampleCodes) { (code, name) ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clickable { handleBarcodeDetected(code) }
                                        .testTag("sample_chip_$code")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$code ($name)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cart Summary & Finish Button
                val uiState = viewModel.uiState.value
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${tr("Keranjang", "Cart")}: ${uiState.totalItemCount} ${tr("item", "items")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = formatRupiah(uiState.grandTotal, currencySymbol),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentEmerald
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("done_scanning_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr("Selesai Pindai", "Done Scanning"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processBarcodeImageProxy(
    barcodeScanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onSuccess: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val raw = barcode.rawValue
                    if (!raw.isNullOrBlank()) {
                        onSuccess(raw)
                        break
                    }
                }
            }
            .addOnFailureListener {
                // Ignore transient frame errors
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

@Composable
private fun CameraPermissionRequiredContent(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    onManualInputSelected: () -> Unit,
    viewModel: PosViewModel,
    currencySymbol: String
) {
    var manualBarcode by remember { mutableStateOf("") }
    var scanResult by remember { mutableStateOf<BarcodeScanResult?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = tr("Izin Akses Kamera Diperlukan", "Camera Access Required"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = tr(
                    "Aplikasi POS membutuhkan akses kamera untuk memindai barcode produk secara cepat dan otomatis menambahkan item ke transaksi kasir.",
                    "POS app requires camera access to quickly scan product barcodes and instantly add items to the cashier transaction."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("grant_camera_permission_button")
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr("Beri Izin Kamera", "Grant Camera Permission"))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = tr("Atau masukkan barcode produk secara manual:", "Or enter product barcode manually:"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = manualBarcode,
                    onValueChange = { manualBarcode = it },
                    placeholder = { Text(tr("Contoh: MNM-001", "e.g. MNM-001")) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("permission_fallback_input")
                )

                Button(
                    onClick = {
                        if (manualBarcode.isNotBlank()) {
                            viewModel.scanBarcodeAndAddToCart(manualBarcode.trim()) { res ->
                                scanResult = res
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text(tr("Tambah", "Add"))
                }
            }

            scanResult?.let { res ->
                Spacer(modifier = Modifier.height(10.dp))
                when (res) {
                    is BarcodeScanResult.Success -> {
                        Text(
                            text = "✓ ${res.product.name} ${tr("ditambahkan ke keranjang", "added to cart")}",
                            color = AccentEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is BarcodeScanResult.NotFound -> {
                        Text(
                            text = "⚠ Barcode ${res.barcode} ${tr("tidak ditemukan", "not found")}",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(tr("Kembali ke Kasir", "Back to Cashier"))
            }
        }
    }
}
