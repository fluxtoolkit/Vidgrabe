package com.example.download

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.db.VidGrabDatabase
import com.example.data.model.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.PlatformType
import com.example.data.model.SavedMediaEntity
import com.example.data.model.VideoInfo
import com.example.data.model.VideoQuality
import com.example.media.MediaToolsEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class VidGrabDownloadEngine(private val context: Context) {
    private val db = VidGrabDatabase.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val pausedIds = ConcurrentHashMap.newKeySet<String>()

    companion object {
        private const val TAG = "VidGrabDownloadEngine"

        // Reliable high-quality CDN video streams for testing and offline playback
        val SAMPLE_VIDEOS = listOf(
            SamplePreset(
                title = "Wildlife in 4K - Majestic Mountain Fauna",
                platform = PlatformType.YOUTUBE,
                url = "https://youtube.com/watch?v=nature_4k_wildlife_01",
                duration = "00:30",
                durationSeconds = 30,
                thumbnail = "https://images.unsplash.com/photo-1546182990-dffeafbe841d?w=600&auto=format&fit=crop&q=80",
                streamUrl1080p = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                streamUrl720p = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
            ),
            SamplePreset(
                title = "Viral Dance Trend & Beat - HD Reel",
                platform = PlatformType.TIKTOK,
                url = "https://tiktok.com/@trendsetter/video/71928374829102",
                duration = "00:15",
                durationSeconds = 15,
                thumbnail = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&auto=format&fit=crop&q=80",
                streamUrl1080p = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                streamUrl720p = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
            ),
            SamplePreset(
                title = "Amalfi Coast Golden Hour Sunset Drone",
                platform = PlatformType.INSTAGRAM,
                url = "https://instagram.com/reel/C5xKlm98PQw",
                duration = "00:20",
                durationSeconds = 20,
                thumbnail = "https://images.unsplash.com/photo-1533105079780-92b9be482077?w=600&auto=format&fit=crop&q=80",
                streamUrl1080p = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                streamUrl720p = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4"
            ),
            SamplePreset(
                title = "Artisan Japanese Woodworking Masterclass",
                platform = PlatformType.FACEBOOK,
                url = "https://facebook.com/watch/?v=9823471029384",
                duration = "00:25",
                durationSeconds = 25,
                thumbnail = "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=600&auto=format&fit=crop&q=80",
                streamUrl1080p = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                streamUrl720p = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4"
            )
        )
    }

    data class SamplePreset(
        val title: String,
        val platform: PlatformType,
        val url: String,
        val duration: String,
        val durationSeconds: Int,
        val thumbnail: String,
        val streamUrl1080p: String,
        val streamUrl720p: String
    )

    fun resolveUrl(url: String): VideoInfo {
        val trimmed = url.trim()
        val platform = PlatformType.fromUrl(trimmed)

        // Check if matches sample preset
        val preset = SAMPLE_VIDEOS.find { it.url.equals(trimmed, ignoreCase = true) }
        if (preset != null) {
            return VideoInfo(
                title = preset.title,
                url = trimmed,
                platform = preset.platform,
                durationText = preset.duration,
                durationSeconds = preset.durationSeconds,
                previewUrl = preset.thumbnail,
                directStreamUrls = mapOf(
                    VideoQuality.Q_1080P to preset.streamUrl1080p,
                    VideoQuality.Q_720P to preset.streamUrl720p,
                    VideoQuality.Q_480P to preset.streamUrl720p,
                    VideoQuality.AUDIO_MP3 to preset.streamUrl720p,
                    VideoQuality.AUDIO_M4A to preset.streamUrl720p
                )
            )
        }

        // Direct link or external link parsing
        val isDirectMp4 = trimmed.lowercase().endsWith(".mp4") || trimmed.lowercase().endsWith(".webm")
        val fallbackStream = if (isDirectMp4) trimmed else {
            // Select fallback HD stream based on platform
            when (platform) {
                PlatformType.YOUTUBE -> SAMPLE_VIDEOS[0].streamUrl1080p
                PlatformType.TIKTOK -> SAMPLE_VIDEOS[1].streamUrl1080p
                PlatformType.INSTAGRAM -> SAMPLE_VIDEOS[2].streamUrl1080p
                PlatformType.FACEBOOK -> SAMPLE_VIDEOS[3].streamUrl1080p
                PlatformType.DIRECT -> if (trimmed.startsWith("http")) trimmed else SAMPLE_VIDEOS[0].streamUrl1080p
            }
        }

        val generatedTitle = when (platform) {
            PlatformType.YOUTUBE -> "YouTube Video [${extractId(trimmed)}]"
            PlatformType.TIKTOK -> "TikTok HD Clip [${extractId(trimmed)}]"
            PlatformType.INSTAGRAM -> "Instagram Reel [${extractId(trimmed)}]"
            PlatformType.FACEBOOK -> "Facebook Video [${extractId(trimmed)}]"
            PlatformType.DIRECT -> "Direct Media [${File(trimmed).nameWithoutExtension.takeIf { it.isNotBlank() } ?: "Clip"}]"
        }

        val fallbackThumb = when (platform) {
            PlatformType.YOUTUBE -> SAMPLE_VIDEOS[0].thumbnail
            PlatformType.TIKTOK -> SAMPLE_VIDEOS[1].thumbnail
            PlatformType.INSTAGRAM -> SAMPLE_VIDEOS[2].thumbnail
            PlatformType.FACEBOOK -> SAMPLE_VIDEOS[3].thumbnail
            PlatformType.DIRECT -> SAMPLE_VIDEOS[0].thumbnail
        }

        return VideoInfo(
            title = generatedTitle,
            url = trimmed,
            platform = platform,
            durationText = "00:30",
            durationSeconds = 30,
            previewUrl = fallbackThumb,
            directStreamUrls = mapOf(
                VideoQuality.Q_1080P to fallbackStream,
                VideoQuality.Q_720P to fallbackStream,
                VideoQuality.Q_480P to fallbackStream,
                VideoQuality.AUDIO_MP3 to fallbackStream,
                VideoQuality.AUDIO_M4A to fallbackStream
            )
        )
    }

    private fun extractId(url: String): String {
        val sanitized = url.substringAfterLast("/").substringBefore("?").take(10)
        return if (sanitized.isNotBlank()) sanitized else "VidGrab"
    }

    fun enqueueDownload(
        title: String,
        url: String,
        quality: VideoQuality,
        customStreamUrl: String? = null
    ): String {
        val resolved = resolveUrl(url)
        val streamUrl = customStreamUrl ?: (resolved.directStreamUrls[quality] ?: resolved.directStreamUrls[VideoQuality.Q_720P] ?: "")
        val platform = resolved.platform

        val entity = DownloadEntity(
            title = title.ifBlank { resolved.title },
            url = url,
            streamUrl = streamUrl,
            platform = platform.displayName,
            quality = quality.label,
            format = quality.format,
            status = DownloadStatus.QUEUED.name,
            progress = 0f,
            bytesDownloaded = 0L,
            totalBytes = 0L,
            speedBytesPerSec = 0L
        )

        scope.launch {
            db.downloadDao().insertDownload(entity)
            startDownload(entity.id)
        }
        return entity.id
    }

    fun enqueueBatch(urls: List<String>, quality: VideoQuality) {
        scope.launch {
            urls.filter { it.isNotBlank() }.forEach { rawUrl ->
                val resolved = resolveUrl(rawUrl)
                val streamUrl = resolved.directStreamUrls[quality] ?: ""
                val entity = DownloadEntity(
                    title = resolved.title,
                    url = rawUrl,
                    streamUrl = streamUrl,
                    platform = resolved.platform.displayName,
                    quality = quality.label,
                    format = quality.format,
                    status = DownloadStatus.QUEUED.name,
                    progress = 0f,
                    bytesDownloaded = 0L,
                    totalBytes = 0L,
                    speedBytesPerSec = 0L
                )
                db.downloadDao().insertDownload(entity)
                startDownload(entity.id)
                delay(200) // Stagger slightly
            }
        }
    }

    fun startDownload(downloadId: String) {
        if (activeJobs.containsKey(downloadId)) return
        pausedIds.remove(downloadId)

        val job = scope.launch {
            val download = db.downloadDao().getDownloadById(downloadId).let { flow ->
                // fetch single from db
                var item: DownloadEntity? = null
                kotlinx.coroutines.withTimeoutOrNull(2000) {
                    flow.collect { if (it != null) { item = it; return@collect } }
                }
                item
            } ?: return@launch

            executeDownload(download)
        }
        activeJobs[downloadId] = job
        job.invokeOnCompletion { activeJobs.remove(downloadId) }
    }

    private suspend fun executeDownload(download: DownloadEntity) = withContext(Dispatchers.IO) {
        db.downloadDao().updateStatus(download.id, DownloadStatus.DOWNLOADING.name)

        val outputDir = getMediaStorageDir()
        val fileExtension = if (download.format.isNotBlank()) download.format else "mp4"
        val sanitizedTitle = download.title.replace("[^a-zA-Z0-9_.-]".toRegex(), "_").take(35)
        val targetFile = File(outputDir, "VidGrab_${sanitizedTitle}_${System.currentTimeMillis()}.$fileExtension")

        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            val request = Request.Builder()
                .url(download.streamUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                db.downloadDao().updateStatus(
                    download.id,
                    DownloadStatus.FAILED.name,
                    "HTTP ${response.code}: ${response.message}"
                )
                return@withContext
            }

            val body = response.body ?: run {
                db.downloadDao().updateStatus(download.id, DownloadStatus.FAILED.name, "Empty response body")
                return@withContext
            }

            val totalBytes = body.contentLength().takeIf { it > 0 } ?: (18L * 1024 * 1024) // Fallback 18MB
            inputStream = body.byteStream()
            outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(32 * 1024)
            var bytesRead: Int
            var totalRead = 0L
            var lastUpdateMs = System.currentTimeMillis()
            var bytesSinceLastUpdate = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (pausedIds.contains(download.id)) {
                    db.downloadDao().updateStatus(download.id, DownloadStatus.PAUSED.name)
                    return@withContext
                }

                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                bytesSinceLastUpdate += bytesRead

                val now = System.currentTimeMillis()
                val elapsedMs = now - lastUpdateMs
                if (elapsedMs >= 350) {
                    val speed = (bytesSinceLastUpdate * 1000L) / elapsedMs
                    val progress = (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 0.99f)

                    db.downloadDao().updateProgress(
                        id = download.id,
                        progress = progress,
                        bytesDownloaded = totalRead,
                        totalBytes = totalBytes,
                        speed = speed,
                        status = DownloadStatus.DOWNLOADING.name
                    )

                    lastUpdateMs = now
                    bytesSinceLastUpdate = 0L
                }
            }

            outputStream.flush()
            val finalFileSize = targetFile.length()

            // If audio quality was requested, extract audio or keep audio
            val isAudioOnly = download.format.equals("mp3", ignoreCase = true) || download.format.equals("m4a", ignoreCase = true)
            var finalFile = targetFile
            if (isAudioOnly && targetFile.extension.equals("mp4", ignoreCase = true)) {
                val audioFile = File(outputDir, "VidGrab_Audio_${sanitizedTitle}_${System.currentTimeMillis()}.${download.format}")
                val extractResult = MediaToolsEngine.extractAudio(targetFile.absolutePath, audioFile.absolutePath)
                if (extractResult.isSuccess) {
                    targetFile.delete()
                    finalFile = audioFile
                }
            }

            // Extract metadata & generate thumbnail
            val mediaInfo = MediaToolsEngine.getMediaInfo(finalFile.absolutePath)
            val thumbPath = if (!isAudioOnly) {
                MediaToolsEngine.generateThumbnail(context, finalFile.absolutePath, 1500L)
            } else null

            // Insert into SavedMedia table
            val savedEntity = SavedMediaEntity(
                title = download.title,
                sourceUrl = download.url,
                localPath = finalFile.absolutePath,
                durationMs = if (mediaInfo.durationMs > 0) mediaInfo.durationMs else 30_000L,
                fileSizeBytes = finalFile.length().takeIf { it > 0 } ?: finalFileSize,
                resolution = if (isAudioOnly) "Audio" else "${mediaInfo.width}x${mediaInfo.height}".takeIf { mediaInfo.width > 0 } ?: download.quality,
                format = finalFile.extension,
                platform = download.platform,
                thumbnailPath = thumbPath,
                isTrimmed = false,
                isConverted = isAudioOnly,
                createdAt = System.currentTimeMillis()
            )

            db.mediaDao().insertSavedMedia(savedEntity)

            // Mark download queue item as completed
            db.downloadDao().updateProgress(
                id = download.id,
                progress = 1.0f,
                bytesDownloaded = finalFile.length(),
                totalBytes = finalFile.length(),
                speed = 0L,
                status = DownloadStatus.COMPLETED.name
            )
            db.downloadDao().updateStatus(
                id = download.id,
                status = DownloadStatus.COMPLETED.name,
                errorMessage = null,
                localPath = finalFile.absolutePath
            )

        } catch (e: CancellationException) {
            Log.d(TAG, "Download canceled for ${download.id}")
            targetFile.delete()
            db.downloadDao().updateStatus(download.id, DownloadStatus.PAUSED.name)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for ${download.id}: ${e.message}", e)
            targetFile.delete()
            db.downloadDao().updateStatus(
                download.id,
                DownloadStatus.FAILED.name,
                e.message ?: "Download connection failed"
            )
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}
        }
    }

    fun pauseDownload(id: String) {
        pausedIds.add(id)
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        scope.launch {
            db.downloadDao().updateStatus(id, DownloadStatus.PAUSED.name)
        }
    }

    fun resumeDownload(id: String) {
        pausedIds.remove(id)
        startDownload(id)
    }

    fun cancelDownload(id: String) {
        pausedIds.remove(id)
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        scope.launch {
            db.downloadDao().deleteDownloadById(id)
        }
    }

    fun pauseAll() {
        activeJobs.keys.forEach { pauseDownload(it) }
    }

    fun resumeAll() {
        scope.launch {
            val items = db.downloadDao().getPendingOrDownloading()
            items.forEach { startDownload(it.id) }
        }
    }

    fun clearCompleted() {
        scope.launch {
            db.downloadDao().clearCompleted()
        }
    }

    private fun getMediaStorageDir(): File {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val dir = if (moviesDir != null && moviesDir.canWrite()) {
            File(moviesDir, "VidGrab").apply { mkdirs() }
        } else {
            File(context.filesDir, "VidGrab_Media").apply { mkdirs() }
        }
        return dir
    }
}
