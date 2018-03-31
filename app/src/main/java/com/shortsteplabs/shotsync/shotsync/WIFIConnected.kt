package com.shortsteplabs.shotsync.shotsync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.SystemClock
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

/**
 * Created by david on 3/30/18.
 */

class WIFIConnected : BroadcastReceiver() {
    private val TAG = "WIFIConnected"

    override fun onReceive(context: Context, intent: Intent) {
        val netInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
        if (netInfo.isConnected) {
            val winfo = intent.getParcelableExtra<WifiInfo>(WifiManager.EXTRA_WIFI_INFO)
            Log.d(TAG, "connecting to " + winfo.bssid + ":" + winfo.ssid)
            // or could use winfo.ssid?
            if (winfo.bssid.startsWith("90:b6:86")) {  // the first 6 mac address values indicate manufacturer
                Log.d(TAG, "detected olympus camera")
                Downloader.startDownload(context)
            }
        }
    }
}
