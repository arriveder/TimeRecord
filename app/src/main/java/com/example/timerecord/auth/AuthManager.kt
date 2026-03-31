package com.example.timerecord.auth

import android.content.Context
import com.example.timerecord.AppDatabase
import com.example.timerecord.data.repository.UserRepository
import com.example.timerecord.entity.User
import com.example.timerecord.network.ApiService
import com.example.timerecord.network.RetrofitClient
import com.example.timerecord.network.dto.*
import com.example.timerecord.util.DataStoreManager
import com.example.timerecord.worker.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AuthManager private constructor(private val context: Context) {

    private val dataStoreManager = DataStoreManager(context)
    private var cachedToken: String? = null

    companion object {
        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return instance ?: AuthManager(context.applicationContext).also {
                instance = it
            }
        }
    }

    // ==================== 状态查询方法 ====================

    /**
     * 检查用户是否已登录（同步阻塞版本）
     */
    fun isLoggedIn(): Boolean = runBlocking {
        dataStoreManager.isLoggedIn.first()
    }

    /**
     * 检查用户是否已登录（suspend 版本）
     */
    suspend fun isLoggedInAsync(): Boolean {
        return dataStoreManager.isLoggedIn.first()
    }

    /**
     * 获取登录状态 Flow
     */
    val isLoggedInFlow: Flow<Boolean> = dataStoreManager.isLoggedIn

    /**
     * 获取 Access Token（同步阻塞版本）
     */
    fun getAccessToken(): String? = runBlocking {
        dataStoreManager.accessToken.first()
    }

    /**
     * 获取 Refresh Token（同步阻塞版本）
     */
    fun getRefreshToken(): String? = runBlocking {
        dataStoreManager.refreshToken.first()
    }

    /**
     * 获取用户 ID（suspend 版本）
     * @return 用户 ID，未登录时返回 null
     */
    suspend fun getUserId(): String? {
        return dataStoreManager.userId.first()
    }

    /**
     * 获取用户 ID，如果未登录则抛出异常
     * @return 用户 ID
     * @throws IllegalStateException 如果用户未登录
     */
    suspend fun requireUserId(): String {
        return getUserId() ?: throw IllegalStateException("用户未登录")
    }

    /**
     * 获取用户名（suspend 版本）
     */
    suspend fun getUsername(): String? {
        return dataStoreManager.username.first()
    }

    /**
     * 获取用户 ID Flow
     */
    val userIdFlow: Flow<String?> = dataStoreManager.userId

    /**
     * 获取用户名 Flow
     */
    val usernameFlow: Flow<String?> = dataStoreManager.username

    // ==================== 认证操作方法 ====================

    /**
     * 用户登录
     * @param usernameOrEmail 用户名或邮箱
     * @param password 密码
     * @return 登录成功返回 AuthResponse，失败返回 Exception
     */
    suspend fun login(usernameOrEmail: String, password: String): Result<AuthResponse> {
        return try {
            val response = RetrofitClient.apiService.login(
                LoginRequest(usernameOrEmail, password)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()?.data
                if (authResponse != null) {
                    // 保存 Token
                    saveTokens(authResponse.accessToken, authResponse.refreshToken)
                    // 保存用户信息并同步到本地数据库
                    authResponse.user?.let { user ->
                        saveUserInfo(user.id, user.username, user.email)
                        syncUserToLocal(user.id, user.username, user.email)
                    }
                    cachedToken = authResponse.accessToken

                    // 安排定期同步
                    SyncScheduler.schedulePeriodicSync(context)

                    Result.success(authResponse)
                } else {
                    Result.failure(Exception("登录响应为空"))
                }
            } else {
                Result.failure(Exception(response.body()?.message ?: "登录失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 用户注册
     * @param username 用户名
     * @param email 邮箱
     * @param code 验证码
     * @param password 密码
     * @return 注册成功返回 AuthResponse，失败返回 Exception
     */
    suspend fun register(username: String, email: String, code: String, password: String): Result<AuthResponse> {
        return try {
            val response = RetrofitClient.apiService.register(
                RegisterRequest(username, email, code, password)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()?.data
                if (authResponse != null) {
                    // 保存 Token
                    saveTokens(authResponse.accessToken, authResponse.refreshToken)
                    // 保存用户信息并同步到本地数据库
                    authResponse.user?.let { user ->
                        saveUserInfo(user.id, user.username, user.email)
                        syncUserToLocal(user.id, user.username, user.email)
                    }
                    cachedToken = authResponse.accessToken

                    // 安排定期同步
                    SyncScheduler.schedulePeriodicSync(context)

                    Result.success(authResponse)
                } else {
                    Result.failure(Exception("注册响应为空"))
                }
            } else {
                Result.failure(Exception(response.body()?.message ?: "注册失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 发送验证码
     */
    suspend fun sendVerificationCode(email: String, type: String = "REGISTER"): Result<Unit> {
        return try {
            val response = RetrofitClient.apiService.sendVerificationCode(
                SendCodeRequest(email, type)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "发送验证码失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 登出
     */
    suspend fun logout() {
        dataStoreManager.logout()
        cachedToken = null

        // 取消定期同步
        SyncScheduler.cancelPeriodicSync(context)
    }

    // ==================== Token 相关方法 ====================

    /**
     * 保存 Token
     */
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStoreManager.saveTokens(accessToken, refreshToken)
    }

    /**
     * 刷新 Token
     */
    suspend fun refreshTokens(): Result<AuthResponse> {
        return try {
            val refreshToken = getRefreshToken()
                ?: return Result.failure(Exception("未登录或 Refresh Token 不存在"))

            val response = RetrofitClient.apiService.refreshToken(
                RefreshTokenRequest(refreshToken)
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()?.data
                if (authResponse != null) {
                    saveTokens(authResponse.accessToken, authResponse.refreshToken)
                    Result.success(authResponse)
                } else {
                    Result.failure(Exception("刷新响应为空"))
                }
            } else {
                Result.failure(Exception(response.body()?.message ?: "Token 刷新失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== API 服务获取 ====================

    /**
     * 获取带认证的 ApiService
     * @throws IllegalStateException 如果用户未登录
     */
    suspend fun getApiService(): ApiService {
        val token = cachedToken ?: getAccessToken() ?: throw IllegalStateException("未登录")
        return RetrofitClient.getApiServiceWithToken(token)
    }

    /**
     * 获取认证头
     */
    suspend fun getAuthHeader(): String {
        val token = cachedToken ?: getAccessToken() ?: throw IllegalStateException("未登录")
        return "Bearer $token"
    }

    // ==================== 私有方法 ====================

    /**
     * 保存用户信息
     */
    private suspend fun saveUserInfo(userId: String, username: String, email: String) {
        dataStoreManager.saveUserInfo(userId, username, email)
    }

    /**
     * 同步用户到本地数据库
     */
    private suspend fun syncUserToLocal(userId: String, username: String, email: String) {
        try {
            val database = AppDatabase.getDatabase(context)
            val userDao = database.userDao()
            val existingUser = userDao.getUserById(userId)
            if (existingUser == null) {
                val localUser = User(
                    id = userId,
                    username = username,
                    email = email,
                    passwordHash = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                userDao.insertUser(localUser)
            } else {
                // 检查是否需要更新
                val needsUpdate = existingUser.username != username || existingUser.email != email
                if (needsUpdate) {
                    val updatedUser = existingUser.copy(
                        username = username,
                        email = email,
                        updatedAt = System.currentTimeMillis()
                    )
                    userDao.updateUser(updatedUser)
                }
            }
        } catch (e: Exception) {
            // 记录错误但不影响登录流程
            android.util.Log.w("AuthManager", "同步用户到本地失败", e)
        }
    }
}
