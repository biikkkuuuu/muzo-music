package com.example.muzo.data.download

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.example.muzo.core.resolveStreamUrl
import com.example.muzo.data.local.DownloadedSongDao
import com.example.muzo.data.local.DownloadedSongEntity
import com.example.muzo.data.local.MuziDatabase
import com.music.innertube.models.SongItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class SongDownloadManager private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = MuziDatabase.getInstance(appContext)
    private val downloadedSongDao: DownloadedSongDao = database.downloadedSongDao()

    // VideoIds currently being downloaded
    private val _activeDownloads = MutableStateFlow<Set<String>>(emptySet())
    val activeDownloads: StateFlow<Set<String>> = _activeDownloads.asStateFlow()

    // Download progress ratio: 0.0f to 1.0f
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    // Reactive flow of all downloaded videoIds for instant UI state update
    val downloadedVideoIds: StateFlow<Set<String>> = downloadedSongDao.getAllDownloaded()
        .map { list -> list.map { it.videoId }.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    private val downloadJobs = ConcurrentHashMap<String, Job>()

    fun getDownloadDir(): File {
        val dir = File(appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: appContext.filesDir, "MuziOffline")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun isDownloaded(videoId: String): Boolean {
        return downloadedVideoIds.value.contains(videoId)
    }

    fun isDownloadedFlow(videoId: String): Flow<Boolean> {
        return downloadedSongDao.isDownloadedFlow(videoId)
    }

    fun isDownloading(videoId: String): Boolean {
        return _activeDownloads.value.contains(videoId)
    }

    fun toggleDownload(song: SongItem) {
        if (isDownloaded(song.id)) {
            removeDownload(song.id, song.title)
        } else {
            downloadSong(song)
        }
    }

    fun getDownloadedFile(videoId: String): File? {
        val file = File(getDownloadDir(), "$videoId.m4a")
        return if (file.exists() && file.length() > 0) file else null
    }

    fun downloadSong(song: SongItem) {
        val videoId = song.id
        if (isDownloading(videoId)) return

        val job = scope.launch {
            _activeDownloads.update { it + videoId }
            _downloadProgress.update { it + (videoId to 0f) }

            withContext(Dispatchers.Main) {
                Toast.makeText(appContext, "Starting download: ${song.title}", Toast.LENGTH_SHORT).show()
            }

            try {
                // 1. Resolve direct audio stream URL
                val streamUrl = resolveStreamUrl(videoId)
                if (streamUrl.isNullOrBlank()) {
                    throw IllegalStateException("Unable to retrieve audio stream URL")
                }

                val destFile = File(getDownloadDir(), "$videoId.m4a")
                val tempFile = File(getDownloadDir(), "$videoId.tmp")

                // 2. Stream audio bytes from resolved URL
                val conn = (URL(streamUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 25000
                    setRequestProperty("User-Agent", "Muzi/2.0")
                    instanceFollowRedirects = true
                }

                if (conn.responseCode in 200..299) {
                    val totalBytes = conn.contentLengthLong
                    var downloadedBytes = 0L

                    conn.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(8 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloadedBytes += read
                                if (totalBytes > 0) {
                                    val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                    _downloadProgress.update { it + (videoId to progress) }
                                }
                            }
                            output.flush()
                        }
                    }

                    // Rename temp file to destination file atomically
                    if (destFile.exists()) destFile.delete()
                    tempFile.renameTo(destFile)

                    // 3. Save to Room database
                    val artistName = song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown Artist" }
                    val entity = DownloadedSongEntity(
                        videoId = videoId,
                        title = song.title,
                        artist = artistName,
                        thumbnailUrl = song.thumbnail,
                        duration = song.duration,
                        localFilePath = destFile.absolutePath,
                        fileSizeBytes = destFile.length(),
                        downloadedAt = System.currentTimeMillis()
                    )
                    downloadedSongDao.insertDownloaded(entity)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(appContext, "Downloaded: ${song.title}", Toast.LENGTH_SHORT).show()
                    }
                    Log.d("SongDownloadManager", "Successfully downloaded ${song.title} (${destFile.length()} bytes)")
                } else {
                    throw IllegalStateException("HTTP error: ${conn.responseCode}")
                }
            } catch (e: Exception) {
                Log.e("SongDownloadManager", "Download failed for ${song.title}: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Download failed: ${e.localizedMessage ?: "Network error"}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _activeDownloads.update { it - videoId }
                _downloadProgress.update { it - videoId }
                downloadJobs.remove(videoId)
            }
        }

        downloadJobs[videoId] = job
    }

    fun removeDownload(videoId: String, songTitle: String? = null) {
        scope.launch {
            try {
                // Cancel active job if downloading
                downloadJobs[videoId]?.cancel()
                downloadJobs.remove(videoId)
                _activeDownloads.update { it - videoId }
                _downloadProgress.update { it - videoId }

                // Delete local file
                val file = File(getDownloadDir(), "$videoId.m4a")
                if (file.exists()) file.delete()

                val tempFile = File(getDownloadDir(), "$videoId.tmp")
                if (tempFile.exists()) tempFile.delete()

                // Delete from Room
                downloadedSongDao.deleteDownloaded(videoId)

                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Download removed${if (!songTitle.isNullOrBlank()) ": $songTitle" else ""}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("SongDownloadManager", "Failed to remove download: ${e.message}")
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SongDownloadManager? = null

        fun getInstance(context: Context): SongDownloadManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SongDownloadManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
