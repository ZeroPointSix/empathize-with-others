package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.repository.PromptRepository
import com.empathy.ai.domain.util.PromptValidator
import com.empathy.ai.domain.util.PromptVariableResolver
import com.empathy.ai.presentation.ui.screen.prompt.PromptEditorUiEvent
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * BUG-00061 场景切换测试
 *
 * ## 测试背景
 * 问题：场景切换时触发全屏加载，导致 Tab 区域消失，用户体验差
 *
 * ## 修复方案
 * 1. 分离加载状态：isInitialLoading vs isSceneSwitching
 * 2. 添加提示词缓存：避免重复加载导致的全屏刷新
 * 3. UI 优化：场景切换时 Tab 区域保持可见
 *
 * ## 测试范围
 * - 场景切换时状态变化（isSceneSwitching）
 * - 缓存命中时直接使用缓存
 * - 缓存未命中时异步加载
 * - 兼容性属性 isLoading 的行为
 *
 * @see BUG-00061 会话历史跳转失败问题
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BUG00061SceneSwitchingTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var promptRepository: PromptRepository
    private lateinit var promptValidator: PromptValidator
    private lateinit var variableResolver: PromptVariableResolver

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        promptRepository = mockk(relaxed = true)
        variableResolver = mockk(relaxed = true)
        promptValidator = PromptValidator(variableResolver)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        mode: String = "global",
        scene: String? = PromptScene.ANALYZE.name
    ): PromptEditorViewModel {
        val savedStateHandle = SavedStateHandle().apply {
            set("mode", mode)
            scene?.let { set("scene", it) }
        }
        return PromptEditorViewModel(promptRepository, promptValidator, savedStateHandle)
    }

    // ========== BUG-00061 场景切换测试 ==========

    /**
     * 测试场景：初始加载状态
     *
     * 验证：isInitialLoading 初始为 true，加载完成后为 false
     */
    @Test
    fun `initial loading state isInitialLoading is true then false`() = runTest(testDispatcher) {
        // Given
        coEvery { promptRepository.getGlobalPrompt(PromptScene.ANALYZE) } returns Result.success("analyze prompt")

        // When
        val viewModel = createViewModel()

        // Then - 初始状态
        assertTrue(viewModel.uiState.value.isInitialLoading)
        assertTrue(viewModel.uiState.value.isLoading) // 兼容性属性

        // When - 加载完成后
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isInitialLoading)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    /**
     * 测试场景：场景切换 - 缓存命中
     *
     * BUG-00061 修复验证：
     * 1. 首次加载 ANALYZE 场景并缓存
     * 2. 切换到 POLISH 场景（缓存未命中，触发加载）
     * 3. 切回 ANALYZE 场景（缓存命中，直接使用）
     *
     * 验证：缓存命中时 isSceneSwitching 不会变为 true
     */
    @Test
    fun `scene switch with cache hit does not trigger loading`() = runTest(testDispatcher) {
        // Given - 首次加载 ANALYZE 场景
        coEvery { promptRepository.getGlobalPrompt(PromptScene.ANALYZE) } returns Result.success("analyze prompt")
        coEvery { promptRepository.getGlobalPrompt(PromptScene.POLISH) } returns Result.success("polish prompt")

        val viewModel = createViewModel(scene = PromptScene.ANALYZE.name)
        advanceUntilIdle()

        // Verify initial load completed
        assertEquals(PromptScene.ANALYZE, viewModel.uiState.value.currentScene)
        assertFalse(viewModel.uiState.value.isSceneSwitching)
        assertEquals("analyze prompt", viewModel.uiState.value.currentPrompt)

        // When - 切换到 POLISH 场景（缓存未命中）
        viewModel.onEvent(PromptEditorUiEvent.SwitchScene(PromptScene.POLISH))

        // Then - 场景立即更新，isSceneSwitching 变为 true
        assertEquals(PromptScene.POLISH, viewModel.uiState.value.currentScene)
        assertTrue(viewModel.uiState.value.isSceneSwitching)

        // When - 加载完成
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSceneSwitching)
        assertEquals("polish prompt", viewModel.uiState.value.currentPrompt)

        // When - 切回 ANALYZE 场景（缓存命中）
        viewModel.onEvent(PromptEditorUiEvent.SwitchScene(PromptScene.ANALYZE))

        // Then - 场景立即更新，isSceneSwitching 不变（缓存命中）
        assertEquals(PromptScene.ANALYZE, viewModel.uiState.value.currentScene)
        assertFalse(viewModel.uiState.value.isSceneSwitching) // 关键验证：缓存命中不触发加载
        assertEquals("analyze prompt", viewModel.uiState.value.currentPrompt)
    }

    /**
     * 测试场景：场景切换 - 缓存未命中
     *
     * 验证：缓存未命中时 isSceneSwitching 变为 true，加载完成后变为 false
     */
    @Test
    fun `scene switch with cache miss triggers isSceneSwitching`() = runTest(testDispatcher) {
        // Given
        coEvery { promptRepository.getGlobalPrompt(PromptScene.ANALYZE) } returns Result.success("analyze prompt")
        coEvery { promptRepository.getGlobalPrompt(PromptScene.CHECK) } returns Result.success("check prompt")

        val viewModel = createViewModel(scene = PromptScene.ANALYZE.name)
        advanceUntilIdle()

        // When - 切换到 CHECK 场景（缓存未命中）
        viewModel.onEvent(PromptEditorUiEvent.SwitchScene(PromptScene.CHECK))

        // Then - 场景立即更新
        assertEquals(PromptScene.CHECK, viewModel.uiState.value.currentScene)
        assertTrue(viewModel.uiState.value.isSceneSwitching)

        // When - 加载完成
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSceneSwitching)
        assertEquals("check prompt", viewModel.uiState.value.currentPrompt)
    }

    /**
     * 测试场景：场景切换 - 加载失败
     *
     * 验证：加载失败时 isSceneSwitching 变为 false，错误信息显示
     */
    @Test
    fun `scene switch failure clears isSceneSwitching and shows error`() = runTest(testDispatcher) {
        // Given
        coEvery { promptRepository.getGlobalPrompt(PromptScene.ANALYZE) } returns Result.success("analyze prompt")
        coEvery { promptRepository.getGlobalPrompt(PromptScene.EXTRACT) } returns Result.failure(Exception("Network error"))

        val viewModel = createViewModel(scene = PromptScene.ANALYZE.name)
        advanceUntilIdle()

        // When - 切换到 EXTRACT 场景（加载失败）
        viewModel.onEvent(PromptEditorUiEvent.SwitchScene(PromptScene.EXTRACT))

        // Then - 场景更新，isSceneSwitching 变为 true
        assertEquals(PromptScene.EXTRACT, viewModel.uiState.value.currentScene)
        assertTrue(viewModel.uiState.value.isSceneSwitching)

        // When - 加载失败
        advanceUntilIdle()

        // Then - isSceneSwitching 变为 false，错误信息显示
        assertFalse(viewModel.uiState.value.isSceneSwitching)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("加载失败") == true)
    }

    /**
     * 测试场景：兼容性属性 isLoading
     *
     * 验证：isLoading 返回 isInitialLoading 的值
     * 注意：isSceneSwitching 不会影响 isLoading（用于全屏加载）
     */
    @Test
    fun `isLoading compatibility property returns isInitialLoading`() = runTest(testDispatcher) {
        // Given
        coEvery { promptRepository.getGlobalPrompt(PromptScene.ANALYZE) } returns Result.success("prompt")

        val viewModel = createViewModel()

        // Then - 初始加载时
        assertTrue(viewModel.uiState.value.isInitialLoading)
        assertTrue(viewModel.uiState.value.isLoading)

        // When - 加载完成
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isInitialLoading)
        assertFalse(viewModel.uiState.value.isLoading)

        // When - 场景切换（isSceneSwitching 不影响 isLoading）
        coEvery { promptRepository.getGlobalPrompt(PromptScene.POLISH) } returns Result.success("polish")
        viewModel.onEvent(PromptEditorUiEvent.SwitchScene(PromptScene.POLISH))

        // Then - isSceneSwitching 为 true，但 isLoading 仍为 false
        assertTrue(viewModel.uiState.value.isSceneSwitching)
        assertFalse(viewModel.uiState.value.isLoading) // 关键：isLoading 不受 isSceneSwitching 影响
    }

    /**
     * 测试场景：场景切换时 canSave 状态
     *
     * 验证：isSceneSwitching 时 canSave 为 false
     */
    @Test
    fun `canSave is false during scene switching`() = runTest(testDispatcher) {
        // Given
        coEvery { promptRepository.getGlobalPrompt(PromptScene.ANALYZE) } returns Result.success("analyze")
        coEvery { promptRepository.getGlobalPrompt(PromptScene.SUMMARY) } returns Result.success("summary")

        val viewModel = createViewModel(scene = PromptScene.ANALYZE.name)
        advanceUntilIdle()

        // Then - 初始状态可以保存
        assertTrue(viewModel.uiState.value.canSave)

        // When - 场景切换中
        viewModel.onEvent(PromptEditorUiEvent.SwitchScene(PromptScene.SUMMARY))
        assertTrue(viewModel.uiState.value.isSceneSwitching)
        assertFalse(viewModel.uiState.value.canSave) // 关键：场景切换时不能保存

        // When - 加载完成
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSceneSwitching)
        assertTrue(viewModel.uiState.value.canSave)
    }

    /**
     * 测试场景：多场景缓存
     *
     * 验证：多个场景的提示词都能正确缓存和复用
     */
    @Test
    fun `multiple scenes are cached correctly`() = runTest(testDispatcher) {
        // Given
        val prompts = mapOf(
            PromptScene.ANALYZE to "analyze prompt",
            PromptScene.CHECK to "check prompt",
            PromptScene.EXTRACT to "extract prompt",
            PromptScene.SUMMARY to "summary prompt",
            PromptScene.POLISH to "polish prompt",
            PromptScene.REPLY to "reply prompt",
            PromptScene.AI_ADVISOR to "advisor prompt"
        )
        prompts.forEach { (scene, prompt) ->
            coEvery { promptRepository.getGlobalPrompt(scene) } returns Result.success(prompt)
        }

        val viewModel = createViewModel(scene = PromptScene.ANALYZE.name)
        advanceUntilIdle()

        // 遍历所有场景并验证缓存
        PromptScene.entries.forEach { scene ->
            // 切换到场景
            viewModel.onEvent(PromptEditorUiEvent.SwitchScene(scene))

            // 如果是第一次访问，需要等待加载
            advanceUntilIdle()

            // 验证场景和提示词
            assertEquals(scene, viewModel.uiState.value.currentScene)
            assertEquals(prompts[scene], viewModel.uiState.value.currentPrompt)
            assertFalse(viewModel.uiState.value.isSceneSwitching)
        }

        // 再次切换到 ANALYZE，验证缓存命中
        viewModel.onEvent(PromptEditorUiEvent.SwitchScene(PromptScene.ANALYZE))
        assertEquals(PromptScene.ANALYZE, viewModel.uiState.value.currentScene)
        assertFalse(viewModel.uiState.value.isSceneSwitching) // 缓存命中，无加载
        assertEquals(prompts[PromptScene.ANALYZE], viewModel.uiState.value.currentPrompt)
    }
}
