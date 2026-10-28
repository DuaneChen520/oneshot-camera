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
        presentationTimestampUs: Long = 0L,
    ): Uri? {
        val videoLength = mp4File.length()
        if (!jpegFile.exists() || !mp4File.exists() || videoLength <= 0) {
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
            val success = context.contentResolver.openOutputStream(uri)?.use { output ->
                FileInputStream(jpegFile).use { jpegIn ->
                    FileInputStream(mp4File).use { mp4In ->
                        writeMotionPhotoStream(
                            jpegIn = jpegIn,
                            mp4In = mp4In,
                            mp4Length = videoLength,
                            output = output,
                            presentationTimestampUs = presentationTimestampUs
                        )
                    }
                }
            } ?: false

            if (!success) {
                runCatching { context.contentResolver.delete(uri, null, null) }
                return null
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

    /**
     * Pure stream processor: Merges a JPEG input stream and an MP4 input stream,
     * preserving all existing APP markers (including Exif and Ultra HDR gainmap metadata),
     * injecting Google/Xiaomi compliant Motion Photo XMP APP1, and appending the video payload.
     *
     * @return true if successfully written, false otherwise.
     */
    fun writeMotionPhotoStream(
        jpegIn: java.io.InputStream,
        mp4In: java.io.InputStream,
        mp4Length: Long,
        output: OutputStream,
        presentationTimestampUs: Long = 0L,
    ): Boolean {
        if (mp4Length <= 0) return false

        val xmpString = buildXmpString(mp4Length, presentationTimestampUs)
        val xmpBytes = xmpString.toByteArray(StandardCharsets.UTF_8)
        val xmpHeaderBytes = XMP_HEADER.toByteArray(StandardCharsets.UTF_8)

        val payloadLength = xmpHeaderBytes.size + xmpBytes.size
        val markerLength = payloadLength + 2
        if (markerLength > 65535) {
            return false
        }

        val pushbackIn = java.io.PushbackInputStream(jpegIn, 4)
        val header = ByteArray(2)
        val readSoi = pushbackIn.read(header)
        if (readSoi != 2 || (header[0].toInt() and 0xFF) != 0xFF || (header[1].toInt() and 0xFF) != 0xD8) {
            return false
        }

        // 1. Write JPEG SOI (0xFF, 0xD8)
        output.write(header)

        // 2. Scan and stream all existing APP segments (APP0 to APP15: 0xE0..0xEF, plus COM: 0xFE)
        // so that Exif remains at offset 2 and Ultra HDR Gainmap metadata is safely preserved.
        while (true) {
            val markerLead = pushbackIn.read()
            if (markerLead == -1) break
            if (markerLead != 0xFF) {
                pushbackIn.unread(markerLead)
                break
            }

            val markerType = pushbackIn.read()
            if (markerType == -1) {
                output.write(markerLead)
                break
            }

            val marker = markerType and 0xFF
            // Check if marker is an APP segment (0xE0..0xEF) or COM (0xFE)
            if ((marker in 0xE0..0xEF) || marker == 0xFE) {
                val lengthByteHigh = pushbackIn.read()
                val lengthByteLow = pushbackIn.read()
                if (lengthByteHigh == -1 || lengthByteLow == -1) {
                    output.write(markerLead)
                    output.write(markerType)
                    if (lengthByteHigh != -1) output.write(lengthByteHigh)
                    break
                }
                val segLen = ((lengthByteHigh and 0xFF) shl 8) or (lengthByteLow and 0xFF)
                val segPayload = ByteArray(maxOf(0, segLen - 2))
                var readTotal = 0
                while (readTotal < segPayload.size) {
                    val r = pushbackIn.read(segPayload, readTotal, segPayload.size - readTotal)
                    if (r == -1) break
                    readTotal += r
                }
                output.write(0xFF)
                output.write(markerType)
                output.write(lengthByteHigh)
                output.write(lengthByteLow)
                output.write(segPayload, 0, readTotal)
            } else {
                // First non-APP marker encountered (e.g. DQT, SOF, SOS); push both back
                pushbackIn.unread(markerType)
                pushbackIn.unread(markerLead)
                break
            }
        }

        // 3. Write injected Motion Photo XMP APP1 Marker
        output.write(0xFF)
        output.write(0xE1)
        output.write((markerLength shr 8) and 0xFF)
        output.write(markerLength and 0xFF)
        output.write(xmpHeaderBytes)
        output.write(xmpBytes)

        // 4. Copy remaining JPEG image stream (DQT, DHT, SOF, SOS, image data, EOI)
        pushbackIn.copyTo(output)

        // 5. Append entire MP4 video stream to the tail of the JPEG
        mp4In.copyTo(output)
        output.flush()
        return true
    }

    internal fun buildXmpString(videoLength: Long, presentationTimestampUs: Long = 0L): String {
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
                  GCamera:MicroVideoPresentationTimestampUs="$presentationTimestampUs">
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
