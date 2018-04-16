package com.shortsteplabs.shotsync.ui

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.shortsteplabs.shotsync.DownloaderService.Companion.startSync
import com.shortsteplabs.shotsync.R

class MainActivity : AppCompatActivity() {
    val perms = Permissions(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
    }

    override fun onStart() {
        super.onStart()
        perms.firstRun()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?) = when (item!!.itemId) {
        R.id.action_settings -> {
            startActivity(Intent(this, SyncSettingsActivity::class.java))
            true
        }
        else ->super.onOptionsItemSelected(item)
    }

    fun startSync(view: View) {
        startSync(this)
    }
}
