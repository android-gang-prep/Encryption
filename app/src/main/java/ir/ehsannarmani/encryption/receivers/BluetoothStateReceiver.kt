package ir.ehsannarmani.encryption.receivers

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.update

class BluetoothStateReceiver (private val onStateChanged:(isConnected:Boolean, BluetoothDevice)->Unit): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val device:BluetoothDevice? = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        when(intent?.action){
            BluetoothDevice.ACTION_ACL_CONNECTED->{
                onStateChanged(true,device ?: return)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED->{
                onStateChanged(false, device ?: return)
            }
        }
    }

}