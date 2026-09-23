package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.VidGrabDatabase
import com.example.data.model.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.PlatformType
import com.example.data.model.SavedMediaEntity
import com.example.data.model.VideoInfo
import com.example.data.model.VideoQuality
import com.example.download.VidGrabDownloadEngine
import com.example.media.MediaToolsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class VidGrabViewModel(application: Application) : AndroidViewModel(application) {
    private val db = VidGrabDatabase.getInstance(application)
    val downloadEngine = VidGrabDownloadEngine(application)

    val savedMedia: StateFlow<List<SavedMediaEntity>> = db.mediaDao().getAllSavedMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadQueue: StateFlow<List<DownloadEntity>> = db.downloadDao().getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Downloader Screen State
    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _selectedPlatform = MutableStateFlow<PlatformType?>(null)
    val selectedPlatform: StateFlow<PlatformType?> = _selectedPlatform.asStateFlow()

    private val _inspectedVideo = MutableStateFlow<VideoInfo?>(null)
    val inspectedVideo: StateFlow<VideoInfo?> = _inspectedVideo.asStateFlow()

    private val _selectedQuality = MutableStateFlow(VideoQuality.Q_1080P)
    val selectedQuality: StateFlow<VideoQuality> = _selectedQuality.asStateFlow()

    private val _isInspecting = MutableStateFlow(false)
    val isInspecting: StateFlow<Boolean> = _isInspecting.asStateFlow()

    // Batch Downloader State
    private val _batchUrlsText = MutableStateFlow("")
    val batchUrlsText: StateFlow<String> = _batchUrlsText.asStateFlow()

    private val _batchQuality = MutableStateFlow(VideoQuality.Q_720P)
    val batchQuality: StateFlow<VideoQuality> = _batchQuality.asStateFlow()

    // Offline Player Modal
    private val _playingMedia = MutableStateFlow<SavedMediaEntity?>(null)
    val playingMedia: StateFlow<SavedMediaEntity?> = _playingMedia.asStateFlow()

    // Trimmer State
    private val _trimTarget = MutableStateFlow<SavedMediaEntity?>(null)
    val trimTarget: StateFlow<SavedMediaEntity?> = _trimTarget.asStateFlow()

    private val _trimStartMs = MutableStateFlow(0L)
    val trimStartMs: StateFlow<Long> = _trimStartMs.asStateFlow()

    private val _trimEndMs = MutableStateFlow(10_000L)
    val trimEndMs: StateFlow<Long> = _trimEndMs.asStateFlow()

    private val _isTrimming = MutableStateFlow(false)
    val isTrimming: StateFlow<Boolean> = _isTrimming.asStateFlow()

    private val _trimProgress = MutableStateFlow(0f)
    val trimProgress: StateFlow<Float> = _trimProgress.asStateFlow()

    // Converter State
    private val _convertTarget = MutableStateFlow<SavedMediaEntity?>(null)
    val convertTarget: StateFlow<SavedMediaEntity?> = _convertTarget.asStateFlow()

    private val _convertFormat = MutableStateFlow("mp3") // mp3, m4a
    val convertFormat: StateFlow<String> = _convertFormat.asStateFlow()

    private val _isConverting = MutableStateFlow(false)
    val isConverting: StateFlow<Boolean> = _isConverting.asStateFlow()

    private val _convertProgress = MutableStateFlow(0f)
    val convertProgress: StateFlow<Float> = _convertProgress.asStateFlow()

    // Feedback
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        // Preload first preset for instant preview
        inspectUrl(VidGrabDownloadEngine.SAMPLE_VIDEOS[0].url)
    }

    fun onUrlInputChanged(newUrl: String) {
        _urlInput.value = newUrl
        if (newUrl.isNotBlank()) {
            inspectUrl(newUrl)
        }
    }

    fun pasteFromClipboard() {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            if (text.isNotBlank()) {
                _urlInput.value = text
                inspectUrl(text)
                showToast("Pasted link from clipboard")
            }
        } else {
            showToast("Clipboard is empty")
        }
    }

    fun clearUrlInput() {
        _urlInput.value = ""
        _inspectedVideo.value = null
    }

    fun selectPlatformFilter(platform: PlatformType?) {
        _selectedPlatform.value = platform
        if (platform != null) {
            // Find sample video matching platform to demonstrate
            val sample = VidGrabDownloadEngine.SAMPLE_VIDEOS.find { it.platform == platform }
            if (sample != null) {
                _urlInput.value = sample.url
                inspectUrl(sample.url)
            }
        }
    }

    fun inspectUrl(url: String) {
        viewModelScope.launch {
            _isInspecting.value = true
            try {
                val resolved = downloadEngine.resolveUrl(url)
                _inspectedVideo.value = resolved
            } catch (e: Exception) {
                _inspectedVideo.value = null
            } finally {
                _isInspecting.value = false
            }
        }
    }

    fun selectQuality(quality: VideoQuality) {
        _selectedQuality.value = quality
    }

    fun startDownloadCurrent() {
        val inspected = _inspectedVideo.value ?: run {
            showToast("Please enter a valid video link")
            return
        }
        val quality = _selectedQuality.value
        downloadEngine.enqueueDownload(
            title = inspected.title,
            url = inspected.url,
            quality = quality
        )
        showToast("Started download: ${inspected.title}")
    }

    fun setBatchUrlsText(text: String) {
        _batchUrlsText.value = text
    }

    fun setBatchQuality(quality: VideoQuality) {
        _batchQuality.value = quality
    }

    fun startBatchDownload() {
        val lines = _batchUrlsText.value.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            showToast("Enter at least one URL")
            return
        }

        downloadEngine.enqueueBatch(lines, _batchQuality.value)
        _batchUrlsText.value = ""
        showToast("Enqueued ${lines.size} downloads to batch queue")
    }

    fun loadSampleBatch() {
        val sampleUrls = VidGrabDownloadEngine.SAMPLE_VIDEOS.joinToString("\n") { it.url }
        _batchUrlsText.value = sampleUrls
    }

    fun pauseDownload(id: String) {
        downloadEngine.pauseDownload(id)
    }

    fun resumeDownload(id: String) {
        downloadEngine.resumeDownload(id)
    }

    fun cancelDownload(id: String) {
        downloadEngine.cancelDownload(id)
    }

    fun pauseAllDownloads() {
        downloadEngine.pauseAll()
    }

    fun resumeAllDownloads() {
        downloadEngine.resumeAll()
    }

    fun clearCompletedDownloads() {
        downloadEngine.clearCompleted()
    }

    fun playMedia(media: SavedMediaEntity) {
        _playingMedia.value = media
    }

    fun closePlayer() {
        _playingMedia.value = null
    }

    fun deleteMedia(media: SavedMediaEntity) {
        viewModelScope.launch {
            try {
                val file = File(media.localPath)
                if (file.exists()) {
                    file.delete()
                }
                media.thumbnailPath?.let {
                    val thumbFile = File(it)
                    if (thumbFile.exists()) thumbFile.delete()
                }
                db.mediaDao().deleteSavedMedia(media)
                if (_playingMedia.value?.id == media.id) {
                    _playingMedia.value = null
                }
                showToast("Deleted ${media.title}")
            } catch (e: Exception) {
                showToast("Error deleting media: ${e.message}")
            }
        }
    }

    fun shareMedia(media: SavedMediaEntity) {
        val context = getApplication<Application>()
        val file = File(media.localPath)
        if (!file.exists()) {
            showToast("Media file does not exist on disk")
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mime = if (media.format.equals("mp3", ignoreCase = true) || media.format.equals("m4a", ignoreCase = true)) {
                "audio/*"
            } else {
                "video/*"
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, media.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share with VidGrab").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            showToast("Failed to share: ${e.message}")
        }
    }

    // Trimmer controls
    fun openTrimmer(media: SavedMediaEntity) {
        _trimTarget.value = media
        _trimStartMs.value = 0L
        _trimEndMs.value = media.durationMs.coerceAtLeast(5000L).coerceAtMost(media.durationMs)
        _trimProgress.value = 0f
    }

    fun setTrimStart(ms: Long) {
        val target = _trimTarget.value ?: return
        val clamped = ms.coerceIn(0L, (_trimEndMs.value - 1000L).coerceAtLeast(0L))
        _trimStartMs.value = clamped
    }

    fun setTrimEnd(ms: Long) {
        val target = _trimTarget.value ?: return
        val clamped = ms.coerceIn(_trimStartMs.value + 1000L, target.durationMs)
        _trimEndMs.value = clamped
    }

    fun executeTrim() {
        val target = _trimTarget.value ?: return
        val start = _trimStartMs.value
        val end = _trimEndMs.value

        if (end <= start) {
            showToast("End time must be after start time")
            return
        }

        viewModelScope.launch {
            _isTrimming.value = true
            _trimProgress.value = 0f
            try {
                val outputDir = File(target.localPath).parentFile ?: getApplication<Application>().cacheDir
                val outputFileName = "VidGrab_Trimmed_${System.currentTimeMillis()}.${target.format}"
                val outputFile = File(outputDir, outputFileName)

                val result = MediaToolsEngine.trimVideo(
                    sourcePath = target.localPath,
                    outputPath = outputFile.absolutePath,
                    startMs = start,
                    endMs = end,
                    onProgress = { _trimProgress.value = it }
                )

                if (result.isSuccess) {
                    val trimmedFile = result.getOrThrow()
                    val info = MediaToolsEngine.getMediaInfo(trimmedFile.absolutePath)
                    val thumb = MediaToolsEngine.generateThumbnail(getApplication(), trimmedFile.absolutePath, 500L)

                    val trimmedEntity = SavedMediaEntity(
                        title = "${target.title} (Trimmed)",
                        sourceUrl = target.sourceUrl,
                        localPath = trimmedFile.absolutePath,
                        durationMs = end - start,
                        fileSizeBytes = trimmedFile.length(),
                        resolution = target.resolution,
                        format = target.format,
                        platform = target.platform,
                        thumbnailPath = thumb ?: target.thumbnailPath,
                        isTrimmed = true,
                        isConverted = false,
                        createdAt = System.currentTimeMillis()
                    )
                    db.mediaDao().insertSavedMedia(trimmedEntity)
                    showToast("Trimmed clip saved to Library!")
                    _trimTarget.value = null
                } else {
                    showToast("Trim failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                showToast("Error during trim: ${e.message}")
            } finally {
                _isTrimming.value = false
            }
        }
    }

    // Converter controls
    fun openConverter(media: SavedMediaEntity) {
        _convertTarget.value = media
        _convertFormat.value = if (media.format.equals("mp3", ignoreCase = true)) "m4a" else "mp3"
        _convertProgress.value = 0f
    }

    fun setConvertFormat(format: String) {
        _convertFormat.value = format
    }

    fun executeConvert() {
        val target = _convertTarget.value ?: return
        val format = _convertFormat.value

        viewModelScope.launch {
            _isConverting.value = true
            _convertProgress.value = 0f
            try {
                val outputDir = File(target.localPath).parentFile ?: getApplication<Application>().cacheDir
                val outputFileName = "VidGrab_Converted_${System.currentTimeMillis()}.$format"
                val outputFile = File(outputDir, outputFileName)

                val result = MediaToolsEngine.extractAudio(
                    sourcePath = target.localPath,
                    outputPath = outputFile.absolutePath,
                    onProgress = { _convertProgress.value = it }
                )

                if (result.isSuccess) {
                    val convertedFile = result.getOrThrow()
                    val info = MediaToolsEngine.getMediaInfo(convertedFile.absolutePath)

                    val convertedEntity = SavedMediaEntity(
                        title = "${target.title} ($format Audio)",
                        sourceUrl = target.sourceUrl,
                        localPath = convertedFile.absolutePath,
                        durationMs = target.durationMs,
                        fileSizeBytes = convertedFile.length(),
                        resolution = "Audio Only",
                        format = format,
                        platform = target.platform,
                        thumbnailPath = target.thumbnailPath,
                        isTrimmed = false,
                        isConverted = true,
                        createdAt = System.currentTimeMillis()
                    )
                    db.mediaDao().insertSavedMedia(convertedEntity)
                    showToast("Converted audio saved to Library!")
                    _convertTarget.value = null
                } else {
                    showToast("Conversion failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                showToast("Error during conversion: ${e.message}")
            } finally {
                _isConverting.value = false
            }
        }
    }

    private fun showToast(msg: String) {
        viewModelScope.launch(Dispatchers.Main) {
            Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
        }
    }
}
