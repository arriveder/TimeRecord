package com.example.timerecord.auth

import android.content.Context
import com.example.timerecord.AppDatabase
import com.example.timerecord.data.repository.UserRepository
import com.example.timerecord.entity.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * 用户管理器
 *
 * 职责：
 * 1. 管理本地用户数据
 * 2. 同步服务器用户到本地数据库
 * 3. 提供当前用户查询
 */
class UserManager private constructor(
    private val context: Context,
    private val authManager: AuthManager,
    private val userRepository: UserRepository
) {

    /**
     * 获取当前用户 ID 的 Flow
     * 未登录时返回 null
     */
    val currentUserId: Flow<String?> = authManager.userIdFlow

    /**
     * 获取当前用户 ID
     * @return 用户 ID，未登录时返回 null
     */
    suspend fun getCurrentUserId(): String? {
        return authManager.getUserId()
    }

    /**
     * 获取当前用户 ID，如果未登录则抛出异常
     * @return 用户 ID
     * @throws IllegalStateException 如果用户未登录
     */
    suspend fun requireUserId(): String {
        return getCurrentUserId() ?: throw IllegalStateException("用户未登录")
    }

    /**
     * 获取当前用户用户名
     */
    suspend fun getUsername(): String? {
        return authManager.getUsername()
    }

    /**
     * 检查用户是否已登录
     */
    suspend fun isLoggedIn(): Boolean {
        return authManager.isLoggedIn()
    }

    /**
     * 同步服务器用户到本地数据库
     * 如果本地不存在则创建，存在则更新信息
     */
    suspend fun syncUserToLocal(userId: String, username: String, email: String?) {
        val existingUser = userRepository.getUserById(userId)
        if (existingUser == null) {
            // 本地不存在，创建新用户
            val newUser = User(
                id = userId,
                username = username,
                email = email,
                passwordHash = null, // 不保存服务器密码
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            userRepository.insertUser(newUser)
        } else {
            // 本地已存在，检查是否需要更新
            val needsUpdate = existingUser.username != username || existingUser.email != email
            if (needsUpdate) {
                val updatedUser = existingUser.copy(
                    username = username,
                    email = email,
                    updatedAt = System.currentTimeMillis()
                )
                userRepository.updateUser(updatedUser)
            }
        }
    }

    /**
     * 获取当前本地用户实体
     */
    suspend fun getCurrentUser(): User? {
        val userId = getCurrentUserId() ?: return null
        return userRepository.getUserById(userId)
    }

    /**
     * 清除本地用户数据（登出时调用）
     */
    suspend fun clearLocalUser() {
        // 注意：这里不清除 User 实体，因为可能有多设备共享同一个用户
        // 只在 AuthManager 中清除 Token 和用户信息
    }

    companion object {
        @Volatile
        private var instance: UserManager? = null

        fun getInstance(
            context: Context,
            authManager: AuthManager,
            database: AppDatabase
        ): UserManager {
            return instance ?: UserManager(
                context.applicationContext,
                authManager,
                UserRepository(database.userDao())
            ).also {
                instance = it
            }
        }
    }
}
