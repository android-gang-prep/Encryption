package ir.ehsannarmani.encryption.utils

import android.app.Activity
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ir.ehsannarmani.encryption.MainActivity


class BiometricUtils(private val activity: FragmentActivity) {
    fun canAuthenticate():Boolean{
        val manager = BiometricManager.from(activity)
        return when(manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)){
            BiometricManager.BIOMETRIC_SUCCESS-> true
            else -> false
        }
    }

    private val prompt by lazy {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate")
            .setDescription("Use your fingerprint to login")
            .setNegativeButtonText("Cancel")
            .build()
    }
    fun authenticate(
        onResult:(Boolean)->Unit
    ){
        val authenticator = BiometricPrompt(activity,ContextCompat.getMainExecutor(activity),object :
            BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onResult(false)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(false)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(true)
            }
        })
        authenticator.authenticate(prompt)
    }
}