package dev.oneshot.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Mp4RecoveryInspectorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testInspectNonExistentFile() {
        val fakeFile = File(tempFolder.root, "not_exist.mp4")
        val result = Mp4RecoveryInspector.inspect(fakeFile)
        assertFalse(result.isHealthy)
        assertEquals(-1L, result.mdatOffset)
    }

    @Test
    fun testInspectTruncatedMp4WithoutMoov() {
        // Create an MP4 that only has ftyp and mdat (e.g. crashed mid-recording before moov was written)
        val file = tempFolder.newFile("crashed_recording.mp4")
        FileOutputStream(file).use { fos ->
            // 1. Write ftyp atom (16 bytes)
            val ftypBuf = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)
            ftypBuf.putInt(16) // size
            ftypBuf.put("ftyp".toByteArray(Charsets.US_ASCII))
            ftypBuf.put("isom".toByteArray(Charsets.US_ASCII))
            ftypBuf.putInt(512)
            fos.write(ftypBuf.array())

            // 2. Write mdat atom with 100 bytes of dummy payload
            val mdatBuf = ByteBuffer.allocate(108).order(ByteOrder.BIG_ENDIAN)
            mdatBuf.putInt(108) // size
            mdatBuf.put("mdat".toByteArray(Charsets.US_ASCII))
            mdatBuf.put(ByteArray(100) { 0x42 })
            fos.write(mdatBuf.array())
        }

        val result = Mp4RecoveryInspector.inspect(file)
        assertTrue("Should detect ftyp", result.hasFtyp)
        assertTrue("Should detect mdat", result.hasMdat)
        assertFalse("Should detect missing moov", result.hasMoov)
        assertFalse("Should be marked unhealthy", result.isHealthy)
        assertEquals(16L, result.mdatOffset)
        assertEquals(108L, result.mdatSize)
        assertTrue(result.diagnosticMessage.contains("缺少 moov 索引"))
    }

    @Test
    fun testInspectHealthyMp4WithMoov() {
        val file = tempFolder.newFile("healthy_recording.mp4")
        FileOutputStream(file).use { fos ->
            // 1. ftyp
            val ftypBuf = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)
            ftypBuf.putInt(16)
            ftypBuf.put("ftyp".toByteArray(Charsets.US_ASCII))
            ftypBuf.put("isom".toByteArray(Charsets.US_ASCII))
            ftypBuf.putInt(512)
            fos.write(ftypBuf.array())

            // 2. mdat
            val mdatBuf = ByteBuffer.allocate(50).order(ByteOrder.BIG_ENDIAN)
            mdatBuf.putInt(50)
            mdatBuf.put("mdat".toByteArray(Charsets.US_ASCII))
            mdatBuf.put(ByteArray(42))
            fos.write(mdatBuf.array())

            // 3. moov
            val moovBuf = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN)
            moovBuf.putInt(32)
            moovBuf.put("moov".toByteArray(Charsets.US_ASCII))
            moovBuf.put(ByteArray(24))
            fos.write(moovBuf.array())
        }

        val result = Mp4RecoveryInspector.inspect(file)
        assertTrue(result.hasFtyp)
        assertTrue(result.hasMdat)
        assertTrue(result.hasMoov)
        assertTrue(result.isHealthy)
        assertEquals("视频结构完整，moov 索引正常", result.diagnosticMessage)
    }
}
