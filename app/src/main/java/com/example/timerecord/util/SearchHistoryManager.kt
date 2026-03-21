package com.example.timerecord.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SearchHistoryManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "search_history_prefs"
        private const val KEY_HISTORY = "search_history_list"
        private const val MAX_HISTORY_SIZE = 10
    }

    /**
     * 添加搜索历史
     */
    fun addHistory(query: String) {
        if (query.isBlank()) return

        val history = getHistory().toMutableList()

        // 如果已存在，先移除（保证最新的在最前面）
        history.remove(query)

        // 添加到开头
        history.add(0, query)

        // 限制数量
        while (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(history.lastIndex)
        }

        saveHistory(history)
    }

    /**
     * 获取搜索历史
     */
    fun getHistory(): List<String> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 删除单条历史
     */
    fun removeHistory(query: String) {
        val history = getHistory().toMutableList()
        history.remove(query)
        saveHistory(history)
    }

    /**
     * 清空所有历史
     */
    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun saveHistory(history: List<String>) {
        val json = gson.toJson(history)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }
}
