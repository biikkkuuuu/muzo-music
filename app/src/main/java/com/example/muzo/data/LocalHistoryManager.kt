package com.example.muzo.data

import android.content.Context
import com.music.innertube.models.SongItem
import org.json.JSONArray
import org.json.JSONObject

class LocalHistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("muzi_playback_history", Context.MODE_PRIVATE)

    fun addSong(song: SongItem) {
        val history = getHistory().toMutableList()
        history.removeAll { it.id == song.id }
        history.add(0, song)
        
        val jsonArray = JSONArray()
        history.take(50).forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("thumbnail", item.thumbnail)
                val artistName = try { item.artists?.firstOrNull()?.name ?: "" } catch(e: Exception) { "" }
                put("artistName", artistName)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("history_data", jsonArray.toString()).apply()
    }

    fun getHistory(): List<SongItem> {
        val jsonStr = prefs.getString("history_data", null) ?: return emptyList()
        val history = mutableListOf<SongItem>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                history.add(
                    SongItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artists = emptyList(),
                        album = null,
                        duration = 0,
                        thumbnail = obj.getString("thumbnail")
                    )
                )
            }
        } catch (e: Exception) { e.printStackTrace() }
        return history
    }

    // SPOTIFY FIX: Find the Most Frequent Artist, not just the last played one!
    fun getTopArtist(): String? {
        val jsonStr = prefs.getString("history_data", null) ?: return null
        try {
            val arr = JSONArray(jsonStr)
            val artistCounts = mutableMapOf<String, Int>()
            for (i in 0 until arr.length()) {
                val artist = arr.getJSONObject(i).optString("artistName", "")
                if (artist.isNotEmpty() && artist.length > 2) {
                    artistCounts[artist] = artistCounts.getOrDefault(artist, 0) + 1
                }
            }
            return artistCounts.maxByOrNull { it.value }?.key
        } catch(e: Exception) {}
        return null
    }
}
