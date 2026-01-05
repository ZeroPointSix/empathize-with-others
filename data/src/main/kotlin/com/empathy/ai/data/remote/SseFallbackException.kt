package com.empathy.ai.data.remote

import java.io.IOException

/**
 * SSE降级异常
 *
 * 当SSE连接连续失败达到阈值时抛出此异常，
 * 触发降级到非流式模式。
 *
 * 业务背景 (FD-00028):
 * - SSE连接可能因网络不稳定而失败
 * - 连续失败3次后应降级到非流式模式
 * - 降级后使用普通HTTP请求获取完整响应
 *
 * @param message 错误描述
 * @param cause 原始异常
 *
 * @see SseStreamReader 流式读取器
 * @see FD-00028 AI军师流式对话升级功能设计
 */
class SseFallbackException(
    message: String,
    cause: Throwable? = null
) : IOException(message, cause)
