package com.shortsteplabs.shotsync.wificonnected

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    var TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, intent.toString())
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            context.startService(Intent(context, WifiListenerService::class.java))
        }
    }
}
