package com.example.timerecord.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.timerecord.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User)

    @Query("SELECT * FROM user WHERE id = :id")
    fun getUserById(id: Int): Flow<User>
}