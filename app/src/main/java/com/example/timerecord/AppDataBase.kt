package com.example.timerecord

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.timerecord.dao.RecordDao
import com.example.timerecord.dao.RecordTagRelDao
import com.example.timerecord.dao.TagDao
import com.example.timerecord.dao.UserDao
import com.example.timerecord.entity.Record
import com.example.timerecord.entity.RecordTagRel
import com.example.timerecord.entity.Tag
import com.example.timerecord.entity.User

@Database(
    entities = [
        User::class,
        Record::class,
        Tag::class,
        RecordTagRel::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun recordDao(): RecordDao

    abstract fun tagDao(): TagDao

    abstract fun recordTagRelDao(): RecordTagRelDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
