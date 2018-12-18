package com.shortsteplabs.shotsync.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import java.net.InetAddress

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
    val wifiMgr = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val wifiInfo = wifiMgr.connectionInfo
    if (wifiInfo != null && wifiInfo.networkId != -1) {
        Log.d(TAG, "WIFI connected")
        val ssid: String = wifiInfo.ssid
        val network = wifiInfoToNetwork(context, wifiInfo)
        return ConnectionInfo(ssid.trim('"'), network)
    }
    throw NoWifi()
}

/**
 * Take a WifiInfo, return the network.
 *
 * This is based on matching ip addresses.
 */
fun wifiInfoToNetwork(context: Context, wifiInfo: WifiInfo): Network {
    val wifiIpAddr = intToInetAddress(wifiInfo.ipAddress).toString()
    val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    for (net in connMgr.allNetworks) {
        val netInfo = connMgr.getNetworkInfo(net)
        if (netInfo.type == ConnectivityManager.TYPE_WIFI && netInfo.isConnected) {
            for (ip in connMgr.getLinkProperties(net).linkAddresses) {
                if (ip.address.toString() == wifiIpAddr)
                    return net
            }
        }
    }
    throw NoWifi()
}


/**
 * Convert a IPv4 address from an integer to an InetAddress.
 * @param hostAddress an int corresponding to the IPv4 address in network byte order
 *
 * Copied from android_tools NetworkUtils in the Android SDK (not an exposed api)
 */
fun intToInetAddress(hostAddress: Int): InetAddress {
    val addressBytes = byteArrayOf((0xff and hostAddress).toByte(), (0xff and (hostAddress shr 8)).toByte(), (0xff and (hostAddress shr 16)).toByte(), (0xff and (hostAddress shr 24)).toByte())
    return InetAddress.getByAddress(addressBytes)
}
