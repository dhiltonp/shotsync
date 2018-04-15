package com.shortsteplabs.shotsync.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import com.shortsteplabs.shotsync.R



/**
 * Created by david on 4/14/18.
 */


class SyncSettingsFragment: PreferenceFragment() {
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            getString(R.string.sync_mode_key) -> updateListSummary(key)
            getString(R.string.sync_period_key) -> updateListSummary(key)
         }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.sync_settings)

        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        prefs.registerOnSharedPreferenceChangeListener(listener)

        updateListSummary(getString(R.string.sync_mode_key))
        updateListSummary(getString(R.string.sync_period_key))
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
