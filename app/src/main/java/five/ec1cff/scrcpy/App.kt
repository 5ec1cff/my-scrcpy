package five.ec1cff.scrcpy

import android.app.Application
import rikka.sui.Sui
import kotlin.properties.Delegates

class App : Application() {
    companion object {
        var isSui by Delegates.notNull<Boolean>()

        init {
            isSui = Sui.init(BuildConfig.APPLICATION_ID)
        }
    }
}