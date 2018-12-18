package com.shortsteplabs.shotsync.util

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.shortsteplabs.shotsync.R
import com.shortsteplabs.shotsync.R.string.*

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


interface SettingsInterface {
    val syncPeriod: Long
    val syncFiles: Boolean
    val autoOff: Boolean
    val liveShooting: Boolean
    var autoSync: Boolean
    var syncJPG: Boolean
    var syncRAW: Boolean
    var syncVID: Boolean
    var syncGPS: Boolean
    var maintainUTC: Boolean
    val syncTime: Boolean
}

// Settings workflow: automatically updated via ui.
//  When it's time for a Sync, copy the stored settings as a default
//  then apply local changes on top as necessary...

class Settings(val context: Context): SettingsInterface {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)!!
    var async = false

    fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun getBoolean(key: Int, default: Boolean): Boolean {
        return prefs.getBoolean(context.getString(key), default)
    }

    private fun setBoolean(key: Int, value: Boolean) {
        with (prefs.edit()) {
            putBoolean(context.getString(key), false)
            if (async) {
                apply()
            } else {
                commit()
            }
        }
    }

    override val syncPeriod: Long
        get() = prefs.getString(context.getString(R.string.sync_period_key), "86400000").toLong()

    override val syncFiles: Boolean
        get() = syncJPG || syncRAW || syncVID


    override val autoOff: Boolean
        get() = prefs.getString(context.getString(R.string.sync_mode_key), "") == context.getString(R.string.sync_then_off_value)

    override val liveShooting: Boolean
        get() = prefs.getString(context.getString(R.string.sync_mode_key), "") != context.getString(R.string.sync_then_off_value)

    override var autoSync: Boolean
        get() = getBoolean(auto_sync_key, true)
        set(value) = setBoolean(auto_sync_key, value)

    override var syncJPG: Boolean
        get() = getBoolean(sync_jpg_key, true)
        set(value) = setBoolean(sync_jpg_key, value)

    override var syncRAW: Boolean
        get() = getBoolean(sync_raw_key, false)
        set(value) = setBoolean(sync_raw_key, value)

    override var syncVID: Boolean
        get() = getBoolean(sync_vid_key, false)
        set(value) = setBoolean(sync_vid_key, value)

    override var syncGPS: Boolean
        get() = getBoolean(sync_gps_key, true)
        set(value) = setBoolean(sync_gps_key, value)

    override var maintainUTC: Boolean
        get() = getBoolean(maintain_utc_key, false)
        set(value) = setBoolean(maintain_utc_key, value)

    override val syncTime = true

//    var syncFiles = true
//    var syncTime = true

}