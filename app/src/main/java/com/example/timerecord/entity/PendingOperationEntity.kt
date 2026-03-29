package com.example.timerecord.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 本地待同步操作实体
 * 用于记录本地产生的数据变更，等待同步到服务器
 */
@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey val id: String,

    @ColumnInfo(name = "entity_type")
    val entityType: String, // "RECORD" or "LABEL"

    @ColumnInfo(name = "entity_id")
    val entityId: String,

    @ColumnInfo(name = "operation_type")
    val operationType: String, // "CREATE", "UPDATE", "DELETE"

    @ColumnInfo(name = "version")
    val version: Long,

    @ColumnInfo(name = "data")
    val data: String?, // JSON 格式

    @ColumnInfo(name = "previous_version")
    val previousVersion: Long? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "synced")
    val synced: Boolean = false
)
