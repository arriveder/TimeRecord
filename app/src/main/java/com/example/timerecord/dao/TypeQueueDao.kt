package com.example.timerecord.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.timerecord.entity.TypeQueue
import kotlinx.coroutines.flow.Flow

@Dao
interface TypeQueueDao {
    @Insert
    suspend fun insert(typeQueue: TypeQueue)

    @Query("SELECT * FROM typequeue")
    fun getTypeQueues(): Flow<List<TypeQueue>>

    @Query("SELECT * FROM typequeue WHERE id = :id")
    fun getTypeQueueById(id: Int): Flow<TypeQueue>
}