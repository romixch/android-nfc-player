package ch.romix.nfcplayer

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.Parcelable
import android.util.Base64
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceManager
import ch.romix.nfcplayer.ui.theme.NFCPlayerTheme
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import com.thegrizzlylabs.sardineandroid.impl.SardineException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var nextcloud: Nextcloud? = null
    private var mediaPlayer: MediaPlayer? = null
    private var mediaPlayerIsPrepared = false
    private val sardine: Sardine = OkHttpSardine()
    private var resources: List<DavResource>? = null
    private var storyName: String? = null
    private var pausedPosition = 0
    private var pausedDuration = 0
    private var resumePosition: Int? = 0

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("storyName", storyName)
        mediaPlayer?.currentPosition?.let { outState.putInt("resumePosition", it) }
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mediaPlayer = MediaPlayer()
        mediaPlayer?.setAudioAttributes(
            AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA).build()
        )

        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { rawMessages ->
                storyName = getStoryName(rawMessages)
                tryToPlay()

                setContent {
                    NFCPlayerTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            LoadingScreen()
                        }
                    }
                }
            }
        } else if (savedInstanceState !== null) {
            storyName = savedInstanceState.getString("storyName")
            resumePosition = savedInstanceState.getInt("resumePosition")
            tryToPlay()
        } else {
            showStartScreen { handleOnSettingsClicked() }
        }
    }

    override fun onStart() {
        super.onStart()
        val nextcloud = readNextcloudPreferences()
        if (nextcloud == null) {
            handleOnSettingsClicked()
        } else {
            this.nextcloud = nextcloud
            sardine.setCredentials(nextcloud.user, nextcloud.password)
            loadFileList()
        }
    }

    private fun handleOnSettingsClicked() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun readNextcloudPreferences(): Nextcloud? {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this).all

        val nextcloudURL = preferences["nextcloud_url"] as String?
        val nextcloudUser = preferences["nextcloud_user"] as String?
        val nextcloudPassword = preferences["nextcloud_password"] as String?
        val nextcloudFolder = preferences["nextcloud_folder"] as String?
        if (nextcloudURL != null && nextcloudUser != null && nextcloudPassword != null && nextcloudFolder != null) {
            return Nextcloud(nextcloudURL, nextcloudUser, nextcloudPassword, nextcloudFolder)
        } else {
            return null
        }

    }

    private fun loadFileList() {
        nextcloud?.let {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val resources =
                        sardine.list("${it.host}/remote.php/dav/files/${it.user}/${it.folder}")
                    withContext(Dispatchers.Main) {
                        this@MainActivity.resources = resources
                        tryToPlay()
                    }
                } catch (error: SardineException) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Verbindung fehlgeschlagen",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun tryToPlay() {
        nextcloud?.let { nextcloud ->
            storyName?.let { storyName ->
                resources?.let { res ->
                    GlobalScope.launch(Dispatchers.IO) {
                        val storyRes =
                            res.filter { res -> res.name.startsWith(storyName, ignoreCase = true) }
                        val audioRes = storyRes.find { res -> isAudioFile(res) }
                        val imageRes = storyRes.find { res -> isImageFile(res) }

                        val audioUri = Uri.parse("${nextcloud.host}${audioRes?.href}")
                        val imageUri = Uri.parse("${nextcloud.host}${imageRes?.href}")
                        audioUri?.let { url -> playAudio(nextcloud, url) }

                        withContext(Dispatchers.Main) {
                            setContent {
                                NFCPlayerTheme {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        color = MaterialTheme.colorScheme.background
                                    ) {
                                        PlayerScreen(
                                            onBackward = { handleBackward() },
                                            onForward = { handleForward() },
                                            onPause = { handlePause() },
                                            onPlay = { handlePlay() },
                                            loadProgress = { handleLoadProgress() },
                                            imageLoader = createNextcloudImageLoader(
                                                LocalContext.current,
                                                nextcloud
                                            ),
                                            imageUri = imageUri
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isImageFile(res: DavResource) =
        res.contentType.contains("image", ignoreCase = true)


    private fun isAudioFile(res: DavResource) =
        res.contentType.contains(
            "audio", ignoreCase = true
        ) || res.contentType.contains(
            "mp4", ignoreCase = true
        )

    private fun playAudio(nextcloud: Nextcloud, audioLink: Uri) {
        val toEncrypt: ByteArray = ("${nextcloud.user}:${nextcloud.password}").toByteArray()
        val encoded: String = Base64.encodeToString(toEncrypt, Base64.DEFAULT)
        val headers: MutableMap<String, String> = HashMap()
        headers["Authorization"] = "Basic $encoded"

        mediaPlayer?.apply {
            setDataSource(this@MainActivity, audioLink, headers)
            prepareAsync()
            setOnPreparedListener {
                mediaPlayerIsPrepared = true
                start()
            }
            setOnCompletionListener {
                pause()
                seekTo(0)
                pausedPosition = 0
                pausedDuration = duration
            }
        }
    }

    private fun handleBackward() {
        mediaPlayer?.apply {

            if (mediaPlayerIsPrepared && currentPosition > 10_000) {
                seekTo(currentPosition - 10_000)
                pausedPosition = currentPosition - 10_000
            } else seekTo(
                0
            )
        }
    }

    private fun handleForward() {
        mediaPlayer?.apply {
            if (mediaPlayerIsPrepared) {
                seekTo(currentPosition + 10_000)
                pausedPosition = currentPosition + 10_000
            }
        }
    }

    private fun handlePause() {
        mediaPlayer?.apply {
            if (mediaPlayerIsPrepared) {
                pausedPosition = currentPosition
                pausedDuration = duration
                pause()
            }
        }
    }

    private fun handlePlay() {
        mediaPlayer?.apply {
            seekTo(pausedPosition)
            start()
        }
    }

    private fun handleLoadProgress(): Float {
        mediaPlayer?.apply {
            if (mediaPlayerIsPrepared && duration > 0) {
                return 1f / duration * currentPosition
            } else if (pausedDuration > 0) {
                return 1f / pausedDuration * pausedPosition
            }
        }
        return 0f
    }

    private fun getStoryName(rawMessages: Array<out Parcelable>): String? {
        val storyName = rawMessages.map { it as NdefMessage }.flatMap { ndefMessage ->
            ndefMessage.records.map { record ->
                Pair(
                    String(record.type), String(record.payload)
                )
            }
        }.find { it.first == "text/storyname" }
        return storyName?.second
    }

    private fun showStartScreen(onSettingsClicked: () -> Unit) {
        setContent {
            NFCPlayerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    StartScreen(onSettingsClicked)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }

}

data class Nextcloud(val host: String, val user: String, val password: String, val folder: String)