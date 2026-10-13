package dev.oneshot.camera.gl

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dev.oneshot.data.CustomLut
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages user-imported .cube 3D LUT presets.
 * Handles storage in internal app directory (context.filesDir/luts/),
 * memory caching of parsed Lut3dData for zero-stutter GL uploads, and file lifecycle.
 */
class CustomLutManager(
    private val context: Context? = null,
    baseDir: File = File(context?.filesDir ?: File(System.getProperty("java.io.tmpdir"), "oneshot_luts"), "luts")
) {

    val lutsDir: File = baseDir.apply {
        if (!exists()) mkdirs()
    }

    private val memoryCache = ConcurrentHashMap<String, Lut3dData>()

    /**
     * Imports a .cube file from the selected URI via ContentResolver.
     * Validates .cube format, extracts title, saves to internal storage, and caches parsed 3D texture.
     */
    fun importLut(uri: Uri, contentResolver: ContentResolver): Result<CustomLut> = runCatching {
        var displayName: String? = null
        runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        displayName = cursor.getString(idx)
                    }
                }
            }
        }

        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("无法读取选中的 LUT 文件")

        val cleanName = (displayName ?: uri.lastPathSegment ?: "Film_LUT")
            .substringAfterLast("/")
            .substringAfterLast("\\")
            .removeSuffix(".cube")
            .removeSuffix(".CUBE")

        importFromBytes(bytes, suggestedName = cleanName).getOrThrow()
    }

    /**
     * Imports a .cube directly from ByteArray (used by file operations and unit tests).
     */
    fun importFromBytes(bytes: ByteArray, suggestedName: String? = null): Result<CustomLut> = runCatching {
        val parsed = CubeLutParser.parseWithMetadata(bytes.inputStream())

        val title = parsed.title?.takeIf { it.isNotBlank() }
            ?: suggestedName?.takeIf { it.isNotBlank() }
            ?: "Custom_LUT"

        val lutId = "lut_${UUID.randomUUID().toString().take(8)}_${System.currentTimeMillis()}"
        val targetFileName = "${lutId}.cube"
        val targetFile = File(lutsDir, targetFileName)

        FileOutputStream(targetFile).use { it.write(bytes) }

        // Cache in memory
        memoryCache[lutId] = parsed.lutData

        CustomLut(
            id = lutId,
            name = title,
            fileName = targetFileName,
            size = parsed.lutData.size,
            importedAtMs = System.currentTimeMillis()
        )
    }

    /**
     * Retrieves parsed Lut3dData for a custom LUT, utilizing memory cache.
     */
    fun getLutData(customLut: CustomLut): Lut3dData? {
        val cached = memoryCache[customLut.id]
        if (cached != null) return cached

        val file = File(lutsDir, customLut.fileName)
        if (!file.exists()) return null

        return runCatching {
            val lutData = file.inputStream().use { CubeLutParser.parse(it) }
            memoryCache[customLut.id] = lutData
            lutData
        }.getOrNull()
    }

    /**
     * Deletes a custom LUT file and clears it from cache.
     */
    fun deleteLut(lutId: String, fileName: String): Boolean {
        memoryCache.remove(lutId)
        val file = File(lutsDir, fileName)
        return if (file.exists()) file.delete() else true
    }

    /**
     * Clears in-memory texture cache.
     */
    fun clearCache() {
        memoryCache.clear()
    }
}
