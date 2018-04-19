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

import android.content.Context
import android.content.Intent
import com.shortsteplabs.shotsync.db.DB
import com.shortsteplabs.shotsync.util.NoWifi
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
            if (ACTION_START_SYNC == action) {
                handleStartSync()
            } else if (ACTION_STOP_SYNC == action) {

            } else if (ACTION_CANCEL_SYNC == action) {

            }
        }
    }

    fun handleStartSync() {
        val (ssid, network) = try {
            getWifiConnection(this)
        } catch (e: NoWifi) {
            return
        }

        if (syncer == null) {
            val camera = DB.getInstance(this).cameraDao().findBySSID(ssid)
            if (camera != null) {
                bindNetwork(this, network)
                syncer = Syncer(this, camera)
                syncer!!.startSync()
            }
        }
    }

    companion object {
        private val TAG = "SyncService"
        private val ACTION_START_SYNC = "com.shortsteplabs.shotsync.action.START_SYNC"
        private val ACTION_STOP_SYNC = "com.shortsteplabs.shotsync.action.STOP_SYNC"
        private val ACTION_CANCEL_SYNC = "com.shortsteplabs.shotsync.action.CANCEL_SYNC"
        private val ACTION_DISCOVER_CAMERA = "com.shortsteplabs.shotsync.action.ACTION_DISCOVER_CAMERA"

        fun startSync(context: Context) {
            val intent = Intent(context, SyncService::class.java)
            intent.action = ACTION_START_SYNC
            context.startService(intent)
        }

        fun stopSync(context: Context) {
            val intent = Intent(context, SyncService::class.java)
            intent.action = ACTION_STOP_SYNC
            context.startService(intent)
        }

        fun cancelSync(context: Context) {
            val intent = Intent(context, SyncService::class.java)
            intent.action = ACTION_CANCEL_SYNC
            context.startService(intent)
        }

        fun discover(context: Context) {
            val intent = Intent(context, SyncService::class.java)
            intent.action = ACTION_DISCOVER_CAMERA
            context.startService(intent)
        }
    }
}
