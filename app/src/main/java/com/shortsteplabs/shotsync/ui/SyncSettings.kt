package com.shortsteplabs.shotsync.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import com.shortsteplabs.shotsync.R

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

class SyncSettingsFragment: PreferenceFragment() {
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            getString(R.string.sync_mode_key) -> updateListSummary(key)
            getString(R.string.sync_period_key) -> updateListSummary(key)
            getString(R.string.sync_files_key) -> Permissions(activity).requestFilePermissions()
         }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Permissions(activity).updateSyncDownloadSetting()
        addPreferencesFromResource(R.xml.sync_settings)
    }

    override fun onStart() {
        super.onStart()
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        prefs.registerOnSharedPreferenceChangeListener(listener)

        updateListSummary(getString(R.string.sync_mode_key))
        updateListSummary(getString(R.string.sync_period_key))

        // TODO: disable Download check box if we don't have file permissions
        //  related: require download check box and file permissions to actually download

    }

    override fun onStop() {
        super.onStop()
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun updateListSummary(key: String) {
        val pref = findPreference(key) as ListPreference
        val entry = pref.entry
        pref.summary = entry
    }
}

class SyncSettingsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction().replace(android.R.id.content, SyncSettingsFragment()).commit()
    }
}
