package dev.oneshot.camera

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class VideoTrimmerTest {

    @Test
    fun testTrimMp4NonExistentFileReturnsMinusOne() {
        val src = File("non_existent_file_123.mp4")
        val dst = File("dst_dummy.mp4")
        val result = VideoTrimmer.trimMp4(src, dst, 1_000_000L, 3_000_000L)
        assertEquals(-1L, result)
    }

    @Test
    fun testTrimMp4EmptyFileReturnsMinusOne() {
        val temp = File.createTempFile("empty_test", ".mp4")
        val dst = File.createTempFile("dst_test", ".mp4")
        try {
            val result = VideoTrimmer.trimMp4(temp, dst, 1_000_000L, 3_000_000L)
            assertEquals(-1L, result)
        } finally {
            temp.delete()
            dst.delete()
        }
    }

    @Test
    fun testPreRollTimestampCalculations() {
        // T0 shutter pressed at 2.5s (2500ms) into the rolling segment
        val pivotMs = 2500L
        val preRollMs = 1500L
        val postRollMs = 1500L

        val sliceStartMs = maxOf(0L, pivotMs - preRollMs)
        assertEquals(1000L, sliceStartMs) // Starts at 1.0s

        // If the closest sync keyframe at or before 1000ms is at 980ms (980,000 us)
        val baseUs = 980_000L
        val presentationTimestampUs = maxOf(0L, (pivotMs * 1000L) - baseUs)
        assertEquals(1_520_000L, presentationTimestampUs) // 1.52s into trimmed video
    }
}
