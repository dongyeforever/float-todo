package vip.lovek.floattodo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vip.lovek.floattodo.dao.TodoDao
import vip.lovek.floattodo.dao.TodoDatabase
import vip.lovek.floattodo.model.CommonConstants
import vip.lovek.floattodo.model.Todo

/**
 * @author zhiruiyu
 */
class TodoViewHolder(private var mContext: Context, itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var mTodo: Todo? = null
    private var todoDao: TodoDao
    private val completedCheckBox: CheckBox
    private val todoTitleTextView: TextView
    private val todoViewModel: TodoViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(mContext as ViewModelStoreOwner)[TodoViewModel::class.java]
    }

    private val onCheckedListener = { _: View, isChecked: Boolean ->
        onCompletedChanged(isChecked)
    }

    private val onClickListener = { _: View ->
        onEditClicked()
    }

    init {
        val database = TodoDatabase.getDatabase(mContext)
        todoDao = database.todoDao()

        completedCheckBox = itemView.findViewById(R.id.completedCheckBox)
        todoTitleTextView = itemView.findViewById(R.id.todoTitleTextView)

        completedCheckBox.setOnCheckedChangeListener(onCheckedListener)
        itemView.setOnClickListener(onClickListener)
    }

    fun bind(todo: Todo) {
        this.mTodo = todo

        todoTitleTextView.apply {
            text = todo.title
            if (todo.isCompleted) {
                setTextColor(mContext.getColor(R.color.c_666666))
                typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
                paint.flags = Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                setTextColor(mContext.getColor(R.color.c_333333))
                typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                paint.flags = 0
            }
        }
        completedCheckBox.isChecked = todo.isCompleted
    }

    private fun onEditClicked() {
        val intent = Intent(mContext, EditTodoActivity::class.java)
        intent.putExtra(CommonConstants.INTENT_KEY_TODO, mTodo)
        (mContext as Activity).startActivityForResult(intent, 1)
    }

    private fun onCompletedChanged(checked: Boolean) {
        mTodo?.let {
            if (checked != it.isCompleted) {
                it.isCompleted = checked
                updateTodoInDatabase(it)
            }
        }
    }

    private fun updateTodoInDatabase(todo: Todo) {
        CoroutineScope(Dispatchers.IO).launch {
            todoDao.updateTodo(todo)
            withContext(Dispatchers.Main) {
                todoViewModel.getSortLiveData().value = true
            }
        }
    }
}