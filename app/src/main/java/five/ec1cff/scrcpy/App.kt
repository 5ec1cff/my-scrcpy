package five.ec1cff.scrcpy

import android.app.Application
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.sui.Sui
import kotlin.properties.Delegates

class App : Application() {
    companion object {
        var isSui by Delegates.notNull<Boolean>()

        init {
            isSui = Sui.init(BuildConfig.APPLICATION_ID)
            HiddenApiBypass.setHiddenApiExemptions("")
        }

        var scrcpyShizukuService: IScrcpyShizukuService? = null
        lateinit var taskController: IScrcpyTaskController
    }

    override fun onCreate() {
        super.onCreate()
        Global.isApp = true
    }
}