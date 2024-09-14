package com.example.timerecord.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TypeQueue(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val typeQueueName: String,
    val data: String,
    val createdAt: Long = System.currentTimeMillis(),
    val userId: Int
)
