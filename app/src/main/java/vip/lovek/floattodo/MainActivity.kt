package vip.lovek.floattodo

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vip.lovek.floattodo.dao.TodoDao
import vip.lovek.floattodo.dao.TodoDatabase
import vip.lovek.floattodo.databinding.ActivityMainBinding
import vip.lovek.floattodo.model.Todo

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val todoList = mutableListOf<Todo>()
    private lateinit var todoDao: TodoDao
    private lateinit var adapter: TodoAdapter
    private val todoViewModel: TodoViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this)[TodoViewModel::class.java]
    }

    private val sortObserver = Observer<Boolean> {
        sortTodoList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = TodoDatabase.getDatabase(this)
        todoDao = database.todoDao()
        adapter = TodoAdapter(this, todoList)

        binding.todoRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.todoRecyclerView.adapter = adapter

        binding.addTodoButton.setOnClickListener {
            val intent = Intent(this, EditTodoActivity::class.java)
            startActivityForResult(intent, REQUEST_TODO)
        }
        binding.tvLogo.setOnClickListener {
            requestOverlayPermission()
        }

        todoViewModel.getSortLiveData().observe(this, sortObserver)
        // 从数据库加载
        loadTodosFromDatabase()

        binding.root.post {
            checkAndStart()
        }
    }

    private fun loadTodosFromDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            val todos = todoDao.getAllTodos()
            CoroutineScope(Dispatchers.Main).launch {
                todoList.clear()
                todoList.addAll(todos)
                sortTodoList()
            }
        }
    }

    private fun sortTodoList() {
        todoList.sortWith(
            compareBy(
                { it.isCompleted },
                { if (it.reminderTime == 0L) Long.MAX_VALUE else it.reminderTime },
                { -it.id })
        )
        adapter.updateList(todoList)
        sendBroadcastReceiver()
    }

    private fun requestOverlayPermission() {
        // 创建一个 Intent 来启动系统设置页面，让用户开启悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQUEST_FLOAT_PERMISSION)
        }
    }

    private fun checkAndStart() {
        // 检查并请求悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
        } else {
            startFloatingClockService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TODO && resultCode == RESULT_OK) {
            val todo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra(TodoViewHolder.INTENT_KEY_TODO, Todo::class.java)
            } else {
                data?.getParcelableExtra(TodoViewHolder.INTENT_KEY_TODO) as? Todo
            }
            todo?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    if (it.id == 0L) {
                        val newId = todoDao.insertTodo(it)
                        it.id = newId
                        todoList.add(it)
                    } else {
                        todoDao.updateTodo(it)
                        val index = todoList.indexOfFirst { existingTodo -> existingTodo.id == it.id }
                        if (index != -1) {
                            todoList[index] = it
                        }
                    }

                    withContext(Dispatchers.Main) {
                        sortTodoList()
                    }
                }
            }
        } else if (requestCode == REQUEST_FLOAT_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingClockService()
            }
        }
    }

    private fun sendBroadcastReceiver() {
        val intent = Intent(FloatingService.FLOAT_SERVICE_ACTION)
        sendBroadcast(intent)
    }

    private fun startFloatingClockService() {
        val intent = Intent(this, FloatingService::class.java)
        startService(intent)
    }

    companion object {
        const val REQUEST_TODO = 1
        const val REQUEST_FLOAT_PERMISSION = 2
    }
}