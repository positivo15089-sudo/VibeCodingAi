package com.vibecoding.ai.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProject(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)

    @Query("SELECT * FROM messages WHERE projectId = :projectId ORDER BY createdAt ASC")
    fun observeMessages(projectId: String): Flow<List<MessageEntity>>

    @Insert suspend fun insertMessage(message: MessageEntity)
    @Query("DELETE FROM messages WHERE projectId = :projectId") suspend fun deleteMessages(projectId: String)

    @Query("SELECT * FROM snapshots WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeSnapshots(projectId: String): Flow<List<SnapshotEntity>>
    @Insert suspend fun insertSnapshot(snapshot: SnapshotEntity)
    @Query("DELETE FROM snapshots WHERE projectId = :projectId") suspend fun deleteSnapshots(projectId: String)
}
