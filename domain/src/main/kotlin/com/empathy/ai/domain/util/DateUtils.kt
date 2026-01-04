package com.empathy.ai.domain.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日期工具类
 *
 * 统一管理日期格式化和解析，避免重复创建SimpleDateFormat实例。
 * 使用ThreadLocal确保线程安全，可在多线程环境中安全使用。
 *
 * 设计决策:
 * - 使用ThreadLocal而非synchronized，提高并发性能
 * - 预定义常用格式，减少重复代码
 * - 解析失败返回0（时间戳0即1970-01-01），便于识别
 */
object DateUtils {

    private const val DATE_FORMAT = "yyyy-MM-dd"
    private const val DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss"

    /**
     * 日期格式化器（线程安全）
     *
     * 使用ThreadLocal确保每个线程有独立的SimpleDateFormat实例
     * 避免synchronized带来的性能开销
     */
    private val dateFormatter = ThreadLocal.withInitial {
        SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
    }

    /**
     * 日期时间格式化器（线程安全）
     */
    private val dateTimeFormatter = ThreadLocal.withInitial {
        SimpleDateFormat(DATETIME_FORMAT, Locale.getDefault())
    }

    /**
     * 格式化时间戳为日期字符串（yyyy-MM-dd）
     *
     * @param timestamp 时间戳（毫秒）
     * @return 格式化后的日期字符串
     */
    fun formatDate(timestamp: Long): String {
        return dateFormatter.get()?.format(Date(timestamp)) ?: ""
    }

    /**
     * 格式化时间戳为日期时间字符串（yyyy-MM-dd HH:mm:ss）
     *
     * @param timestamp 时间戳（毫秒）
     * @return 格式化后的日期时间字符串
     */
    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormatter.get()?.format(Date(timestamp)) ?: ""
    }

    /**
     * 获取当前日期字符串（yyyy-MM-dd）
     *
     * @return 当前日期字符串
     */
    fun getCurrentDateString(): String {
        return formatDate(System.currentTimeMillis())
    }

    /**
     * 获取当前日期时间字符串（yyyy-MM-dd HH:mm:ss）
     *
     * @return 当前日期时间字符串
     */
    fun getCurrentDateTimeString(): String {
        return formatDateTime(System.currentTimeMillis())
    }

    /**
     * 解析日期字符串为时间戳
     *
     * @param dateString 日期字符串（yyyy-MM-dd）
     * @return 时间戳（毫秒），解析失败返回0
     */
    fun parseDate(dateString: String): Long {
        return try {
            dateFormatter.get()?.parse(dateString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 解析日期时间字符串为时间戳
     *
     * @param dateTimeString 日期时间字符串（yyyy-MM-dd HH:mm:ss）
     * @return 时间戳（毫秒），解析失败返回0
     */
    fun parseDateTime(dateTimeString: String): Long {
        return try {
            dateTimeFormatter.get()?.parse(dateTimeString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 获取昨天的日期字符串（yyyy-MM-dd）
     *
     * @return 昨天的日期字符串
     */
    fun getYesterdayDateString(): String {
        val yesterday = System.currentTimeMillis() - MemoryConstants.ONE_DAY_MILLIS
        return formatDate(yesterday)
    }

    /**
     * 格式化时间戳为简短时间字符串（HH:mm）
     *
     * @param timestamp 时间戳（毫秒）
     * @return 简短时间字符串
     */
    fun formatTimestamp(timestamp: Long): String {
        return formatDateTime(timestamp).substring(11, 16)
    }

    /**
     * 判断两个日期字符串是否为同一天
     *
     * @param date1 第一个日期字符串
     * @param date2 第二个日期字符串
     * @return 是否为同一天
     */
    fun isSameDay(date1: String, date2: String): Boolean {
        return date1 == date2
    }

    /**
     * 格式化相对时间
     *
     * 将时间戳转换为相对时间描述，如"刚刚"、"5分钟前"、"2小时前"等。
     *
     * 业务规则:
     * - < 1分钟: "刚刚"
     * - < 1小时: "X分钟前"
     * - < 1天: "X小时前"
     * - < 7天: "X天前"
     * - >= 7天: 显示具体日期（MM-dd）
     *
     * @param timestamp 时间戳（毫秒）
     * @return 相对时间描述字符串
     */
    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 0 -> "刚刚" // 处理时间戳在未来的情况
            diff < 60_000 -> "刚刚"
            diff < 3600_000 -> "${diff / 60_000}分钟前"
            diff < 86400_000 -> "${diff / 3600_000}小时前"
            diff < 604800_000 -> "${diff / 86400_000}天前"
            else -> {
                // 超过7天显示具体日期
                val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}
