package com.example.muzo.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class PinnedItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val thumbnailUrl: String?,
    val type: String
)

object SpeedDialManager {
    private const val PREFS_NAME = "muzo_speed_dial_prefs"
    private const val KEY_PINNED_ITEMS = "pinned_items_json"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isPinned(context: Context, id: String): Boolean {
        return getPinnedItems(context).any { it.id == id }
    }

    fun getPinnedItems(context: Context): List<PinnedItem> {
        val jsonStr = getPrefs(context).getString(KEY_PINNED_ITEMS, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<PinnedItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PinnedItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        subtitle = obj.optString("subtitle", null),
                        thumbnailUrl = obj.optString("thumbnailUrl", null),
                        type = obj.getString("type")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Toggles pin state. Returns true if pinned, false if unpinned.
     */
    fun togglePin(
        context: Context,
        id: String,
        title: String,
        subtitle: String?,
        thumbnailUrl: String?,
        type: String
    ): Boolean {
        val items = getPinnedItems(context).toMutableList()
        val exists = items.any { it.id == id }
        val nowPinned: Boolean

        if (exists) {
            items.removeAll { it.id == id }
            nowPinned = false
        } else {
            items.add(
                0,
                PinnedItem(
                    id = id,
                    title = title,
                    subtitle = subtitle,
                    thumbnailUrl = thumbnailUrl,
                    type = type
                )
            )
            nowPinned = true
        }

        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("subtitle", item.subtitle ?: "")
                put("thumbnailUrl", item.thumbnailUrl ?: "")
                put("type", item.type)
            }
            array.put(obj)
        }

        getPrefs(context).edit().putString(KEY_PINNED_ITEMS, array.toString()).apply()
        return nowPinned
    }
}
