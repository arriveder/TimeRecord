package com.example.timerecord.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "record")
data class Record(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timeRecord: String,
    val createdAt: Long = System.currentTimeMillis(),
    val userId: Int
)