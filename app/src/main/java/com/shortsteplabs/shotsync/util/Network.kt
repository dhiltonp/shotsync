package com.shortsteplabs.shotsync.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.util.Log

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

const val TAG = "network"

fun bindNetwork(context: Context, network: Network) {
    if (Build.VERSION.SDK_INT >= 23) {
        val mgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        mgr.bindProcessToNetwork(network)
    } else {
        ConnectivityManager.setProcessDefaultNetwork(network)
    }
}


class NoWifi : Exception()

data class ConnectionInfo(val ssid: String, val network: Network)
fun getWifiConnection(context: Context): ConnectionInfo {
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