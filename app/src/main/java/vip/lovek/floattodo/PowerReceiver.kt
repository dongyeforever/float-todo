package vip.lovek.floattodo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class PowerReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "PowerReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "action: ${intent.action}")
        val i = Intent(context, FloatingService::class.java)
        context.startService(i)
    }

}