package five.ec1cff.scrcpy

import android.os.*
import android.system.Os
import android.util.Log
import com.genymobile.scrcpy.ext.IMEController
import five.ec1cff.scrcpy.ext.shared.IInputMethod
import java.net.ServerSocket
import kotlin.concurrent.thread

class ScrcpyShizukuService : IScrcpyShizukuService.Stub() {
    val TAG = "scrcpy:${ScrcpyShizukuService::class.java.canonicalName}"

    override fun destroy() {
        System.exit(1)
    }

    override fun exit() {
        Log.d(TAG, "exit called")
        System.exit(0)
    }

    override fun startScrcpy(port: Int, inputMethod: IBinder?): Int {
        Handler(Looper.getMainLooper()).post {
            // seteuid for main thread can be used for Binder.getCallingUid check
            Os.seteuid(1000)
            Log.d(TAG, "euid=${Os.geteuid()}, uid=${Process.myUid()}")
        }
        inputMethod?.let {
            IMEController.method = IInputMethod.Stub.asInterface(it)?.apply {
                setClient(IMEController.client.asBinder())
            }
        }
            /*
            val options = Server.createOptions(
                "1.18",
                "info",
                "0",
                "8000000",
                "0",
                "-1",
                "false",
                "-",
                "true",
                "true",
                "0",
                "false",
                "false",
                "-",
                "-",
                "false",
                "none"
            )
            Ln.initLogLevel(options.logLevel)
            Server.scrcpy(options, serverSocket)*/
        return startServer(port)
    }
}