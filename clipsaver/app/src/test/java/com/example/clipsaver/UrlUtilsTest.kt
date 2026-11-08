package com.example.clipsaver

import com.example.clipsaver.download.UrlUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlUtilsTest {
    @Test fun extractsLinkFromShareText() {
        assertEquals(
            "https://www.instagram.com/reel/abc/",
            UrlUtils.extractUrl("Watch this https://www.instagram.com/reel/abc/ now"),
        )
    }

    @Test fun stripsTrailingPunctuation() {
        assertEquals("https://youtu.be/xyz", UrlUtils.extractUrl("see https://youtu.be/xyz."))
    }

    @Test fun keepsQueryString() {
        assertEquals(
            "https://www.youtube.com/watch?v=9TTODPnm_YQ",
            UrlUtils.extractUrl("https://www.youtube.com/watch?v=9TTODPnm_YQ"),
        )
    }

    @Test fun returnsNullWithoutLink() {
        assertNull(UrlUtils.extractUrl("hello world"))
        assertNull(UrlUtils.extractUrl(null))
        assertNull(UrlUtils.extractUrl("   "))
    }
}
