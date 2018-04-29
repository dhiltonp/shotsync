package com.shortsteplabs.shotsync.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import com.shortsteplabs.shotsync.db.DB
import com.shortsteplabs.shotsync.db.getCamera

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
    companion object {
        const val WRITE_EXTERNAL = 1
    }

    fun requestFilePermissions() {
        // TODO: handle the callback
        val neededPermissions = mutableListOf<String>()

        val writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (writePermission != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(
                    activity,
                    neededPermissions.toTypedArray(),
                    WRITE_EXTERNAL
            )
        }
    }

    fun requestFilePermissionsCallback(permissions: Array<out String>, grantResults: IntArray) {
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

        class dbPermissions: AsyncTask<Void, Void, Void?>() {
            override fun doInBackground(vararg params: Void?): Void? {
                val camera = getCamera(DB.getInstance(activity))
                if (granted) {
                    camera.syncFiles = true
                } else {
                    camera.syncFiles = false
                    camera.syncJPG = false
                    camera.syncRAW = false
                    camera.syncVID = false
                }
                DB.getInstance(activity).cameraDao().update(camera)
                return null
            }
        }
        dbPermissions().execute()
    }

    fun requestIgnoreBattery() {
        if (Build.VERSION.SDK_INT >= 23) {
            val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(activity.packageName)) {
                try {
                    activity.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS))
                } catch (e: ActivityNotFoundException) {
                    val builder = AlertDialog.Builder(activity)
                    builder.setMessage("Automatic Syncs require ignoring battery optimizations. " +
                            "The app is careful to not impact battery life in standby, but syncs do use battery. " +
                            "Tap 'Not optimized', switch to 'All apps'. Find 'ShotSync' and select 'Don't optimize'.")
                    builder.setPositiveButton("Open", fun(_: DialogInterface?, _: Int) {
                        activity.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    })
                    builder.setNegativeButton("No Automatic Syncs", fun(_: DialogInterface?, _: Int) {
                        class noAutoSync: AsyncTask<Void, Void, Void?>() {
                            override fun doInBackground(vararg params: Void?): Void? {
                                val camera = getCamera(DB.getInstance(activity))
                                camera.autoSync = false
                                DB.getInstance(activity).cameraDao().update(camera)
                                return null
                            }
                        }
                        noAutoSync().execute()
                    })
                    builder.show()
                }
            }
        }
    }
}
