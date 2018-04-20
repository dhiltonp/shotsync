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


@Entity(indices = [(Index(value = ["baseName", "extension", "bytes"], unique = true))])
class DBFile {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    // base name==DCF Object, see:
    // https://en.wikipedia.org/wiki/Design_rule_for_Camera_File_system#DCF_objects
    var baseName = ""
    var extension = ""

    var bytes = 0L
    var time = 0L  // time-zone agnostic. Account for file time timezones during conversion.

    var downloaded = false
    var geotagged = false

    fun filename(): String {
        return "$baseName.$extension"
    }
}

@Dao
interface FileDao {
    @get:Query("SELECT * FROM dbFile")
    val all: List<DBFile>

    @Query("SELECT * FROM dbFile WHERE time >= :oldest AND NOT downloaded ORDER BY time, baseName")
    fun toDownload(oldest: Long): List<DBFile>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertNew(vararg files: DBFile)

    @Update
    fun update(file: DBFile)

    @Delete
    fun delete(file: DBFile)
}
