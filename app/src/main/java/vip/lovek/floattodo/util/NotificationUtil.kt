package vip.lovek.floattodo.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import vip.lovek.floattodo.FloatingService.Companion.CHANNEL_ID
import vip.lovek.floattodo.FloatingService.Companion.NOTIFICATION_ID
import vip.lovek.floattodo.MainActivity
import vip.lovek.floattodo.model.Todo

/**
 * @author zhiruiyu
 */
object NotificationUtil {

    val notificationTodoList = HashSet<Long>()

    fun sendNotification(context: Context, todo: Todo) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 创建一个 Intent 用于打开某个 Activity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH)
        channel.setShowBadge(true)
        notificationManager.createNotificationChannel(channel)

        // 构建通知
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("您有代办需要现在处理")
            .setContentText(todo.title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            // 设置通知在锁屏界面的显示方式
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        // 发送通知
        notificationManager.notify(NOTIFICATION_ID, notification)
        notificationTodoList.add(todo.id)
    }
}