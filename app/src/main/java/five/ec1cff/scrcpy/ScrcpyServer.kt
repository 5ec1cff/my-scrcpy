package five.ec1cff.scrcpy

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.genymobile.scrcpy.*
import java.io.FileDescriptor
import java.lang.IllegalStateException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

class SocketHandler {
    private val reader = ControlMessageReader()
    private lateinit var controller: Controller
    private lateinit var sender: DeviceMessageSender

    fun read(channel: SocketChannel, key: SelectionKey) {
        reader.readFrom(channel)
        val msg = reader.next() ?: return
        if (this::controller.isInitialized) {
            controller.onMessage(msg)
        } else {
            when (msg) {
                is InitControl -> {
                    Ln.d("init control for ${channel.remoteAddress} $msg")
                    val handler = Handler(Looper.getMainLooper())
                    sender = DeviceMessageSender(key, handler)
                    controller = Controller(msg, handler, sender)
                }
                is InitVideo -> {
                    Ln.d("init video for ${channel.remoteAddress} $msg")
                    startVideoStream(msg, channel)
                    key.attach(null)
                    key.cancel()
                }
                else -> throw IllegalStateException("The socket should initiate first!")
            }
        }
    }

    fun write() {
        sender.scheduleWrite()
    }
}

const val DEVICE_NAME_FIELD_LENGTH = 64

private fun sendVideoInitMsg(stream: ChannelOutputStream,deviceName: String, width: Int, height: Int) {
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

class ChannelOutputStream(val channel: SocketChannel) {

    fun write(buffer: ByteBuffer) {
        while (true) {
            if (!buffer.hasRemaining()) break
            channel.write(buffer)
        }
    }
}

fun startVideoStream(msg: InitVideo, socket: SocketChannel) {
    val device = ScreenDevice(msg.displayId, msg.maxSize, msg.lockedVideoOrientation)
    val codecOptions = CodecOption.parse(msg.codecOptions)
    val screenEncoder = ScreenEncoder(true, msg.bitRate, msg.maxFps, codecOptions, msg.encoderName)
    thread(name="video-${msg.displayId}") {
        try {
            val size = device.screenInfo.videoSize
            val stream = ChannelOutputStream(socket)
            sendVideoInitMsg(stream, ScreenDevice.getDeviceName(), size.width, size.height)
            screenEncoder.streamScreen(device, stream)
        } catch (e: Throwable) {
            Ln.e("error", e)
            Ln.d("streaming stop")
        }
    }
}

fun startServer(port: Int): Int {
    Ln.initLogLevel(Ln.Level.DEBUG)
    val serverSocketChannel = ServerSocketChannel.open().also {
        it.socket().bind(InetSocketAddress(port))
        it.configureBlocking(false)
    }
    val selector = Selector.open()
    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)
    thread(name="scrcpy-main") {
        while (true) {
            try {
                if (selector.select() == 0) continue
                val iterator = selector.selectedKeys().iterator()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    iterator.remove()
                    if (key.isAcceptable) {
                        val client = (key.channel() as ServerSocketChannel).accept()
                        client.configureBlocking(false)
                        client.register(selector, SelectionKey.OP_READ, SocketHandler())
                        Ln.d("accept ${client.remoteAddress}")
                    }

                    if (key.isReadable) {
                        val channel = key.channel() as SocketChannel
                        try {
                            (key.attachment() as? SocketHandler)?.read(channel, key)
                        } catch (t: Throwable) {
                            Log.e("", "failed to handle message from socket ${channel.remoteAddress}", t)
                            try {
                                channel.shutdownInput()
                                channel.shutdownOutput()
                                channel.close()
                            } catch (t: Throwable) {

                            }
                        }
                    }

                    if (key.isValid && key.isWritable) {
                        (key.attachment() as? SocketHandler)?.write()
                        key.interestOps(SelectionKey.OP_READ)
                    }
                }
            } catch (e: InterruptedException) {
                selector.close()
                serverSocketChannel.close()
                break
            }
        }
    }
    return serverSocketChannel.socket().localPort.also { Ln.d("listen on $it") }
}