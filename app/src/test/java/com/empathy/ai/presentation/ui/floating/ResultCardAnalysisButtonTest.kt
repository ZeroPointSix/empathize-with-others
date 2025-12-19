package com.empathy.ai.presentation.ui.floating

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.empathy.ai.R
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.PolishResult
import com.empathy.ai.domain.model.ReplyResult
import com.empathy.ai.domain.model.RiskLevel
import com.google.android.material.button.MaterialButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * ResultCard分析模式按钮可见性测试
 *
 * 测试场景：
 * 1. 分析结果显示时按钮应该可见
 * 2. 长内容分析结果按钮应该可见
 * 3. 三种结果类型的按钮都应该可见
 *
 * @see BUG-00018 分析模式复制/重新生成按钮不可见问题分析
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ResultCardAnalysisButtonTest {

    private lateinit var context: Context
    private lateinit var resultCard: ResultCard

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        resultCard = ResultCard(context)
    }

    // ==================== 基础可见性测试 ====================

    @Test
    fun `分析结果显示时复制按钮应该可见`() {
        // Given
        val result = AnalysisResult(
            strategyAnalysis = "测试分析内容",
            replySuggestion = "测试建议内容",
            riskLevel = RiskLevel.SAFE
        )

        // When
        resultCard.showAnalysisResult(result)

        // Then
        val btnCopy = resultCard.findViewById<MaterialButton>(R.id.btn_copy)
        assertNotNull("复制按钮不应为null", btnCopy)
        assertEquals("复制按钮应该可见", View.VISIBLE, btnCopy.visibility)
    }

    @Test
    fun `分析结果显示时重新生成按钮应该可见`() {
        // Given
        val result = AnalysisResult(
            strategyAnalysis = "测试分析内容",
            replySuggestion = "测试建议内容",
            riskLevel = RiskLevel.SAFE
        )

        // When
        resultCard.showAnalysisResult(result)

        // Then
        val btnRegenerate = resultCard.findViewById<MaterialButton>(R.id.btn_regenerate)
        assertNotNull("重新生成按钮不应为null", btnRegenerate)
        assertEquals("重新生成按钮应该可见", View.VISIBLE, btnRegenerate.visibility)
    }

    // ==================== 长内容测试 ====================

    @Test
    fun `超长分析内容时复制按钮应该可见`() {
        // Given - 模拟超长分析内容
        val longAnalysis = "这是一段很长的分析内容。".repeat(50)
        val longSuggestion = "这是一段很长的建议内容。".repeat(50)
        val result = AnalysisResult(
            strategyAnalysis = longAnalysis,
            replySuggestion = longSuggestion,
            riskLevel = RiskLevel.SAFE
        )

        // When
        resultCard.showAnalysisResult(result)

        // Then
        val btnCopy = resultCard.findViewById<MaterialButton>(R.id.btn_copy)
        assertNotNull("复制按钮不应为null", btnCopy)
        assertEquals("超长内容时复制按钮应该可见", View.VISIBLE, btnCopy.visibility)
    }

    @Test
    fun `超长分析内容时重新生成按钮应该可见`() {
        // Given - 模拟超长分析内容
        val longAnalysis = "这是一段很长的分析内容。".repeat(50)
        val longSuggestion = "这是一段很长的建议内容。".repeat(50)
        val result = AnalysisResult(
            strategyAnalysis = longAnalysis,
            replySuggestion = longSuggestion,
            riskLevel = RiskLevel.SAFE
        )

        // When
        resultCard.showAnalysisResult(result)

        // Then
        val btnRegenerate = resultCard.findViewById<MaterialButton>(R.id.btn_regenerate)
        assertNotNull("重新生成按钮不应为null", btnRegenerate)
        assertEquals("超长内容时重新生成按钮应该可见", View.VISIBLE, btnRegenerate.visibility)
    }

    @Test
    fun `极端长度分析内容时按钮应该可见`() {
        // Given - 模拟极端长度内容（1000字符）
        val extremeAnalysis = "A".repeat(1000)
        val extremeSuggestion = "B".repeat(1000)
        val result = AnalysisResult(
            strategyAnalysis = extremeAnalysis,
            replySuggestion = extremeSuggestion,
            riskLevel = RiskLevel.SAFE
        )

        // When
        resultCard.showAnalysisResult(result)

        // Then
        val btnCopy = resultCard.findViewById<MaterialButton>(R.id.btn_copy)
        val btnRegenerate = resultCard.findViewById<MaterialButton>(R.id.btn_regenerate)
        
        assertEquals("极端长度时复制按钮应该可见", View.VISIBLE, btnCopy?.visibility)
        assertEquals("极端长度时重新生成按钮应该可见", View.VISIBLE, btnRegenerate?.visibility)
    }

    // ==================== 三种结果类型对比测试 ====================

    @Test
    fun `润色结果显示时按钮应该可见`() {
        // Given
        val result = PolishResult(
            polishedText = "润色后的文本内容",
            hasRisk = false,
            riskWarning = null
        )

        // When
        resultCard.showPolishResult(result)

        // Then
        val btnCopy = resultCard.findViewById<MaterialButton>(R.id.btn_copy)
        val btnRegenerate = resultCard.findViewById<MaterialButton>(R.id.btn_regenerate)
        
        assertEquals("润色结果复制按钮应该可见", View.VISIBLE, btnCopy?.visibility)
        assertEquals("润色结果重新生成按钮应该可见", View.VISIBLE, btnRegenerate?.visibility)
    }

    @Test
    fun `回复结果显示时按钮应该可见`() {
        // Given
        val result = ReplyResult(
            suggestedReply = "建议的回复内容",
            strategyNote = "策略说明"
        )

        // When
        resultCard.showReplyResult(result)

        // Then
        val btnCopy = resultCard.findViewById<MaterialButton>(R.id.btn_copy)
        val btnRegenerate = resultCard.findViewById<MaterialButton>(R.id.btn_regenerate)
        
        assertEquals("回复结果复制按钮应该可见", View.VISIBLE, btnCopy?.visibility)
        assertEquals("回复结果重新生成按钮应该可见", View.VISIBLE, btnRegenerate?.visibility)
    }

    @Test
    fun `三种结果类型切换时按钮始终可见`() {
        // Given
        val analysisResult = AnalysisResult(
            strategyAnalysis = "分析内容",
            replySuggestion = "建议内容",
            riskLevel = RiskLevel.SAFE
        )
        val polishResult = PolishResult(
            polishedText = "润色内容",
            hasRisk = false,
            riskWarning = null
        )
        val replyResult = ReplyResult(
            suggestedReply = "回复内容",
            strategyNote = "策略说明"
        )

        // When & Then - 分析结果
        resultCard.showAnalysisResult(analysisResult)
        assertEquals(View.VISIBLE, resultCard.findViewById<MaterialButton>(R.id.btn_copy)?.visibility)
        assertEquals(View.VISIBLE, resultCard.findViewById<MaterialButton>(R.id.btn_regenerate)?.visibility)

        // When & Then - 润色结果
        resultCard.showPolishResult(polishResult)
        assertEquals(View.VISIBLE, resultCard.findViewById<MaterialButton>(R.id.btn_copy)?.visibility)
        assertEquals(View.VISIBLE, resultCard.findViewById<MaterialButton>(R.id.btn_regenerate)?.visibility)

        // When & Then - 回复结果
        resultCard.showReplyResult(replyResult)
        assertEquals(View.VISIBLE, resultCard.findViewById<MaterialButton>(R.id.btn_copy)?.visibility)
        assertEquals(View.VISIBLE, resultCard.findViewById<MaterialButton>(R.id.btn_regenerate)?.visibility)
    }

    // ==================== 风险等级测试 ====================

    @Test
    fun `不同风险等级的分析结果按钮都应该可见`() {
        // Test SAFE
        resultCard.showAnalysisResult(AnalysisResult(
            strategyAnalysis = "安全分析",
            replySuggestion = "安全建议",
            riskLevel = RiskLevel.SAFE
        ))
        assertEquals(View.VISIBLE, resultCard.findViewById<MaterialButton>(R.id.btn_copy)?.visibility)

        // Test WARNING
        resultCard.showAnalysisResult(AnalysisResult(
            strategyAnalysis = "警告分析",
            replySuggestion = "警告建议",
            riskLevel = RiskLevel.WARNING
        ))
        assertEquals(View.VISIBLE, resultCard.findViewById<MaterialButton>(R.id.btn_copy)?.visibility)

        // Test DANGER
        resultCard.showAnalysisResult(AnalysisResult(
            strategyAnalysis = "危险分析",
            replySuggestion = "危险建议",
            riskLevel = RiskLevel.DANGER
        ))
        assertEquals(View.VISIBLE, resultCard.findViewById<MaterialButton>(R.id.btn_copy)?.visibility)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `空内容分析结果按钮应该可见`() {
        // Given
        val result = AnalysisResult(
            strategyAnalysis = "",
            replySuggestion = "",
            riskLevel = RiskLevel.SAFE
        )

        // When
        resultCard.showAnalysisResult(result)

        // Then
        val btnCopy = resultCard.findViewById<MaterialButton>(R.id.btn_copy)
        val btnRegenerate = resultCard.findViewById<MaterialButton>(R.id.btn_regenerate)
        
        assertEquals("空内容时复制按钮应该可见", View.VISIBLE, btnCopy?.visibility)
        assertEquals("空内容时重新生成按钮应该可见", View.VISIBLE, btnRegenerate?.visibility)
    }

    @Test
    fun `只有分析内容无建议时按钮应该可见`() {
        // Given
        val result = AnalysisResult(
            strategyAnalysis = "只有分析内容，没有建议",
            replySuggestion = "",
            riskLevel = RiskLevel.SAFE
        )

        // When
        resultCard.showAnalysisResult(result)

        // Then
        assertEquals(View.VISIBLE, resultCard.findViewById<MaterialButton>(R.id.btn_copy)?.visibility)
        assertEquals(View.VISIBLE, resultCard.findViewById<MaterialButton>(R.id.btn_regenerate)?.visibility)
    }

    @Test
    fun `只有建议内容无分析时按钮应该可见`() {
        // Given
        val result = AnalysisResult(
            strategyAnalysis = "",
            replySuggestion = "只有建议内容，没有分析",
            riskLevel = RiskLevel.SAFE
        )

        // When
        resultCard.showAnalysisResult(result)

        // Then
        assertEquals(View.VISIBLE, resultCard.findViewById<MaterialButton>(R.id.btn_copy)?.visibility)
        assertEquals(View.VISIBLE, resultCard.findViewById<MaterialButton>(R.id.btn_regenerate)?.visibility)
    }

    // ==================== 多次调用测试 ====================

    @Test
    fun `多次显示分析结果按钮始终可见`() {
        // Given
        val result = AnalysisResult(
            strategyAnalysis = "分析内容",
            replySuggestion = "建议内容",
            riskLevel = RiskLevel.SAFE
        )

        // When - 多次调用
        repeat(5) {
            resultCard.showAnalysisResult(result)
            
            // Then - 每次都应该可见
            assertEquals(View.VISIBLE, resultCard.findViewById<MaterialButton>(R.id.btn_copy)?.visibility)
            assertEquals(View.VISIBLE, resultCard.findViewById<MaterialButton>(R.id.btn_regenerate)?.visibility)
        }
    }

    @Test
    fun `清空结果后重新显示按钮应该可见`() {
        // Given
        val result = AnalysisResult(
            strategyAnalysis = "分析内容",
            replySuggestion = "建议内容",
            riskLevel = RiskLevel.SAFE
        )

        // When - 显示 -> 清空 -> 重新显示
        resultCard.showAnalysisResult(result)
        resultCard.clearResult()
        resultCard.showAnalysisResult(result)

        // Then
        assertEquals(View.VISIBLE, resultCard.findViewById<MaterialButton>(R.id.btn_copy)?.visibility)
        assertEquals(View.VISIBLE, resultCard.findViewById<MaterialButton>(R.id.btn_regenerate)?.visibility)
    }
}
