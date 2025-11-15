package com.example.timerecord.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.timerecord.entity.Tag

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<Tag>)

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getTagById(id: String): Tag?

    @Query("""
        SELECT * FROM tags 
        WHERE user_id = :userId 
        ORDER BY name ASC
    """)
    suspend fun getTagsByUser(userId: String): List<Tag>

    @Query("""
        SELECT * FROM tags 
        WHERE user_id = :userId AND name = :name 
        LIMIT 1
    """)
    suspend fun getTagByName(userId: String, name: String): Tag?

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTagById(id: String)
}
