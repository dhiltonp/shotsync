package com.shortsteplabs.shotsync.ui

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceFragment
import android.support.v7.app.AppCompatActivity
import com.shortsteplabs.shotsync.R
import com.shortsteplabs.shotsync.util.Settings

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

const val CAMERA_ID = "Camera ID"

class MainSettingsFragment: PreferenceFragment() {
    private var settings: Settings? =  null

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            getString(R.string.sync_mode_key) -> updateListSummary(key)
            getString(R.string.sync_period_key) -> updateListSummary(key)
            getString(R.string.auto_sync_key) -> if (settings!!.autoSync) Permissions(activity).requestIgnoreBattery()
            getString(R.string.sync_jpg_key),
            getString(R.string.sync_raw_key),
            getString(R.string.sync_vid_key) -> if (settings!!.syncFiles) Permissions(activity).requestPermission(WRITE_EXTERNAL_STORAGE)
            getString(R.string.sync_gps_key) -> if (settings!!.syncGPS)
                Permissions(activity).requestPermission(ACCESS_FINE_LOCATION)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.sync_settings)
    }
    
    override fun onStart() {
        super.onStart()
        settings = Settings(activity)
        settings!!.registerChangeListener(listener)

        updateListSummary(getString(R.string.sync_mode_key))
        updateListSummary(getString(R.string.sync_period_key))
    }

    override fun onStop() {
        super.onStop()
        settings!!.unregisterChangeListener(listener)
    }

    private fun updateListSummary(key: String) {
        val pref = findPreference(key) as ListPreference
        val entry = pref.entry
        pref.summary = entry
    }
}

class MainSettingsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cameraSettings = MainSettingsFragment()

        fragmentManager.beginTransaction().replace(android.R.id.content, cameraSettings).commit()
    }
}
