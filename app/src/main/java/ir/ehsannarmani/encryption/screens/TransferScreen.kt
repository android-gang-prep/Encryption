package ir.ehsannarmani.encryption.screens

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.ehsannarmani.encryption.LocalAppState
import ir.ehsannarmani.encryption.MainActivity
import ir.ehsannarmani.encryption.viewModels.TransferViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.Buffer
import java.nio.ByteBuffer
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.ehsannarmani.encryption.R

enum class SocketType { Server, Client }

@Composable
fun TransferScreen(
    socketType: SocketType,
    viewModel: TransferViewModel = viewModel()
) {

    val appState = LocalAppState.current


    val progress = remember {
        mutableFloatStateOf(0f)
    }

    val decryptedPath = remember {
        mutableStateOf<String?>(null)
    }
    val decrypted = remember {
        mutableStateOf(false)
    }
    val receivedFile = remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {
        val broker = MainActivity.broker
        when (socketType) {
            SocketType.Client -> {
                appState.scope.launch(Dispatchers.IO) {
                    val fileToSend = File(appState.sharedPreferences.getString("latest_voice", ""))
                    val fis = FileInputStream(fileToSend)
                    val ops = broker.clientSocket?.outputStream
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    var totalBytesSent = 0L
                    val totalFileSize = fileToSend.length()
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        ops?.write(buffer, 0, bytesRead)
                        totalBytesSent += bytesRead

                        progress.value = (totalBytesSent.toFloat() / totalFileSize) * 100
                    }
                    fis.close()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(appState.context, "Sent", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            SocketType.Server -> {
                val voiceSize = appState.sharedPreferences.getLong("last_voice_size", 0)
                appState.scope.launch(Dispatchers.IO) {
                    var totalBytesReceived = 0
                    val dirToSave = File(
                        appState.context.cacheDir,
                        "received-${System.currentTimeMillis()}.mp3.crypt"
                    )
                    receivedFile.value = dirToSave.path
                    val fos = FileOutputStream(dirToSave)
                    val buffer = ByteArray(1024)
                    while (true) {
                        try {
                            val byteCount = broker.clientSocket?.inputStream?.read(buffer)
                            if (byteCount == -1) break

                            fos.write(buffer, 0, byteCount!!)
                            totalBytesReceived += byteCount
                            progress.value = (totalBytesReceived * 100.0f) / voiceSize
                        }catch (_:Exception){}
                    }
                    fos.close()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(appState.context, "Received", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (socketType == SocketType.Server) {
            Text(
                text = if (progress.value == 100f) "Received" else "Receiving...",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = if (progress.value == 100f) "Sent" else "Sending...",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = .6f)),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .fillMaxWidth(progress.value / 100)
                    .background(
                        Color(0xFF2BB351)
                    )
            )
            Text(
                text = progress.value.toString() + "%",
                fontSize = 9.sp,
                color = Color.White,
                modifier = Modifier.align(
                    Alignment.Center
                )
            )
        }
        if (socketType == SocketType.Server) {
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedVisibility(visible = progress.value == 100f && !decrypted.value) {
                Button(onClick = {
                    val password = appState
                        .sharedPreferences
                        .getString("last_key", "")
                    val dirToDecrypt =
                        File(appState.context.cacheDir, "voice-${System.currentTimeMillis()}.mp3")
                    decryptedPath.value = dirToDecrypt.path
                    viewModel.decrypt(
                        password = password!!,
                        encryptedPath = receivedFile.value!!,
                        pathToDecrypt = dirToDecrypt.path,
                        onFinish = {
                            decrypted.value = true
                        }
                    )
                }) {
                    Text(text = "Decrypt")
                }
            }
            AnimatedVisibility(visible = progress.value == 100f && decrypted.value) {
                FloatingActionButton(onClick = {
                    MediaPlayer()
                        .apply {
                            setDataSource(decryptedPath.value!!)
                            prepare()
                            start()
                        }

                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_play),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}