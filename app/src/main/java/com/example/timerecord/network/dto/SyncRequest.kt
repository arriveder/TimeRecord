package com.example.timerecord.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 同步请求
 */
data class SyncRequest(
    @SerializedName("clientId") val clientId: String,
    @SerializedName("since") val since: Long? = null,
    @SerializedName("lastVersion") val lastVersion: Long? = null,
    @SerializedName("pendingOperations") val pendingOperations: List<PendingOperation>? = null
)

/**
 * 待同步的操作
 */
data class PendingOperation(
    @SerializedName("operationType") val operationType: String, // CREATE, UPDATE, DELETE
    @SerializedName("entityId") val entityId: String,
    @SerializedName("version") val version: Long,
    @SerializedName("data") val data: Any? = null, // RecordData or LabelData
    @SerializedName("previousVersion") val previousVersion: Long? = null,
    @SerializedName("timestamp") val timestamp: Long
)

/**
 * 记录数据（用于同步）
 */
data class RecordData(
    @SerializedName("note") val note: String?,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("date") val date: String,
    @SerializedName("time24") val time24: String,
    @SerializedName("time12") val time12: String,
    @SerializedName("amPm") val amPm: String
)

/**
 * 标签数据（用于同步）
 */
data class LabelData(
    @SerializedName("name") val name: String,
    @SerializedName("color") val color: String?
)

/**
 * 同步响应（记录）
 */
data class RecordSyncResponse(
    @SerializedName("syncToken") val syncToken: String,
    @SerializedName("serverTime") val serverTime: Long,
    @SerializedName("changes") val changes: RecordSyncChanges,
    @SerializedName("conflicts") val conflicts: List<SyncConflict>? = null,
    @SerializedName("hasMore") val hasMore: Boolean = false
)

/**
 * 同步响应（标签）
 */
data class LabelSyncResponse(
    @SerializedName("syncToken") val syncToken: String,
    @SerializedName("serverTime") val serverTime: Long,
    @SerializedName("changes") val changes: LabelSyncChanges,
    @SerializedName("conflicts") val conflicts: List<SyncConflict>? = null,
    @SerializedName("hasMore") val hasMore: Boolean = false
)

/**
 * 同步响应
 */
data class SyncResponse(
    @SerializedName("syncToken") val syncToken: String,
    @SerializedName("serverTime") val serverTime: Long,
    @SerializedName("changes") val changes: SyncChanges,
    @SerializedName("conflicts") val conflicts: List<SyncConflict>? = null,
    @SerializedName("hasMore") val hasMore: Boolean = false
)

/**
 * 同步变更数据（记录）
 */
data class RecordSyncChanges(
    @SerializedName("created") val created: List<RecordResponse>? = null,
    @SerializedName("updated") val updated: List<RecordResponse>? = null,
    @SerializedName("deleted") val deleted: List<String>? = null
)

/**
 * 同步变更数据（标签）
 */
data class LabelSyncChanges(
    @SerializedName("created") val created: List<LabelResponse>? = null,
    @SerializedName("updated") val updated: List<LabelResponse>? = null,
    @SerializedName("deleted") val deleted: List<String>? = null
)

/**
 * 同步变更数据（通用）
 */
data class SyncChanges(
    @SerializedName("created") val created: List<RecordResponse>? = null,
    @SerializedName("updated") val updated: List<RecordResponse>? = null,
    @SerializedName("deleted") val deleted: List<String>? = null
)

/**
 * 同步冲突信息
 */
data class SyncConflict(
    @SerializedName("conflictId") val conflictId: String,
    @SerializedName("entityId") val entityId: String,
    @SerializedName("entityType") val entityType: String, // RECORD or LABEL
    @SerializedName("localVersion") val localVersion: Long,
    @SerializedName("remoteVersion") val remoteVersion: Long,
    @SerializedName("localData") val localData: Map<String, Any?>? = null,
    @SerializedName("remoteData") val remoteData: Map<String, Any?>? = null,
    @SerializedName("suggestedResolution") val suggestedResolution: String = "LAST_WRITE_WINS"
)

/**
 * 冲突解决请求
 */
data class ConflictResolutionRequest(
    @SerializedName("resolutionStrategy") val resolutionStrategy: String,
    @SerializedName("mergedData") val mergedData: Map<String, Any?>? = null
)

/**
 * 同步结果（本地使用）
 */
data class SyncResult(
    val syncToken: String,
    val serverTime: Long,
    val recordsCreated: Int = 0,
    val recordsUpdated: Int = 0,
    val recordsDeleted: Int = 0,
    val labelsCreated: Int = 0,
    val labelsUpdated: Int = 0,
    val labelsDeleted: Int = 0,
    val conflicts: List<SyncConflict> = emptyList()
)
