package com.personaltrainer.integrations

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint

/**
 * Receives geofence transition events from Google Play Services.
 * Phase 6 implementation.
 */
@AndroidEntryPoint
class GeofenceReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Phase 6: parse GeofencingEvent.fromIntent(intent) and
        // auto-start the session when GEOFENCE_TRANSITION_ENTER fires.
        Log.d(TAG, "Geofence transition received — Phase 6 implementation pending")
    }
}
