package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PromptError单元测试
 */
class PromptErrorTest {

    @Test
    fun `ValidationError should return message as getUserMessage`() {
        val error = PromptError.ValidationError(
            message = "提示词不能为空",
            errorType = PromptValidationResult.ErrorType.EMPTY_PROMPT
        )
        assertEquals("提示词不能为空", error.getUserMessage())
    }

    @Test
    fun `ValidationError should be recoverable`() {
        val error = PromptError.ValidationError(
            message = "测试",
            errorType = PromptValidationResult.ErrorType.EMPTY_PROMPT
        )
        assertTrue(error.isRecoverable())
    }

    @Test
    fun `StorageError should return friendly message`() {
        val error = PromptError.StorageError(
            message = "文件写入失败",
            cause = Exception("IO Error")
        )
        assertEquals("保存失败，请重试", error.getUserMessage())
    }

    @Test
    fun `StorageError should be recoverable`() {
        val error = PromptError.StorageError(message = "测试")
        assertTrue(error.isRecoverable())
    }

    @Test
    fun `ParseError should return friendly message`() {
        val error = PromptError.ParseError(
            message = "JSON解析失败",
            jsonContent = "{invalid json}"
        )
        assertEquals("配置文件损坏，已恢复默认设置", error.getUserMessage())
    }

    @Test
    fun `ParseError should be recoverable`() {
        val error = PromptError.ParseError(message = "测试")
        assertTrue(error.isRecoverable())
    }

    @Test
    fun `BackupError should return friendly message`() {
        val error = PromptError.BackupError(message = "备份文件不存在")
        assertEquals("备份恢复失败", error.getUserMessage())
    }

    @Test
    fun `BackupError should not be recoverable`() {
        val error = PromptError.BackupError(message = "测试")
        assertFalse(error.isRecoverable())
    }

    @Test
    fun `DatabaseError should return friendly message`() {
        val error = PromptError.DatabaseError(
            message = "数据库连接失败",
            cause = Exception("DB Error")
        )
        assertEquals("数据库操作失败，请重试", error.getUserMessage())
    }

    @Test
    fun `DatabaseError should be recoverable`() {
        val error = PromptError.DatabaseError(message = "测试")
        assertTrue(error.isRecoverable())
    }

    @Test
    fun `ValidationError should preserve errorType`() {
        val error = PromptError.ValidationError(
            message = "超过长度限制",
            errorType = PromptValidationResult.ErrorType.EXCEEDS_LENGTH_LIMIT
        )
        assertEquals(PromptValidationResult.ErrorType.EXCEEDS_LENGTH_LIMIT, error.errorType)
    }

    @Test
    fun `StorageError should preserve cause`() {
        val cause = RuntimeException("原始错误")
        val error = PromptError.StorageError(message = "存储错误", cause = cause)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `ParseError should preserve jsonContent`() {
        val json = "{\"key\": \"value\"}"
        val error = PromptError.ParseError(message = "解析错误", jsonContent = json)
        assertEquals(json, error.jsonContent)
    }

    @Test
    fun `all error types should extend Exception`() {
        val errors = listOf(
            PromptError.ValidationError("msg", PromptValidationResult.ErrorType.EMPTY_PROMPT),
            PromptError.StorageError("msg"),
            PromptError.ParseError("msg"),
            PromptError.BackupError("msg"),
            PromptError.DatabaseError("msg")
        )
        errors.forEach { error ->
            assertTrue("${error::class.simpleName} should be Exception", error is Exception)
        }
    }
}
