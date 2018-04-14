package com.shortsteplabs.shotsync.ui

import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceFragment
import android.support.v7.app.AppCompatActivity
import com.shortsteplabs.shotsync.R

/**
 * Created by david on 4/14/18.
 */


class SyncSettingsFragment: PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.sync_settings)
//        registerOnSharedPreferenceChangeListener(this)

        updateListSummary("default_sync_mode")
        updateListSummary("sync_range")
    }
//
//    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
//        TODO("not implemented")
//    }

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
