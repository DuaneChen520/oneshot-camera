# SPEC-002: Still Capture Pipeline Isolation & Hardware Image Quality Restoration

## Problem Statement

Static photos taken by OneShot are severely blurry and soft compared to the device's stock camera under identical conditions. Side-by-side Exif analysis of real test captures from a Xiaomi 17 Pro reveals:

- **OneShot**: shutter speed 1/33s (0.03s), no hardware ISP sharpening, no AF pre-capture lock.
- **Stock Xiaomi Camera**: shutter speed 1/1066s (~0.001s), full Qualcomm ISP pipeline engaged.

The 32× slower shutter speed causes unavoidable hand-shake motion blur on the large 1/1.3" sensor at f/1.67. The root cause is an **architectural parameter isolation failure**: a `[30, 30]` FPS lock intended solely for video recording was applied to the shared camera session via `Camera2CameraControl`, polluting the still capture AE engine and capping exposure time at 1/30s minimum.

Additionally, hardware image quality parameters (`EDGE_MODE_HIGH_QUALITY`, `NOISE_REDUCTION_MODE_HIGH_QUALITY`, `COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY`) were applied only to the repeating preview request via `Camera2CameraControl`, but **never injected into the `ImageCapture` builder**. The single-shot capture request therefore ran with HAL defaults (FAST/balanced mode), missing edge sharpening, advanced denoising, and chromatic aberration correction.

Finally, `takeStillPhoto()` fires `ImageCapture.takePicture()` without an AF/AE pre-capture convergence trigger. On a large-aperture sensor with razor-thin depth of field, this risks capturing mid-focus-scan frames.

## Solution

Surgically decouple the three camera pipelines (Preview, ImageCapture, VideoCapture) so that each carries only its own parameters, eliminating cross-contamination. Specifically:

1. **Remove the FPS lock from the shared session-level `Camera2CameraControl`**, and move it exclusively into the `VideoCapture` builder via `Camera2Interop.Extender`. This frees the AE engine during still capture to select high-speed shutter speeds (1/200s–1/2000s) that freeze hand tremor.
2. **Inject hardware ISP high-quality parameters directly into the `ImageCapture` builder** via `Camera2Interop.Extender`, ensuring the single-shot capture request explicitly demands `EDGE_MODE_HIGH_QUALITY`, `NOISE_REDUCTION_MODE_HIGH_QUALITY`, `COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY`, `SHADING_MODE_HIGH_QUALITY`, and `JPEG_QUALITY = 100`.
3. **Implement AF/AE pre-capture convergence** before releasing the still shutter, ensuring focus is locked and exposure is metered before the frame is captured.

## User Stories

1. As a user taking handheld photos indoors, I want my photos to be crisp and shake-free, so that I don't see motion blur from hand tremor.
2. As a user comparing OneShot to the stock camera, I want equivalent sharpness and detail in my still photos, so that I have no reason to switch back to the stock app.
3. As a user shooting close-up subjects, I want the camera to confirm focus lock before capturing, so that the shallow depth of field doesn't produce out-of-focus images.
4. As a user shooting in bright daylight, I want the camera to use fast shutter speeds (1/500s+), so that moving subjects are frozen sharp.
5. As a user recording video, I want smooth 30fps playback without dropped frames, so that the video FPS lock is preserved for the recording pipeline only.
6. As a user taking a Live Photo (motion photo), I want the still cover frame to be razor-sharp while the video portion remains smooth at 30fps, so that both components serve their distinct purposes.
7. As a user shooting in low light, I want the hardware ISP's multi-frame noise reduction to engage during still capture, so that shadow areas remain clean rather than noisy.
8. As a user viewing photos at 100% crop, I want hardware edge enhancement applied to the capture, so that fine details (text, fabric, hair) are resolved crisply.
9. As a user shooting against bright windows, I want chromatic aberration correction applied during capture, so that color fringing at high-contrast edges is suppressed.
10. As a power user, I want the JPEG output at maximum quality (100), so that no compression artifacts degrade the resolved detail from the ISP pipeline.
11. As a user rapidly tapping the shutter, I want successive captures to each independently converge AF/AE, so that burst-like shooting doesn't produce progressively worse frames.
12. As a user shooting video while taking a simultaneous still (video-snapshot), I want the still frame to benefit from high-quality ISP processing even though the video stream runs in FAST mode, so that the snapshot is noticeably sharper than a video frame extract.

## Implementation Decisions

### 1. Pipeline Parameter Isolation Architecture

The camera session binds three use cases simultaneously: `Preview`, `ImageCapture`, and `VideoCapture`. Parameters must be routed to the correct pipeline:

| Parameter | Preview (repeating) | ImageCapture (single-shot) | VideoCapture (recording) |
|---|---|---|---|
| `CONTROL_AE_TARGET_FPS_RANGE [30, 30]` | ❌ Remove | ❌ Never set | ✅ Via `Camera2Interop.Extender(videoCaptureBuilder)` |
| `EDGE_MODE_HIGH_QUALITY` | Keep in repeating | ✅ Via `Camera2Interop.Extender(imageCaptureBuilder)` | Not needed |
| `NOISE_REDUCTION_MODE_HIGH_QUALITY` | Keep in repeating | ✅ Via `Camera2Interop.Extender(imageCaptureBuilder)` | Not needed |
| `COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY` | Keep in repeating | ✅ Via `Camera2Interop.Extender(imageCaptureBuilder)` | Not needed |
| `SHADING_MODE_HIGH_QUALITY` | Keep in repeating | ✅ Via `Camera2Interop.Extender(imageCaptureBuilder)` | Not needed |
| `JPEG_QUALITY = 100` | N/A | ✅ Via `Camera2Interop.Extender(imageCaptureBuilder)` | N/A |
| DCG HDR vendor tag | Session-level (all) | Session-level (all) | Session-level (all) |
| Video stabilization | Preview only | N/A | Via `Camera2Interop.Extender(videoCaptureBuilder)` |

### 2. FPS Lock Migration

- **Remove** from `applyInFlightParameters()`: the `CONTROL_AE_TARGET_FPS_RANGE` option currently set via `Camera2CameraControl.setCaptureRequestOptions()` (which applies session-wide to all streams).
- **Add** to `rebindCamera()`: inject `CONTROL_AE_TARGET_FPS_RANGE` only into `VideoCapture.Builder` via `Camera2Interop.Extender`. This ensures the recording encoder receives steady 30fps frames while the AE engine remains unconstrained for still capture.

### 3. ImageCapture Builder Enhancement

In `rebindCamera()`, apply `Camera2Interop.Extender(imageCaptureBuilder)` to set:
- `CaptureRequest.EDGE_MODE` → `EDGE_MODE_HIGH_QUALITY`
- `CaptureRequest.NOISE_REDUCTION_MODE` → `NOISE_REDUCTION_MODE_HIGH_QUALITY`
- `CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE` → `COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY`
- `CaptureRequest.SHADING_MODE` → `SHADING_MODE_HIGH_QUALITY`
- `CaptureRequest.JPEG_QUALITY` → `100` (byte)

These are standard Android Camera2 public APIs (API 21+), not vendor extensions.

### 4. AF/AE Pre-Capture Convergence

Before calling `imageCapture.takePicture()` in `takeStillPhoto()`, trigger a focus-and-metering action using `CameraControl.startFocusAndMetering()` with `FLAG_AF | FLAG_AE`, and await its `ListenableFuture` completion. This ensures:
- The phase-detect AF confirms subject lock (critical for f/1.67 shallow DoF).
- The AE engine has converged on optimal exposure (enabling high-speed shutter selection).

If AF is in manual mode (`CONTROL_AF_MODE_OFF`), skip the pre-capture trigger to avoid overriding the user's manual focus distance.

### 5. Mutual Exclusion: Motion Photo vs. Active Recording

Add a guard in `takeMotionPhoto()`: if a long-form video recording is already active (`isRecording == true`), reject the motion photo request and notify the user. This prevents two concurrent consumers (long recording + motion micro-video) from competing for the single `VideoCapture` output, which would cause one to fail silently.

## Testing Decisions

### Testing Philosophy

Tests verify observable external behavior at module boundaries. A good test for this spec asserts: "Given these user settings and this device capability profile, the generated Camera2 parameter set for pipeline X contains exactly these keys with these values, and pipeline Y does NOT contain the conflicting keys."

### Testing Seam: Parameter Generation Extraction

The primary testable seam is **extracting the parameter-building logic into pure functions** that take `CameraSettings` + `DeviceCapabilities` as input and return structured parameter sets (maps or data classes) for each pipeline. This avoids needing real Camera2 hardware in unit tests.

Candidate extraction:
- `StillCaptureParams.build(settings, capabilities) → Map<CaptureRequest.Key, Any>` — returns the exact key-value pairs that should be injected into `ImageCapture`.
- `VideoCaptureParams.build(settings, capabilities) → Map<CaptureRequest.Key, Any>` — returns the key-value pairs for `VideoCapture`.
- `SessionParams.build(settings, capabilities) → Map<CaptureRequest.Key, Any>` — returns shared session-level parameters.

### Modules Under Test

1. **`StillCaptureParams` (new, extracted)**:
   - Assert `EDGE_MODE_HIGH_QUALITY` is always present.
   - Assert `CONTROL_AE_TARGET_FPS_RANGE` is never present.
   - Assert `JPEG_QUALITY` is 100.
   - Assert `NOISE_REDUCTION_MODE_HIGH_QUALITY` is always present.

2. **`VideoCaptureParams` (new, extracted)**:
   - Assert `CONTROL_AE_TARGET_FPS_RANGE` is `[30, 30]` when FPS preset is 30fps.
   - Assert `EDGE_MODE_HIGH_QUALITY` is never present (video uses FAST mode).
   - Assert video stabilization parameters match user setting.

3. **`CameraController` integration behavior**:
   - Assert that `takeStillPhoto()` in auto-focus mode triggers AF/AE convergence before capture (observable via mock/spy on `CameraControl`).
   - Assert that `takeMotionPhoto()` is rejected when `isRecording == true`.

### Prior Art in Codebase

- `DcgHdrConfigTest.kt`: Pure-function unit tests verifying parameter resolution logic — same pattern to follow for `StillCaptureParams` and `VideoCaptureParams`.
- `DeviceCapabilityDegraderTest.kt`: Tests that verify settings sanitization given hardware capabilities — same input/output testing pattern.
- `CameraSettingsTest.kt`: Serialization/deserialization tests for user preference data classes.

## Out of Scope

1. **Custom shutter speed manual control**: This spec addresses automatic exposure restoration only. Manual shutter speed UI is a separate feature.
2. **Multi-frame computational photography (Night Mode, Super Resolution)**: These require Xiaomi's proprietary AISP pipeline which is behind SELinux signature permissions.
3. **Video recording quality tuning**: Video sharpness, bitrate optimization, and encoding quality are not addressed here; the video pipeline parameters remain unchanged.
4. **RAW/DNG capture pipeline**: This spec covers JPEG and Ultra HDR JPEG output only.

## Further Notes

- Real-device A/B comparison data is available in `tools/test_still.jpg` (OneShot, 1/33s) and `tools/stock_photo.jpg` (stock camera, 1/1066s) for regression testing after the fix.
- The `CAPTURE_MODE_MAXIMIZE_QUALITY` flag is already set on `ImageCapture.Builder`, which instructs CameraX to prefer quality over latency. However, this flag alone does not inject the Camera2-level ISP parameters — those must be set explicitly via `Camera2Interop.Extender`.
- Post-fix verification: pull a new test photo from the device, confirm Exif `ExposureTime` is ≥ 1/200s in indoor ambient light, and visually confirm edge sharpness at 100% crop.
