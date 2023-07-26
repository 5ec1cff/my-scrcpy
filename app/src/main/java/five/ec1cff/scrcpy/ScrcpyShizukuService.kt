package five.ec1cff.scrcpy

import android.os.*
import android.util.Log
import com.genymobile.scrcpy.ext.IMEController
import five.ec1cff.scrcpy.ext.shared.IInputMethod
import five.ec1cff.scrcpy.multitask.TaskController
import five.ec1cff.scrcpy.multitask.TaskOrganizer
import five.ec1cff.scrcpy.util.BinderWrapper

class ScrcpyShizukuService : IScrcpyShizukuService.Stub() {
    companion object {
        private val TAG = "scrcpy:${ScrcpyShizukuService::class.java.canonicalName}"
    }

    override fun destroy() {
        System.exit(1)
    }

    override fun exit() {
        Log.d(TAG, "exit called")
        System.exit(0)
    }

    override fun startScrcpy(port: Int, inputMethod: IBinder?): Int {
        inputMethod?.let {
            IMEController.method = IInputMethod.Stub.asInterface(it)?.apply {
                setClient(IMEController.client.asBinder())
            }
        }
        TaskOrganizer.init()
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
        return ScrcpyServer.start(port)
    }

    override fun getBinderWrapper(target: IBinder?): IBinder {
        return BinderWrapper(target)
    }

    override fun getTaskController(): IScrcpyTaskController {
        return TaskController
    }

}