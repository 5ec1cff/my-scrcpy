package five.ec1cff.scrcpy

import android.view.Surface
import com.genymobile.scrcpy.CodecOption
import java.nio.channels.SocketChannel

abstract class ScreenStreamer(val bitRate: Int, val maxFps: Int, val codecOptions: List<CodecOption>, val encoderName: String) {
    abstract fun onSurfaceCreated(surface: Surface)
    abstract fun onSurfaceDestroy(surface: Surface)

    init {

    }

    fun startStream(channel: SocketChannel) {}
    fun stopStream() {}
}