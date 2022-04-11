package com.genymobile.scrcpy

import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets

class ControlMessageReader {

    val INJECT_KEYCODE_PAYLOAD_LENGTH = 13
    val INJECT_TOUCH_EVENT_PAYLOAD_LENGTH = 27
    val INJECT_SCROLL_EVENT_PAYLOAD_LENGTH = 20
    val BACK_OR_SCREEN_ON_LENGTH = 1
    val SET_SCREEN_POWER_MODE_PAYLOAD_LENGTH = 1
    val SET_CLIPBOARD_FIXED_PAYLOAD_LENGTH = 1

    private val MESSAGE_MAX_SIZE = 1 shl 18; // 256k

    val CLIPBOARD_TEXT_MAX_LENGTH = MESSAGE_MAX_SIZE - 6; // type: 1 byte; paste flag: 1 byte; length: 4 bytes
    val INJECT_TEXT_MAX_LENGTH = 300

    val buffer = ByteBuffer.allocateDirect(MESSAGE_MAX_SIZE).also { it.limit(0) }

    private fun isFull() = buffer.remaining() == buffer.capacity()

    fun readFrom(channel: SocketChannel) {
        if (isFull()) {
            throw IllegalStateException("Buffer full, call next() to consume")
        }
        buffer.compact()
        while (true) {
            val r = channel.read(buffer)
            if (r == -1) {
                throw EOFException("Controller socket closed")
            }
            if (r == 0) break
        }
        buffer.flip()
    }

    fun next(): ControlMessage? {
        if (!buffer.hasRemaining()) {
            return null
        }
        val savedPosition = buffer.position()

        val type = buffer.get()
        val msg : ControlMessage? = when(type.toInt()) {
            TYPE_INJECT_KEYCODE -> {
                if (buffer.remaining() < INJECT_KEYCODE_PAYLOAD_LENGTH) {
                    null
                } else {
                    val action = toUnsigned(buffer.get())
                    val keycode = buffer.int
                    val repeat = buffer.int
                    val metaState = buffer.int
                    InjectKeycode(action, keycode, repeat, metaState)
                }
            }
            TYPE_INJECT_TEXT -> {
                parseString() ?. let { InjectText(it) }
            }
            TYPE_INJECT_TOUCH_EVENT -> {
                if (buffer.remaining() < INJECT_TOUCH_EVENT_PAYLOAD_LENGTH) {
                    return null
                } else {
                    val action = toUnsigned (buffer.get())
                    val pointerId = buffer.long
                    val position = readPosition (buffer)
                    // 16 bits fixed-point
                    val pressureInt = toUnsigned (buffer.short)
                    // convert it to a float between 0 and 1 (0x1p16f is 2^16 as float)
                    val pressure = if (pressureInt == 0xffff) 1f else (pressureInt / 65536.0f) // 0x1p16f
                    val buttons = buffer.int
                    InjectTouchEvent(
                        action,
                        pointerId,
                        position,
                        pressure,
                        buttons
                    )
                }
            }
            TYPE_INJECT_SCROLL_EVENT -> {
                if (buffer.remaining() < INJECT_SCROLL_EVENT_PAYLOAD_LENGTH) {
                    return null
                } else {
                    val position = readPosition (buffer)
                    val hScroll = buffer.int
                    val vScroll = buffer.int
                    InjectScrollEvent(position, hScroll, vScroll)
                }
            }
            TYPE_BACK_OR_SCREEN_ON -> {
                if (buffer.remaining() < BACK_OR_SCREEN_ON_LENGTH) {
                    return null
                } else {
                    val action = toUnsigned (buffer.get())
                    BackOrScreenOn(action)
                }
            }
            TYPE_SET_CLIPBOARD -> {
                if (buffer.remaining() < SET_CLIPBOARD_FIXED_PAYLOAD_LENGTH) {
                    return null
                } else {
                    val paste = buffer.get().toInt() != 0
                    parseString() ?. let { text ->
                        SetClipboard(text, paste)
                    }
                }
            }
            TYPE_SET_SCREEN_POWER_MODE -> {
                if (buffer.remaining() < SET_SCREEN_POWER_MODE_PAYLOAD_LENGTH) {
                    return null
                } else {
                    val mode = buffer.get().toInt()
                    SetScreenPowerMode(mode)
                }
            }
            TYPE_EXPAND_NOTIFICATION_PANEL,
            TYPE_EXPAND_SETTINGS_PANEL,
            TYPE_COLLAPSE_PANELS,
            TYPE_GET_CLIPBOARD,
            TYPE_ROTATE_DEVICE -> Empty(type.toInt())
            TYPE_IME_COMPOSING -> parseString() ?. let { text ->
                IMEComposing(text)
            }
            TYPE_INIT_CONTROL -> parseString() ?. let { version ->
                if (buffer.remaining() < 12) null
                else InitControl(version, buffer.int, buffer.int, buffer.int)
            }
            TYPE_INIT_VIDEO -> parseString() ?. let { version ->
                if (buffer.remaining() < 20) null
                else InitVideo(version, buffer.int, buffer.int, buffer.int, buffer.int, buffer.int, parseString(), parseString())
            }
            else -> {
                Ln.w("Unknown event type: $type")
                null
            }
        }

        if (msg == null) {
            // failure, reset savedPosition
            buffer.position(savedPosition)
        }
        return msg
    }

    private fun parseString(): String? {
        if (buffer.remaining() >= 4) {
            val len = buffer.int
            if (buffer.remaining() >= len) {
                val position = buffer.position()
                // Move the buffer position to consume the text
                buffer.position(position + len)
                return String(buffer.array(), position, len, StandardCharsets.UTF_8)
            }
        }
        return null
    }
}

fun readPosition(buffer: ByteBuffer): Position {
    val x = buffer.int
    val y = buffer.int
    val screenWidth = toUnsigned(buffer.short)
    val screenHeight = toUnsigned(buffer.short)
    return Position(x, y, screenWidth, screenHeight)
}

fun toUnsigned(value: Short) = value.toInt() and 0xffff

fun toUnsigned(value: Byte) = value.toInt() and 0xff
