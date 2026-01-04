package com.empathy.ai.domain.model

/**
 * 消息发送状态枚举
 *
 * 用于跟踪消息的发送状态，支持UI显示发送进度和重试功能
 */
enum class SendStatus {
    /** 发送中 */
    PENDING,
    
    /** 发送成功 */
    SUCCESS,
    
    /** 发送失败 */
    FAILED
}
