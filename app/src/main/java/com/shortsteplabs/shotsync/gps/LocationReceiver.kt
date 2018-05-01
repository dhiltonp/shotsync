package com.shortsteplabs.shotsync.gps

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import android.support.v4.app.ActivityCompat
import android.util.Log
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.shortsteplabs.shotsync.db.DB


const val TAG = "LocationReceiver"
const val AUTO_UPDATE = "com.shortsteplabs.shotsync.LocationReceiver.AUTO_UPDATE"
const val SINGLE_UPDATE = "com.shortsteplabs.shotsync.LocationReceiver.MANUAL_UPDATE"

const val EXTRA_LOCATION_RESULT = "com.shortsteplabs.shotsync.LocationReceiver.EXTRA_LOCATION_RESULT"

class LocationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "entering onReceive")
        if (AUTO_UPDATE == intent.action) {
            val result = LocationResult.extractResult(intent)
            if (result != null) {
                locationsHandler(context, result.locations)
            }
        } else if (SINGLE_UPDATE == intent.action) {
            val result: Location? = if (intent.hasExtra(EXTRA_LOCATION_RESULT)) intent.getParcelableExtra(EXTRA_LOCATION_RESULT) else null
            if (result != null) {
                locationsHandler(context, listOf(result))
            }
        }
    }

    private fun locationsHandler(context: Context, locations: List<Location>) {
        class locationUpdate: AsyncTask<Context, Void, Void?>() {
            override fun doInBackground(vararg params: Context): Void? {
                val dbLocations = mutableListOf<com.shortsteplabs.shotsync.db.Location>()

                for (location in locations) {
                    Log.d(TAG, location.time.toString() + " " + location.accuracy.toString() + " " + location.latitude.toString() + " " + location.longitude.toString())

                    val dbLocation = com.shortsteplabs.shotsync.db.Location()
                    dbLocation.time = location.time
                    dbLocation.latitude = location.latitude
                    dbLocation.longitude = location.longitude
                    dbLocation.accuracy = location.accuracy

                    dbLocations.add(dbLocation)
                }
                DB.getInstance(context).locationDao().insertNew(*dbLocations.toTypedArray())
                return null
            }
        }
        locationUpdate().execute(context)
    }

    companion object {
        private fun getPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, LocationReceiver::class.java)
            intent.action = AUTO_UPDATE
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        private fun getLocationRequest(): LocationRequest {
            val minutes = 60*1000L
            val interval = 20*minutes
            val fastestInterval = 5*minutes
            val maxWaitTime = interval*3

            val locationRequest = LocationRequest()
            locationRequest.interval = interval
            locationRequest.fastestInterval = fastestInterval
            locationRequest.maxWaitTime = maxWaitTime
            locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

            return locationRequest
        }

        private fun singleLocation(context: Context) {
            if (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(context,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                val location = LocationServices.getFusedLocationProviderClient(context).lastLocation
                location.addOnSuccessListener {
                    val intent = Intent(context, LocationReceiver::class.java)
                    intent.action = SINGLE_UPDATE
                    intent.putExtra(EXTRA_LOCATION_RESULT, it)

                    context.sendBroadcast(intent)
                }
            }
        }

        private fun flushLocations(context: Context) {
            LocationServices.getFusedLocationProviderClient(context)
                    .flushLocations()
        }

        fun startUpdates(context: Context): Boolean {
            if (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(context,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                LocationServices.getFusedLocationProviderClient(context)
                        .requestLocationUpdates(getLocationRequest(), getPendingIntent(context))
                return true
            }
            return false
        }

        fun stopUpdates(context: Context) {
            LocationServices.getFusedLocationProviderClient(context)
                    .removeLocationUpdates(getPendingIntent(context))
        }

        fun flush(context: Context) {
            flushLocations(context)
            singleLocation(context)
        }
    }
}