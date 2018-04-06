package com.shortsteplabs.shotsync

import android.content.Intent
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.toolbox.*
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
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
    private val TAG = "DownloaderService"
    private val CHANNEL_ID = TAG
    private val NOTIFICATION_ID = 21
    // TODO:
    // change to regular service, IntentService doesn't wait for volley to finish
    // refactor so that DownloaderService drives pipeline+updates notifications+saves,
    //  OlyInterface is just for camera-specific stuff.
    // create notification helper class
    // intent:
    //  downloadLoop files (need to actually store them, maybe check for free space, too?)
    //  put notification of DL status in foreground

    var downloading = false
    var queue: RequestQueue? = null

    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            if (ACTION_START_DOWNLOAD == action) {
                handleStartDownload()
            }
        }
    }

    private fun startQueue() {
        if (queue == null) {
            val cache = DiskBasedCache(cacheDir, 1024 * 1024)
            val network = BasicNetwork(HurlStack())
            queue = RequestQueue(cache, network, 1).apply {
                start()
            }
        }
    }

    private fun downloadLoop() {
        val camera = OlyInterface.getCamInfo(queue!!)
        setNotification("Starting Download", "Connected to $camera, discovering new files.")

        // which resources to downloadLoop?
        val new_resources = mutableListOf<OlyEntry>()
        val now = Date()
        for (resource in OlyInterface.listResources(queue!!)) {
            if (resource.year == 1900+now.year && resource.month == now.month+1 && resource.day == now.day+1 &&
                    resource.extension == "ORF") {
                new_resources.add(resource)
            }
        }

        var i = 0
        for (resource in new_resources) {
            i += 1
            setNotification("Downloading from $camera", "$i/${new_resources.size} new: ${resource.filename}")
            val contents = OlyInterface.download(queue!!, resource)
            if (contents.size != resource.bytes) {
                Log.e(TAG, "${resource.filename}: downloadLoop vs. expected bytes don't match")
            } else {
                Log.d(TAG, "${resource.filename} downloaded, " + contents.size + " bytes")
                if (isStorageWritable() && bytesAvailable() > resource.bytes) {
                    // TODO: actually save... is there a way to DL direct to disk? videos are huge...
                    val f = getPublicFile(resource.filename)
                    f.writeBytes(contents)
                }
            }
        }
        setNotification("Downloading from $camera", "$i downloaded, shutting down.")

        OlyInterface.shutdown(queue!!)
        stopSelf()
    }

    private fun handleStartDownload() {
        if (!downloading) {
            Log.d(TAG, "Starting downloadLoop")
            downloading = true

            setNotification("Starting Download", "Connecting to camera.")
            startQueue()

            // TODO: do the following asynchronously, stopSelf when it stops running. also allow cancellation.
            if (!OlyInterface.connect(queue!!)) {
                stopSelf()
                return
            }

            downloadLoop()
        }
    }

    fun isStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    fun bytesAvailable(): Long {
        val stat = StatFs(Environment.getExternalStorageDirectory().getPath())
        return stat.getBlockSizeLong() * stat.getBlockCountLong()
    }

    fun getPublicFile(filename: String): File {
        val dir = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "ShotSync")
        if (!dir.mkdirs()) {
            Log.e(TAG, "Directory not created")
        }
        return File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "ShotSync/$filename")
    }

    private fun setNotification(title: String, text: String) {
        Log.i(TAG, "setNotification: $title, $text")
        val mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(true)
        startForeground(NOTIFICATION_ID, mBuilder.build())
    }

    override fun onDestroy() {
        Log.d(TAG, "clearing notification")
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    companion object {
        private val ACTION_START_DOWNLOAD = "com.shortsteplabs.shotsync.action.START_DOWNLOAD"

        fun startDownload(context: Context) {
            val intent = Intent(context, DownloaderService::class.java)
            intent.action = ACTION_START_DOWNLOAD
            context.startService(intent)
        }
    }
}
