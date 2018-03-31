package com.shortsteplabs.shotsync.shotsync

import android.app.IntentService
import android.content.Intent
import android.content.Context

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 *
 *
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
class Downloader : IntentService("Downloader") {
    // intent:
    //  receive that we have connected/disconnected
    //  download files
    //  put notfication of DL status in foreground

    var downloading = false

    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            if (ACTION_START_DOWNLOAD == action) {
                handleStartDownload()
            } else if (ACTION_STOP_DOWNLOAD == action) {
                handleStopDownload()
            }
        }
    }

    private fun handleStartDownload() {
        if (!downloading) {
            downloading = true
//                // If using Volley, will need to tell it to just use one downloader.
//                val queue = Volley.newRequestQueue(context);
//
//                // list images
//
//                // dl images
//                val imageRequest = StringRequest(Request.Method.GET, "http://192.168.0.10/DCIM/117OLYMP/P3272208.ORF",
//                        Response.Listener<String> { response ->
//                            Log.d(TAG, "img downloaded, " + response.length + " bytes")
//                        },
//                        Response.ErrorListener {
//                            Log.d(TAG, "img failed to download!")
//                        })
//                queue.add(imageRequest)
//
//
        }
    }

    private fun handleStopDownload() {
        if (downloading) {
            downloading = false
//                // shutdown camera:
//                // may have to also disconnect from the network to prevent having to enter password every time...
//                val stringRequest = StringRequest(Request.Method.GET, "http://192.168.0.10/exec_pwoff.cgi",
//                        Response.Listener<String> { response ->
//                            Log.d(TAG, "Camera off")
//                        },
//                        Response.ErrorListener {
//                            Log.d(TAG, "Failed to turn off camera!")
//                        })
//                // can't just queue it up, requests run in parallel
//                //queue.add(stringRequest)
        }
    }

    companion object {
        private val ACTION_START_DOWNLOAD = "com.shortsteplabs.shotsync.shotsync.action.START_DOWNLOAD"
        private val ACTION_STOP_DOWNLOAD = "com.shortsteplabs.shotsync.shotsync.action.STOP_DOWNLOAD"

        fun startDownload(context: Context) {
            val intent = Intent(context, Downloader::class.java)
            intent.action = ACTION_START_DOWNLOAD
            context.startService(intent)
        }

        fun stopDownload(context: Context) {
            val intent = Intent(context, Downloader::class.java)
            intent.action = ACTION_STOP_DOWNLOAD
            context.startService(intent)
        }
    }
}
