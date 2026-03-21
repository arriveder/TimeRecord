package com.example.timerecord.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.timerecord.entity.Record
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: Record)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<Record>)

    @Query("SELECT * FROM records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: String): Record?

    // 按用户查询所有记录，按时间降序
    @Query("""
        SELECT * FROM records 
        WHERE user_id = :userId 
        ORDER BY timestamp DESC
    """)
    suspend fun getRecordsByUser(userId: String): List<Record>

    // 按日期查询记录（YYYY-MM-DD）
    @Query("""
        SELECT * FROM records
        WHERE user_id = :userId AND date = :date
        ORDER BY timestamp ASC
    """)
    suspend fun getRecordsByDate(userId: String, date: String): List<Record>

    @Update
    suspend fun updateRecord(record: Record)

    @Delete
    suspend fun deleteRecord(record: Record)

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteRecordById(id: String)

    // 搜索记录（按笔记内容）
    @Query("""
        SELECT * FROM records
        WHERE user_id = :userId AND note LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    suspend fun searchRecordsByNote(userId: String, query: String): List<Record>

    // 通过标签搜索记录
    @Query("""
        SELECT DISTINCT r.* FROM records r
        INNER JOIN record_label_rel rel ON r.id = rel.record_id
        INNER JOIN labels l ON rel.label_id = l.id
        WHERE r.user_id = :userId AND l.name LIKE '%' || :query || '%'
        ORDER BY r.timestamp DESC
    """)
    suspend fun searchRecordsByLabel(userId: String, query: String): List<Record>
}
