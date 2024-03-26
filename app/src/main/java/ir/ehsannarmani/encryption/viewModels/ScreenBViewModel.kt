package ir.ehsannarmani.encryption.viewModels

import android.media.MediaRecorder
import androidx.lifecycle.ViewModel
import ir.ehsannarmani.encryption.utils.EncryptionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

val VOICE_DURATION_LIMIT = 60.seconds

class ScreenBViewModel: ViewModel() {

    private var startTime:Long = 0L
    private lateinit var outputFile:File
    private lateinit var mediaRecorder: MediaRecorder

    private val _voiceReady = MutableStateFlow(false)
    val voiceReady = _voiceReady.asStateFlow()

    lateinit var lastPassword:String

    fun startRecording(dir:File){
        startTime = System.currentTimeMillis()
        outputFile = File(dir, "voice-${System.currentTimeMillis()}.mp3")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(outputFile.path)
            setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
            setAudioEncodingBitRate(96000)
            setAudioSamplingRate(44100)
            prepare()
        }
        mediaRecorder.start()
    }
    fun stopRecording(){
        runCatching {
            mediaRecorder.stop()
            mediaRecorder.release()
        }
        _voiceReady.update { true }
    }

    fun encrypt(){
         lastPassword = UUID.randomUUID().toString()
        EncryptionUtils.encrypt(
            outputFile.path,
            lastPassword
        )
        outputFile.delete()
        _voiceReady.update { false }
    }

    val shouldRelease:Boolean get() {
        return (System.currentTimeMillis()- startTime) >= VOICE_DURATION_LIMIT.inWholeMilliseconds
    }
}