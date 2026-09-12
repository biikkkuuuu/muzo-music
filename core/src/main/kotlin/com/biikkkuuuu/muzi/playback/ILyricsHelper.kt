package com.biikkkuuuu.muzi.playback

import com.biikkkuuuu.muzi.models.MediaMetadata

data class LyricsWithProvider(
    val lyrics: String?,
    val providerName: String
)

interface ILyricsHelper {
    suspend fun getLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider
}
