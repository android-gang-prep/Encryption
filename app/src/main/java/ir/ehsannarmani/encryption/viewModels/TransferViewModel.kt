package ir.ehsannarmani.encryption.viewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import ir.ehsannarmani.encryption.utils.EncryptionUtils
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.UUID

class TransferViewModel: ViewModel() {

    fun decrypt(
        password:String,
        encryptedPath:String,
        pathToDecrypt:String,
        onFinish:()->Unit,
    ){
        EncryptionUtils.decrypt(
            path = encryptedPath,
            outputPath = pathToDecrypt,
            password,
        )
        File(encryptedPath).delete()
        onFinish()
    }
}