package vip.lovek.floattodo.util

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle

/**
 * @author zhiruiyu
 */
object ActivityUtil {
    private var count = 0

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                count++
            }

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                count--
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    fun getActivityCount(): Int {
        return count
    }

    fun isAppTop(): Boolean {
        return count > 0
    }

    fun isAppInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appPackageName = context.packageName
        val runningTasks = activityManager.getRunningTasks(1)
        return runningTasks.isNotEmpty() && runningTasks[0].topActivity?.packageName == appPackageName
    }
}