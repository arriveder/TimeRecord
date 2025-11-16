package com.example.timerecord.data.repository

import com.example.timerecord.dao.RecordDao
import com.example.timerecord.entity.Record
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class RecordRepository(
    private val recordDao: RecordDao
) {
    suspend fun insertRecord(record: Record) = recordDao.insertRecord(record)

    suspend fun getRecordById(id: String) = recordDao.getRecordById(id)

    suspend fun getRecordsByUser(userId: String) =
        recordDao.getRecordsByUser(userId)

    suspend fun getRecordsByDate(userId: String, date: String) =
        recordDao.getRecordsByDate(userId, date)

    suspend fun deleteRecord(record: Record) = recordDao.deleteRecord(record)

    suspend fun deleteRecordById(id: String) = recordDao.deleteRecordById(id)

    suspend fun createRecord(
        userId: String,
        note: String? = null
    ) {
        val now = System.currentTimeMillis()

        val ldt = Instant.ofEpochMilli(now)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

        val date = ldt.toLocalDate().toString()  // YYYY-MM-DD

        val time24 = ldt.toLocalTime()
            .format(DateTimeFormatter.ofPattern("HH:mm"))

        val time12 = ldt.toLocalTime()
            .format(DateTimeFormatter.ofPattern("hh:mm"))

        val amPm = if (ldt.hour < 12) "AM" else "PM"

        val record = Record(
            id = UUID.randomUUID().toString(),
            userId = userId,
            timestamp = now,
            date = date,
            time12 = time12,
            amPm = amPm,
            time24 = time24,
            note = note,
            createdAt = now,
            updatedAt = now
        )

        recordDao.insertRecord(record)
    }
}