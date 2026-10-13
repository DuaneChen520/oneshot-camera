package dev.oneshot.camera.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CustomLutManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var manager: CustomLutManager

    private val sampleCubeContent = """
        # Test Custom LUT
        TITLE "Fuji Eterna 250D"
        LUT_3D_SIZE 2
        0.0 0.0 0.0
        1.0 0.0 0.0
        0.0 1.0 0.0
        1.0 1.0 0.0
        0.0 0.0 1.0
        1.0 0.0 1.0
        0.0 1.0 1.0
        1.0 1.0 1.0
    """.trimIndent()

    @Before
    fun setUp() {
        val testDir = tempFolder.newFolder("luts_test")
        manager = CustomLutManager(baseDir = testDir)
    }

    @Test
    fun testImportFromBytesSuccess() {
        val result = manager.importFromBytes(sampleCubeContent.toByteArray(), suggestedName = "fallback_name")
        assertTrue("Import should succeed", result.isSuccess)

        val customLut = result.getOrThrow()
        assertEquals("Fuji Eterna 250D", customLut.name)
        assertEquals(2, customLut.size)
        assertTrue(customLut.fileName.endsWith(".cube"))

        // File should exist in lutsDir
        val fileOnDisk = File(manager.lutsDir, customLut.fileName)
        assertTrue("LUT file should exist on disk", fileOnDisk.exists())

        // Memory cache retrieval should succeed
        val lutData = manager.getLutData(customLut)
        assertNotNull("Lut3dData should be retrieved", lutData)
        assertEquals(2, lutData!!.size)
    }

    @Test
    fun testFallbackNameToSuggestedWhenNoTitle() {
        val cubeWithoutTitle = """
            LUT_3D_SIZE 2
            0.0 0.0 0.0
            1.0 0.0 0.0
            0.0 1.0 0.0
            1.0 1.0 0.0
            0.0 0.0 1.0
            1.0 0.0 1.0
            0.0 1.0 1.0
            1.0 1.0 1.0
        """.trimIndent()

        val result = manager.importFromBytes(cubeWithoutTitle.toByteArray(), suggestedName = "My_Custom_Profile")
        assertTrue(result.isSuccess)
        assertEquals("My_Custom_Profile", result.getOrThrow().name)
    }

    @Test
    fun testDeleteLutRemovesFileAndCache() {
        val customLut = manager.importFromBytes(sampleCubeContent.toByteArray()).getOrThrow()
        val file = File(manager.lutsDir, customLut.fileName)
        assertTrue(file.exists())

        val deleted = manager.deleteLut(customLut.id, customLut.fileName)
        assertTrue("Delete should return true", deleted)
        assertTrue("File should be deleted from disk", !file.exists())

        // Retrieval should return null since file is gone and cache is evicted
        val retrieved = manager.getLutData(customLut)
        assertEquals(null, retrieved)
    }

    @Test
    fun testImportCorruptedBytesFailsGracefully() {
        val corruptedBytes = "This is not a cube file".toByteArray()
        val result = manager.importFromBytes(corruptedBytes)
        assertTrue("Corrupted file import should fail", result.isFailure)
    }
}
