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
    val sortDate = split[4].toInt()

    // each tick = 2 seconds. 32 are allocated to each minute, 2048 are allocated to each hour.
    val sortTime = split[5].toInt()

    val year = 1980 + sortDate / 512
    val month = (sortDate % 512) / 32
    val day = (sortDate % 32)
    val hour = sortTime / 2048
    val minute = (sortTime % 2048) / 32
    val second = (sortTime % 32) * 2

    init {
        val cal = GregorianCalendar(year, month-1, day, hour, minute, second)
        cal.timeZone = TimeZone.getTimeZone("UTC")
        time = cal.timeInMillis - tzOffset
    }

    override fun toString(): String {
        val formatter = SimpleDateFormat("yyyy/MM/dd, HH:mm:ss")
        val t = formatter.format(Date(time))
        return "$path: ${bytes}b, $t"
    }

    override fun compareTo(other: OlyEntry) = when {
        sortDate != other.sortDate -> sortDate - other.sortDate
        sortTime != other.sortTime -> sortTime - other.sortTime
        filename != other.filename -> filename.compareTo(other.filename)
        else -> 0
    }
}

object OlyInterface {
    private const val TAG = "OlyInterface"

    /**
     * returns all resources found, ordered from oldest to newest.
     */
    fun listFiles(client: HttpHelper, tzOffset: Long=0): List<OlyEntry> {
        Log.d(TAG, "listFiles")
        val dirs = listDirWeb(client, "/DCIM", tzOffset)

        val resources = mutableListOf<OlyEntry>()

        for (dir in dirs) {
            val result = listDirWeb(client, dir.path, tzOffset)
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

    fun enableShooting(client: HttpHelper) {
        Log.d(TAG, "enableShooting")
        val response = client.get("http://192.168.0.10/switch_cammode.cgi?mode=shutter")
        val i = 0
    }

    fun setTime(client: HttpHelper, date: Date, timeZone: TimeZone): Boolean {
        Log.d(TAG, "setTime")

        val utc = TimeZone.getTimeZone("UTC")
        val timeFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss")
        timeFormat.timeZone = utc
        val timeStr = timeFormat.format(date.time)

        val zoneFormat = SimpleDateFormat("Z")
        zoneFormat.timeZone = timeZone
        val tzStr = zoneFormat.format(date)
        val response = client.get("http://192.168.0.10/set_utctimediff.cgi?utctime=$timeStr&diff=$tzStr")

        if (response != "") {
            return true
        }

        return false
    }

    //get_gpsdivunit - returns 500 on my e-m5 ii
    fun req_attachexifgps(client: HttpHelper) {

    }

    fun req_storegpsinfo(client: HttpHelper) {
        // mode=new
        // date=


        /*
maximum delay between samples in official app: 60s

2 files - log and sns. sns is custom/proprietary. Data seems optional, file may be required?

log file format sample:

@Olympus/-0500/-0500
$GPGGA,012323.1,3950.5160,N,08248.5405,W,1,00,00.00,0.0,M,0,M,0,*5f
$GPRMC,012323,A,3950.5160,N,08248.5405,W,000.0,000.0,290418,00,*7
$GPGGA,012333.9,3950.5162,N,08248.5402,W,1,00,00.00,0.0,M,0,M,0,*53
$GPRMC,012333,A,3950.5162,N,08248.5402,W,000.0,000.0,290418,00,*3

http://aprs.gids.nl/nmea/#gga
http://aprs.gids.nl/nmea/#rmc
so:

$GPGGA,$time,$latitude,$NS,$longitude,$EW,1,00,00.00,0.0,M,0,M,0,*$checksum
$GPRMC,$time,A,$latitude,$NS,$longitude,$EW,000.0,000,$date,00,*$checksum


checksum:
https://rietman.wordpress.com/2008/09/25/how-to-calculate-the-nmea-checksum/

To calculate the checksum you parse all characters between $ and * from the NMEA
sentence into a new string.  In the examples below the name of this new string
is stringToCalculateTheChecksumOver. Then just XOR the first character with the
next character, until the end of the string.


https://electronics.stackexchange.com/questions/288521/olympus-tg-tracker-what-does-this-data-stand-for/288527#288527
my sns:
@Olympus/-0500/-0500
$OLTIM,20180430,023819
$OLCMP,
$OLPRE,998,129.8,425.8
$OLTMP,
$OLACC,,,

their sns
$OLTIM,20170224,045641// Time obviously, no probs here
$OLCMP,23.7 // Compass
$OLPRE,963,421.2,1389.9 // Pressure
$OLTMP,20.5,1,0 // Temperature in F's, no probs here
$OLACC,-1007.1,-3.6,4.7 // Acceleration (vector, 1G down)

         */

//<http_method type="post">
//      <cmd1 name="mode">
//	<param1 name="new"/>
//	<param1 name="append"/>
//	<cmd2 name="date"/>
//      </cmd1>
//    </http_method>
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
        val response = client.get("http://192.168.0.10/get_imglist.cgi?DIR=$path")
        Log.d(TAG, "converting to file entries")
        val split = response.trim().split("\r\n")
        val entries = split.slice(1 until split.size).map { line -> OlyEntry(line, tzOffset) }
        Log.d(TAG, "found ${entries.size} entries")
        return entries
    }

    private fun listDirWeb(client: HttpHelper, path: String, tzOffset: Long): List<OlyEntry> {
        Log.d(TAG, "listDir")
        val r = try {
            client.get("http://192.168.0.10/$path")
        } catch (e: HttpHelper.NoConnection) {
            if (e.code == 404) { // no files, or no camera... btw, 520=camera api is busy
                Log.d(TAG, "404, may be connected with 0 files")
                return emptyList()
            }
            throw e
        }

        Log.d(TAG, "converting to file entries")

        val keepers = mutableListOf<OlyEntry>()
        for (l in r.split('\n')) {
            if (l.startsWith("wlansd[")) {
                val entry = l.split('"')[1]
                keepers.add(OlyEntry(entry, tzOffset))
            }
        }
        return keepers
    }
}