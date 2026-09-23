package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class PlatformType(val displayName: String, val badgeColor: Long) {
    YOUTUBE("YouTube", 0xFFFF0033),
    TIKTOK("TikTok", 0xFF00F2FE),
    INSTAGRAM("Instagram", 0xFFE1306C),
    FACEBOOK("Facebook", 0xFF1877F2),
    DIRECT("Direct Video", 0xFF6366F1);

    companion object {
        fun fromUrl(url: String): PlatformType {
            val lower = url.lowercase()
            return when {
                lower.contains("youtube.com") || lower.contains("youtu.be") -> YOUTUBE
                lower.contains("tiktok.com") -> TIKTOK
                lower.contains("instagram.com") || lower.contains("instagr.am") -> INSTAGRAM
                lower.contains("facebook.com") || lower.contains("fb.watch") || lower.contains("fb.com") -> FACEBOOK
                else -> DIRECT
            }
        }
    }
}

enum class VideoQuality(
    val label: String,
    val resolutionText: String,
    val format: String,
    val isAudioOnly: Boolean,
    val estimatedMbPerMin: Double
) {
    Q_1080P("1080p Full HD", "1920x1080", "mp4", false, 28.0),
    Q_720P("720p HD", "1280x720", "mp4", false, 14.0),
    Q_480P("480p SD", "854x480", "mp4", false, 7.5),
    AUDIO_MP3("MP3 Audio (320 kbps)", "Audio Only", "mp3", true, 2.4),
    AUDIO_M4A("M4A AAC Audio", "Audio Only", "m4a", true, 1.8)
}

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

@Entity(tableName = "saved_media")
data class SavedMediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val sourceUrl: String,
    val localPath: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val resolution: String,
    val format: String,
    val platform: String,
    val thumbnailPath: String? = null,
    val isTrimmed: Boolean = false,
    val isConverted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "download_queue")
data class DownloadEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val streamUrl: String,
    val platform: String,
    val quality: String,
    val format: String,
    val status: String = DownloadStatus.QUEUED.name,
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val localPath: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class VideoInfo(
    val title: String,
    val url: String,
    val platform: PlatformType,
    val durationText: String,
    val durationSeconds: Int,
    val previewUrl: String?,
    val directStreamUrls: Map<VideoQuality, String>
)
