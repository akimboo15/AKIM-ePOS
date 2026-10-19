package com.example.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

sealed class PrinterConnectionState {
    object Disconnected : PrinterConnectionState()
    data class Connecting(val deviceName: String) : PrinterConnectionState()
    data class Connected(
        val deviceName: String,
        val address: String,
        val protocol: BluetoothDeviceType = BluetoothDeviceType.CLASSIC
    ) : PrinterConnectionState()
    data class Error(val message: String) : PrinterConnectionState()
}

class BluetoothPrinterManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    val scanner = BluetoothScannerUtility(context)

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val PREFS_NAME = "notapos_printer_prefs"
    private val KEY_SAVED_MAC = "saved_printer_mac"
    private val KEY_SAVED_NAME = "saved_printer_name"

    // Classic RFCOMM Socket resources
    private var currentSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    // BLE GATT resources
    private var currentGatt: BluetoothGatt? = null
    private var bleWriteCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow<PrinterConnectionState>(PrinterConnectionState.Disconnected)
    val connectionState: StateFlow<PrinterConnectionState> = _connectionState.asStateFlow()

    private val _lastPrintedText = MutableStateFlow<String?>(null)
    val lastPrintedText: StateFlow<String?> = _lastPrintedText.asStateFlow()

    val isBluetoothSupported: Boolean
        get() = bluetoothAdapter != null

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    init {
        val savedMac = getSavedPrinterMac()
        val savedName = getSavedPrinterName()
        if (!savedMac.isNullOrBlank() && !savedName.isNullOrBlank()) {
            _connectionState.value = PrinterConnectionState.Disconnected
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDeviceInfo> {
        val adapter = bluetoothAdapter ?: return emptyList()
        return try {
            val bonded = adapter.bondedDevices ?: emptySet()
            val connectedAddr = (connectionState.value as? PrinterConnectionState.Connected)?.address
            bonded.map { device ->
                val isCurrentlyConnected = connectedAddr != null && connectedAddr == device.address
                val devName = device.name ?: "Perangkat Bluetooth"
                val majorClass = try { device.bluetoothClass?.majorDeviceClass } catch (_: Exception) { null }
                val uuids = try { device.uuids?.toList() } catch (_: Exception) { null }
                BluetoothDeviceInfo(
                    name = devName,
                    address = device.address,
                    isConnected = isCurrentlyConnected,
                    isBonded = true,
                    deviceType = BluetoothScannerUtility.mapDeviceType(device.type),
                    rssi = null,
                    isLikelyPrinter = BluetoothScannerUtility.detectLikelyPrinter(devName, majorClass, uuids)
                )
            }.sortedWith(
                compareByDescending<BluetoothDeviceInfo> { it.isConnected }
                    .thenByDescending { it.isLikelyPrinter }
                    .thenBy { it.name }
            )
        } catch (e: SecurityException) {
            Log.e("BluetoothPrinter", "Permission missing for bondedDevices: ${e.message}")
            emptyList()
        }
    }

    fun startDiscoveryScan(scanMode: ScanMode = ScanMode.DUAL_ALL) {
        val connectedAddr = (connectionState.value as? PrinterConnectionState.Connected)?.address
        scanner.startScan(scanMode = scanMode, connectedAddress = connectedAddr)
    }

    fun stopDiscoveryScan() {
        scanner.stopScan()
    }

    fun pairDevice(address: String): Result<Boolean> {
        return scanner.pairDevice(address)
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(deviceAddress: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter
            ?: return@withContext Result.failure(Exception("Bluetooth tidak didukung di perangkat ini."))

        if (!adapter.isEnabled) {
            return@withContext Result.failure(Exception("Bluetooth belum aktif. Silakan aktifkan Bluetooth."))
        }

        // Stop any active scan before connecting
        scanner.stopScan()

        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
            val deviceName = try {
                device.name ?: "Thermal Printer"
            } catch (e: SecurityException) {
                "Thermal Printer"
            }
            val deviceType = try {
                BluetoothScannerUtility.mapDeviceType(device.type)
            } catch (_: Exception) {
                BluetoothDeviceType.CLASSIC
            }

            _connectionState.value = PrinterConnectionState.Connecting(deviceName)
            disconnectInternal()

            try {
                if (adapter.isDiscovering) {
                    adapter.cancelDiscovery()
                }
            } catch (_: SecurityException) {
            }

            // If device is strictly BLE, connect via BLE GATT directly
            if (deviceType == BluetoothDeviceType.LE) {
                val bleSuccess = connectBleGatt(device, deviceName)
                if (bleSuccess) {
                    savePrinter(deviceAddress, deviceName)
                    _connectionState.value = PrinterConnectionState.Connected(
                        deviceName = deviceName,
                        address = deviceAddress,
                        protocol = BluetoothDeviceType.LE
                    )
                    return@withContext Result.success(true)
                } else {
                    _connectionState.value = PrinterConnectionState.Error(
                        "Gagal terhubung ke printer BLE $deviceName."
                    )
                    return@withContext Result.failure(Exception("Koneksi BLE GATT gagal."))
                }
            }

            // Try Classic RFCOMM SPP Socket first for Classic or Dual devices
            try {
                val socket = try {
                    device.createRfcommSocketToServiceRecord(SPP_UUID)
                } catch (e: IOException) {
                    val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    m.invoke(device, 1) as BluetoothSocket
                }

                socket.connect()
                currentSocket = socket
                outputStream = socket.outputStream

                savePrinter(deviceAddress, deviceName)
                _connectionState.value = PrinterConnectionState.Connected(
                    deviceName = deviceName,
                    address = deviceAddress,
                    protocol = BluetoothDeviceType.CLASSIC
                )
                return@withContext Result.success(true)
            } catch (rfcommException: Exception) {
                Log.w("BluetoothPrinter", "RFCOMM failed, attempting BLE GATT fallback: ${rfcommException.message}")
                // Fallback to BLE GATT if RFCOMM fails (common on dual-mode BLE thermal printers)
                val bleFallbackSuccess = connectBleGatt(device, deviceName)
                if (bleFallbackSuccess) {
                    savePrinter(deviceAddress, deviceName)
                    _connectionState.value = PrinterConnectionState.Connected(
                        deviceName = deviceName,
                        address = deviceAddress,
                        protocol = BluetoothDeviceType.LE
                    )
                    return@withContext Result.success(true)
                }
                throw rfcommException
            }
        } catch (e: Exception) {
            Log.e("BluetoothPrinter", "Failed to connect: ${e.message}", e)
            disconnectInternal()
            _connectionState.value = PrinterConnectionState.Error(
                "Gagal terhubung: ${e.localizedMessage ?: "Pastikan printer menyala & berada dalam jangkauan"}"
            )
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectBleGatt(device: BluetoothDevice, deviceName: String): Boolean {
        val connectionDeferred = CompletableDeferred<Boolean>()

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (!connectionDeferred.isCompleted) {
                        connectionDeferred.complete(false)
                    } else {
                        _connectionState.value = PrinterConnectionState.Disconnected
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    var foundCharacteristic: BluetoothGattCharacteristic? = null
                    for (service in gatt.services) {
                        for (characteristic in service.characteristics) {
                            val props = characteristic.properties
                            val isWritable = (props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                                    (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                            if (isWritable) {
                                foundCharacteristic = characteristic
                                // Prefer known printer service UUIDs if present
                                val sUuid = service.uuid.toString().lowercase()
                                if (sUuid.startsWith("000018f0") || sUuid.startsWith("e7810a71") ||
                                    sUuid.startsWith("49535343") || sUuid.startsWith("0000ff00")
                                ) {
                                    break
                                }
                            }
                        }
                        if (foundCharacteristic != null) break
                    }

                    if (foundCharacteristic != null) {
                        bleWriteCharacteristic = foundCharacteristic
                        connectionDeferred.complete(true)
                    } else {
                        connectionDeferred.complete(false)
                    }
                } else {
                    connectionDeferred.complete(false)
                }
            }
        }

        val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
        currentGatt = gatt

        val connected = withTimeoutOrNull(8000L) {
            connectionDeferred.await()
        } ?: false

        if (!connected) {
            try {
                gatt?.close()
            } catch (_: Exception) {
            }
            currentGatt = null
            bleWriteCharacteristic = null
            Log.w("BluetoothPrinter", "BLE GATT connection failed for $deviceName")
        }
        return connected
    }

    @SuppressLint("MissingPermission")
    suspend fun print(bytes: ByteArray, plainTextSummary: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        if (plainTextSummary != null) {
            _lastPrintedText.value = plainTextSummary
        }

        // 1. Check Classic RFCOMM stream
        val socket = currentSocket
        val stream = outputStream
        if (socket != null && socket.isConnected && stream != null) {
            try {
                stream.write(bytes)
                stream.flush()
                return@withContext Result.success(true)
            } catch (e: IOException) {
                Log.e("BluetoothPrinter", "RFCOMM Write failed: ${e.message}", e)
                disconnectInternal()
                _connectionState.value = PrinterConnectionState.Error("Koneksi terputus saat mencetak: ${e.message}")
                return@withContext Result.failure(e)
            }
        }

        // 2. Check BLE GATT characteristic
        val gatt = currentGatt
        val characteristic = bleWriteCharacteristic
        if (gatt != null && characteristic != null) {
            return@withContext try {
                writeBytesOverBle(gatt, characteristic, bytes)
                Result.success(true)
            } catch (e: Exception) {
                Log.e("BluetoothPrinter", "BLE Write failed: ${e.message}", e)
                disconnectInternal()
                _connectionState.value = PrinterConnectionState.Error("Gagal mengirim data BLE: ${e.message}")
                Result.failure(e)
            }
        }

        // 3. Auto-reconnect to saved printer if available
        val savedMac = getSavedPrinterMac()
        if (!savedMac.isNullOrBlank()) {
            val reconnectResult = connect(savedMac)
            if (reconnectResult.isSuccess) {
                if (outputStream != null) {
                    return@withContext try {
                        outputStream!!.write(bytes)
                        outputStream!!.flush()
                        Result.success(true)
                    } catch (e: IOException) {
                        Result.failure(e)
                    }
                } else if (currentGatt != null && bleWriteCharacteristic != null) {
                    return@withContext try {
                        writeBytesOverBle(currentGatt!!, bleWriteCharacteristic!!, bytes)
                        Result.success(true)
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }
            }
        }

        return@withContext Result.failure(
            Exception("Printer belum terhubung. Silakan pindai & hubungkan printer di menu Printer.")
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun writeBytesOverBle(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        bytes: ByteArray
    ) {
        val chunkSize = 20 // Standard safe BLE GATT payload chunk
        var offset = 0
        while (offset < bytes.size) {
            val end = (offset + chunkSize).coerceAtMost(bytes.size)
            val chunk = bytes.copyOfRange(offset, end)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
                    characteristic,
                    chunk,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                )
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = chunk
                @Suppress("DEPRECATION")
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(characteristic)
            }
            offset = end
            delay(15L) // Pacing for thermal printer buffer
        }
    }

    fun disconnect() {
        scanner.stopScan()
        disconnectInternal()
        _connectionState.value = PrinterConnectionState.Disconnected
    }

    @SuppressLint("MissingPermission")
    private fun disconnectInternal() {
        try {
            outputStream?.close()
        } catch (_: Exception) {
        }
        try {
            currentSocket?.close()
        } catch (_: Exception) {
        }
        try {
            currentGatt?.disconnect()
            currentGatt?.close()
        } catch (_: Exception) {
        }
        currentSocket = null
        outputStream = null
        currentGatt = null
        bleWriteCharacteristic = null
    }

    fun savePrinter(address: String, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SAVED_MAC, address)
            .putString(KEY_SAVED_NAME, name)
            .apply()
    }

    fun getSavedPrinterMac(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SAVED_MAC, null)
    }

    fun getSavedPrinterName(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SAVED_NAME, null)
    }

    fun clearSavedPrinter() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SAVED_MAC).remove(KEY_SAVED_NAME).apply()
        disconnect()
    }
}
