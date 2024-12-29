package ch.romix.nfcplayer

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.romix.nfcplayer.ui.theme.NFCPlayerTheme
import coil3.ImageLoader
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    onBackward: () -> Unit,
    onForward: () -> Unit,
    onPause: () -> Unit,
    onPlay: () -> Unit,
    loadProgress: () -> Float,
    imageLoader: ImageLoader,
    imageUri: Uri
) {
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var pulseRateMs by remember { mutableLongStateOf(200L) }
    var isPlaying by remember { mutableStateOf(true) }

    LaunchedEffect(pulseRateMs) {
        while (currentProgress < 1) {
            delay(pulseRateMs)
            currentProgress = loadProgress()
        }
    }

    fun handlePlayPauseButton() {
        if (isPlaying) onPause() else onPlay()
        isPlaying = !isPlaying
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = imageUri,
            imageLoader = imageLoader,
            contentDescription = "Hintergrund",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LinearProgressIndicator(progress = currentProgress, color = Color.Magenta)
            Row {
                Button(
                    onClick = { onBackward() },
                    modifier = Modifier.padding(6.dp),
                    colors = ButtonDefaults.buttonColors(Color.Magenta)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FastRewind,
                        contentDescription = "Rückwärts",
                    )
                }
                Button(
                    onClick = { handlePlayPauseButton() },
                    modifier = Modifier.padding(6.dp),
                    colors = ButtonDefaults.buttonColors(Color.Magenta)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Start",
                    )
                }
                Button(
                    onClick = { onForward() },
                    modifier = Modifier.padding(6.dp),
                    colors = ButtonDefaults.buttonColors(Color.Magenta)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FastForward,
                        contentDescription = "Vorwärts",
                    )
                }
            }
        }

    }
}

@Preview(showBackground = true)
@Composable
fun PlayerScreenPreview() {
    NFCPlayerTheme {
        PlayerScreen(
            onForward = {},
            onBackward = {},
            loadProgress = { 0.3f },
            imageLoader = createNextcloudImageLoader(LocalContext.current, Nextcloud("","","","")),
            onPause = {},
            onPlay = {},
            imageUri = Uri.parse("https://nextcloud.romix.ch/remote.php/dav/files/roman/%c3%9csi%20Familie/Diverses/Weihnachtsgeschichten/Opa%20-%20Der%20Rabe%20im%20Schnee.jpg")
        )
    }
}
