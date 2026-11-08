package com.example.clipsaver.download

/** Builds yt-dlp command-line options. Pure Kotlin, so it can be unit tested. */
object YtOptions {

    /** yt-dlp "format sort": highest resolution first (optionally capped), then a codec Android likes. */
    fun sortFor(req: DownloadRequest): String {
        val res = if (req.maxHeight > 0) "res:${req.maxHeight}" else "res"
        val codecs = when (req.container) {
            VideoContainer.MP4 -> ",vcodec:h264,acodec:aac"   // plays everywhere on Android
            VideoContainer.WEBM -> ",vcodec:vp9,acodec:opus"
            VideoContainer.MKV -> ""
        }
        return res + codecs
    }

    fun build(
        req: DownloadRequest,
        outputTemplate: String,
        cookiesPath: String? = null,
    ): List<Pair<String, String?>> {
        val o = mutableListOf<Pair<String, String?>>()
        o += "-o" to outputTemplate
        o += "--no-playlist" to null
        o += "--no-mtime" to null
        o += "--retries" to "5"
        o += "--fragment-retries" to "5"
        o += "--concurrent-fragments" to "4"
        if (cookiesPath != null) o += "--cookies" to cookiesPath

        when (req.kind) {
            Kind.VIDEO -> {
                o += "-f" to "bv*+ba/b"
                o += "-S" to sortFor(req)
                o += "--merge-output-format" to req.container.ext
            }
            Kind.AUDIO -> {
                o += "-f" to "bestaudio/best"
                o += "-x" to null
                o += "--audio-format" to req.audio.ext
                o += "--audio-quality" to "0"
                if (req.audio != AudioFormat.WAV) o += "--embed-metadata" to null
            }
        }
        return o
    }
}
