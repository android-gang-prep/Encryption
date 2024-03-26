package ir.ehsannarmani.encryption.viewModels

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
@SuppressLint("MissingPermission")
class ScreenDViewModel: ViewModel() {

    private val _scanning = MutableStateFlow(false)
    val scanning = _scanning.asStateFlow()

    private val _scanFailure = MutableStateFlow(false)
    val scanFailure = _scanFailure.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val availableDevices = _availableDevices.asStateFlow()

    val scanBroadcast = object :BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            println("on receive")
            when(action){
                BluetoothAdapter.ACTION_DISCOVERY_STARTED->{
                    _scanning.update { true }
                    _scanFailure.update { false }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED-> _scanning.update { false }
                BluetoothDevice.ACTION_FOUND->{
                    val bluetoothDevice:BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    bluetoothDevice?.let {device->
                        if (device.address !in _availableDevices.value.map { it.address }){
                            _availableDevices.update { it+device }
                        }
                    }
                }
            }
        }
    }

    fun startScan(adapter: BluetoothAdapter, ){
        adapter.startDiscovery().also { started->
            println("discovery status: $started")
            _scanFailure.update { !started }
        }
    }
    fun registerReceiver(context: Context){
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(scanBroadcast,filter)
    }
}