package vip.lovek.floattodo

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vip.lovek.floattodo.dao.TodoDatabase
import vip.lovek.floattodo.model.Todo
import vip.lovek.floattodo.util.ActivityUtil
import vip.lovek.floattodo.util.DisplayUtil
import vip.lovek.floattodo.util.NotificationUtil
import vip.lovek.floattodo.util.TimeUtils
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Timer
import java.util.TimerTask

class FloatingService : Service() {
    private lateinit var vibrator: Vibrator
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var deleteArea: View
    private lateinit var clockTextView: TextView
    private lateinit var closeButton: TextView
    private var timer: Timer? = null
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
    private var actionDownTime: Long = 0
    private var lastTodo: Todo? = null

    // 广播
    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == FLOAT_SERVICE_ACTION) {
                loadFirstTodo()
            }
        }
    }

    private var powerBroadcastReceiver: PowerReceiver = PowerReceiver()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
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
        registerTodoBroadcast()
        registerPowerBroadcast()
    }

    private fun registerPowerBroadcast() {
        val intent = IntentFilter()
        intent.addAction(Intent.ACTION_SCREEN_ON)
        intent.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(powerBroadcastReceiver, intent)
    }

    private fun registerTodoBroadcast() {
        val filter = IntentFilter(FLOAT_SERVICE_ACTION)
        registerReceiver(
            broadcastReceiver,
            filter,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) RECEIVER_NOT_EXPORTED else 0
        )
    }

    private fun initListener() {
        // 设置悬浮窗布局参数
        val bottomLayoutParams = initFloatBottomView()
        windowManager.addView(deleteArea, bottomLayoutParams)
        val layoutParams = initFloatView()
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
                        // 检查是否拖动到删除区域
                        if (isInDeleteArea(event.rawY.toInt())) {
                            stopSelf()
                        }
                        // 隐藏删除区域
                        deleteArea.visibility = View.GONE
                        if (currentIsClick(event, System.currentTimeMillis())) {
                            goMainActivity()
                        }
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, layoutParams)
                        if (!currentIsClick(event, System.currentTimeMillis())) {
                            deleteArea.visibility = View.VISIBLE
                        }
                        return true
                    }
                }
                return false
            }

            private fun currentIsClick(event: MotionEvent, currentTimeMillis: Long): Boolean {
                return (event.rawX - initialTouchX < touchSlop) && (event.rawY - initialTouchY < touchSlop) &&
                        (currentTimeMillis - actionDownTime < 300)
            }
        })
        // 添加悬浮窗到 WindowManager
        windowManager.addView(floatingView, layoutParams)
        // 初始化隐藏删除区域
        deleteArea.visibility = View.GONE

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

    private fun isInDeleteArea(y: Int): Boolean {
        val bottomHeight = DisplayUtil.dp2px(BOTTOM_DELETE_HEIGHT)
        val screenHeight = DisplayUtil.getServiceScreenHeight(applicationContext)
        return y > screenHeight - bottomHeight
    }

    private fun initFloatView(): WindowManager.LayoutParams {
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
        layoutParams.x = 0
        layoutParams.y = screenHeight - DisplayUtil.dp2px(FLOAT_MARGIN_BOTTOM)
        return layoutParams
    }

    private fun initFloatBottomView(): WindowManager.LayoutParams {
        // 加载删除区域布局
        deleteArea = LayoutInflater.from(this).inflate(R.layout.floating_delete_area, null)
        val bottomHeight = DisplayUtil.dp2px(BOTTOM_DELETE_HEIGHT)
        val deleteParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            bottomHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        deleteParams.gravity = Gravity.BOTTOM
        deleteParams.y = 0
        return deleteParams
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
        }, 0, 40 * TIME_UPDATE_INTERVAL)
    }

    private fun loadFirstTodo() {
        val database = TodoDatabase.getDatabase(this)
        val todoDao = database.todoDao()
        CoroutineScope(Dispatchers.IO).launch {
            val todos = todoDao.getAllTodos()
            if (todos.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    floatingView.visibility = View.VISIBLE
                    val todo = todos[0]
                    if (todo != lastTodo) {
                        if (todo.isCompleted) {
                            floatingView.visibility = View.GONE
                        } else {
                            clockTextView.text = todo.title
                            if (TimeUtils.isInCurrentMinute(todo.reminderTime)) {
                                // 构造通知
                                NotificationUtil.sendNotification(this@FloatingService, todo)
                                // 动画提醒
                                startAnimation()
                            }
                        }
                        lastTodo = todo
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
        Log.d(TAG, "startAnimation()")
        // 宽度动画
        var elapsedTime = 0L
        val textWidth = DisplayUtil.dp2px(ANIMATION_TEXT_WIDTH)
        val targetWidth = (textWidth * 1.2).toInt()
        val animator = ValueAnimator.ofInt(textWidth, targetWidth).apply {
            duration = TIME_UPDATE_INTERVAL
            addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                val layoutParams = clockTextView.layoutParams
                layoutParams.width = value
                clockTextView.layoutParams = layoutParams
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    elapsedTime += TIME_UPDATE_INTERVAL
                    if (elapsedTime < VIBRATOR_DURATION) {
                        start()
                    } else {
                        val layoutParams = clockTextView.layoutParams
                        layoutParams.width = textWidth
                        clockTextView.layoutParams = layoutParams
                    }
                }
            })
        }
        animator.start()
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
        const val VIBRATOR_DURATION = 20000
        const val TIME_UPDATE_INTERVAL = 1000L
        const val FLOAT_MARGIN_BOTTOM = 360f
        const val CHANNEL_ID = "todo"
        const val NOTIFICATION_ID = 1

        // 动画TextView宽度
        const val ANIMATION_TEXT_WIDTH = 90f
        const val BOTTOM_DELETE_HEIGHT = 80f

        // 滑动阈值
        const val touchSlop = 16

        // 同步 to do 列表数据广播
        const val FLOAT_SERVICE_ACTION = "vip.lovek.floattodo.FLOAT_SERVICE_ACTION"
    }
}