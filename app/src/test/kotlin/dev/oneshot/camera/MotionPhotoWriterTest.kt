package dev.oneshot.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MotionPhotoWriterTest {

    @Test
    fun testBuildXmpStringContainsRequiredGoogleAndXiaomiTags() {
        val testVideoLength = 1_048_576L // 1 MB
        val timestampUs = 1_500_000L // 1.5s
        val xmp = MotionPhotoWriter.buildXmpString(testVideoLength, timestampUs)

        assertTrue("XMP should declare GCamera namespace", xmp.contains("xmlns:GCamera=\"http://ns.google.com/photos/1.0/camera/\""))
        assertTrue("XMP should declare Container namespace", xmp.contains("xmlns:Container=\"http://schemas.google.com/photos/1.0/container/\""))
        assertTrue("XMP should declare MotionPhoto=1", xmp.contains("GCamera:MotionPhoto=\"1\""))
        assertTrue("XMP should declare MicroVideo=1", xmp.contains("GCamera:MicroVideo=\"1\""))
        assertTrue("XMP should specify offset matching video length", xmp.contains("GCamera:MicroVideoOffset=\"1048576\""))
        assertTrue("XMP should declare presentation timestamp", xmp.contains("GCamera:MicroVideoPresentationTimestampUs=\"1500000\""))
        assertTrue("XMP should declare Primary item", xmp.contains("Item:Semantic=\"Primary\""))
        assertTrue("XMP should declare MotionPhoto item with offset", xmp.contains("Item:Semantic=\"MotionPhoto\" Item:Length=\"1048576\""))
    }

    @Test
    fun testWriteMotionPhotoPreservesMultiAppSegmentsAndAppendsVideo() {
        // Construct a synthetic JPEG with: SOI + APP1 (Exif) + APP1 (Ultra HDR Gainmap XMP) + APP2 (MPF) + Body + EOI
        val jpegOut = ByteArrayOutputStream()
        jpegOut.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte())) // SOI

        // APP1 Exif (len = 2 + 4 = 6)
        jpegOut.write(byteArrayOf(0xFF.toByte(), 0xE1.toByte(), 0x00, 0x06, 'E'.code.toByte(), 'x'.code.toByte(), 'i'.code.toByte(), 'f'.code.toByte()))

        // APP1 Ultra HDR Gainmap XMP (len = 2 + 7 = 9)
        jpegOut.write(byteArrayOf(0xFF.toByte(), 0xE1.toByte(), 0x00, 0x09, 'G'.code.toByte(), 'a'.code.toByte(), 'i'.code.toByte(), 'n'.code.toByte(), 'M'.code.toByte(), 'a'.code.toByte(), 'p'.code.toByte()))

        // APP2 MPF (len = 2 + 3 = 5)
        jpegOut.write(byteArrayOf(0xFF.toByte(), 0xE2.toByte(), 0x00, 0x05, 'M'.code.toByte(), 'P'.code.toByte(), 'F'.code.toByte()))

        // Image body bytes + EOI
        val bodyBytes = byteArrayOf(0x01, 0x02, 0x03, 0xFF.toByte(), 0xD9.toByte())
        jpegOut.write(bodyBytes)

        val fakeJpegBytes = jpegOut.toByteArray()
        val fakeMp4Bytes = "FAKE_MP4_PAYLOAD_TEST_12345".toByteArray()

        val output = ByteArrayOutputStream()
        val success = MotionPhotoWriter.writeMotionPhotoStream(
            jpegIn = ByteArrayInputStream(fakeJpegBytes),
            mp4In = ByteArrayInputStream(fakeMp4Bytes),
            mp4Length = fakeMp4Bytes.size.toLong(),
            output = output,
            presentationTimestampUs = 800_000L
        )

        assertTrue("writeMotionPhotoStream should return true", success)
        val result = output.toByteArray()

        // 1. Should start with SOI
        assertEquals(0xFF.toByte(), result[0])
        assertEquals(0xD8.toByte(), result[1])

        // 2. Should contain all original segments: Exif, GainMap, MPF
        val resultStr = String(result, Charsets.ISO_8859_1)
        assertTrue("Exif must be preserved", resultStr.contains("Exif"))
        assertTrue("Ultra HDR GainMap marker must be preserved", resultStr.contains("GainMap"))
        assertTrue("MPF marker must be preserved", resultStr.contains("MPF"))

        // 3. Should contain the injected Motion Photo XMP with timestamp 800000
        assertTrue("Injected XMP must contain presentation timestamp 800000", resultStr.contains("MicroVideoPresentationTimestampUs=\"800000\""))

        // 4. Must end strictly with fakeMp4Bytes
        val endBytes = result.takeLast(fakeMp4Bytes.size).toByteArray()
        assertEquals(String(fakeMp4Bytes), String(endBytes))
    }
}
