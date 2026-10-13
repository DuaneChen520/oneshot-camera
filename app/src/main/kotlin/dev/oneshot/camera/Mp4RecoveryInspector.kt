package dev.oneshot.camera

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Diagnostic and integrity tool for MP4 video recordings.
 * Inspects container atom hierarchy (ftyp, mdat, moov) and verifies file finalization.
 */
object Mp4RecoveryInspector {

    data class Mp4IntegrityResult(
        val file: File,
        val hasFtyp: Boolean,
        val hasMdat: Boolean,
        val hasMoov: Boolean,
        val isHealthy: Boolean,
        val mdatOffset: Long,
        val mdatSize: Long,
        val fileSize: Long,
        val diagnosticMessage: String,
    )

    /**
     * Inspects an MP4 file for container atom integrity.
     */
    fun inspect(file: File): Mp4IntegrityResult {
        if (!file.exists() || file.length() < 8) {
            return Mp4IntegrityResult(
                file = file,
                hasFtyp = false,
                hasMdat = false,
                hasMoov = false,
                isHealthy = false,
                mdatOffset = -1,
                mdatSize = -1,
                fileSize = file.length(),
                diagnosticMessage = "文件不存在或小于 8 字节"
            )
        }

        var hasFtyp = false
        var hasMdat = false
        var hasMoov = false
        var mdatOffset = -1L
        var mdatSize = -1L

        val length = file.length()
        runCatching {
            RandomAccessFile(file, "r").use { raf ->
                var offset = 0L
                val header = ByteArray(8)
                val buffer = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN)

                while (offset + 8 <= length) {
                    raf.seek(offset)
                    raf.readFully(header)
                    buffer.rewind()

                    val atomSize = buffer.int.toLong() and 0xFFFFFFFFL
                    val atomType = String(header, 4, 4, Charsets.US_ASCII)

                    when (atomType) {
                        "ftyp" -> hasFtyp = true
                        "mdat" -> {
                            hasMdat = true
                            mdatOffset = offset
                            mdatSize = if (atomSize == 1L) {
                                // Extended 64-bit size
                                raf.seek(offset + 8)
                                raf.readLong()
                            } else {
                                atomSize
                            }
                        }
                        "moov" -> hasMoov = true
                    }

                    val step = when {
                        atomSize == 0L -> length - offset // Extends to EOF
                        atomSize == 1L -> {
                            raf.seek(offset + 8)
                            raf.readLong()
                        }
                        atomSize > 0 -> atomSize
                        else -> break
                    }

                    if (step <= 0) break
                    offset += step
                }
            }
        }

        val isHealthy = hasFtyp && hasMdat && hasMoov
        val message = when {
            isHealthy -> "视频结构完整，moov 索引正常"
            !hasMoov && hasMdat -> "检测到断电/异常中断视频：mdat 数据存在 (${mdatSize / (1024 * 1024)} MB)，缺少 moov 索引元数据"
            !hasFtyp -> "损坏的文件：缺少标准 ftyp 文件头"
            else -> "视频流不完整"
        }

        return Mp4IntegrityResult(
            file = file,
            hasFtyp = hasFtyp,
            hasMdat = hasMdat,
            hasMoov = hasMoov,
            isHealthy = isHealthy,
            mdatOffset = mdatOffset,
            mdatSize = mdatSize,
            fileSize = length,
            diagnosticMessage = message
        )
    }
}
