package dev.oneshot.camera.gl

import java.nio.ByteBuffer

/**
 * Snapshot of luminance and color distribution for the active viewfinder.
 * 64 bins represent 0..255 range (4 luminance steps per bin).
 */
data class HistogramData(
    val lumaBins: FloatArray, // 64 values, normalized 0f..1f
    val rBins: FloatArray,    // 64 values, normalized 0f..1f
    val gBins: FloatArray,    // 64 values, normalized 0f..1f
    val bBins: FloatArray,    // 64 values, normalized 0f..1f
    val isShadowClipped: Boolean,
    val isHighlightClipped: Boolean,
    val meanLuma: Float,      // 0f..255f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HistogramData) return false
        return lumaBins.contentEquals(other.lumaBins) &&
                rBins.contentEquals(other.rBins) &&
                gBins.contentEquals(other.gBins) &&
                bBins.contentEquals(other.bBins) &&
                isShadowClipped == other.isShadowClipped &&
                isHighlightClipped == other.isHighlightClipped &&
                meanLuma == other.meanLuma
    }

    override fun hashCode(): Int {
        var result = lumaBins.contentHashCode()
        result = 31 * result + rBins.contentHashCode()
        result = 31 * result + gBins.contentHashCode()
        result = 31 * result + bBins.contentHashCode()
        result = 31 * result + isShadowClipped.hashCode()
        result = 31 * result + isHighlightClipped.hashCode()
        result = 31 * result + meanLuma.hashCode()
        return result
    }

    companion object {
        const val BIN_COUNT = 64

        /**
         * Fast extraction of 64-bin histogram metrics from downsampled RGBA byte buffer.
         * Buffer must contain at least width * height * 4 bytes.
         */
        fun computeFromRgba(width: Int, height: Int, buffer: ByteBuffer): HistogramData {
            val totalPixels = width * height
            if (totalPixels <= 0) {
                return empty()
            }

            val lumaCounts = IntArray(BIN_COUNT)
            val rCounts = IntArray(BIN_COUNT)
            val gCounts = IntArray(BIN_COUNT)
            val bCounts = IntArray(BIN_COUNT)

            var shadowClippedPixels = 0
            var highlightClippedPixels = 0
            var totalLumaSum = 0L

            for (i in 0 until totalPixels) {
                val offset = i * 4
                val r = buffer.get(offset).toInt() and 0xFF
                val g = buffer.get(offset + 1).toInt() and 0xFF
                val b = buffer.get(offset + 2).toInt() and 0xFF

                // Rec.709 Luminance weights (0.2126 R + 0.7152 G + 0.0722 B)
                val luma = ((54 * r + 183 * g + 19 * b) shr 8).coerceIn(0, 255)
                totalLumaSum += luma

                val rBin = r shr 2
                val gBin = g shr 2
                val bBin = b shr 2
                val lumaBin = luma shr 2

                rCounts[rBin]++
                gCounts[gBin]++
                bCounts[bBin]++
                lumaCounts[lumaBin]++

                if (luma < 5) shadowClippedPixels++
                if (luma > 250) highlightClippedPixels++
            }

            // Normalization: find peak bin to scale into 0f..1f for UI drawing
            var maxLuma = 1
            var maxR = 1
            var maxG = 1
            var maxB = 1

            for (i in 0 until BIN_COUNT) {
                if (lumaCounts[i] > maxLuma) maxLuma = lumaCounts[i]
                if (rCounts[i] > maxR) maxR = rCounts[i]
                if (gCounts[i] > maxG) maxG = gCounts[i]
                if (bCounts[i] > maxB) maxB = bCounts[i]
            }

            val lumaBins = FloatArray(BIN_COUNT) { (lumaCounts[it].toFloat() / maxLuma).coerceIn(0f, 1f) }
            val rBins = FloatArray(BIN_COUNT) { (rCounts[it].toFloat() / maxR).coerceIn(0f, 1f) }
            val gBins = FloatArray(BIN_COUNT) { (gCounts[it].toFloat() / maxG).coerceIn(0f, 1f) }
            val bBins = FloatArray(BIN_COUNT) { (bCounts[it].toFloat() / maxB).coerceIn(0f, 1f) }

            val clippingThreshold = (totalPixels * 0.04f).toInt().coerceAtLeast(4)

            return HistogramData(
                lumaBins = lumaBins,
                rBins = rBins,
                gBins = gBins,
                bBins = bBins,
                isShadowClipped = shadowClippedPixels > clippingThreshold,
                isHighlightClipped = highlightClippedPixels > clippingThreshold,
                meanLuma = totalLumaSum.toFloat() / totalPixels
            )
        }

        fun empty(): HistogramData = HistogramData(
            lumaBins = FloatArray(BIN_COUNT),
            rBins = FloatArray(BIN_COUNT),
            gBins = FloatArray(BIN_COUNT),
            bBins = FloatArray(BIN_COUNT),
            isShadowClipped = false,
            isHighlightClipped = false,
            meanLuma = 0f
        )
    }
}
