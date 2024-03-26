package ir.ehsannarmani.encryption.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.fragment.app.FragmentActivity
import ir.ehsannarmani.encryption.LocalAppState
import ir.ehsannarmani.encryption.MainActivity
import ir.ehsannarmani.encryption.R
import ir.ehsannarmani.encryption.navigation.Routes
import ir.ehsannarmani.encryption.utils.BiometricUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun ScreenA() {

    val appState = LocalAppState.current
    val activity = appState.context as FragmentActivity
    val biometric = remember {
        BiometricUtils(activity)
    }
    val permissionGranted = remember {
        ContextCompat.checkSelfPermission(activity,android.Manifest.permission.CAMERA) == PERMISSION_GRANTED
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setJpegQuality(50)
            .build()
    }
    val captureRequest = remember {
        mutableStateOf(false)
    }
    val cameraProvider = remember {
        mutableStateOf<ProcessCameraProvider?>(null)
    }
    LaunchedEffect(captureRequest.value) {
        if (captureRequest.value){
            launch {
                delay(500)
                imageCapture.capture(
                    activity,
                    onFinish = {
                        captureRequest.value = false
                        cameraProvider.value?.unbindAll()
                        appState.navController.navigate(Routes.ScreenB.route){
                            popUpTo(0){
                                inclusive = false
                            }
                        }
                    }
                )
            }
        }
    }
    Column(
        modifier=Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!permissionGranted){
            Column {
                Text(text = "Camera Permission Denied!", color = Color(0xFFEF5350))
                Spacer(modifier = Modifier.height(22.dp))
            }
        }
        if (permissionGranted && captureRequest.value){
            HiddenCamera(
                imageCapture = imageCapture,
                onProviderReady = {
                    cameraProvider.value = it
                }
            )
        }
        Text(text = "Use Your Fingerprint for Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_finger),
            contentDescription = null,
            modifier= Modifier
                .size(150.dp)
                .padding(22.dp)
                .clip(CircleShape)
                .clickable {
                    if (biometric.canAuthenticate()) {
                        biometric.authenticate {
                            if (it) {
                                Toast
                                    .makeText(
                                        activity,
                                        "Authentication Success",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                                appState.navController.navigate(Routes.ScreenB.route){
                                    popUpTo(0){
                                        inclusive = false
                                    }
                                }
                            } else {
                                Toast
                                    .makeText(
                                        activity,
                                        "Authentication Failed",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                                captureRequest.value = true
                            }
                        }
                    } else {
                        Toast
                            .makeText(
                                activity,
                                "Unable to authenticate with biometric",
                                Toast.LENGTH_SHORT
                            )
                            .show()
                        captureRequest.value = true
                    }
                }
        )
    }
}

@Composable
private fun HiddenCamera(
    imageCapture: ImageCapture,
    onProviderReady:(ProcessCameraProvider)->Unit,
) {
    val appState = LocalAppState.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraSelector = CameraSelector
        .Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
        .build()

    val previewView = remember {
        PreviewView(appState.context)
    }
    val preview = remember {
        Preview.Builder().build()
    }
    LaunchedEffect(Unit){
        val cameraProvider = appState.context.getCameraProvider()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
        onProviderReady(cameraProvider)

        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    Box(modifier = Modifier
        .size(1.dp)
        .alpha(0f), contentAlignment = Alignment.BottomCenter){
        AndroidView(factory = { previewView })
    }
}
suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this)
        .also {
            it.addListener({
                continuation.resume(
                    it.get()
                )
            },ContextCompat.getMainExecutor(this))
        }
}
fun ImageCapture.capture(
    context: Context,
    onFinish:(Boolean)->Unit,
){
    val output = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"image-${System.currentTimeMillis()}.jpg")
    output.createNewFile()
    val captureOptions = ImageCapture.OutputFileOptions
        .Builder(
            output
        )

        .build()
    takePicture(
        captureOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                val uri = FileProvider.getUriForFile(
                    context,
                    context.packageName + ".provider",
                    output
                )
                uri?.let {
                    MediaScannerConnection
                        .scanFile(
                            context,
                            arrayOf(
                                output.path
                            ),
                            arrayOf("image/jpeg")
                        ) { path, uri ->

                        }
                    Toast.makeText(context, "You are captured!", Toast.LENGTH_SHORT).show()
                    onFinish(true)
                }

            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(context, "Image capture failed.", Toast.LENGTH_SHORT).show()
                onFinish(false)
            }

        }
    )
}
