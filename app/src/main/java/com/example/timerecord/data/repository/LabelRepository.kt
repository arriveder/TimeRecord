package com.example.timerecord.data.repository

import com.example.timerecord.dao.LabelDao
import com.example.timerecord.entity.Label

class LabelRepository(
    private val labelDao: LabelDao
) {

    suspend fun insertLabel(label: Label) = labelDao.insertLabel(label)

    suspend fun getLabelsByUser(userId: String) = labelDao.getLabelsByUser(userId)

    suspend fun getLabelById(id: String) = labelDao.getLabelById(id)

    suspend fun getLabelByName(userId: String, name: String) =
        labelDao.getLabelByName(userId, name)

    suspend fun deleteLabel(label: Label) = labelDao.deleteLabel(label)

    suspend fun deleteLabelById(id: String) = labelDao.deleteLabelById(id)
}