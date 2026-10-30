package com.myresume.app.data

import kotlinx.coroutines.flow.Flow

class ResumeRepository(private val dao: ResumeDao) {
    fun observeAll(): Flow<List<ResumeEntity>> = dao.observeAll()

    fun observeById(id: String): Flow<ResumeEntity?> = dao.observeById(id)

    suspend fun getById(id: String): ResumeEntity? = dao.getById(id)

    suspend fun create(title: String = "Untitled resume"): ResumeEntity {
        val resume = ResumeFactory.blankResume(title)
        dao.upsert(resume)
        return resume
    }

    suspend fun save(resume: ResumeEntity) {
        dao.upsert(resume.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun duplicate(id: String, newTitle: String): ResumeEntity? {
        val source = dao.getById(id) ?: return null
        val copy = ResumeFactory.duplicate(source, newTitle)
        dao.upsert(copy)
        return copy
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}
