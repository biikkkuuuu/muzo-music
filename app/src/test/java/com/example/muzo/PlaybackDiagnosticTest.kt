package com.example.muzo

import com.music.innertube.NewPipeExtractor
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class PlaybackDiagnosticTest {
    @Test
    fun testNewPipeUrl() = runBlocking {
        val videoId = "kJQP7kiw5Fk"
        NewPipeExtractor.init()
        val streams = NewPipeExtractor.newPipePlayer(videoId)
        println("NewPipe streams count: ${streams.size}")
        val audioStream = streams.firstOrNull { it.first in listOf(140, 251, 250, 249) } ?: streams.firstOrNull()
        if (audioStream != null) {
            println("Testing NewPipe stream itag=${audioStream.first}...")
            val conn = URL(audioStream.second).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")
            conn.setRequestProperty("Range", "bytes=0-")
            val code = conn.responseCode
            println("NewPipe open-ended bytes=0- HTTP code: $code")
        }
    }
}
