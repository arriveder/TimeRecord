package com.example.timerecord.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.timerecord.entity.RecordTagRel
import com.example.timerecord.entity.Tag
import com.example.timerecord.entity.Record

@Dao
interface RecordTagRelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelation(rel: RecordTagRel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelations(rels: List<RecordTagRel>)

    @Delete
    suspend fun deleteRelation(rel: RecordTagRel)

    @Query("DELETE FROM record_tag_rel WHERE id = :id")
    suspend fun deleteRelationById(id: String)

    // 根据记录查询所有标签
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN record_tag_rel rtr ON t.id = rtr.tag_id
        WHERE rtr.record_id = :recordId
    """)
    suspend fun getTagsForRecord(recordId: String): List<Tag>

    // 根据标签查询所有记录
    @Query("""
        SELECT r.* FROM records r
        INNER JOIN record_tag_rel rtr ON r.id = rtr.record_id
        WHERE rtr.tag_id = :tagId
        ORDER BY r.timestamp DESC
    """)
    suspend fun getRecordsByTag(tagId: String): List<Record>
}
