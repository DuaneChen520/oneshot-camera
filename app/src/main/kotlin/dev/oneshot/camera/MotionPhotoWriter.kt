package dev.oneshot.camera

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

object MotionPhotoWriter {

    private const val XMP_HEADER = "http://ns.adobe.com/xap/1.0/\u0000"

    /**
     * Creates a Google Photos / Xiaomi Gallery compliant Motion Photo JPEG file.
     * Merges [jpegFile] and [mp4File], injects dual XMP container metadata,
     * and publishes directly to MediaStore under DCIM/OneShot.
     *
     * @return MediaStore Uri of the newly created Motion Photo.
     */
    fun createMotionPhoto(
        context: Context,
        jpegFile: File,
        mp4File: File,
        displayName: String,
    ): Uri? {
        val videoLength = mp4File.length()
        if (!jpegFile.exists() || !mp4File.exists() || videoLength <= 0) {
            return null
        }

        val xmpString = buildXmpString(videoLength)
        val xmpBytes = xmpString.toByteArray(StandardCharsets.UTF_8)
        val xmpHeaderBytes = XMP_HEADER.toByteArray(StandardCharsets.UTF_8)

        // APP1 payload = xmpHeaderBytes + xmpBytes
        // Marker length = 2 (for length field itself) + payload length
        val payloadLength = xmpHeaderBytes.size + xmpBytes.size
        val markerLength = payloadLength + 2
        if (markerLength > 65535) {
            // XMP packet exceeds maximum JPEG segment size
            return null
        }

        // Setup MediaStore entry
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/OneShot")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return null

        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                FileInputStream(jpegFile).use { jpegIn ->
                    val header = ByteArray(2)
                    val read = jpegIn.read(header)
                    if (read != 2 || header[0] != 0xFF.toByte() || header[1] != 0xD8.toByte()) {
                        throw IllegalStateException("Invalid JPEG: missing SOI marker")
                    }

                    // 1. Write JPEG SOI (0xFF, 0xD8)
                    output.write(header)

                    // 2. Write injected XMP APP1 Marker (0xFF, 0xE1, length MSB, length LSB)
                    output.write(0xFF)
                    output.write(0xE1)
                    output.write((markerLength shr 8) and 0xFF)
                    output.write(markerLength and 0xFF)
                    output.write(xmpHeaderBytes)
                    output.write(xmpBytes)

                    // 3. Write remaining JPEG bytes
                    jpegIn.copyTo(output)
                }

                // 4. Append entire MP4 video stream to the tail of the JPEG
                FileInputStream(mp4File).use { mp4In ->
                    mp4In.copyTo(output)
                }
                output.flush()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val completeValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, completeValues, null, null)
            }

            return uri
        } catch (e: Exception) {
            e.printStackTrace()
            runCatching { context.contentResolver.delete(uri, null, null) }
            return null
        }
    }

    internal fun buildXmpString(videoLength: Long): String {
        return """
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.0-jc003">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""
                  xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
                  xmlns:Container="http://schemas.google.com/photos/1.0/container/"
                  xmlns:Item="http://schemas.google.com/photos/1.0/container/item/"
                  GCamera:MotionPhoto="1"
                  GCamera:MotionPhotoVersion="1"
                  GCamera:MicroVideo="1"
                  GCamera:MicroVideoVersion="1"
                  GCamera:MicroVideoOffset="$videoLength"
                  GCamera:MicroVideoPresentationTimestampUs="0">
                  <Container:Directory>
                    <rdf:Seq>
                      <rdf:li rdf:parseType="Resource">
                        <Container:Item Item:Mime="image/jpeg" Item:Semantic="Primary" Item:Length="0" Item:Padding="0"/>
                      </rdf:li>
                      <rdf:li rdf:parseType="Resource">
                        <Container:Item Item:Mime="video/mp4" Item:Semantic="MotionPhoto" Item:Length="$videoLength" Item:Padding="0"/>
                      </rdf:li>
                    </rdf:Seq>
                  </Container:Directory>
                </rdf:Description>
              </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()
    }
}
