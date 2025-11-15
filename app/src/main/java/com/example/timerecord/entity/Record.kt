package com.example.timerecord.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "records",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("user_id"), Index("date")]
)
data class Record(
    @PrimaryKey val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    val timestamp: Long,     // 毫秒

    val date: String,        // YYYY-MM-DD

    @ColumnInfo(name = "time_12")
    val time12: String,      // hh:mm

    @ColumnInfo(name = "am_pm")
    val amPm: String,        // "AM" or "PM"

    @ColumnInfo(name = "time_24")
    val time24: String,      // HH:mm

    val note: String?,       // 备注

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
