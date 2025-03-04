package vip.lovek.floattodo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import vip.lovek.floattodo.model.CommonConstants
import vip.lovek.floattodo.model.Todo
import vip.lovek.floattodo.util.NotificationUtil

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "待办事项提醒！", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "待办事项提醒！")
        // 检查是否标记完成
        if (intent.extras == null) {
            return
        }
        val id = intent.extras!!.getLong(CommonConstants.INTENT_KEY_TODO_ID)
        val title = intent.extras!!.getString(CommonConstants.INTENT_KEY_TODO_TITLE) ?: ""
        if (!NotificationUtil.notificationTodoList.contains(id)) {
            NotificationUtil.sendNotification(context, Todo(id = id, title = title))
        }
    }

}