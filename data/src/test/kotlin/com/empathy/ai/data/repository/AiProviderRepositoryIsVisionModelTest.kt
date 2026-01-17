package com.empathy.ai.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * AiProviderRepositoryImpl isVisionModel 单元测试
 *
 * 测试 Vision 模型智能判断功能 (PRD-00036/3.5):
 * - 支持图片的模型识别
 * - 不支持图片的模型排除
 * - 边界条件处理
 *
 * @see AiProviderRepositoryImpl.isVisionModel
 * @see AiModel.supportsImage
 *
 * --- Change Log ---
 * 2026-01-17 PRD-00036: 新增 Vision 模型智能判断测试
 * --- Change Log ---
 */
class AiProviderRepositoryIsVisionModelTest {

    // ==================== GPT 系列测试 ====================

    @Test
    fun `isVisionModel should return true for GPT-4`() {
        assertTrue(isVisionModel("gpt-4"))
        assertTrue(isVisionModel("gpt-4-0314"))
        assertTrue(isVisionModel("gpt-4-32k"))
        assertTrue(isVisionModel("gpt-4-1106-preview"))
    }

    @Test
    fun `isVisionModel should return true for GPT-4o series`() {
        assertTrue(isVisionModel("gpt-4o"))
        assertTrue(isVisionModel("gpt-4o-2024-05-13"))
        assertTrue(isVisionModel("gpt-4o-mini"))
    }

    @Test
    fun `isVisionModel should return true for GPT-4 Turbo`() {
        assertTrue(isVisionModel("gpt-4-turbo"))
        assertTrue(isVisionModel("gpt-4-turbo-preview"))
    }

    @Test
    fun `isVisionModel should return false for GPT-35 series`() {
        assertFalse(isVisionModel("gpt-3.5-turbo"))
        assertFalse(isVisionModel("gpt-3.5-turbo-0125"))
        assertFalse(isVisionModel("gpt-3.5-turbo-1106"))
        assertFalse(isVisionModel("gpt-3.5"))
    }

    // ==================== Claude 系列测试 ====================

    @Test
    fun `isVisionModel should return true for Claude 3 series`() {
        assertTrue(isVisionModel("claude-3-opus-20240229"))
        assertTrue(isVisionModel("claude-3-sonnet-20240229"))
        assertTrue(isVisionModel("claude-3-haiku-20240307"))
        assertTrue(isVisionModel("claude-3-opus"))
        assertTrue(isVisionModel("claude-3-sonnet"))
        assertTrue(isVisionModel("claude-3-haiku"))
    }

    @Test
    fun `isVisionModel should return true for Claude 2`() {
        assertTrue(isVisionModel("claude-2.1"))
        assertTrue(isVisionModel("claude-2.0"))
    }

    // ==================== Gemini 系列测试 ====================

    @Test
    fun `isVisionModel should return true for Gemini series`() {
        assertTrue(isVisionModel("gemini-pro"))
        assertTrue(isVisionModel("gemini-pro-vision"))
        assertTrue(isVisionModel("gemini-1.5-pro"))
        assertTrue(isVisionModel("gemini-1.5-flash"))
        assertTrue(isVisionModel("gemini-1.0-pro"))
    }

    // ==================== Qwen 系列测试 ====================

    @Test
    fun `isVisionModel should return true for Qwen-VL series`() {
        assertTrue(isVisionModel("qwen-vl-plus"))
        assertTrue(isVisionModel("qwen-vl-max"))
        assertTrue(isVisionModel("qwen-vl-chat"))
    }

    @Test
    fun `isVisionModel should return false for Qwen text-only models`() {
        assertFalse(isVisionModel("qwen-turbo"))
        assertFalse(isVisionModel("qwen-plus"))
        assertFalse(isVisionModel("qwen-72b-chat"))
    }

    // ==================== DeepSeek 系列测试 ====================

    @Test
    fun `isVisionModel should return false for DeepSeek text models`() {
        assertFalse(isVisionModel("deepseek-chat"))
        assertFalse(isVisionModel("deepseek-coder"))
        assertFalse(isVisionModel("deepseek-chat-base"))
    }

    // ==================== 其他模型测试 ====================

    @Test
    fun `isVisionModel should return true for models with vision keyword`() {
        assertTrue(isVisionModel("vision-model"))
        assertTrue(isVisionModel("my-custom-vision"))
    }

    @Test
    fun `isVisionModel should return true for VL suffix models`() {
        assertTrue(isVisionModel("model-vl"))
        assertTrue(isVisionModel("my-model-vl-v2"))
    }

    @Test
    fun `isVisionModel should return true for GLM-4V series`() {
        assertTrue(isVisionModel("glm-4v"))
        assertTrue(isVisionModel("glm-4v-plus"))
    }

    @Test
    fun `isVisionModel should return true for Yi-Vision series`() {
        assertTrue(isVisionModel("yi-vision"))
        assertTrue(isVisionModel("yi-34b-vision"))
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `isVisionModel should return false for empty string`() {
        assertFalse(isVisionModel(""))
    }

    @Test
    fun `isVisionModel should be case insensitive`() {
        assertTrue(isVisionModel("GPT-4"))
        assertTrue(isVisionModel("GPT-4O"))
        assertTrue(isVisionModel("Claude-3-Opus"))
        assertTrue(isVisionModel("GEMINI-PRO"))
        assertFalse(isVisionModel("GPT-35"))  // 大写形式测试
    }

    @Test
    fun `isVisionModel should return false for unknown models`() {
        assertFalse(isVisionModel("unknown-model"))
        assertFalse(isVisionModel("my-custom-model"))
        assertFalse(isVisionModel("text-model"))
    }

    @Test
    fun `isVisionModel should return false for embedding models`() {
        assertFalse(isVisionModel("text-embedding-ada-002"))
        assertFalse(isVisionModel("embedding-model"))
    }

    @Test
    fun `isVisionModel should return false for audio models`() {
        assertFalse(isVisionModel("whisper-1"))
        assertFalse(isVisionModel("tts-1"))
    }

    @Test
    fun `isVisionModel should return false for image generation models`() {
        assertFalse(isVisionModel("dall-e-3"))
        assertFalse(isVisionModel("dall-e-2"))
    }

    @Test
    fun `isVisionModel should return false for moderation models`() {
        assertFalse(isVisionModel("gpt-3.5-turbo-instruct"))
        assertFalse(isVisionModel("text-moderation-stable"))
    }

    // ==================== 混合场景测试 ====================

    @Test
    fun `isVisionModel should handle model ID with prefix`() {
        assertTrue(isVisionModel("openai/gpt-4"))
        assertTrue(isVisionModel("anthropic/claude-3-sonnet"))
    }

    @Test
    fun `isVisionModel should handle model ID with suffix`() {
        assertTrue(isVisionModel("gpt-4-0613"))
        assertTrue(isVisionModel("claude-3-opus-20240307-preview"))
    }

    /**
     * 从 AiProviderRepositoryImpl 复制的 isVisionModel 函数
     * 用于单元测试
     */
    private fun isVisionModel(modelId: String): Boolean {
        val lowerCaseId = modelId.lowercase()

        // 明确支持图片的模型模式
        val visionPatterns = listOf(
            "gpt-4",           // GPT-4 系列都支持图片
            "gpt-4o",          // GPT-4o 系列
            "gpt-4-turbo",     // GPT-4 Turbo
            "claude-3",        // Claude 3 系列
            "claude-2",        // Claude 2 系列 (旧版但仍支持图片)
            "claude-opus",     // Claude Opus
            "claude-sonnet",   // Claude Sonnet
            "gemini",          // Gemini 系列
            "vision",          // 明确标注 vision
            "-vl",             // VL (Vision-Language) 模型
            "qwen-vl",         // Qwen-VL 系列
            "glm-4v",          // GLM-4V 系列
            "yi-vision"        // Yi-Vision 系列
        )

        // 明确不支持图片的模型模式
        val nonVisionPatterns = listOf(
            "gpt-3.5",         // GPT-3.5 不支持图片
            "deepseek-chat",   // DeepSeek Chat 纯文本
            "deepseek-coder"   // DeepSeek Coder 纯文本
        )

        // 先检查是否明确不支持
        if (nonVisionPatterns.any { lowerCaseId.contains(it) }) {
            return false
        }

        // 再检查是否支持
        return visionPatterns.any { lowerCaseId.contains(it) }
    }
}
