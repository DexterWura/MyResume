package com.myresume.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeDao {
    @Query("SELECT * FROM resumes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ResumeEntity>>

    @Query("SELECT * FROM resumes WHERE id = :id")
    fun observeById(id: String): Flow<ResumeEntity?>

    @Query("SELECT * FROM resumes WHERE id = :id")
    suspend fun getById(id: String): ResumeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(resume: ResumeEntity)

    @Update
    suspend fun update(resume: ResumeEntity)

    @Query("DELETE FROM resumes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM resumes")
    suspend fun deleteAll()
}
