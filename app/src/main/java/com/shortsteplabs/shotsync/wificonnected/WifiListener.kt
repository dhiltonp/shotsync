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
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.shortsteplabs.shotsync.R
import com.shortsteplabs.shotsync.sync.SyncService

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

class WifiListenerService : Service() {
    val TAG = "WifiListenerService"
    val CHANNEL_ID = TAG
    private val WIFI_NOTIFICATION_ID = 202

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= 26) {
            createChannel()
            // todo: only register callback and set notification if autoSync = true
            registerCallback()
            persistentNotification("", "Listening for new connections to cameras")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(CHANNEL_ID, "Wifi Listener", NotificationManager.IMPORTANCE_MIN)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun persistentNotification(title: String, text: String) {
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

    @RequiresApi(Build.VERSION_CODES.M)
    private fun registerCallback() {
        val mgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // the builder's setNetworkSpecifier might some day be used to allow registering only
        //  specific SSIDs. As of 8.1/27, only one WIFI network type uses it, a form of P2P:
        //  WifiAwareSession().createNetworkSpecifierOpen(mac_address).
        val requirements = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

        mgr.registerNetworkCallback(requirements, Wifi(this))
    }

    class Wifi(val context: Context): ConnectivityManager.NetworkCallback() {
        val TAG = "wifi listener"
        override fun onAvailable(network: Network?) {
            Log.d(TAG, "onAvailable")
            SyncService.autoSyncIntent(context).send()
            super.onAvailable(network)
        }
    }

    companion object {
        fun startListener(context: Context) {
            context.startService(Intent(context, WifiListenerService::class.java))
        }
    }
}

