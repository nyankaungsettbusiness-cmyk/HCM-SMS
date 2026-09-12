package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.CurriculumChunkEntity
import com.example.data.local.entity.CurriculumDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurriculumKnowledgeDao {

    // --- Curriculum Documents ---

    @Query("SELECT * FROM curriculum_documents ORDER BY gradeLevel ASC, subject ASC")
    fun getAllDocumentsFlow(): Flow<List<CurriculumDocumentEntity>>

    @Query("SELECT * FROM curriculum_documents WHERE gradeLevel = :gradeLevel AND subject = :subject")
    suspend fun getDocumentsByGradeAndSubject(gradeLevel: String, subject: String): List<CurriculumDocumentEntity>

    @Query("SELECT * FROM curriculum_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): CurriculumDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: CurriculumDocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<CurriculumDocumentEntity>): List<Long>

    @Delete
    suspend fun deleteDocument(document: CurriculumDocumentEntity)

    // --- Curriculum Chunks ---

    @Query("SELECT * FROM curriculum_chunks WHERE gradeLevel = :gradeLevel AND subject = :subject ORDER BY id ASC")
    fun getChunksByGradeAndSubjectFlow(gradeLevel: String, subject: String): Flow<List<CurriculumChunkEntity>>

    @Query("SELECT * FROM curriculum_chunks WHERE gradeLevel = :gradeLevel AND subject = :subject ORDER BY id ASC")
    suspend fun getChunksByGradeAndSubject(gradeLevel: String, subject: String): List<CurriculumChunkEntity>

    @Query("SELECT * FROM curriculum_chunks WHERE gradeLevel = :gradeLevel AND subject = :subject AND chapterUnit = :chapterUnit ORDER BY id ASC")
    suspend fun getChunksForUnit(gradeLevel: String, subject: String, chapterUnit: String): List<CurriculumChunkEntity>

    @Query("""
        SELECT * FROM curriculum_chunks 
        WHERE gradeLevel = :gradeLevel 
          AND subject = :subject 
          AND (:chapterUnit = '' OR chapterUnit = :chapterUnit)
          AND (:sectionTopic = '' OR sectionTopic LIKE '%' || :sectionTopic || '%')
        ORDER BY id ASC
    """)
    suspend fun getChunksForScope(
        gradeLevel: String,
        subject: String,
        chapterUnit: String = "",
        sectionTopic: String = ""
    ): List<CurriculumChunkEntity>

    @Query("""
        SELECT DISTINCT chapterUnit FROM curriculum_chunks 
        WHERE gradeLevel = :gradeLevel AND subject = :subject 
        ORDER BY chapterUnit ASC
    """)
    suspend fun getAvailableUnits(gradeLevel: String, subject: String): List<String>

    @Query("""
        SELECT DISTINCT sectionTopic FROM curriculum_chunks 
        WHERE gradeLevel = :gradeLevel AND subject = :subject AND chapterUnit = :chapterUnit 
        ORDER BY sectionTopic ASC
    """)
    suspend fun getAvailableSections(gradeLevel: String, subject: String, chapterUnit: String): List<String>

    @Query("""
        SELECT * FROM curriculum_chunks 
        WHERE gradeLevel = :gradeLevel 
          AND subject = :subject 
          AND (content LIKE '%' || :query || '%' OR keywords LIKE '%' || :query || '%' OR vocabularyWords LIKE '%' || :query || '%' OR sectionTopic LIKE '%' || :query || '%')
        LIMIT 10
    """)
    suspend fun searchChunks(gradeLevel: String, subject: String, query: String): List<CurriculumChunkEntity>

    @Query("SELECT COUNT(*) FROM curriculum_chunks")
    suspend fun getChunkCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: CurriculumChunkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<CurriculumChunkEntity>)

    @Query("DELETE FROM curriculum_chunks WHERE id = :id")
    suspend fun deleteChunkById(id: Long)

    @Query("DELETE FROM curriculum_chunks WHERE documentId = :documentId")
    suspend fun deleteChunksByDocumentId(documentId: Long)
}
