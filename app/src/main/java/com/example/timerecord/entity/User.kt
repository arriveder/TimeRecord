package com.example.timerecord.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userName: String,
    val password: String,
    val createdAt: Long = System.currentTimeMillis()
    )
