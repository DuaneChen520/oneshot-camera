package dev.oneshot.probe

import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Drives the probe phases in escalating-risk order and flushes the report after each one,
 * so a hard crash or a wedged camera HAL in a later phase cannot destroy earlier evidence.
 *
 * Order: A (read-only characteristics) → C (device open, no session) → B/D (real bind).
 */
class ProbeRunner(
    context: Context,
    private val onProgress: (String) -> Unit,
) {

    private val appContext = context.applicationContext
    private val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val report = ProbeReport()

    private val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())

    private val errors = mutableListOf<JsonElement>()
    private val completedPhases = mutableListOf<JsonElement>()

    val outFile: File = File(appContext.getExternalFilesDir(null), "probe-latest.json")
    val archivedFile: File = File(appContext.getExternalFilesDir(null), "probe-$stamp.json")

    fun runStaticPhases() {
        report["schemaVersion"] = jsonOf(1)
        report["capturedAtEpochMs"] = jsonOf(System.currentTimeMillis())
        report["runStamp"] = jsonOf(stamp)
        report["phasesCompleted"] = JsonArray(completedPhases)
        report["errors"] = JsonArray(errors)

        onProgress("Phase A: reading CameraCharacteristics (no camera opened)")
        runCatching { PhaseA(cameraManager).run(report) }
            .onFailure { recordFailure("A", it) }
        markCompleted("A")
        flush()

        onProgress("Phase C: prechecking stream combinations against the HAL")
        val ids = runCatching { cameraManager.cameraIdList.toList() }.getOrElse { emptyList() }
        val phaseC = PhaseC(cameraManager)
        val results = try {
            ids.mapNotNull { id ->
                val chars = runCatching { cameraManager.getCameraCharacteristics(id) }.getOrNull()
                    ?: return@mapNotNull null
                onProgress("  Phase C: camera $id")
                runCatching { phaseC.runFor(id, chars) }
                    .onFailure { recordFailure("C:$id", it) }
                    .getOrNull()
            }
        } finally {
            phaseC.close()
        }
        report["sessionPrecheck"] = buildJsonArray { results.forEach { add(it) } }
        markCompleted("C")
        flush()
    }

    fun runBindPhases(owner: LifecycleOwner, outDir: File) {
        val ids = runCatching { cameraManager.cameraIdList.toList() }.getOrElse { emptyList() }
        if (ids.isEmpty()) {
            recordFailure("B/D", IllegalStateException("no camera ids visible"))
            return
        }

        val provider = runCatching {
            ProcessCameraProvider.getInstance(appContext).get(PROVIDER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        }.getOrElse {
            recordFailure("B:provider-init", it)
            return
        }

        val harness = BindHarness(owner)
        try {
            onProgress("Phase B: CameraX capabilities and session-shape bind matrix")
            report["cameraxBindMatrix"] = JsonArray(ids.map { id ->
                runCatching { PhaseB(harness, onProgress).runFor(provider, id) }
                    .onFailure { recordFailure("B:$id", it) }
                    .getOrElse { errorJson(id, it) }
                    .also { harness.releaseAll(provider) }
            })
            markCompleted("B")
            flush()

            onProgress("Phase D: open-gate bind + record attempts")
            report["openGateAttempts"] = JsonArray(ids.map { id ->
                runCatching {
                    PhaseD(appContext, cameraManager, harness, onProgress, outDir, VENDOR_TAG_BUDGET)
                        .runFor(provider, id)
                }.onFailure { recordFailure("D:$id", it) }
                    .getOrElse { errorJson(id, it) }
                    .also { harness.releaseAll(provider) }
            })
            markCompleted("D")
            flush()
        } finally {
            harness.close()
        }
    }

    private fun errorJson(cameraId: String, t: Throwable): JsonElement = buildJsonObject {
        put("cameraId", cameraId)
        put("exception", t.javaClass.name)
        put("message", t.message?.take(600))
    }

    private fun recordFailure(phase: String, t: Throwable) {
        errors += buildJsonObject {
            put("phase", phase)
            put("exception", t.javaClass.name)
            put("message", t.message?.take(1000))
        }
        onProgress("  FAILED in $phase: ${t.javaClass.simpleName}: ${t.message}")
    }

    private fun markCompleted(phase: String) {
        if (completedPhases.none { (it as? JsonPrimitive)?.content == phase }) {
            completedPhases += JsonPrimitive(phase)
        }
    }

    private fun flush() {
        report["phasesCompleted"] = JsonArray(completedPhases)
        report["errors"] = JsonArray(errors)
        runCatching {
            report.writeTo(outFile)
            report.writeTo(archivedFile)
        }.onFailure { onProgress("  could not write report: ${it.message}") }
    }

    private companion object {
        const val PROVIDER_TIMEOUT_SECONDS = 30L

        /** Vendor session tags are HAL-specific and can wedge the camera, so they are capped. */
        const val VENDOR_TAG_BUDGET = 6
    }
}
