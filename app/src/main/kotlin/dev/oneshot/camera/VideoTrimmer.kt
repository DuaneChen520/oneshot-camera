package dev.oneshot.camera

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 * Fast container-level MP4 trimmer (remuxer).
 *
 * Slices a continuous video stream without re-encoding, preserving exact original H.264/HEVC NAL units
 * and AAC audio samples. Aligns slicing to the closest previous sync frame (keyframe) at or before [startUs].
 */
object VideoTrimmer {

    /**
     * Slices [srcFile] starting around [startUs] for [durationUs] and writes to [dstFile].
     *
     * @param srcFile Source recorded MP4 file.
     * @param dstFile Destination trimmed MP4 file.
     * @param startUs Target start timestamp in microseconds in the source video.
     * @param durationUs Target duration in microseconds (or -1L for all remaining).
     * @return The presentation timestamp in microseconds in the source video that became time 0
     *         in [dstFile], or -1L if trimming failed.
     */
    fun trimMp4(srcFile: File, dstFile: File, startUs: Long, durationUs: Long = -1L): Long {
        if (!srcFile.exists() || srcFile.length() <= 0) return -1L

        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false

        try {
            val ex = MediaExtractor()
            extractor = ex
            ex.setDataSource(srcFile.absolutePath)

            var rotation = 0
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(srcFile.absolutePath)
                rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    ?.toIntOrNull() ?: 0
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
            }

            val mx = MediaMuxer(dstFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = mx
            mx.setOrientationHint(rotation)

            val trackMap = mutableMapOf<Int, Int>()
            var videoTrackIndex = -1

            for (i in 0 until ex.trackCount) {
                val format = ex.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    val muxerTrack = mx.addTrack(format)
                    trackMap[i] = muxerTrack
                    if (mime.startsWith("video/") && videoTrackIndex < 0) {
                        videoTrackIndex = i
                    }
                }
            }

            if (videoTrackIndex < 0) return -1L

            // 1. Locate sync frame (keyframe) at or immediately prior to startUs
            ex.selectTrack(videoTrackIndex)
            val seekTargetUs = maxOf(0L, startUs)
            ex.seekTo(seekTargetUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val baseUs = ex.sampleTime
            if (baseUs < 0) return -1L

            // 2. Select all tracks (audio & video) and align to baseUs
            for (track in trackMap.keys) {
                ex.selectTrack(track)
            }
            ex.seekTo(baseUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            mx.start()
            muxerStarted = true

            val buffer = ByteBuffer.allocate(2 * 1024 * 1024) // 2MB frame buffer
            val bufferInfo = MediaCodec.BufferInfo()
            val endUs = if (durationUs > 0) baseUs + durationUs else Long.MAX_VALUE

            while (true) {
                buffer.clear()
                val sampleSize = ex.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTime = ex.sampleTime
                if (sampleTime > endUs) {
                    if (ex.sampleTrackIndex == videoTrackIndex) break
                }

                val pts = sampleTime - baseUs
                val muxerTrack = trackMap[ex.sampleTrackIndex]
                if (pts >= 0 && muxerTrack != null) {
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = pts
                    bufferInfo.flags = if ((ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                        MediaCodec.BUFFER_FLAG_KEY_FRAME
                    } else {
                        0
                    }
                    mx.writeSampleData(muxerTrack, buffer, bufferInfo)
                }
                ex.advance()
            }

            return baseUs
        } catch (e: Exception) {
            e.printStackTrace()
            return -1L
        } finally {
            if (muxerStarted) {
                runCatching { muxer?.stop() }
            }
            runCatching { muxer?.release() }
            runCatching { extractor?.release() }
        }
    }
}
