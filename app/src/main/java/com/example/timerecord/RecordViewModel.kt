package com.example.timerecord

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.timerecord.data.repository.LabelRepository
import com.example.timerecord.data.repository.RecordLabelRelRepository
import com.example.timerecord.data.repository.RecordRepository
import com.example.timerecord.data.repository.UserRepository
import com.example.timerecord.entity.Label
import com.example.timerecord.entity.Record
import com.example.timerecord.entity.RecordLabelRel
import com.example.timerecord.entity.User
import kotlinx.coroutines.launch
import java.util.UUID

class RecordViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val recordRepository = RecordRepository(database.recordDao())
    private val labelRepository = LabelRepository(database.labelDao())
    private val recordLabelRelRepository = RecordLabelRelRepository(database.recordLabelRelDao())
    private val userRepository = UserRepository(database.userDao())

    private val _saveResult = MutableLiveData<Result<String>>()
    val saveResult: LiveData<Result<String>> = _saveResult

    private val _userLabels = MutableLiveData<List<Label>>()
    val userLabels: LiveData<List<Label>> = _userLabels

    private val _createLabelResult = MutableLiveData<Result<Label>>()
    val createLabelResult: LiveData<Result<Label>> = _createLabelResult

    companion object {
        const val DEFAULT_USER_ID = "default_user"
    }

    init {
        ensureDefaultUserExists()
    }

    private fun ensureDefaultUserExists() {
        viewModelScope.launch {
            try {
                val user = userRepository.getUserById(DEFAULT_USER_ID)
                if (user == null) {
                    val defaultUser = User(
                        id = DEFAULT_USER_ID,
                        username = "默认用户",
                        email = null,
                        passwordHash = null,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    userRepository.insertUser(defaultUser)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun loadLabels(userId: String = DEFAULT_USER_ID) {
        viewModelScope.launch {
            try {
                val labels = labelRepository.getLabelsByUser(userId)
                _userLabels.postValue(labels)
            } catch (e: Exception) {
                _userLabels.postValue(emptyList())
            }
        }
    }

    fun createRecord(userId: String, note: String?, labelIds: List<String>) {
        viewModelScope.launch {
            try {
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

    fun createLabel(userId: String, name: String, color: String? = null) {
        viewModelScope.launch {
            try {
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

                loadLabels(userId)
            } catch (e: Exception) {
                _createLabelResult.postValue(Result.failure(e))
            }
        }
    }

    private val _userRecords = MutableLiveData<List<Record>>()
    val userRecords: LiveData<List<Record>> = _userRecords

    fun loadRecords(userId: String = DEFAULT_USER_ID) {
        viewModelScope.launch {
            try {
                val records = recordRepository.getRecordsByUser(userId)
                _userRecords.postValue(records)
            } catch (e: Exception) {
                _userRecords.postValue(emptyList())
            }
        }
    }

    suspend fun getRecordsWithLabels(userId: String = DEFAULT_USER_ID): List<RecordWithLabels> {
        return try {
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

    suspend fun getDistinctDates(userId: String = DEFAULT_USER_ID): List<String> {
        return try {
            val records = recordRepository.getRecordsByUser(userId)
            records.map { it.date }
                .distinct()
                .sortedDescending()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRecordsWithLabelsByDate(
        userId: String = DEFAULT_USER_ID,
        date: String?
    ): List<RecordWithLabels> {
        return try {
            val records = if (date == null) {
                recordRepository.getRecordsByUser(userId)
            } else {
                recordRepository.getRecordsByUser(userId).filter { it.date == date }
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

    // Label management methods
    fun updateLabel(labelId: String, name: String) {
        viewModelScope.launch {
            try {
                val existingLabel = labelRepository.getLabelById(labelId)
                if (existingLabel == null) {
                    _createLabelResult.postValue(Result.failure(Exception("Label not found")))
                    return@launch
                }

                // Check if new name conflicts with another label
                val labelWithSameName = labelRepository.getLabelByName(DEFAULT_USER_ID, name)
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

                loadLabels(DEFAULT_USER_ID)
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

                loadLabels(DEFAULT_USER_ID)
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
}

data class RecordWithLabels(
    val record: Record,
    val labels: List<Label>
)
