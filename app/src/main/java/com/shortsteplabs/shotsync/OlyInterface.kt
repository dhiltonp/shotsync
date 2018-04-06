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

import android.os.SystemClock.sleep
import android.util.Log
import android.util.Xml
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.StringRequest
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.concurrent.TimeUnit

/**
 * Created by david on 3/31/18.
 */

class OlyEntry constructor(entry: String) : Comparable<OlyEntry> {
    private val split = entry.split(',')
    val dirname = split[0]
    val filename = split[1]
    val extension = if (filename.contains('.')) filename.split('.')[1] else ""
    val path = "$dirname/$filename"
    val bytes = split[2].toInt()

    // 512 days are allocated to each year, 32 days each month.
    //  The extra 128 days (4 months worth) are added at the end of the year.
    // Raw Data, the first of each month from Jan 2002 to Jan 2003:
    //    11297, 11329, 11361, 11393, 11425, 11457, 11489, 11521, 11553, 11585, 11585, 11617, 11649, 11809
    // We do not know if the camera timezone is identical to the phone timezone :/
    //  In fact, the time is automatically set from the phone every time OIShare is connected.
    val sortdate = split[4].toInt()

    // each tick = 2 seconds. 32 are allocated to each minute, 2048 are allocated to each hour.
    val sorttime = split[5].toInt()

    val year = 1980 + sortdate/512
    val month = (sortdate % 512) / 32
    val day = (sortdate % 32)
    val hour = sorttime / 2048
    val minute = (sorttime % 2048) / 32
    val second = (sorttime % 32) * 2

    // TODO: put the above into a standard date/time object

    override fun toString(): String {
        return "$path: ${bytes}b, $year/$month/$day, $hour:$minute:$second,"
    }

    override fun compareTo(other: OlyEntry) = when {
        sortdate != other.sortdate -> sortdate - other.sortdate
        sorttime != other.sorttime -> sorttime - other.sorttime
        filename != other.filename -> filename.compareTo(other.filename)
        else -> 0
    }
}

object OlyInterface {
    private val TAG = "OlyInterface"

    class NoConnection: Exception()

    /**
     * returns all resources found in dirs, ordered from oldest to newest.
     */
    fun listResources(queue: RequestQueue): List<OlyEntry> {
        Log.d(TAG, "listResources")
        val dirs = OlyInterface.listDir(queue!!, "/DCIM")

        val resources = mutableListOf<OlyEntry>()

        for (dir in dirs) {
            val result = listDir(queue, dir.path)
            resources.addAll(result)
        }

        resources.sort()
        return resources.toList()
    }

    fun get(queue: RequestQueue, url: String, retries: Int=3, timeout: Long=60): String {
        Log.d(TAG, "get $url")
        val future = RequestFuture.newFuture<String>()

        val downloadRequest = StringRequest(Request.Method.GET, url, future, future)
        downloadRequest.retryPolicy = DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                retries,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        downloadRequest.tag = TAG

        queue.add(downloadRequest)
        // success vs. failure? 2 futures?
        return future.get(timeout, TimeUnit.SECONDS)
    }

    fun fetch(queue: RequestQueue, url: String, retries: Int=3, timeout: Long=60): ByteArray {
        Log.d(TAG, "get $url")
        val future = RequestFuture.newFuture<ByteArray>()

        val downloadRequest = InputStreamVolleyRequest(Request.Method.GET, url, future, future, null)
        downloadRequest.retryPolicy = DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                retries,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        downloadRequest.tag = TAG

        queue.add(downloadRequest)
        // success vs. failure? 2 futures?
        return future.get(timeout, TimeUnit.SECONDS)
    }

    private fun listDir(queue: RequestQueue, path: String): List<OlyEntry> {
        Log.d(TAG, "listDir")
        val response = get(queue, "http://192.168.0.10/get_imglist.cgi?DIR="+path)
        Log.d(TAG, "converting to file entries")
        val split = response.trim().split("\r\n")
        val entries = split.slice(1 until split.size).map { line -> OlyEntry(line) }
        Log.d(TAG, "found ${entries.size} entries")
        return entries
    }



    fun download(queue: RequestQueue, file: OlyEntry): ByteArray {
        Log.d(TAG, "download")
        return fetch(queue, "http://192.168.0.10"+file.path)
    }

    fun shutdown(queue: RequestQueue) {
        Log.d(TAG, "shutdown")
        val response = get(queue, "http://192.168.0.10/exec_pwoff.cgi")
        Log.d(TAG, "shutdown: $response")
    }

    /**
     * attempt to connect to camera
     * return true on success
     */
    fun connect(queue: RequestQueue, retries: Int=5): Boolean {
        Log.d(TAG, "connect")
        for (i in 1..retries) {
            try {
                return getCamInfo(queue) != ""
            } catch (e: java.util.concurrent.ExecutionException) {
                // no route to host sometimes happens? not sure why... Olympus app still works? confusing.
                Log.d(TAG, e.toString())
                sleep(2000)
                continue
            }
        }
        return false
    }

    fun getCamInfo(queue: RequestQueue): String {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(get(queue, "http://192.168.0.10/get_caminfo.cgi")))

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.name == "caminfo") {
                parser.nextTag()
                if (parser.name == "model") {
                    if (parser.next() == XmlPullParser.TEXT) {
                        return parser.text
                    }
                }
            } else {
                break
            }
        }
        return ""
    }
}