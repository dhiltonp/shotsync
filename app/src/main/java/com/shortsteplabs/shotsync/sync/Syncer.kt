package com.shortsteplabs.shotsync.sync

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.webkit.MimeTypeMap
import com.shortsteplabs.shotsync.HttpHelper
import com.shortsteplabs.shotsync.R
import com.shortsteplabs.shotsync.camera.OlyEntry
import com.shortsteplabs.shotsync.camera.OlyInterface
import com.shortsteplabs.shotsync.db.Camera
import com.shortsteplabs.shotsync.db.DB
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


class Syncer(val syncService: SyncService) {
    val TAG = "Syncer"
    val notification = SyncNotification(syncService)
    val client = HttpHelper()
    var camera: Camera? = null
    var downloaded = 0

    var downloading = false

    fun cleanup() {
        notification.clearStatus()
    }

    fun startDownload(camera: Camera) {
        Log.d(TAG, "Starting downloadLoop")
//            downloading = true

        this.camera = camera


        notification.status("Starting Download", "Connecting to camera.")

        // TODO: do the following asynchronously, stopSelf when it stops running. also allow cancellation.
        try {
//            OlyInterface.connect(client)
//            downloadLoop(client)
            syncLoop()
        } catch (e: HttpHelper.NoConnection) {
            notification.clearable("Download stopped", "Unable to connect")
//                stopSelf()
            return
        }
    }

    private fun syncLoop() {
        // request new geolocation
        while (true) {
            discoverFiles() // todo: make incremental, reload if camera sync range changes
//            updateTime()
//            enableShooting?() // interactive
//            geotagFiles() // (also update file.bytes when done)
            downloadFiles()
            maybeShutdownCamera()
        }
    }

    private fun maybeShutdownCamera() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(syncService)
        val mode = prefs.getString(syncService.getString(R.string.sync_mode_key), "")
        if (mode == "Sync then Off") {
            OlyInterface.shutdown(client)
        }
    }

    private fun discoverFiles() {
        val files = mutableListOf<com.shortsteplabs.shotsync.db.File>()
        for (file in OlyInterface.listFiles(client, camera!!.lastTimeZoneOffset)) {
            val f = com.shortsteplabs.shotsync.db.File()
            f.bytes = file.bytes
            f.extension = file.extension
            f.time = file.time
            f.baseName = file.baseName
            files.add(f)
        }
        DB.getInstance(syncService).fileDao().insertNew(*files.toTypedArray())
    }

    private fun downloadFiles() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(syncService)

        val download = prefs.getBoolean(syncService.getString(R.string.sync_files_key), false)
        if (!download) return

        val period = prefs.getString(syncService.getString(R.string.sync_period_key), "0").toLong()
        val oldest = if (period > 0L) Date().time - period else 0L

        val dbFiles = mutableSetOf<String>()
        for (file in DB.getInstance(syncService).fileDao().toDownload(oldest)) {
            if (shouldWrite(file)) {
                dbFiles.add(file.filename())
            }
        }

        for (file in OlyInterface.listFiles(client)) {
            // also, cooperatively check for 'cancel' or 'stop'
            if (file.filename !in dbFiles) continue
            if (!canWrite(file.bytes)) break
            notification.status("Downloading from ${camera!!.model}", "$downloaded/${dbFiles.size} new: ${file.filename}")
            downloadFile(file)
            downloaded++
        }
    }

    fun shouldWrite(file: com.shortsteplabs.shotsync.db.File): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(syncService)
        val jpg = prefs.getBoolean(syncService.getString(R.string.sync_jpg_key), true)
        val raw = prefs.getBoolean(syncService.getString(R.string.sync_raw_key), false)
        val vid = prefs.getBoolean(syncService.getString(R.string.sync_video_key), false)

        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
        return when {
            mime == "image/jpg" -> jpg
            mime.startsWith("image/") -> raw
            mime.startsWith("video/") -> vid
            else -> false
        }
    }

    fun canWrite(bytes: Long): Boolean {
        val error: String
        if (!hasWritePermission()) {
            error = "Storage permission not granted!"
        } else if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            error = "Media not mounted!"
        } else if (bytes > bytesAvailable()) {
            error = "Low on storage!"
        } else if (bytes > 4294967295) { // 4GB, 2^32-1. TODO: detect actual limit
            error = "File too large!"
        } else {
            return true
        }
        notification.clearable("Error", error)
        return false
    }

    private fun bytesAvailable(): Long {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        return stat.blockSizeLong * stat.blockCountLong
    }

    fun hasWritePermission(): Boolean {
        return ActivityCompat.checkSelfPermission(syncService, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun downloadFile(file: OlyEntry) {
        var partial: File
        for (i in 0 until 3) {
            partial = getPublicFile(file.filename + ".partial")
            OlyInterface.download(client, file, partial)
            if (partial.length() != file.bytes) {
                Log.e(TAG, "${file.filename}: downloaded vs. expected bytes don't match")
            } else {
                val final = moveDownload(file, partial)
                registerFile(file, final)
                break
            }
        }
    }



    private fun downloadLoop(client: HttpHelper) {
//        val camera = OlyInterface.getCamInfo(client)
        notification.status("Starting Download", "Connected to ${camera!!.model}, discovering new files.")

        var toDownload = 0
        var downloaded = 0
        var currentFilename = ""
        try {
            // which resources to downloadFiles?
            val newResources = mutableListOf<OlyEntry>()
            val now = Date()
            for (resource in OlyInterface.listFiles(client, camera!!.lastTimeZoneOffset)) {
//                if (resource.year == 1900 + now.year && resource.month == now.month + 1 && resource.day == now.date &&
//                        resource.extension == "ORF") {
                    newResources.add(resource)
//                }
            }

            toDownload += newResources.size
            for (resource in newResources) {

                if (hasWritePermission()) {
                    downloaded++
                    notification.status("Downloading from ${camera!!.model}", "$downloaded/$toDownload new: ${resource.filename}")

                    if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
                        notification.clearable("Error", "Storage permissions not granted")
                    } else if (resource.bytes > bytesAvailable()) {
                        notification.clearable("Error", "${resource.filename} cannot fit on storage")
                    } else if (resource.bytes > 4294967295) { // 4GB, 2^32-1. TODO: detect actual limit
                        notification.clearable("Error", "${resource.filename} exceeds 4GB limit")
                    } else {
                        // downloadFiles file to tmp
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
            notification.clearable("Downloading from ${camera!!.model}", "$downloaded downloaded, shutting down.")

            OlyInterface.shutdown(client)
            //stopSelf()
        } catch (e: HttpHelper.NoConnection) {
            notification.clearable("Download from ${camera!!.model} interrupted", "$downloaded/$toDownload downloaded")
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
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, resource.time)
        values.put(MediaStore.Images.ImageColumns.MIME_TYPE, MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension))
        values.put(MediaStore.Images.ImageColumns.SIZE, file.length())
        // TODO: add LATITUDE, LONGITUDE, ORIENTATION, WIDTH, HEIGHT... THUMBNAIL <- may help strava not crash?

        syncService.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun moveDownload(resource: OlyEntry, partial: File): File {
        // verify downloadFiles, move to position, update entry
        val file = getPublicFile(resource.filename)
        Log.d(TAG, "${resource.filename} downloaded, " + partial.length() + " bytes")
        partial.renameTo(file)
        return file
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