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

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat.startActivity
import android.util.Log


class ConnectReceiver : BroadcastReceiver() {
    private val TAG = "ConnectReceiver"
    companion object {
        val MANUAL_START = "manual start"
    }

    private class NoWifi : Exception()

    fun detectCamera(context: Context, wifi: WifiInfo): Boolean {
        // with multiple manufacturers, return interface
        Log.d(TAG, "connecting to " + wifi.bssid + ":" + wifi.ssid)

        if (wifi.bssid.startsWith("90:b6:86")) {  // the first 6 mac address values indicate manufacturer
            Log.d(TAG, "detected olympus camera")
            return true
        }
        return false
    }

    fun hasWritePermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    fun getWifiConnection(context: Context, intent: Intent): WifiInfo {
        if (intent.action == "android.net.wifi.STATE_CHANGE") {
            val netInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
            if (netInfo.isConnected) {
                Log.d(TAG, "WIFI connected")
                return intent.getParcelableExtra<WifiInfo>(WifiManager.EXTRA_WIFI_INFO)
            }
        } else if (intent.action == MANUAL_START) {
            val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            for (network in connMgr.allNetworks) {
                val netInfo = connMgr.getNetworkInfo(network)
                if (netInfo.type == ConnectivityManager.TYPE_WIFI &&
                        netInfo.isConnected) {
                    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    Log.d(TAG, "Manual start requested, WIFI connected")
                    return wifiManager.connectionInfo
                }
            }
        }
        throw NoWifi()
    }

    override fun onReceive(context: Context, intent: Intent) {
        val wifiConnection = try { getWifiConnection(context, intent) } catch (e: NoWifi) { return }

        if (hasWritePermission(context)) {
            if (detectCamera(context, wifiConnection)) {
                DownloaderService.startDownload(context)
            }
        } else {
           if (intent.action == "android.net.wifi.STATE_CHANGE") {
               Log.d(TAG, "starting activity to request write permissions")
               startActivity(context, Intent(context, SettingsActivity::class.java), null)
           }
        }
    }

}
