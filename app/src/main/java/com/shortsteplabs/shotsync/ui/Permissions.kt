package com.shortsteplabs.shotsync.ui

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.appcompat.app.AlertDialog

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
        const val PERMISSIONS = 1
    }

    fun requestPermissions() {
        val neededPermissions = mutableListOf<String>()

        addPermission(WRITE_EXTERNAL_STORAGE, neededPermissions)
        addPermission(ACCESS_FINE_LOCATION, neededPermissions)

        if (neededPermissions.size > 0) {
            ActivityCompat.requestPermissions(activity, neededPermissions.toTypedArray(), PERMISSIONS)
        }
    }

    // todo: if permission request fails, clear the Setting that requires it...
    fun requestPermission(permission: String) {
        val neededPermissions = mutableListOf<String>()
        addPermission(permission, neededPermissions)
        if (neededPermissions.size > 0) {
            ActivityCompat.requestPermissions(activity, neededPermissions.toTypedArray(), PERMISSIONS)
        }
    }

    private fun addPermission(permission: String, neededPermissions: MutableList<String>) {
        val locationPermission = ActivityCompat.checkSelfPermission(activity, permission)
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(permission)
        }
    }

    fun requestPermissionsCallback(permissions: Array<out String>, grantResults: IntArray) {
        val settings = com.shortsteplabs.shotsync.util.Settings(activity)
        settings.async = true

        for (i in 0 until permissions.size) {
            if (permissions[i] == ACCESS_FINE_LOCATION) {
                settings.syncGPS = grantResults[i] == PackageManager.PERMISSION_GRANTED
            } else if (permissions[i] == WRITE_EXTERNAL_STORAGE) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    settings.syncJPG = false
                    settings.syncRAW = false
                    settings.syncVID = false
                }
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
                    val builder = AlertDialog.Builder(activity)
                    builder.setMessage("Automatic Syncs require ignoring battery optimizations. " +
                            "ShotSync is careful to not impact battery life between syncs, but syncs use battery.\n\n" +
                            "Continue, then tap 'Not optimized', switch to 'All apps'. Find 'ShotSync' and select 'Don't optimize'.")
                    builder.setPositiveButton("Continue", fun(_: DialogInterface?, _: Int) {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        intent.flags = intent.flags or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS
                        activity.startActivity(intent)
                    })
                    builder.setNegativeButton("No Automatic Syncs", fun(_: DialogInterface?, _: Int) {
                        val settings = com.shortsteplabs.shotsync.util.Settings(activity)
                        settings.autoSync = false
                    })
                    builder.show()
                }
            }
        }
    }
}
