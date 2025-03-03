package vip.lovek.floattodo

import android.animation.ObjectAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vip.lovek.floattodo.dao.TodoDatabase
import vip.lovek.floattodo.model.Todo
import vip.lovek.floattodo.util.ActivityUtil
import vip.lovek.floattodo.util.DisplayUtil
import vip.lovek.floattodo.util.TimeUtils
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Timer
import java.util.TimerTask

class FloatingService : Service() {
    private lateinit var vibrator: Vibrator
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var clockTextView: TextView
    private lateinit var closeButton: TextView
    private var timer: Timer? = null
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
    private var actionDownTime: Long = 0

    // 广播
    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == FLOAT_SERVICE_ACTION) {
                loadFirstTodo()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        // 加载悬浮窗布局
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_clock, null)
        clockTextView = floatingView.findViewById(R.id.clock_text)
        closeButton = floatingView.findViewById(R.id.close_button)

        initListener()
        // 启动时钟更新任务
        startClock()
        loadFirstTodo()
        // 广播
        val filter = IntentFilter(FLOAT_SERVICE_ACTION)
        registerReceiver(
            broadcastReceiver,
            filter,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Context.RECEIVER_NOT_EXPORTED else 0
        )
    }

    private fun initListener() {
        // 设置悬浮窗布局参数
        val layoutParams = initFloatWindowManager()
        // 处理悬浮窗拖动事件
        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        actionDownTime = System.currentTimeMillis()
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        if ((event.rawX - initialTouchX < touchSlop) && (event.rawY - initialTouchY < touchSlop) &&
                            (System.currentTimeMillis() - actionDownTime < 300)
                        ) {
                            goMainActivity()
                        }
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, layoutParams)
                        return true
                    }
                }
                return false
            }
        })
        // 添加悬浮窗到 WindowManager
        windowManager.addView(floatingView, layoutParams)

        // 设置关闭按钮点击事件监听器
        closeButton.setOnClickListener {
            timer?.cancel()
            stopSelf() // 停止服务
        }
    }

    private fun goMainActivity() {
        if (ActivityUtil.isAppTop()) return
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun initFloatWindowManager(): WindowManager.LayoutParams {
        val screenWidth = DisplayUtil.getServiceScreenWidth(applicationContext)
        val screenHeight = DisplayUtil.getServiceScreenHeight(applicationContext)
        val layoutParams =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

        // 设置悬浮窗位置
        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = screenWidth
        layoutParams.y = screenHeight - DisplayUtil.dp2px(FLOAT_MARGIN_BOTTOM)
        return layoutParams
    }

    private fun startClock() {
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                // 获取当前精确的时间
//                val now = Instant.now()
//                val currentTime = timeFormatter.format(now)
                // 在 UI 线程更新时钟显示
//                clockTextView.post {
//                    clockTextView.text = currentTime
//                }
                loadFirstTodo()
            }
        }, 0, 10 * TIME_UPDATE_INTERVAL)
    }

    private fun loadFirstTodo() {
        val database = TodoDatabase.getDatabase(this)
        val todoDao = database.todoDao()
        CoroutineScope(Dispatchers.IO).launch {
            val todos = todoDao.getAllTodos()
            if (todos.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    floatingView.visibility = View.VISIBLE
                }
                val todo = todos[0]
                CoroutineScope(Dispatchers.Main).launch {
                    if (todo.isCompleted) {
                        floatingView.visibility = View.GONE
                    } else {
                        clockTextView.text = todo.title
                        if (TimeUtils.isInCurrentMinute(todo.reminderTime)) {
                            // 构造通知
                            sendNotification(todo)
                            // 动画提醒
                            startAnimation()
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    floatingView.visibility = View.GONE
                }
            }
        }
    }

    private fun startAnimation() {
        // 单次心跳动画的时长
        // 计算 10 秒内动画的循环次数
        val repeatCount = (VIBRATOR_DURATION / TIME_UPDATE_INTERVAL - 1).toInt()
        // 创建心跳样式的动画
        val currentWidth = clockTextView.width
        // 定义目标宽度
        val targetWidth = DisplayUtil.dp2px(ANIMATION_TEXT_WIDTH)
        val textAnimator = ObjectAnimator.ofInt(clockTextView, "width", currentWidth, targetWidth)
        textAnimator.duration = TIME_UPDATE_INTERVAL
        // 启动动画
        textAnimator.start()
        textAnimator.addUpdateListener {
            clockTextView.requestLayout()
        }
    }

    // 发送通知
    private fun sendNotification(todo: Todo) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 创建一个 Intent 用于打开某个 Activity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH)
        channel.setShowBadge(true)
        notificationManager.createNotificationChannel(channel)

        // 构建通知
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("您有代办需要现在处理")
            .setContentText(todo.title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // 发送通知
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 移除悬浮窗
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
        // 停止时钟更新任务
        timer?.cancel()
    }

    companion object {
        const val TAG = "FloatingService"
        const val TIME_UPDATE_INTERVAL = 1000L
        const val FLOAT_MARGIN_BOTTOM = 360f
        const val VIBRATOR_DURATION = 10000
        const val CHANNEL_ID = "todo"
        const val NOTIFICATION_ID = 1

        // 动画TextView宽度
        const val ANIMATION_TEXT_WIDTH = 108f

        // 滑动阈值
        const val touchSlop = 16
        const val FLOAT_SERVICE_ACTION = "vip.lovek.floattodo.FLOAT_SERVICE_ACTION"
    }
}