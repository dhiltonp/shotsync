package com.shortsteplabs.shotsync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.util.Log

/**
 * Created by david on 3/30/18.
 */

class ConnectReceiver : BroadcastReceiver() {
    private val TAG = "ConnectReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.net.wifi.STATE_CHANGE") {
            val netInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
            if (netInfo.isConnected) {
                val winfo = intent.getParcelableExtra<WifiInfo>(WifiManager.EXTRA_WIFI_INFO)
                Log.d(TAG, "connecting to " + winfo.bssid + ":" + winfo.ssid)

                if (winfo.bssid.startsWith("90:b6:86")) {  // the first 6 mac address values indicate manufacturer
                    Log.d(TAG, "detected olympus camera")
                    Downloader.startDownload(context)
                }
            }
        }
    }
}
