package com.shortsteplabs.shotsync.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.view.View
import com.shortsteplabs.shotsync.DownloaderService.Companion.startSync

/**
 * Created by david on 4/13/18.
 */

class Permissions(private val activity: Activity): ActivityCompat.OnRequestPermissionsResultCallback {
    val WRITE_EXTERNAL = 1

    fun requestFilePermissions() {
        // TODO: handle the callback
        val neededPermissions = mutableListOf<String>()

        val writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (writePermission != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        // We don't have permission so prompt the user
        if (neededPermissions.size > 0) {
            ActivityCompat.requestPermissions(
                    activity,
                    neededPermissions.toTypedArray(),
                    WRITE_EXTERNAL
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            WRITE_EXTERNAL -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startSync(activity)
                }
            }
            else -> {
            }
        }
    }

    fun requestIgnoreBattery() {
        if (Build.VERSION.SDK_INT >= 23) {
            val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(activity.packageName)) {
                try {
                    activity.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS))
                } catch (e: ActivityNotFoundException) {

                    class LaunchBatteryOptimizations : View.OnClickListener {
                        override fun onClick(v: View) {
                            activity.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    }
                    val notification = Snackbar.make(activity.findViewById(android.R.id.content),
                            "Mark app as 'Don't Optimize' for reliable communication with camera",
                            Snackbar.LENGTH_INDEFINITE)
                    notification.setAction("Open", LaunchBatteryOptimizations())
                    notification.show()
                }
            }
        }
    }
}