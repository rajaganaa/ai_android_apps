package com.example.clipsaver.download

import kotlin.coroutines.cancellation.CancellationException

/** Like runCatching, but never swallows coroutine cancellation. */
inline fun <T> safeCall(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

/** Turns a long yt-dlp error into one readable line plus a hint when we know the cause. */
fun Throwable.friendlyMessage(): String {
    val raw = message.orEmpty()
    val lower = raw.lowercase()
    val last = raw.lines().lastOrNull { it.isNotBlank() }?.trim() ?: javaClass.simpleName
    val hint = when {
        "login" in lower || "sign in" in lower || "cookies" in lower || "private" in lower ->
            "  Tip: this needs a login. Import cookies.txt in Settings."
        "unsupported url" in lower -> "  Tip: this link is not supported."
        "unable to resolve host" in lower || "network is unreachable" in lower ->
            "  Tip: check your internet connection."
        else -> ""
    }
    return last.take(300) + hint
}
