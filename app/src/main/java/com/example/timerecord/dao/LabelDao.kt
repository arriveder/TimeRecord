package com.example.timerecord.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.timerecord.entity.Label

@Dao
interface LabelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: Label)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabels(labels: List<Label>)

    @Query("SELECT * FROM labels WHERE id = :id LIMIT 1")
    suspend fun getLabelById(id: String): Label?

    @Query("""
        SELECT * FROM labels 
        WHERE user_id = :userId 
        ORDER BY name ASC
    """)
    suspend fun getLabelsByUser(userId: String): List<Label>

    @Query("""
        SELECT * FROM labels
        WHERE user_id = :userId AND name = :name
        LIMIT 1
    """)
    suspend fun getLabelByName(userId: String, name: String): Label?

    @Delete
    suspend fun deleteLabel(label: Label)

    @Query("DELETE FROM labels WHERE id = :id")
    suspend fun deleteLabelById(id: String)

    @androidx.room.Update
    suspend fun updateLabel(label: Label)
}
