package com.example.timerecord.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "record_tag_rel",
    foreignKeys = [
        ForeignKey(
            entity = Record::class,
            parentColumns = ["id"],
            childColumns = ["record_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["record_id"]), Index(value = ["tag_id"]),
        Index(value = ["record_id", "tag_id"], unique = true)]
)
data class RecordTagRel(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "record_id") val recordId: String,
    @ColumnInfo(name = "tag_id") val tagId: String
)

