package com.shortsteplabs.shotsync.ui

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import com.shortsteplabs.shotsync.R
import com.shortsteplabs.shotsync.db.Camera
import com.shortsteplabs.shotsync.db.DB
import com.shortsteplabs.shotsync.db.getCamera

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

class CameraSettingsFragment: PreferenceFragment() {
    private var camera: Camera? = null
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            getString(R.string.sync_mode_key) -> updateListSummary(key)
            getString(R.string.sync_period_key) -> updateListSummary(key)
            getString(R.string.auto_sync_key) -> if (autoSync()) Permissions(activity).requestIgnoreBattery()
            getString(R.string.sync_jpg_key),
            getString(R.string.sync_raw_key),
            getString(R.string.sync_vid_key) -> if (syncFiles()) Permissions(activity).requestPermission(WRITE_EXTERNAL_STORAGE)
            getString(R.string.sync_gps_key) -> if (syncGPS()) Permissions(activity).requestPermission(ACCESS_FINE_LOCATION)
        // todo: trigger ui refresh when camera entry in db is updated...
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.sync_settings)

    }
    
    override fun onStart() {
        super.onStart()
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        prefs.registerOnSharedPreferenceChangeListener(listener)

        updateListSummary(getString(R.string.sync_mode_key))
        updateListSummary(getString(R.string.sync_period_key))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val camera_id = arguments.getInt(CAMERA_ID)
        class load: AsyncTask<Context, Void, Camera>() {
            override fun doInBackground(vararg params: Context?): Camera {
                camera = getCamera(DB.getInstance(activity), camera_id)
                return camera!!
            }
        }
        load().execute(activity)
    }

    override fun onStop() {
        super.onStop()
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        prefs.unregisterOnSharedPreferenceChangeListener(listener)

        updateCamera()
    }

    private fun autoSync(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        return prefs.getBoolean(getString(R.string.auto_sync_key), true)
    }

    private fun syncFiles(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val syncJPG = prefs.getBoolean(getString(R.string.sync_jpg_key), true)
        val syncRAW = prefs.getBoolean(getString(R.string.sync_raw_key), false)
        val syncVID = prefs.getBoolean(getString(R.string.sync_vid_key), false)
        return syncJPG || syncRAW || syncVID
    }

    private fun syncGPS(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        return prefs.getBoolean(getString(R.string.sync_gps_key), true)
    }

    private fun updateCamera() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)

        if (camera == null) return

        val camera = camera!!

        if (prefs.getString(getString(R.string.sync_mode_key), "") == getString(R.string.sync_then_off_value)) {
            camera.autoOff = true
            camera.liveShooting = false
        } else {
            camera.autoOff = false
            camera.liveShooting = true
        }

        camera.autoSync = prefs.getBoolean(getString(R.string.auto_sync_key), true)

        camera.syncPeriod = prefs.getString(getString(R.string.sync_period_key), "86400000").toLong()
        camera.syncJPG = prefs.getBoolean(getString(R.string.sync_jpg_key), true)
        camera.syncRAW = prefs.getBoolean(getString(R.string.sync_raw_key), false)
        camera.syncVID = prefs.getBoolean(getString(R.string.sync_vid_key), false)
        camera.syncFiles = syncFiles()

        camera.syncGPS = prefs.getBoolean(getString(R.string.sync_gps_key), true)
        camera.syncTime = true
        camera.maintainUTC = prefs.getBoolean(getString(R.string.maintain_utc_key), false)

        class save: AsyncTask<Context, Void, Camera>() {
            override fun doInBackground(vararg params: Context?): Camera {
                DB.getInstance(activity).cameraDao().update(camera)
                return camera
            }
        }
        save().execute(activity)
    }

    private fun updateListSummary(key: String) {
        val pref = findPreference(key) as ListPreference
        val entry = pref.entry
        pref.summary = entry
    }
}

class CameraSettingsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val camera_id = intent.getIntExtra(CAMERA_ID, 1)
        val cameraSettings = CameraSettingsFragment()
        val arguments = Bundle()
        arguments.putInt(CAMERA_ID, camera_id)
        cameraSettings.arguments = arguments

        fragmentManager.beginTransaction().replace(android.R.id.content, cameraSettings).commit()
    }
}
