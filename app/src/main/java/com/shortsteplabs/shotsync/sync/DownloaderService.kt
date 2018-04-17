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
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.util.Log


/**
 * An [ManualIntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 *
 *
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
class DownloaderService : ManualIntentService("DownloaderService") {
    // TODO:
    // refactor so that DownloaderService manages drives pipeline+updates notifications+saves,

    var syncer: Syncer? = null

    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            if (ACTION_START_SYNC == action) {
                if (syncer == null) {
                    syncer = Syncer(this)
                    syncer!!.startDownload()
                }
            } else if (ACTION_STOP_SYNC == action) {

            } else if (ACTION_CANCEL_SYNC == action) {

            }
        }
    }

    companion object {
        private val TAG = "DownloaderService"
        private val ACTION_START_SYNC = "com.shortsteplabs.shotsync.action.START_SYNC"
        private val ACTION_STOP_SYNC = "com.shortsteplabs.shotsync.action.STOP_SYNC"
        private val ACTION_CANCEL_SYNC = "com.shortsteplabs.shotsync.action.CANCEL_SYNC"


        class NoWifi : Exception()

        private data class ConnectionInfo(val ssid: String, val network: Network)
        private fun getWifiConnection(context: Context): ConnectionInfo {
            val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            for (network in connMgr.allNetworks) {
                val netInfo = connMgr.getNetworkInfo(network)
                if (netInfo.type == ConnectivityManager.TYPE_WIFI && netInfo.isConnected) {
                    Log.d(TAG, "WIFI connected")
                    val ssid = netInfo.extraInfo.toString()
                    return ConnectionInfo(ssid.trim('"'), network)
                }
            }
            throw NoWifi()
        }

        fun bindNetwork(context: Context, network: Network) {
            if (Build.VERSION.SDK_INT >= 23) {
                val mgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                mgr.bindProcessToNetwork(network)
            } else {
                ConnectivityManager.setProcessDefaultNetwork(network)
            }
        }


        fun detectCamera(ssid: String): Boolean {
            // with multiple manufacturers, return interface
            Log.d(TAG, "connecting to $ssid")


            // todo: explicitly register SSIDs
            if (ssid.startsWith("E-M5")) {
                Log.d(TAG, "detected olympus camera")
                return true
            }
            return false
        }

        fun startSync(context: Context) {
            val (ssid, network) = try {
                getWifiConnection(context)
            } catch (e: NoWifi) {
                return
            }

            bindNetwork(context, network)

            if (detectCamera(ssid)) {
                val intent = Intent(context, DownloaderService::class.java)
                intent.action = ACTION_START_SYNC
                context.startService(intent)
            }
        }

        fun stopSync(context: Context) {
            val intent = Intent(context, DownloaderService::class.java)
            intent.action = ACTION_STOP_SYNC
            context.startService(intent)
        }

        fun cancelSync(context: Context) {
            val intent = Intent(context, DownloaderService::class.java)
            intent.action = ACTION_CANCEL_SYNC
            context.startService(intent)
        }
    }
}
