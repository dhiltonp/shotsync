package com.shortsteplabs.shotsync

import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.StringRequest
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
    // TODO: better error handling

    fun Download(queue: RequestQueue) {
        Log.d(TAG, "Download")
        val dirs = listDir(queue, "/DCIM")
        val resources = listResources(queue, dirs)
        queueDownloads(queue, resources)
        //queueShutdown(queue)
    }

    private fun queueDownloads(queue: RequestQueue, resources: List<OlyEntry>) {
        Log.d(TAG, "queueing downloads")
        for (resource in resources) {
            if (resource.year == 2018 && resource.month == 3 && resource.day == 31) {
                Log.d(TAG, "should download $resource")
                queueDownload(queue, resource)
            }
        }
    }

    /**
     * returns all resources found in dirs, ordered from oldest to newest.
     */
    private fun listResources(queue: RequestQueue, dirs: List<OlyEntry>): List<OlyEntry> {
        Log.d(TAG, "listResources")
        val resources = mutableListOf<OlyEntry>()

        for (dir in dirs) {
            val result = listDir(queue, dir.path)
            resources.addAll(result)
        }

        resources.sort()
        return resources.toList()
    }

    private fun listDir(queue: RequestQueue, path: String): List<OlyEntry> {
        Log.d(TAG, "listDir")
        val future = RequestFuture.newFuture<String>()
        val downloadRequest2 = StringRequest(Request.Method.GET, "http://192.168.0.10"+path, future, future)
        queue.add(downloadRequest2)
        Log.d(TAG, "reading $path")
        val response = future.get(60, TimeUnit.SECONDS)
        Log.d(TAG, "converting to file entries")
        val entries = getEntries(response)
        Log.d(TAG, "found ${entries.size} entries")
        return entries
    }

    private fun queueDownload(queue: RequestQueue, file: OlyEntry) {
        Log.d(TAG, "queueDownload")
        val downloadRequest = StringRequest(Request.Method.GET, "http://192.168.0.10"+file.path,
                Response.Listener<String> { response ->
                    if (response.length != file.bytes) {
                        Log.e(TAG, "${file.filename}: download vs. expected bytes don't match")
                    }

                    Log.d(TAG, "${file.filename} downloaded, " + response.length + " bytes")
                },
                Response.ErrorListener {
                    Log.d(TAG, "img failed to download!")
                })
        downloadRequest.tag = TAG
        queue.add(downloadRequest)
    }

    fun queueShutdown(queue: RequestQueue) {
        Log.d(TAG, "queueShutdown")
        // may have to also disconnect from the network to prevent having to enter password every time?
        val shutdownRequest = StringRequest(Request.Method.GET, "http://192.168.0.10/exec_pwoff.cgi",
                Response.Listener<String> { _ ->
                    Log.d(TAG, "Camera off")
                },
                Response.ErrorListener {
                    Log.d(TAG, "Failed to turn off camera!")
                })
        shutdownRequest.tag = TAG
        queue.add(shutdownRequest)
    }

    fun getEntries(response: String): List<OlyEntry> {
        Log.d(TAG, "Download")
        val entries = mutableListOf<OlyEntry>()
        for (line in response.split("\n"))
        {
            if (line.startsWith("wlansd[")) {
                val contents = line.split('"')[1]
                entries.add(OlyEntry(contents))
            }
        }
        return entries.toList()
    }
}