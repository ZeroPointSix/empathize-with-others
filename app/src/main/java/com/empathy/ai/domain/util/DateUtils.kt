package com.empathy.ai.domain.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日期工具类
 *
 * 统一管理日期格式化和解析，避免重复创建SimpleDateFormat实例
 * 使用ThreadLocal确保线程安全
 */
object DateUtils {

    private const val DATE_FORMAT = "yyyy-MM-dd"
    private const val DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss"

    /**
     * 日期格式化器（线程安全）
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
     */
    fun formatDate(timestamp: Long): String {
        return dateFormatter.get()?.format(Date(timestamp)) ?: ""
    }

    /**
     * 格式化时间戳为日期时间字符串（yyyy-MM-dd HH:mm:ss）
     */
    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormatter.get()?.format(Date(timestamp)) ?: ""
    }

    /**
     * 获取当前日期字符串（yyyy-MM-dd）
     */
    fun getCurrentDateString(): String {
        return formatDate(System.currentTimeMillis())
    }

    /**
     * 获取当前日期时间字符串（yyyy-MM-dd HH:mm:ss）
     */
    fun getCurrentDateTimeString(): String {
        return formatDateTime(System.currentTimeMillis())
    }

    /**
     * 解析日期字符串为时间戳
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
     */
    fun getYesterdayDateString(): String {
        val yesterday = System.currentTimeMillis() - MemoryConstants.ONE_DAY_MILLIS
        return formatDate(yesterday)
    }

    /**
     * 格式化时间戳为简短时间字符串（HH:mm）
     */
    fun formatTimestamp(timestamp: Long): String {
        return formatDateTime(timestamp).substring(11, 16)
    }

    /**
     * 判断两个日期字符串是否为同一天
     */
    fun isSameDay(date1: String, date2: String): Boolean {
        return date1 == date2
    }
}
