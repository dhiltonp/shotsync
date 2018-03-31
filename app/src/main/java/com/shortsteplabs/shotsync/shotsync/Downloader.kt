package com.shortsteplabs.shotsync.shotsync

import android.app.IntentService
import android.content.Intent
import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
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
    //  receive that we have connected/disconnected
    //  download files
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

            // list images

            // dl images
            val imageRequest = StringRequest(Request.Method.GET, "http://192.168.0.10/DCIM/117OLYMP/P3272208.ORF",
                    Response.Listener<String> { response ->
                        Log.d(TAG, "img downloaded, " + response.length + " bytes")
                    },
                    Response.ErrorListener {
                        Log.d(TAG, "img failed to download!")
                    })
            imageRequest.tag = TAG
            queue?.add(imageRequest)

            // may have to also disconnect from the network to prevent having to enter password every time...
            val stringRequest = StringRequest(Request.Method.GET, "http://192.168.0.10/exec_pwoff.cgi",
                    Response.Listener<String> { response ->
                        Log.d(TAG, "Camera off")
                    },
                    Response.ErrorListener {
                        Log.d(TAG, "Failed to turn off camera!")
                    })
            imageRequest.tag = TAG
            queue?.add(stringRequest)
        }
    }

    companion object {
        private val ACTION_START_DOWNLOAD = "com.shortsteplabs.shotsync.shotsync.action.START_DOWNLOAD"

        fun startDownload(context: Context) {
            val intent = Intent(context, Downloader::class.java)
            intent.action = ACTION_START_DOWNLOAD
            context.startService(intent)
        }
    }
}
