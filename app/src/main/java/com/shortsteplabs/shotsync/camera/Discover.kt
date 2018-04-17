package com.shortsteplabs.shotsync.camera

import android.content.Context
import com.shortsteplabs.shotsync.HttpHelper
import com.shortsteplabs.shotsync.db.Camera
import com.shortsteplabs.shotsync.db.DB
import com.shortsteplabs.shotsync.util.NoWifi
import com.shortsteplabs.shotsync.util.bindNetwork
import com.shortsteplabs.shotsync.util.getWifiConnection
import java.util.*

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

class Discover {
    val TAG = "Discover"

    fun discover(context: Context): String {
        // detect camera on current wifi, add camera to DB, enable buttons/change text?
        val (ssid, network) = try {
            getWifiConnection(context)
        } catch (e: NoWifi) {
            return "Not connected to WiFi"
        }

        val match = DB.getInstance(context).cameraDao().findBySSID(ssid)
        if (match?.ssid != ssid) {
            bindNetwork(context, network)

            // populate data
            val camera = Camera()
            camera.ssid = ssid
            camera.timeAdded = Date().time

            val client = HttpHelper()

            try {
                camera.model = OlyInterface.getCamInfo(client)
                camera.apiVersion = OlyInterface.getVersion(client)
            } catch (e: HttpHelper.NoConnection) {
                return "Not connected to compatible camera"
            }

            DB.getInstance(context).cameraDao().insertAll(camera)
            return "Added ${camera.model}, ready to Sync!"
        } else {
            return "Already added $ssid, ready to Sync!"
        }
    }
}