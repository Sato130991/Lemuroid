package com.swordfish.touchinput.radial.settings

import android.content.SharedPreferences
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.max
import androidx.core.content.edit
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import com.swordfish.lemuroid.common.compose.pxToDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

class TouchControllerSettingsManager(private val sharedPreferences: SharedPreferences) {
    private val flowSharedPreferences = FlowSharedPreferences(sharedPreferences)

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    enum class Orientation {
        PORTRAIT,
        LANDSCAPE,
    }

    @Serializable
    data class Settings(
        val scale: Float = DEFAULT_SCALE,
        val rotation: Float = DEFAULT_ROTATION,
        val marginX: Float = DEFAULT_MARGIN_X,
        val marginY: Float = DEFAULT_MARGIN_Y,
        val useAnalogStick: Boolean = DEFAULT_USE_ANALOG_STICK,
        val opacity: Float = DEFAULT_OPACITY,
        val autoHideSeconds: Int = DEFAULT_AUTO_HIDE_SECONDS,
        val skinId: String = DEFAULT_SKIN_ID,
        val showQuickActions: Boolean = DEFAULT_SHOW_QUICK_ACTIONS,
        val profileName: String = DEFAULT_PROFILE_NAME,
    )

    data class ControlProfile(
        val id: String,
        val name: String,
        val isPreset: Boolean = false,
    )

    private fun computeInsetsPaddings(
        density: Density,
        insets: WindowInsets,
    ): PaddingValues {
        return PaddingValues(
            insets.getLeft(density, layoutDirection = LayoutDirection.Ltr).pxToDp(density),
            insets.getTop(density).pxToDp(density),
            insets.getRight(density, layoutDirection = LayoutDirection.Ltr).pxToDp(density),
            insets.getBottom(density).pxToDp(density),
        )
    }

    private val cachedSettings = mutableMapOf<String, MutableStateFlow<Settings?>>()

    fun observeSettings(
        touchControllerID: TouchControllerID,
        orientation: Orientation,
        density: Density,
        insets: WindowInsets,
    ): Flow<Settings> {
        val paddings = computeInsetsPaddings(density, insets)
        val horizontalPadding =
            max(
                paddings.calculateLeftPadding(LayoutDirection.Ltr),
                paddings.calculateRightPadding(LayoutDirection.Ltr),
            )
        val verticalPadding = paddings.calculateBottomPadding()
        val defaultSettings =
            Settings(
                scale = DEFAULT_SCALE,
                rotation = DEFAULT_ROTATION,
                marginX = horizontalPadding.value / MAX_MARGINS,
                marginY = verticalPadding.value / MAX_MARGINS,
                useAnalogStick = sharedPreferences.getBoolean(USE_ANALOG_STICK_KEY, DEFAULT_USE_ANALOG_STICK),
                opacity = sharedPreferences.getInt(OPACITY_KEY, DEFAULT_OPACITY_INT) / 100f,
                autoHideSeconds = sharedPreferences.getInt(AUTO_HIDE_SECONDS_KEY, DEFAULT_AUTO_HIDE_SECONDS),
                skinId = sharedPreferences.getString(SKIN_ID_KEY, DEFAULT_SKIN_ID) ?: DEFAULT_SKIN_ID,
                showQuickActions = sharedPreferences.getBoolean(SHOW_QUICK_ACTIONS_KEY, DEFAULT_SHOW_QUICK_ACTIONS),
            )
        val settingsKey = getPreferenceString(touchControllerID, orientation)
        val cachedStateFlow =
            cachedSettings.getOrPut(settingsKey) {
                val currentSettings =
                    sharedPreferences.getString(settingsKey, null)
                        ?.let {
                            runCatching {
                                json.decodeFromString(Settings.serializer(), it)
                            }.getOrNull()
                        }

                MutableStateFlow(currentSettings)
            }

        val useAnalogStickFlow =
            flowSharedPreferences
                .getBoolean(USE_ANALOG_STICK_KEY, DEFAULT_USE_ANALOG_STICK)
                .asFlow()

        val opacityFlow =
            flowSharedPreferences
                .getInt(OPACITY_KEY, DEFAULT_OPACITY_INT)
                .asFlow()
                .map { (it / 100f).coerceIn(0.15f, 1.0f) }

        val autoHideFlow =
            flowSharedPreferences
                .getInt(AUTO_HIDE_SECONDS_KEY, DEFAULT_AUTO_HIDE_SECONDS)
                .asFlow()

        val skinFlow =
            flowSharedPreferences
                .getString(SKIN_ID_KEY, DEFAULT_SKIN_ID)
                .asFlow()

        val quickActionsFlow =
            flowSharedPreferences
                .getBoolean(SHOW_QUICK_ACTIONS_KEY, DEFAULT_SHOW_QUICK_ACTIONS)
                .asFlow()

        val globalConfigFlow =
            combine(
                useAnalogStickFlow,
                opacityFlow,
                autoHideFlow,
                skinFlow,
                quickActionsFlow,
            ) { useAnalogStick, opacity, autoHideSeconds, skinId, showQuickActions ->
                GlobalTouchPrefs(useAnalogStick, opacity, autoHideSeconds, skinId, showQuickActions)
            }

        return combine(
            cachedStateFlow.map { it ?: defaultSettings },
            globalConfigFlow,
        ) { settings, globalPrefs ->
            settings.copy(
                useAnalogStick = globalPrefs.useAnalogStick,
                opacity = globalPrefs.opacity,
                autoHideSeconds = globalPrefs.autoHideSeconds,
                skinId = globalPrefs.skinId,
                showQuickActions = globalPrefs.showQuickActions,
            )
        }
    }

    private data class GlobalTouchPrefs(
        val useAnalogStick: Boolean,
        val opacity: Float,
        val autoHideSeconds: Int,
        val skinId: String,
        val showQuickActions: Boolean,
    )

    suspend fun storeSettings(
        touchControllerID: TouchControllerID,
        orientation: Orientation,
        settings: Settings,
    ) {
        Timber.d("Updating touch settings for $touchControllerID at $orientation to $settings")
        updateCachedSettings(touchControllerID, orientation, settings)
        withContext(Dispatchers.IO) {
            sharedPreferences.edit {
                putString(
                    getPreferenceString(touchControllerID, orientation),
                    json.encodeToString(Settings.serializer(), settings),
                )
                putBoolean(USE_ANALOG_STICK_KEY, settings.useAnalogStick)
                putInt(OPACITY_KEY, (settings.opacity * 100).toInt().coerceIn(15, 100))
                putInt(AUTO_HIDE_SECONDS_KEY, settings.autoHideSeconds)
                putString(SKIN_ID_KEY, settings.skinId)
                putBoolean(SHOW_QUICK_ACTIONS_KEY, settings.showQuickActions)
            }
        }
    }

    suspend fun saveProfile(
        profileId: String,
        orientation: Orientation,
        settings: Settings,
    ) {
        val updatedSettings = settings.copy(profileName = profileId)
        withContext(Dispatchers.IO) {
            val customIds =
                sharedPreferences
                    .getStringSet(CUSTOM_PROFILES_SET_KEY, emptySet())
                    ?.toMutableSet() ?: mutableSetOf()
            if (profileId !in PRESET_PROFILES.map { it.id }) {
                customIds.add(profileId)
            }
            sharedPreferences.edit {
                putStringSet(CUSTOM_PROFILES_SET_KEY, customIds)
                putString(
                    getProfilePreferenceKey(profileId, orientation),
                    json.encodeToString(Settings.serializer(), updatedSettings),
                )
            }
        }
    }

    suspend fun loadProfile(
        profileId: String,
        touchControllerID: TouchControllerID,
        orientation: Orientation,
        currentSettings: Settings,
    ): Settings {
        val loaded =
            withContext(Dispatchers.IO) {
                when (profileId) {
                    "default" ->
                        currentSettings.copy(
                            scale = DEFAULT_SCALE,
                            rotation = DEFAULT_ROTATION,
                            opacity = DEFAULT_OPACITY,
                            profileName = "default",
                        )

                    "compact" ->
                        currentSettings.copy(
                            scale = 0.25f,
                            rotation = 0f,
                            marginX = 0.05f,
                            marginY = 0.05f,
                            opacity = 0.75f,
                            profileName = "compact",
                        )

                    "large" ->
                        currentSettings.copy(
                            scale = 0.82f,
                            rotation = 0f,
                            marginX = 0.12f,
                            marginY = 0.12f,
                            opacity = 0.95f,
                            profileName = "large",
                        )

                    "minimal" ->
                        currentSettings.copy(
                            scale = 0.45f,
                            opacity = 0.45f,
                            autoHideSeconds = 5,
                            profileName = "minimal",
                        )

                    else -> {
                        sharedPreferences.getString(getProfilePreferenceKey(profileId, orientation), null)
                            ?.let {
                                runCatching {
                                    json.decodeFromString(Settings.serializer(), it)
                                }.getOrNull()
                            }?.copy(profileName = profileId)
                            ?: currentSettings.copy(profileName = profileId)
                    }
                }
            }
        storeSettings(touchControllerID, orientation, loaded)
        return loaded
    }

    fun getAvailableProfiles(): List<ControlProfile> {
        val customIds =
            sharedPreferences.getStringSet(CUSTOM_PROFILES_SET_KEY, emptySet())
                ?.sorted()
                ?: emptyList()
        val customProfiles =
            customIds.map { id ->
                ControlProfile(id = id, name = id, isPreset = false)
            }
        return PRESET_PROFILES + customProfiles
    }

    private fun updateCachedSettings(
        touchControllerID: TouchControllerID,
        orientation: Orientation,
        settings: Settings?,
    ) {
        val cacheKey = getPreferenceString(touchControllerID, orientation)
        val cacheFlow = cachedSettings.getOrPut(cacheKey) { MutableStateFlow(settings) }
        cacheFlow.value = settings
    }

    suspend fun resetSettings(
        touchControllerID: TouchControllerID,
        orientation: Orientation,
    ) {
        updateCachedSettings(touchControllerID, orientation, null)
        withContext(Dispatchers.IO) {
            sharedPreferences.edit {
                remove(getPreferenceString(touchControllerID, orientation))
                putInt(OPACITY_KEY, DEFAULT_OPACITY_INT)
                putInt(AUTO_HIDE_SECONDS_KEY, DEFAULT_AUTO_HIDE_SECONDS)
                putString(SKIN_ID_KEY, DEFAULT_SKIN_ID)
            }
        }
    }

    companion object {
        const val DEFAULT_SCALE = 0.5f
        const val DEFAULT_ROTATION = 0.0f
        const val DEFAULT_MARGIN_X = 0.0f
        const val DEFAULT_MARGIN_Y = 0.0f
        const val DEFAULT_USE_ANALOG_STICK = false
        const val DEFAULT_OPACITY = 0.85f
        const val DEFAULT_OPACITY_INT = 85
        const val DEFAULT_AUTO_HIDE_SECONDS = 0
        const val DEFAULT_SKIN_ID = "auto"
        const val DEFAULT_SHOW_QUICK_ACTIONS = true
        const val DEFAULT_PROFILE_NAME = "default"

        const val USE_ANALOG_STICK_KEY = "use_analog_stick"
        const val OPACITY_KEY = "touch_controls_opacity"
        const val AUTO_HIDE_SECONDS_KEY = "touch_auto_hide_seconds"
        const val SKIN_ID_KEY = "touch_controls_skin"
        const val SHOW_QUICK_ACTIONS_KEY = "touch_quick_actions"
        private const val CUSTOM_PROFILES_SET_KEY = "touch_custom_profiles_set"

        const val MAX_ROTATION = 45f
        const val MIN_SCALE = 0.55f
        const val MAX_SCALE = 1.65f

        const val MAX_MARGINS = 96f

        val PRESET_PROFILES =
            listOf(
                ControlProfile("default", "Estándar", isPreset = true),
                ControlProfile("compact", "Compacto", isPreset = true),
                ControlProfile("large", "Grande", isPreset = true),
                ControlProfile("minimal", "Minimalista", isPreset = true),
                ControlProfile("Perfil 1", "Perfil 1", isPreset = false),
                ControlProfile("Perfil 2", "Perfil 2", isPreset = false),
                ControlProfile("Perfil 3", "Perfil 3", isPreset = false),
            )
    }

    private fun getPreferenceString(
        controllerID: TouchControllerID,
        orientation: Orientation,
    ): String {
        return "touch_controller_settings_${controllerID}_${orientation.ordinal}"
    }

    private fun getProfilePreferenceKey(
        profileId: String,
        orientation: Orientation,
    ): String {
        return "touch_controller_profile_${profileId}_${orientation.ordinal}"
    }
}
