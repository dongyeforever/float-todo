package vip.lovek.floattodo.util

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * @author zhiruiyu
 */
object DisplayUtil {
    /**
     * 获取屏幕的宽度（像素）
     * @param context 上下文对象
     * @return 屏幕宽度（像素）
     */
    fun getScreenWidth(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.width ?: 0
        } else {
            @Suppress("DEPRECATION")
            val displayMetrics = context.resources.displayMetrics
            displayMetrics.widthPixels
        }
    }

    /**
     * 获取屏幕的高度（像素）
     * @param context 上下文对象
     * @return 屏幕高度（像素）
     */
    fun getScreenHeight(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.height ?: 0
        } else {
            @Suppress("DEPRECATION")
            val displayMetrics = context.resources.displayMetrics
            displayMetrics.heightPixels
        }
    }

    fun getServiceScreenWidth(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        windowManager.getDefaultDisplay().getMetrics(dm)
        return dm.widthPixels
    }

    fun getServiceScreenHeight(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        windowManager.getDefaultDisplay().getMetrics(dm)
        return dm.heightPixels
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     *
     * @param dpValue 虚拟像素
     * @return 像素
     */
    fun dp2px(dpValue: Float): Int {
        return (0.5f + dpValue * Resources.getSystem().displayMetrics.density).toInt()
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     *
     * @param pxValue 像素
     * @return 虚拟像素
     */
    fun px2dp(pxValue: Int): Float {
        return (pxValue / Resources.getSystem().displayMetrics.density)
    }

}