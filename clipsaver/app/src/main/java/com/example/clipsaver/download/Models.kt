package com.example.clipsaver.download

enum class Kind { VIDEO, AUDIO }

enum class VideoContainer(val ext: String) { MP4("mp4"), MKV("mkv"), WEBM("webm") }

enum class AudioFormat(val ext: String) { MP3("mp3"), M4A("m4a"), OPUS("opus"), FLAC("flac"), WAV("wav") }

enum class TaskState { QUEUED, RUNNING, DONE, FAILED, CANCELED }

/** What the link contains, shown before the user chooses a format. */
data class MediaInfo(
    val url: String,
    val title: String,
    val uploader: String?,
    val durationSec: Long,
    val thumbnail: String?,
    /** Available video qualities (short side in pixels, e.g. 1080), highest first. */
    val heights: List<Int>,
)

/** What the user asked for. maxHeight 0 means "best available". */
data class DownloadRequest(
    val url: String,
    val title: String,
    val thumbnail: String?,
    val kind: Kind,
    val maxHeight: Int = 0,
    val container: VideoContainer = VideoContainer.MP4,
    val audio: AudioFormat = AudioFormat.MP3,
) {
    val label: String
        get() = when (kind) {
            Kind.VIDEO -> container.ext.uppercase() + " · " +
                if (maxHeight <= 0) "Best quality" else "up to ${maxHeight}p"
            Kind.AUDIO -> audio.ext.uppercase() + " · Best quality"
        }
}

data class DownloadTask(
    val id: String,
    val request: DownloadRequest,
    val state: TaskState = TaskState.QUEUED,
    /** 0..1, or -1 when unknown. */
    val progress: Float = -1f,
    val etaSec: Long = -1,
    val message: String = "",
    val resultUri: String? = null,
    val sizeBytes: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

const val COOKIES_FILE = "cookies.txt"
