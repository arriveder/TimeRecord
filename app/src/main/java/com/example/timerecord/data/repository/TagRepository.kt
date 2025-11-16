package com.example.timerecord.data.repository

import com.example.timerecord.dao.TagDao
import com.example.timerecord.entity.Tag

class TagRepository(
    private val tagDao: TagDao
) {

    suspend fun insertTag(tag: Tag) = tagDao.insertTag(tag)

    suspend fun getTagsByUser(userId: String) = tagDao.getTagsByUser(userId)

    suspend fun getTagById(id: String) = tagDao.getTagById(id)

    suspend fun getTagByName(userId: String, name: String) =
        tagDao.getTagByName(userId, name)

    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)

    suspend fun deleteTagById(id: String) = tagDao.deleteTagById(id)
}