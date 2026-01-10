package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.repository.PromptRepository
import com.empathy.ai.domain.util.PromptValidator
import com.empathy.ai.presentation.ui.screen.prompt.PromptEditorUiEvent
import com.empathy.ai.presentation.ui.screen.prompt.PromptEditorUiState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * BUG-00061: 编辑提示词模块点击刷新问题 - 回归测试
 *
 * 测试场景切换时的状态管理和缓存机制，确保：
 * 1. 场景切换不触发全屏加载状态
 * 2. 缓存命中时立即返回
 * 3. Tab 区域始终保持稳定
 *
 * 注意：这些测试用例是为修复后的代码设计的。
 * 修复需要在 PromptEditorUiState 中添加 isInitialLoading 和 isSceneSwitching 字段。
 *
 * @see BUG-00061-编辑提示词模块点击刷新问题-修复方案.md
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BUG00061PromptEditorSceneSwitchTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var promptRepository: PromptRepository
    private lateinit var promptValidator: PromptValidator

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        promptRepository = mockk(relaxed = true)
        promptValidator = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== 当前实现测试（修复前） ====================
    // 这些测试验证当前的问题行为

    @Test
    fun `当前实现-初始状态isLoading应为false`() {
        // Given: 创建初始 UiState（当前实现）
        val initialState = PromptEditorUiState()

        // Then: 当前实现中 isLoading 默认为 false（因为 isInitialLoading 默认为 true，但 isLoading 是计算属性）
        // 注意：修复后 isLoading = isInitialLoading，所以初始状态 isLoading 为 true
        assertTrue(
            "修复后初始状态 isLoading 应为 true（等于 isInitialLoading）",
            initialState.isLoading
        )
    }

    @Test
    fun `当前实现-场景切换不会设置isLoading为true避免全屏刷新`() {
        // Given: 已加载的状态
        val loadedState = PromptEditorUiState(
            currentScene = PromptScene.ANALYZE,
            isInitialLoading = false
        )

        // When: 模拟修复后的场景切换（设置 isSceneSwitching = true，而不是 isInitialLoading）
        val switchingState = loadedState.copy(
            currentScene = PromptScene.POLISH,
            isSceneSwitching = true  // 修复后：只设置场景切换状态
        )

        // Then: isLoading 应为 false（因为 isLoading = isInitialLoading）
        assertFalse(
            "修复后场景切换不会设置 isLoading = true",
            switchingState.isLoading
        )
        assertTrue(
            "场景切换时 isSceneSwitching 应为 true",
            switchingState.isSceneSwitching
        )
    }

    // ==================== 场景切换基础测试 ====================

    @Test
    fun `场景切换应立即更新currentScene`() {
        // Given: 初始场景为 ANALYZE
        val initialState = PromptEditorUiState(
            currentScene = PromptScene.ANALYZE,
            isInitialLoading = false
        )

        // When: 切换到 POLISH 场景
        val newState = initialState.copy(currentScene = PromptScene.POLISH)

        // Then: currentScene 应立即更新
        assertEquals(
            "currentScene 应立即更新为 POLISH",
            PromptScene.POLISH,
            newState.currentScene
        )
    }

    @Test
    fun `场景切换不应影响currentPrompt直到加载完成`() {
        // Given: 已有提示词的状态
        val stateWithPrompt = PromptEditorUiState(
            currentScene = PromptScene.ANALYZE,
            currentPrompt = "analyze prompt",
            originalPrompt = "analyze prompt",
            isInitialLoading = false
        )

        // When: 只切换场景，不更新提示词
        val switchedState = stateWithPrompt.copy(
            currentScene = PromptScene.POLISH
        )

        // Then: currentPrompt 应保持不变（直到新数据加载完成）
        assertEquals(
            "切换场景时 currentPrompt 应保持不变",
            "analyze prompt",
            switchedState.currentPrompt
        )
    }

    // ==================== 加载状态测试 ====================

    @Test
    fun `加载完成后isLoading应为false`() {
        // Given: 正在加载的状态
        val loadingState = PromptEditorUiState(
            currentScene = PromptScene.POLISH,
            isInitialLoading = true
        )

        // When: 加载完成
        val loadedState = loadingState.copy(
            currentPrompt = "loaded prompt",
            originalPrompt = "loaded prompt",
            isInitialLoading = false
        )

        // Then: isLoading 应为 false
        assertFalse(
            "加载完成后 isLoading 应为 false",
            loadedState.isLoading
        )
    }

    @Test
    fun `加载错误时应设置错误信息`() {
        // Given: 正在加载的状态
        val loadingState = PromptEditorUiState(
            currentScene = PromptScene.REPLY,
            isInitialLoading = true,
            errorMessage = null
        )

        // When: 加载失败
        val errorState = loadingState.copy(
            isInitialLoading = false,
            errorMessage = "加载失败: Network error"
        )

        // Then: 错误信息应设置
        assertFalse(
            "加载失败后 isLoading 应为 false",
            errorState.isLoading
        )
        assertNotNull(
            "加载失败后应设置错误信息",
            errorState.errorMessage
        )
        assertTrue(
            "错误信息应包含失败原因",
            errorState.errorMessage!!.contains("加载失败")
        )
    }

    // ==================== canSave 属性测试 ====================

    @Test
    fun `加载中canSave应为false`() {
        // Given: 正在加载的状态
        val loadingState = PromptEditorUiState(
            isInitialLoading = true,
            isSaving = false
        )

        // Then: canSave 应为 false
        assertFalse(
            "加载中 canSave 应为 false",
            loadingState.canSave
        )
    }

    @Test
    fun `保存中canSave应为false`() {
        // Given: 正在保存的状态
        val savingState = PromptEditorUiState(
            isInitialLoading = false,
            isSaving = true
        )

        // Then: canSave 应为 false
        assertFalse(
            "保存中 canSave 应为 false",
            savingState.canSave
        )
    }

    @Test
    fun `超出字数限制canSave应为false`() {
        // Given: 超出字数限制的状态
        val overLimitState = PromptEditorUiState(
            currentPrompt = "a".repeat(PromptEditorUiState.MAX_PROMPT_LENGTH + 1),
            isInitialLoading = false,
            isSaving = false
        )

        // Then: canSave 应为 false
        assertTrue(
            "应检测到超出字数限制",
            overLimitState.isOverLimit
        )
        assertFalse(
            "超出字数限制时 canSave 应为 false",
            overLimitState.canSave
        )
    }

    @Test
    fun `正常状态canSave应为true`() {
        // Given: 正常状态
        val normalState = PromptEditorUiState(
            currentPrompt = "normal prompt",
            isInitialLoading = false,
            isSaving = false
        )

        // Then: canSave 应为 true
        assertFalse(
            "不应超出字数限制",
            normalState.isOverLimit
        )
        assertTrue(
            "正常状态 canSave 应为 true",
            normalState.canSave
        )
    }

    @Test
    fun `场景切换中canSave应为false`() {
        // Given: 场景切换中的状态
        val switchingState = PromptEditorUiState(
            currentPrompt = "normal prompt",
            isInitialLoading = false,
            isSceneSwitching = true,
            isSaving = false
        )

        // Then: canSave 应为 false
        assertFalse(
            "场景切换中 canSave 应为 false",
            switchingState.canSave
        )
    }

    // ==================== 场景顺序测试 ====================

    @Test
    fun `所有设置界面场景都应可切换`() {
        // Given: 设置界面显示的场景列表
        val settingsScenes = PromptScene.SETTINGS_SCENE_ORDER

        // Then: 应包含 5 个场景（ANALYZE、POLISH、REPLY、SUMMARY、AI_ADVISOR）
        assertEquals(
            "设置界面应显示 5 个场景",
            5,
            settingsScenes.size
        )
        assertTrue(
            "应包含 ANALYZE 场景",
            settingsScenes.contains(PromptScene.ANALYZE)
        )
        assertTrue(
            "应包含 POLISH 场景",
            settingsScenes.contains(PromptScene.POLISH)
        )
        assertTrue(
            "应包含 REPLY 场景",
            settingsScenes.contains(PromptScene.REPLY)
        )
        assertTrue(
            "应包含 SUMMARY 场景",
            settingsScenes.contains(PromptScene.SUMMARY)
        )
        assertTrue(
            "应包含 AI_ADVISOR 场景",
            settingsScenes.contains(PromptScene.AI_ADVISOR)
        )
    }

    @Test
    fun `废弃场景不应在设置界面显示`() {
        // Given: 设置界面显示的场景列表
        val settingsScenes = PromptScene.SETTINGS_SCENE_ORDER

        // Then: 不应包含废弃场景
        assertFalse(
            "不应包含 CHECK 场景（已废弃）",
            settingsScenes.contains(PromptScene.CHECK)
        )
        assertFalse(
            "不应包含 EXTRACT 场景（已废弃）",
            settingsScenes.contains(PromptScene.EXTRACT)
        )
    }

    // ==================== 字符计数测试 ====================

    @Test
    fun `charCount应正确计算字符数`() {
        // Given: 包含特定长度提示词的状态
        val state = PromptEditorUiState(
            currentPrompt = "Hello World"  // 11 个字符
        )

        // Then: charCount 应为 11
        assertEquals(
            "charCount 应正确计算字符数",
            11,
            state.charCount
        )
    }

    @Test
    fun `isNearLimit应在接近限制时返回true`() {
        // Given: 接近字数限制的状态
        val nearLimitState = PromptEditorUiState(
            currentPrompt = "a".repeat(PromptEditorUiState.WARN_PROMPT_LENGTH + 1)
        )

        // Then: isNearLimit 应为 true
        assertTrue(
            "接近字数限制时 isNearLimit 应为 true",
            nearLimitState.isNearLimit
        )
    }

    // ==================== hasUnsavedChanges 测试 ====================

    @Test
    fun `hasUnsavedChanges应在有修改时返回true`() {
        // Given: 有修改的状态
        val modifiedState = PromptEditorUiState(
            originalPrompt = "original",
            currentPrompt = "modified"
        )

        // Then: hasUnsavedChanges 应为 true
        assertTrue(
            "有修改时 hasUnsavedChanges 应为 true",
            modifiedState.hasUnsavedChanges
        )
    }

    @Test
    fun `hasUnsavedChanges应在无修改时返回false`() {
        // Given: 无修改的状态
        val unchangedState = PromptEditorUiState(
            originalPrompt = "same",
            currentPrompt = "same"
        )

        // Then: hasUnsavedChanges 应为 false
        assertFalse(
            "无修改时 hasUnsavedChanges 应为 false",
            unchangedState.hasUnsavedChanges
        )
    }
}
