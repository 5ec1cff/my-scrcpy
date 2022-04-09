package five.ec1cff.scrcpy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Binder
import android.os.IBinder
import android.util.Log
import rikka.shizuku.Shizuku

const val CHANNEL_ID = "scrcpy"
const val NOTIFICATION_ID = 1
const val ACTION_START_SERVICE = "five.ec1cff.scrcpy.START_SERVICE"
const val ACTION_STOP_SERVICE = "five.ec1cff.scrcpy.STOP_SERVICE"

class ScrcpyFrontService : Service() {
    private val TAG = "scrcpy:${ScrcpyFrontService::class.java.canonicalName}"

    private val serviceArgs =
        Shizuku.UserServiceArgs(
            ComponentName(BuildConfig.APPLICATION_ID, ScrcpyShizukuService::class.java.name)
        ).apply {
        daemon(false)
        processNameSuffix("service")
        debuggable(BuildConfig.DEBUG)
        version(BuildConfig.VERSION_CODE)
    }

    private lateinit var shizukuService: IScrcpyShizukuService

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            Log.d(TAG, "shizuku service connected")
            shizukuService = IScrcpyShizukuService.Stub.asInterface(p1)
            val serverPort = shizukuService.startScrcpy(0)
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "scrcpyTest"
                serviceType = "_scrcpy._tcp"
                port = serverPort
            }
            Log.d(TAG, "server listen on ${serverPort}")
            (getSystemService(Context.NSD_SERVICE) as NsdManager).registerService(serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                object :NsdManager.RegistrationListener {
                    override fun onRegistrationFailed(p0: NsdServiceInfo?, p1: Int) {
                        Log.e(TAG, "register nsd failed")
                    }

                    override fun onUnregistrationFailed(p0: NsdServiceInfo?, p1: Int) {
                        Log.e(TAG, "unregister nsd failed")
                    }

                    override fun onServiceRegistered(p0: NsdServiceInfo?) {
                        Log.d(TAG, "registered: $p0")
                    }

                    override fun onServiceUnregistered(p0: NsdServiceInfo?) {
                        Log.d(TAG, "unregistered: $p0")
                    }

                }
            )
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            Log.d(TAG, "shizuku service disconnected")
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun statusText(): String {
        return "waiting..."
    }

    private fun createNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("scrcpy")
            .setContentText(statusText())
            .build()

    override fun onCreate() {
        Log.d(TAG, "create")
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "scrcpy", NotificationManager.IMPORTANCE_NONE)
        )
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "start")
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                startShizukuService()
            }
            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startShizukuService() {
        Shizuku.bindUserService(serviceArgs, serviceConnection)
    }
}