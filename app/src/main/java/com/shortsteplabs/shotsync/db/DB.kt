package com.shortsteplabs.shotsync.db

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.migration.Migration
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


@Database(entities = [Camera::class, DBFile::class, Location::class], version = 2, exportSchema = false)
abstract class DB : RoomDatabase() {
    abstract fun cameraDao(): CameraDao
    abstract fun fileDao(): FileDao
    abstract fun locationDao(): LocationDao

    companion object : SingletonHolder<DB, Context>({
        Room.databaseBuilder(it.applicationContext,
                DB::class.java, "DB.db").addMigrations(Migration1_2())
                .build()
    })
}

class Migration1_2: Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE camera RENAME TO camera_backup")
        database.execSQL("DROP INDEX index_Camera_ssid")
        database.execSQL("CREATE TABLE camera(id INTEGER NOT NULL PRIMARY KEY, ssid TEXT NOT NULL,model TEXT NOT NULL,apiVersion TEXT NOT NULL,filesDownloaded INTEGER NOT NULL," +
                        "timeAdded INTEGER NOT NULL,timeLastSynced INTEGER NOT NULL,lastTimeZoneOffset INTEGER NOT NULL);")
        database.execSQL("CREATE UNIQUE INDEX index_Camera_ssid ON camera (ssid)")
        database.execSQL("INSERT INTO camera SELECT id,ssid,model,apiVersion,filesDownloaded," +
                "timeAdded,timeLastSynced,lastTimeZoneOffset FROM camera_backup;")
        database.execSQL("DROP TABLE camera_backup;")
    }
}

//class Migration1To2 : Migration(1,2) {
//    override fun migrate(database: SupportSQLiteDatabase) {
//        val TABLE_NAME_TEMP = "camera_new"
//
//        // 1. Create new table
//        database.execSQL("CREATE TABLE IF NOT EXISTS `$TABLE_NAME_TEMP` " +
//                "(id Int,ssid,model,apiVersion,filesDownloaded,\" +\n" +
//                "                \"timeAdded,timeLastSynced,lastTimeZoneOffset);")
//
//        // 2. Copy the data
//        database.execSQL("INSERT INTO $TABLE_NAME_TEMP (game_name) "
//                + "SELECT game_name "
//                + "FROM $TABLE_NAME")
//
//        // 3. Remove the old table
//        database.execSQL("DROP TABLE $TABLE_NAME")
//
//        // 4. Change the table name to the correct one
//        database.execSQL("ALTER TABLE $TABLE_NAME_TEMP RENAME TO $TABLE_NAME")
//    }
//}

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