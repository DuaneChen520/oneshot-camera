package dev.oneshot.camera.gl

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ln
import kotlin.math.pow

data class Lut3dData(
    val size: Int,
    val buffer: ByteBuffer,
)

object CubeLutParser {

    /**
     * Parses standard .cube file into 3D RGB ByteBuffer.
     */
    fun parse(inputStream: InputStream): Lut3dData {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var lutSize = 0
        val entries = mutableListOf<FloatArray>()

        reader.useLines { lines ->
            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) continue

                if (line.startsWith("LUT_3D_SIZE")) {
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        lutSize = parts[1].toInt()
                    }
                    continue
                }

                if (line.startsWith("DOMAIN_MIN") || line.startsWith("DOMAIN_MAX") || line.startsWith("TITLE")) {
                    continue
                }

                val tokens = line.split("\\s+".toRegex())
                if (tokens.size >= 3) {
                    val r = tokens[0].toFloatOrNull() ?: 0f
                    val g = tokens[1].toFloatOrNull() ?: 0f
                    val b = tokens[2].toFloatOrNull() ?: 0f
                    entries.add(floatArrayOf(r, g, b))
                }
            }
        }

        if (lutSize == 0) {
            lutSize = Math.round(Math.cbrt(entries.size.toDouble())).toInt()
        }

        val byteBuffer = ByteBuffer.allocateDirect(lutSize * lutSize * lutSize * 3)
            .order(ByteOrder.nativeOrder())

        for (rgb in entries) {
            val rByte = (rgb[0].coerceIn(0f, 1f) * 255f).toInt().toByte()
            val gByte = (rgb[1].coerceIn(0f, 1f) * 255f).toInt().toByte()
            val bByte = (rgb[2].coerceIn(0f, 1f) * 255f).toInt().toByte()
            byteBuffer.put(rByte)
            byteBuffer.put(gByte)
            byteBuffer.put(bByte)
        }
        byteBuffer.rewind()
        return Lut3dData(size = lutSize, buffer = byteBuffer)
    }

    /**
     * Generates a built-in Hollywood Teal & Orange 3D LUT (size 17x17x17)
     */
    fun createTealAndOrange(size: Int = 17): Lut3dData {
        val byteBuffer = ByteBuffer.allocateDirect(size * size * size * 3)
            .order(ByteOrder.nativeOrder())

        for (bIdx in 0 until size) {
            val b = bIdx.toFloat() / (size - 1)
            for (gIdx in 0 until size) {
                val g = gIdx.toFloat() / (size - 1)
                for (rIdx in 0 until size) {
                    val r = rIdx.toFloat() / (size - 1)

                    // Luminance
                    val luma = 0.299f * r + 0.587f * g + 0.114f * b

                    // Push shadows toward teal/cyan, highlights toward amber/warm orange
                    val shadowTeal = floatArrayOf(0.1f * luma, 0.4f * luma, 0.6f * luma)
                    val highlightWarm = floatArrayOf(1.0f * (1f - luma), 0.7f * (1f - luma), 0.3f * (1f - luma))

                    val outR = (r * 1.15f + highlightWarm[0] * 0.25f - shadowTeal[0] * 0.15f).coerceIn(0f, 1f)
                    val outG = (g * 0.98f + highlightWarm[1] * 0.15f + shadowTeal[1] * 0.15f).coerceIn(0f, 1f)
                    val outB = (b * 0.85f - highlightWarm[2] * 0.2f + shadowTeal[2] * 0.35f).coerceIn(0f, 1f)

                    byteBuffer.put((outR * 255f).toInt().toByte())
                    byteBuffer.put((outG * 255f).toInt().toByte())
                    byteBuffer.put((outB * 255f).toInt().toByte())
                }
            }
        }
        byteBuffer.rewind()
        return Lut3dData(size = size, buffer = byteBuffer)
    }

    /**
     * Generates a built-in Kodak Portra Film 3D LUT (size 17x17x17)
     */
    fun createKodakPortra(size: Int = 17): Lut3dData {
        val byteBuffer = ByteBuffer.allocateDirect(size * size * size * 3)
            .order(ByteOrder.nativeOrder())

        for (bIdx in 0 until size) {
            val b = bIdx.toFloat() / (size - 1)
            for (gIdx in 0 until size) {
                val g = gIdx.toFloat() / (size - 1)
                for (rIdx in 0 until size) {
                    val r = rIdx.toFloat() / (size - 1)

                    // Film S-curve tone curve with soft rolled highlights
                    val outR = (r.pow(0.92f) * 1.05f).coerceIn(0f, 1f)
                    val outG = (g.pow(0.96f) * 1.02f).coerceIn(0f, 1f)
                    val outB = (b.pow(1.05f) * 0.94f).coerceIn(0f, 1f)

                    byteBuffer.put((outR * 255f).toInt().toByte())
                    byteBuffer.put((outG * 255f).toInt().toByte())
                    byteBuffer.put((outB * 255f).toInt().toByte())
                }
            }
        }
        byteBuffer.rewind()
        return Lut3dData(size = size, buffer = byteBuffer)
    }

    /**
     * Generates a Cineon / ARRI LogC inspired Flat Log 3D LUT (size 17x17x17).
     * Compresses highlights and lifts shadows to maximize dynamic range headroom.
     */
    fun createFlatLog(size: Int = 17): Lut3dData {
        val byteBuffer = ByteBuffer.allocateDirect(size * size * size * 3)
            .order(ByteOrder.nativeOrder())

        for (bIdx in 0 until size) {
            val b = bIdx.toFloat() / (size - 1)
            for (gIdx in 0 until size) {
                val g = gIdx.toFloat() / (size - 1)
                for (rIdx in 0 until size) {
                    val r = rIdx.toFloat() / (size - 1)

                    // Logarithmic tone mapping curve: lifts shadows, compresses highlights
                    val outR = (0.24f * ln(5.5f * r + 1.0f) + 0.12f * (1.0f - r) + 0.64f * r).coerceIn(0f, 1f)
                    val outG = (0.24f * ln(5.5f * g + 1.0f) + 0.12f * (1.0f - g) + 0.64f * g).coerceIn(0f, 1f)
                    val outB = (0.24f * ln(5.5f * b + 1.0f) + 0.12f * (1.0f - b) + 0.64f * b).coerceIn(0f, 1f)

                    byteBuffer.put((outR * 255f).toInt().toByte())
                    byteBuffer.put((outG * 255f).toInt().toByte())
                    byteBuffer.put((outB * 255f).toInt().toByte())
                }
            }
        }
        byteBuffer.rewind()
        return Lut3dData(size = size, buffer = byteBuffer)
    }
}
