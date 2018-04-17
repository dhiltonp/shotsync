package com.shortsteplabs.shotsync.sync

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.webkit.MimeTypeMap
import com.shortsteplabs.shotsync.HttpHelper
import com.shortsteplabs.shotsync.OlyEntry
import com.shortsteplabs.shotsync.OlyInterface
import com.shortsteplabs.shotsync.ui.SyncNotification
import java.io.File
import java.util.*

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


class Syncer(val downloaderService: DownloaderService) {
    val TAG = "Syncer"
    val notification = SyncNotification(downloaderService)

    var downloading = false

    fun cleanup() {
        notification.clearStatus()
    }

    fun hasWritePermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }


    fun startDownload() {
        Log.d(TAG, "Starting downloadLoop")
//            downloading = true

        notification.status("Starting Download", "Connecting to camera.")

        val client = HttpHelper()

        // TODO: do the following asynchronously, stopSelf when it stops running. also allow cancellation.
        try {
            OlyInterface.connect(client)
            downloadLoop(client)
        } catch (e: HttpHelper.NoConnection) {
            notification.clearable("Download stopped", "Unable to connect")
//                stopSelf()
            return
        }
    }

    private fun downloadLoop(client: HttpHelper) {
        val camera = OlyInterface.getCamInfo(client)
        notification.status("Starting Download", "Connected to $camera, discovering new files.")

        var toDownload = 0
        var downloaded = 0
        var currentFilename = ""
        try {
            // which resources to download?
            val newResources = mutableListOf<OlyEntry>()
            val now = Date()
            for (resource in OlyInterface.listResources(client)) {

                if (resource.year == 1900 + now.year && resource.month == now.month + 1 && resource.day == now.date &&
                        resource.extension == "ORF") {
                    newResources.add(resource)
                }
            }

            toDownload += newResources.size
            for (resource in newResources) {

                if (hasWritePermission(downloaderService)) {
                    downloaded++
                    notification.status("Downloading from $camera", "$downloaded/$toDownload new: ${resource.filename}")

                    if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
                        notification.clearable("Error", "Storage permissions not granted")
                    } else if (resource.bytes > bytesAvailable()) {
                        notification.clearable("Error", "${resource.filename} cannot fit on storage")
                    } else if (resource.bytes > 4294967295) { // 4GB, 2^32-1. TODO: detect actual limit
                        notification.clearable("Error", "${resource.filename} exceeds 4GB limit")
                    } else {
                        // download file to tmp
                        var partial: File
                        for (i in 0 until 3) {
                            partial = getPublicFile(resource.filename + ".partial")
                            OlyInterface.download(client, resource, partial)
                            if (partial.length() != resource.bytes) {
                                Log.e(TAG, "${resource.filename}: downloaded vs. expected bytes don't match")
                            } else {
                                val file = moveDownload(resource, partial)
                                registerFile(resource, file)
                                break
                            }
                        }

                    }
                }
            }
            notification.clearable("Downloading from $camera", "$downloaded downloaded, shutting down.")

            OlyInterface.shutdown(client)
            //stopSelf()
        } catch (e: HttpHelper.NoConnection) {
            notification.clearable("Download from $camera interrupted", "$downloaded/$toDownload downloaded")
        } finally {
            //stopSelf()
        }
    }


    private fun registerFile(resource: OlyEntry, file: File) {
        val values = ContentValues()
        values.put(MediaStore.MediaColumns.DATA, file.path)
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
        values.put(MediaStore.Images.ImageColumns.TITLE, file.name)
        values.put(MediaStore.Images.ImageColumns.IS_PRIVATE, false)
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, resource.timestamp)
        values.put(MediaStore.Images.ImageColumns.MIME_TYPE, MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension))
        values.put(MediaStore.Images.ImageColumns.SIZE, file.length())
        // TODO: add LATITUDE, LONGITUDE, ORIENTATION, WIDTH, HEIGHT... THUMBNAIL <- may help strava not crash?

        downloaderService.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun moveDownload(resource: OlyEntry, partial: File): File {
        // verify download, move to position, update entry
        val file = getPublicFile(resource.filename)
        Log.d(TAG, "${resource.filename} downloaded, " + partial.length() + " bytes")
        partial.renameTo(file)
        return file
    }

    private fun bytesAvailable(): Long {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        return stat.blockSizeLong * stat.blockCountLong
    }

    private fun getPublicFile(filename: String, dir: String=Environment.DIRECTORY_PICTURES): File {
        val path = File(Environment.getExternalStoragePublicDirectory(dir), "ShotSync")
        if (!path.exists()) {
            if (!path.mkdirs()) {
                Log.e(TAG,"Directory not created; already exists")
            }
        }
        return File(Environment.getExternalStoragePublicDirectory(dir), "ShotSync/$filename")
    }
}