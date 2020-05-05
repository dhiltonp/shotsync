package com.shortsteplabs.shotsync.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log
import com.shortsteplabs.shotsync.R
import com.shortsteplabs.shotsync.sync.ManualIntentService

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

class SyncNotification(val service: ManualIntentService) {
    private val CHANNEL_ID = TAG
    private val DOWNLOAD_NOTIFICATION_ID = 21
    private val ERROR_NOTIFICATION_TAG = "$TAG error"
    private var errorID = 1_234_789

    companion object {
        const val TAG = "SyncNotification"
    }

    init {
        if (Build.VERSION.SDK_INT >= 26) {
            val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(CHANNEL_ID, "Sync Status", NotificationManager.IMPORTANCE_NONE)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun clearStatus() {
        val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(DOWNLOAD_NOTIFICATION_ID)
    }

    fun clearable(title: String, text: String) {
        Log.i(TAG, "clearable: $title, $text")
        val mBuilder = NotificationCompat.Builder(service, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
        val notificationMgr = NotificationManagerCompat.from(service)
        notificationMgr.notify(ERROR_NOTIFICATION_TAG, errorID++, mBuilder.build())
    }

    fun status(title: String, text: String) {
        Log.i(TAG, "status: $title, $text")
        val mBuilder = NotificationCompat.Builder(service, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(true)
        service.startForeground(DOWNLOAD_NOTIFICATION_ID, mBuilder.build())
    }
}