package com.shortsteplabs.shotsync.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shortsteplabs.shotsync.db.Camera
import com.shortsteplabs.shotsync.db.DBFile
import com.shortsteplabs.shotsync.util.SettingsInterface
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Copyright (C) 2018  David Hilton <david.hilton.p@gmail.com>
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
        class Settings: SettingsInterface {
            override val autoOff = true
            override var autoSync = true
            override val liveShooting = true
            override var maintainUTC = true
            override val syncFiles = true
            override var syncGPS = true
            override val syncPeriod = 0L
            override val syncTime = true

            override var syncJPG = false
            override var syncRAW = false
            override var syncVID = false
        }

        val settings = Settings()
        val camera = Camera()
        val syncer = Syncer(SyncService(), settings, camera)
        val file = DBFile()

        file.extension = "jpg"
        settings.syncJPG = true
        assertEquals(true, syncer.shouldWrite(file))
        settings.syncJPG = false
        assertEquals(false, syncer.shouldWrite(file))

        file.extension = "orf"
        settings.syncRAW = true
        assertEquals(true, syncer.shouldWrite(file))
        settings.syncRAW = false
        assertEquals(false, syncer.shouldWrite(file))

        file.extension = "dng"
        settings.syncRAW = true
        assertEquals(true, syncer.shouldWrite(file))
        settings.syncRAW = false
        assertEquals(false, syncer.shouldWrite(file))

        file.extension = "avi"
        settings.syncVID = true
        assertEquals(true, syncer.shouldWrite(file))
        settings.syncVID = false
        assertEquals(false, syncer.shouldWrite(file))

        file.extension = "txt"
        settings.syncJPG = true
        settings.syncRAW = true
        settings.syncVID = true
        assertEquals(false, syncer.shouldWrite(file))
    }

    @Test
    fun hasWritePermission() {
    }

}