package ir.ehsannarmani.encryption.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.ehsannarmani.encryption.LocalAppState
import ir.ehsannarmani.encryption.viewModels.ScreenDViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.ehsannarmani.encryption.MainActivity
import ir.ehsannarmani.encryption.R
import ir.ehsannarmani.encryption.bluetooth.BluetoothBroker
import ir.ehsannarmani.encryption.bluetooth.BluetoothBroker.Companion.SERVICE_UUID
import ir.ehsannarmani.encryption.navigation.Routes
import ir.ehsannarmani.encryption.viewModels.ScreenBViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID


val bluetoothPermissions = arrayOf(
    android.Manifest.permission.BLUETOOTH_SCAN,
    android.Manifest.permission.BLUETOOTH_CONNECT,
    android.Manifest.permission.BLUETOOTH_ADMIN,
    android.Manifest.permission.ACCESS_COARSE_LOCATION,
    android.Manifest.permission.ACCESS_FINE_LOCATION,
)

@SuppressLint("MissingPermission")
@Composable
fun ScreenD(
    viewModel: ScreenDViewModel = viewModel(),
) {
    val appState = LocalAppState.current

    val bluetoothAdapter = remember {
        (appState.context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
            .adapter
    }

    val scanning by viewModel.scanning.collectAsState()
    val scanFailure by viewModel.scanFailure.collectAsState()

    val availableDevices by viewModel.availableDevices.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            if (it.values.all { it }) {
                appState.scope.launch {
                    viewModel.registerReceiver(appState.context)
                    delay(100)
                    viewModel.startScan(bluetoothAdapter)
                }
            }
        }
    )
    LaunchedEffect(Unit) {
        permissionLauncher.launch(bluetoothPermissions)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = {
                if (!scanning) {
                    viewModel.startScan(bluetoothAdapter)
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bluetooth_scan),
                    contentDescription = null
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        AnimatedVisibility(visible = scanning) {
            CircularProgressIndicator()
        }
        Spacer(modifier = Modifier.height(16.dp))
        AnimatedVisibility(visible = scanFailure) {
            Column {
                Text(
                    text = "Could not scan, check your bluetooth.",
                    color = Color(0xFFEF5350),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    viewModel.startScan(bluetoothAdapter)
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Text(text = "Retry")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        AnimatedVisibility(visible = availableDevices.isNotEmpty()) {
            Column {
                Text(text = "Available Devices:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableDevices) {
                        DeviceItem(
                            device = it,
                            onClick = {
                                Toast.makeText(appState.context, "Wait, connecting...", Toast.LENGTH_SHORT).show()
                                appState.scope.launch(Dispatchers.IO) {
                                    MainActivity.broker.connect(it.address) {
                                        appState.scope.launch {
                                            Toast.makeText(
                                                appState.context,
                                                "Connected",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                            appState.navController.navigate(Routes.Transfer.route+"/client")
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceItem(
    device: BluetoothDevice,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(text = device.name.orEmpty(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = device.address, fontSize = 13.sp)
        }
    }
}