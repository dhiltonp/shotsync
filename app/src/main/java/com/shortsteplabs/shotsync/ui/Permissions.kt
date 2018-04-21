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
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import com.shortsteplabs.shotsync.R
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
                camera.syncFiles = granted
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
                    builder.setMessage("Mark app as 'Don't Optimize' for background syncs")
                    builder.setPositiveButton("Open", fun(dialog: DialogInterface?, id: Int) {
                        activity.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    })
                    builder.setNegativeButton("Cancel", fun(_: DialogInterface?, _: Int) {
                        val camera = getCamera(DB.getInstance(activity), 1)
                        camera.autoSync = false
                    })
                    builder.show()
                }
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
