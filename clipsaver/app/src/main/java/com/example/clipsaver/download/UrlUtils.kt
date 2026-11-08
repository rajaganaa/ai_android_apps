package com.example.clipsaver.download

object UrlUtils {
    private val urlRegex = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

    /** Finds the first link in shared text such as "Watch this https://... now". */
    fun extractUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        return urlRegex.find(text)?.value?.trimEnd('.', ',', ';', ')', ']', '"', '\'')
    }
}
