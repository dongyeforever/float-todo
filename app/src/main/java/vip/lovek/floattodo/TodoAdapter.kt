package vip.lovek.floattodo

/**
 * @author zhiruiyu
 */

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import vip.lovek.floattodo.model.Todo

class TodoAdapter(private var mContext: Context, private var todoList: List<Todo>) :
    RecyclerView.Adapter<TodoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(mContext, view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo = todoList[position]
        holder.bind(todo)
    }

    override fun getItemCount(): Int = todoList.size

    fun updateList(newList: List<Todo>) {
        todoList = newList
        notifyDataSetChanged()
    }
}