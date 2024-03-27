package ir.ehsannarmani.encryption

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ir.ehsannarmani.encryption.navigation.Routes
import ir.ehsannarmani.encryption.screens.QRCodeScannerScreen
import ir.ehsannarmani.encryption.screens.ScreenA
import ir.ehsannarmani.encryption.screens.ScreenB
import ir.ehsannarmani.encryption.screens.ScreenD
import ir.ehsannarmani.encryption.ui.theme.EncryptionTheme
import ir.ehsannarmani.encryption.utils.BiometricUtils

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EncryptionTheme(true) {
                // A surface container using the 'background' color from the theme
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
                    }
                }
            }
        }
    }
}
