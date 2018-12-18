/**
Copyright (C) 2018  David Hilton <david.hilton.p@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.shortsteplabs.shotsync.sync

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.shortsteplabs.shotsync.db.DB
import com.shortsteplabs.shotsync.util.NoWifi
import com.shortsteplabs.shotsync.util.Settings
import com.shortsteplabs.shotsync.util.bindNetwork
import com.shortsteplabs.shotsync.util.getWifiConnection


/**
 * An [ManualIntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 *
 *
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
class SyncService : ManualIntentService("SyncService") {
    // TODO:
    // refactor so that SyncService manages drives pipeline+updates notifications+saves,

    var syncer: Syncer? = null

    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            when {
                ACTION_START_SYNC == action -> handleStartSync(false)
                ACTION_START_AUTO_SYNC == action -> handleStartSync(true)
                ACTION_STOP_SYNC == action -> {

                }
                ACTION_CANCEL_SYNC == action -> {

                }
            }
        }
    }

    fun handleStartSync(auto: Boolean) {
        val (ssid, network) = try {
            getWifiConnection(this)
        } catch (e: NoWifi) {
            return
        }

        if (syncer == null) {
            val settings = Settings(this)
            val camera = DB.getInstance(this).cameraDao().findBySSID(ssid)
            if (camera != null) {
                if (auto && !settings.autoSync) return
                bindNetwork(this, network)
                syncer = Syncer(this, settings, camera)
                syncer!!.startSync()
            }
        }
    }

    companion object {
        private const val TAG = "SyncService"
        private const val ACTION_START_SYNC = "com.shortsteplabs.shotsync.action.START_SYNC"
        private const val ACTION_START_AUTO_SYNC = "com.shortsteplabs.shotsync.action.START_AUTO_SYNC"
        private const val ACTION_STOP_SYNC = "com.shortsteplabs.shotsync.action.STOP_SYNC"
        private const val ACTION_CANCEL_SYNC = "com.shortsteplabs.shotsync.action.CANCEL_SYNC"

        fun syncIntent(context: Context): PendingIntent {
            val intent = Intent(context, SyncService::class.java)
            intent.action = ACTION_START_SYNC
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        fun autoSyncIntent(context: Context): PendingIntent {
            val intent = Intent(context, SyncService::class.java)
            intent.action = SyncService.ACTION_START_AUTO_SYNC
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        fun stopSyncIntent(context: Context): PendingIntent {
            val intent = Intent(context, SyncService::class.java)
            intent.action = ACTION_STOP_SYNC
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        fun cancelSyncIntent(context: Context): PendingIntent {
            val intent = Intent(context, SyncService::class.java)
            intent.action = ACTION_CANCEL_SYNC
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}
