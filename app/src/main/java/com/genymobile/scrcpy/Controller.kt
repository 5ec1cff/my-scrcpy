package com.genymobile.scrcpy

import android.os.*
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import com.genymobile.scrcpy.ext.IMEController
import java.nio.charset.StandardCharsets


class Controller(initControl: InitControl, val handler: Handler, val sender: DeviceMessageSender) {

    private val DEFAULT_DEVICE_ID = 0

    private var device: ScreenDevice

    private val charMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

    private var lastTouchDown: Long = 0
    private val pointersState = PointersState()
    private val pointerProperties = arrayOfNulls<MotionEvent.PointerProperties>(PointersState.MAX_POINTERS)
    private val pointerCoords = arrayOfNulls<MotionEvent.PointerCoords>(PointersState.MAX_POINTERS)

    private var keepPowerModeOff: Boolean = false

    init {
        val (_, maxSize, lockedVideoOrientation, displayId) = initControl
        device = ScreenDevice(displayId, maxSize, lockedVideoOrientation)
        initPointers()
        if (device.supportsInputEvents()) {
            IMEController.get().setListener(object: IMEController.Listener {
                override fun onInputStarted() {
                    sender.schedulePushMessage(DeviceMessage.createEmpty(DeviceMessage.TYPE_IME_INPUT_STARTED))
                }

                override fun onInputFinished() {
                    sender.schedulePushMessage(DeviceMessage.createEmpty(DeviceMessage.TYPE_IME_INPUT_FINISHED))
                }

                override fun onCursorChanged(x: Float, y: Float) {
                    val pos = floatArrayOf(x, y)
                    device.toScreenPoint(x, y, pos)
                    Ln.d("Controller: onCursorChanged pos from ($x, $y) to (${pos[0]}, ${pos[1]})")
                    sender.schedulePushMessage(DeviceMessage.createCursorChanged(pos[0], pos[1]))
                }
            })
        }
    }

    private fun initPointers() {
        for (i in 0 until PointersState.MAX_POINTERS) {
            val props = MotionEvent.PointerProperties()
            props.toolType = MotionEvent.TOOL_TYPE_FINGER

            val coords = MotionEvent.PointerCoords()
            coords.orientation = 0f
            coords.size = 0f

            pointerProperties[i] = props
            pointerCoords[i] = coords
        }
    }

    fun onMessage(msg: ControlMessage) {
        handler.post {
            handleMessage(msg)
        }
    }

    private fun handleMessage(msg: ControlMessage) {
        // Ln.d("$msg")
        when (msg) {
            is InjectKeycode -> {
                if (device.supportsInputEvents()) {
                    injectKeycode(msg)
                }
                 }
            is InjectText -> {
                if (device.supportsInputEvents()) {
                    injectText(msg.text)
                }
                 }
            is InjectTouchEvent -> {
                if (device.supportsInputEvents()) {
                    injectTouch(msg)
                }
                 }
            is InjectScrollEvent -> {
                if (device.supportsInputEvents()) {
                    injectScroll(msg)
                }
                 }
            is BackOrScreenOn -> {
                if (device.supportsInputEvents()) {
                    pressBackOrTurnScreenOn(msg.action)
                }
                 }
            is Empty -> {
                when (msg.type) {
                    TYPE_EXPAND_NOTIFICATION_PANEL -> {
                        ScreenDevice.expandNotificationPanel()
                    }
                    TYPE_EXPAND_SETTINGS_PANEL -> {
                        ScreenDevice.expandSettingsPanel()
                    }
                    TYPE_COLLAPSE_PANELS -> {
                        ScreenDevice.collapsePanels()
                    }
                    TYPE_GET_CLIPBOARD -> {
                        val clipboardText = ScreenDevice.getClipboardText()
                        if (clipboardText != null) {
                            sender.schedulePushMessage(DeviceMessage.createClipboard(clipboardText))
                        }
                    }
                    TYPE_ROTATE_DEVICE -> {
                        ScreenDevice.rotateDevice()
                    }
                }
            }
            is SetClipboard -> {
                setClipboard(msg.text, msg.paste)
                 }
            is SetScreenPowerMode -> {
                if (device.supportsInputEvents()) {
                    val mode = msg.mode
                    val setPowerModeOk = ScreenDevice.setScreenPowerMode(mode)
                    if (setPowerModeOk) {
                        keepPowerModeOff = mode == ScreenDevice.POWER_MODE_OFF
                        Ln.i("Device screen turned " + (if (mode == ScreenDevice.POWER_MODE_OFF) "off" else "on"))
                    }
                }
                 }

            is IMEComposing -> {
                if (device.supportsInputEvents()) {
                    IMEController.get().commitComposingText(msg.text)
                }
                 }
        }
    }

    private fun injectKeycode(msg: InjectKeycode): Boolean {
        val (action: Int, keycode: Int, repeat: Int, metaState: Int) = msg
        if (keepPowerModeOff && action == KeyEvent.ACTION_UP && (keycode == KeyEvent.KEYCODE_POWER || keycode == KeyEvent.KEYCODE_WAKEUP)) {
            schedulePowerModeOff()
        }
        return device.injectKeyEvent(action, keycode, repeat, metaState)
    }

    private fun injectChar(c: Char): Boolean {
        val decomposed = KeyComposition.decompose(c)
        val chars = decomposed?.toCharArray() ?: charArrayOf(c)
        val events = charMap.getEvents(chars) ?: return false
        for (event in events) {
            if (!device.injectEvent(event)) {
                return false
            }
        }
        return true
    }

    private fun injectText(text: String): Int {
        val ic = IMEController.get()
        if (ic.isIMENeeded()) {
            if (ic.commitText(text))
                return text.length
            Ln.w("ime inject failed, inject via IM instead")
        }
        var successCount = 0
        for (c in text.toCharArray()) {
            if (!injectChar(c)) {
                Ln.w("Could not inject char u+" + String.format("%04x", c))
                continue
            }
            successCount++
        }
        return successCount
    }

    private fun injectTouch(msg: InjectTouchEvent): Boolean {
        var (action: Int, pointerId: Long, position: Position, pressure: Float, buttons: Int) = msg
        val now = SystemClock.uptimeMillis()

        val point = device.getPhysicalPoint(position)
        if (point == null) {
            Ln.w("Ignore touch event, it was generated for a different device size")
            return false
        }

        val pointerIndex = pointersState.getPointerIndex(pointerId)
        if (pointerIndex == -1) {
            Ln.w("Too many pointers for touch event")
            return false
        }
        val pointer = pointersState.get(pointerIndex)
        pointer.setPoint(point)
        pointer.setPressure(pressure)
        pointer.setUp(action == MotionEvent.ACTION_UP)

        val pointerCount = pointersState.update(pointerProperties, pointerCoords)

        if (pointerCount == 1) {
            if (action == MotionEvent.ACTION_DOWN) {
                lastTouchDown = now
            }
        } else {
            // secondary pointers must use ACTION_POINTER_* ORed with the pointerIndex
            if (action == MotionEvent.ACTION_UP) {
                action = MotionEvent.ACTION_POINTER_UP or (pointerIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
            } else if (action == MotionEvent.ACTION_DOWN) {
                action = MotionEvent.ACTION_POINTER_DOWN or (pointerIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
            }
        }

        // Right-click and middle-click only work if the source is a mouse
        val nonPrimaryButtonPressed = (buttons and (MotionEvent.BUTTON_PRIMARY).inv()) != 0
        val source = if (nonPrimaryButtonPressed) InputDevice.SOURCE_MOUSE else InputDevice.SOURCE_TOUCHSCREEN
        if (source != InputDevice.SOURCE_MOUSE) {
            // Buttons must not be set for touch events
            buttons = 0
        }

        val event = MotionEvent.obtain(lastTouchDown, now, action, pointerCount, pointerProperties, pointerCoords, 0, buttons, 1f, 1f, DEFAULT_DEVICE_ID, 0, source,
                        0)
        return device.injectEvent(event)
    }

    private fun injectScroll(msg: InjectScrollEvent): Boolean {
        val (position: Position, hScroll: Int, vScroll: Int) = msg
        val now = SystemClock.uptimeMillis()
        val point = device.getPhysicalPoint(position) ?: return false

        val props = pointerProperties[0]!!
        props.id = 0
        val coords = pointerCoords[0]!!
        coords.x = point.x.toFloat()
        coords.y = point.y.toFloat()
        coords.setAxisValue(MotionEvent.AXIS_HSCROLL, hScroll.toFloat())
        coords.setAxisValue(MotionEvent.AXIS_VSCROLL, vScroll.toFloat())

        val event = MotionEvent
                .obtain(lastTouchDown, now, MotionEvent.ACTION_SCROLL, 1, pointerProperties, pointerCoords, 0, 0, 1f, 1f, DEFAULT_DEVICE_ID, 0,
                        InputDevice.SOURCE_TOUCHSCREEN, 0)
        return device.injectEvent(event)
    }

    /**
     * Schedule a call to set power mode to off after a small delay.
     */
    private fun schedulePowerModeOff() {
        handler.postDelayed({
                Ln.i("Forcing screen off")
                ScreenDevice.setScreenPowerMode(ScreenDevice.POWER_MODE_OFF)
            }, 200)
    }

    private fun pressBackOrTurnScreenOn(action: Int): Boolean {
        if (ScreenDevice.isScreenOn()) {
            return device.injectKeyEvent(action, KeyEvent.KEYCODE_BACK, 0, 0)
        }

        // Screen is off
        // Only press POWER on ACTION_DOWN
        if (action != KeyEvent.ACTION_DOWN) {
            // do nothing,
            return true
        }

        if (keepPowerModeOff) {
            schedulePowerModeOff()
        }
        return device.pressReleaseKeycode(KeyEvent.KEYCODE_POWER)
    }

    private fun setClipboard(text: String, paste: Boolean): Boolean {
        val ok = device.setClipboardText(text)
        if (ok) {
            Ln.i("Device clipboard set")
        }

        // On Android >= 7, also press the PASTE key if requested
        if (paste && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && device.supportsInputEvents()) {
            device.pressReleaseKeycode(KeyEvent.KEYCODE_PASTE)
        }

        return ok
    }
}
