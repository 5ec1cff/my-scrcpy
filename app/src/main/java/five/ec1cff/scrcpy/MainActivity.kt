package five.ec1cff.scrcpy

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Shizuku.addRequestPermissionResultListener { _, result ->
            if (result == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted()
            }
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(0)
        } else {
            onPermissionGranted()
        }
    }

    private fun onPermissionGranted() {
        startForegroundService(Intent(this, ScrcpyFrontService::class.java).also {
            it.action = ACTION_START_SERVICE
        })
    }
}