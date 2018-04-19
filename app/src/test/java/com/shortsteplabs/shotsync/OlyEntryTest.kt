package com.shortsteplabs.shotsync

import com.shortsteplabs.shotsync.camera.OlyEntry
import org.junit.Assert.*
import org.junit.Test

/**
 * Created by david on 3/31/18.
 */
class OlyEntryTest {
    // Samples from an E-M5 II.
    private val DIR = OlyEntry("/DCIM,117OLYMP,0,16,19347,43969", 0)
    private val ORF = OlyEntry("/DCIM/117OLYMP,P3302284.ORF,13928637,0,19582,31629", 0)
    private val JPG = OlyEntry("/DCIM/117OLYMP,PC211339.JPG,2835602,0,19349,26638", 0)

    @Test
    fun getDirname() {
        assertEquals("/DCIM", DIR.dirname)
        assertEquals("/DCIM/117OLYMP", ORF.dirname)
        assertEquals("/DCIM/117OLYMP", JPG.dirname)
    }

    @Test
    fun getFilename() {
        assertEquals("117OLYMP", DIR.filename)
        assertEquals("P3302284.ORF", ORF.filename)
        assertEquals("PC211339.JPG", JPG.filename)
    }

    @Test
    fun getExtension() {
        assertEquals("", DIR.extension)
        assertEquals("ORF", ORF.extension)
        assertEquals("JPG", JPG.extension)
    }

    @Test
    fun getPath() {
        assertEquals("/DCIM/117OLYMP", DIR.path)
        assertEquals("/DCIM/117OLYMP/P3302284.ORF", ORF.path)
        assertEquals("/DCIM/117OLYMP/PC211339.JPG", JPG.path)
    }

    @Test
    fun getBytes() {
        assertEquals(0, DIR.bytes)
        assertEquals(13928637, ORF.bytes)
        assertEquals(2835602, JPG.bytes)
    }

    @Test
    fun getSortdate() {
        assertEquals(19347, DIR.sortdate)
        assertEquals(19582, ORF.sortdate)
        assertEquals(19349, JPG.sortdate)
    }

    @Test
    fun getSorttime() {
        assertEquals(43969, DIR.sorttime)
        assertEquals(31629, ORF.sorttime)
        assertEquals(26638, JPG.sorttime)
    }

    private val jan1_2002 = OlyEntry(",,0,0,11297,0", 0)
    private val dec1_2002 = OlyEntry(",,0,0,11649,0", 0)
    private val jan1_2003 = OlyEntry(",,0,0,11809,0", 0)

    @Test
    fun getYear() {
        assertEquals(2002, jan1_2002.year)
        assertEquals(2002, dec1_2002.year)
        assertEquals(2003, jan1_2003.year)
    }

    @Test
    fun getMonth() {
        assertEquals(1, jan1_2002.month)
        assertEquals(12, dec1_2002.month)
        assertEquals(1, jan1_2003.month)
    }

    @Test
    fun getDay() {
        assertEquals(1, jan1_2002.day)
        assertEquals(1, dec1_2002.day)
        assertEquals(1, jan1_2003.day)

        val may21_2002 = OlyEntry(",,0,0,11445,0", 0)
        assertEquals(21, may21_2002.day)
    }

    // tH_MM_SS
    private val t0_00_00 = OlyEntry(",,0,0,0,0", 0)
    private val t0_01_00 = OlyEntry(",,0,0,0,32", 0)
    private val t0_02_00 = OlyEntry(",,0,0,0,64", 0)
    private val t0_59_00 = OlyEntry(",,0,0,0,1888", 0)
    private val t1_00_00 = OlyEntry(",,0,0,0,2048", 0)
    private val t23_59_58 = OlyEntry(",,0,0,0,49021", 0)

    @Test
    fun getHour() {
        assertEquals(0, t0_00_00.hour)
        assertEquals(0, t0_59_00.hour)
        assertEquals(1, t1_00_00.hour)
        assertEquals(23, t23_59_58.hour)
    }

    @Test
    fun getMinute() {
        assertEquals(0, t0_00_00.minute)
        assertEquals(1, t0_01_00.minute)
        assertEquals(2, t0_02_00.minute)
        assertEquals(59, t0_59_00.minute)
        assertEquals(0, t1_00_00.minute)
        assertEquals(59, t23_59_58.minute)
    }

    @Test
    fun getSecond() {
        assertEquals(0, t0_00_00.second)
        assertEquals(0, t0_01_00.second)
        assertEquals(0, t0_02_00.second)
        assertEquals(0, t0_59_00.second)
        assertEquals(0, t1_00_00.second)
        assertEquals(58, t23_59_58.second)
    }

    @Test
    fun compareTo() {
        val fileA = OlyEntry(",A,0,0,0,0", 0)
        val fileB = OlyEntry(",B,0,0,0,0", 0)
        assertTrue(t0_00_00 < t1_00_00)
        assertTrue(jan1_2002 < jan1_2003)
        assertTrue(fileA<fileB)

        // date is more significant than time
        val before = OlyEntry(",,0,0,0,1", 0)
        val after = OlyEntry(",,0,0,10,0", 0)
        assertTrue(before < after)

        // time is more significant than filename
        val fileA1 = OlyEntry(",A,0,0,0,1", 0)
        val fileB1 = OlyEntry(",B,0,0,0,0", 0)
        assertTrue(fileB1<fileA1)

        // identical entries are identical
        assertFalse(DIR<DIR)
    }
}