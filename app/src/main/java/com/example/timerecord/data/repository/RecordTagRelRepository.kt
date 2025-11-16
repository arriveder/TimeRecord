package com.example.timerecord.data.repository

import com.example.timerecord.dao.RecordTagRelDao
import com.example.timerecord.entity.RecordTagRel
import com.example.timerecord.entity.Tag
import com.example.timerecord.entity.Record
import java.util.UUID

class RecordTagRelRepository(
    private val dao: RecordTagRelDao
) {
    suspend fun addTagToRecord(recordId: String, tagId: String) {
        val rel = RecordTagRel(
            id = UUID.randomUUID().toString(),
            recordId = recordId,
            tagId = tagId
        )
        dao.insertRelation(rel)
    }

    suspend fun removeTagFromRecord(rel: RecordTagRel) =
        dao.deleteRelation(rel)

    suspend fun deleteRelationById(id: String) =
        dao.deleteRelationById(id)

    suspend fun getTagsForRecord(recordId: String): List<Tag> =
        dao.getTagsForRecord(recordId)

    suspend fun getRecordsByTag(tagId: String): List<Record> =
        dao.getRecordsByTag(tagId)
}