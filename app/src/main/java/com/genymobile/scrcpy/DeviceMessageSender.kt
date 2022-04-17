package com.genymobile.scrcpy;

import android.os.Handler
import five.ec1cff.scrcpy.DEVICE_NAME_FIELD_LENGTH
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets

class DeviceMessageSender(private val selectionKey: SelectionKey, private val handler: Handler) {
    companion object {
        private const val MESSAGE_MAX_SIZE = 1 shl 18 // 256k
        private const val CLIPBOARD_TEXT_MAX_LENGTH = MESSAGE_MAX_SIZE - 5 // type: 1 byte; length: 4 bytes
    }

    private val buffer = ByteBuffer.allocateDirect(MESSAGE_MAX_SIZE).apply { limit(0) }

    fun schedulePushMessage(msg: DeviceMessage) {
        handler.post {
            pushMessage(msg)
        }
    }

    @Throws(IOException::class)
    private fun pushMessage(msg: DeviceMessage) {
        buffer.compact()
        when (msg) {
            is DeviceMessage.PushClipboard -> {
                buffer.put(TYPE_CLIPBOARD.toByte())
                val text = msg.text
                val raw = text.toByteArray(StandardCharsets.UTF_8)
                val len = StringUtils.getUtf8TruncationIndex(raw, CLIPBOARD_TEXT_MAX_LENGTH)
                buffer.putInt(len)
                buffer.put(raw, 0, len)
            }
            is DeviceMessage.IMEInputStarted -> buffer.put(TYPE_IME_INPUT_STARTED.toByte())
            is DeviceMessage.IMEInputFinished -> buffer.put(TYPE_IME_INPUT_FINISHED.toByte())
            is DeviceMessage.IMECursorChanged -> {
                buffer.put(TYPE_IME_CURSOR_CHANGED.toByte())
                buffer.putFloat(msg.x)
                buffer.putFloat(msg.y)
            }
            else -> Ln.w("Unknown device message: $msg")
        }
        buffer.flip()
        write()
    }

    fun schedulePushInitReply(sessionId: Int, w: Int, h: Int, deviceName: String) {
        handler.post {
            buffer.compact()
            buffer.putInt(sessionId)
            buffer.putShort(w.toShort())
            buffer.putShort(h.toShort())
            val deviceNameBytes = deviceName.toByteArray(StandardCharsets.UTF_8).let {
                val realBytes = ByteArray(DEVICE_NAME_FIELD_LENGTH)
                val len = StringUtils.getUtf8TruncationIndex(
                    it,
                    DEVICE_NAME_FIELD_LENGTH - 1
                )
                System.arraycopy(it, 0, realBytes, 0, len)
                realBytes
            }
            buffer.put(deviceNameBytes)
            buffer.flip()
            write()
        }
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
