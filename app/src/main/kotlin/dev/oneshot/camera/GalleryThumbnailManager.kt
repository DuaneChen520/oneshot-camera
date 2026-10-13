package dev.oneshot.camera

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MediaThumbnail(
    val uri: Uri,
    val isVideo: Boolean,
    val bitmap: Bitmap?,
    val displayName: String = "",
    val dateAddedSec: Long = 0L,
    val sizeBytes: Long = 0L,
)

class GalleryThumbnailManager(
    private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _latestMedia = MutableStateFlow<MediaThumbnail?>(null)
    val latestMedia: StateFlow<MediaThumbnail?> = _latestMedia.asStateFlow()

    init {
        scope.launch {
            refreshLatestMedia()
        }
    }

    suspend fun refreshLatestMedia() = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // 1. Query newest image
        var latestImageUri: Uri? = null
        var latestImageDate = 0L
        var latestImageName = ""
        var latestImageSize = 0L

        runCatching {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE
            )
            resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                    val id = cursor.getLong(idCol)
                    latestImageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    latestImageDate = cursor.getLong(dateCol)
                    latestImageName = cursor.getString(nameCol) ?: ""
                    latestImageSize = cursor.getLong(sizeCol)
                }
            }
        }

        // 2. Query newest video
        var latestVideoUri: Uri? = null
        var latestVideoDate = 0L
        var latestVideoName = ""
        var latestVideoSize = 0L

        runCatching {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE
            )
            resolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

                    val id = cursor.getLong(idCol)
                    latestVideoUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    latestVideoDate = cursor.getLong(dateCol)
                    latestVideoName = cursor.getString(nameCol) ?: ""
                    latestVideoSize = cursor.getLong(sizeCol)
                }
            }
        }

        // 3. Determine whichever is newer
        val isVideoNewer = latestVideoDate > latestImageDate && latestVideoUri != null
        val chosenUri = if (isVideoNewer) latestVideoUri else latestImageUri
        val isVideo = isVideoNewer
        val chosenDate = if (isVideoNewer) latestVideoDate else latestImageDate
        val chosenName = if (isVideoNewer) latestVideoName else latestImageName
        val chosenSize = if (isVideoNewer) latestVideoSize else latestImageSize

        if (chosenUri != null) {
            val thumb = loadThumbnailBitmap(chosenUri, isVideo)
            _latestMedia.value = MediaThumbnail(
                uri = chosenUri,
                isVideo = isVideo,
                bitmap = thumb,
                displayName = chosenName,
                dateAddedSec = chosenDate,
                sizeBytes = chosenSize
            )
        }
    }

    fun updateWithCapturedUri(uri: Uri, isVideo: Boolean, displayName: String = "") {
        scope.launch {
            val thumb = loadThumbnailBitmap(uri, isVideo)
            _latestMedia.value = MediaThumbnail(
                uri = uri,
                isVideo = isVideo,
                bitmap = thumb,
                displayName = displayName.ifEmpty { if (isVideo) "VID_CAPTURE.mp4" else "IMG_CAPTURE.jpg" },
                dateAddedSec = System.currentTimeMillis() / 1000L,
                sizeBytes = 0L
            )
        }
    }

    private fun loadThumbnailBitmap(uri: Uri, isVideo: Boolean): Bitmap? {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                return resolver.loadThumbnail(uri, Size(160, 160), null)
            }
        }

        // Fallback for image stream decoding with downsampling
        if (!isVideo) {
            runCatching {
                resolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 8
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    return BitmapFactory.decodeStream(stream, null, options)
                }
            }
        }

        return null
    }
}
