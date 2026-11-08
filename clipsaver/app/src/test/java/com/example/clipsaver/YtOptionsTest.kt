package com.example.clipsaver

import com.example.clipsaver.download.AudioFormat
import com.example.clipsaver.download.DownloadRequest
import com.example.clipsaver.download.Kind
import com.example.clipsaver.download.VideoContainer
import com.example.clipsaver.download.YtOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YtOptionsTest {

    private fun video(maxHeight: Int = 0, c: VideoContainer = VideoContainer.MP4) =
        DownloadRequest("https://x.test/v", "t", null, Kind.VIDEO, maxHeight, c)

    private fun audio(a: AudioFormat) =
        DownloadRequest("https://x.test/v", "t", null, Kind.AUDIO, audio = a)

    private fun List<Pair<String, String?>>.value(key: String) = first { it.first == key }.second
    private fun List<Pair<String, String?>>.has(key: String) = any { it.first == key }

    @Test fun bestMp4PrefersH264() {
        val o = YtOptions.build(video(), "out")
        assertEquals("res,vcodec:h264,acodec:aac", o.value("-S"))
        assertEquals("mp4", o.value("--merge-output-format"))
        assertEquals("bv*+ba/b", o.value("-f"))
    }

    @Test fun qualityCapIsApplied() {
        assertEquals("res:1080,vcodec:h264,acodec:aac", YtOptions.build(video(1080), "out").value("-S"))
    }

    @Test fun webmPrefersVp9() {
        assertEquals("res,vcodec:vp9,acodec:opus",
            YtOptions.build(video(c = VideoContainer.WEBM), "out").value("-S"))
    }

    @Test fun mkvHasNoCodecPreference() {
        assertEquals("res", YtOptions.build(video(c = VideoContainer.MKV), "out").value("-S"))
    }

    @Test fun mp3ExtractsAudio() {
        val o = YtOptions.build(audio(AudioFormat.MP3), "out")
        assertTrue(o.has("-x"))
        assertEquals("mp3", o.value("--audio-format"))
        assertEquals("0", o.value("--audio-quality"))
        assertTrue(o.has("--embed-metadata"))
    }

    @Test fun wavSkipsMetadataEmbedding() {
        assertTrue(!YtOptions.build(audio(AudioFormat.WAV), "out").has("--embed-metadata"))
    }

    @Test fun cookiesOnlyWhenProvided() {
        assertNull(YtOptions.build(video(), "out").firstOrNull { it.first == "--cookies" })
        assertEquals("/c.txt", YtOptions.build(video(), "out", "/c.txt").value("--cookies"))
    }

    @Test fun neverDownloadsPlaylists() {
        assertTrue(YtOptions.build(video(), "out").has("--no-playlist"))
    }
}
