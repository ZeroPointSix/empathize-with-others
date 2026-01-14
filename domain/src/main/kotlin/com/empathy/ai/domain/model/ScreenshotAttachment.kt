package com.empathy.ai.domain.model

/**
 * 截图附件（临时文件）
 */
data class ScreenshotAttachment(
    val id: String,
    val localPath: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val createdAt: Long
)
