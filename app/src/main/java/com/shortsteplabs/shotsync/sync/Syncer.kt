package com.shortsteplabs.shotsync.sync

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
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
import com.shortsteplabs.shotsync.db.DBFile
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

class Syncer(val syncService: SyncService, val camera: Camera) {
    val TAG = "Syncer"
    val notification = SyncNotification(syncService)
    val client = HttpHelper()

    var downloading = false

    fun cleanup() {
        notification.clearStatus()
        syncService.stopSelf()
    }

    fun startSync() {
        Log.d(TAG, "Starting downloadLoop")
//            downloading = true

        notification.status("Starting Sync", "Connecting to camera.")

        // TODO: do the following asynchronously, stopSelf when it stops running. also allow cancellation.
        try {
            syncLoop()
        } catch (e: HttpHelper.NoConnection) {
            notification.clearable("Sync stopped", "Unable to connect")
        }
        cleanup()
    }

    private fun syncLoop() {
        // request new geolocation
        discoverFiles() // todo: make incremental, reload if camera sync range changes
        updateTime()
        // geotagFiles() // (also update file.bytes when done)
        if (enableShooting()) {
            while (true) {
                downloadFiles()
                Thread.sleep(10000)
                discoverFiles() // todo: make incremental, reload if camera sync range changes
            }
        } else {
            downloadFiles()
            OlyInterface.shutdown(client)
        }
//        while (true) {
//            discoverFiles() // todo: make incremental, reload if camera sync range changes
//            updateTime()
//            enableShooting()
//            geotagFiles() // (also update file.bytes when done)
//            downloadFiles()
//            if (camera.defaultSyncMode == syncService.getString(R.string.sync_then_off_value)) {
//                OlyInterface.shutdown(client)
//                break
//            }
//        }
    }

    private fun enableShooting(): Boolean {
        if (camera.defaultSyncMode == syncService.getString(R.string.sync_while_shooting_value)) {
            notification.status("Syncing with ${camera.model}", "Updating clock")
            OlyInterface.enableShooting(client)
            return true
        }
        return false
    }

    private fun updateTime() {
        notification.status("Syncing with ${camera.model}", "Updating clock")

        val date = Date()
        val tz = TimeZone.getDefault()
        if (OlyInterface.setTime(client, date, tz)) {
            camera.lastTimeZoneOffset = tz.getOffset(date.time).toLong()
            DB.getInstance(syncService).cameraDao().update(camera)
        }
    }

    private fun discoverFiles() {
        notification.status("Syncing with ${camera.model}", "Scanning available files")
        val files = mutableListOf<DBFile>()
        for (file in OlyInterface.listFiles(client, camera.lastTimeZoneOffset)) {
            val f = DBFile()
            f.bytes = file.bytes
            f.extension = file.extension
            f.time = file.time
            f.baseName = file.baseName
            files.add(f)
        }
        DB.getInstance(syncService).fileDao().insertNew(*files.toTypedArray())
    }

    private fun downloadFiles() {
        if (!camera.syncFiles) return

        notification.status("Syncing with ${camera.model}", "Selecting files for download")
        val oldest = if (camera.syncPeriod > 0L) Date().time - camera.syncPeriod else 0L

        val dbFiles = mutableMapOf<String, DBFile>()
        for (file in DB.getInstance(syncService).fileDao().toDownload(oldest)) {
            if (shouldWrite(file)) {
                dbFiles[file.filename()] = file
            }
        }

        if (dbFiles.size == 0) {
            notification.status("Syncing with ${camera.model}", "No new files to download!")
            return
        }

        var downloaded = 0
        for (file in OlyInterface.listFiles(client)) {
            // also, cooperatively check for 'cancel' or 'stop'
            if (file.filename !in dbFiles) continue
            if (!canWrite(file.bytes)) break
            notification.status("Syncing with ${camera.model}", "Downloading ${downloaded+1}/${dbFiles.size} new: ${file.filename}")
            downloadFile(file)
            dbFiles[file.filename]!!.downloaded = true
            DB.getInstance(syncService).fileDao().update(dbFiles[file.filename]!!)
            downloaded++
        }
        notification.clearable("Syncing with ${camera.model}", "$downloaded new files downloaded!")
    }

    companion object {
        val jpg = setOf("JPG", "JPEG")
        val raw = setOf("ARW", "CR2", "CRW", "DCR", "DNG", "ERF", "K25", "KDC",
                "MRW", "NEF", "ORF", "PEF", "RAF", "RAW", "SR2", "SRF", "X3F")
        val vid = setOf("mp4", "3gp", "mov", "avi", "wmv")
    }

    fun shouldWrite(file: DBFile) = when (file.extension.toUpperCase()) {
        in jpg -> camera.syncJPG
        in raw -> camera.syncRAW
        in vid -> camera.syncVID
        else -> false
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