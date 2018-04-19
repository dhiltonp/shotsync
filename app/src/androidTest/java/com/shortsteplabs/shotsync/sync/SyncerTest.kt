package com.shortsteplabs.shotsync.sync

//  import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.shortsteplabs.shotsync.db.Camera
import com.shortsteplabs.shotsync.db.File
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Copyright (C) 2018  David Hilton <david.hilton.p></david.hilton.p>@gmail.com>
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https:></https:>//www.gnu.org/licenses/>.
 */

@RunWith(AndroidJUnit4::class)
class SyncerTest {
    @Test
    fun startDownload() {
    }

    @Test
    fun canWrite() {
    }

    @Test
    fun shouldWrite() {
        val camera = Camera()
        val syncer = Syncer(SyncService(), camera)
        val file = File()

        file.extension = "jpg"
        camera.syncJPG = true
        assertEquals(true, syncer.shouldWrite(file))
        camera.syncJPG = false
        assertEquals(false, syncer.shouldWrite(file))

        file.extension = "orf"
        camera.syncRAW = true
        assertEquals(true, syncer.shouldWrite(file))
        camera.syncRAW = false
        assertEquals(false, syncer.shouldWrite(file))

        file.extension = "dng"
        camera.syncRAW = true
        assertEquals(true, syncer.shouldWrite(file))
        camera.syncRAW = false
        assertEquals(false, syncer.shouldWrite(file))

        file.extension = "avi"
        camera.syncVID = true
        assertEquals(true, syncer.shouldWrite(file))
        camera.syncVID = false
        assertEquals(false, syncer.shouldWrite(file))

        file.extension = "txt"
        camera.syncJPG = true
        camera.syncRAW = true
        camera.syncVID = true
        assertEquals(false, syncer.shouldWrite(file))
    }

    @Test
    fun hasWritePermission() {
    }

}