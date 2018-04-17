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

package com.shortsteplabs.shotsync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.shortsteplabs.shotsync.sync.SyncService


class ConnectReceiver : BroadcastReceiver() {
    private val TAG = "ConnectReceiver"

    private fun isValidAction(intent: Intent) = when (intent.action) {
        "android.net.wifi.STATE_CHANGE" -> {
            val netInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
            netInfo.isConnected
        }
        else -> {
            false
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!isValidAction(intent)) return
        Log.d(TAG, intent.action)

        SyncService.startSync(context)
    }
}
