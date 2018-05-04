package com.shortsteplabs.shotsync.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.shortsteplabs.shotsync.gps.LocationReceiver
import com.shortsteplabs.shotsync.wificonnected.WifiListenerService

class StartReceiver : BroadcastReceiver() {
    var TAG = "StartReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, intent.toString())
        if (intent != null
                && (intent.action == "android.intent.action.BOOT_COMPLETED"
                 || intent.action == "android.intent.action.MY_PACKAGE_REPLACED" )) {
            WifiListenerService.startListener(context)
            LocationReceiver.startUpdates(context)
        }
    }
}
