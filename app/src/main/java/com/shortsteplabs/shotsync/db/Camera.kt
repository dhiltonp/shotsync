package com.shortsteplabs.shotsync.db

import android.arch.persistence.room.*

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

@Entity(indices = [Index(value = ["ssid"], unique = true)])
class Camera {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    var ssid = ""
    var model = ""
    var apiVersion = ""
    var filesDownloaded = 0
    var timeAdded = 0L
    var timeLastSynced = 0L
    // offset is the number of milliseconds to add to UTC to get local time.
    // To convert from local time to UTC, *subtract* the offset.
    var lastTimeZoneOffset = 0L

    var defaultSyncMode = "Sync then Off"
    var syncFiles = true
    var syncGPS = true
    var syncTime = true

    var syncPeriod = 86400000L
    var syncJPG = true
    var syncRAW = false
    var syncVID = false

    var maintainUTC = false
}

@Dao
interface CameraDao {
    @get:Query("SELECT * FROM camera")
    val all: List<Camera>

    @Query("SELECT * FROM camera WHERE id IN (:cameraIds)")
    fun loadAllByIds(cameraIds: IntArray): List<Camera>

    @Query("SELECT * FROM camera WHERE id = :id")
    fun findByID(id: Int): Camera

    @Query("SELECT * FROM camera WHERE ssid = :ssid")
    fun findBySSID(ssid: String): Camera?

    @Insert
    fun insertAll(vararg cameras: Camera)

    @Update
    fun update(camera: Camera)

    @Delete
    fun delete(camera: Camera)
}
