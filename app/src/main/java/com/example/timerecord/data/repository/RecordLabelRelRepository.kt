package com.example.timerecord.data.repository

import com.example.timerecord.dao.RecordLabelRelDao
import com.example.timerecord.entity.RecordLabelRel
import com.example.timerecord.entity.Label
import com.example.timerecord.entity.Record
import java.util.UUID

class RecordLabelRelRepository(
    private val dao: RecordLabelRelDao
) {
    suspend fun addLabelToRecord(recordId: String, labelId: String) {
        val rel = RecordLabelRel(
            id = UUID.randomUUID().toString(),
            recordId = recordId,
            labelId = labelId
        )
        dao.insertRelation(rel)
    }

    suspend fun removeLabelFromRecord(rel: RecordLabelRel) =
        dao.deleteRelation(rel)

    suspend fun deleteRelationById(id: String) =
        dao.deleteRelationById(id)

    suspend fun getLabelsForRecord(recordId: String): List<Label> =
        dao.getLabelsForRecord(recordId)

    suspend fun getRecordsByLabel(labelId: String): List<Record> =
        dao.getRecordsByLabel(labelId)
}