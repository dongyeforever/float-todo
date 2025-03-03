package vip.lovek.floattodo.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import vip.lovek.floattodo.model.Todo

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_table ORDER BY isCompleted ASC, id DESC")
    fun getAllTodos(): List<Todo>

    @Insert
    fun insertTodo(todo: Todo): Long

    @Update
    fun updateTodo(todo: Todo)
}