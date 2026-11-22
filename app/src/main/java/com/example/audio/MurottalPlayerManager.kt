package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import com.example.data.QariCatalog
import com.example.data.local.AyahEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class AudioPlayerState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val surahNomor: Int = 0,
    val surahNamaLatin: String = "",
    val currentAyahNomor: Int = 0,
    val totalAyahsInQueue: Int = 0,
    val qariId: String = "01",
    val repeatCurrentAyah: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val isDownloadingAudio: Boolean = false,
    val audioDownloadProgress: Float = 0f,
    val errorMessage: String? = null
)

class MurottalPlayerManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _state = MutableStateFlow(AudioPlayerState())
    val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var queue: List<AyahEntity> = emptyList()
    private var currentIndex: Int = 0
    private var continuousMode: Boolean = true

    private fun getCacheFile(qariId: String, surahNomor: Int, ayahNomor: Int): File {
        val dir = File(context.filesDir, "murottal/$qariId")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "${surahNomor}_${ayahNomor}.mp3")
    }

    fun isSurahAudioCachedOffline(qariId: String, surahNomor: Int, jumlahAyat: Int): Boolean {
        if (jumlahAyat <= 0) return false
        val dir = File(context.filesDir, "murottal/$qariId")
        if (!dir.exists()) return false
        for (a in 1..jumlahAyat) {
            val f = File(dir, "${surahNomor}_${a}.mp3")
            if (!f.exists() || f.length() < 512L) return false
        }
        return true
    }

    fun playSurahFrom(
        ayahs: List<AyahEntity>,
        startAyahNomor: Int,
        qariId: String,
        continuous: Boolean = true
    ) {
        if (ayahs.isEmpty()) return
        val index = ayahs.indexOfFirst { it.nomorAyat == startAyahNomor }.coerceAtLeast(0)
        queue = ayahs
        currentIndex = index
        continuousMode = continuous
        playIndex(index, qariId)
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            _state.value = _state.value.copy(isPlaying = false)
        } else {
            mp.start()
            _state.value = _state.value.copy(isPlaying = true)
        }
    }

    fun nextAyah() {
        if (queue.isEmpty()) return
        if (currentIndex + 1 < queue.size) {
            currentIndex++
            playIndex(currentIndex, _state.value.qariId)
        }
    }

    fun previousAyah() {
        if (queue.isEmpty()) return
        if (currentIndex - 1 >= 0) {
            currentIndex--
            playIndex(currentIndex, _state.value.qariId)
        }
    }

    fun toggleRepeatAyah() {
        _state.value = _state.value.copy(repeatCurrentAyah = !_state.value.repeatCurrentAyah)
    }

    fun cyclePlaybackSpeed() {
        val next = when (_state.value.playbackSpeed) {
            1.0f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 0.75f
            else -> 1.0f
        }
        _state.value = _state.value.copy(playbackSpeed = next)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        mp.playbackParams = mp.playbackParams.setSpeed(next)
                    }
                }
            }
        }
    }

    fun updateQari(newQariId: String) {
        val current = _state.value
        _state.value = current.copy(qariId = newQariId)
        if (current.isActive && queue.isNotEmpty()) {
            playIndex(currentIndex, newQariId)
        }
    }

    fun stop() {
        releasePlayer()
        queue = emptyList()
        _state.value = AudioPlayerState(
            qariId = _state.value.qariId,
            playbackSpeed = _state.value.playbackSpeed
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun downloadSurahAudioOffline(ayahs: List<AyahEntity>, qariId: String, onFinished: (Boolean) -> Unit) {
        if (ayahs.isEmpty() || _state.value.isDownloadingAudio) return
        scope.launch {
            _state.value = _state.value.copy(
                isDownloadingAudio = true,
                audioDownloadProgress = 0f,
                errorMessage = null
            )
            var successCount = 0
            withContext(Dispatchers.IO) {
                ayahs.forEachIndexed { idx, ayah ->
                    val target = getCacheFile(qariId, ayah.surahNomor, ayah.nomorAyat)
                    val ok = if (target.exists() && target.length() > 512L) {
                        true
                    } else {
                        val primaryUrl = ayah.audioForQari(qariId).ifBlank {
                            QariCatalog.buildFallbackAyahAudioUrl(qariId, ayah.surahNomor, ayah.nomorAyat)
                        }
                        downloadToFile(primaryUrl, target) ||
                            downloadToFile(
                                QariCatalog.buildFallbackAyahAudioUrl(qariId, ayah.surahNomor, ayah.nomorAyat),
                                target
                            )
                    }
                    if (ok) successCount++
                    _state.value = _state.value.copy(
                        audioDownloadProgress = (idx + 1).toFloat() / ayahs.size.toFloat()
                    )
                }
            }
            val allOk = successCount == ayahs.size
            _state.value = _state.value.copy(
                isDownloadingAudio = false,
                audioDownloadProgress = if (allOk) 1f else 0f,
                errorMessage = if (!allOk) "Gagal mengunduh seluruh audio. Periksa koneksi internet Anda." else null
            )
            onFinished(allOk)
        }
    }

    private fun downloadToFile(url: String, dest: File): Boolean {
        return try {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return false
                val bytes = resp.body?.bytes() ?: return false
                if (bytes.size < 512) return false
                dest.writeBytes(bytes)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun playIndex(index: Int, qariId: String) {
        if (index !in queue.indices) return
        val ayah = queue[index]
        releasePlayer()

        _state.value = _state.value.copy(
            isActive = true,
            isPlaying = false,
            isLoading = true,
            surahNomor = ayah.surahNomor,
            surahNamaLatin = ayah.surahNamaLatin,
            currentAyahNomor = ayah.nomorAyat,
            totalAyahsInQueue = queue.size,
            qariId = qariId,
            errorMessage = null
        )

        scope.launch {
            val localFile = getCacheFile(qariId, ayah.surahNomor, ayah.nomorAyat)
            val dataSource = if (localFile.exists() && localFile.length() > 512L) {
                localFile.absolutePath
            } else {
                ayah.audioForQari(qariId).ifBlank {
                    QariCatalog.buildFallbackAyahAudioUrl(qariId, ayah.surahNomor, ayah.nomorAyat)
                }
            }

            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(dataSource)
                    setOnPreparedListener { preparedMp ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            runCatching {
                                preparedMp.playbackParams =
                                    preparedMp.playbackParams.setSpeed(_state.value.playbackSpeed)
                            }
                        }
                        preparedMp.start()
                        _state.value = _state.value.copy(
                            isLoading = false,
                            isPlaying = true
                        )
                    }
                    setOnCompletionListener {
                        if (_state.value.repeatCurrentAyah) {
                            playIndex(currentIndex, _state.value.qariId)
                        } else if (continuousMode && currentIndex + 1 < queue.size) {
                            currentIndex++
                            playIndex(currentIndex, _state.value.qariId)
                        } else {
                            _state.value = _state.value.copy(isPlaying = false)
                        }
                    }
                    setOnErrorListener { _, _, _ ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            isPlaying = false,
                            errorMessage = "Audio tidak dapat diputar secara offline. Unduh audio surat saat terhubung internet."
                        )
                        true
                    }
                    prepareAsync()
                }
                mediaPlayer = mp

                // Background cache if streaming from network
                if (!localFile.exists() && dataSource.startsWith("http")) {
                    launch(Dispatchers.IO) {
                        downloadToFile(dataSource, localFile)
                    }
                }
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isPlaying = false,
                    errorMessage = "Gagal memutar audio murottal."
                )
            }
        }
    }

    private fun releasePlayer() {
        runCatching {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        }
        mediaPlayer = null
    }
}
