package com.shortsteplabs.shotsync

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File



/**
 * Created by david on 4/7/18.
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
        val buf = ByteArray(1024*64)
        while (true) {
            // todo: test for cancellation
            try {
                val chunkBytes = inputStream.read(buf)
                if (chunkBytes == -1) { break }
                outputStream.write(buf, 0, chunkBytes)
            } catch (e: java.net.ProtocolException) {
                throw NoConnection(e.toString())
            }
        }
    }
}