package five.ec1cff.scrcpy

import com.genymobile.scrcpy.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

interface ScrcpySocket {
    fun read(channel: SocketChannel)
    fun notifyWrite(selectionKey: SelectionKey)
    fun close()
}

class ScrcpyControllerSocket(val clientRecord: ScrcpyClientRecord,
                             private val reader: ControlMessageReader,
                             private val controller: Controller,
                             private val sender: DeviceMessageSender,
                             private val socket: SocketChannel
): ScrcpySocket {

    override fun read(channel: SocketChannel) {
        reader.readFrom(channel)
        consumeMessage(channel)
    }

    fun consumeMessage(channel: SocketChannel) {
        val msg = reader.next() ?: return
        var newMsg: ControlMessage? = msg
        while (newMsg != null) {
            controller.onMessage(newMsg)
            reader.readFrom(channel)
            newMsg = reader.next()
        }
    }

    override fun notifyWrite(selectionKey: SelectionKey) {
        selectionKey.interestOps(SelectionKey.OP_READ)
        sender.scheduleWrite()
    }

    override fun close() {
        try {
            socket.shutdownInput()
            socket.shutdownOutput()
            socket.close()
        } catch (e: IOException) {

        }
    }
}

const val DEVICE_NAME_FIELD_LENGTH = 64

// TODO: Use async MediaCodec instead
class BlockedOutputStream(val channel: SocketChannel, val key: SelectionKey) {
    fun write(buffer: ByteBuffer) {
        while (true) {
            channel.write(buffer)
            if (!buffer.hasRemaining()) break
            else {
                key.interestOps(SelectionKey.OP_WRITE)
                synchronized(this) {
                    key.selector().wakeup()
                    (this as java.lang.Object).wait()
                }
            }
        }
    }
}

class ScrcpyVideoSocket(val clientRecord: ScrcpyClientRecord, private val socket: SocketChannel, key: SelectionKey): ScrcpySocket {
    private val stream: BlockedOutputStream
    init {
        val msg = clientRecord.initData
        val device = clientRecord.device
        val codecOptions = CodecOption.parse(msg.codecOptions)
        val screenEncoder =
            ScreenEncoder(true, msg.bitRate, msg.maxFps, codecOptions, msg.encoderName)
        stream = BlockedOutputStream(socket, key)
        thread(name = "video-${msg.displayId}") {
            try {
                // val size = device.screenInfo.videoSize
                // sendVideoInitMsg(stream, ScreenDevice.getDeviceName(), size.width, size.height)
                screenEncoder.streamScreen(device, stream)
            } catch (e: Throwable) {
                Ln.e("error", e)
                Ln.d("streaming stop")
            }
        }
    }

    private fun sendVideoInitMsg(stream: BlockedOutputStream, deviceName: String, width: Int, height: Int) {
        val buffer = ByteArray(DEVICE_NAME_FIELD_LENGTH + 4)
        val deviceNameBytes = deviceName.toByteArray(StandardCharsets.UTF_8)
        val len = StringUtils.getUtf8TruncationIndex(
            deviceNameBytes,
            DEVICE_NAME_FIELD_LENGTH - 1
        )
        System.arraycopy(deviceNameBytes, 0, buffer, 0, len)
        // byte[] are always 0-initialized in java, no need to set '\0' explicitly
        buffer[DEVICE_NAME_FIELD_LENGTH] = (width shr 8).toByte()
        buffer[DEVICE_NAME_FIELD_LENGTH + 1] = width.toByte()
        buffer[DEVICE_NAME_FIELD_LENGTH + 2] = (height shr 8).toByte()
        buffer[DEVICE_NAME_FIELD_LENGTH + 3] = height.toByte()
        stream.write(ByteBuffer.wrap(buffer))
    }

    override fun read(channel: SocketChannel) {
        // no need
    }

    override fun notifyWrite(selectionKey: SelectionKey) {
        synchronized(stream) {
            (stream as java.lang.Object).notify()
        }
        selectionKey.interestOps(0)
    }

    override fun close() {
        try {
            socket.shutdownInput()
            socket.shutdownOutput()
            socket.close()
            synchronized(stream) {
                (stream as java.lang.Object).notify()
            }
        } catch (e: IOException) {

        }
    }
}