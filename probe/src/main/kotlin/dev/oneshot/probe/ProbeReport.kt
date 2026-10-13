package dev.oneshot.probe

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File

/**
 * Crash-resilient accumulator for the probe report.
 *
 * The tree is a plain map rather than a @Serializable model because the interesting
 * content is device-specific and untyped: vendor tag values arrive as Int/Long/Float/
 * byte[]/int[]/String depending on what the HAL declared, and the key set varies per
 * OEM. A typed schema would either lose that or need 80 data classes.
 *
 * Every phase flushes [writeTo] after it completes, so a crash in a later phase still
 * leaves everything earlier on disk.
 */
class ProbeReport {

    private val root = LinkedHashMap<String, JsonElement>()

    operator fun set(section: String, value: JsonElement) {
        root[section] = value
    }

    fun snapshot(): JsonObject = JsonObject(LinkedHashMap(root))

    fun writeTo(file: File) {
        val text = JSON.encodeToString(JsonObject.serializer(), snapshot())
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        // A half-written report is worse than a stale one.
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }

    companion object {
        val JSON = Json {
            prettyPrint = true
            // "Key absent" and "key present but null" mean different things in a device
            // dump, so nulls stay in the output.
            explicitNulls = true
        }
    }
}

/** Formats an arbitrary value for the report without ever throwing on an unexpected type. */
internal fun jsonOf(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is JsonElement -> value
    is Boolean -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is String -> JsonPrimitive(value)
    is Enum<*> -> JsonPrimitive(value.name)
    is IntArray -> JsonArray(value.map { JsonPrimitive(it) })
    is LongArray -> JsonArray(value.map { JsonPrimitive(it) })
    is FloatArray -> JsonArray(value.map { JsonPrimitive(it) })
    is DoubleArray -> JsonArray(value.map { JsonPrimitive(it) })
    is BooleanArray -> JsonArray(value.map { JsonPrimitive(it) })
    is ByteArray -> JsonPrimitive(value.joinToString(",") { "%02x".format(it) })
    is Array<*> -> JsonArray(value.map { jsonOf(it) })
    is Collection<*> -> JsonArray(value.map { jsonOf(it) })
    is Map<*, *> -> JsonObject(value.entries.associate { (k, v) -> k.toString() to jsonOf(v) })
    else -> JsonPrimitive(value.toString())
}
