package com.example.timerecord.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Type(
    @PrimaryKey(autoGenerate = true)
    val id :Int = 0,
    val typeName :String,
    val userId :Int
)
