package com.shortsteplabs.shotsync.shotsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Created by david on 3/30/18.
 */

public class WIFIConnected extends BroadcastReceiver {
    private static final String TAG = "WIFIConnected";

    @Override
    public void onReceive(Context context, Intent intent) {
        NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (netInfo.isConnected()) {
            Log.d(TAG, "connected");
            WifiInfo winfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            // or could use winfo.getSSID()?
            if (winfo.getBSSID().startsWith("90:b6:86")) {
                Log.d(TAG, "detected olympus camera");
            }

            Log.d(TAG, "Connecting to " + winfo.getBSSID() + ":" + winfo.getSSID());
        } else {
            Log.d(TAG, "not connected");
        }
    }
}
