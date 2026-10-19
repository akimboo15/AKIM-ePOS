package com.example.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class BluetoothDeviceType(val label: String) {
    CLASSIC("Classic"),
    LE("BLE"),
    DUAL("Dual (Classic+BLE)"),
    UNKNOWN("Bluetooth")
}

enum class ScanMode(val label: String) {
    DUAL_ALL("Semua (Classic + BLE)"),
    BLE_ONLY("Bluetooth LE (BLE)"),
    CLASSIC_ONLY("Bluetooth Classic")
}

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val isConnected: Boolean = false,
    val isBonded: Boolean = false,
    val deviceType: BluetoothDeviceType = BluetoothDeviceType.CLASSIC,
    val rssi: Int? = null,
    val isLikelyPrinter: Boolean = false
) {
    val signalQualityLabel: String
        get() = when {
            rssi == null -> ""
            rssi >= -60 -> "Sinyal Kuat ($rssi dBm)"
            rssi >= -80 -> "Sinyal Sedang ($rssi dBm)"
            else -> "Sinyal Lemah ($rssi dBm)"
        }
}

/**
 * Utility class for scanning both Bluetooth Low Energy (BLE) and Bluetooth Classic devices
 * to detect available thermal printers nearby.
 */
class BluetoothScannerUtility(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var scanTimeoutJob: Job? = null

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices.asStateFlow()

    private val _scanStatusMessage = MutableStateFlow<String?>(null)
    val scanStatusMessage: StateFlow<String?> = _scanStatusMessage.asStateFlow()

    private var isReceiverRegistered = false
    private var isBleScanning = false

    companion object {
        private const val TAG = "BluetoothScannerUtil"
        const val DEFAULT_SCAN_DURATION_MS = 12_000L

        // Common BLE Thermal Printer Service UUID prefixes
        private val KNOWN_PRINTER_UUID_PREFIXES = listOf(
            "000018f0", // Standard BLE Printer Service
            "0000ff00", // Common Chinese Thermal Printer BLE Service
            "0000ffe0", // UART / BLE Serial Port Service
            "e7810a71", // Star / Xprinter BLE Service
            "49535343", // Microchip / ISSC Transparent UART used by many POS printers
            "00001101"  // Standard SPP UUID
        )

        // Common Thermal Printer Name Keywords
        private val KNOWN_PRINTER_NAME_KEYWORDS = listOf(
            "printer", "print", "pos", "thermal", "rpp", "mtp", "mpt",
            "goojprt", "panda", "iware", "eppos", "zjiang", "zj-",
            "vsc", "bellav", "bixolon", "epson", "star", "xprinter",
            "xp-", "pt-", "bt-spp", "innerprinter", "58mm", "80mm",
            "bluetooth printer", " mobile printer", "kassen", "codesoft"
        )

        /**
         * Determines whether a Bluetooth device is likely a thermal receipt printer
         * based on its name, Bluetooth Class (IMAGING), or advertised UUIDs.
         */
        fun detectLikelyPrinter(
            name: String?,
            majorDeviceClass: Int?,
            serviceUuids: List<ParcelUuid>?
        ): Boolean {
            if (majorDeviceClass == BluetoothClass.Device.Major.IMAGING) {
                return true
            }
            if (!name.isNullOrBlank()) {
                val lowerName = name.lowercase()
                if (KNOWN_PRINTER_NAME_KEYWORDS.any { lowerName.contains(it) }) {
                    return true
                }
            }
            if (!serviceUuids.isNullOrEmpty()) {
                val hasPrinterUuid = serviceUuids.any { parcelUuid ->
                    val uuidStr = parcelUuid.uuid.toString().lowercase()
                    KNOWN_PRINTER_UUID_PREFIXES.any { prefix -> uuidStr.startsWith(prefix) }
                }
                if (hasPrinterUuid) return true
            }
            return false
        }

        fun mapDeviceType(typeInt: Int): BluetoothDeviceType {
            return when (typeInt) {
                BluetoothDevice.DEVICE_TYPE_CLASSIC -> BluetoothDeviceType.CLASSIC
                BluetoothDevice.DEVICE_TYPE_LE -> BluetoothDeviceType.LE
                BluetoothDevice.DEVICE_TYPE_DUAL -> BluetoothDeviceType.DUAL
                else -> BluetoothDeviceType.UNKNOWN
            }
        }
    }

    // BroadcastReceiver for Bluetooth Classic Discovery & Bond state changes
    private val classicDiscoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                        .takeIf { it != Short.MIN_VALUE.toInt() }

                    if (device != null) {
                        addOrUpdateDiscoveredDevice(
                            device = device,
                            rssi = rssi,
                            explicitType = null,
                            advertisedName = null,
                            serviceUuids = null
                        )
                    }
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (device != null) {
                        addOrUpdateDiscoveredDevice(
                            device = device,
                            rssi = null,
                            explicitType = null,
                            advertisedName = null,
                            serviceUuids = null
                        )
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (!isBleScanning) {
                        _isScanning.value = false
                        _scanStatusMessage.value = "Pemindaian selesai (${_discoveredDevices.value.size} perangkat ditemukan)"
                    }
                }
            }
        }
    }

    // ScanCallback for Bluetooth Low Energy (BLE)
    private val bleScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val scanResult = result ?: return
            val device = scanResult.device ?: return
            val scanRecord = scanResult.scanRecord
            val advertisedName = scanRecord?.deviceName
            val serviceUuids = scanRecord?.serviceUuids

            addOrUpdateDiscoveredDevice(
                device = device,
                rssi = scanResult.rssi,
                explicitType = BluetoothDeviceType.LE,
                advertisedName = advertisedName,
                serviceUuids = serviceUuids
            )
        }

        @SuppressLint("MissingPermission")
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach { scanResult ->
                val device = scanResult.device ?: return@forEach
                val scanRecord = scanResult.scanRecord
                addOrUpdateDiscoveredDevice(
                    device = device,
                    rssi = scanResult.rssi,
                    explicitType = BluetoothDeviceType.LE,
                    advertisedName = scanRecord?.deviceName,
                    serviceUuids = scanRecord?.serviceUuids
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "BLE Scan failed with error code: $errorCode")
            isBleScanning = false
        }
    }

    /**
     * Starts scanning for Bluetooth devices according to the selected [ScanMode].
     */
    @SuppressLint("MissingPermission")
    fun startScan(
        scanMode: ScanMode = ScanMode.DUAL_ALL,
        durationMs: Long = DEFAULT_SCAN_DURATION_MS,
        connectedAddress: String? = null
    ) {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _scanStatusMessage.value = "Bluetooth tidak didukung pada perangkat ini"
            return
        }
        if (!adapter.isEnabled) {
            _scanStatusMessage.value = "Bluetooth belum aktif. Silakan nyalakan Bluetooth"
            return
        }

        // Stop any ongoing scan first
        stopScan()

        // Seed with currently bonded devices that match the mode
        val initialList = mutableListOf<BluetoothDeviceInfo>()
        try {
            adapter.bondedDevices?.forEach { bonded ->
                val type = mapDeviceType(bonded.type)
                val include = when (scanMode) {
                    ScanMode.DUAL_ALL -> true
                    ScanMode.BLE_ONLY -> type == BluetoothDeviceType.LE || type == BluetoothDeviceType.DUAL
                    ScanMode.CLASSIC_ONLY -> type == BluetoothDeviceType.CLASSIC || type == BluetoothDeviceType.DUAL
                }
                if (include) {
                    val majorClass = try { bonded.bluetoothClass?.majorDeviceClass } catch (_: Exception) { null }
                    val uuids = try { bonded.uuids?.toList() } catch (_: Exception) { null }
                    val devName = bonded.name ?: "Perangkat Bluetooth"
                    initialList.add(
                        BluetoothDeviceInfo(
                            name = devName,
                            address = bonded.address,
                            isConnected = bonded.address == connectedAddress,
                            isBonded = true,
                            deviceType = type,
                            rssi = null,
                            isLikelyPrinter = detectLikelyPrinter(devName, majorClass, uuids)
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission missing when reading bonded devices: ${e.message}")
            _scanStatusMessage.value = "Izin Bluetooth diperlukan untuk memindai perangkat"
            return
        }

        _discoveredDevices.value = initialList.sortedWith(
            compareByDescending<BluetoothDeviceInfo> { it.isConnected }
                .thenByDescending { it.isLikelyPrinter }
                .thenByDescending { it.isBonded }
        )

        _isScanning.value = true
        _scanStatusMessage.value = when (scanMode) {
            ScanMode.DUAL_ALL -> "Memindai printer Bluetooth Classic & BLE..."
            ScanMode.BLE_ONLY -> "Memindai perangkat Bluetooth Low Energy (BLE)..."
            ScanMode.CLASSIC_ONLY -> "Memindai perangkat Bluetooth Classic..."
        }

        // 1. Start Classic Discovery if requested
        if (scanMode == ScanMode.DUAL_ALL || scanMode == ScanMode.CLASSIC_ONLY) {
            try {
                registerClassicReceiver()
                if (adapter.isDiscovering) {
                    adapter.cancelDiscovery()
                }
                adapter.startDiscovery()
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException starting Classic discovery: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Exception starting Classic discovery: ${e.message}")
            }
        }

        // 2. Start BLE Scan if requested
        if (scanMode == ScanMode.DUAL_ALL || scanMode == ScanMode.BLE_ONLY) {
            try {
                val leScanner: BluetoothLeScanner? = adapter.bluetoothLeScanner
                if (leScanner != null) {
                    val settings = ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build()
                    leScanner.startScan(null, settings, bleScanCallback)
                    isBleScanning = true
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException starting BLE scan: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Exception starting BLE scan: ${e.message}")
            }
        }

        // Schedule auto-stop after timeout
        scanTimeoutJob = scope.launch {
            delay(durationMs)
            stopScan()
            val count = _discoveredDevices.value.size
            val printerCount = _discoveredDevices.value.count { it.isLikelyPrinter }
            _scanStatusMessage.value =
                "Pemindaian selesai: $count perangkat ditemukan ($printerCount terdeteksi sebagai printer)"
        }
    }

    /**
     * Stops both Bluetooth Classic discovery and BLE scanning immediately.
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null

        val adapter = bluetoothAdapter
        if (adapter != null) {
            try {
                if (adapter.isDiscovering) {
                    adapter.cancelDiscovery()
                }
            } catch (_: Exception) {
            }

            if (isBleScanning) {
                try {
                    adapter.bluetoothLeScanner?.stopScan(bleScanCallback)
                } catch (_: Exception) {
                }
                isBleScanning = false
            }
        }

        unregisterClassicReceiver()
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    private fun addOrUpdateDiscoveredDevice(
        device: BluetoothDevice,
        rssi: Int?,
        explicitType: BluetoothDeviceType?,
        advertisedName: String?,
        serviceUuids: List<ParcelUuid>?
    ) {
        try {
            val address = device.address ?: return
            val rawName = advertisedName?.takeIf { it.isNotBlank() }
                ?: device.name?.takeIf { it.isNotBlank() }

            val currentList = _discoveredDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.address.equals(address, ignoreCase = true) }
            val existing = if (existingIndex >= 0) currentList[existingIndex] else null

            val resolvedName = rawName
                ?: existing?.name?.takeIf { !it.startsWith("Perangkat Tanpa Nama") }
                ?: "Perangkat Tanpa Nama (${address.takeLast(5)})"

            val nativeType = mapDeviceType(device.type)
            val resolvedType = when {
                existing != null && existing.deviceType != BluetoothDeviceType.UNKNOWN &&
                        explicitType != null && existing.deviceType != explicitType -> BluetoothDeviceType.DUAL
                nativeType != BluetoothDeviceType.UNKNOWN -> nativeType
                explicitType != null -> explicitType
                existing != null -> existing.deviceType
                else -> BluetoothDeviceType.CLASSIC
            }

            val majorClass = try { device.bluetoothClass?.majorDeviceClass } catch (_: Exception) { null }
            val deviceUuids = serviceUuids ?: try { device.uuids?.toList() } catch (_: Exception) { null }
            val isLikelyPrinter = (existing?.isLikelyPrinter == true) ||
                    detectLikelyPrinter(resolvedName, majorClass, deviceUuids)

            val isBonded = device.bondState == BluetoothDevice.BOND_BONDED

            val updatedInfo = BluetoothDeviceInfo(
                name = resolvedName,
                address = address,
                isConnected = existing?.isConnected ?: false,
                isBonded = isBonded,
                deviceType = resolvedType,
                rssi = rssi ?: existing?.rssi,
                isLikelyPrinter = isLikelyPrinter
            )

            if (existingIndex >= 0) {
                currentList[existingIndex] = updatedInfo
            } else {
                currentList.add(updatedInfo)
            }

            // Sort: Connected first -> Likely Printers -> Bonded -> Strongest RSSI -> Name
            _discoveredDevices.value = currentList.sortedWith(
                compareByDescending<BluetoothDeviceInfo> { it.isConnected }
                    .thenByDescending { it.isLikelyPrinter }
                    .thenByDescending { it.isBonded }
                    .thenByDescending { it.rssi ?: -120 }
                    .thenBy { it.name }
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException processing discovered device: ${e.message}")
        }
    }

    /**
     * Initiates Bluetooth pairing (bonding) with an unpaired device.
     */
    @SuppressLint("MissingPermission")
    fun pairDevice(address: String): Result<Boolean> {
        val adapter = bluetoothAdapter
            ?: return Result.failure(Exception("Bluetooth tidak tersedia"))
        return try {
            val device = adapter.getRemoteDevice(address)
            if (device.bondState == BluetoothDevice.BOND_BONDED) {
                Result.success(true)
            } else {
                val started = device.createBond()
                if (started) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Gagal memulai proses pairing Bluetooth"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun registerClassicReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(classicDiscoveryReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(classicDiscoveryReceiver, filter)
            }
            isReceiverRegistered = true
        }
    }

    private fun unregisterClassicReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(classicDiscoveryReceiver)
            } catch (_: IllegalArgumentException) {
            }
            isReceiverRegistered = false
        }
    }
}
