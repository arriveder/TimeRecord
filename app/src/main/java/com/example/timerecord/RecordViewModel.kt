package com.example.timerecord

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.timerecord.auth.AuthManager
import com.example.timerecord.data.repository.LabelRepository
import com.example.timerecord.data.repository.RecordLabelRelRepository
import com.example.timerecord.data.repository.RecordRepository
import com.example.timerecord.data.repository.UserRepository
import com.example.timerecord.entity.Label
import com.example.timerecord.entity.Record
import com.example.timerecord.entity.RecordLabelRel
import com.example.timerecord.entity.User
import com.example.timerecord.sync.SyncManager
import android.util.Log
import com.example.timerecord.network.dto.LabelRequest
import com.example.timerecord.network.dto.RecordRequest
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID

class RecordViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val recordRepository = RecordRepository(database.recordDao())
    private val labelRepository = LabelRepository(database.labelDao())
    private val recordLabelRelRepository = RecordLabelRelRepository(database.recordLabelRelDao())
    private val userRepository = UserRepository(database.userDao())
    private val recordLabelRelDao = database.recordLabelRelDao()
    private val authManager = AuthManager.getInstance(application)
    private val syncManager = SyncManager(application, authManager, database)
    private val gson = Gson()

    private val _saveResult = MutableLiveData<Result<String>>()
    val saveResult: LiveData<Result<String>> = _saveResult

    private val _userLabels = MutableLiveData<List<Label>>()
    val userLabels: LiveData<List<Label>> = _userLabels

    private val _createLabelResult = MutableLiveData<Result<Label>>()
    val createLabelResult: LiveData<Result<Label>> = _createLabelResult

    private val _syncResult = MutableLiveData<Result<String>>()
    val syncResult: LiveData<Result<String>> = _syncResult

    private val _isSyncing = MutableLiveData<Boolean>()
    val isSyncing: LiveData<Boolean> = _isSyncing

    private var currentSortOption = RecordSortOption.TIME_DESC

    /**
     * 获取当前用户 ID
     * @return 用户 ID，未登录时抛出异常
     */
    fun getCurrentUserId(): String {
        return runBlocking {
            authManager.getUserId()
        } ?: throw IllegalStateException("用户未登录，请先登录")
    }

    /**
     * 异步获取当前用户 ID
     */
    suspend fun getCurrentUserIdAsync(): String? {
        return authManager.getUserId()
    }

    /**
     * 检查用户是否已登录
     */
    fun isLoggedIn(): Boolean = authManager.isLoggedIn()

    /**
     * 加载用户标签
     */
    fun loadLabels() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val labels = labelRepository.getLabelsByUser(userId)
                _userLabels.postValue(labels)
            } catch (e: Exception) {
                _userLabels.postValue(emptyList())
            }
        }
    }

    /**
     * 创建记录
     */
    fun createRecord(note: String?, labelIds: List<String>) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val recordId = recordRepository.createRecord(userId, note)

                labelIds.forEach { labelId ->
                    val rel = RecordLabelRel(
                        id = UUID.randomUUID().toString(),
                        recordId = recordId,
                        labelId = labelId
                    )
                    recordLabelRelRepository.insertRecordLabelRel(rel)
                }

                _saveResult.postValue(Result.success(recordId))
            } catch (e: Exception) {
                _saveResult.postValue(Result.failure(e))
            }
        }
    }

    /**
     * 创建标签
     */
    fun createLabel(name: String, color: String? = null) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val existingLabel = labelRepository.getLabelByName(userId, name)
                if (existingLabel != null) {
                    _createLabelResult.postValue(Result.failure(Exception("Label already exists")))
                    return@launch
                }

                val label = Label(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = name,
                    color = color,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                labelRepository.insertLabel(label)
                _createLabelResult.postValue(Result.success(label))

                loadLabels()
            } catch (e: Exception) {
                _createLabelResult.postValue(Result.failure(e))
            }
        }
    }

    private val _userRecords = MutableLiveData<List<Record>>()
    val userRecords: LiveData<List<Record>> = _userRecords

    fun setSortOption(sortOption: RecordSortOption) {
        currentSortOption = sortOption
    }

    fun getSortOption(): RecordSortOption = currentSortOption

    fun loadRecords() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val records = recordRepository.getRecordsByUser(userId)
                _userRecords.postValue(records)
            } catch (e: Exception) {
                _userRecords.postValue(emptyList())
            }
        }
    }

    suspend fun getRecordsWithLabels(): List<RecordWithLabels> {
        return try {
            val userId = getCurrentUserId()
            val records = recordRepository.getRecordsByUser(userId)
            records.map { record ->
                val labelRels = recordLabelRelRepository.getRecordLabelRelsByRecordId(record.id)
                val labels = labelRels.mapNotNull { rel ->
                    labelRepository.getLabelById(rel.labelId)
                }
                RecordWithLabels(record, labels)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDistinctDates(): List<String> {
        return try {
            val userId = getCurrentUserId()
            val records = recordRepository.getRecordsByUser(userId)
            records.map { it.date }
                .distinct()
                .sortedDescending()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRecordsWithLabelsByDate(
        date: String?,
        sortOption: RecordSortOption = currentSortOption
    ): List<RecordWithLabels> {
        return try {
            val userId = getCurrentUserId()
            val records = if (date == null) {
                recordRepository.getRecordsByUser(userId, sortOption)
            } else {
                recordRepository.getRecordsByUser(userId, sortOption).filter { it.date == date }
            }
            records.map { record ->
                val labelRels = recordLabelRelRepository.getRecordLabelRelsByRecordId(record.id)
                val labels = labelRels.mapNotNull { rel ->
                    labelRepository.getLabelById(rel.labelId)
                }
                RecordWithLabels(record, labels)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteRecordById(recordId: String) {
        try {
            // Delete record-label relationships first
            recordLabelRelRepository.deleteRecordLabelRelsByRecordId(recordId)
            // Delete the record
            recordRepository.deleteRecordById(recordId)
        } catch (e: Exception) {
            throw e
        }
    }

    fun updateRecord(recordId: String, note: String?, labelIds: List<String>) {
        viewModelScope.launch {
            try {
                // Get existing record
                val existingRecord = recordRepository.getRecordById(recordId)
                if (existingRecord == null) {
                    _saveResult.postValue(Result.failure(Exception("Record not found")))
                    return@launch
                }

                // Update record with new note and updated timestamp
                val updatedRecord = existingRecord.copy(
                    note = note,
                    updatedAt = System.currentTimeMillis()
                )
                recordRepository.updateRecord(updatedRecord)

                // Update label relationships: delete old ones and create new ones
                recordLabelRelRepository.deleteRecordLabelRelsByRecordId(recordId)
                labelIds.forEach { labelId ->
                    val rel = RecordLabelRel(
                        id = UUID.randomUUID().toString(),
                        recordId = recordId,
                        labelId = labelId
                    )
                    recordLabelRelRepository.insertRecordLabelRel(rel)
                }

                _saveResult.postValue(Result.success(recordId))
            } catch (e: Exception) {
                _saveResult.postValue(Result.failure(e))
            }
        }
    }

    suspend fun getRecordWithLabels(recordId: String): RecordWithLabels? {
        return try {
            val record = recordRepository.getRecordById(recordId) ?: return null
            val labelRels = recordLabelRelRepository.getRecordLabelRelsByRecordId(recordId)
            val labels = labelRels.mapNotNull { rel ->
                labelRepository.getLabelById(rel.labelId)
            }
            RecordWithLabels(record, labels)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchRecords(query: String): List<RecordWithLabels> {
        return try {
            val userId = getCurrentUserId()
            if (query.isBlank()) {
                // 如果查询为空，返回所有记录
                getRecordsWithLabels()
            } else {
                // 搜索笔记和标签，合并结果
                val recordsByNote = recordRepository.searchRecordsByNote(userId, query)
                val recordsByLabel = recordRepository.searchRecordsByLabel(userId, query)

                // 合并并去重
                val allRecords = (recordsByNote + recordsByLabel)
                    .distinctBy { it.id }

                // 转换为 RecordWithLabels
                allRecords.map { record ->
                    val labelRels = recordLabelRelRepository.getRecordLabelRelsByRecordId(record.id)
                    val labels = labelRels.mapNotNull { rel ->
                        labelRepository.getLabelById(rel.labelId)
                    }
                    RecordWithLabels(record, labels)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Label management methods
    fun updateLabel(labelId: String, name: String) {
        viewModelScope.launch {
            try {
                val existingLabel = labelRepository.getLabelById(labelId)
                if (existingLabel == null) {
                    _createLabelResult.postValue(Result.failure(Exception("Label not found")))
                    return@launch
                }

                val userId = getCurrentUserId()
                // Check if new name conflicts with another label
                val labelWithSameName = labelRepository.getLabelByName(userId, name)
                if (labelWithSameName != null && labelWithSameName.id != labelId) {
                    _createLabelResult.postValue(Result.failure(Exception("Label name already exists")))
                    return@launch
                }

                val updatedLabel = existingLabel.copy(
                    name = name,
                    updatedAt = System.currentTimeMillis()
                )
                labelRepository.updateLabel(updatedLabel)
                _createLabelResult.postValue(Result.success(updatedLabel))

                loadLabels()
            } catch (e: Exception) {
                _createLabelResult.postValue(Result.failure(e))
            }
        }
    }

    fun deleteLabel(labelId: String) {
        viewModelScope.launch {
            try {
                // Delete all record-label relationships for this label
                recordLabelRelRepository.deleteRecordLabelRelsByLabelId(labelId)
                // Delete the label
                labelRepository.deleteLabelById(labelId)
                _saveResult.postValue(Result.success(labelId))

                loadLabels()
            } catch (e: Exception) {
                _saveResult.postValue(Result.failure(e))
            }
        }
    }

    suspend fun getLabelUsageCount(labelId: String): Int {
        return try {
            recordLabelRelRepository.getRecordLabelRelsByLabelId(labelId).size
        } catch (e: Exception) {
            0
        }
    }

    // ==================== 同步相关方法 ====================

    /**
     * 检查用户是否已登录到服务器
     */
    fun isLoggedInToServer(): Boolean = authManager.isLoggedIn()

    /**
     * 登录后触发同步
     */
    fun syncAfterLogin() {
        viewModelScope.launch {
            _isSyncing.postValue(true)
            try {
                val result = syncManager.fullSync()
                if (result.isSuccess) {
                    // 同步成功后直接获取数据并更新 LiveData，确保 UI 立即刷新
                    // 获取记录数据
                    val recordsWithLabels = getRecordsWithLabels()
                    _userRecords.postValue(recordsWithLabels.map { it.record })

                    // 获取标签数据
                    val userId = getCurrentUserId()
                    val labels = labelRepository.getLabelsByUser(userId)
                    _userLabels.postValue(labels)

                    _syncResult.postValue(Result.success("同步完成"))
                } else {
                    _syncResult.postValue(Result.failure(result.exceptionOrNull() ?: Exception("同步失败")))
                }
            } catch (e: Exception) {
                _syncResult.postValue(Result.failure(e))
            } finally {
                _isSyncing.postValue(false)
            }
        }
    }

    /**
     * 手动触发同步
     */
    fun manualSync() {
        if (!authManager.isLoggedIn()) {
            _syncResult.postValue(Result.failure(Exception("请先登录")))
            _isSyncing.postValue(false)
            return
        }
        syncAfterLogin()
    }

    /**
     * 立即同步单个记录到服务器
     */
    private suspend fun syncRecordToServer(record: Record, operationType: String) {
        try {
            // 检查登录状态
            if (!authManager.isLoggedIn()) {
                Log.w("Sync", "用户未登录，跳过同步")
                return
            }

            val accessToken = authManager.getAccessToken()
            Log.d("Sync", "开始同步记录：operation=$operationType, recordId=${record.id}, token=${if (accessToken != null) "exists" else "null"}")

            val apiService = authManager.getApiService()
            val token = "Bearer $accessToken"

            // 获取记录的标签 ID 列表
            val labelIds = recordLabelRelDao.getRecordLabelRelsByRecordId(record.id)
                .map { it.labelId }

            when (operationType) {
                "CREATE" -> {
                    val request = RecordRequest(
                        note = record.note,
                        timestamp = record.timestamp,
                        date = record.date,
                        time24 = record.time24,
                        time12 = record.time12,
                        amPm = record.amPm,
                        labelIds = labelIds.ifEmpty { null }
                    )
                    Log.d("Sync", "发送创建记录请求：${gson.toJson(request)}")
                    val response = apiService.createRecord(token, request)
                    Log.d("Sync", "创建记录响应：code=${response.code()}, message=${response.message()}")
                    if (response.isSuccessful && response.body()?.success == true) {
                        Log.d("Sync", "记录已同步到服务器：${record.id}")
                        // 同步完成后从服务器拉取最新数据
                        syncRecordsFromServer()
                    } else {
                        Log.e("Sync", "记录同步失败：${response.body()?.message}")
                    }
                }
                "UPDATE" -> {
                    val request = RecordRequest(
                        note = record.note,
                        timestamp = record.timestamp,
                        date = record.date,
                        time24 = record.time24,
                        time12 = record.time12,
                        amPm = record.amPm,
                        labelIds = labelIds.ifEmpty { null }
                    )
                    Log.d("Sync", "发送更新记录请求：${gson.toJson(request)}")
                    val response = apiService.updateRecord(token, record.id, request)
                    Log.d("Sync", "更新记录响应：code=${response.code()}, message=${response.message()}")
                    if (response.isSuccessful && response.body()?.success == true) {
                        Log.d("Sync", "记录已更新到服务器：${record.id}")
                        // 同步完成后从服务器拉取最新数据
                        syncRecordsFromServer()
                    } else {
                        Log.e("Sync", "记录更新失败：${response.body()?.message}")
                    }
                }
                "DELETE" -> {
                    Log.d("Sync", "发送删除记录请求：recordId=${record.id}")
                    val response = apiService.deleteRecord(token, record.id)
                    Log.d("Sync", "删除记录响应：code=${response.code()}, message=${response.message()}")
                    if (response.isSuccessful && response.body()?.success == true) {
                        Log.d("Sync", "记录已从服务器删除：${record.id}")
                        // 同步完成后从服务器拉取最新数据
                        syncRecordsFromServer()
                    } else {
                        Log.e("Sync", "记录删除失败：${response.body()?.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "同步记录失败", e)
            // 如果立即同步失败，记录到待同步队列
            when (operationType) {
                "CREATE", "UPDATE" -> syncManager.recordCreateOperation(record)
                "DELETE" -> syncManager.recordDeleteOperation(record.id)
            }
        }
    }

    /**
     * 从服务器拉取记录数据到本地
     */
    private suspend fun syncRecordsFromServer() {
        try {
            if (!authManager.isLoggedIn()) return

            val apiService = authManager.getApiService()
            val token = "Bearer ${authManager.getAccessToken()}"

            Log.d("Sync", "从服务器拉取记录数据...")
            val response = apiService.getRecords(token)

            if (response.isSuccessful && response.body()?.success == true) {
                val serverRecords = response.body()?.data ?: return
                Log.d("Sync", "服务器记录数量：${serverRecords.size}")

                // 获取本地所有记录
                val localUserId = getCurrentUserId()
                val localRecords = recordRepository.getRecordsByUser(localUserId)
                val localRecordIds = localRecords.map { it.id }.toSet()
                val serverRecordIds = serverRecords.map { it.id }.toSet()

                // 1. 更新或插入服务器记录
                serverRecords.forEach { serverRecord ->
                    val localRecord = localRecords.find { it.id == serverRecord.id }
                    val createdAt = serverRecord.createdAt?.let { parseIso8601(it) } ?: System.currentTimeMillis()
                    val updatedAt = serverRecord.updatedAt?.let { parseIso8601(it) } ?: createdAt

                    val record = Record(
                        id = serverRecord.id,
                        userId = localUserId,
                        timestamp = serverRecord.timestamp,
                        date = serverRecord.date,
                        time24 = serverRecord.time24,
                        time12 = serverRecord.time12,
                        amPm = serverRecord.amPm,
                        note = serverRecord.note,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )

                    if (localRecord == null) {
                        // 本地不存在，插入新记录
                        recordRepository.insertRecord(record)
                        Log.d("Sync", "从服务器新增记录：${serverRecord.id}")
                    } else if (localRecord.updatedAt < updatedAt) {
                        // 服务器数据更新，更新本地
                        recordRepository.updateRecord(record)
                        Log.d("Sync", "更新本地记录：${serverRecord.id}")
                    }

                    // 同步标签关联关系：先删除旧的，再插入新的
                    recordLabelRelRepository.deleteRecordLabelRelsByRecordId(serverRecord.id)
                    serverRecord.labels?.forEach { labelResponse ->
                        val rel = RecordLabelRel(
                            id = UUID.randomUUID().toString(),
                            recordId = serverRecord.id,
                            labelId = labelResponse.id
                        )
                        recordLabelRelRepository.insertRecordLabelRel(rel)
                        Log.d("Sync", "同步标签关联：recordId=${serverRecord.id}, labelId=${labelResponse.id}")
                    }
                }

                // 2. 删除本地有但服务器没有的记录（服务器被删除）
                val recordsToDelete = localRecordIds - serverRecordIds
                recordsToDelete.forEach { recordId ->
                    recordRepository.deleteRecordById(recordId)
                    Log.d("Sync", "删除本地记录（服务器已删除）: $recordId")
                }

                // 重新加载记录
                loadRecords()

            } else {
                Log.e("Sync", "拉取记录失败：${response.body()?.message}")
            }
        } catch (e: Exception) {
            Log.e("Sync", "拉取记录异常", e)
        }
    }

    /**
     * 立即同步单个标签到服务器
     */
    private suspend fun syncLabelToServer(label: Label, operationType: String) {
        try {
            if (!authManager.isLoggedIn()) {
                Log.w("Sync", "用户未登录，跳过同步")
                return
            }

            val apiService = authManager.getApiService()
            val token = "Bearer ${authManager.getAccessToken()}"

            when (operationType) {
                "CREATE" -> {
                    val request = LabelRequest(name = label.name, color = label.color)
                    val response = apiService.createLabel(token, request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        Log.d("Sync", "标签已同步到服务器：${label.id}")
                        // 同步完成后从服务器拉取最新数据
                        syncLabelsFromServer()
                    }
                }
                "UPDATE" -> {
                    val request = LabelRequest(name = label.name, color = label.color)
                    val response = apiService.updateLabel(token, label.id, request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        Log.d("Sync", "标签已更新到服务器：${label.id}")
                        // 同步完成后从服务器拉取最新数据
                        syncLabelsFromServer()
                    }
                }
                "DELETE" -> {
                    val response = apiService.deleteLabel(token, label.id)
                    if (response.isSuccessful && response.body()?.success == true) {
                        Log.d("Sync", "标签已从服务器删除：${label.id}")
                        // 同步完成后从服务器拉取最新数据
                        syncLabelsFromServer()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "同步标签失败", e)
            // 如果立即同步失败，记录到待同步队列
            when (operationType) {
                "CREATE", "UPDATE" -> syncManager.recordLabelCreateOperation(label)
                "DELETE" -> syncManager.recordLabelDeleteOperation(label.id)
            }
        }
    }

    /**
     * 从服务器拉取标签数据到本地
     */
    private suspend fun syncLabelsFromServer() {
        try {
            if (!authManager.isLoggedIn()) return

            val apiService = authManager.getApiService()
            val token = "Bearer ${authManager.getAccessToken()}"

            Log.d("Sync", "从服务器拉取标签数据...")
            val response = apiService.getLabels(token)

            if (response.isSuccessful && response.body()?.success == true) {
                val serverLabels = response.body()?.data ?: return
                Log.d("Sync", "服务器标签数量：${serverLabels.size}")

                // 获取本地所有标签
                val localUserId = getCurrentUserId()
                val localLabels = labelRepository.getLabelsByUser(localUserId)
                val localLabelIds = localLabels.map { it.id }.toSet()
                val serverLabelIds = serverLabels.map { it.id }.toSet()

                // 1. 更新或插入服务器标签
                serverLabels.forEach { serverLabel ->
                    val localLabel = localLabels.find { it.id == serverLabel.id }
                    val createdAt = serverLabel.createdAt?.let { parseIso8601(it) } ?: System.currentTimeMillis()
                    val updatedAt = serverLabel.updatedAt?.let { parseIso8601(it) } ?: createdAt

                    val label = Label(
                        id = serverLabel.id,
                        userId = localUserId,
                        name = serverLabel.name,
                        color = serverLabel.color,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )

                    if (localLabel == null) {
                        // 本地不存在，插入新标签
                        labelRepository.insertLabel(label)
                        Log.d("Sync", "从服务器新增标签：${serverLabel.id}")
                    } else if (localLabel.updatedAt < updatedAt) {
                        // 服务器数据更新，更新本地
                        labelRepository.updateLabel(label)
                        Log.d("Sync", "更新本地标签：${serverLabel.id}")
                    }
                }

                // 2. 删除本地有但服务器没有的标签（服务器被删除）
                val labelsToDelete = localLabelIds - serverLabelIds
                labelsToDelete.forEach { labelId ->
                    labelRepository.deleteLabelById(labelId)
                    Log.d("Sync", "删除本地标签（服务器已删除）: $labelId")
                }

                // 重新加载标签
                loadLabels()

            } else {
                Log.e("Sync", "拉取标签失败：${response.body()?.message}")
            }
        } catch (e: Exception) {
            Log.e("Sync", "拉取标签异常", e)
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
     * 创建记录并立即同步到服务器
     */
    fun createRecordWithSync(note: String?, labelIds: List<String>) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val recordId = recordRepository.createRecord(userId, note)

                Log.d("Sync", "创建记录：recordId=$recordId, note=$note, labelIds=$labelIds")

                labelIds.forEach { labelId ->
                    val rel = RecordLabelRel(
                        id = UUID.randomUUID().toString(),
                        recordId = recordId,
                        labelId = labelId
                    )
                    recordLabelRelRepository.insertRecordLabelRel(rel)
                    Log.d("Sync", "保存标签关联：recordId=$recordId, labelId=$labelId")
                }

                // 如果已登录服务器，立即同步到服务器
                if (authManager.isLoggedIn()) {
                    val record = recordRepository.getRecordById(recordId)
                    Log.d("Sync", "准备同步记录到服务器：recordId=$recordId, isLoggedIn=true")
                    record?.let {
                        // 验证标签关联是否已保存
                        val savedLabelIds = recordLabelRelDao.getRecordLabelRelsByRecordId(recordId)
                            .map { it.labelId }
                        Log.d("Sync", "数据库中记录的标签 ID: $savedLabelIds")
                        syncRecordToServer(it, "CREATE")
                    }
                } else {
                    Log.w("Sync", "用户未登录，跳过同步")
                }

                _saveResult.postValue(Result.success(recordId))
            } catch (e: Exception) {
                Log.e("Sync", "创建记录失败", e)
                _saveResult.postValue(Result.failure(e))
            }
        }
    }

    /**
     * 更新记录并立即同步到服务器
     */
    fun updateRecordWithSync(recordId: String, note: String?, labelIds: List<String>) {
        viewModelScope.launch {
            try {
                val existingRecord = recordRepository.getRecordById(recordId)
                if (existingRecord == null) {
                    _saveResult.postValue(Result.failure(Exception("Record not found")))
                    return@launch
                }

                val updatedRecord = existingRecord.copy(
                    note = note,
                    updatedAt = System.currentTimeMillis()
                )
                recordRepository.updateRecord(updatedRecord)

                // 更新标签关系
                recordLabelRelRepository.deleteRecordLabelRelsByRecordId(recordId)
                labelIds.forEach { labelId ->
                    val rel = RecordLabelRel(
                        id = UUID.randomUUID().toString(),
                        recordId = recordId,
                        labelId = labelId
                    )
                    recordLabelRelRepository.insertRecordLabelRel(rel)
                }

                // 如果已登录服务器，立即同步到服务器
                if (authManager.isLoggedIn()) {
                    syncRecordToServer(updatedRecord, "UPDATE")
                }

                _saveResult.postValue(Result.success(recordId))
            } catch (e: Exception) {
                _saveResult.postValue(Result.failure(e))
            }
        }
    }

    /**
     * 删除记录并从服务器删除
     */
    fun deleteRecordWithSync(recordId: String) {
        viewModelScope.launch {
            try {
                // 如果已登录服务器，先从服务器删除
                if (authManager.isLoggedIn()) {
                    syncRecordToServer(Record(
                        id = recordId,
                        userId = "",
                        timestamp = 0,
                        date = "",
                        time12 = "",
                        amPm = "",
                        time24 = "",
                        note = null,
                        createdAt = 0,
                        updatedAt = 0
                    ), "DELETE")
                }

                // 删除 record-label relationships
                recordLabelRelRepository.deleteRecordLabelRelsByRecordId(recordId)
                // 删除记录
                recordRepository.deleteRecordById(recordId)
            } catch (e: Exception) {
                _saveResult.postValue(Result.failure(e))
            }
        }
    }

    /**
     * 创建标签并立即同步到服务器
     */
    fun createLabelWithSync(name: String, color: String? = null) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val existingLabel = labelRepository.getLabelByName(userId, name)
                if (existingLabel != null) {
                    _createLabelResult.postValue(Result.failure(Exception("Label already exists")))
                    return@launch
                }

                val label = Label(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = name,
                    color = color,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                labelRepository.insertLabel(label)

                // 如果已登录服务器，立即同步到服务器
                if (authManager.isLoggedIn()) {
                    syncLabelToServer(label, "CREATE")
                }

                _createLabelResult.postValue(Result.success(label))

                loadLabels()
            } catch (e: Exception) {
                _createLabelResult.postValue(Result.failure(e))
            }
        }
    }

    /**
     * 更新标签并立即同步到服务器
     */
    fun updateLabelWithSync(labelId: String, name: String) {
        viewModelScope.launch {
            try {
                val existingLabel = labelRepository.getLabelById(labelId)
                if (existingLabel == null) {
                    _createLabelResult.postValue(Result.failure(Exception("Label not found")))
                    return@launch
                }

                val userId = getCurrentUserId()
                val labelWithSameName = labelRepository.getLabelByName(userId, name)
                if (labelWithSameName != null && labelWithSameName.id != labelId) {
                    _createLabelResult.postValue(Result.failure(Exception("Label name already exists")))
                    return@launch
                }

                val updatedLabel = existingLabel.copy(
                    name = name,
                    updatedAt = System.currentTimeMillis()
                )
                labelRepository.updateLabel(updatedLabel)

                // 如果已登录服务器，立即同步到服务器
                if (authManager.isLoggedIn()) {
                    syncLabelToServer(updatedLabel, "UPDATE")
                }

                _createLabelResult.postValue(Result.success(updatedLabel))

                loadLabels()
            } catch (e: Exception) {
                _createLabelResult.postValue(Result.failure(e))
            }
        }
    }

    /**
     * 删除标签并从服务器删除
     */
    fun deleteLabelWithSync(labelId: String) {
        viewModelScope.launch {
            try {
                // 如果已登录服务器，立即从服务器删除
                if (authManager.isLoggedIn()) {
                    syncLabelToServer(
                        Label(
                            id = labelId,
                            userId = "",
                            name = "",
                            color = null,
                            createdAt = 0,
                            updatedAt = 0
                        ),
                        "DELETE"
                    )
                }

                // 删除 record-label relationships
                recordLabelRelRepository.deleteRecordLabelRelsByLabelId(labelId)
                // 删除标签
                labelRepository.deleteLabelById(labelId)
                _saveResult.postValue(Result.success(labelId))

                loadLabels()
            } catch (e: Exception) {
                _saveResult.postValue(Result.failure(e))
            }
        }
    }
}

data class RecordWithLabels(
    val record: Record,
    val labels: List<Label>
)
