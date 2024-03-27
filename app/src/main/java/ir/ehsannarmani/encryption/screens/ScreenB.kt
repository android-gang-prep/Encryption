package ir.ehsannarmani.encryption.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ir.ehsannarmani.encryption.LocalAppState
import ir.ehsannarmani.encryption.R
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.ehsannarmani.encryption.MainActivity
import ir.ehsannarmani.encryption.bluetooth.BluetoothBroker.Companion.SERVICE_UUID
import ir.ehsannarmani.encryption.navigation.Routes
import ir.ehsannarmani.encryption.utils.qrcode.rememberQrBitmapPainter
import ir.ehsannarmani.encryption.viewModels.ScreenBViewModel
import ir.ehsannarmani.encryption.viewModels.VOICE_DURATION_LIMIT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.util.UUID

fun checkBluetoothState(context: Context,requestOn:()->Unit){
    val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    if (!adapter.isEnabled){
        Toast.makeText(context, "To send recorded voices, turn on your bluetooth", Toast.LENGTH_SHORT).show()
        requestOn()
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ScreenB(viewModel: ScreenBViewModel = viewModel()) {

    val appState = LocalAppState.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )
    val activityResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {}
    )
    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }
    val borderAnimation = remember {
        Animatable(0f)
    }

    // avoid putting key in shared multiple times
    val qrcodeScanned = remember {
        mutableStateOf(false)
    }
    LaunchedEffect(qrcodeScanned.value) {
        if (qrcodeScanned.value){
            delay(500)
            qrcodeScanned.value = false
        }
    }
    LaunchedEffect(borderAnimation.value) {
        if (borderAnimation.targetValue == 1f) {
            if (viewModel.shouldRelease) {
                borderAnimation.snapTo(
                    0f,
                )
                viewModel.stopRecording()
            }
        }
    }

    val voiceReady by viewModel.voiceReady.collectAsState()
    val qrDialogOpen = remember {
        mutableStateOf(false)
    }

    val qrcodeScannerDialogOpen = remember {
        mutableStateOf(false)
    }
    val bluetoothPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            qrcodeScannerDialogOpen.value = it.values.all { it }
            checkBluetoothState(appState.context, requestOn = {
                activityResultLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            })

        }
    )
    if (qrDialogOpen.value) {
        Dialog(onDismissRequest = { qrDialogOpen.value = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                    painter = rememberQrBitmapPainter(
                        content = viewModel.lastPassword+"#"+viewModel.encryptedFileLength,
                        size = 200.dp,
                        padding = (.5).dp
                    ),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Scan To Receive", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(18.dp))
                Button(onClick = {
                    qrDialogOpen.value = false
                    appState.navController.navigate(Routes.ScreenD.route)
                }, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Pick Device")
                }
            }
        }
    }
    if (qrcodeScannerDialogOpen.value) {
        Dialog(onDismissRequest = { qrcodeScannerDialogOpen.value = false }) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .height(300.dp)
                    .padding(8.dp)
            ) {
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))) {
                    QRCodeScannerScreen(onScan = { key ->
                        val (uuid,voiceSize) = key.split("#")
                        if (!qrcodeScanned.value) {
                            // avoid scanning other qr codes
                            runCatching {
                                UUID.fromString(uuid)
                            }
                                .onSuccess {
                                    Toast.makeText(
                                        appState.context,
                                        "Scan success",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    appState
                                        .sharedPreferences
                                        .edit()
                                        .putString("last_key", uuid)
                                        .putLong("last_voice_size",voiceSize.toLong())
                                        .apply()
                                    qrcodeScannerDialogOpen.value = false
                                    activityResultLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                        putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                                    })
                                    qrcodeScanned.value = true
                                    appState.scope.launch(Dispatchers.IO) {
                                        MainActivity.broker.startServer(onConnect = {
                                            appState.scope.launch {
                                                Toast.makeText(
                                                    appState.context,
                                                    "Connected",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                appState.navController.navigate(Routes.Transfer.route+"/server")
                                            }
                                        })
                                    }
                                }
                        }
                    })
                }
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(115.dp)
                    .clip(CircleShape)
                    .rotate(-90f)
                    .border(
                        3.dp, brush = Brush.sweepGradient(
                            borderAnimation.value to Color(0xFFFFA726),
                            borderAnimation.value to Color.Transparent,
                        ),
                        CircleShape
                    ), contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .rotate(90f)
                    .border(1.dp, color = Color.White, CircleShape)
                    .pointerInteropFilter {
                        when (it.action) {
                            MotionEvent.ACTION_DOWN -> {
                                appState.scope.launch {
                                    borderAnimation.animateTo(
                                        1f,
                                        animationSpec = tween(
                                            VOICE_DURATION_LIMIT.inWholeMilliseconds.toInt(),
                                            easing = LinearEasing
                                        )
                                    )
                                }
                                viewModel.startRecording(appState.context.cacheDir)
                            }

                            MotionEvent.ACTION_UP -> {
                                appState.scope.launch {
                                    borderAnimation.animateTo(
                                        0f,
                                        animationSpec = tween(500, easing = LinearEasing)
                                    )
                                }
                                viewModel.stopRecording()
                            }
                        }
                        true
                    }, contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_mic),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }
            Button(onClick = {
                viewModel.encrypt()
                appState.sharedPreferences
                    .edit()
                    .putString("latest_voice",viewModel.encryptedPath)
                    .apply()
                qrDialogOpen.value = true
            }, enabled = voiceReady) {
                Text(text = "Send")
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(22.dp)
        ) {
            IconButton(onClick = {
                bluetoothPermissionsLauncher.launch(bluetoothPermissions)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_camera),
                    contentDescription = null
                )
            }
        }
    }
}
