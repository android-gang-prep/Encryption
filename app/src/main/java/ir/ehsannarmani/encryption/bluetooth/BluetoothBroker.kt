package ir.ehsannarmani.encryption.bluetooth

import android.bluetooth.BluetoothSocket
import android.os.Handler
import java.io.IOException

class BluetoothBroker (private val handler:Handler){

    companion object{
        const val MESSAGE_READ = 0
        const val MESSAGE_WRITE = 1
    }

    inner class ConnectedThread(private val socket:BluetoothSocket):Thread(){
        private val inStream = socket.inputStream
        private val outStream = socket.outputStream
        private val buffer = ByteArray(1024)

        override fun run() {
            var numBytes:Int
            while (true){
                numBytes = try {
                    inStream.read(buffer)
                }catch (e:IOException){

                    break
                }
                val readMsg = handler.obtainMessage(
                    MESSAGE_READ,
                    numBytes,
                    -1,
                    buffer
                )
                readMsg.sendToTarget()
            }
        }
        fun write(bytes: ByteArray){
            try {
                outStream.write(bytes)
            } catch (e: IOException) {
                println("error in sending data")
                return
            }

            // Share the sent message with the UI activity.
            val writtenMsg = handler.obtainMessage(
                MESSAGE_WRITE, -1, -1, buffer)
            writtenMsg.sendToTarget()
        }

        fun cancel(){
            runCatching { socket.close() }
        }
    }


}