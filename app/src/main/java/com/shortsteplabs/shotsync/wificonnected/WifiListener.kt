package com.shortsteplabs.shotsync.wificonnected

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.AsyncTask
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.shortsteplabs.shotsync.R
import com.shortsteplabs.shotsync.db.DB
import com.shortsteplabs.shotsync.db.autoSyncEnabled
import com.shortsteplabs.shotsync.db.getCamera
import com.shortsteplabs.shotsync.sync.SyncService.Companion.startAutoSync
import kotlinx.android.synthetic.main.content_main.*

/**
 * Copyright (C) 2018  David Hilton <david.hilton.p@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

// Wifi notifications for android >= 26/8.0/Oreo.
// todo: only show notification if any camera has autosync enabled

class WifiListenerService : Service() {
    val TAG = "WifiListenerService"
    val CHANNEL_ID = TAG
    private val WIFI_NOTIFICATION_ID = 202

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (createChannel()) {
            registerCallback()
            persistentNotification("", "Listening for new connections to cameras")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun createChannel(): Boolean {
        if (Build.VERSION.SDK_INT >= 26) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(CHANNEL_ID, "Wifi Listener", NotificationManager.IMPORTANCE_MIN)
                notificationManager.createNotificationChannel(channel)
            }
            return true
        }
        return false
    }

    fun persistentNotification(title: String, text: String) {
        Log.i(TAG, "background: $title, $text")

        val mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setLocalOnly(true)
                // Oldest notifications are at the bottom. This
                //  is 100 ms after the dawn of time.
                .setWhen(100)
                .setShowWhen(false)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)

        if (title.isNotEmpty())
            mBuilder.setContentTitle(title)
        if (text.isNotEmpty())
            mBuilder.setContentText(text)

        startForeground(WIFI_NOTIFICATION_ID, mBuilder.build())
    }

    fun registerCallback() {
        val mgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val builder = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        val requirements = builder.build()

        mgr.registerNetworkCallback(requirements, wifi(this))
    }

    class wifi(val context: Context): ConnectivityManager.NetworkCallback() {
        val TAG = "wifi listener"
        override fun onAvailable(network: Network?) {
            Log.d(TAG, "onAvailable")
            startAutoSync(context)
            super.onAvailable(network)
        }
    }
}

