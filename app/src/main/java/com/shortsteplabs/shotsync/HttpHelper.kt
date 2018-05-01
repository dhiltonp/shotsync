package com.shortsteplabs.shotsync

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

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

class HttpHelper {
    val TAG="HttpHelper"
    val client = OkHttpClient()

    class NoConnection(override var message:String): Exception(message)

    fun get(url: String): String {
        Log.d(TAG, "get $url")
        val request = Request.Builder()
                .url(url)
                .build()
        try {
            val response = client.newCall(request).execute()
            return response.body()!!.string()
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
            throw NoConnection(e.toString())
        }
    }

    fun fetch(url: String, file: File) {
        Log.d(TAG, "fetch $url")
        val request = Request.Builder()
                .url(url)
                .build()
        val response = client.newCall(request).execute()

        val inputStream = response.body()!!.byteStream()
        val outputStream = file.outputStream()
        val buf = ByteArray(1024*256)
        while (true) {
            // todo: test for cancellation
            try {
                val chunkBytes = inputStream.read(buf)
                if (chunkBytes == -1) {
                    if (response.code() == 503) {
                        Log.d(TAG, "503 - wait a bit...")
                        Thread.sleep(2000)
                    }
                    break
                }
                outputStream.write(buf, 0, chunkBytes)
                Log.d(TAG, "bytes written")
            } catch (e: java.net.ProtocolException) {
                Log.d(TAG, "NoConnection")
                throw NoConnection(e.toString())
            }
        }
    }
}