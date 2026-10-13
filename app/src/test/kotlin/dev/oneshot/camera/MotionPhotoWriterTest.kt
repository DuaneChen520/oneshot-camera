package dev.oneshot.camera

import org.junit.Assert.assertTrue
import org.junit.Test

class MotionPhotoWriterTest {

    @Test
    fun testBuildXmpStringContainsRequiredGoogleAndXiaomiTags() {
        val testVideoLength = 1_048_576L // 1 MB
        val xmp = MotionPhotoWriter.buildXmpString(testVideoLength)

        assertTrue("XMP should declare GCamera namespace", xmp.contains("xmlns:GCamera=\"http://ns.google.com/photos/1.0/camera/\""))
        assertTrue("XMP should declare Container namespace", xmp.contains("xmlns:Container=\"http://schemas.google.com/photos/1.0/container/\""))
        assertTrue("XMP should declare MotionPhoto=1", xmp.contains("GCamera:MotionPhoto=\"1\""))
        assertTrue("XMP should declare MicroVideo=1", xmp.contains("GCamera:MicroVideo=\"1\""))
        assertTrue("XMP should specify offset matching video length", xmp.contains("GCamera:MicroVideoOffset=\"1048576\""))
        assertTrue("XMP should declare Primary item", xmp.contains("Item:Semantic=\"Primary\""))
        assertTrue("XMP should declare MotionPhoto item with offset", xmp.contains("Item:Semantic=\"MotionPhoto\" Item:Length=\"1048576\""))
    }
}
