package vip.lovek.floattodo.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @author zhiruiyu
 */
@Entity(tableName = "todo_table")
data class Todo(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var title: String,
    var note: String = "",
    var isCompleted: Boolean = false,
    var isImportant: Boolean = false,
    var reminderTime: Long = 0
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        title = parcel.readString() ?: "",
        note = parcel.readString() ?: "",
        isCompleted = parcel.readByte() != 0.toByte(),
        isImportant = parcel.readByte() != 0.toByte(),
        reminderTime = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(title)
        parcel.writeString(note)
        parcel.writeByte(if (isCompleted) 1 else 0)
        parcel.writeByte(if (isImportant) 1 else 0)
        parcel.writeLong(reminderTime)
    }

    override fun describeContents(): Int {
        return 0
    }

    // 用于创建 对象的 Parcelable.Creator
    companion object CREATOR : Parcelable.Creator<Todo> {
        // 从 Parcel 中创建对象
        override fun createFromParcel(parcel: Parcel): Todo {
            return Todo(parcel)
        }

        // 创建指定大小的数组
        override fun newArray(size: Int): Array<Todo?> {
            return arrayOfNulls(size)
        }
    }
}