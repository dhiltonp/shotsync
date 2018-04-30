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

@Entity(indices = [(Index(value = ["time"], unique = true))])
class Location {
    @PrimaryKey()
    var time = 0L

    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var accuracy: Float = 0.0f
}

@Dao
interface LocationDao {
    @get:Query("SELECT * FROM location")
    val all: List<Location>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertNew(vararg locations: Location)

    @Query("SELECT * from location WHERE abs(time-:time)<30*60*1000 AND time in (SELECT min(time) FROM location WHERE time >= :time union SELECT max(time) from location WHERE time <= :time) order by time desc")
    fun closest(time: Long): List<Location>
}