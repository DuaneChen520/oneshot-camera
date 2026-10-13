package dev.oneshot.camera.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class HistogramDataTest {

    @Test
    fun testEmptyHistogram() {
        val empty = HistogramData.empty()
        assertEquals(64, empty.lumaBins.size)
        assertEquals(64, empty.rBins.size)
        assertEquals(0f, empty.meanLuma, 0.001f)
        assertFalse(empty.isShadowClipped)
        assertFalse(empty.isHighlightClipped)
    }

    @Test
    fun testAllBlackPixels() {
        val width = 8
        val height = 8
        val buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
        // All pixels (0, 0, 0, 255)
        for (i in 0 until width * height) {
            buffer.put(0.toByte())
            buffer.put(0.toByte())
            buffer.put(0.toByte())
            buffer.put(255.toByte())
        }
        buffer.rewind()

        val data = HistogramData.computeFromRgba(width, height, buffer)
        assertEquals(0f, data.meanLuma, 0.001f)
        // Bin 0 should be max (1.0f)
        assertEquals(1.0f, data.lumaBins[0], 0.001f)
        assertEquals(0.0f, data.lumaBins[63], 0.001f)
        assertTrue(data.isShadowClipped)
        assertFalse(data.isHighlightClipped)
    }

    @Test
    fun testAllWhitePixels() {
        val width = 8
        val height = 8
        val buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
        // All pixels (255, 255, 255, 255)
        for (i in 0 until width * height) {
            buffer.put(255.toByte())
            buffer.put(255.toByte())
            buffer.put(255.toByte())
            buffer.put(255.toByte())
        }
        buffer.rewind()

        val data = HistogramData.computeFromRgba(width, height, buffer)
        assertEquals(255f, data.meanLuma, 0.001f)
        assertEquals(0.0f, data.lumaBins[0], 0.001f)
        assertEquals(1.0f, data.lumaBins[63], 0.001f)
        assertFalse(data.isShadowClipped)
        assertTrue(data.isHighlightClipped)
    }

    @Test
    fun testPureRedChannel() {
        val width = 4
        val height = 4
        val buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
        // All pixels (255, 0, 0, 255)
        for (i in 0 until width * height) {
            buffer.put(255.toByte())
            buffer.put(0.toByte())
            buffer.put(0.toByte())
            buffer.put(255.toByte())
        }
        buffer.rewind()

        val data = HistogramData.computeFromRgba(width, height, buffer)
        // Red bin 63 should be 1.0f
        assertEquals(1.0f, data.rBins[63], 0.001f)
        // Green and Blue bin 0 should be 1.0f
        assertEquals(1.0f, data.gBins[0], 0.001f)
        assertEquals(1.0f, data.bBins[0], 0.001f)
    }
}
