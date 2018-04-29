package com.shortsteplabs.shotsync.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.shortsteplabs.gpstest.LocationReceiver
import com.shortsteplabs.shotsync.wificonnected.WifiListenerService

class BootReceiver : BroadcastReceiver() {
    var TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, intent.toString())
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            WifiListenerService.startListener(context)
            LocationReceiver.startUpdates(context)
        }
    }
}
