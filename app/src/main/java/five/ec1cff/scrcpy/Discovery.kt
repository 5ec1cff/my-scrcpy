package five.ec1cff.scrcpy

import android.util.Log
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import kotlin.concurrent.thread

object discoveryService {
    val TAG = "DiscoveryService"
    var serviceThread: Thread? = null
    var serviceSocket: MulticastSocket? = null
    val group = InetAddress.getByName("224.0.0.252")

    fun start(port: Int, deviceName: String) {
        if (serviceThread != null) return
        val socket = MulticastSocket(1415)
        Log.d(TAG, "bind on ${socket.localAddress}")
        socket.joinGroup(group)
        socket.loopbackMode = false
        serviceSocket = socket
        serviceThread = thread(name=TAG) {
            val buffer = ByteArray(32)
            val packet = DatagramPacket(buffer, buffer.size)
            while (true) {
                try {
                    socket.receive(packet)
                    Log.d(TAG, "receive ${packet.socketAddress} ${String(buffer)}")
                    if (String(buffer.sliceArray(0..7)) == "!!SCRCPY") {
                        val end = buffer.indexOf(0)
                        val name = String(buffer.sliceArray(8 until end))
                        Log.d(TAG, "query name=$name")
                        if (name == deviceName) {
                            val data = "!!SCRCPY${port}".toByteArray()
                            Log.d(TAG, "name match, reply")
                            val reply =
                                DatagramPacket(data, data.size, packet.address, packet.port)
                            socket.send(reply)
                        }
                    }
                } catch (e: IOException) {
                    Log.d(TAG, "exit loop")
                    break
                }
            }
            Log.d(TAG, "thread exit")
        }
    }

    fun stop() {
        serviceThread?.interrupt()
        try {
            serviceSocket?.leaveGroup(group)
        } finally {
            serviceSocket?.close()
            serviceSocket = null
        }
    }
}