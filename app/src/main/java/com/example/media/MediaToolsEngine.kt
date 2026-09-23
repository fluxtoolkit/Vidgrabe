package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

object MediaToolsEngine {
    private const val TAG = "MediaToolsEngine"

    data class MediaInfo(
        val durationMs: Long,
        val width: Int,
        val height: Int,
        val bitrate: Long,
        val hasVideo: Boolean,
        val hasAudio: Boolean,
        val rotation: Int
    )

    suspend fun getMediaInfo(filePath: String): MediaInfo = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != null
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null

            MediaInfo(
                durationMs = durationStr?.toLongOrNull() ?: 0L,
                width = widthStr?.toIntOrNull() ?: 0,
                height = heightStr?.toIntOrNull() ?: 0,
                bitrate = bitrateStr?.toLongOrNull() ?: 0L,
                hasVideo = hasVideo,
                hasAudio = hasAudio,
                rotation = rotationStr?.toIntOrNull() ?: 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving media info: ${e.message}", e)
            MediaInfo(0L, 0, 0, 0L, hasVideo = true, hasAudio = true, rotation = 0)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    suspend fun generateThumbnail(context: Context, filePath: String, atTimeMs: Long = 1000L): String? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(filePath)
            val timeUs = atTimeMs * 1000L
            val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime

            if (bitmap != null) {
                val thumbDir = File(context.cacheDir, "thumbnails").apply { mkdirs() }
                val thumbFile = File(thumbDir, "thumb_${System.currentTimeMillis()}_${(0..999).random()}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                bitmap.recycle()
                return@withContext thumbFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating thumbnail: ${e.message}", e)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
        null
    }

    suspend fun trimVideo(
        sourcePath: String,
        outputPath: String,
        startMs: Long,
        endMs: Long,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        val outputFile = File(outputPath)
        try {
            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) {
                outputFile.delete()
            }

            extractor = MediaExtractor()
            extractor.setDataSource(sourcePath)

            val trackCount = extractor.trackCount
            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Get orientation hint
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(sourcePath)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val rotation = rotationStr?.toIntOrNull() ?: 0
            muxer.setOrientationHint(rotation)
            retriever.release()

            val indexMap = HashMap<Int, Int>(trackCount)
            var bufferSize = 1024 * 1024 // 1MB default

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                val isVideo = mime.startsWith("video/")
                val isAudio = mime.startsWith("audio/")

                if (isVideo || isAudio) {
                    extractor.selectTrack(i)
                    val dstIndex = muxer.addTrack(format)
                    indexMap[i] = dstIndex
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        val newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        if (newSize > bufferSize) bufferSize = newSize
                    }
                }
            }

            if (indexMap.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("No valid video or audio tracks found in source file."))
            }

            muxer.start()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            val totalTrimDurationUs = (endUs - startUs).coerceAtLeast(1L)

            val dstBuf = ByteBuffer.allocateDirect(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(dstBuf, 0)
                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0
                    break
                }
                bufferInfo.presentationTimeUs = extractor.sampleTime
                if (bufferInfo.presentationTimeUs > endUs) {
                    break
                }
                if (bufferInfo.presentationTimeUs >= startUs) {
                    bufferInfo.flags = extractor.sampleFlags
                    val trackIndex = extractor.sampleTrackIndex
                    val muxerTrack = indexMap[trackIndex]
                    if (muxerTrack != null) {
                        muxer.writeSampleData(muxerTrack, dstBuf, bufferInfo)
                    }

                    val progress = ((bufferInfo.presentationTimeUs - startUs).toFloat() / totalTrimDurationUs.toFloat())
                        .coerceIn(0f, 1f)
                    onProgress(progress)
                }
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null

            extractor.release()
            extractor = null

            onProgress(1f)
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error trimming video: ${e.message}", e)
            try {
                muxer?.release()
            } catch (_: Exception) {}
            try {
                extractor?.release()
            } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    suspend fun extractAudio(
        sourcePath: String,
        outputPath: String,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        val outputFile = File(outputPath)
        try {
            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) {
                outputFile.delete()
            }

            extractor = MediaExtractor()
            extractor.setDataSource(sourcePath)

            val trackCount = extractor.trackCount
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                return@withContext Result.failure(IllegalStateException("No audio track found in file."))
            }

            extractor.selectTrack(audioTrackIndex)
            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            var bufferSize = 256 * 1024
            if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                val newSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                if (newSize > bufferSize) bufferSize = newSize
            }

            val dstBuf = ByteBuffer.allocateDirect(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(sourcePath)
            val durationUs = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 1000L) * 1000L
            retriever.release()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(dstBuf, 0)
                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0
                    break
                }
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(muxerTrackIndex, dstBuf, bufferInfo)

                if (durationUs > 0) {
                    val progress = (bufferInfo.presentationTimeUs.toFloat() / durationUs.toFloat()).coerceIn(0f, 1f)
                    onProgress(progress)
                }
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null

            extractor.release()
            extractor = null

            onProgress(1f)
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting audio: ${e.message}", e)
            try {
                muxer?.release()
            } catch (_: Exception) {}
            try {
                extractor?.release()
            } catch (_: Exception) {}
            Result.failure(e)
        }
    }
}
