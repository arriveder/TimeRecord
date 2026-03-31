package com.example.timerecord.dao

import androidx.room.*
import com.example.timerecord.entity.PendingOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOperationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: PendingOperationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperations(operations: List<PendingOperationEntity>)

    @Update
    suspend fun updateOperation(operation: PendingOperationEntity)

    @Delete
    suspend fun deleteOperation(operation: PendingOperationEntity)

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun deleteOperationById(id: String)

    @Query("SELECT * FROM pending_operations WHERE synced = :synced ORDER BY timestamp ASC")
    suspend fun getOperationsBySyncStatus(synced: Boolean = false): List<PendingOperationEntity>

    @Query("SELECT * FROM pending_operations WHERE synced = :synced ORDER BY timestamp ASC")
    fun getOperationsBySyncStatusFlow(synced: Boolean = false): Flow<List<PendingOperationEntity>>

    @Query("SELECT * FROM pending_operations WHERE entity_type = :entityType AND synced = :synced")
    suspend fun getOperationsByEntityType(entityType: String, synced: Boolean = false): List<PendingOperationEntity>

    @Query("SELECT * FROM pending_operations WHERE entity_id = :entityId AND entity_type = :entityType")
    suspend fun getOperationsByEntity(entityId: String, entityType: String): List<PendingOperationEntity>

    @Query("UPDATE pending_operations SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE pending_operations SET synced = 1 WHERE id IN (:ids)")
    suspend fun markOperationsAsSynced(ids: List<String>)

    @Query("DELETE FROM pending_operations WHERE synced = 1")
    suspend fun deleteSyncedOperations()

    @Query("SELECT COUNT(*) FROM pending_operations WHERE synced = :synced")
    suspend fun getUnsyncedCount(synced: Boolean = false): Int
}
