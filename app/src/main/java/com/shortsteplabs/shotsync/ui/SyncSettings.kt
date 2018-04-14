package com.shortsteplabs.shotsync.ui

import android.os.Bundle
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
    }
}

class SyncSettingsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction().replace(android.R.id.content, SyncSettingsFragment()).commit()
    }
//    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
//        super.onCreate(savedInstanceState, persistentState)
//        fragmentManager.beginTransaction().replace(android.R.id.content, SyncSettingsFragment()).commit()
//    }
}
