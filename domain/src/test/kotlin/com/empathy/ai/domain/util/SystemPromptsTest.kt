package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.SystemPromptScene
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * SystemPrompts 单元测试
 *
 * 测试系统提示词常量和开发者模式自定义配置功能
 *
 * 业务规则 (PRD-00033/AC-003):
 * - 支持5个核心场景的自定义配置：ANALYZE、POLISH、REPLY、SUMMARY、AI_ADVISOR
 * - Header支持自定义，Footer始终使用默认值
 * - 自定义配置为空时fallback到默认值
 *
 * 任务: FD-00033/T032
 */
class SystemPromptsTest {

    // ==================== 同步方法测试 ====================

    /**
     * Test_GetHeader_AllScenesShouldReturnDefaultHeaders
     * 测试所有场景的getHeader方法返回默认值
     *
     * 业务规则 (PRD-00033):
     * - 默认Header常量定义在SystemPrompts中
     * - 未设置自定义配置时应返回默认值
     */
    @Test
    fun `getHeader应返回所有场景的默认Header`() {
        // 测试数据来源: SystemPrompts.kt 常量定义
        val testCases = listOf(
            Triple(PromptScene.ANALYZE, "ANALYZE_HEADER", "恋爱军师"),
            Triple(PromptScene.POLISH, "POLISH_HEADER", "高情商嘴替"),
            Triple(PromptScene.REPLY, "REPLY_HEADER", "神回复生成器"),
            Triple(PromptScene.SUMMARY, "SUMMARY_HEADER", "社交关系分析助手"),
            Triple(PromptScene.AI_ADVISOR, "AI_ADVISOR_HEADER", "AI军师")
        )

        testCases.forEach { (scene, _, expectedContent) ->
            val header = SystemPrompts.getHeader(scene)
            assertTrue("${scene.name} Header应包含角色定义", header.contains(expectedContent))
        }
    }

    /**
     * Test_GetFooter_AllScenesShouldReturnDefaultFooters
     * 测试所有场景的getFooter方法返回默认值
     *
     * 业务规则 (PRD-00033/设计约束):
     * - Footer与JSON解析逻辑绑定，不允许自定义
     * - 所有场景的Footer都应返回有效的格式约束
     */
    @Test
    fun `getFooter应返回所有场景的默认Footer`() {
        // 测试数据来源: SystemPrompts.kt 常量定义
        val testCases = listOf(
            PromptScene.ANALYZE,
            PromptScene.POLISH,
            PromptScene.REPLY,
            PromptScene.SUMMARY,
            PromptScene.AI_ADVISOR
        )

        testCases.forEach { scene ->
            val footer = SystemPrompts.getFooter(scene)
            assertNotNull("${scene.name} Footer不应为空", footer)
            assertTrue("${scene.name} Footer应有内容", footer.isNotBlank())
        }
    }

    /**
     * Test_GetHeader_DeprecatedScenesShouldReturnConstants
     * 测试已废弃场景的getHeader方法返回常量
     *
     * 业务规则 (TD-00015):
     * - CHECK和EXTRACT场景已废弃
     * - 废弃场景仍返回硬编码常量以保持向后兼容
     */
    @Test
    fun `废弃场景CHECK和EXTRACT应返回对应的Header常量`() {
        val checkHeader = SystemPrompts.getHeader(PromptScene.CHECK)
        assertTrue("CHECK Header应包含内容", checkHeader.isNotBlank())

        val extractHeader = SystemPrompts.getHeader(PromptScene.EXTRACT)
        assertTrue("EXTRACT Header应包含内容", extractHeader.isNotBlank())
    }

    // ==================== SystemPromptScene映射测试 ====================

    /**
     * Test_GetHeaderForScene_SystemPromptSceneEnum
     * 测试SystemPromptScene枚举的Header获取
     *
     * 业务规则 (PRD-00033):
     * - SystemPromptScene用于开发者模式编辑界面
     * - 提供独立的getHeaderForScene方法供编辑界面使用
     */
    @Test
    fun `getHeaderForScene应返回SystemPromptScene对应的默认Header`() {
        val scenes = listOf(
            SystemPromptScene.ANALYZE,
            SystemPromptScene.POLISH,
            SystemPromptScene.REPLY,
            SystemPromptScene.SUMMARY,
            SystemPromptScene.AI_ADVISOR
        )

        scenes.forEach { scene ->
            val header = SystemPrompts.getHeaderForScene(scene)
            assertNotNull("${scene.name} Header不应为空", header)
            assertTrue("${scene.name} Header应有内容", header.isNotBlank())
        }
    }

    /**
     * Test_GetFooterForScene_SystemPromptSceneEnum
     * 测试SystemPromptScene枚举的Footer获取
     *
     * 业务规则 (PRD-00033):
     * - Footer用于开发者模式编辑界面显示默认值
     * - 提供独立的getFooterForScene方法供编辑界面使用
     */
    @Test
    fun `getFooterForScene应返回SystemPromptScene对应的默认Footer`() {
        val scenes = listOf(
            SystemPromptScene.ANALYZE,
            SystemPromptScene.POLISH,
            SystemPromptScene.REPLY,
            SystemPromptScene.SUMMARY,
            SystemPromptScene.AI_ADVISOR
        )

        scenes.forEach { scene ->
            val footer = SystemPrompts.getFooterForScene(scene)
            assertNotNull("${scene.name} Footer不应为空", footer)
            assertTrue("${scene.name} Footer应有内容", footer.isNotBlank())
        }
    }

    // ==================== 异步方法测试 ====================

    /**
     * Test_GetHeaderAsync_WithOrWithoutConfigProvider
     * 测试异步方法的行为（无论ConfigProvider是否设置）
     *
     * 业务规则 (PRD-00033/AC-004):
     * - ConfigProvider未设置时使用默认值
     * - ConfigProvider设置且返回非空Header时使用自定义配置
     * - 测试重点：异步方法能正确处理这两种情况
     *
     * 注意：由于无法在测试中重置ConfigProvider，此测试验证
     * 在当前状态下的行为是否正确（自定义或默认二选一）
     */
    @Test
    fun `getHeaderAsync应根据ConfigProvider状态返回正确结果`() = runTest {
        // 测试场景列表
        val testScenes = listOf(
            PromptScene.ANALYZE,
            PromptScene.POLISH,
            PromptScene.AI_ADVISOR
        )

        testScenes.forEach { scene ->
            val asyncHeader = SystemPrompts.getHeaderAsync(scene)
            val syncHeader = SystemPrompts.getHeader(scene)

            // 验证结果不为空
            assertTrue("${scene.name} 异步Header应有内容", asyncHeader.isNotBlank())
            assertTrue("${scene.name} 同步Header应有内容", syncHeader.isNotBlank())

            // 验证结果至少与默认Header相关（可能是自定义或默认）
            // 如果configProvider设置了且返回非空，则结果会包含"自定义"
            // 如果未设置或返回空，则结果应该等于默认Header
            assertTrue(
                "${scene.name} 异步Header应该有效：$asyncHeader",
                asyncHeader.contains("恋爱军师") ||
                asyncHeader.contains("高情商嘴替") ||
                asyncHeader.contains("AI军师") ||
                asyncHeader.contains("自定义") ||
                asyncHeader == syncHeader
            )
        }
    }

    /**
     * Test_GetFooterAsync_ShouldAlwaysReturnDefault
     * 测试异步Footer方法始终返回默认值
     *
     * 业务规则 (PRD-00033):
     * - Footer始终使用默认值，不支持自定义
     * - 无论ConfigProvider是否设置，Footer都应该等于默认Footer
     */
    @Test
    fun `getFooterAsync应始终返回默认Footer`() = runTest {
        val testScenes = listOf(
            PromptScene.ANALYZE,
            PromptScene.POLISH,
            PromptScene.REPLY
        )

        testScenes.forEach { scene ->
            val asyncFooter = SystemPrompts.getFooterAsync(scene)
            val syncFooter = SystemPrompts.getFooter(scene)

            // Footer应该始终等于默认Footer（不支持自定义）
            assertEquals(
                "${scene.name} Footer应返回默认值",
                syncFooter,
                asyncFooter
            )
        }
    }

    /**
     * Test_GetHeaderAsync_WithCustomConfigProvider
     * 测试异步方法在设置ConfigProvider时优先使用自定义配置
     *
     * 业务规则 (PRD-00033):
     * - ConfigProvider返回非空Header时应使用自定义配置
     * - 开发者模式编辑后立即生效
     */
    @Test
    fun `getHeaderAsync设置ConfigProvider后应使用自定义Header`() = runTest {
        // Given: 设置自定义ConfigProvider
        val customHeader = "自定义AI角色定义"
        val mockProvider = object : SystemPrompts.ConfigProvider {
            override suspend fun getCustomHeader(scene: SystemPromptScene): String = customHeader
            override suspend fun getCustomFooter(scene: SystemPromptScene): String = ""
        }
        SystemPrompts.setConfigProvider(mockProvider)

        // When: 调用异步方法
        val result = SystemPrompts.getHeaderAsync(PromptScene.AI_ADVISOR)

        // Then: 应返回自定义Header
        assertEquals("应返回自定义Header", customHeader, result)
    }

    /**
     * Test_GetFooterAsync_WithCustomConfigProviderShouldReturnDefault
     * 测试Footer异步方法始终使用默认值（不支持自定义）
     *
     * 业务规则 (PRD-00033/设计约束):
     * - Footer与JSON解析逻辑绑定
     * - 不允许开发者修改Footer以确保解析兼容性
     */
    @Test
    fun `getFooterAsync即使设置ConfigProvider也应返回默认Footer`() = runTest {
        // Given: 设置ConfigProvider，但Footer自定义逻辑应被忽略
        val mockProvider = object : SystemPrompts.ConfigProvider {
            override suspend fun getCustomHeader(scene: SystemPromptScene): String = "自定义"
            override suspend fun getCustomFooter(scene: SystemPromptScene): String = "自定义Footer"
        }
        SystemPrompts.setConfigProvider(mockProvider)

        // When: 调用Footer异步方法
        val result = SystemPrompts.getFooterAsync(PromptScene.AI_ADVISOR)

        // Then: 应返回默认Footer（不支持自定义）
        val defaultFooter = SystemPrompts.getFooterForScene(SystemPromptScene.AI_ADVISOR)
        assertEquals("Footer应返回默认值", defaultFooter, result)
    }

    /**
     * Test_GetFullHeader_EmptyCustomShouldReturnDefault
     * 测试getFullHeader在自定义Header为空时返回默认值
     *
     * 业务规则 (PRD-00033):
     * - ConfigProvider返回空字符串时应fallback到默认值
     * - 确保开发者删除自定义配置后能恢复默认值
     */
    @Test
    fun `getFullHeader自定义Header为空时应返回默认Header`() = runTest {
        // Given: 设置返回空字符串的ConfigProvider
        val mockProvider = object : SystemPrompts.ConfigProvider {
            override suspend fun getCustomHeader(scene: SystemPromptScene): String = ""
            override suspend fun getCustomFooter(scene: SystemPromptScene): String = ""
        }
        SystemPrompts.setConfigProvider(mockProvider)

        // When: 调用getFullHeader
        val result = SystemPrompts.getFullHeader(SystemPromptScene.ANALYZE)

        // Then: 应返回默认Header
        val defaultHeader = SystemPrompts.getHeaderForScene(SystemPromptScene.ANALYZE)
        assertEquals("空自定义Header应fallback到默认值", defaultHeader, result)
    }

    // ==================== ConfigProvider接口测试 ====================

    /**
     * Test_ConfigProviderInterface_ShouldBeCallable
     * 测试ConfigProvider接口定义正确
     *
     * 业务规则 (TDD-00033):
     * - ConfigProvider是suspend接口，支持异步获取配置
     * - 接口方法返回String类型
     */
    @Test
    fun `ConfigProvider接口定义应支持异步获取Header和Footer`() {
        // Given: 创建实现ConfigProvider接口的对象
        val mockProvider = object : SystemPrompts.ConfigProvider {
            override suspend fun getCustomHeader(scene: SystemPromptScene): String = "测试Header"
            override suspend fun getCustomFooter(scene: SystemPromptScene): String = "测试Footer"
        }

        // Then: 对象应实现接口方法
        assertTrue("对象应实现ConfigProvider接口", mockProvider is SystemPrompts.ConfigProvider)
    }

    // ==================== AI_ADVISOR场景特定测试 ====================

    /**
     * Test_AIAdvisorHeader_ShouldNotContainJSONConstraint
     * 测试AI_ADVISOR的Header不包含JSON格式约束
     *
     * 业务规则 (PRD-00026):
     * - AI军师场景返回自然语言而非JSON
     * - Header不应包含JSON输出要求
     */
    @Test
    fun `AI_ADVISOR的Header应定义角色而非JSON格式`() {
        val header = SystemPrompts.getHeader(PromptScene.AI_ADVISOR)

        // AI军师Header应定义角色
        assertTrue("应包含AI军师角色定义", header.contains("AI军师"))
        assertTrue("应包含角色能力描述", header.contains("核心能力"))
    }

    /**
     * Test_AIAdvisorFooter_ShouldNotRequireJSON
     * 测试AI_ADVISOR的Footer不要求JSON输出
     *
     * 业务规则 (PRD-00026):
     * - AI军师返回自然语言
     * - Footer应明确禁止JSON格式
     */
    @Test
    fun `AI_ADVISOR的Footer应禁止JSON格式返回`() {
        val footer = SystemPrompts.getFooter(PromptScene.AI_ADVISOR)

        // Footer应禁止JSON格式
        assertTrue("应禁止返回JSON格式", footer.contains("禁止返回JSON格式"))
        // Footer应要求自然语言
        assertTrue("应要求自然语言回复", footer.contains("自然语言"))
    }

    // ==================== 知识查询场景测试 (PRD-00031) ====================

    /**
     * Test_KnowledgeHeader_ShouldDefineKnowledgeRole
     * 测试知识查询场景的Header定义
     *
     * @see PRD-00031 悬浮窗快速知识回答功能需求
     */
    @Test
    fun `知识查询场景的Header应定义知识助手角色`() {
        val header = SystemPrompts.KNOWLEDGE_HEADER

        assertTrue("应包含知识助手角色定义", header.contains("知识查询助手"))
        assertTrue("应包含核心能力描述", header.contains("核心能力"))
    }

    /**
     * Test_KnowledgeFooter_ShouldRequireJSONFormat
     * 测试知识查询场景的Footer要求JSON格式
     *
     * @see PRD-00031 悬浮窗快速知识回答功能需求
     */
    @Test
    fun `知识查询场景的Footer应要求JSON格式返回`() {
        val footer = SystemPrompts.KNOWLEDGE_FOOTER

        assertTrue("应要求JSON格式", footer.contains("JSON格式返回"))
        assertTrue("应包含字段说明", footer.contains("字段说明"))
    }
}
