package com.example.timerecord.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.timerecord.entity.RecordLabelRel
import com.example.timerecord.entity.Label
import com.example.timerecord.entity.Record

@Dao
interface RecordLabelRelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelation(rel: RecordLabelRel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelations(rels: List<RecordLabelRel>)

    @Delete
    suspend fun deleteRelation(rel: RecordLabelRel)

    @Query("DELETE FROM record_label_rel WHERE id = :id")
    suspend fun deleteRelationById(id: String)

    // 根据记录查询所有标签
    @Query("""
        SELECT l.* FROM labels l
        INNER JOIN record_label_rel rtr ON l.id = rtr.label_id
        WHERE rtr.record_id = :recordId
    """)
    suspend fun getLabelsForRecord(recordId: String): List<Label>

    // 根据标签查询所有记录
    @Query("""
        SELECT r.* FROM records r
        INNER JOIN record_label_rel rtr ON r.id = rtr.record_id
        WHERE rtr.label_id = :labelId
        ORDER BY r.timestamp DESC
    """)
    suspend fun getRecordsByLabel(labelId: String): List<Record>
}
