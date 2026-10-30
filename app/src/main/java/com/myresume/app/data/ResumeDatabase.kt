package com.myresume.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ResumeEntity::class], version = 1, exportSchema = false)
abstract class ResumeDatabase : RoomDatabase() {
    abstract fun resumeDao(): ResumeDao

    companion object {
        @Volatile
        private var instance: ResumeDatabase? = null

        fun getInstance(context: Context): ResumeDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ResumeDatabase::class.java,
                    "myresume.db"
                ).build().also { instance = it }
            }
        }
    }
}
