package ir.ehsannarmani.encryption.screens

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import ir.ehsannarmani.encryption.utils.qrcode.QRAnalyzer

@Composable
fun QRCodeScannerScreen(onScan:(String)->Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFeature = remember {
        ProcessCameraProvider.getInstance(context)
    }
    val previewView = remember {
        PreviewView(context)
    }
    val preview = remember {
        Preview.Builder().build()
    }
    val imageAnalysis = remember {
        ImageAnalysis.Builder().build()
    }
    val cameraSelector = remember {
        CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
    }
    LaunchedEffect(Unit) {

    }
    DisposableEffect(Unit) {
        val feature = cameraProviderFeature.get()
        feature.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis
        )
        onDispose {
            feature.unbindAll()
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
        AndroidView(
            modifier=Modifier.fillMaxSize(),
            factory = {
                preview.setSurfaceProvider(previewView.surfaceProvider)
                imageAnalysis.setAnalyzer(
                    ContextCompat.getMainExecutor(context),
                    QRAnalyzer(
                        context = context,
                        onScan = onScan
                    )
                )
                previewView
            }
        )
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(.6f))
        )
    }
}

