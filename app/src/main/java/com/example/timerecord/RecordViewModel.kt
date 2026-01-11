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
    private val database = com.example.timerecord.AppDatabase.getDatabase(application)
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
}

data class RecordWithLabels(
    val record: Record,
    val labels: List<Label>
)
