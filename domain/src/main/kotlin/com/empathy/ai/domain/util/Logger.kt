package com.empathy.ai.domain.util

/**
 * 日志接口
 *
 * 用于在Domain层进行日志记录，不依赖Android SDK。
 * 实现类在Data层或App层提供（如AndroidLogger）。
 *
 * @see TDD-00017 Clean Architecture模块化改造技术设计
 */
interface Logger {
    /**
     * 调试级别日志
     *
     * @param tag 日志标签
     * @param message 日志消息
     */
    fun d(tag: String, message: String)

    /**
     * 错误级别日志
     *
     * @param tag 日志标签
     * @param message 日志消息
     * @param throwable 可选的异常信息
     */
    fun e(tag: String, message: String, throwable: Throwable? = null)

    /**
     * 警告级别日志
     *
     * @param tag 日志标签
     * @param message 日志消息
     */
    fun w(tag: String, message: String)

    /**
     * 信息级别日志
     *
     * @param tag 日志标签
     * @param message 日志消息
     */
    fun i(tag: String, message: String)

    /**
     * 详细级别日志
     *
     * @param tag 日志标签
     * @param message 日志消息
     */
    fun v(tag: String, message: String)
}
