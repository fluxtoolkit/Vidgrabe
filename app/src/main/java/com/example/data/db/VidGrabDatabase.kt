package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DownloadEntity
import com.example.data.model.SavedMediaEntity

@Database(
    entities = [SavedMediaEntity::class, DownloadEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VidGrabDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: VidGrabDatabase? = null

        fun getInstance(context: Context): VidGrabDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VidGrabDatabase::class.java,
                    "vidgrab.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
