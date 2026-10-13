package dev.oneshot.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "oneshot_camera_prefs")
private val jsonFormat = Json { ignoreUnknownKeys = true }

class ParameterStore(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _settings = MutableStateFlow(CameraSettings())
    val settings: StateFlow<CameraSettings> = _settings.asStateFlow()

    init {
        scope.launch {
            context.dataStore.data.collectLatest { prefs ->
                val aspectName = prefs[KEY_ASPECT] ?: AspectRatioMode.RATIO_4_3.name
                val drName = prefs[KEY_DR] ?: DynamicRangeMode.HLG10.name
                val qualityName = prefs[KEY_QUALITY] ?: QualityPreset.FHD.name
                val lensName = prefs[KEY_LENS] ?: LensFacingMode.BACK.name
                val dcg = prefs[KEY_DCG] ?: true
                val ev = prefs[KEY_EV] ?: 0
                val awb = prefs[KEY_AWB] ?: true
                val cct = prefs[KEY_CCT] ?: 5500
                val tint = prefs[KEY_TINT] ?: 0
                val zoom = prefs[KEY_ZOOM] ?: 1.0f
                val uHdr = prefs[KEY_ULTRA_HDR] ?: true
                val motionDur = prefs[KEY_MOTION_DUR] ?: MotionDurationPreset.DUR_1_5S.name
                val volUp = prefs[KEY_VOL_UP] ?: VolumeKeyAction.STILL_SHUTTER.name
                val volDown = prefs[KEY_VOL_DOWN] ?: VolumeKeyAction.VIDEO_SHUTTER.name
                val autoShutter = prefs[KEY_AUTO_SHUTTER] ?: true
                val shutterNanos = prefs[KEY_SHUTTER_NANOS] ?: (1_000_000_000L / 60L)
                val autoIso = prefs[KEY_AUTO_ISO] ?: true
                val iso = prefs[KEY_ISO] ?: 100
                val fps = prefs[KEY_FPS] ?: FpsPreset.FPS_30.name
                val cs = prefs[KEY_COLOR_SPACE] ?: ColorSpacePreset.REC_2020.name
                val lut = prefs[KEY_LUT] ?: false
                val lutPresetName = prefs[KEY_LUT_PRESET] ?: LutPreset.TEAL_AND_ORANGE.name
                val selectedCustomId = prefs[KEY_SELECTED_CUSTOM_LUT_ID]
                val customLutsRaw = prefs[KEY_CUSTOM_LUTS_JSON] ?: "[]"
                val customLuts = runCatching {
                    jsonFormat.decodeFromString<List<CustomLut>>(customLutsRaw)
                }.getOrDefault(emptyList())
                val lutBurn = prefs[KEY_LUT_BURN] ?: false
                val peaking = prefs[KEY_PEAKING] ?: false
                val zebra = prefs[KEY_ZEBRA] ?: false
                val histModeName = prefs[KEY_HISTOGRAM_MODE] ?: HistogramMode.OFF.name

                _settings.value = CameraSettings(
                    aspectMode = runCatching { AspectRatioMode.valueOf(aspectName) }.getOrDefault(AspectRatioMode.RATIO_4_3),
                    dynamicRange = runCatching { DynamicRangeMode.valueOf(drName) }.getOrDefault(DynamicRangeMode.HLG10),
                    quality = runCatching { QualityPreset.valueOf(qualityName) }.getOrDefault(QualityPreset.FHD),
                    lensFacing = runCatching { LensFacingMode.valueOf(lensName) }.getOrDefault(LensFacingMode.BACK),
                    dcgHdrEnabled = dcg,
                    evIndex = ev,
                    isAutoWb = awb,
                    cctKelvin = cct,
                    tintGm = tint,
                    zoomRatio = zoom,
                    ultraHdrEnabled = uHdr,
                    motionDuration = runCatching { MotionDurationPreset.valueOf(motionDur) }.getOrDefault(MotionDurationPreset.DUR_1_5S),
                    volumeKeyUpAction = runCatching { VolumeKeyAction.valueOf(volUp) }.getOrDefault(VolumeKeyAction.STILL_SHUTTER),
                    volumeKeyDownAction = runCatching { VolumeKeyAction.valueOf(volDown) }.getOrDefault(VolumeKeyAction.VIDEO_SHUTTER),
                    isAutoShutter = autoShutter,
                    shutterSpeedNanos = shutterNanos,
                    isAutoIso = autoIso,
                    isoValue = iso,
                    fpsPreset = runCatching { FpsPreset.valueOf(fps) }.getOrDefault(FpsPreset.FPS_30),
                    colorSpace = runCatching { ColorSpacePreset.valueOf(cs) }.getOrDefault(ColorSpacePreset.REC_2020),
                    lutEnabled = lut,
                    lutPreset = runCatching { LutPreset.valueOf(lutPresetName) }.getOrDefault(LutPreset.TEAL_AND_ORANGE),
                    selectedCustomLutId = selectedCustomId,
                    customLuts = customLuts,
                    lutBurnIn = lutBurn,
                    focusPeakingEnabled = peaking,
                    zebraPatternEnabled = zebra,
                    histogramMode = runCatching { HistogramMode.valueOf(histModeName) }.getOrDefault(HistogramMode.OFF),
                )
            }
        }
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        scope.launch { context.dataStore.edit { it[KEY_ASPECT] = mode.name } }
    }

    fun setDynamicRange(mode: DynamicRangeMode) {
        scope.launch { context.dataStore.edit { it[KEY_DR] = mode.name } }
    }

    fun setQuality(preset: QualityPreset) {
        scope.launch { context.dataStore.edit { it[KEY_QUALITY] = preset.name } }
    }

    fun setLensFacing(facing: LensFacingMode) {
        scope.launch { context.dataStore.edit { it[KEY_LENS] = facing.name } }
    }

    fun setDcgHdr(enabled: Boolean) {
        scope.launch { context.dataStore.edit { it[KEY_DCG] = enabled } }
    }

    fun setEvIndex(index: Int) {
        scope.launch { context.dataStore.edit { it[KEY_EV] = index } }
    }

    fun setAutoWb(auto: Boolean) {
        scope.launch { context.dataStore.edit { it[KEY_AWB] = auto } }
    }

    fun setCctKelvin(kelvin: Int) {
        scope.launch { context.dataStore.edit { it[KEY_CCT] = kelvin } }
    }

    fun setTintGm(tint: Int) {
        scope.launch { context.dataStore.edit { it[KEY_TINT] = tint } }
    }

    fun setZoomRatio(zoom: Float) {
        scope.launch { context.dataStore.edit { it[KEY_ZOOM] = zoom } }
    }

    fun setUltraHdr(enabled: Boolean) {
        scope.launch { context.dataStore.edit { it[KEY_ULTRA_HDR] = enabled } }
    }

    fun setMotionDuration(preset: MotionDurationPreset) {
        scope.launch { context.dataStore.edit { it[KEY_MOTION_DUR] = preset.name } }
    }

    fun setVolumeKeyUpAction(action: VolumeKeyAction) {
        scope.launch { context.dataStore.edit { it[KEY_VOL_UP] = action.name } }
    }

    fun setVolumeKeyDownAction(action: VolumeKeyAction) {
        scope.launch { context.dataStore.edit { it[KEY_VOL_DOWN] = action.name } }
    }

    fun setAutoShutter(auto: Boolean) {
        scope.launch { context.dataStore.edit { it[KEY_AUTO_SHUTTER] = auto } }
    }

    fun setShutterSpeedNanos(nanos: Long) {
        scope.launch { context.dataStore.edit { it[KEY_SHUTTER_NANOS] = nanos } }
    }

    fun setAutoIso(auto: Boolean) {
        scope.launch { context.dataStore.edit { it[KEY_AUTO_ISO] = auto } }
    }

    fun setIsoValue(iso: Int) {
        scope.launch { context.dataStore.edit { it[KEY_ISO] = iso } }
    }

    fun setFpsPreset(fps: FpsPreset) {
        scope.launch { context.dataStore.edit { it[KEY_FPS] = fps.name } }
    }

    fun setColorSpace(cs: ColorSpacePreset) {
        scope.launch { context.dataStore.edit { it[KEY_COLOR_SPACE] = cs.name } }
    }

    fun setLutEnabled(enabled: Boolean) {
        scope.launch { context.dataStore.edit { it[KEY_LUT] = enabled } }
    }

    fun setLutPreset(preset: LutPreset) {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_LUT_PRESET] = preset.name
                prefs.remove(KEY_SELECTED_CUSTOM_LUT_ID)
            }
        }
    }

    fun selectCustomLut(lutId: String?) {
        scope.launch {
            context.dataStore.edit { prefs ->
                if (lutId == null) {
                    prefs.remove(KEY_SELECTED_CUSTOM_LUT_ID)
                } else {
                    prefs[KEY_SELECTED_CUSTOM_LUT_ID] = lutId
                    prefs[KEY_LUT] = true
                }
            }
        }
    }

    fun addCustomLut(lut: CustomLut, selectImmediately: Boolean = true) {
        scope.launch {
            context.dataStore.edit { prefs ->
                val currentRaw = prefs[KEY_CUSTOM_LUTS_JSON] ?: "[]"
                val currentList = runCatching {
                    jsonFormat.decodeFromString<List<CustomLut>>(currentRaw)
                }.getOrDefault(emptyList()).toMutableList()

                currentList.removeAll { it.id == lut.id }
                currentList.add(lut)

                prefs[KEY_CUSTOM_LUTS_JSON] = jsonFormat.encodeToString(currentList)
                if (selectImmediately) {
                    prefs[KEY_SELECTED_CUSTOM_LUT_ID] = lut.id
                    prefs[KEY_LUT] = true
                }
            }
        }
    }

    fun removeCustomLut(lutId: String) {
        scope.launch {
            context.dataStore.edit { prefs ->
                val currentRaw = prefs[KEY_CUSTOM_LUTS_JSON] ?: "[]"
                val currentList = runCatching {
                    jsonFormat.decodeFromString<List<CustomLut>>(currentRaw)
                }.getOrDefault(emptyList()).filter { it.id != lutId }

                prefs[KEY_CUSTOM_LUTS_JSON] = jsonFormat.encodeToString(currentList)
                if (prefs[KEY_SELECTED_CUSTOM_LUT_ID] == lutId) {
                    prefs.remove(KEY_SELECTED_CUSTOM_LUT_ID)
                    prefs[KEY_LUT_PRESET] = LutPreset.TEAL_AND_ORANGE.name
                }
            }
        }
    }

    fun setLutBurnIn(burnIn: Boolean) {
        scope.launch { context.dataStore.edit { it[KEY_LUT_BURN] = burnIn } }
    }

    fun setFocusPeaking(enabled: Boolean) {
        scope.launch { context.dataStore.edit { it[KEY_PEAKING] = enabled } }
    }

    fun setZebraPattern(enabled: Boolean) {
        scope.launch { context.dataStore.edit { it[KEY_ZEBRA] = enabled } }
    }

    fun setHistogramMode(mode: HistogramMode) {
        scope.launch { context.dataStore.edit { it[KEY_HISTOGRAM_MODE] = mode.name } }
    }

    private companion object {
        val KEY_ASPECT = stringPreferencesKey("aspect_mode")
        val KEY_DR = stringPreferencesKey("dynamic_range")
        val KEY_QUALITY = stringPreferencesKey("quality")
        val KEY_LENS = stringPreferencesKey("lens_facing")
        val KEY_DCG = booleanPreferencesKey("dcg_hdr")
        val KEY_EV = intPreferencesKey("ev_index")
        val KEY_AWB = booleanPreferencesKey("auto_wb")
        val KEY_CCT = intPreferencesKey("cct_kelvin")
        val KEY_TINT = intPreferencesKey("tint_gm")
        val KEY_ZOOM = floatPreferencesKey("zoom_ratio")
        val KEY_ULTRA_HDR = booleanPreferencesKey("ultra_hdr")
        val KEY_MOTION_DUR = stringPreferencesKey("motion_duration")
        val KEY_VOL_UP = stringPreferencesKey("volume_key_up")
        val KEY_VOL_DOWN = stringPreferencesKey("volume_key_down")
        val KEY_AUTO_SHUTTER = booleanPreferencesKey("auto_shutter")
        val KEY_SHUTTER_NANOS = longPreferencesKey("shutter_speed_nanos")
        val KEY_AUTO_ISO = booleanPreferencesKey("auto_iso")
        val KEY_ISO = intPreferencesKey("iso_value")
        val KEY_FPS = stringPreferencesKey("fps_preset")
        val KEY_COLOR_SPACE = stringPreferencesKey("color_space")
        val KEY_LUT = booleanPreferencesKey("lut_enabled")
        val KEY_LUT_PRESET = stringPreferencesKey("lut_preset")
        val KEY_SELECTED_CUSTOM_LUT_ID = stringPreferencesKey("selected_custom_lut_id")
        val KEY_CUSTOM_LUTS_JSON = stringPreferencesKey("custom_luts_json")
        val KEY_LUT_BURN = booleanPreferencesKey("lut_burn_in")
        val KEY_PEAKING = booleanPreferencesKey("focus_peaking")
        val KEY_ZEBRA = booleanPreferencesKey("zebra_pattern")
        val KEY_HISTOGRAM_MODE = stringPreferencesKey("histogram_mode")
    }
}
