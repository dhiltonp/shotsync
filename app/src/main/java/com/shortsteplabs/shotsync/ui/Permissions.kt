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
* Copyright (C) 2018  David Hilton <david.hilton.p@gmail.com>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
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
        requestFilePermissions()
        requestIgnoreBattery()
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