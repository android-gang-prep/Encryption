package ir.ehsannarmani.encryption

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ir.ehsannarmani.encryption.bluetooth.BluetoothBroker
import ir.ehsannarmani.encryption.navigation.Routes
import ir.ehsannarmani.encryption.screens.QRCodeScannerScreen
import ir.ehsannarmani.encryption.screens.ScreenA
import ir.ehsannarmani.encryption.screens.ScreenB
import ir.ehsannarmani.encryption.screens.ScreenD
import ir.ehsannarmani.encryption.screens.SocketType
import ir.ehsannarmani.encryption.screens.TransferScreen
import ir.ehsannarmani.encryption.ui.theme.EncryptionTheme
import ir.ehsannarmani.encryption.utils.BiometricUtils



class MainActivity : FragmentActivity() {
    companion object{
        lateinit var broker: BluetoothBroker
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        setContent {

            EncryptionTheme(true) {
                broker = BluetoothBroker(
                    adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter,
                    context = this@MainActivity
                )
//                val connected by broker.isConnected.collectAsState()
//                Toast.makeText(this, "conntected: $connected", Toast.LENGTH_SHORT).show()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = LocalAppState.current.navController

                    NavHost(navController = navController, startDestination = Routes.ScreenA.route){
                        composable(Routes.ScreenA.route){
                            ScreenA()
                        }
                        composable(Routes.ScreenB.route){
                            ScreenB()
                        }
                        composable(Routes.ScreenD.route){
                            ScreenD()
                        }
                        composable(Routes.Transfer.route+"/{type}", arguments = listOf(navArgument("type"){type = NavType.StringType})){
                            TransferScreen(
                                socketType = if (it.arguments?.getString("type") == "client") SocketType.Client else SocketType.Server
                            )
                        }
                    }
                }
            }
        }
    }
}
