package com.example.muzo.data.model

enum class ShelfType { MOOD_CHIPS, PLAYLIST_CARDS, SONG_CARDS, GENRE_GRID }
enum class ItemType { SONG, PLAYLIST, ALBUM, ARTIST, CHART }

data class HomeShelf(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val type: ShelfType,
    val items: List<ShelfItem>,
    val seeAllRoute: String? = null
)

data class ShelfItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrls: List<String>, // Size 1 = Single, Size 4 = Collage
    val type: ItemType
)
