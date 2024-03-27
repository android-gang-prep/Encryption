package ir.ehsannarmani.encryption.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.os.Handler
import ir.ehsannarmani.encryption.receivers.BluetoothStateReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.util.UUID


@SuppressLint("MissingPermission")
class BluetoothBroker(
    private val adapter: BluetoothAdapter,
    private val context: Context
) {


    var serverSocket: BluetoothServerSocket? = null
    var clientSocket: BluetoothSocket? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val bluetoothStateReceiver =
        BluetoothStateReceiver(onStateChanged = { isConnected, bluetoothDevice ->
            _isConnected.update { isConnected }
        })

    init {
        context.registerReceiver(bluetoothStateReceiver, IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        })
    }

    fun startServer(
        onConnect: () -> Unit
    ) {
        serverSocket = adapter.listenUsingRfcommWithServiceRecord(
            "Ehsan_Encryption",
            UUID.fromString(SERVICE_UUID)
        )
        var shouldLoop = true
        while (shouldLoop) {
            try {
                val socket = serverSocket?.accept()
                clientSocket = socket
                onConnect()
            } catch (e: IOException) {
                println("error when creating server: ${e.message}")
                shouldLoop = false
                clientSocket = null
            }
            clientSocket?.let {
                shouldLoop = false
                serverSocket?.close()
            }
        }
    }

    fun connect(
        macAddress: String,
        onConnect: () -> Unit,
    ) {
        adapter.cancelDiscovery()
        clientSocket = adapter
            .getRemoteDevice(macAddress)
            .createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_UUID))
        clientSocket?.let { socket ->
            try {
                socket.connect()
                onConnect()
            } catch (e: IOException) {
                socket.close()
                clientSocket = null
                println("error when connecting: ${e.message}")
            }
        }
    }

    fun closeConnection() {
        clientSocket?.close()
        serverSocket?.close()
        clientSocket = null
        serverSocket = null
    }

    companion object {
        const val SERVICE_UUID = "5fdb4b5d-e458-4855-bb60-b1df0edd33f1"
    }
}

sealed class ConnectionResult() {
    data object Error : ConnectionResult()
    data object Connected : ConnectionResult()
}