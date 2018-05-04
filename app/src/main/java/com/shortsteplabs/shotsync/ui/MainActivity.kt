package com.shortsteplabs.shotsync.ui

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.shortsteplabs.shotsync.R
import com.shortsteplabs.shotsync.camera.Discover
import com.shortsteplabs.shotsync.db.DB
import com.shortsteplabs.shotsync.db.getCamera
import com.shortsteplabs.shotsync.gps.LocationReceiver
import com.shortsteplabs.shotsync.sync.SyncService
import com.shortsteplabs.shotsync.util.RecursiveDelete
import com.shortsteplabs.shotsync.util.filesExist
import com.shortsteplabs.shotsync.wificonnected.WifiListenerService
import kotlinx.android.synthetic.main.content_main.*

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

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
    }

    override fun onStart() {
        super.onStart()
        enableStartSync()
        enableDeleteDownloaded()
        WifiListenerService.startListener(this)
        LocationReceiver.startUpdates(this)
    }

    fun enableStartSync() {
        class enable: AsyncTask<Context, Void, Boolean>() {
            override fun doInBackground(vararg params: Context?): Boolean {
                val context = params.first()!!
                val camera = getCamera(DB.getInstance(context))
                return camera.ssid != ""
            }
            override fun onPostExecute(camera: Boolean) {
                if (camera) {
                    startSync.isEnabled = true
                }
            }
        }
        enable().execute(this)
    }

    fun enableDeleteDownloaded() {
        deleteDownloaded.isEnabled = filesExist(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?) = when (item!!.itemId) {
        R.id.action_settings -> {
            val intent = Intent(this, CameraSettingsActivity::class.java)
            intent.putExtra(CAMERA_ID, 1)
            startActivity(intent)
            true
        }
        else ->super.onOptionsItemSelected(item)
    }

    fun pairCamera(view: View) {
        val activity = this

        Permissions(this).requestPermissions()
        Permissions(this).requestIgnoreBattery()

        val builder = AlertDialog.Builder(this)
        builder.setMessage("Please turn on your camera's wifi, connect to it, then continue.")   // mom ignored this popup, just clicking "continue"
        builder.setPositiveButton("Continue", fun(dialog: DialogInterface?, id: Int) {
            class discover: AsyncTask<Context, Void, Discover.DiscoverResult>() {
                override fun doInBackground(vararg params: Context?): Discover.DiscoverResult {
                    return Discover().discover(params[0]!!)
                }

                override fun onPostExecute(result: Discover.DiscoverResult?) {
                    super.onPostExecute(result)
                    if (result != null) {
                        if (result.success) {
                            enableStartSync()
                        }
                        notifyBottom(activity, result.text, 6)
                    }
                }
            }
            discover().execute(this)
        })
        builder.setNegativeButton("Cancel", fun(_: DialogInterface?, _: Int) {})
        builder.create().show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Permissions(this).requestPermissionsCallback(permissions, grantResults)
    }

    fun startSync(view: View) {
        SyncService.syncIntent(this).send()
        notifyBottom(this, "Sync started", 3)
    }

    fun deleteDownloaded(view: View) {
        class delete: RecursiveDelete() {
            override fun onPostExecute(result: Long?) {
                enableDeleteDownloaded()
            }
        }
        delete().execute(this)
    }
}
