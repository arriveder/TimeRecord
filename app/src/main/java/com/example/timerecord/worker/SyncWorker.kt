package com.example.timerecord.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.timerecord.AppDatabase
import com.example.timerecord.auth.AuthManager
import com.example.timerecord.sync.SyncManager

/**
 * 后台同步 Worker
 * 定期在后台执行数据同步任务
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "sync_worker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "开始执行后台同步")

            val authManager = AuthManager.getInstance(applicationContext)

            // 如果用户未登录，跳过同步
            if (!authManager.isLoggedIn()) {
                Log.d(TAG, "用户未登录，跳过同步")
                return Result.success()
            }

            val database = AppDatabase.getDatabase(applicationContext)
            val syncManager = SyncManager(applicationContext, authManager, database)

            // 执行同步
            val syncResult = syncManager.fullSync()

            if (syncResult.isSuccess) {
                Log.d(TAG, "后台同步成功")
                Result.success()
            } else {
                Log.e(TAG, "后台同步失败", syncResult.exceptionOrNull())
                // 失败后重试
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "后台同步异常", e)
            Result.retry()
        }
    }
}
