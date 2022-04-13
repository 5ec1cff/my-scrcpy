package com.genymobile.scrcpy;

import android.os.Handler
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets

class DeviceMessageSender(val selectionKey: SelectionKey, val handler: Handler) {
    private val MESSAGE_MAX_SIZE = 1 shl 18 // 256k

    val CLIPBOARD_TEXT_MAX_LENGTH = MESSAGE_MAX_SIZE - 5 // type: 1 byte; length: 4 bytes

    private val buffer = ByteBuffer.allocateDirect(MESSAGE_MAX_SIZE).apply { limit(0) }

    fun schedulePushMessage(msg: DeviceMessage) {
        handler.post {
            pushMessage(msg)
        }
    }

    @Throws(IOException::class)
    private fun pushMessage(msg: DeviceMessage) {
        buffer.compact()
        when (msg.type) {
            DeviceMessage.TYPE_CLIPBOARD -> {
                buffer.put(DeviceMessage.TYPE_CLIPBOARD.toByte())
                val text = msg.text
                val raw = text.toByteArray(StandardCharsets.UTF_8)
                val len = StringUtils.getUtf8TruncationIndex(raw, CLIPBOARD_TEXT_MAX_LENGTH)
                buffer.putInt(len)
                buffer.put(raw, 0, len)
            }
            DeviceMessage.TYPE_IME_INPUT_FINISHED, DeviceMessage.TYPE_IME_INPUT_STARTED -> buffer.put(
                msg.type.toByte()
            )
            DeviceMessage.TYPE_IME_CURSOR_CHANGED -> {
                buffer.put(DeviceMessage.TYPE_IME_CURSOR_CHANGED.toByte())
                val floats = msg.floats
                buffer.putFloat(floats[0])
                buffer.putFloat(floats[1])
            }
            else -> Ln.w("Unknown device message: " + msg.type)
        }
        buffer.flip()
        write()
    }

    fun scheduleWrite() {
        handler.post {
            write()
        }
    }

    private fun write() {
        val channel = selectionKey.channel() as SocketChannel
        channel.write(buffer)
        if (buffer.hasRemaining()) {
            selectionKey.interestOps(SelectionKey.OP_WRITE or SelectionKey.OP_READ)
            selectionKey.selector().wakeup()
        }
    }
}
