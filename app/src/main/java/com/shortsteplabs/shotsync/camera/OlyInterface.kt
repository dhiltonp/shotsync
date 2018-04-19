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

package com.shortsteplabs.shotsync.camera

import android.os.SystemClock.sleep
import android.util.Log
import android.util.Xml
import com.shortsteplabs.shotsync.HttpHelper
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*



// https://en.wikipedia.org/wiki/Design_rule_for_Camera_File_system - defines clustering?
class OlyEntry constructor(entry: String, tzOffset: Long) : Comparable<OlyEntry> {
    val split = entry.split(',')
    val dirname= split[0]
    val filename = split[1]
    val path = "$dirname/$filename"
    val extension = if (filename.contains('.')) filename.split('.')[1] else ""
    val baseName = if (filename.contains('.')) filename.split('.')[0] else ""
    val bytes = split[2].toLong()
    val time: Long


    // 512 days are allocated to each year, 32 days each month.
    //  The extra 128 days (4 months worth) are added at the end of the year.
    // Raw Data, the first of each month from Jan 2002 to Jan 2003:
    //    11297, 11329, 11361, 11393, 11425, 11457, 11489, 11521, 11553, 11585, 11585, 11617, 11649, 11809
    // We do not know if the camera timezone is identical to the phone timezone :/
    //  In fact, the time is automatically set from the phone every time OIShare is connected.
    val sortdate = split[4].toInt()

    // each tick = 2 seconds. 32 are allocated to each minute, 2048 are allocated to each hour.
    val sorttime = split[5].toInt()

    val year = 1980 + sortdate / 512
    val month = (sortdate % 512) / 32
    val day = (sortdate % 32)
    val hour = sorttime / 2048
    val minute = (sorttime % 2048) / 32
    val second = (sorttime % 32) * 2

    init {
        time = GregorianCalendar(year, month, day, hour, minute, second).timeInMillis - tzOffset
    }

    override fun toString(): String {
        val formatter = SimpleDateFormat("yyyy/MM/dd, HH:mm:ss")
        val t = formatter.format(Date(time))
        return "$path: ${bytes}b, $t"
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

    /**
     * returns all resources found, ordered from oldest to newest.
     */
    fun listFiles(client: HttpHelper, tzOffset: Long=0): List<OlyEntry> {
        Log.d(TAG, "listFiles")
        val dirs = listDir(client, "/DCIM", tzOffset)

        val resources = mutableListOf<OlyEntry>()

        for (dir in dirs) {
            val result = listDir(client, dir.path, tzOffset)
            resources.addAll(result)
        }

        resources.sort()
        return resources.toList()
    }

    fun download(client: HttpHelper, resource: OlyEntry, file: File) {
        Log.d(TAG, "download")
        client.fetch("http://192.168.0.10"+ resource.path, file)
    }

    fun shutdown(client: HttpHelper) {
        Log.d(TAG, "shutdown")
        val response = client.get("http://192.168.0.10/exec_pwoff.cgi")
        Log.d(TAG, "shutdown: $response")
    }

    /**
     * attempt to connect to camera
     * raise NoConnection on failure
     */
    fun connect(client: HttpHelper, retries: Int=5) {
        Log.d(TAG, "connect")
        for (i in 1..retries) {
            try {
                getCamInfo(client)
                break
            } catch (e: HttpHelper.NoConnection) {
                Log.d(TAG, e.toString())
                if (i < retries) {
                    sleep(2000)
                    continue
                } else {
                    throw e
                }
            }
        }
    }

    fun getCamInfo(client: HttpHelper): String {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(client.get("http://192.168.0.10/get_caminfo.cgi")))

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

    fun getVersion(client: HttpHelper): String {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(client.get("http://192.168.0.10/get_commandlist.cgi")))

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.name == "oishare") {
                parser.nextTag()
                if (parser.name == "version") {
                    if (parser.next() == XmlPullParser.TEXT) {
                        return parser.text.trim()
                    }
                }
            } else {
                break
            }
        }
        return ""
    }

    private fun listDir(client: HttpHelper, path: String, tzOffset: Long): List<OlyEntry> {
        Log.d(TAG, "listDir")
        val response = client.get("http://192.168.0.10/get_imglist.cgi?DIR="+path)
        Log.d(TAG, "converting to file entries")
        val split = response.trim().split("\r\n")
        val entries = split.slice(1 until split.size).map { line -> OlyEntry(line, tzOffset) }
        Log.d(TAG, "found ${entries.size} entries")
        return entries
    }
}