package vip.lovek.floattodo

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import vip.lovek.floattodo.databinding.ActivityEditTodoBinding
import vip.lovek.floattodo.model.Todo
import java.util.Calendar

class EditTodoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditTodoBinding
    private var todo: Todo? = null
    private var originTodo: Todo? = null
    private var reminderTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditTodoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        todo = intent.getParcelableExtra(TodoViewHolder.INTENT_KEY_TODO) as? Todo
        originTodo = todo?.copy()
        todo?.let {
            binding.todoTitleEditText.setText(it.title)
            binding.todoNoteEditText.setText(it.note)
            binding.importantCheckBox.isChecked = it.isImportant
            reminderTime = it.reminderTime
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = it.reminderTime
            binding.tvSetReminder.text = if (it.reminderTime > 0) formatToHourMinute(it.reminderTime) else ""
        }

        initListener()
    }

    private fun initListener() {
        setSaveEnable(false)
        binding.layoutRemindTime.setOnClickListener {
            showTimePicker()
        }

        binding.saveTodoButton.setOnClickListener {
            saveTodo()
        }

        binding.ivBack.setOnClickListener {
            onBackClick()
        }
        binding.todoTitleEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                setSaveEnable(hasTodoChanged())
            }
        })

        binding.todoNoteEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                setSaveEnable(hasTodoChanged())
            }
        })

        binding.importantCheckBox.setOnCheckedChangeListener { _, _ -> setSaveEnable(hasTodoChanged()) }
    }

    private fun onBackClick() {
        if (hasTodoChanged()) {
            showBackDialog()
        } else {
            finish()
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                selectedCalendar.set(Calendar.MINUTE, selectedMinute)
                reminderTime = selectedCalendar.timeInMillis
                binding.tvSetReminder.text = formatToHourMinute(reminderTime)
                setSaveEnable(hasTodoChanged())

                setAlarm(reminderTime)
                Toast.makeText(this, "提醒时间已设置", Toast.LENGTH_SHORT).show()
            },
            hour,
            minute,
            true
        )
        timePickerDialog.show()
    }

    private fun setAlarm(time: Long) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent)
    }

    private fun showBackDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("提示")
        builder.setMessage("信息未保存，是否返回？")
        builder.setPositiveButton("确定") { dialog, _ ->
            dialog.dismiss()
            finish()
        }
        // 设置取消按钮及其点击事件
        builder.setNegativeButton("取消") { dialog, _ ->
            dialog.dismiss()
        }

        // 创建并显示对话框
        val dialog = builder.create()
        dialog.show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackClick()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun saveTodo() {
        val title = binding.todoTitleEditText.text.toString()
        if (title.isBlank()) {
            Toast.makeText(this, "标题不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        val note = binding.todoNoteEditText.text.toString()
        val isImportant = binding.importantCheckBox.isChecked

        todo = todo?.copy(
            title = title,
            note = note,
            isImportant = isImportant,
            reminderTime = reminderTime
        ) ?: Todo(
            title = title,
            note = note,
            isImportant = isImportant,
            reminderTime = reminderTime
        )

        val resultIntent = Intent()
        resultIntent.putExtra("todo", todo)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun formatToHourMinute(milliseconds: Long): String {
        // 创建一个 Calendar 实例
        val calendar = Calendar.getInstance()
        // 设置 Calendar 的时间为传入的毫秒值
        calendar.timeInMillis = milliseconds

        // 获取小时和分钟
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        // 格式化输出，确保小时和分钟为两位数
        return String.format("%02d:%02d", hour, minute)
    }

    private fun setSaveEnable(enable: Boolean) {
        binding.saveTodoButton.isEnabled = enable
        if (enable) {
            binding.saveTodoButton.setTextColor(resources.getColor(R.color.c_1a1a1a))
        } else {
            binding.saveTodoButton.setTextColor(resources.getColor(R.color.c_999999))
        }
    }

    private fun hasTodoChanged(): Boolean {
        val title = binding.todoTitleEditText.text.toString()
        val note = binding.todoNoteEditText.text.toString()
        val isImportant = binding.importantCheckBox.isChecked

        val todo = todo?.copy(
            title = title,
            note = note,
            isImportant = isImportant,
            reminderTime = reminderTime
        ) ?: Todo(
            title = title,
            note = note,
            isImportant = isImportant,
            reminderTime = reminderTime
        )
        return if (originTodo == null) {
            todo.title.isNotEmpty() || todo.note.isNotEmpty() || todo.isImportant || todo.reminderTime != 0L
        } else {
            todo.id != originTodo!!.id || todo.title != originTodo!!.title ||
                    todo.note != originTodo!!.note || todo.isCompleted != originTodo!!.isCompleted ||
                    todo.isImportant != originTodo!!.isImportant || todo.reminderTime != originTodo!!.reminderTime
        }
    }
}