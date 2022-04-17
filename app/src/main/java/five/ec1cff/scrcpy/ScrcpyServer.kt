package five.ec1cff.scrcpy

import android.annotation.SuppressLint
import android.app.ActivityThread
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.system.Os
import android.util.Log
import com.genymobile.scrcpy.*
import java.io.IOException
import java.lang.IllegalStateException
import java.net.InetSocketAddress
import java.nio.channels.*
import kotlin.concurrent.thread

class ScrcpyClientRecord(val sessionId: Int, val initData: Init) {
    lateinit var controller: ScrcpyControllerSocket
    lateinit var video: ScrcpyVideoSocket
    var device: ScreenDevice

    init {
        val (_, maxSize, lockedVideoOrientation, displayId) = initData
        device = ScreenDevice(displayId, maxSize, lockedVideoOrientation)
        ScrcpyServer.clientMap.put(sessionId, this)
    }

    fun cleanUp() {
        if (this::controller.isInitialized)
            controller.close()
        if (this::video.isInitialized)
            video.close()
        device.cleanUp()
        ScrcpyServer.clientMap.remove(sessionId)
        Ln.d("client $sessionId cleanUp")
    }
}

@SuppressLint("StaticFieldLeak")
object ScrcpyServer {
    private var started = false
    private var running = false
    private lateinit var selector: Selector
    private lateinit var serverSocketChannel: ServerSocketChannel
    private var initReader: ControlMessageReader = ControlMessageReader()
    val clientMap: MutableMap<Int, ScrcpyClientRecord> = HashMap()

    val context: Context = ActivityThread.currentActivityThread().systemContext
    val handler = Handler(Looper.getMainLooper())

    private var nextSessionId = 0
        get() {
            field += 1
            return field
        }

    private fun selectOnce() {
        if (selector.select() == 0) return
        val iterator = selector.selectedKeys().iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            iterator.remove()
            if (key.isAcceptable) {
                val client = (key.channel() as ServerSocketChannel).accept()
                client.configureBlocking(false)
                client.register(selector, SelectionKey.OP_READ, initReader)
                Ln.d("accept ${client.remoteAddress}")
            }

            if (!key.isValid) {
                key.cancel()
                continue
            }

            if (key.isReadable) {
                val channel = key.channel() as SocketChannel
                try {
                    val attachment = key.attachment()
                    when (attachment) {
                        is ControlMessageReader -> {
                            initReader.readFrom(channel)
                            val initMsg = initReader.next() ?: continue
                            when (initMsg) {
                                is Init -> {
                                    Ln.d("init control for ${channel.remoteAddress} $initMsg")
                                    val handler = Handler(Looper.getMainLooper())
                                    val sender = DeviceMessageSender(key, handler)
                                    val sessionId = nextSessionId
                                    val client = ScrcpyClientRecord(sessionId, initMsg)
                                    Ln.d("create session $sessionId")
                                    val controller = Controller(client, handler, sender)
                                    val controllerSocket = ScrcpyControllerSocket(client, initReader, controller, sender, channel)
                                    client.controller = controllerSocket
                                    key.attach(controllerSocket)
                                    initReader = ControlMessageReader()
                                    controllerSocket.consumeMessage(channel)
                                }
                                is StartVideo -> {
                                    Ln.d("init video for ${channel.remoteAddress} $initMsg")
                                    key.interestOps(0)
                                    val clientRecord = clientMap.get(initMsg.sessionId) ?: throw IllegalStateException("failed to find client record")
                                    val videoSocket = ScrcpyVideoSocket(clientRecord, channel, key)
                                    clientRecord.video = videoSocket
                                    key.attach(videoSocket)
                                    initReader.clear()
                                }
                                else -> throw IllegalStateException("The socket should initiate first!")
                            }
                        }
                        is ScrcpySocket -> {
                            attachment.read(channel)
                        }
                    }
                } catch (t: Throwable) {
                    Log.e(
                        "",
                        "failed to handle message from socket ${channel.remoteAddress}",
                        t
                    )
                    try {
                        val attachment = key.attachment()
                        when (attachment) {
                            is ScrcpyControllerSocket -> attachment.clientRecord.cleanUp()
                            is ScrcpyVideoSocket -> attachment.clientRecord.cleanUp()
                            else -> {
                                channel.shutdownInput()
                                channel.shutdownOutput()
                                channel.close()
                            }
                        }
                    } catch (t: Throwable) {
                    }
                }
            }

            if (key.isValid && key.isWritable) {
                (key.attachment() as? ScrcpySocket)?.notifyWrite(key)
            }
        }
    }

    fun start(port: Int): Int {
        if (started) return 0
        Ln.initLogLevel(Ln.Level.DEBUG)
        handler.post {
            // seteuid for main thread can be used for Binder.getCallingUid check
            Os.seteuid(1000)
            Ln.d("euid=${Os.geteuid()}, uid=${Process.myUid()}")
        }
        serverSocketChannel = ServerSocketChannel.open().also {
            it.socket().bind(InetSocketAddress(port))
            it.configureBlocking(false)
        }
        selector = Selector.open()
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)
        running = true
        thread(name = "scrcpy-main") {
            while (running) {
                try {
                    selectOnce()
                } catch (e: IOException) {
                    selector.close()
                    serverSocketChannel.close()
                    break
                }
            }
        }
        started = true
        return serverSocketChannel.socket().localPort.also { Ln.d("listen on $it") }
    }

    fun stop() {}
}