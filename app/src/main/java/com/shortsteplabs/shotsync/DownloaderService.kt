/**
Copyright (C) 2018  David Hilton <david.hilton.p@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.shortsteplabs.shotsync

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore.Images.ImageColumns.*
import android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
import android.provider.MediaStore.MediaColumns.DATA
import android.provider.MediaStore.MediaColumns.DISPLAY_NAME
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File
import java.util.*


/**
 * An [ManualIntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 *
 *
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
class DownloaderService : ManualIntentService("DownloaderService") {
    // TODO:
    // change to regular service, IntentService doesn't wait for volley to finish
    // refactor so that DownloaderService drives pipeline+updates notifications+saves,
    //  OlyInterface is just for camera-specific stuff.
    // create notification helper class
    // intent:
    //  downloadLoop files (need to actually store them, maybe check for free space, too?)
    //  put notification of DL status in foreground

    var downloading = false

    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            if (ACTION_START_DOWNLOAD == action) {
                handleStartDownload()
            }
        }
    }

    fun hasWritePermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun downloadLoop(client: HttpHelper) {
        val camera = OlyInterface.getCamInfo(client)
        statusNotification("Starting Download", "Connected to $camera, discovering new files.")

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

                if (hasWritePermission(this)) {
                    downloaded++
                    statusNotification("Downloading from $camera", "$downloaded/$toDownload new: ${resource.filename}")

                    if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
                        clearableNotification("Error", "Storage permissions not granted")
                    } else if (resource.bytes > bytesAvailable()) {
                        clearableNotification("Error", "${resource.filename} cannot fit on storage")
                    } else if (resource.bytes > 4294967295) { // 4GB, 2^32-1. TODO: detect actual limit
                        clearableNotification("Error", "${resource.filename} exceeds 4GB limit")
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
            clearableNotification("Downloading from $camera", "$downloaded downloaded, shutting down.")

            OlyInterface.shutdown(client)
            stopSelf()
        } catch (e: HttpHelper.NoConnection) {
            clearableNotification("Download from $camera interrupted", "$downloaded/$toDownload downloaded")
        } finally {
            stopSelf()
        }
    }

    private fun registerFile(resource: OlyEntry, file: File) {
        val values = ContentValues()
        values.put(DATA, file.path)
        values.put(DISPLAY_NAME, file.name)
        values.put(TITLE, file.name)
        values.put(IS_PRIVATE, false)
        values.put(DATE_TAKEN, resource.timestamp)
        values.put(MIME_TYPE, MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension))
        values.put(SIZE, file.length())
        // TODO: add LATITUDE, LONGITUDE, ORIENTATION, WIDTH, HEIGHT... THUMBNAIL <- may help strava not crash?

        this.contentResolver.insert(EXTERNAL_CONTENT_URI, values)
    }

    private fun moveDownload(resource: OlyEntry, partial: File): File {
        // verify download, move to position, update entry
        val file = getPublicFile(resource.filename)
        Log.d(TAG, "${resource.filename} downloaded, " + partial.length() + " bytes")
        partial.renameTo(file)
        return file
    }

    private fun handleStartDownload() {
        if (!downloading) {
            Log.d(TAG, "Starting downloadLoop")
            downloading = true

            statusNotification("Starting Download", "Connecting to camera.")

            val client = HttpHelper()

            // TODO: do the following asynchronously, stopSelf when it stops running. also allow cancellation.
            try {
                OlyInterface.connect(client)
                downloadLoop(client)
            } catch (e: HttpHelper.NoConnection) {
                clearableNotification("Download stopped", "Unable to connect")
                stopSelf()
                return
            }

        }
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

    private fun clearableNotification(title: String, text: String) {
        Log.i(TAG, "clearableNotification: $title, $text")
        val mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
        val notificationMgr = NotificationManagerCompat.from(this)
        notificationMgr.notify(ERROR_NOTIFICATION_TAG, errorID++, mBuilder.build())
    }

    private fun statusNotification(title: String, text: String) {
        Log.i(TAG, "statusNotification: $title, $text")
        val mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(true)
        startForeground(DOWNLOAD_NOTIFICATION_ID, mBuilder.build())
    }

    override fun onDestroy() {
        Log.d(TAG, "clearing notification")
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.cancel(DOWNLOAD_NOTIFICATION_ID)
        super.onDestroy()
    }

    companion object {
        private val TAG = "DownloaderService"
        private val CHANNEL_ID = TAG
        private val DOWNLOAD_NOTIFICATION_ID = 21
        private val ERROR_NOTIFICATION_TAG = TAG+"error notification"
        private var errorID = 1_234_789

        private val ACTION_START_DOWNLOAD = "com.shortsteplabs.shotsync.action.START_DOWNLOAD"
        private val WIFI = "wifi"

        fun startDownload(context: Context) {
            val intent = Intent(context, DownloaderService::class.java)
            intent.action = ACTION_START_DOWNLOAD
            context.startService(intent)
        }
    }
}
