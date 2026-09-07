package com.example.muzo.mock

data class MockLyricLine(
    val timeMs: Long,
    val text: String
)

data class MockSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val thumbnailUrl: String,
    val lyrics: List<MockLyricLine> = emptyList()
)

data class MockArtist(
    val id: String,
    val name: String,
    val thumbnailUrl: String,
    val subscribers: String = "12.4M",
    val monthlyListeners: String = "28.5M"
)

data class MockAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val year: String,
    val thumbnailUrl: String,
    val songCount: Int = 10
)

data class MockPlaylist(
    val id: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String,
    val songCount: Int = 25
)

data class MockShelf(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val seeAllRoute: String? = null,
    val songs: List<MockSong>
)

/**
 * Pre-populated rich mock repository with realistic album arts, tracks, and live timestamped lyrics.
 */
object MockDataRepository {

    val sampleLyrics1 = listOf(
        MockLyricLine(0L, "♪ (Gentle rain and soft melodic intro) ♪"),
        MockLyricLine(4000L, "Ajj din chadheya tere rang warga"),
        MockLyricLine(9500L, "Phool sa khila aaj din"),
        MockLyricLine(15000L, "Rabba mere din yeh na dhale"),
        MockLyricLine(21000L, "Woh jo mujhe khwaab mein mile"),
        MockLyricLine(27500L, "Uske bina jeena nahi ab gawara"),
        MockLyricLine(34000L, "Ajj din chadheya tere rang warga"),
        MockLyricLine(41000L, "Bakhsha gunaahon ko, sun ke duwaaon ko"),
        MockLyricLine(48000L, "Rabba pyaar hai bas tera sahaara"),
        MockLyricLine(55000L, "Ajj din chadheya..."),
        MockLyricLine(62000L, "♪ (Melodic acoustic guitar interlude) ♪"),
        MockLyricLine(74000L, "Maanga jo mera tha, jaata kya tera tha"),
        MockLyricLine(81500L, "Thoda sa guzaara kar deta tu"),
        MockLyricLine(89000L, "Koyi aur sahara nahi mera yahaan pe"),
        MockLyricLine(96000L, "Ajj din chadheya tere rang warga")
    )

    val sampleLyrics2 = listOf(
        MockLyricLine(0L, "♪ (Soft thunderstorm ambiance) ♪"),
        MockLyricLine(6000L, "Close your eyes and let the raindrops fall"),
        MockLyricLine(14000L, "Washing away the noise of the day"),
        MockLyricLine(22000L, "Distant thunder rolling across the night"),
        MockLyricLine(31000L, "Peaceful stillness in every drop"),
        MockLyricLine(40000L, "Drifting into calm dreams..."),
        MockLyricLine(50000L, "♪ (Deep soothing rumble & rain) ♪"),
        MockLyricLine(65000L, "The storm carries the silence"),
        MockLyricLine(80000L, "Rest now, the night has arrived")
    )

    val sampleLyrics3 = listOf(
        MockLyricLine(0L, "♪ (Warm synth intro) ♪"),
        MockLyricLine(5000L, "Kaisi paheli zindagani hai"),
        MockLyricLine(11000L, "Kabhi toh hasaaye, kabhi rulaye"),
        MockLyricLine(17500L, "Hum toh chal diye us rah par"),
        MockLyricLine(24000L, "Jahaan hawaayein le jaayein"),
        MockLyricLine(31000L, "♪ (Instrumental chorus) ♪"),
        MockLyricLine(40000L, "Har pal naya ek silsila"),
        MockLyricLine(47000L, "Yeh jo dil ko mila")
    )

    val allMockSongs = listOf(
        MockSong(
            id = "t2aEijUo-H0",
            title = "Ajj Din Chadheya",
            artist = "Rahat Fateh Ali Khan, Pritam",
            album = "Love Aaj Kal",
            durationMs = 105000L,
            thumbnailUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80",
            lyrics = sampleLyrics1
        ),
        MockSong(
            id = "q76bMs-NwRk",
            title = "Heavy Thunderstorm Sounds",
            artist = "Stardust Vibes",
            album = "Rain Therapy",
            durationMs = 95000L,
            thumbnailUrl = "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?w=600&auto=format&fit=crop&q=80",
            lyrics = sampleLyrics2
        ),
        MockSong(
            id = "q6Hq7-V6XEY",
            title = "Dil Ne Yeh Kaha Hai",
            artist = "Udit Narayan, Alka Yagnik",
            album = "Dhadkan",
            durationMs = 88000L,
            thumbnailUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            lyrics = sampleLyrics3
        ),
        MockSong(
            id = "hHuG7FIKgtc",
            title = "Apna Bana Le",
            artist = "Arijit Singh, Sachin-Jigar",
            album = "Bhediya",
            durationMs = 120000L,
            thumbnailUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=600&auto=format&fit=crop&q=80",
            lyrics = sampleLyrics1
        ),
        MockSong(
            id = "5Eqb_-j3FDA",
            title = "Tum Mere Na Huye",
            artist = "Sachin-Jigar",
            album = "Midnight Melodies",
            durationMs = 98000L,
            thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            lyrics = sampleLyrics3
        ),
        MockSong(
            id = "VAdGW7QDJiU",
            title = "Chaleya",
            artist = "Arijit Singh, Shilpa Rao",
            album = "Jawan",
            durationMs = 112000L,
            thumbnailUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            lyrics = sampleLyrics1
        ),
        MockSong(
            id = "5qap5aO4i9A",
            title = "Lofi Study Session",
            artist = "ChillHop Cafe",
            album = "Focus Beats",
            durationMs = 130000L,
            thumbnailUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80",
            lyrics = sampleLyrics2
        ),
        MockSong(
            id = "MVPTGNGiI-4",
            title = "Midnight Drive",
            artist = "Synthwave Collective",
            album = "Neon Horizon",
            durationMs = 100000L,
            thumbnailUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
            lyrics = sampleLyrics3
        )
    )

    val mockArtists = listOf(
        MockArtist("art_1", "Arijit Singh", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=600&auto=format&fit=crop&q=80", "42.1M", "85.2M"),
        MockArtist("art_2", "Pritam", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=600&auto=format&fit=crop&q=80", "18.3M", "34.0M"),
        MockArtist("art_3", "Rahat Fateh Ali Khan", "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=600&auto=format&fit=crop&q=80", "15.9M", "29.4M"),
        MockArtist("art_4", "Sachin-Jigar", "https://images.unsplash.com/photo-1463453091185-61582044d556?w=600&auto=format&fit=crop&q=80", "8.2M", "19.5M")
    )

    val mockPlaylists = listOf(
        MockPlaylist("pl_1", "Rain Therapy 🍀🌧️", "Echo Curators", "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?w=600&auto=format&fit=crop&q=80", 35),
        MockPlaylist("pl_2", "Focus & Study", "Metrolist Group", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80", 42),
        MockPlaylist("pl_3", "Night Chill Vibes", "Community Hub", "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80", 28),
        MockPlaylist("pl_4", "Romantic Moments", "Bollywood Hits", "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80", 50)
    )

    val mockHomeShelves = listOf(
        MockShelf(
            id = "shelf_rain_therapy",
            title = "Rain Therapy 🍀🌧️",
            subtitle = "FOR COZY DAYS AND ENDLESS CUPS OF TEA",
            seeAllRoute = "playlist/pl_1",
            songs = allMockSongs.subList(0, 5)
        ),
        MockShelf(
            id = "shelf_positive_note",
            title = "Feel Good Anthems",
            subtitle = "START YOUR DAY ON A POSITIVE NOTE",
            seeAllRoute = "playlist/pl_4",
            songs = allMockSongs.subList(2, 7)
        ),
        MockShelf(
            id = "shelf_keep_listening",
            title = "Keep Listening",
            subtitle = "RECENTLY PLAYED",
            seeAllRoute = "history",
            songs = allMockSongs.take(4)
        ),
        MockShelf(
            id = "shelf_new_releases",
            title = "New Releases",
            subtitle = "FRESH PICKS FOR YOU",
            seeAllRoute = "new_releases",
            songs = allMockSongs.reversed().take(5)
        )
    )
}
