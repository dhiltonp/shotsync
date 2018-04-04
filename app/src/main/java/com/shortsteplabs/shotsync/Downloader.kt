package com.shortsteplabs.shotsync

import android.app.IntentService
import android.content.Intent
import android.content.Context
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.toolbox.*
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import java.lang.Thread.sleep
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RetryPolicy




/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 *
 *
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
class Downloader : IntentService("Downloader") {
    private val TAG = "Downloader"
    private val CHANNEL_ID = "com.shortsteplabs.shotsync"
    private val NOTIFICATION_ID = 21
    // TODO:
    // change to regular service, IntentService doesn't wait for volley to finish
    // refactor so that Downloader drives pipeline+updates notifications+saves,
    //  OlyInterface is just for camera-specific stuff.
    // create notification helper class
    // intent:
    //  download files (need to actually store them, maybe check for free space, too?)
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

    private fun handleStartDownload() {
        if (!downloading) {
            Log.d(TAG, "Starting download")
            downloading = true

            val mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentTitle("snapsyncTitle")
                    .setContentText("Checking for files")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            startForeground( NOTIFICATION_ID, mBuilder.build())
            Log.d(TAG, "notification started in foreground")

            startQueue()
            sleep(2000)
            OlyInterface.Download(queue!!)
        }
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
            val intent = Intent(context, Downloader::class.java)
            intent.action = ACTION_START_DOWNLOAD
            context.startService(intent)
        }
    }
}
