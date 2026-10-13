package dev.oneshot.camera.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class CubeLutParserTest {

    @Test
    fun testParseCubeString() {
        val sampleCube = """
            # Sample Cube LUT
            TITLE "TestLUT"
            LUT_3D_SIZE 2
            DOMAIN_MIN 0.0 0.0 0.0
            DOMAIN_MAX 1.0 1.0 1.0
            0.0 0.0 0.0
            1.0 0.0 0.0
            0.0 1.0 0.0
            1.0 1.0 0.0
            0.0 0.0 1.0
            1.0 0.0 1.0
            0.0 1.0 1.0
            1.0 1.0 1.0
        """.trimIndent()

        val data = CubeLutParser.parse(ByteArrayInputStream(sampleCube.toByteArray()))
        assertEquals(2, data.size)
        // 2 * 2 * 2 * 3 = 24 bytes
        assertEquals(24, data.buffer.remaining())
    }

    @Test
    fun testCreateTealAndOrange() {
        val size = 17
        val lut = CubeLutParser.createTealAndOrange(size)
        assertEquals(size, lut.size)
        val expectedBytes = size * size * size * 3
        assertEquals(expectedBytes, lut.buffer.remaining())

        // Check first byte (shadows)
        val firstR = lut.buffer.get(0).toInt() and 0xFF
        assertTrue("Shadow byte should be valid", firstR in 0..255)
    }

    @Test
    fun testCreateKodakPortra() {
        val size = 17
        val lut = CubeLutParser.createKodakPortra(size)
        assertEquals(size, lut.size)
        val expectedBytes = size * size * size * 3
        assertEquals(expectedBytes, lut.buffer.remaining())
    }

    @Test
    fun testCreateFlatLog() {
        val size = 17
        val lut = CubeLutParser.createFlatLog(size)
        assertEquals(size, lut.size)
        val expectedBytes = size * size * size * 3
        assertEquals(expectedBytes, lut.buffer.remaining())

        // In Flat Log, shadow black point (0,0,0) is lifted above zero
        val shadowLiftedR = lut.buffer.get(0).toInt() and 0xFF
        assertTrue("Flat log shadow should be lifted above 0", shadowLiftedR > 0)
    }
}
