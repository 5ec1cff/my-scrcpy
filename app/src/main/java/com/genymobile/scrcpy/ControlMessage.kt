package com.genymobile.scrcpy;

import android.graphics.Rect

/**
 * Union of all supported event types, identified by their {@code type}.
 */
sealed class ControlMessage

const val TYPE_INJECT_KEYCODE = 0
const val TYPE_INJECT_TEXT = 1
const val TYPE_INJECT_TOUCH_EVENT = 2
const val TYPE_INJECT_SCROLL_EVENT = 3
const val TYPE_BACK_OR_SCREEN_ON = 4
const val TYPE_EXPAND_NOTIFICATION_PANEL = 5
const val TYPE_EXPAND_SETTINGS_PANEL = 6
const val TYPE_COLLAPSE_PANELS = 7
const val TYPE_GET_CLIPBOARD = 8
const val TYPE_SET_CLIPBOARD = 9
const val TYPE_SET_SCREEN_POWER_MODE = 10
const val TYPE_ROTATE_DEVICE = 11
const val TYPE_IME_COMPOSING = 12
const val TYPE_INIT_CONTROL = 13
const val TYPE_INIT_VIDEO = 14

data class InjectKeycode(val action: Int, val keycode: Int, val repeat: Int, val metaState: Int) : ControlMessage()
data class InjectText(val text: String) : ControlMessage()
data class InjectTouchEvent(val action: Int, val pointerId: Long, val position: Position, val pressure: Float, val buttons: Int) : ControlMessage()
data class InjectScrollEvent(val position: Position, val hScroll: Int, val vScroll: Int) : ControlMessage()
data class BackOrScreenOn(val action: Int) : ControlMessage()
data class SetClipboard(val text: String, val paste: Boolean) : ControlMessage()
/**
 * @param mode one of the {@code Device.SCREEN_POWER_MODE_*} constants
 */
data class SetScreenPowerMode(val mode: Int) : ControlMessage()
data class Empty(val type: Int) : ControlMessage()
data class IMEComposing(val text: String) : ControlMessage()
sealed class InitialMessage : ControlMessage()
data class Init(
    val version: String,
    val maxSize: Int,
    val lockedVideoOrientation: Int,
    val displayId: Int,
    val bitRate: Int,
    val maxFps: Int,
    val encoderName: String?,
    val codecOptions: String?
    ): InitialMessage()
data class StartVideo(
    val version: String,
    val sessionId: Int
): InitialMessage()



