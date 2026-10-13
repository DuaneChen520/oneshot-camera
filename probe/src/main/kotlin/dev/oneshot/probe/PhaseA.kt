package dev.oneshot.probe

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.DynamicRangeProfiles
import android.hardware.camera2.params.MandatoryStreamCombination
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Range
import android.util.Size
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.abs
import kotlin.math.hypot

/** Nanoseconds per frame at 30fps. Above this a stream cannot sustain 30fps. */
internal const val NS_PER_FRAME_30FPS = 33_333_333L

private val CAPABILITY_NAMES = mapOf(
    0 to "BACKWARD_COMPATIBLE", 1 to "MANUAL_SENSOR", 2 to "MANUAL_POST_PROCESSING",
    3 to "RAW", 4 to "PRIVATE_REPROCESSING", 5 to "READ_SENSOR_SETTINGS", 6 to "BURST_CAPTURE",
    7 to "YUV_REPROCESSING", 8 to "DEPTH_OUTPUT", 9 to "CONSTRAINED_HIGH_SPEED_VIDEO",
    10 to "MOTION_TRACKING", 11 to "LOGICAL_MULTI_CAMERA", 12 to "MONOCHROME",
    13 to "SECURE_IMAGE_DATA", 14 to "SYSTEM_CAMERA", 15 to "OFFLINE_PROCESSING",
    16 to "ULTRA_HIGH_RESOLUTION_SENSOR", 17 to "REMOSAIC_REPROCESSING",
    18 to "DYNAMIC_RANGE_TEN_BIT", 19 to "STREAM_USE_CASE",
)

private val IMAGE_FORMAT_NAMES: Map<Int, String> = mapOf(
    ImageFormat.RAW_SENSOR to "RAW_SENSOR",
    ImageFormat.RAW10 to "RAW10",
    ImageFormat.RAW12 to "RAW12",
    ImageFormat.RAW_PRIVATE to "RAW_PRIVATE",
    ImageFormat.JPEG to "JPEG",
    ImageFormat.JPEG_R to "JPEG_R",
    ImageFormat.PRIVATE to "PRIVATE",
    ImageFormat.YUV_420_888 to "YUV_420_888",
    ImageFormat.NV21 to "NV21",
    ImageFormat.NV16 to "NV16",
    ImageFormat.Y8 to "Y8",
    ImageFormat.DEPTH16 to "DEPTH16",
    ImageFormat.DEPTH_JPEG to "DEPTH_JPEG",
    ImageFormat.DEPTH_POINT_CLOUD to "DEPTH_POINT_CLOUD",
    ImageFormat.HEIC to "HEIC",
    ImageFormat.HEIC_ULTRAHDR to "HEIC_ULTRAHDR",
    ImageFormat.YCBCR_P010 to "YCBCR_P010",
)

/** Bit values read from CameraMetadata, so the names here always match the platform. */
private val DYNAMIC_RANGE_NAMES: List<Pair<Long, String>> = listOf(
    DynamicRangeProfiles.STANDARD to "STANDARD",
    DynamicRangeProfiles.HLG10 to "HLG10",
    DynamicRangeProfiles.HDR10 to "HDR10",
    DynamicRangeProfiles.HDR10_PLUS to "HDR10_PLUS",
    DynamicRangeProfiles.DOLBY_VISION_10B_HDR_REF to "DOLBY_VISION_10B_HDR_REF",
    DynamicRangeProfiles.DOLBY_VISION_10B_HDR_REF_PO to "DOLBY_VISION_10B_HDR_REF_PO",
    DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM to "DOLBY_VISION_10B_HDR_OEM",
    DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM_PO to "DOLBY_VISION_10B_HDR_OEM_PO",
    DynamicRangeProfiles.DOLBY_VISION_8B_HDR_REF to "DOLBY_VISION_8B_HDR_REF",
    DynamicRangeProfiles.DOLBY_VISION_8B_HDR_REF_PO to "DOLBY_VISION_8B_HDR_REF_PO",
    DynamicRangeProfiles.DOLBY_VISION_8B_HDR_OEM to "DOLBY_VISION_8B_HDR_OEM",
    DynamicRangeProfiles.DOLBY_VISION_8B_HDR_OEM_PO to "DOLBY_VISION_8B_HDR_OEM_PO",
)

private val STREAM_USE_CASE_NAMES: List<Pair<Long, String>> = listOf(
    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT.toLong() to "DEFAULT",
    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong() to "PREVIEW",
    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE.toLong() to "STILL_CAPTURE",
    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong() to "VIDEO_RECORD",
    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL.toLong() to "PREVIEW_VIDEO_STILL",
    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_CALL.toLong() to "VIDEO_CALL",
    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_CROPPED_RAW.toLong() to "CROPPED_RAW",
)

private val COLOR_CORRECTION_MODE_NAMES: Map<Int, String> = mapOf(
    CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX to "TRANSFORM_MATRIX",
    CameraMetadata.COLOR_CORRECTION_MODE_FAST to "FAST",
    CameraMetadata.COLOR_CORRECTION_MODE_HIGH_QUALITY to "HIGH_QUALITY",
    CameraMetadata.COLOR_CORRECTION_MODE_CCT to "CCT",
)

/** Reads a characteristic without ever throwing — the probe must survive unknown API levels. */
internal fun <T> CameraCharacteristics.safe(key: CameraCharacteristics.Key<T>): T? =
    try {
        get(key)
    } catch (_: Throwable) {
        null
    }

internal fun formatName(format: Int): String =
    IMAGE_FORMAT_NAMES[format] ?: "0x${Integer.toHexString(format)}"

internal fun dynamicRangeName(profile: Long): String =
    DYNAMIC_RANGE_NAMES.firstOrNull { it.first == profile }?.second ?: "0x${java.lang.Long.toHexString(profile)}"

internal fun streamUseCaseName(useCase: Long): String =
    STREAM_USE_CASE_NAMES.firstOrNull { it.first == useCase }?.second ?: useCase.toString()

internal fun Size.aspect(): Float = width.toFloat() / height

internal fun aspectLabel(ratio: Float): String = when {
    abs(ratio - 4f / 3f) < 0.02f -> "4:3"
    abs(ratio - 3f / 4f) < 0.02f -> "3:4"
    abs(ratio - 16f / 9f) < 0.02f -> "16:9"
    abs(ratio - 9f / 16f) < 0.02f -> "9:16"
    abs(ratio - 1f) < 0.02f -> "1:1"
    abs(ratio - 3f / 2f) < 0.02f -> "3:2"
    abs(ratio - 2f) < 0.02f -> "2:1"
    ratio > 2f -> "wider-than-2:1"
    else -> "%.4f".format(ratio)
}

/**
 * Phase A: read-only [CameraCharacteristics] dump. Opens no camera, so it cannot fail for
 * permission or thermal reasons and always produces the bulk of the report.
 */
class PhaseA(private val cameraManager: CameraManager) {

    fun run(report: ProbeReport) {
        val ids = cameraManager.cameraIdList.toList()

        report["device"] = buildJsonObject {
            put("fingerprint", Build.FINGERPRINT)
            put("manufacturer", Build.MANUFACTURER)
            put("model", Build.MODEL)
            put("device", Build.DEVICE)
            put("sdkInt", Build.VERSION.SDK_INT)
            put("release", Build.VERSION.RELEASE)
            put("incremental", Build.VERSION.INCREMENTAL)
        }

        report["concurrentCameraIds"] = runCatching {
            cameraManager.concurrentCameraIds.map { set -> set.toList() }.let { jsonOf(it) }
        }.getOrElse { JsonArray(emptyList()) }

        report["cameraIds"] = buildJsonArray {
            ids.forEach { id ->
                val chars = runCatching { cameraManager.getCameraCharacteristics(id) }.getOrNull()
                if (chars != null) add(cameraReport(id, chars))
            }
        }
    }

    private fun cameraReport(id: String, chars: CameraCharacteristics): JsonObject {
        val activeArray = chars.safe(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val activeAspect = activeArray?.let { it.width().toFloat() / it.height() }
        val sensorPhys = chars.safe(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)

        return buildJsonObject {
            put("cameraId", id)
            put("physicalCameraIds", jsonOf(runCatching { chars.physicalCameraIds.toList() }.getOrElse { emptyList<String>() }))
            put("lensFacing", chars.safe(CameraCharacteristics.LENS_FACING)?.let { facing ->
                when (facing) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"
                    CameraCharacteristics.LENS_FACING_BACK -> "BACK"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "EXTERNAL"
                    else -> "UNKNOWN($facing)"
                }
            })
            put("hardwareLevel", chars.safe(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)?.let { level ->
                when (level) {
                    CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                    CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                    CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                    CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                    CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
                    else -> "UNKNOWN($level)"
                }
            })

            put("requestAvailableCapabilities", buildJsonArray {
                chars.safe(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.forEach { cap ->
                    add(buildJsonObject {
                        put("value", cap)
                        put("name", CAPABILITY_NAMES[cap] ?: "UNKNOWN")
                    })
                }
            })

            put("sensor", buildJsonObject {
                put("activeArraySize", activeArray?.let { "${it.width()}x${it.height()}" })
                put("activeArrayAspectRatio", activeAspect)
                put("activeArrayAspectLabel", activeAspect?.let { aspectLabel(it) })
                put("pixelArraySize", chars.safe(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)?.let { "${it.width}x${it.height}" })
                put("physicalSizeMm", jsonOf(sensorPhys?.let {
                    buildJsonObject { put("width", it.width.toDouble()); put("height", it.height.toDouble()) }
                }))
                put("sensorDiagonalMm", sensorPhys?.let { hypot(it.width.toDouble(), it.height.toDouble()) })
                put("colorFilterArrangement", chars.safe(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT)?.let { cfa ->
                    when (cfa) {
                        CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGGB -> "RGGB"
                        CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GRBG -> "GRBG"
                        CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GBRG -> "GBRG"
                        CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_BGGR -> "BGGR"
                        CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGB -> "RGB"
                        else -> "UNKNOWN($cfa)"
                    }
                })
                put("timestampSource", chars.safe(CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE))
                put("orientationDegrees", chars.safe(CameraCharacteristics.SENSOR_ORIENTATION))
                if (Build.VERSION.SDK_INT >= 31) {
                    put("activeArraySizeMaximumResolution",
                        chars.safe(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION)
                            ?.let { "${it.width()}x${it.height()}" })
                    put("pixelArraySizeMaximumResolution",
                        chars.safe(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION)
                            ?.let { "${it.width}x${it.height}" })
                }
            })

            put("lens", buildJsonObject {
                val focals = chars.safe(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                put("availableFocalLengthsMm", jsonOf(focals))
                put("minimumFocusDistanceDiopters", chars.safe(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE))
                put("hyperfocalDistance", chars.safe(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE))
                if (focals != null && sensorPhys != null && sensorPhys.width > 0f && sensorPhys.height > 0f) {
                    val diag = hypot(sensorPhys.width.toDouble(), sensorPhys.height.toDouble())
                    put("equivalent35mmFocalLengths", jsonOf(focals.map { focal ->
                        buildJsonObject {
                            put("focalLengthMm", focal.toDouble())
                            put("eq35mm", focal * 43.2666f / diag)
                        }
                    }))
                }
            })

            put("manualRanges", buildJsonObject {
                put("exposureTimeRangeNs", jsonOf(chars.safe(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.asRangeJson()))
                put("sensitivityRange", jsonOf(chars.safe(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.asRangeJson()))
                put("aeCompensationRange", jsonOf(chars.safe(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)?.asRangeJson()))
                put("aeCompensationStep", chars.safe(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)?.let {
                    it.numerator.toDouble() / it.denominator.toDouble()
                })
                put("availableAeTargetFpsRanges", jsonOf(
                    chars.safe(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)?.map { it.asRangeJson() }
                ))
                put("aeAvailableModes", jsonOf(chars.safe(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)))
                put("afAvailableModes", jsonOf(chars.safe(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)))
                put("awbAvailableModes", jsonOf(chars.safe(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)))
                put("availableModes", jsonOf(chars.safe(CameraCharacteristics.CONTROL_AVAILABLE_MODES)))
                put("maxRegionsAe", chars.safe(CameraCharacteristics.CONTROL_MAX_REGIONS_AE))
                put("maxRegionsAf", chars.safe(CameraCharacteristics.CONTROL_MAX_REGIONS_AF))
                put("maxRegionsAwb", chars.safe(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB))
                put("availableEffectsModes", jsonOf(chars.safe(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS)))
                put("availableSceneModes", jsonOf(chars.safe(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)))
                put("availableVideoStabilizationModes",
                    jsonOf(chars.safe(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)))
            })

            put("colorCorrection", buildJsonObject {
                val modes = chars.safe(CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_MODES)?.toList() ?: emptyList()
                put("availableModes", jsonOf(modes))
                put("availableModeNames", jsonOf(modes.map { COLOR_CORRECTION_MODE_NAMES[it] ?: "UNKNOWN($it)" }))
                // CCT == 3 is the native ABGM-pad path (API 36, @FlaggedApi on the request keys).
                put("supportsCctMode", modes.contains(CameraMetadata.COLOR_CORRECTION_MODE_CCT))
                if (Build.VERSION.SDK_INT >= 36) {
                    put("colorTemperatureRange", jsonOf(chars.safe(
                        CameraCharacteristics.COLOR_CORRECTION_COLOR_TEMPERATURE_RANGE
                    )?.asRangeJson()))
                }
                put("availableAberrationModes", jsonOf(chars.safe(CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES)))
            })

            put("streamUseCases", buildJsonObject {
                if (Build.VERSION.SDK_INT >= 33) {
                    val available = chars.safe(CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES)?.toList() ?: emptyList()
                    put("available", jsonOf(available))
                    put("availableNames", jsonOf(available.map { streamUseCaseName(it) }))
                    put("mandatoryUseCaseCombinations", mandatoryCombinations(
                        chars, CameraCharacteristics.SCALER_MANDATORY_USE_CASE_STREAM_COMBINATIONS))
                }
                put("mandatoryBaseCombinations", mandatoryCombinations(
                    chars, CameraCharacteristics.SCALER_MANDATORY_STREAM_COMBINATIONS))
                put("mandatoryTenBitCombinations", mandatoryCombinations(
                    chars, CameraCharacteristics.SCALER_MANDATORY_TEN_BIT_OUTPUT_STREAM_COMBINATIONS))
                put("mandatoryMaximumResolutionCombinations", mandatoryCombinations(
                    chars, CameraCharacteristics.SCALER_MANDATORY_MAXIMUM_RESOLUTION_STREAM_COMBINATIONS))
                put("mandatoryPreviewStabilizationCombinations", mandatoryCombinations(
                    chars, CameraCharacteristics.SCALER_MANDATORY_PREVIEW_STABILIZATION_OUTPUT_STREAM_COMBINATIONS))
                put("mandatoryConcurrentCombinations", mandatoryCombinations(
                    chars, CameraCharacteristics.SCALER_MANDATORY_CONCURRENT_STREAM_COMBINATIONS))
            })

            put("dynamicRangeProfiles", dynamicRangeReport(chars))
            put("streamConfig", streamConfigReport(chars, activeAspect))
            put("vendorTags", VendorTags.dump(chars))
        }
    }

    private fun <T> Range<T>.asRangeJson(): JsonObject where T : Comparable<T> = buildJsonObject {
        put("min", lower.toString())
        put("max", upper.toString())
    }

    /**
     * AOSP's guaranteed stream-combination tables. [MandatoryStreamCombination] exposes the
     * per-stream format, the 10-bit variant, and — critically — the list of sizes AOSP promises
     * for that slot. This is a stronger statement than inferring capability from
     * `getOutputMinFrameDuration`, because it is what the platform promises rather than what
     * the HAL merely advertises.
     */
    private fun mandatoryCombinations(
        chars: CameraCharacteristics,
        key: CameraCharacteristics.Key<Array<MandatoryStreamCombination>>,
    ): JsonElement {
        val combos = chars.safe(key) ?: return JsonNull
        return buildJsonArray {
            combos.forEach { combo ->
                add(buildJsonObject {
                    put("description", combo.description?.toString())
                    put("reprocessable", combo.isReprocessable)
                    put("streams", buildJsonArray {
                        combo.streamsInformation.forEach { s ->
                            add(buildJsonObject {
                                put("streamUseCase", s.streamUseCase)
                                put("streamUseCaseName", streamUseCaseName(s.streamUseCase))
                                put("format", s.format)
                                put("formatName", formatName(s.format))
                                put("is10BitCapable", s.is10BitCapable())
                                put("bit10Format", runCatching { formatName(s.get10BitFormat()) }.getOrNull())
                                put("isInput", s.isInput())
                                put("isMaximumSize", s.isMaximumSize())
                                put("isUltraHighResolution", s.isUltraHighResolution())
                                put("availableSizes", jsonOf(s.availableSizes?.map { "${it.width}x${it.height} ${aspectLabel(it.aspect())}" }))
                            })
                        }
                    })
                })
            }
        }
    }

    private fun dynamicRangeReport(chars: CameraCharacteristics): JsonObject = buildJsonObject {
        val profiles: DynamicRangeProfiles? =
            if (Build.VERSION.SDK_INT >= 33) chars.safe(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES) else null
        if (profiles == null) {
            put("supported", JsonArray(emptyList()))
            return@buildJsonObject
        }
        // getSupportedProfiles() is a Set<Long>, not a bit field.
        val supported: Set<Long> = runCatching { profiles.supportedProfiles }.getOrElse { emptySet() }
        put("supported", buildJsonArray {
            supported.sorted().forEach { bit ->
                add(buildJsonObject {
                    put("bit", bit)
                    put("name", dynamicRangeName(bit))
                    put("extraLatencyPresent", runCatching { profiles.isExtraLatencyPresent(bit) }.getOrNull())
                    put("captureRequestConstraints", jsonOf(runCatching {
                        profiles.getProfileCaptureRequestConstraints(bit).map { it.toString() }
                    }.getOrNull()))
                })
            }
        })
        put("containsHLG10", supported.contains(DynamicRangeProfiles.HLG10))
        put("recommendedTenBitProfile", chars.safe(
            CameraCharacteristics.REQUEST_RECOMMENDED_TEN_BIT_DYNAMIC_RANGE_PROFILE
        )?.let { dynamicRangeName(it) })
    }

    private fun streamConfigReport(chars: CameraCharacteristics, activeAspect: Float?): JsonObject {
        val map = chars.safe(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: return buildJsonObject { put("error", "no SCALER_STREAM_CONFIGURATION_MAP") }

        return buildJsonObject {
            put("sizesByFormat", sizesByFormat(map))
            put("highSpeedVideo", buildJsonObject {
                put("sizes", jsonOf(runCatching { map.highSpeedVideoSizes?.map { "${it.width}x${it.height}" } }.getOrNull()))
                put("fpsRanges", jsonOf(runCatching { map.highSpeedVideoFpsRanges?.map { "${it.lower}-${it.upper}" } }.getOrNull()))
                put("sizesForRange", jsonOf(runCatching {
                    map.highSpeedVideoFpsRanges?.map { range ->
                        buildJsonObject {
                            put("range", "${range.lower}-${range.upper}")
                            put("sizes", jsonOf(map.getHighSpeedVideoSizesFor(range)?.map { "${it.width}x${it.height}" }))
                        }
                    }
                }.getOrNull()))
            })
            if (Build.VERSION.SDK_INT >= 31) {
                val maxMap = chars.safe(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION)
                put("maximumResolutionSizesByFormat", maxMap?.let { sizesByFormat(it) } ?: JsonNull)
            }
            put("multiResolutionMapPresent",
                chars.safe(CameraCharacteristics.SCALER_MULTI_RESOLUTION_STREAM_CONFIGURATION_MAP) != null)
            put("openGateCandidates", openGateCandidates(map, activeAspect))
        }
    }

    private fun sizesByFormat(map: StreamConfigurationMap): JsonElement {
        val highResByFormat = if (Build.VERSION.SDK_INT >= 31) {
            runCatching {
                map.outputFormats.associateWith { f ->
                    runCatching { map.getHighResolutionOutputSizes(f).toSet() }.getOrDefault(emptySet())
                }
            }.getOrDefault(emptyMap())
        } else emptyMap()

        return JsonObject(map.outputFormats.associate { format ->
            val sizes = runCatching { map.getOutputSizes(format) }.getOrNull() ?: emptyArray()
            val highRes = highResByFormat[format] ?: emptySet()

            formatName(format) to buildJsonObject {
                put("format", format)
                put("count", sizes.size)
                put("sizes", buildJsonArray {
                    sizes.sortedByDescending { it.width.toLong() * it.height }.forEach { size ->
                        add(sizeEntry(map, format, size, size in highRes))
                    }
                })
            }
        })
    }

    private fun sizeEntry(map: StreamConfigurationMap, format: Int, size: Size, stillsOnly: Boolean): JsonObject =
        buildJsonObject {
            put("size", "${size.width}x${size.height}")
            put("width", size.width)
            put("height", size.height)
            put("aspectRatio", size.aspect())
            put("aspectLabel", aspectLabel(size.aspect()))
            put("pixels", size.width.toLong() * size.height)
            val minDuration = runCatching { map.getOutputMinFrameDuration(format, size) }.getOrNull()
            put("minFrameDurationNs", minDuration)
            put("maxFps", minDuration?.takeIf { it > 0 }?.let { 1_000_000_000.0 / it })
            put("stallDurationNs", runCatching { map.getOutputStallDuration(format, size) }.getOrNull())
            put("stillsOnlyHighResolution", stillsOnly)
            put("sustains30fps", minDuration != null && minDuration in 1..NS_PER_FRAME_30FPS && !stillsOnly)
        }

    /**
     * Open-gate evidence. Two independent questions, reported separately because they have
     * different answers and different consequences:
     *
     *  (a) does the HAL advertise a PRIVATE stream at the sensor's own aspect ratio that can
     *      sustain 30fps? — CameraX can select this through the PUBLIC
     *      `Recorder.Builder.setAspectRatio(AspectRatio.RATIO_4_3)`, provided the sensor's
     *      aspect is 4:3 (which covers essentially every phone sensor).
     *  (b) is there a *larger* 4:3 stream than CameraX's Quality ladder would offer? — that
     *      extra headroom is the only thing needing the @RestrictTo `setCustomOrderedResolutions`
     *      door, and it is what M3.5 actually measures.
     */
    private fun openGateCandidates(map: StreamConfigurationMap, activeAspect: Float?): JsonObject = buildJsonObject {
        put("activeArrayAspectRatio", activeAspect)
        put("criteria", "aspect within 2% of active array AND minFrameDuration <= 33.33ms AND not in getHighResolutionOutputSizes")

        val videoFormats = intArrayOf(ImageFormat.PRIVATE, ImageFormat.YUV_420_888, ImageFormat.YCBCR_P010)
        val allVideoCapable = mutableListOf<Pair<Int, Size>>()

        videoFormats.forEach { format ->
            val sizes = runCatching { map.getOutputSizes(format) }.getOrNull() ?: return@forEach
            val highRes = if (Build.VERSION.SDK_INT >= 31) {
                runCatching { map.getHighResolutionOutputSizes(format).toSet() }.getOrDefault(emptySet())
            } else emptySet()

            sizes.forEach { size ->
                val minDuration = runCatching { map.getOutputMinFrameDuration(format, size) }.getOrDefault(Long.MAX_VALUE)
                if (minDuration in 1..NS_PER_FRAME_30FPS && size !in highRes) allVideoCapable += format to size
            }
        }

        put("aspectHistogram", buildJsonObject {
            allVideoCapable.groupBy { aspectLabel(it.second.aspect()) }
                .forEach { (label, entries) ->
                    put(label, buildJsonObject {
                        put("count", entries.size)
                        put("largest", entries.maxOf { it.second.width.toLong() * it.second.height }
                            .let { p -> entries.first { it.second.width.toLong() * it.second.height == p }.second.toString() })
                    })
                }
        })

        if (activeAspect != null) {
            put("fullSensorAspectVideoCapable", buildJsonArray {
                allVideoCapable
                    .filter { abs(it.second.aspect() - activeAspect) <= 0.02f * activeAspect }
                    .sortedByDescending { it.second.width.toLong() * it.second.height }
                    .take(8)
                    .forEach { (format, size) ->
                        add(buildJsonObject {
                            put("format", formatName(format))
                            put("size", "${size.width}x${size.height}")
                            put("aspectLabel", aspectLabel(size.aspect()))
                            put("minFrameDurationNs", map.getOutputMinFrameDuration(format, size))
                        })
                    }
            })
        }

        val fourThree = allVideoCapable.filter { abs(it.second.aspect() - 4f / 3f) < 0.02f }
        put("ratio4ThreeVideoCapable", buildJsonArray {
            fourThree.sortedByDescending { it.second.width.toLong() * it.second.height }
                .take(10)
                .forEach { (format, size) ->
                    add(buildJsonObject {
                        put("format", formatName(format))
                        put("size", "${size.width}x${size.height}")
                        put("minFrameDurationNs", map.getOutputMinFrameDuration(format, size))
                    })
                }
        })
        // What CameraX's public setAspectRatio(RATIO_4_3) can reach is bounded by the sizes the
        // Quality ladder offers; this records the ceiling so M3.5 knows what's left over.
        put("largestRatio4Three", fourThree.maxByOrNull { it.second.width.toLong() * it.second.height }
            ?.second?.let { "${it.width}x${it.height}" })
        put("largestVideoCapableOverall", allVideoCapable.maxByOrNull { it.second.width.toLong() * it.second.height }
            ?.second?.let { "${it.width}x${it.height} (${aspectLabel(it.aspect())})" })
    }
}
