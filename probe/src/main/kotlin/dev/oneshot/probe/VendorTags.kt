package dev.oneshot.probe

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Vendor tag enumeration and classification.
 *
 * This is the only route to a device's native LOG profile and to any vendor sensor-mode /
 * binning control, because AOSP standardises no LOG dynamic-range profile and pairs no
 * full-sensor stream with a RECORD stream.
 *
 * Important detail: [CameraCharacteristics.Key] does not publicly expose its declared type
 * (`getType()` is @hide), so we cannot ask the framework what a vendor tag's type is. We do
 * not need to — the returned Key objects already carry the HAL-declared type internally, so
 * passing them straight back to `characteristics.get()` / `result.get()` yields a correctly
 * typed value. Types below are therefore *inferred from the observed value*, which is why
 * [dumpFromResult] exists: it is the only place we can learn a request key's real type.
 */
object VendorTags {

    /** Tags whose names suggest colour/gamma/log control, sensor readout mode, or image tuning. */
    internal val INTERESTING = Regex(
        "(?i)log|gamma|tone|gate|binning|sensor.?mode|lut|hdr|cinema|" +
            "saturation|sharpness|contrast|denoise|noise|cds|lofic|remosaic|profile"
    )

    /** Known-real Qualcomm section prefixes, kept for orientation only — never coded against. */
    private val KNOWN_NAMESPACES = listOf(
        "org.codeaurora.qcamera3",
        "org.quic.camera",
        "com.qti",
        "com.xiaomi",
        "com.mi",
        "vendor.xiaomi",
        "com.miui",
        "org.chromium",
        "com.google",
    )

    fun dump(chars: CameraCharacteristics): JsonObject = buildJsonObject {
        put("note", "Names come from the framework's vendor tag cache. Types are @hide, so " +
            "they are inferred from observed values in phase B (dumpFromResult).")

        put("characteristics", keyList(chars.safeKeys(), chars))
        put("request", nameList(chars.safeRequestKeys().map { it.name }))
        put("result", nameList(chars.safeResultKeys().map { it.name }))
        put("session", nameList(chars.safeSessionKeys().map { it.name }))

        val sessionNames = chars.safeSessionKeys().map { it.name }.toSet()
        val requestNames = chars.safeRequestKeys().map { it.name }.toSet()

        put("classification", buildJsonObject {
            put("sessionAndInteresting", jsonOf(sessionNames.filter { INTERESTING.containsMatchIn(it) }.sorted()))
            put("requestAndInteresting", jsonOf(requestNames.filter { INTERESTING.containsMatchIn(it) }.sorted()))
            put("sessionIsSubsetOfRequest", sessionNames.all { it in requestNames })
            put("sessionCount", sessionNames.size)
            put("interestingSessionCount", sessionNames.count { INTERESTING.containsMatchIn(it) })
            put("namespacesPresent", jsonOf(KNOWN_NAMESPACES.filter { ns ->
                requestNames.any { it.startsWith(ns) } || sessionNames.any { it.startsWith(ns) }
            }))
        })
    }

    /**
     * Reads every result key that carries a value, recording the inferred type. Run in phase B
     * once a live [CaptureResult] exists.
     */
    fun dumpFromResult(result: CaptureResult, requestKeys: List<CaptureRequest.Key<*>>): JsonObject =
        buildJsonObject {
            put("resultKeys", buildJsonArray {
                result.keys.forEach { key ->
                    val value = runCatching { result.get(key) }.getOrNull()
                    add(buildJsonObject {
                        put("name", key.name)
                        if (INTERESTING.containsMatchIn(key.name)) put("interesting", true)
                        put("valueType", value?.javaClass?.simpleName)
                        put("value", summarise(value))
                    })
                }
            })
            // Request-only keys never appear in results; record their presence so we know the
            // HAL advertises them even though we cannot learn their type without setting them.
            val resultNames = result.keys.map { it.name }.toSet()
            put("requestOnlyKeys", jsonOf(requestKeys.map { it.name }
                .filter { INTERESTING.containsMatchIn(it) && it !in resultNames }
                .sorted()))
        }

    private fun summarise(value: Any?): JsonElement = when {
        value == null -> JsonNull
        value is ByteArray && value.size > 64 -> JsonPrimitive("<${value.size} bytes>")
        value is IntArray && value.size > 64 -> JsonPrimitive("<${value.size} ints>")
        value is LongArray && value.size > 64 -> JsonPrimitive("<${value.size} longs>")
        value is FloatArray && value.size > 64 -> JsonPrimitive("<${value.size} floats>")
        value is Array<*> && value.size > 64 -> JsonPrimitive("<${value.size} elements>")
        else -> jsonOf(value)
    }

    private fun nameList(names: List<String>): JsonElement =
        JsonArray(names.sorted().map { JsonPrimitive(it) })

    private fun keyList(
        keys: List<CameraCharacteristics.Key<*>>,
        chars: CameraCharacteristics,
    ): JsonElement = buildJsonArray {
        keys.sortedBy { it.name }.forEach { key ->
            val value = runCatching { chars.get(key) }.getOrNull()
            add(buildJsonObject {
                put("name", key.name)
                if (INTERESTING.containsMatchIn(key.name)) put("interesting", true)
                put("valueType", value?.javaClass?.simpleName)
                put("value", summarise(value))
            })
        }
    }
}

internal fun CameraCharacteristics.safeKeys(): List<CameraCharacteristics.Key<*>> =
    runCatching { keys.toList() }.getOrElse { emptyList() }

internal fun CameraCharacteristics.safeRequestKeys(): List<CaptureRequest.Key<*>> =
    runCatching { availableCaptureRequestKeys.toList() }.getOrElse { emptyList() }

internal fun CameraCharacteristics.safeResultKeys(): List<CaptureResult.Key<*>> =
    runCatching { availableCaptureResultKeys.toList() }.getOrElse { emptyList() }

/**
 * Session keys are the only vendor tags that can be passed to
 * `SessionConfigurationInterop.setSessionParameter`. AOSP silently ignores anything not in
 * this list, so treating it as authoritative is what stops us believing a tag worked.
 */
internal fun CameraCharacteristics.safeSessionKeys(): List<CaptureRequest.Key<*>> =
    runCatching { availableSessionKeys.toList() }.getOrElse { emptyList() }
