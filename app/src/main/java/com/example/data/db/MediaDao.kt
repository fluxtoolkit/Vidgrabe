package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DownloadEntity
import com.example.data.model.SavedMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM saved_media ORDER BY createdAt DESC")
    fun getAllSavedMedia(): Flow<List<SavedMediaEntity>>

    @Query("SELECT * FROM saved_media WHERE id = :id LIMIT 1")
    fun getSavedMediaById(id: Long): Flow<SavedMediaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedMedia(media: SavedMediaEntity): Long

    @Update
    suspend fun updateSavedMedia(media: SavedMediaEntity)

    @Delete
    suspend fun deleteSavedMedia(media: SavedMediaEntity)

    @Query("DELETE FROM saved_media WHERE id = :id")
    suspend fun deleteSavedMediaById(id: Long)

    @Query("SELECT COUNT(*) FROM saved_media")
    fun getSavedMediaCount(): Flow<Int>
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_queue ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM download_queue WHERE id = :id LIMIT 1")
    fun getDownloadById(id: String): Flow<DownloadEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Query("UPDATE download_queue SET progress = :progress, bytesDownloaded = :bytesDownloaded, totalBytes = :totalBytes, speedBytesPerSec = :speed, status = :status WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float, bytesDownloaded: Long, totalBytes: Long, speed: Long, status: String)

    @Query("UPDATE download_queue SET status = :status, errorMessage = :errorMessage, localPath = :localPath WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, errorMessage: String? = null, localPath: String? = null)

    @Query("SELECT * FROM download_queue WHERE status = 'QUEUED' OR status = 'DOWNLOADING'")
    suspend fun getPendingOrDownloading(): List<DownloadEntity>

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)

    @Query("DELETE FROM download_queue WHERE id = :id")
    suspend fun deleteDownloadById(id: String)

    @Query("DELETE FROM download_queue WHERE status = 'COMPLETED'")
    suspend fun clearCompleted()
}
