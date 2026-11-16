package com.swordfish.lemuroid.app.mobile.feature.game

import android.graphics.RectF
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelTouchControls.Companion.MENU_LOADING_ANIMATION_MILLIS
import com.swordfish.lemuroid.app.shared.settings.HapticFeedbackMode
import com.swordfish.lemuroid.lib.controller.ControllerConfig
import com.swordfish.touchinput.controller.R
import com.swordfish.touchinput.radial.LemuroidPadTheme
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import com.swordfish.touchinput.radial.PadSkin
import com.swordfish.touchinput.radial.sensors.TiltConfiguration
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import com.swordfish.touchinput.radial.ui.GlassSurface
import com.swordfish.touchinput.radial.ui.LemuroidButtonPressFeedback
import gg.padkit.PadKit
import gg.padkit.config.HapticFeedbackType
import gg.padkit.inputstate.InputState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun MobileGameScreen(viewModel: BaseGameScreenViewModel) {
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            viewModel.notifyTouchInteraction()
                        }
                    }
                },
    ) {
        val isLandscape = constraints.maxWidth > constraints.maxHeight

        LaunchedEffect(isLandscape) {
            val orientation =
                if (isLandscape) {
                    TouchControllerSettingsManager.Orientation.LANDSCAPE
                } else {
                    TouchControllerSettingsManager.Orientation.PORTRAIT
                }
            viewModel.onScreenOrientationChanged(orientation)
        }

        val controllerConfigState = viewModel.getTouchControllerConfig().collectAsState(null)
        val touchControlsVisibleState = viewModel.isTouchControllerVisible().collectAsState(false)
        val touchControllerSettingsState =
            viewModel
                .getTouchControlsSettings(LocalDensity.current, WindowInsets.displayCutout)
                .collectAsState(null)

        val touchControllerSettings = touchControllerSettingsState.value
        val currentControllerConfig = controllerConfigState.value

        val tiltConfiguration = viewModel.getTiltConfiguration().collectAsState(TiltConfiguration.Disabled)
        val tiltSimulatedStates = viewModel.getSimulatedTiltEvents().collectAsState(InputState())
        val tiltSimulatedControls = remember { derivedStateOf { tiltConfiguration.value.controlIds() } }

        val touchGamePads = currentControllerConfig?.getTouchControllerConfig()
        val leftGamePad = touchGamePads?.leftComposable
        val rightGamePad = touchGamePads?.rightComposable

        val hapticFeedbackMode =
            viewModel
                .getTouchHapticFeedbackMode()
                .collectAsState(HapticFeedbackMode.NONE)

        val padHapticFeedback =
            when (hapticFeedbackMode.value) {
                HapticFeedbackMode.NONE -> HapticFeedbackType.NONE
                HapticFeedbackMode.PRESS -> HapticFeedbackType.PRESS
                HapticFeedbackMode.PRESS_RELEASE -> HapticFeedbackType.PRESS_RELEASE
            }

        val lastInteractionTime by viewModel.getLastTouchInteractionTime().collectAsState()
        val manualHidden by viewModel.isManualControlsHidden().collectAsState()
        val isEditShown by viewModel.isEditControlShown().collectAsState(false)
        val autoHideSeconds = touchControllerSettings?.autoHideSeconds ?: 0

        var autoHidden by remember { mutableStateOf(false) }

        LaunchedEffect(lastInteractionTime, autoHideSeconds, isEditShown) {
            if (autoHideSeconds <= 0 || isEditShown) {
                autoHidden = false
            } else {
                autoHidden = false
                delay(autoHideSeconds * 1000L)
                autoHidden = true
            }
        }

        val controlsTargetAlpha =
            if ((autoHidden || manualHidden) && !isEditShown) {
                0f
            } else {
                1f
            }

        val animatedControlsAlpha by animateFloatAsState(
            targetValue = controlsTargetAlpha,
            animationSpec = tween(durationMillis = 300),
            label = "TouchControlsAlpha",
        )

        PadKit(
            modifier = Modifier.fillMaxSize(),
            onInputEvents = { viewModel.handleVirtualInputEvent(it) },
            hapticFeedbackType = padHapticFeedback,
            simulatedState = tiltSimulatedStates,
            simulatedControlIds = tiltSimulatedControls,
        ) {
            val localContext = LocalContext.current
            val lifecycle = LocalLifecycleOwner.current

            val fullScreenPosition = remember { mutableStateOf<Rect?>(null) }
            val viewportPosition = remember { mutableStateOf<Rect?>(null) }

            AndroidView(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { fullScreenPosition.value = it.boundsInRoot() },
                factory = {
                    viewModel.createRetroView(localContext, lifecycle)
                },
            )

            val fullPos = fullScreenPosition.value
            val viewPos = viewportPosition.value

            LaunchedEffect(fullPos, viewPos) {
                val gameView = viewModel.retroGameView.retroGameViewFlow()
                if (fullPos == null || viewPos == null) return@LaunchedEffect
                val viewport =
                    RectF(
                        (viewPos.left - fullPos.left) / fullPos.width,
                        (viewPos.top - fullPos.top) / fullPos.height,
                        (viewPos.right - fullPos.left) / fullPos.width,
                        (viewPos.bottom - fullPos.top) / fullPos.height,
                    )
                gameView.viewport = viewport
            }

            ConstraintLayout(
                modifier = Modifier.fillMaxSize(),
                constraintSet =
                    GameScreenLayout.buildConstraintSet(
                        isLandscape,
                        currentControllerConfig?.allowTouchOverlay ?: true,
                    ),
            ) {
                Box(
                    modifier =
                        Modifier
                            .layoutId(GameScreenLayout.CONSTRAINTS_GAME_VIEW)
                            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
                            .onGloballyPositioned { viewportPosition.value = it.boundsInRoot() }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { viewModel.notifyTouchInteraction() },
                                    onDoubleTap = {
                                        viewModel.notifyTouchInteraction()
                                        viewModel.triggerConfiguredGesture(viewModel.getDoubleTapGestureAction())
                                    },
                                )
                            },
                )

                val isVisible =
                    touchControllerSettings != null &&
                        currentControllerConfig != null &&
                        touchControlsVisibleState.value

                if (isVisible && touchControllerSettings != null && currentControllerConfig != null) {
                    val padTheme =
                        remember(
                            touchControllerSettings.skinId,
                            currentControllerConfig.touchControllerID,
                            touchControllerSettings.opacity,
                        ) {
                            LemuroidPadTheme(
                                skin = PadSkin.fromId(touchControllerSettings.skinId),
                                controllerID = currentControllerConfig.touchControllerID,
                                opacity = touchControllerSettings.opacity,
                            )
                        }

                    CompositionLocalProvider(LocalLemuroidPadTheme provides padTheme) {
                        val controlsModifier = Modifier.alpha(animatedControlsAlpha)

                        if (!isLandscape) {
                            PadContainer(
                                modifier =
                                    Modifier
                                        .layoutId(GameScreenLayout.CONSTRAINTS_BOTTOM_CONTAINER)
                                        .then(controlsModifier),
                            )
                        } else if (!currentControllerConfig.allowTouchOverlay) {
                            PadContainer(
                                modifier =
                                    Modifier
                                        .layoutId(GameScreenLayout.CONSTRAINTS_LEFT_CONTAINER)
                                        .then(controlsModifier),
                            )
                            PadContainer(
                                modifier =
                                    Modifier
                                        .layoutId(GameScreenLayout.CONSTRAINTS_RIGHT_CONTAINER)
                                        .then(controlsModifier),
                            )
                        }

                        if (!manualHidden) {
                            leftGamePad?.invoke(
                                this,
                                Modifier
                                    .layoutId(GameScreenLayout.CONSTRAINTS_LEFT_PAD)
                                    .then(controlsModifier),
                                touchControllerSettings,
                            )
                            rightGamePad?.invoke(
                                this,
                                Modifier
                                    .layoutId(GameScreenLayout.CONSTRAINTS_RIGHT_PAD)
                                    .then(controlsModifier),
                                touchControllerSettings,
                            )
                        }

                        GameScreenRunningCentralMenu(
                            modifier = Modifier.layoutId(GameScreenLayout.CONSTRAINTS_GAME_CONTAINER),
                            controllerConfig = currentControllerConfig,
                            touchControllerSettings = touchControllerSettings,
                            viewModel = viewModel,
                        )
                    }
                }
            }

            if (touchControllerSettings?.showQuickActions != false) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
                            .padding(top = 6.dp)
                            .alpha(if (autoHidden && !manualHidden) 0.25f else 0.85f),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    QuickActionsOverlayBar(
                        viewModel = viewModel,
                        manualHidden = manualHidden,
                    )
                }
            }
        }

        val isLoading =
            viewModel.loadingState
                .collectAsState(true)
                .value

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun QuickActionsOverlayBar(
    viewModel: BaseGameScreenViewModel,
    manualHidden: Boolean,
) {
    val fastForwardEnabled by viewModel.fastForwardEnabled.collectAsState(false)
    val statesSupported = viewModel.isStatesSupported()
    val fastForwardSupported = viewModel.isFastForwardSupported()

    Row(
        modifier =
            Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (statesSupported) {
            IconButton(
                onClick = { viewModel.saveQuickSave() },
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Guardado rápido",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(
                onClick = { viewModel.loadQuickSave() },
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = "Carga rápida",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (fastForwardSupported) {
            IconButton(
                onClick = { viewModel.toggleFastForward() },
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = "Fast Forward",
                    tint = if (fastForwardEnabled) Color(0xFF3EE366) else Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        IconButton(
            onClick = { viewModel.toggleManualControlsHidden() },
            modifier = Modifier.size(34.dp),
        ) {
            Icon(
                imageVector = if (manualHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = "Mostrar/Ocultar controles",
                tint = if (manualHidden) Color(0xFFFFB74D) else Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(
            onClick = { viewModel.openGameMenu() },
            modifier = Modifier.size(34.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menú",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun PadContainer(modifier: Modifier = Modifier) {
    val theme = LocalLemuroidPadTheme.current
    GlassSurface(
        modifier = modifier,
        cornerRadius = theme.level0CornerRadius,
        fillColor = theme.level0Fill,
        shadowColor = theme.level0Shadow,
        shadowWidth = theme.level0ShadowWidth,
    )
}

@Composable
private fun GameScreenRunningCentralMenu(
    modifier: Modifier = Modifier,
    viewModel: BaseGameScreenViewModel,
    touchControllerSettings: TouchControllerSettingsManager.Settings,
    controllerConfig: ControllerConfig,
) {
    val menuPressed = viewModel.isMenuPressed().collectAsState(false)
    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.Center,
    ) {
        LemuroidButtonPressFeedback(
            pressed = menuPressed.value,
            animationDurationMillis = MENU_LOADING_ANIMATION_MILLIS,
            icon = R.drawable.button_menu,
        )
        MenuEditTouchControls(viewModel, controllerConfig, touchControllerSettings)
    }
}

@Composable
private fun MenuEditTouchControls(
    viewModel: BaseGameScreenViewModel,
    controllerConfig: ControllerConfig,
    touchControllerSettings: TouchControllerSettingsManager.Settings,
) {
    val showEditControls = viewModel.isEditControlShown().collectAsState(false)
    if (!showEditControls.value) return

    val profiles = remember { viewModel.getAvailableControlProfiles() }
    val autoHideOptions = listOf(0, 3, 5, 10, 15)

    Dialog(onDismissRequest = { viewModel.showEditControls(false) }) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Personalizar controles táctiles",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                // Scale Slider
                val scalePercent = (55 + (touchControllerSettings.scale * 110)).roundToInt()
                Text(
                    text = "Tamaño: $scalePercent%",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                MenuEditTouchControlRow(Icons.Default.OpenInFull, "Scale", 0f) {
                    Slider(
                        value = touchControllerSettings.scale,
                        onValueChange = {
                            viewModel.updateTouchControllerSettings(
                                touchControllerSettings.copy(scale = it),
                            )
                        },
                    )
                }

                // Opacity Slider
                val opacityPercent = (touchControllerSettings.opacity * 100).roundToInt()
                Text(
                    text = "Opacidad: $opacityPercent%",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                MenuEditTouchControlRow(Icons.Default.Opacity, "Opacity", 0f) {
                    Slider(
                        value = touchControllerSettings.opacity,
                        valueRange = 0.15f..1.0f,
                        onValueChange = {
                            viewModel.updateTouchControllerSettings(
                                touchControllerSettings.copy(opacity = it),
                            )
                        },
                    )
                }

                // Margins & Rotation
                MenuEditTouchControlRow(Icons.Default.Height, "Horizontal Margin", 90f) {
                    Slider(
                        value = touchControllerSettings.marginX,
                        onValueChange = {
                            viewModel.updateTouchControllerSettings(
                                touchControllerSettings.copy(marginX = it),
                            )
                        },
                    )
                }
                MenuEditTouchControlRow(Icons.Default.Height, "Vertical Margin", 0f) {
                    Slider(
                        value = touchControllerSettings.marginY,
                        onValueChange = {
                            viewModel.updateTouchControllerSettings(
                                touchControllerSettings.copy(marginY = it),
                            )
                        },
                    )
                }
                if (controllerConfig.allowTouchRotation) {
                    MenuEditTouchControlRow(Icons.Default.RotateLeft, "Rotate", 0f) {
                        Slider(
                            value = touchControllerSettings.rotation,
                            onValueChange = {
                                viewModel.updateTouchControllerSettings(
                                    touchControllerSettings.copy(rotation = it),
                                )
                            },
                        )
                    }
                }

                // Auto-hide selector
                Text(
                    text = "Auto-ocultar controles inactivos",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    autoHideOptions.forEach { seconds ->
                        val label = if (seconds == 0) "Nunca" else "${seconds}s"
                        FilterChip(
                            selected = touchControllerSettings.autoHideSeconds == seconds,
                            onClick = {
                                viewModel.updateTouchControllerSettings(
                                    touchControllerSettings.copy(autoHideSeconds = seconds),
                                )
                            },
                            label = { Text(label) },
                        )
                    }
                }

                // Console Skins selector
                Text(
                    text = "Skin del mando",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PadSkin.values().forEach { skin ->
                        FilterChip(
                            selected = touchControllerSettings.skinId == skin.id,
                            onClick = {
                                viewModel.updateTouchControllerSettings(
                                    touchControllerSettings.copy(skinId = skin.id),
                                )
                            },
                            label = { Text(skin.displayName) },
                        )
                    }
                }

                // Control Profiles (Save & Load)
                Text(
                    text = "Perfiles de controles",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    profiles.forEach { profile ->
                        FilterChip(
                            selected = touchControllerSettings.profileName == profile.id,
                            onClick = {
                                viewModel.loadTouchControllerProfile(
                                    profile.id,
                                    touchControllerSettings,
                                )
                            },
                            label = { Text(profile.name) },
                        )
                    }
                }
                if (!touchControllerSettings.profileName.startsWith("default") &&
                    !touchControllerSettings.profileName.startsWith("compact") &&
                    !touchControllerSettings.profileName.startsWith("large") &&
                    !touchControllerSettings.profileName.startsWith("minimal")
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.saveTouchControllerProfile(
                                touchControllerSettings.profileName,
                                touchControllerSettings,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Guardar cambios en ${touchControllerSettings.profileName}")
                    }
                }

                // Switches
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Usar joystick en lugar de D-pad",
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = touchControllerSettings.useAnalogStick,
                        onCheckedChange = {
                            viewModel.updateTouchControllerSettings(
                                touchControllerSettings.copy(useAnalogStick = it),
                            )
                        },
                    )
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Barra rápida (Guardado / Fast Forward)",
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = touchControllerSettings.showQuickActions,
                        onCheckedChange = {
                            viewModel.updateTouchControllerSettings(
                                touchControllerSettings.copy(showQuickActions = it),
                            )
                        },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { viewModel.resetTouchControls() },
                        modifier = Modifier.padding(4.dp),
                    ) {
                        Text(text = stringResource(R.string.touch_customize_button_reset))
                    }
                    TextButton(
                        onClick = { viewModel.showEditControls(false) },
                        modifier = Modifier.padding(4.dp),
                    ) {
                        Text(text = stringResource(R.string.touch_customize_button_done))
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuEditTouchControlRow(
    icon: ImageVector,
    label: String,
    rotation: Float,
    slider: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            modifier = Modifier.rotate(rotation),
            imageVector = icon,
            contentDescription = label,
        )
        slider()
    }
}
