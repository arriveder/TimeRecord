package com.example.timerecord.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val username: String,
    val email: String?,
    val passwordHash: String?,
    val createdAt: Long,
    val updatedAt: Long
)

