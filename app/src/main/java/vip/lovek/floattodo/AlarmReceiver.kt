package vip.lovek.floattodo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "待办事项提醒！", Toast.LENGTH_SHORT).show()
        Log.d("TAG", "待办事项提醒！")
    }

}