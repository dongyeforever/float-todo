package vip.lovek.floattodo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * @author zhiruiyu
 */
class TodoViewModel : ViewModel() {
    private val _sortLiveData = MutableLiveData<Boolean>()

    fun getSortLiveData(): MutableLiveData<Boolean> {
        return _sortLiveData
    }
}