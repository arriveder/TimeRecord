package com.example.timerecord

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.timerecord.dao.RecordDao
import com.example.timerecord.dao.TypeDao
import com.example.timerecord.dao.TypeQueueDao
import com.example.timerecord.dao.UserDao
import com.example.timerecord.entity.Record
import com.example.timerecord.entity.Type
import com.example.timerecord.entity.TypeQueue
import com.example.timerecord.entity.User

@Database(entities = [Record::class, Type::class, TypeQueue::class, User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
    abstract fun typeDao(): TypeDao
    abstract fun userDao(): UserDao
    abstract fun typeQueueDao(): TypeQueueDao
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
