package vip.lovek.floattodo

/**
 * @author zhiruiyu
 */
import android.app.Application
import vip.lovek.floattodo.util.ActivityUtil

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        ActivityUtil.init(this)
    }

}