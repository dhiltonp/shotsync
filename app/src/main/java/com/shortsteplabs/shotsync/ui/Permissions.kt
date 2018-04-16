package com.shortsteplabs.shotsync.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.view.View
import com.shortsteplabs.shotsync.R

/**
 * Created by david on 4/13/18.
 */

class Permissions(private val activity: Activity): FragmentActivity() {
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

    fun firstRun() {
        val pref = activity.getPreferences(Context.MODE_PRIVATE)
        val lastVersion = pref.getInt(activity.getString(R.string.version_code), -1)
        val thisVersion = activity.packageManager.getPackageInfo(activity.packageName, 0).versionCode
        if (lastVersion != thisVersion) {
            requestFilePermissions()
            requestIgnoreBattery()
            with(pref.edit()) {
                putInt(activity.getString(R.string.version_code), thisVersion)
                commit()
            }
        }
    }

    fun updateSyncDownloadSetting() {
        val writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (writePermission != PackageManager.PERMISSION_GRANTED) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            with (prefs.edit()) {
                putBoolean(activity.getString(R.string.sync_files_key), false)
                commit()
            }
        }
    }
}