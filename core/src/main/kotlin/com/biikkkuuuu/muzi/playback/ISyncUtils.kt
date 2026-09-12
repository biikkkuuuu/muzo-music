package com.biikkkuuuu.muzi.playback

import com.biikkkuuuu.muzi.db.entities.SongEntity

interface ISyncUtils {
    fun likeSong(song: SongEntity)
}
