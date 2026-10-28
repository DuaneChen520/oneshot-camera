# SPEC-003: Still Resolution Decoupling, Motion Photo Pre-Roll Engine & Refined Top Bar Iconography

## 1. Context & Motivation

In previous iterations, two functional gaps and one design defect were identified during field validation:

1. **Coupled Still/Video Resolution**:
   - `ImageCapture` was hardcoded to `ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY` without independent user configuration.
   - The user requested explicit, independent selection between **50MP Ultra High-Resolution** (sensor unbinned / maximum resolving power) and **12MP Standard Quad-Bayer Fusion** (maximum dynamic range, lowest noise, fastest multi-frame processing), completely decoupled from the video stream resolution (1080p, 2K, 4K, 4096 Open Gate).

2. **Missing Pre-Roll in Motion Photo (Live Photo)**:
   - When the Motion Photo shutter was tapped at $T_0$, recording only started *at* $T_0$. The resulting video snippet captured only post-shutter movement, completely missing the crucial rolling motion *prior* to the shutter press ($T_0 - 1.5\text{s}$).
   - Genuine Live Photo / Motion Photo requires a continuous rolling pre-roll buffer in the background, so that tapping the shutter captures the window $[T_0 - 1.5\text{s}, T_0 + 1.5\text{s}]$ with the still image presentation timestamp placed squarely at 1.5s.

3. **Messy Dynamic-Width Top Bar Text Boxes**:
   - The top control bar used `TactilePillButton` with changing text labels ("4:3 OPEN" vs "16:9 SCOPE", "10-bit HLG" vs "SDR", "24 FPS" vs "120 FPS").
   - Whenever a button was toggled, its text length changed, causing the surrounding elements to shift horizontally and creating layout jitter.
   - The user instructed: "用工整的图标代替原来散乱、尺寸跟随切换变化的文字框" (Replace the messy, dimension-shifting text boxes with neat, uniform icons).

---

## 2. Technical Architecture & Decisions

### 2.1 Still Resolution Decoupling (50MP vs 12MP)

We introduce `StillResolutionPreset` in `CameraParameters.kt`:
```kotlin
enum class StillResolutionPreset(val label: String, val shortLabel: String, val approxMegaPixels: String) {
    RES_50MP("50MP 全像素超清", "50MP", "8192×6144"),
    RES_12MP("12MP 四合一融合", "12MP", "4096×3072"),
}
```

- **50MP (`RES_50MP`)**: Uses `ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE` and `ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY`. Forces the camera HAL to output sensor-max resolution.
- **12MP (`RES_12MP`)**: Uses `ResolutionSelector.PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION` with `ResolutionStrategy(Size(4096, 3072), FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)`. Uses 4-in-1 pixel binning for superior low-light SNR and fastest ISP processing.
- **Independence**: `CameraController.rebindCamera()` applies `stillResolution` exclusively to `ImageCapture.Builder`, leaving `VideoCapture`'s `QualitySelector` completely untouched.
- **Settings UI**: Added to Q-Menu under `FORMAT` (格式编码) with tactile selector cards.

---

### 2.2 Motion Photo Pre-Roll Engine (Rolling Segment Buffer + Fast Remux)

To achieve reliable pre-roll without conflicting with CameraX's surface allocations or causing Qualcomm HAL crashes:
1. **Top Bar Live Switch (`motionPhotoLiveEnabled: Boolean`)**:
   - When enabled, the app maintains an active rolling video recording in cache (`live_roll_xxx.mp4`) using CameraX `activeVideoCapture`.
   - The rolling segment rotates smoothly every 6 seconds to bound disk and memory footprint.
2. **Shutter Interception**:
   - When the user taps Motion Photo at $T_{\text{click}}$:
     - Record `pivotMs = currentDurationMs`.
     - Concurrently trigger `imageCapture.takePicture()` for the pristine still frame.
     - Allow the video recording to continue until $T_{\text{target}} = \text{pivotMs} + \text{postRollMs}$ (e.g. +1.5s).
     - Stop the recording segment.
3. **Lossless Video Trimming via `VideoTrimmer`**:
   - Uses Android `MediaExtractor` and `MediaMuxer` to remux the range $[ \max(0, \text{pivotMs} - \text{preRollMs}), \text{targetMs} ]$ at keyframe sync points without video transcoding (takes ~30-50ms).
   - Accurately computes `presentationTimestampUs = (\text{pivotMs} - \text{trimmedBaseMs}) * 1000L` (e.g. 1,500,000 us).
4. **Packaging**:
   - `MotionPhotoWriter.writeMotionPhotoStream` embeds the trimmed MP4 and dual XMP metadata tags (`GCamera:MotionPhoto="1"`, `GCamera:MicroVideo="1"`, `GCamera:MicroVideoPresentationTimestampUs="$presentationTimestampUs"`) into the still JPEG.
   - The rolling buffer immediately re-arms for the next capture.

---

### 2.3 Refined Uniform Fixed-Grid Top Bar Iconography

Replace the variable-width text pills with a **fixed-dimension, uniform 6-slot industrial tactile cell matrix**:
- Every cell has identical dimensions (`width = 46.dp`, `height = 36.dp`) and rounded corners (`8.dp`), completely eliminating layout shifts.
- The 6 slots are:
  1. **Aspect Ratio (`ASPECT`)**: `AspectRatioOpenGate` vector icon + compact static sub-label ("4:3" / "16:9").
  2. **Dynamic Range (`DR`)**: `DynamicRangeHlg` vector icon + status diode (Amber for 10-bit HLG, dim for SDR).
  3. **Frame Rate (`FPS`)**: `FpsShutterAngle` vector icon + fixed number ("24" / "30" / "60" / "120").
  4. **Dual Conversion Gain (`DCG`)**: `DcgDualGain` vector icon + green status diode (On/Off).
  5. **Motion Photo Pre-Roll (`LIVE`)**: `MotionPhotoLive` concentric ring icon + Amber diode + "LIVE" tag (Toggles pre-roll buffer on/off).
  6. **Q-Menu Matrix (`Q`)**: `QMenuMatrix` icon with industrial milled border for opening drawer.

---

## 3. Test Plan & Acceptance Criteria

1. **Unit Tests**:
   - `StillResolutionPresetTest`: Verifies 50MP vs 12MP resolution selector generation and fallback rules.
   - `VideoTrimmerTest`: Verifies MP4 sample extraction, presentation timestamp calculation, and boundary clamping.
   - `ParameterStoreTest`: Verifies setting and persistence of `stillResolution` and `motionPhotoLiveEnabled`.
2. **Pipeline Parameter Isolation**:
   - Confirm changing `stillResolution` does not change video bitrate or video resolution.
   - Confirm changing video quality does not alter still capture resolution.
3. **Real Device Verification on Xiaomi 17 Pro**:
   - Capture a 50MP still photo and verify image dimensions are ~8192x6144 (or sensor max).
   - Capture a 12MP still photo and verify image dimensions are ~4096x3072.
   - Turn ON the Top Bar LIVE icon; tap Motion Photo button; verify the captured motion photo contains movements *prior* to the button tap.
   - Verify top control bar maintains strictly static geometry when toggling options (zero layout jitter).
