package com.empathy.ai.presentation.ui.floating

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.empathy.ai.R
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.RiskLevel
import com.google.android.material.button.MaterialButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Field

/**
 * ResultCard按钮初始化测试
 *
 * BUG-00021修复验证：
 * 1. 按钮引用在初始化后不为null
 * 2. 延迟查找机制正常工作
 * 3. ensureButtonsVisible()方法正确设置按钮可见性
 *
 * @see BUG-00021 分析模式复制重新生成按钮未渲染问题
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ResultCardButtonInitTest {

    private lateinit var context: Context
    private lateinit var resultCard: ResultCard

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        resultCard = ResultCard(context)
    }

    // ==================== 按钮初始化测试 ====================

    @Test
    fun `初始化后btnCopy引用不为null`() {
        // Given - ResultCard已在setup中创建

        // When - 通过反射获取私有字段
        val btnCopyField = getPrivateField("btnCopy")
        val btnCopy = btnCopyField.get(resultCard)

        // Then
        assertNotNull("btnCopy引用不应为null", btnCopy)
    }

    @Test
    fun `初始化后btnRegenerate引用不为null`() {
        // Given - ResultCard已在setup中创建

        // When - 通过反射获取私有字段
        val btnRegenerateField = getPrivateField("btnRegenerate")
        val btnRegenerate = btnRegenerateField.get(resultCard)

        // Then
        assertNotNull("btnRegenerate引用不应为null", btnRegenerate)
    }

    @Test
    fun `初始化后所有关键视图引用不为null`() {
        // Given - ResultCard已在setup中创建

        // When & Then - 验证所有关键视图引用
        val fieldsToCheck = listOf(
            "resultCard",
            "resultScroll",
            "resultTitle",
            "resultContent",
            "btnCopy",
            "btnRegenerate"
        )

        fieldsToCheck.forEach { fieldName ->
            val field = getPrivateField(fieldName)
            val value = field.get(resultCard)
            assertNotNull("$fieldName 引用不应为null", value)
        }
    }

    // ==================== findViewById验证测试 ====================

    @Test
    fun `通过findViewById可以找到复制按钮`() {
        // Given - ResultCard已在setup中创建

        // When
        val btnCopy = resultCard.findViewById<MaterialButton>(R.id.btn_copy)

        // Then
        assertNotNull("通过findViewById应该能找到复制按钮", btnCopy)
    }

    @Test
    fun `通过findViewById可以找到重新生成按钮`() {
        // Given - ResultCard已在setup中创建

        // When
        val btnRegenerate = resultCard.findViewById<MaterialButton>(R.id.btn_regenerate)

        // Then
        assertNotNull("通过findViewById应该能找到重新生成按钮", btnRegenerate)
    }

    // ==================== ensureButtonsVisible测试 ====================

    @Test
    fun `showAnalysisResult调用后按钮可见`() {
        // Given
        val result = AnalysisResult(
            strategyAnalysis = "测试分析",
            replySuggestion = "测试建议",
            riskLevel = RiskLevel.SAFE
        )

        // When
        resultCard.showAnalysisResult(result)

        // Then
        val btnCopy = resultCard.findViewById<MaterialButton>(R.id.btn_copy)
        val btnRegenerate = resultCard.findViewById<MaterialButton>(R.id.btn_regenerate)

        assertEquals("复制按钮应该可见", View.VISIBLE, btnCopy?.visibility)
        assertEquals("重新生成按钮应该可见", View.VISIBLE, btnRegenerate?.visibility)
    }

    @Test
    fun `多次调用showAnalysisResult按钮始终可见`() {
        // Given
        val result = AnalysisResult(
            strategyAnalysis = "测试分析",
            replySuggestion = "测试建议",
            riskLevel = RiskLevel.SAFE
        )

        // When - 多次调用
        repeat(3) {
            resultCard.showAnalysisResult(result)

            // Then - 每次都验证
            val btnCopy = resultCard.findViewById<MaterialButton>(R.id.btn_copy)
            val btnRegenerate = resultCard.findViewById<MaterialButton>(R.id.btn_regenerate)

            assertEquals("第${it + 1}次调用后复制按钮应该可见", View.VISIBLE, btnCopy?.visibility)
            assertEquals("第${it + 1}次调用后重新生成按钮应该可见", View.VISIBLE, btnRegenerate?.visibility)
        }
    }

    // ==================== 按钮点击监听器测试 ====================

    @Test
    fun `复制按钮点击监听器正确设置`() {
        // Given
        var copyClicked = false
        resultCard.setOnCopyClickListener { copyClicked = true }

        val result = AnalysisResult(
            strategyAnalysis = "测试分析",
            replySuggestion = "测试建议",
            riskLevel = RiskLevel.SAFE
        )
        resultCard.showAnalysisResult(result)

        // When
        val btnCopy = resultCard.findViewById<MaterialButton>(R.id.btn_copy)
        btnCopy?.performClick()

        // Then
        assertEquals("复制按钮点击监听器应该被触发", true, copyClicked)
    }

    @Test
    fun `重新生成按钮点击监听器正确设置`() {
        // Given
        var regenerateClicked = false
        resultCard.setOnRegenerateClickListener { regenerateClicked = true }

        val result = AnalysisResult(
            strategyAnalysis = "测试分析",
            replySuggestion = "测试建议",
            riskLevel = RiskLevel.SAFE
        )
        resultCard.showAnalysisResult(result)

        // When
        val btnRegenerate = resultCard.findViewById<MaterialButton>(R.id.btn_regenerate)
        btnRegenerate?.performClick()

        // Then
        assertEquals("重新生成按钮点击监听器应该被触发", true, regenerateClicked)
    }

    // ==================== 辅助方法 ====================

    private fun getPrivateField(fieldName: String): Field {
        val field = ResultCard::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field
    }
}
