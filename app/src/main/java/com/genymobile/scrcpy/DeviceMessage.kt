package com.genymobile.scrcpy

const val TYPE_CLIPBOARD = 0
const val TYPE_IME_INPUT_STARTED = 1
const val TYPE_IME_INPUT_FINISHED = 2
const val TYPE_IME_CURSOR_CHANGED = 3

sealed class DeviceMessage {
    data class PushClipboard(val text: String) : DeviceMessage()
    object IMEInputStarted : DeviceMessage()
    object IMEInputFinished : DeviceMessage()
    data class IMECursorChanged(val x: Float, val y: Float) : DeviceMessage()
}