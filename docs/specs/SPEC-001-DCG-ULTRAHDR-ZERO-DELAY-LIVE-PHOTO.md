# SPEC-001: Qualcomm DCG Hardware HDR, Full-pipeline Ultra HDR & Zero-Latency Motion Photo Architecture

## Problem Statement

When using the camera application for photography and dynamic moment capture:
1. **Motion Photo Capture Stutters and Fails**: When capturing Live Photos, the viewfinder experiences severe jank and dropped frames. Because recording is started synchronously with still capture, cold-start encoder latency causes the video to miss the decisive moment before the shutter click. Furthermore, because the static alignment timestamp in metadata is hardcoded to zero, system galleries (such as Xiaomi HyperOS Gallery and Google Photos) experience stuttering, jump cuts, or frozen playback.
2. **Still Images Deviate Drastically from Human Vision**: Still captures look washed out, with blown-out highlights and crushed shadows, presenting a severe visual disconnect from the vibrant 10-bit HLG HDR live preview.
3. **Qualcomm DCG HDR is an Unconnected Phantom Switch**: While the UI surfaces a "DCG HDR" toggle based on hardware detection, the camera session pipeline does not actually dispatch the vendor session parameter to the Camera HAL. The switch is completely cosmetic.
4. **Storage Format and Compatibility Ambiguity**: There is uncertainty around dynamic image storage containers (JPEG vs HEIC) and how cross-ecosystem Live Photos function, creating a need for an authoritative, lossless, and universally compatible single-file storage standard.

## Solution

1. **Zero-Latency Circular Motion Photo Pipeline**: Implement a hardware-encoded circular pre-roll buffer running in memory. The camera continuously buffers the rolling past dynamic frames at negligible memory overhead (~3-6 MB). On shutter press, the full-sensor still photo is captured in parallel while the post-roll snippet completes. The two streams are muxed and embedded directly into the primary image container with precise microsecond timestamp alignment, supporting user-customizable durations (1.5s, 3.0s, and 5.0s ceiling).
2. **Full-Pipeline Ultra HDR Photography**: Provide an explicit user toggle for Ultra HDR (ISO 21496-1) that defaults to enabled on supported devices. When enabled, the capture pipeline generates full dynamic range images with embedded gainmaps that ignite true physical peak brightness on HDR OLED displays, eliminating the visual gap between viewfinder and final output.
3. **Hardwired Qualcomm DCG Vendor Session Integration**: Wire the vendor tag (`org.codeaurora.qcamera3.sessionParameters.EnableHDRDCGMode`) directly into the Camera HAL session parameters and capture requests, ensuring the physical dual conversion gain mode toggles in real hardware.
4. **Authoritative Single-File Ultra HDR JPEG + HEVC Storage**: Standardize Motion Photos on single-file Ultra HDR JPEG containers carrying hardware-encoded H.265 (HEVC) micro-videos with ISO 21496-1 and Adobe XMP directory metadata. This guarantees 100% lossless dynamic range, full backward compatibility, and seamless long-press playback across Xiaomi HyperOS and Google Photos.

## User Stories

1. As a mobile photographer, I want the camera viewfinder to remain at full frame rate when taking a Live Photo, so that I never lose track of a moving subject.
2. As a candid street photographer, I want the Live Photo to record the moments *before* I press the shutter button, so that I capture spontaneous smiles and sudden gestures that happened prior to my physical reaction.
3. As a user reviewing my photos in Xiaomi Gallery, I want to long-press any Live Photo and have it play instantly without a black frame, jump cut, or stutter, so that the experience feels magical and continuous.
4. As an everyday user, I want my captured still photos to match the brilliant dynamic range, deep shadows, and luminous highlights I see on screen in the viewfinder, so that I do not feel disappointed by dull or washed-out results.
5. As an enthusiast photographer, I want to toggle Qualcomm DCG HDR on and off, so that I can exploit physical dual conversion gain for high dynamic scenes without software ghosting artifacts.
6. As a power-conscious user, I want the ability to turn off Live Photo mode completely, so that the camera does not run background hardware encoding when I only need single still snapshots.
7. As a mobile filmmaker, I want to configure the motion duration between compact (1.5s), standard (3.0s), and extended (5.0s), so that I can balance storage consumption against action duration.
8. As a social media user, I want to send my captured photos over standard messaging apps without them breaking, knowing they render as crisp standard photos on older screens while glowing with HDR vibrancy on modern phones.
9. As a gallery user, I want my Live Photos stored as a single standalone file rather than paired loose files, so that transferring or backing up photos never leaves behind orphaned video clips.
10. As a pro user, I want manual control over the Ultra HDR feature in quick settings, so that I can produce standard SDR images when targeting legacy web services or stringent upload limits.
11. As a mobile shooter in low light, I want dual conversion gain hardware activation to suppress analog read noise, so that indoor and night shots maintain clean shadow detail.
12. As a photographer capturing high-speed action, I want the post-shutter capture latency to be minimal, so that I can trigger subsequent shots in rapid succession.

## Implementation Decisions

### 1. Zero-Latency Pre-Roll Dynamic Buffer Architecture
- **In-Memory Rolling Compressed Stream**: The motion capture engine maintains a continuous hardware-encoded H.265 (HEVC) elementary stream in an in-memory ring buffer using a dedicated background encoder surface.
- **Timestamp-Aligned Muxing**: When the shutter fires at $T_0$, the still capture completes independently. The buffer retains frames from $[T_0 - T_{\text{pre}}, T_0]$ (starting cleanly from the nearest preceding IDR keyframe) and collects ongoing frames until $T_0 + T_{\text{post}}$.
- **Decoupled Concurrency**: Static capture requests and continuous video encoding run on distinct Camera2 surfaces, eliminating buffer starvation and pipeline stalling.
- **Duration Presets Schema**:
  ```text
  MotionDurationMode:
    - COMPACT:  Pre 0.8s + Post 0.7s (Total 1.5s)
    - STANDARD: Pre 1.5s + Post 1.5s (Total 3.0s, Default)
    - EXTENDED: Pre 2.5s + Post 2.5s (Total 5.0s, Hard Ceiling)
  ```

### 2. Full-Pipeline Ultra HDR (ISO 21496-1) Integration
- **Output Format Modernization**: The static capture use case selects `OUTPUT_FORMAT_JPEG_ULTRA_HDR` on API 34+ when the setting is enabled.
- **User Preference Sovereignty**: Ultra HDR remains an explicit toggle in settings and quick drawer, defaulting to enabled on capable devices and degrading gracefully to standard JPEG when disabled or on unsupported hardware.
- **Elimination of Lossy Transcoding**: Remove intermediate JPEG-to-Bitmap-to-HEIF conversion routines. Still captures output directly to final compressed streams without software decodes.

### 3. Hardware DCG Vendor Tag Session Pipeline
- **Session Parameter Injection**: Inject `org.codeaurora.qcamera3.sessionParameters.EnableHDRDCGMode` (tag `0x80200019`) into the underlying Camera2 session configuration upon binding.
- **Dynamic Rebind on State Change**: When the user toggles DCG HDR in the UI, trigger an in-flight session reconfiguration ensuring the hardware sensor transitions between single-conversion and dual-conversion readouts.
- **Capability Guard**: Retain hardware detection rules that hide the control on front-facing cameras or non-Qualcomm hardware.

### 4. Container & Metadata Integrity (MotionPhotoWriter)
- **Multi-APP Marker Safe Insertion**: Update the container builder to inspect the entire JPEG header, skipping all existing APP markers (APP1 Exif, APP1 Gainmap XMP, APP2 MPF) before injecting Motion Photo metadata, preventing corruption of the Ultra HDR gainmap.
- **Microsecond Timestamp Sync**: Calculate and write `GCamera:MicroVideoPresentationTimestampUs` corresponding exactly to $T_{\text{pre}} \times 1,000,000\,\mu\text{s}$ relative to the start of the embedded MP4 stream.
- **MP4 Moov Fast-Start**: Ensure the embedded MP4 puts its index metadata (moov atom) before the media data (mdat atom), allowing gallery decoders to initiate playback with zero seek latency.

## Testing Decisions

### Testing Philosophy
Tests must strictly verify observable external behavior and data contracts, avoiding coupling to internal private methods. A good test asserts that given a specific hardware capability and user setting, the resulting output container is standard-compliant and the camera session is configured correctly.

### Modules & Seams Under Test
1. **Container & Metadata Seam (`MotionPhotoWriter`)**:
   - Provide raw JPEG buffers with synthetic multi-APP headers (including Ultra HDR gainmap markers) and an MP4 stream.
   - Assert that the output file remains a valid JPEG, preserves all original APP markers, injects the motion photo XMP block, appends the video payload, and writes the correct non-zero presentation timestamp.
2. **Capability & Settings Seam (`DeviceCapabilityDegrader` & `ParameterStore`)**:
   - Verify that DCG HDR and Ultra HDR state changes serialize and deserialize accurately.
   - Assert that non-Qualcomm profiles safely disable DCG, while supported platforms allow full state retention.
   - Test duration preset math (ensuring 1.5s, 3.0s, and 5.0s split into exact pre- and post-roll millisecond targets).
3. **Session Parameter Generation Seam (`CameraController`)**:
   - Verify that when DCG is toggled, the generated Camera2 interop options include key `0x80200019` with integer value 1 or 0.

### Prior Art in Codebase
- `MotionPhotoWriterTest.kt`: Unit tests verifying dual XMP injection and file length bounds.
- `DeviceCapabilityDegraderTest.kt`: Existing tests verifying graceful fallback across hardware tiers.
- `CameraSettingsTest.kt`: Unit tests verifying serialization of user preferences.

## Out of Scope

1. **Privileged Xiaomi AISP Binder Invocations**: No attempts to bind system-private `com.xiaomi.cameramind` interfaces or bypass Android SELinux / signature permissions.
2. **Single-file HEIC Dynamic Photos**: Android system galleries do not support embedded MP4 inside HEIF containers; HEIC Live Photos are strictly out of scope.
3. **Durations Exceeding 5.0 Seconds**: Pre-roll buffers longer than 5.0 seconds are excluded due to battery drain, thermal throttling, and excessive file size (>25MB).
4. **RAW / DNG Motion Photos**: Live dynamic photos apply exclusively to compressed delivery formats.

## Further Notes

- Real-device validation has been confirmed on Xiaomi 17 Pro (`SM8850`, Android 17, API 37) with verified HAL support for format `4101` (`JPEG_R`) and vendor session parameter `0x80200019`.
- The background pre-roll encoder should pause when the app is placed in the background or when the screen dims, preserving device thermals and battery life.
