package com.example.muzo.data

import android.content.Context
import org.json.JSONArray

class UserPreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("muzi_user_prefs", Context.MODE_PRIVATE)

    fun saveGenres(genres: List<String>) {
        val jsonArray = JSONArray()
        genres.forEach { jsonArray.put(it) }
        prefs.edit().putString("selected_genres", jsonArray.toString()).apply()
    }

    fun getGenres(): List<String> {
        val jsonStr = prefs.getString("selected_genres", null) ?: return emptyList()
        val genres = mutableListOf<String>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) { genres.add(arr.getString(i)) }
        } catch (e: Exception) { e.printStackTrace() }
        return genres
    }
}
