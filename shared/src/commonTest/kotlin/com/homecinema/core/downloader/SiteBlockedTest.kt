package com.homecinema.core.downloader

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SiteBlockedTest {
    private val noImpersonation =
        "WARNING: [PornHub] The extractor is attempting impersonation, but no impersonate target is available. " +
            "If you encounter errors, then see  https://github.com/yt-dlp/yt-dlp#impersonation  for information"

    @Test
    fun `403 without an impersonate target is a block`() =
        assertTrue(
            isSiteBlocked(
                "$noImpersonation\nERROR: [PornHub] x: Unable to download webpage: HTTP Error 403: Forbidden " +
                    "(caused by <HTTPError 403: Forbidden>)",
            ),
        )

    @Test
    fun `403 alone is not a block`() =
        assertFalse(isSiteBlocked("ERROR: [Generic] x: Unable to download webpage: HTTP Error 403: Forbidden"))

    @Test
    fun `missing impersonation with another error is not a block`() =
        assertFalse(isSiteBlocked("$noImpersonation\nERROR: [PornHub] x: HTTP Error 404: Not Found"))
}
