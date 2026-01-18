package com.example.timerecord.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "record_label_rel",
    foreignKeys = [
        ForeignKey(
            entity = Record::class,
            parentColumns = ["id"],
            childColumns = ["record_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Label::class,
            parentColumns = ["id"],
            childColumns = ["label_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["record_id"]), Index(value = ["label_id"]),
        Index(value = ["record_id", "label_id"], unique = true)]
)
data class RecordLabelRel(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "record_id") val recordId: String,
    @ColumnInfo(name = "label_id") val labelId: String
)

