package com.example.muzo.data.local

import android.content.Context
import org.json.JSONArray

object SearchHistoryManager {
    private const val PREFS_NAME = "muzi_search_history_prefs"
    private const val KEY_QUERIES = "recent_queries"
    private const val MAX_HISTORY = 20

    fun getHistory(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_QUERIES, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val q = jsonArray.optString(i, "").trim()
                if (q.isNotEmpty()) list.add(q)
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addQuery(context: Context, query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        val current = getHistory(context).toMutableList()
        current.removeAll { it.equals(clean, ignoreCase = true) }
        current.add(0, clean)
        val limited = current.take(MAX_HISTORY)
        saveHistory(context, limited)
    }

    fun removeQuery(context: Context, query: String) {
        val current = getHistory(context).toMutableList()
        current.removeAll { it.equals(query.trim(), ignoreCase = true) }
        saveHistory(context, current)
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_QUERIES).apply()
    }

    private fun saveHistory(context: Context, list: List<String>) {
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it) }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_QUERIES, jsonArray.toString()).apply()
    }
}
