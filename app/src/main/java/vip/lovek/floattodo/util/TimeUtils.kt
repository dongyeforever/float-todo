package vip.lovek.floattodo.util

import java.util.Calendar

object TimeUtils {
    /**
     * 判断给定的毫秒值是否属于当前分钟
     * @param millis 要判断的毫秒值
     * @return 如果给定的毫秒值属于当前分钟，返回 true；否则返回 false
     */
    fun isInCurrentMinute(millis: Long): Boolean {
        // 获取当前时间的 Calendar 实例
        val currentCalendar = Calendar.getInstance()
        // 获取当前分钟
        val currentMinute = currentCalendar.get(Calendar.MINUTE)
        // 获取当前小时
        val currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY)
        // 获取当前日期
        val currentDay = currentCalendar.get(Calendar.DAY_OF_YEAR)
        // 获取当前年份
        val currentYear = currentCalendar.get(Calendar.YEAR)

        // 创建一个新的 Calendar 实例，并设置为给定的毫秒值
        val targetCalendar = Calendar.getInstance()
        targetCalendar.timeInMillis = millis
        // 获取目标分钟
        val targetMinute = targetCalendar.get(Calendar.MINUTE)
        // 获取目标小时
        val targetHour = targetCalendar.get(Calendar.HOUR_OF_DAY)
        // 获取目标日期
        val targetDay = targetCalendar.get(Calendar.DAY_OF_YEAR)
        // 获取目标年份
        val targetYear = targetCalendar.get(Calendar.YEAR)

        // 判断年份、日期、小时和分钟是否都相同
        return currentYear == targetYear && currentDay == targetDay && currentHour == targetHour && currentMinute == targetMinute
    }
}