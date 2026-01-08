package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AiResult 单元测试
 *
 * @see AiResult
 * @see TDD-00009 悬浮窗功能重构技术设计
 */
class AiResultTest {

    // ==================== Analysis 子类测试 ====================

    @Test
    fun `Analysis getCopyableText 返回回复建议`() {
        val analysisResult = AnalysisResult(
            replySuggestion = "建议的回复",
            strategyAnalysis = "策略分析",
            riskLevel = RiskLevel.SAFE
        )
        val aiResult = AiResult.Analysis(analysisResult)

        assertEquals("建议的回复", aiResult.getCopyableText())
    }

    @Test
    fun `Analysis getDisplayContent 返回格式化内容`() {
        val analysisResult = AnalysisResult(
            replySuggestion = "建议的回复",
            strategyAnalysis = "策略分析内容",
            riskLevel = RiskLevel.SAFE
        )
        val aiResult = AiResult.Analysis(analysisResult)

        val content = aiResult.getDisplayContent()
        assertTrue(content.contains("【军师分析】"))
        assertTrue(content.contains("策略分析内容"))
        assertTrue(content.contains("【话术建议】"))
        assertTrue(content.contains("建议的回复"))
    }

    @Test
    fun `Analysis getActionType 返回 ANALYZE`() {
        val analysisResult = AnalysisResult(
            replySuggestion = "回复",
            strategyAnalysis = "分析",
            riskLevel = RiskLevel.SAFE
        )
        val aiResult = AiResult.Analysis(analysisResult)

        assertEquals(ActionType.ANALYZE, aiResult.getActionType())
    }

    // ==================== Polish 子类测试 ====================

    @Test
    fun `Polish getCopyableText 返回润色后的文本`() {
        val polishResult = PolishResult(
            polishedText = "润色后的文本",
            hasRisk = false,
            riskWarning = null
        )
        val aiResult = AiResult.Polish(polishResult)

        assertEquals("润色后的文本", aiResult.getCopyableText())
    }

    @Test
    fun `Polish getDisplayContent 无风险时返回纯文本`() {
        val polishResult = PolishResult(
            polishedText = "润色后的文本",
            hasRisk = false,
            riskWarning = null
        )
        val aiResult = AiResult.Polish(polishResult)

        assertEquals("润色后的文本", aiResult.getDisplayContent())
    }

    @Test
    fun `Polish getDisplayContent 有风险时包含警告`() {
        val polishResult = PolishResult(
            polishedText = "润色后的文本",
            hasRisk = true,
            riskWarning = "风险提示"
        )
        val aiResult = AiResult.Polish(polishResult)

        val content = aiResult.getDisplayContent()
        assertTrue(content.contains("润色后的文本"))
        assertTrue(content.contains("⚠️ 风险提示"))
    }

    @Test
    fun `Polish getActionType 返回 POLISH`() {
        val polishResult = PolishResult(polishedText = "文本")
        val aiResult = AiResult.Polish(polishResult)

        assertEquals(ActionType.POLISH, aiResult.getActionType())
    }

    // ==================== Reply 子类测试 ====================

    @Test
    fun `Reply getCopyableText 返回建议回复`() {
        val replyResult = ReplyResult(
            suggestedReply = "建议的回复",
            strategyNote = "策略说明"
        )
        val aiResult = AiResult.Reply(replyResult)

        assertEquals("建议的回复", aiResult.getCopyableText())
    }

    @Test
    fun `Reply getDisplayContent 无策略时返回纯回复`() {
        val replyResult = ReplyResult(
            suggestedReply = "建议的回复",
            strategyNote = null
        )
        val aiResult = AiResult.Reply(replyResult)

        assertEquals("建议的回复", aiResult.getDisplayContent())
    }

    @Test
    fun `Reply getDisplayContent 有策略时包含说明`() {
        val replyResult = ReplyResult(
            suggestedReply = "建议的回复",
            strategyNote = "策略说明"
        )
        val aiResult = AiResult.Reply(replyResult)

        val content = aiResult.getDisplayContent()
        assertTrue(content.contains("建议的回复"))
        assertTrue(content.contains("💡 策略说明"))
    }

    @Test
    fun `Reply getActionType 返回 REPLY`() {
        val replyResult = ReplyResult(suggestedReply = "回复")
        val aiResult = AiResult.Reply(replyResult)

        assertEquals(ActionType.REPLY, aiResult.getActionType())
    }

    // ==================== 密封类测试 ====================

    @Test
    fun `when 表达式覆盖所有子类`() {
        val results = listOf(
            AiResult.Analysis(
                AnalysisResult("回复", "分析", RiskLevel.SAFE)
            ),
            AiResult.Polish(
                PolishResult("文本")
            ),
            AiResult.Reply(
                ReplyResult("回复")
            ),
            // TD-00031: 新增 Knowledge 类型的测试
            AiResult.Knowledge(
                KnowledgeQueryResponse(
                    title = "测试标题",
                    content = "测试内容"
                )
            )
        )

        results.forEach { result ->
            val type = when (result) {
                is AiResult.Analysis -> "analysis"
                is AiResult.Polish -> "polish"
                is AiResult.Reply -> "reply"
                is AiResult.Knowledge -> "knowledge"  // TD-00031: 新增分支
            }
            assertTrue(type.isNotEmpty())
        }
    }

    @Test
    fun `is 检查正确识别子类`() {
        val analysis = AiResult.Analysis(
            AnalysisResult("回复", "分析", RiskLevel.SAFE)
        )
        val polish = AiResult.Polish(PolishResult("文本"))
        val reply = AiResult.Reply(ReplyResult("回复"))

        assertTrue(analysis is AiResult.Analysis)
        assertTrue(polish is AiResult.Polish)
        assertTrue(reply is AiResult.Reply)
    }
}
