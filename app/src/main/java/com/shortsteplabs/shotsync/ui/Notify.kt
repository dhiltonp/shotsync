package com.shortsteplabs.shotsync.ui

import android.app.Activity
import android.os.AsyncTask
import com.google.android.material.snackbar.Snackbar
import android.view.View

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


fun notifyBottom(activity: Activity, text: String, seconds: Int=5, actionText: String="", actionListener: View.OnClickListener? = null) {
    val notification = Snackbar.make(
            activity.findViewById(android.R.id.content),
            text,
            Snackbar.LENGTH_INDEFINITE)
    if (actionListener != null) {
        notification.setAction(actionText, actionListener)
    }
    notification.show()

    class dismisser: AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg params: Void?): Boolean {
            Thread.sleep(seconds * 1000L)
            notification.dismiss()
            return true
        }
    }
    dismisser().execute()
}