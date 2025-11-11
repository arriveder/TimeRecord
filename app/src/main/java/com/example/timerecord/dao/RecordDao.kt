package com.example.timerecord.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.timerecord.entity.Record
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Insert
    suspend fun insert(record: Record)

    @Query("SELECT * FROM records WHERE date = :date")
    fun getRecordsByDate(date: String): List<Record>

}