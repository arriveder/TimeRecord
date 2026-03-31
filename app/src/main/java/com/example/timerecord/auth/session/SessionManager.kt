package com.example.timerecord.auth.session

import android.content.Context
import android.content.SharedPreferences
import com.example.timerecord.auth.AuthManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * 会话管理器
 *
 * 职责：
 * 1. 管理用户会话状态
 * 2. 监听登录/登出事件
 * 3. 提供会话信息（如 clientId 用于同步）
 */
class SessionManager private constructor(
    private val context: Context,
    private val authManager: AuthManager
) {

    private val prefs: SharedPreferences = context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.LoggedOut)
    val sessionState: Flow<SessionState> = _sessionState.asStateFlow()

    /**
     * 客户端唯一标识，用于数据同步
     */
    val clientId: String by lazy {
        val storedClientId = prefs.getString(KEY_CLIENT_ID, null)
        if (storedClientId == null) {
            val newClientId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_CLIENT_ID, newClientId).apply()
            newClientId
        } else {
            storedClientId
        }
    }

    /**
     * 获取最后同步时间戳
     */
    fun getLastSyncTimestamp(): Long? {
        val timestamp = prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, -1)
        return if (timestamp > 0) timestamp else null
    }

    /**
     * 保存最后同步时间戳
     */
    fun saveLastSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp).apply()
    }

    /**
     * 清除同步相关信息
     */
    fun clearSyncInfo() {
        prefs.edit().remove(KEY_LAST_SYNC_TIMESTAMP).apply()
    }

    /**
     * 监听认证状态变化
     */
    fun observeAuthState(): Flow<SessionState> {
        return authManager.isLoggedInFlow.map { isLoggedIn ->
            if (isLoggedIn) {
                SessionState.LoggedIn(clientId)
            } else {
                SessionState.LoggedOut
            }
        }.distinctUntilChanged()
            .onEach { state ->
                _sessionState.value = state
            }
    }

    /**
     * 初始化会话状态
     */
    fun initialize() {
        _sessionState.value = if (authManager.isLoggedIn()) {
            SessionState.LoggedIn(clientId)
        } else {
            SessionState.LoggedOut
        }
    }

    companion object {
        private const val SESSION_PREFS = "session_prefs"
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context, authManager: AuthManager): SessionManager {
            return instance ?: SessionManager(context.applicationContext, authManager).also {
                instance = it
                it.initialize()
            }
        }
    }
}

/**
 * 会话状态
 */
sealed class SessionState {
    object LoggedOut : SessionState()
    data class LoggedIn(val clientId: String) : SessionState()
}
