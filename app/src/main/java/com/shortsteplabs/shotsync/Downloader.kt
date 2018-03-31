package com.shortsteplabs.shotsync

import android.app.IntentService
import android.content.Intent
import android.content.Context
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.toolbox.*


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

            startQueue()
            OlyInterface.Download(queue!!)
        }
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
