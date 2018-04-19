package com.shortsteplabs.shotsync.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

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

// singleton+argument code from:
// https://medium.com/@BladeCoder/kotlin-singletons-with-argument-194ef06edd9e
// SingletonHolder is based on 'lazy' from the Kotlin standard library


@Database(entities = [Camera::class, File::class], version = 1, exportSchema = false)
abstract class DB : RoomDatabase() {
    abstract fun cameraDao(): CameraDao
    abstract fun fileDao(): FileDao

    companion object : SingletonHolder<DB, Context>({
        Room.databaseBuilder(it.applicationContext,
                DB::class.java, "DB.db")
                .build()
    })
}

open class SingletonHolder<out T, in A>(creator: (A) -> T) {
    private var creator: ((A) -> T)? = creator
    @Volatile private var instance: T? = null

    fun getInstance(arg: A): T {
        val i = instance
        if (i != null) {
            return i
        }

        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2
            } else {
                val created = creator!!(arg)
                instance = created
                creator = null
                created
            }
        }
    }
}