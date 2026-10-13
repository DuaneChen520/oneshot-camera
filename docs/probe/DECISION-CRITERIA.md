# M0 ADR decision criteria (pre-registered)

These rules are written **before** the probe has run on the Xiaomi 17 Pro. The point is that
the call gets made by the data, not by whatever is most convenient after we see it. Each ADR
must cite the JSON path that satisfied or failed its rule.

Report path: `docs/probe/probe-latest.json` (via `./tools/dump-probe.sh`).

---

## ADR-001 — open gate

**Question:** can a third-party app get a full-sensor-aspect video readout on this device?

**Corrected premise.** This was originally framed as "CameraX cannot select an arbitrary video
size, so open gate needs the `@RestrictTo` door". That framing was wrong. Verified from
`android-36/android.jar` and `Recorder.java:4305` javadoc: **`Recorder.Builder.setAspectRatio(
AspectRatio.RATIO_4_3)` is public API** and is documented to respect aspect *and* quality
simultaneously ("if the aspect ratio is 4:3 and the preferred quality is HD, then a HD quality
resolution with 4:3 aspect ratio such as 1280x960 or 960x720 will be used… no other aspect
ratios (such as 16:9) will be used"). On a sensor whose active array is 4:3 — essentially every
phone sensor, including the 17 Pro's main — a 4:3 video stream *is* the un-letterboxed full
gate. So the open-gate question is mostly a **device capability question, not an API question**.

Candidate stream = a `PRIVATE` size whose aspect ratio is within 2% of
`SENSOR_INFO_ACTIVE_ARRAY_SIZE`'s aspect ratio, with
`getOutputMinFrameDuration(PRIVATE, size) <= 33_333_333` ns, not listed in
`getHighResolutionOutputSizes` (that method means stills-only by contract). Cross-check against
the AOSP promise tables (`SCALER_MANDATORY_{STREAM,USE_CASE,TEN_BIT_OUTPUT,…}_STREAM_COMBINATIONS`
→ `MandatoryStreamInformation.getAvailableSizes()`), which are stronger evidence than
frame-duration inference.

Evidence: `cameraIds[].streamConfig.openGateCandidates.ratio4ThreeVideoCapable[]`,
`.largestRatio4Three`, `.fullSensorAspectVideoCapable[]`, `.aspectHistogram`,
`cameraIds[].sensor.activeArrayAspectLabel`, `sessionPrecheck[].results[]` labelled `OPENGATE-*`,
phase B `cameraxBindMatrix[]` (per cameraId: `.videoCapabilities`, `.bindMatrix[]` with
`isSessionConfigSupported` / `bindError` / `negotiated[]`), and phase D `openGateAttempts[]`
(`.attempts[]` split into **D1 / D2 / D3** tiers, each carrying `media.tracks[]` read back out of
the recorded clip).

Ground truth for "did we actually get a 4:3 stream" is the recorded file's `width`/`height` keys,
not our intention: CameraX chooses the encoder surface itself, and only the container proves what
was written. The preview `SurfaceRequest` resolution is recorded as corroboration.

| Rule | Outcome |
|---|---|
| **D1** — `Recorder.Builder.setAspectRatio(RATIO_4_3)` + `QualitySelector`, with `VideoCapture.Builder.setDynamicRange(DynamicRange.HLG_10_BIT)`, binds and the recorded clip is 4:3 within 2% of the active array, sustaining ≥29.7 fps over 30 s | **GO on the public path.** This is the primary outcome we expect. Ship a "全画幅 4:3" switch labelled with the real size (e.g. `1920×1440 全画幅`). No RestrictTo, no raw Camera2 |
| **D1** binds 4:3 in SDR but not with HLG10 | **PARTIAL.** Offer 4:3 and HLG10 as independent switches and surface that the combination is unavailable. Note it prominently: the PM's daily recipe is exactly 4:3 + HLG10 + 30fps |
| **D1** fails but **D3** (`setCustomOrderedResolutions`, `@RestrictTo`) delivers a *larger* 4:3 stream than the Quality ladder | **GO with the restriction.** Record in ADR-005 that the only path to full-size 4:3 is an unsupported API, isolate it in one file, and decide whether we want to carry it at all |
| No 4:3 video-capable stream exists in the stream map at all | **NO-GO.** Cancel M3.5 and M3.9. Offer `widestVideoCapableOverall` truthfully labelled with its aspect. Record as a device limitation, not an app bug |

Also decide here, and record the reasoning:
- `SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION` is **not** the open-gate mechanism. Its AOSP guaranteed
  combinations are stills-only, and `SCALER_MANDATORY_MAXIMUM_RESOLUTION_STREAM_COMBINATIONS`
  should be read to confirm that for this device rather than asserted.
- Whether `SENSOR_PIXEL_MODE` / any binning or sensor-mode tag appears in `getAvailableSessionKeys()`.
  Only session-listed keys can reach `setSessionParameter`; everything else is silently dropped.
- `VideoCapabilities.isQualitySupported(Quality, DynamicRange)` is **aspect-agnostic**, so it cannot
  answer "is 4:3 + FHD + HLG10 available". The UI's decision must come from `OpenGateEvidence`
  derived from the stream map + promise tables, not from `VideoCapabilities`.

### API facts pinned by `javap` on the alpha03 binaries (2026-10-10)

These were measured against the actual AARs/`android.jar`, because the natural names were wrong:

- `DynamicRange` is set on **`VideoCapture.Builder.setDynamicRange(DynamicRange)`**.
  `Recorder.Builder` has **no** `setDynamicRange`; its public knobs are `setAspectRatio`,
  `setQualitySelector`, `setOutputFormat`, `setVideoMimeType`, `setTargetVideoEncodingBitRate`,
  `setVideoCapabilitiesSource`, `setMuxerFactory`, `setAudioSource`, `setRequiredFreeStorageBytes`.
- `PendingRecording.start(Executor, Consumer<VideoRecordEvent>)` returns **`Recording`**
  (`pause/resume/stop/mute/close`). There is no `VideoRecordControl` class in these binaries.
- `CameraInfo.isSessionConfigSupported(SessionConfig)` is public and callable before binding;
  `ProcessCameraProvider` has a `bindToLifecycle(owner, selector, SessionConfig)` overload plus
  `isBound(SessionConfig)` / `unbind(SessionConfig)` / `getCameraInfo(selector, sessionConfig)`.
- `Camera2Interop.forSessionConfig { }` yields `InteropConfigurator<SessionConfig.Builder>`, so
  `SessionConfig.Builder.setInterop(...)` accepts it directly. Use-case interop goes on the
  **builder** (`VideoCapture.Builder.setInterop`), not on the `UseCase` instance.
- `OutputConfigurationInterop` in alpha03 exposes exactly six setters
  (`setPhysicalCameraId`, `setStreamUseCase`, `setMirrorMode`, `setTimestampBase`,
  `setDynamicRangeProfile`, `setSurfaceGroupId`) — **no** `setSensorPixelModesUsed`, so sensor
  pixel mode can only be a session parameter, never a per-stream declaration, on this version.
- `CaptureRequest.Key.getType()` is **not public API** (hidden). A vendor tag's value type cannot
  be queried, so phase D *probes* it by trying Integer/Long/Float/Boolean and records which one
  `setSessionParameter` accepted, alongside whether the bind survived it.
- `androidx.camera:camera-view` is the **only** consumer of `androidx.appcompat` in this graph;
  `emoji2` and `androidx.window` come from Compose instead. Dropping camera-view took 212 KB of
  `resources.arsc` out of the probe APK (264,408 → 52,020).
- `com.google.guava:guava` (2,918,070 B jar) is required by `androidx.media3:media3-common` under
  `camera-video`, and the `listenablefuture:9999.0-empty` stub contains **no classes at all**, so
  there is no Guava to "trim": R8 already keeps only what is called (zero Guava classes survived
  in the placeholder build).
- `Recorder.getMediaCapabilities(CameraInfo, source)` and `QualitySelector.getSupportedQualities(
  CameraInfo)` are public — phase B reads both, with `VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE`
  and `..._CODEC_CAPABILITIES` compared side by side.


---

## ADR-002 — zoom strategy

**Question:** seamless multi-lens zoom, or a masked cut?

Evidence: `cameraIds[].requestAvailableCapabilities` containing `LOGICAL_MULTI_CAMERA`,
plus phase B's `isLogicalMultiCameraSupported()` and the `zoomSweep` intrinsic-ratio trace.

| Rule | Outcome |
|---|---|
| Back camera reports `LOGICAL_MULTI_CAMERA` **and** the zoom sweep shows `getActiveIntrinsicZoomRatio()` changing while `getZoomRatio()` moves continuously | **`LogicalMultiCameraZoom`.** One `setZoomRatio` sweep; HAL blends. This is the only genuinely seamless outcome |
| No `LOGICAL_MULTI_CAMERA`, but physical IDs are enumerable | **`LensLadderZoom`.** Build the ladder from `getCameraFilterFromCameraId`. Count how many of the three boundaries fall inside a ≤1.6× digital crop of the wider lens — those become crop-bridged (effectively seamless); the remainder are masked cuts (~180-250 ms freeze + cross-fade) |
| No `LOGICAL_MULTI_CAMERA` **and** physical lenses are not independently selectable | **Ladder on front/back only.** Report the degradation to the PM before M2 starts, because requirement 10 then cannot be met at all |

Whatever the outcome, ADR-002 must state the expected boundary count and freeze duration in
numbers. "Smooth" is not a deliverable; "≤250 ms per boundary, 2 of 3 boundaries crop-bridged" is.

---

## ADR-003 — white balance backend

**Question:** native CCT/tint pad, or RGGB gain pad?

Evidence: `cameraIds[].colorCorrection.supportsCctMode` (i.e.
`COLOR_CORRECTION_AVAILABLE_MODES` contains `3`), `colorCorrection.colorTemperatureRange`
non-null, and phase B's `wbRoundTrip`.

| Rule | Outcome |
|---|---|
| `supportsCctMode == true` **and** range non-null **and** round-trip shows the requested Kelvin is reflected in `CaptureResult` | **`CctTintBackend`.** The ABGM pad maps directly onto `COLOR_CORRECTION_COLOR_TEMPERATURE` + `COLOR_CORRECTION_COLOR_TINT`. Note the API is `@FlaggedApi` on API 36, so the runtime guard stays even when the probe passes |
| Otherwise | **`RggbGainBackend`.** `CONTROL_AWB_MODE=OFF` + `COLOR_CORRECTION_MODE_TRANSFORM_MATRIX` + `COLOR_CORRECTION_GAINS`, with the pad centre calibrated to the native AWB gains read from `wbRoundTrip` |
| Gains round-trip but do not change the image | **Grey out manual WB entirely.** Capability-driven UI already supports per-control visibility; a control that does nothing is worse than an absent control |

---

## ADR-004 — HLG10 + custom CameraEffect

**Question:** does the HAL accept a custom `CameraEffect`/`SurfaceProcessor` on an HLG10 stream?

Evidence: phase B `camerax.effects` — effect bound at `PREVIEW` and at `PREVIEW|VIDEO_CAPTURE`,
each under SDR and HLG10, with `SurfaceRequest.getDynamicRange()` recorded.

| Rule | Outcome |
|---|---|
| Effect accepted at `PREVIEW` under both SDR and HLG10, dynamic range reported correctly | **M2 proceeds as designed** (preview-only LUT + overlays in our own GL chain) |
| Accepted at `PREVIEW` only under SDR | **M2 proceeds with a constraint:** enabling a LUT forces SDR preview. Surface it as an explicit user-facing trade, not a silent downgrade |
| Rejected at `PREVIEW` under HLG10 | **M2 fallback:** draw LUT/peaking in a GL layer *above* the viewfinder instead of in the pipeline. Costs latency; v1 is preview-only anyway so M5's burn-in path is unaffected — but M5 must then be re-scoped |
| Rejected at `VIDEO_CAPTURE` | **No impact on M2** (v1 is preview-only). Record it as M5's opening problem |

Also: `Recorder.getVideoCapabilities` must show FHD+HLG10 at 30 fps or the PM's daily recipe
cannot exist at all — that finding blocks M3 regardless of effects.

---

## ADR-005 — vendor tag inventory

**Question:** which vendor tags does this device actually honour?

Evidence: `cameraIds[].vendorTags.classification.{sessionAndInteresting,requestAndInteresting}`,
`cameraIds[].vendorTags.namespacesPresent`, phase B `measure.vendorTagRoundTrip[]`.

Rules:
- A tag is **CONFIRMED** only if setting it changes the read-back value in `CaptureResult`, or
  changes pixels (verifiable by a frame diff). A tag that merely appears in
  `getAvailableCaptureRequestKeys()` is a **candidate**, nothing more.
- A **session** tag is unusable unless it appears in `getAvailableSessionKeys()` — AOSP
  silently drops everything else, which is exactly the failure mode that would make us believe
  LOG was on when it was not.
- If no LOG/gamma/tone-natured tag survives confirmation: **the LOG control is absent from the
  UI on this device.** That is the agreed product decision — no app-side fake log curve.
- Never code against a tag name that was not seen in an on-device dump. The Qualcomm tag names
  in the research notes are from a 2017 msm8998 HAL; this device runs CamX/CHI.

**Also recorded in this ADR:** the single `@RestrictTo(LIBRARY_GROUP)` dependency —
`VideoCapture.Builder.setCustomOrderedResolutions(List<Size>)`. Cite
`VideoCapture.java:1568-1593` (CameraX early-returns so our list is not overwritten by
`QualitySelector`), the `@SuppressLint("RestrictedApi")` site
(`interop/OpenGateResolutionOverride.kt`), and the documented 1.6.2 downgrade that drops open
gate but keeps everything else.
