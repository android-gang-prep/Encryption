package ir.ehsannarmani.encryption.utils.qrcode

import android.content.Context
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage


@OptIn(ExperimentalGetImage::class)
class QRAnalyzer(private val context: Context,private val onScan:(String)->Unit):ImageAnalysis.Analyzer {
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    override fun analyze(image: ImageProxy) {
        image.image?.let {img->
            scanner
                .process(InputImage.fromMediaImage(img,image.imageInfo.rotationDegrees))
                .addOnSuccessListener { barcode->
                    barcode?.takeIf { it.isNotEmpty() }
                        ?.mapNotNull { it.rawValue }
                        ?.joinToString(",")
                        ?.let(onScan)
                }.addOnCompleteListener {
                    image.close()
                }
        }
    }
}