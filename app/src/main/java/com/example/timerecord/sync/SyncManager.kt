package com.example.timerecord.sync

import android.content.Context
import android.util.Log
import com.example.timerecord.AppDatabase
import com.example.timerecord.auth.AuthManager
import com.example.timerecord.dao.PendingOperationDao
import com.example.timerecord.entity.PendingOperationEntity
import com.example.timerecord.entity.Record
import com.example.timerecord.entity.Label
import com.example.timerecord.entity.RecordLabelRel
import com.example.timerecord.network.dto.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 数据同步管理器
 * 负责本地数据与服务器之间的双向同步
 */
class SyncManager(
    private val context: Context,
    private val authManager: AuthManager,
    private val database: AppDatabase
) {
    private val pendingOperationDao: PendingOperationDao = database.pendingOperationDao()
    private val recordDao = database.recordDao()
    private val labelDao = database.labelDao()
    private val recordLabelRelDao = database.recordLabelRelDao()
    private val gson = Gson()

    companion object {
        private const val TAG = "SyncManager"
        private const val ENTITY_TYPE_RECORD = "RECORD"
        private const val ENTITY_TYPE_LABEL = "LABEL"

        private const val OP_CREATE = "CREATE"
        private const val OP_UPDATE = "UPDATE"
        private const val OP_DELETE = "DELETE"
    }

    /**
     * 同步记录数据
     * 1. 上传本地待同步的操作
     * 2. 下载服务器变更
     * 3. 应用本地变更
     */
    suspend fun syncRecords(): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            if (!authManager.isLoggedIn()) {
                Log.w(TAG, "用户未登录，跳过同步")
                return@withContext Result.failure(Exception("用户未登录"))
            }

            Log.d(TAG, "开始同步记录...")

            val apiService = authManager.getApiService()
            val clientId = getOrCreateClientId()

            // 1. 获取本地待同步的操作
            val pendingOperations = getPendingRecordOperations(clientId)
            val pendingOperationIds = pendingOperationDao.getOperationsByEntityType(ENTITY_TYPE_RECORD, synced = false).map { it.id }

            Log.d(TAG, "待同步操作数量：${pendingOperations.size}, 操作 IDs: $pendingOperationIds")

            // 2. 构建同步请求
            val syncRequest = SyncRequest(
                clientId = clientId,
                since = getLastSyncTimestamp(),
                pendingOperations = pendingOperations.ifEmpty { null }
            )

            Log.d(TAG, "发送同步请求：clientId=$clientId, since=${getLastSyncTimestamp()}, pendingOps=${pendingOperations.size}")

            // 3. 发送同步请求
            val token = "Bearer ${authManager.getAccessToken()}"
            Log.d(TAG, "使用 Token: ${if (authManager.getAccessToken() != null) "exists" else "null"}")
            val response = apiService.syncRecords(token, syncRequest)

            Log.d(TAG, "同步响应：code=${response.code()}, body=${response.body()}")

            if (!response.isSuccessful || response.body()?.success != true) {
                return@withContext Result.failure(Exception(response.body()?.message ?: "同步失败"))
            }

            val syncResponse = response.body()?.data ?: return@withContext Result.failure(Exception("同步响应为空"))

            // 4. 应用服务器变更
            applyServerRecordChanges(syncResponse.changes)

            // 5. 标记本地操作为已同步
            if (pendingOperationIds.isNotEmpty()) {
                pendingOperationDao.markOperationsAsSynced(pendingOperationIds)
            }

            // 6. 更新最后同步时间
            saveLastSyncTimestamp(syncResponse.serverTime)

            Log.i(TAG, "记录同步完成：创建=${syncResponse.changes.created?.size}, 更新=${syncResponse.changes.updated?.size}, 删除=${syncResponse.changes.deleted?.size}")

            Result.success(
                SyncResult(
                    syncToken = syncResponse.syncToken,
                    serverTime = syncResponse.serverTime,
                    recordsCreated = syncResponse.changes.created?.size ?: 0,
                    recordsUpdated = syncResponse.changes.updated?.size ?: 0,
                    recordsDeleted = syncResponse.changes.deleted?.size ?: 0,
                    conflicts = syncResponse.conflicts ?: emptyList()
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "记录同步失败", e)
            Result.failure(e)
        }
    }

    /**
     * 同步标签数据
     */
    suspend fun syncLabels(): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            if (!authManager.isLoggedIn()) {
                return@withContext Result.failure(Exception("用户未登录"))
            }

            val apiService = authManager.getApiService()
            val clientId = getOrCreateClientId()

            // 1. 获取本地待同步的操作
            val pendingOperations = getPendingLabelOperations(clientId)
            val pendingOperationIds = pendingOperationDao.getOperationsByEntityType(ENTITY_TYPE_LABEL, synced = false).map { it.id }

            // 2. 构建同步请求
            val syncRequest = SyncRequest(
                clientId = clientId,
                since = getLastSyncTimestamp(),
                pendingOperations = pendingOperations.ifEmpty { null }
            )

            // 3. 发送同步请求
            val response = apiService.syncLabels("Bearer ${authManager.getAccessToken()}", syncRequest)

            if (!response.isSuccessful || response.body()?.success != true) {
                return@withContext Result.failure(Exception(response.body()?.message ?: "同步失败"))
            }

            val syncResponse = response.body()?.data ?: return@withContext Result.failure(Exception("同步响应为空"))

            // 4. 应用服务器变更
            applyServerLabelChanges(syncResponse.changes)

            // 5. 标记本地操作为已同步
            if (pendingOperationIds.isNotEmpty()) {
                pendingOperationDao.markOperationsAsSynced(pendingOperationIds)
            }

            // 6. 更新最后同步时间
            saveLastSyncTimestamp(syncResponse.serverTime)

            Log.i(TAG, "标签同步完成：创建=${syncResponse.changes.created?.size}, 更新=${syncResponse.changes.updated?.size}, 删除=${syncResponse.changes.deleted?.size}")

            Result.success(
                SyncResult(
                    syncToken = syncResponse.syncToken,
                    serverTime = syncResponse.serverTime,
                    labelsCreated = syncResponse.changes.created?.size ?: 0,
                    labelsUpdated = syncResponse.changes.updated?.size ?: 0,
                    labelsDeleted = syncResponse.changes.deleted?.size ?: 0,
                    conflicts = syncResponse.conflicts ?: emptyList()
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "标签同步失败", e)
            Result.failure(e)
        }
    }

    /**
     * 记录创建操作
     */
    suspend fun recordCreateOperation(record: Record) {
        withContext(Dispatchers.IO) {
            // 获取记录的标签 ID 列表
            val labelIds = recordLabelRelDao.getRecordLabelRelsByRecordId(record.id)
                .map { it.labelId }

            val operation = PendingOperationEntity(
                id = UUID.randomUUID().toString(),
                entityType = ENTITY_TYPE_RECORD,
                entityId = record.id,
                operationType = OP_CREATE,
                version = 1,
                data = gson.toJson(
                    RecordData(
                        note = record.note,
                        timestamp = record.timestamp,
                        date = record.date,
                        time24 = record.time24,
                        time12 = record.time12,
                        amPm = record.amPm,
                        labelIds = labelIds.ifEmpty { null }
                    )
                ),
                timestamp = System.currentTimeMillis()
            )
            pendingOperationDao.insertOperation(operation)
            Log.d(TAG, "记录创建操作已记录：${record.id}, labelIds=$labelIds")
        }
    }

    /**
     * 记录更新操作
     */
    suspend fun recordUpdateOperation(record: Record, previousVersion: Long) {
        withContext(Dispatchers.IO) {
            // 获取记录的标签 ID 列表
            val labelIds = recordLabelRelDao.getRecordLabelRelsByRecordId(record.id)
                .map { it.labelId }

            val operation = PendingOperationEntity(
                id = UUID.randomUUID().toString(),
                entityType = ENTITY_TYPE_RECORD,
                entityId = record.id,
                operationType = OP_UPDATE,
                version = previousVersion + 1,
                previousVersion = previousVersion,
                data = gson.toJson(
                    RecordData(
                        note = record.note,
                        timestamp = record.timestamp,
                        date = record.date,
                        time24 = record.time24,
                        time12 = record.time12,
                        amPm = record.amPm,
                        labelIds = labelIds.ifEmpty { null }
                    )
                ),
                timestamp = System.currentTimeMillis()
            )
            pendingOperationDao.insertOperation(operation)
            Log.d(TAG, "记录更新操作已记录：${record.id}, labelIds=$labelIds")
        }
    }

    /**
     * 记录删除操作
     */
    suspend fun recordDeleteOperation(recordId: String) {
        withContext(Dispatchers.IO) {
            val operation = PendingOperationEntity(
                id = UUID.randomUUID().toString(),
                entityType = ENTITY_TYPE_RECORD,
                entityId = recordId,
                operationType = OP_DELETE,
                version = 1,
                data = null,
                timestamp = System.currentTimeMillis()
            )
            pendingOperationDao.insertOperation(operation)
            Log.d(TAG, "记录删除操作已记录：$recordId")
        }
    }

    /**
     * 记录标签创建操作
     */
    suspend fun recordLabelCreateOperation(label: Label) {
        withContext(Dispatchers.IO) {
            val operation = PendingOperationEntity(
                id = UUID.randomUUID().toString(),
                entityType = ENTITY_TYPE_LABEL,
                entityId = label.id,
                operationType = OP_CREATE,
                version = 1,
                data = gson.toJson(LabelData(name = label.name, color = label.color)),
                timestamp = System.currentTimeMillis()
            )
            pendingOperationDao.insertOperation(operation)
            Log.d(TAG, "标签创建操作已记录：${label.id}")
        }
    }

    /**
     * 记录标签更新操作
     */
    suspend fun recordLabelUpdateOperation(label: Label, previousVersion: Long) {
        withContext(Dispatchers.IO) {
            val operation = PendingOperationEntity(
                id = UUID.randomUUID().toString(),
                entityType = ENTITY_TYPE_LABEL,
                entityId = label.id,
                operationType = OP_UPDATE,
                version = previousVersion + 1,
                previousVersion = previousVersion,
                data = gson.toJson(LabelData(name = label.name, color = label.color)),
                timestamp = System.currentTimeMillis()
            )
            pendingOperationDao.insertOperation(operation)
            Log.d(TAG, "标签更新操作已记录：${label.id}")
        }
    }

    /**
     * 记录标签删除操作
     */
    suspend fun recordLabelDeleteOperation(labelId: String) {
        withContext(Dispatchers.IO) {
            val operation = PendingOperationEntity(
                id = UUID.randomUUID().toString(),
                entityType = ENTITY_TYPE_LABEL,
                entityId = labelId,
                operationType = OP_DELETE,
                version = 1,
                data = null,
                timestamp = System.currentTimeMillis()
            )
            pendingOperationDao.insertOperation(operation)
            Log.d(TAG, "标签删除操作已记录：$labelId")
        }
    }

    /**
     * 执行完整同步（记录 + 标签）
     */
    suspend fun fullSync(): Result<Pair<SyncResult, SyncResult>> = withContext(Dispatchers.IO) {
        try {
            val recordsResult = syncRecords()
            val labelsResult = syncLabels()

            if (recordsResult.isFailure && labelsResult.isFailure) {
                Result.failure(Exception("同步完全失败"))
            } else {
                Result.success(
                    Pair(
                        recordsResult.getOrNull() ?: SyncResult("", 0),
                        labelsResult.getOrNull() ?: SyncResult("", 0)
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 获取待同步的记录操作
     */
    private suspend fun getPendingRecordOperations(clientId: String): List<PendingOperation> {
        val operations = pendingOperationDao.getOperationsByEntityType(ENTITY_TYPE_RECORD, synced = false)
        return operations.map { op ->
            val data = op.data?.let {
                gson.fromJson(it, RecordData::class.java)
            }
            PendingOperation(
                operationType = op.operationType,
                entityId = op.entityId,
                version = op.version,
                data = data,
                previousVersion = op.previousVersion,
                timestamp = op.timestamp
            )
        }
    }

    /**
     * 获取待同步的标签操作
     */
    private suspend fun getPendingLabelOperations(clientId: String): List<PendingOperation> {
        val operations = pendingOperationDao.getOperationsByEntityType(ENTITY_TYPE_LABEL, synced = false)
        return operations.map { op ->
            val data = op.data?.let {
                gson.fromJson(it, LabelData::class.java)
            }
            PendingOperation(
                operationType = op.operationType,
                entityId = op.entityId,
                version = op.version,
                data = data,
                previousVersion = op.previousVersion,
                timestamp = op.timestamp
            )
        }
    }

    /**
     * 应用服务器记录变更到本地数据库
     */
    private suspend fun applyServerRecordChanges(changes: RecordSyncChanges) {
        // 获取当前登录用户 ID，用于替换服务器用户 ID
        val localUserId = authManager.getUserId() ?: return

        // 处理创建的记录
        changes.created?.forEach { recordResponse ->
            val existingRecord = recordDao.getRecordById(recordResponse.id)
            if (existingRecord == null) {
                // 服务器创建时间格式为 ISO 8601，需要转换
                val createdAt = recordResponse.createdAt?.let { parseIso8601(it) } ?: System.currentTimeMillis()
                val updatedAt = recordResponse.updatedAt?.let { parseIso8601(it) } ?: createdAt

                val record = Record(
                    id = recordResponse.id,
                    userId = localUserId,  // 使用本地用户 ID
                    timestamp = recordResponse.timestamp,
                    date = recordResponse.date,
                    time24 = recordResponse.time24,
                    time12 = recordResponse.time12,
                    amPm = recordResponse.amPm,
                    note = recordResponse.note,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
                recordDao.insertRecord(record)

                // 同步标签关联关系
                recordResponse.labels?.forEach { labelResponse ->
                    val rel = RecordLabelRel(
                        id = UUID.randomUUID().toString(),
                        recordId = recordResponse.id,
                        labelId = labelResponse.id
                    )
                    recordLabelRelDao.insertRelation(rel)
                }

                Log.d(TAG, "同步创建记录：${record.id}, labels=${recordResponse.labels?.size ?: 0}")
            }
        }

        // 处理更新的记录
        changes.updated?.forEach { recordResponse ->
            val existingRecord = recordDao.getRecordById(recordResponse.id)
            if (existingRecord != null) {
                val updatedAt = recordResponse.updatedAt?.let { parseIso8601(it) } ?: System.currentTimeMillis()

                val updatedRecord = existingRecord.copy(
                    note = recordResponse.note,
                    timestamp = recordResponse.timestamp,
                    date = recordResponse.date,
                    time24 = recordResponse.time24,
                    time12 = recordResponse.time12,
                    amPm = recordResponse.amPm,
                    updatedAt = updatedAt
                )
                recordDao.updateRecord(updatedRecord)

                // 同步标签关联关系：先删除旧的，再插入新的
                recordLabelRelDao.deleteRecordLabelRelsByRecordId(recordResponse.id)
                recordResponse.labels?.forEach { labelResponse ->
                    val rel = RecordLabelRel(
                        id = UUID.randomUUID().toString(),
                        recordId = recordResponse.id,
                        labelId = labelResponse.id
                    )
                    recordLabelRelDao.insertRelation(rel)
                }

                Log.d(TAG, "同步更新记录：${recordResponse.id}, labels=${recordResponse.labels?.size ?: 0}")
            }
        }

        // 处理删除的记录
        changes.deleted?.forEach { recordId ->
            val existingRecord = recordDao.getRecordById(recordId)
            if (existingRecord != null) {
                // 删除记录前，先删除标签关联关系（ CASCADE 也会自动删除，但显式删除更清晰）
                recordLabelRelDao.deleteRecordLabelRelsByRecordId(recordId)
                recordDao.deleteRecord(existingRecord)
                Log.d(TAG, "同步删除记录：$recordId")
            }
        }
    }

    /**
     * 应用服务器标签变更到本地数据库
     */
    private suspend fun applyServerLabelChanges(changes: LabelSyncChanges) {
        // 获取当前登录用户 ID，用于替换服务器用户 ID
        val localUserId = authManager.getUserId() ?: return

        // 处理创建的标签
        changes.created?.forEach { labelResponse ->
            val existingLabel = labelDao.getLabelById(labelResponse.id)
            if (existingLabel == null) {
                val createdAt = labelResponse.createdAt?.let { parseIso8601(it) } ?: System.currentTimeMillis()
                val updatedAt = labelResponse.updatedAt?.let { parseIso8601(it) } ?: createdAt

                val label = Label(
                    id = labelResponse.id,
                    userId = localUserId,  // 使用本地用户 ID
                    name = labelResponse.name,
                    color = labelResponse.color,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
                labelDao.insertLabel(label)
                Log.d(TAG, "同步创建标签：${label.id}")
            }
        }

        // 处理更新的标签
        changes.updated?.forEach { labelResponse ->
            val existingLabel = labelDao.getLabelById(labelResponse.id)
            if (existingLabel != null) {
                val updatedAt = labelResponse.updatedAt?.let { parseIso8601(it) } ?: System.currentTimeMillis()

                val updatedLabel = existingLabel.copy(
                    name = labelResponse.name,
                    color = labelResponse.color,
                    updatedAt = updatedAt
                )
                labelDao.updateLabel(updatedLabel)
                Log.d(TAG, "同步更新标签：${labelResponse.id}")
            }
        }

        // 处理删除的标签
        changes.deleted?.forEach { labelId ->
            val existingLabel = labelDao.getLabelById(labelId)
            if (existingLabel != null) {
                labelDao.deleteLabel(existingLabel)
                Log.d(TAG, "同步删除标签：$labelId")
            }
        }
    }

    /**
     * 解析 ISO 8601 时间字符串为毫秒时间戳
     */
    private fun parseIso8601(isoString: String): Long {
        return try {
            java.time.Instant.parse(isoString).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    /**
     * 获取或创建客户端 ID
     */
    private fun getOrCreateClientId(): String {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        var clientId = prefs.getString("client_id", null)
        if (clientId == null) {
            clientId = UUID.randomUUID().toString()
            prefs.edit().putString("client_id", clientId).apply()
        }
        return clientId
    }

    /**
     * 获取最后同步时间戳
     */
    private fun getLastSyncTimestamp(): Long? {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        val timestamp = prefs.getLong("last_sync_timestamp", -1)
        return if (timestamp > 0) timestamp else null
    }

    /**
     * 保存最后同步时间戳
     */
    private fun saveLastSyncTimestamp(timestamp: Long) {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_sync_timestamp", timestamp).apply()
    }
}
