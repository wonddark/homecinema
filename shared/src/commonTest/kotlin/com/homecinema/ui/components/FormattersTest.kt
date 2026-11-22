package com.homecinema.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class FormattersTest {

    private val kb = 1024L
    private val mb = 1024L * kb
    private val gb = 1024L * mb

    @Test
    fun `sizes under one megabyte are whole kilobytes`() {
        assertEquals("0 KB", formatSize(0))
        assertEquals("0 KB", formatSize(1000))
        assertEquals("512 KB", formatSize(512 * kb))
        assertEquals("1023 KB", formatSize(mb - 1))
    }

    @Test
    fun `sizes under ten megabytes keep one decimal`() {
        assertEquals("1.0 MB", formatSize(mb))
        assertEquals("9.4 MB", formatSize((9.4 * mb).toLong()))
    }

    @Test
    fun `sizes from ten megabytes have no decimals`() {
        assertEquals("10 MB", formatSize(10 * mb))
        assertEquals("86 MB", formatSize(86 * mb))
        assertEquals("248 MB", formatSize(248 * mb))
        assertEquals("1023 MB", formatSize(1023 * mb))
    }

    @Test
    fun `sizes from one gigabyte use one decimal`() {
        assertEquals("1.0 GB", formatSize(gb))
        assertEquals("1.1 GB", formatSize((1.1 * gb).toLong()))
        assertEquals("12.5 GB", formatSize(12 * gb + gb / 2))
    }

    @Test
    fun `durations under an hour are minutes and padded seconds`() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:07", formatDuration(7))
        assertEquals("12:48", formatDuration(12 * 60 + 48))
        assertEquals("59:59", formatDuration(3599))
    }

    @Test
    fun `durations from an hour include hours and padded minutes`() {
        assertEquals("1:00:00", formatDuration(3600))
        assertEquals("1:02:03", formatDuration(3723))
        assertEquals("10:00:05", formatDuration(36005))
    }
}
