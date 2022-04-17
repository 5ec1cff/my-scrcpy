package com.genymobile.scrcpy

import java.io.EOFException
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets

class ControlMessageReader {
    companion object {
        const val INJECT_KEYCODE_PAYLOAD_LENGTH = 13
        const val INJECT_TOUCH_EVENT_PAYLOAD_LENGTH = 27
        const val INJECT_SCROLL_EVENT_PAYLOAD_LENGTH = 20
        const val BACK_OR_SCREEN_ON_LENGTH = 1
        const val SET_SCREEN_POWER_MODE_PAYLOAD_LENGTH = 1
        const val SET_CLIPBOARD_FIXED_PAYLOAD_LENGTH = 1

        private const  val MESSAGE_MAX_SIZE = 1 shl 18; // 256k

        const val CLIPBOARD_TEXT_MAX_LENGTH =
            MESSAGE_MAX_SIZE - 6; // type: 1 byte; paste flag: 1 byte; length: 4 bytes
        const val INJECT_TEXT_MAX_LENGTH = 300
    }

    val buffer = ByteBuffer.allocateDirect(MESSAGE_MAX_SIZE).also { it.limit(0) }

    private fun isFull() = buffer.remaining() == buffer.capacity()

    fun clear() {
        buffer.clear()
        buffer.limit(0)
    }

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
                if (buffer.remaining() < 20) null
                else Init(version, buffer.int, buffer.int, buffer.int, buffer.int, buffer.int, parseString(), parseString())
            }
            TYPE_INIT_VIDEO -> parseString() ?. let { version ->
                if (buffer.remaining() < 4) null
                else StartVideo(version, buffer.int)
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

    val utf8Decoder = StandardCharsets.UTF_8.newDecoder()

    private fun parseString(): String? {
        if (buffer.remaining() >= 4) {
            val len = buffer.int
            if (buffer.remaining() >= len) {
                val limit = buffer.limit()
                buffer.limit(buffer.position() + len)
                return utf8Decoder.decode(buffer).toString().also {
                    buffer.limit(limit)
                    // Ln.d("parseString $len $it ${buffer.remaining()}")
                }
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
