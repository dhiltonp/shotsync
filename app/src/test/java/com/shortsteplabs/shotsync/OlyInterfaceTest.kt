package com.shortsteplabs.shotsync

import com.android.volley.RequestQueue
import io.mockk.mockk
import org.junit.Test

/**
 * Created by david on 3/31/18.
 */
class OlyInterfaceTest {

    @Test
    fun getEntries() {
        val data = """
blablabla
wlansd = new Array()
wlansd[0]="/DCIM,117OLYMP,0,16,19347,43969";
wlansd[1]="/DCIM/117OLYMP,P3302282.ORF,13928637,0,19582,31629";
wlansd[2]="/DCIM/117OLYMP,P3302282.ORF,13928637,0,19582,31629";
wlansd[3]="/DCIM/117OLYMP,P3302282.ORF,13928637,0,19582,31629";
blablabla
"""
        assert(OlyInterface.getEntries(data).size == 4)
    }

    @Test
    fun queueShutdown() {
        val queueMock = mockk<RequestQueue>()
        OlyInterface.shutdown(queueMock)

    }
}