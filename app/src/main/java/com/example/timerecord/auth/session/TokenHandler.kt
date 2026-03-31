package com.example.timerecord.auth.session

import android.content.Context
import com.example.timerecord.auth.AuthManager
import com.example.timerecord.network.RetrofitClient
import com.example.timerecord.network.dto.RefreshTokenRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * Token 处理器
 *
 * 职责：
 * 1. 管理 Access Token 的获取和刷新
 * 2. 提供带 Token 的 API 服务
 * 3. 处理 Token 过期自动刷新
 */
class TokenHandler private constructor(
    private val context: Context,
    private val authManager: AuthManager
) {

    private val _tokenState = MutableStateFlow<TokenState>(TokenState.Unauthorized)
    val tokenState: StateFlow<TokenState> = _tokenState.asStateFlow()

    /**
     * 获取当前的 Access Token
     * @return Access Token，未登录时返回 null
     */
    fun getAccessToken(): String? = runBlocking {
        authManager.getAccessToken()
    }

    /**
     * 获取当前的 Refresh Token
     */
    fun getRefreshToken(): String? = runBlocking {
        authManager.getRefreshToken()
    }

    /**
     * 获取有效的 Access Token，如果已过期则尝试刷新
     * @return Access Token
     * @throws IllegalStateException 如果刷新失败
     */
    suspend fun getValidAccessToken(): String {
        val accessToken = authManager.getAccessToken()
        val refreshToken = authManager.getRefreshToken()

        if (accessToken == null) {
            throw IllegalStateException("未登录")
        }

        // TODO: 检查 token 是否过期（需要解析 JWT 或捕获 401 响应）
        // 目前简化处理：直接返回，如果 401 再由调用方处理刷新

        return accessToken
    }

    /**
     * 刷新 Token
     * @return 刷新后的新 Access Token
     * @throws Exception 刷新失败时抛出异常
     */
    suspend fun refreshTokens(): String {
        val refreshToken = getRefreshToken()
            ?: throw IllegalStateException("未登录或 Refresh Token 不存在")

        val response = RetrofitClient.apiService.refreshToken(
            RefreshTokenRequest(refreshToken)
        )

        if (response.isSuccessful && response.body()?.success == true) {
            val authResponse = response.body()?.data
                ?: throw IllegalStateException("刷新响应为空")

            // 保存新 Token
            authManager.saveTokens(authResponse.accessToken, authResponse.refreshToken)

            // 更新状态
            _tokenState.value = TokenState.Authorized(authResponse.accessToken)

            return authResponse.accessToken
        } else {
            val error = response.body()?.message ?: "Token 刷新失败"
            _tokenState.value = TokenState.Unauthorized
            throw IllegalStateException(error)
        }
    }

    /**
     * 获取认证头
     * @return "Bearer <token>" 格式的头信息
     * @throws IllegalStateException 未登录时抛出
     */
    suspend fun getAuthHeader(): String {
        val token = getValidAccessToken()
        return "Bearer $token"
    }

    /**
     * 监听登录状态变化
     */
    fun observeAuthState(): Flow<AuthState> {
        return authManager.isLoggedInFlow.map { isLoggedIn ->
            if (isLoggedIn) {
                AuthState.LoggedIn
            } else {
                AuthState.LoggedOut
            }
        }
    }

    companion object {
        @Volatile
        private var instance: TokenHandler? = null

        fun getInstance(context: Context, authManager: AuthManager): TokenHandler {
            return instance ?: TokenHandler(context.applicationContext, authManager).also {
                instance = it
            }
        }
    }
}

/**
 * Token 状态
 */
sealed class TokenState {
    object Unauthorized : TokenState()
    data class Authorized(val accessToken: String) : TokenState()
}

/**
 * 认证状态
 */
sealed class AuthState {
    object LoggedIn : AuthState()
    object LoggedOut : AuthState()
}
