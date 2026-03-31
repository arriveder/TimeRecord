package com.example.timerecord.worker

import android.content.Context
import androidx.work.*
import com.example.timerecord.auth.AuthManager
import java.util.concurrent.TimeUnit

/**
 * 同步任务调度器
 * 负责安排和取消定期同步任务
 */
object SyncScheduler {

    private const val SYNC_INTERVAL_HOURS = 1L // 同步间隔（小时）

    /**
     * 安排定期同步任务
     * 应在用户登录后调用
     */
    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(SyncWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }

    /**
     * 取消定期同步任务
     * 应在用户登出后调用
     */
    fun cancelPeriodicSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.WORK_NAME)
    }

    /**
     * 立即执行一次同步任务
     */
    fun scheduleImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag(SyncWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueue(syncRequest)
    }
}
