package com.shortsteplabs.shotsync.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Environment
import android.support.v4.app.ActivityCompat
import java.io.File



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

fun filesExist(context: Context): Boolean {
    val writePermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    if (writePermission == PackageManager.PERMISSION_GRANTED) {
        val path = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "ShotSync")

        return path.exists() && path.listFiles().isNotEmpty()
    }

    return false
}

open class RecursiveDelete:AsyncTask<Activity, Long, Long>() {
    var filesProcessed = 0
    var bytes = 0L

    override fun doInBackground(vararg params: Activity): Long {
        val path = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "ShotSync")

        if (path.exists()) {
            return delete(path)
        }
        return 0
    }

    fun delete(path: File): Long {
        filesProcessed += 1
        if (path.isDirectory) {
            for (f in path.listFiles()) {
                delete(f)
            }
        }

        bytes += path.length()
//        val mediaStore = ContentValues()
//        mediaStore.remove()
        path.delete()

        if (filesProcessed % 25 == 0) {
            publishProgress(bytes)
        }

        return bytes
    }
}

