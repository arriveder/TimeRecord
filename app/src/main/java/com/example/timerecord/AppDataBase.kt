package com.example.timerecord

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.timerecord.dao.RecordDao
import com.example.timerecord.dao.RecordLabelRelDao
import com.example.timerecord.dao.LabelDao
import com.example.timerecord.dao.UserDao
import com.example.timerecord.dao.PendingOperationDao
import com.example.timerecord.entity.Record
import com.example.timerecord.entity.RecordLabelRel
import com.example.timerecord.entity.Label
import com.example.timerecord.entity.User
import com.example.timerecord.entity.PendingOperationEntity

@Database(
    entities = [
        User::class,
        Record::class,
        Label::class,
        RecordLabelRel::class,
        PendingOperationEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun recordDao(): RecordDao

    abstract fun labelDao(): LabelDao

    abstract fun recordLabelRelDao(): RecordLabelRelDao

    abstract fun pendingOperationDao(): PendingOperationDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
