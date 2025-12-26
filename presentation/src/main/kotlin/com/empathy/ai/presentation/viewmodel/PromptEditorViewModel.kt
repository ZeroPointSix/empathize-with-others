package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.repository.PromptRepository
import com.empathy.ai.domain.util.PromptValidator
import com.empathy.ai.presentation.ui.screen.prompt.PromptEditMode
import com.empathy.ai.presentation.ui.screen.prompt.PromptEditorResult
import com.empathy.ai.presentation.ui.screen.prompt.PromptEditorUiEvent
import com.empathy.ai.presentation.ui.screen.prompt.PromptEditorUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

/**
 * 提示词编辑器ViewModel
 *
 * 负责管理提示词编辑界面的状态和业务逻辑
 */
@HiltViewModel
class PromptEditorViewModel @Inject constructor(
    private val promptRepository: PromptRepository,
    private val promptValidator: PromptValidator,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editMode: PromptEditMode = parseEditMode(savedStateHandle)
    
    private val initialScene: PromptScene = when (editMode) {
        is PromptEditMode.GlobalScene -> editMode.scene
        is PromptEditMode.ContactCustom -> PromptScene.ANALYZE
    }

    private val _uiState = MutableStateFlow(
        PromptEditorUiState(
            editMode = editMode,
            currentScene = initialScene,
            placeholderText = getPlaceholderText(editMode)
        )
    )
    val uiState: StateFlow<PromptEditorUiState> = _uiState.asStateFlow()

    private val _result = MutableSharedFlow<PromptEditorResult>()
    val result: SharedFlow<PromptEditorResult> = _result.asSharedFlow()

    // 防抖处理：使用Channel在ViewModel层处理
    private val promptInputChannel = Channel<String>(Channel.CONFLATED)

    init {
        loadPrompt()
        setupInputDebounce()
    }

    /**
     * 设置输入防抖处理
     *
     * 设计说明：
     * - UI层直接发送UpdatePrompt事件，保持UI逻辑纯粹
     * - ViewModel层使用Channel+debounce处理防抖
     * - 便于单元测试，可独立测试防抖逻辑
     */
    @Suppress("OPT_IN_USAGE")
    private fun setupInputDebounce() {
        viewModelScope.launch {
            promptInputChannel.receiveAsFlow()
                .debounce(DEBOUNCE_DELAY_MS)
                .collect { text ->
                    // 防抖后可执行验证等操作（当前仅用于防抖）
                    // 实际状态更新在handlePromptInput中立即执行
                }
        }
    }

    /**
     * 处理UI事件
     */
    fun onEvent(event: PromptEditorUiEvent) {
        when (event) {
            is PromptEditorUiEvent.UpdatePrompt -> handlePromptInput(event.text)
            is PromptEditorUiEvent.SwitchScene -> handleSwitchScene(event.scene)
            is PromptEditorUiEvent.SavePrompt -> savePrompt()
            is PromptEditorUiEvent.CancelEdit -> handleCancel()
            is PromptEditorUiEvent.ConfirmDiscard -> confirmDiscard()
            is PromptEditorUiEvent.DismissDiscardDialog -> dismissDiscardDialog()
            is PromptEditorUiEvent.ClearError -> clearError()
            is PromptEditorUiEvent.ResetToDefault -> resetToDefault()
        }
    }

    /**
     * 处理场景切换
     */
    private fun handleSwitchScene(scene: PromptScene) {
        // 更新当前场景
        _uiState.update { it.copy(currentScene = scene) }
        
        // 重新加载该场景的提示词
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val promptResult = promptRepository.getGlobalPrompt(scene)
                
                promptResult.fold(
                    onSuccess = { prompt ->
                        val actualPrompt = prompt ?: ""
                        _uiState.update {
                            it.copy(
                                editMode = PromptEditMode.GlobalScene(scene),
                                originalPrompt = actualPrompt,
                                currentPrompt = actualPrompt,
                                placeholderText = getPlaceholderText(PromptEditMode.GlobalScene(scene)),
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "加载失败: ${e.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "加载失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 恢复默认提示词
     */
    private fun resetToDefault() {
        _uiState.update {
            it.copy(
                currentPrompt = "",
                errorMessage = null
            )
        }
    }

    /**
     * 处理提示词输入
     *
     * 安全措施：
     * 1. 先截断超大文本（防止粘贴10万字卡死主线程）
     * 2. 立即更新UI显示（保证输入响应）
     * 3. 通过Channel发送防抖处理
     */
    private fun handlePromptInput(text: String) {
        // 大文本保护：先截断再处理，防止粘贴超大文本卡死
        val safeText = text.take(PromptEditorUiState.MAX_PROMPT_LENGTH)

        // 立即更新UI（保证输入响应性）
        _uiState.update { it.copy(currentPrompt = safeText) }

        // 发送到Channel进行防抖处理（用于后续验证等操作）
        viewModelScope.launch {
            promptInputChannel.send(safeText)
        }
    }

    /**
     * 加载提示词
     */
    private fun loadPrompt() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val promptResult = when (val mode = editMode) {
                    is PromptEditMode.GlobalScene ->
                        promptRepository.getGlobalPrompt(mode.scene)
                    is PromptEditMode.ContactCustom ->
                        promptRepository.getContactPrompt(mode.contactId)
                }

                promptResult.fold(
                    onSuccess = { prompt ->
                        val actualPrompt = prompt ?: ""
                        _uiState.update {
                            it.copy(
                                originalPrompt = actualPrompt,
                                currentPrompt = actualPrompt,
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "加载失败: ${e.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "加载失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 保存提示词
     */
    private fun savePrompt() {
        val currentState = _uiState.value
        if (!currentState.canSave) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                val result = when (val mode = editMode) {
                    is PromptEditMode.GlobalScene ->
                        promptRepository.saveGlobalPrompt(mode.scene, currentState.currentPrompt)
                    is PromptEditMode.ContactCustom ->
                        promptRepository.saveContactPrompt(
                            mode.contactId,
                            currentState.currentPrompt.ifBlank { null }
                        )
                }

                result.fold(
                    onSuccess = {
                        _result.emit(PromptEditorResult.Saved)
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = "保存失败: ${e.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "保存失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 处理取消操作
     */
    private fun handleCancel() {
        if (_uiState.value.hasUnsavedChanges) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            viewModelScope.launch {
                _result.emit(PromptEditorResult.Cancelled)
            }
        }
    }

    /**
     * 确认放弃修改
     */
    private fun confirmDiscard() {
        viewModelScope.launch {
            _uiState.update { it.copy(showDiscardDialog = false) }
            _result.emit(PromptEditorResult.Cancelled)
        }
    }

    /**
     * 关闭放弃修改对话框
     */
    private fun dismissDiscardDialog() {
        _uiState.update { it.copy(showDiscardDialog = false) }
    }

    /**
     * 清除错误信息
     */
    private fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        /** 防抖延迟时间（毫秒） */
        private const val DEBOUNCE_DELAY_MS = 100L

        /**
         * 获取占位符文本
         *
         * 设计说明：
         * - 占位符文本应该引导用户定义"AI应该怎么做"
         * - 不需要提及数据变量，因为数据由系统自动注入
         * - 使用友好的自然语言示例
         */
        fun getPlaceholderText(editMode: PromptEditMode): String {
            return when (editMode) {
                is PromptEditMode.ContactCustom ->
                    "可选：针对这位联系人的特殊要求\n\n例如：\n• 和她聊天时语气要温柔一些\n• 多关注她的情绪变化\n• 避开前任相关话题"
                is PromptEditMode.GlobalScene -> when (editMode.scene) {
                    PromptScene.ANALYZE -> "可选：自定义分析风格\n\n例如：\n• 分析时要特别关注对方的情绪变化\n• 给出的回复建议要温和友好\n• 如果对方在生气，先安抚情绪"
                    PromptScene.CHECK -> "可选：自定义检查重点\n\n例如：\n• 检查时要特别注意工作相关话题\n• 避免使用太直接的表达\n• 注意语气是否过于强硬"
                    PromptScene.EXTRACT -> "可选：自定义提取重点\n\n例如：\n• 重点关注对方提到的兴趣爱好\n• 注意记录重要的日期和事件\n• 留意对方的情绪触发点"
                    PromptScene.SUMMARY -> "可选：自定义总结风格\n\n例如：\n• 总结今天的对话亮点\n• 指出需要注意的地方\n• 给出明天跟进的建议"
                    PromptScene.POLISH -> "可选：自定义润色风格\n\n例如：\n• 润色时保持原意不变\n• 让表达更加得体礼貌\n• 避免使用过于生硬的词汇"
                    PromptScene.REPLY -> "可选：自定义回复风格\n\n例如：\n• 回复要简洁有力\n• 语气要友好亲切\n• 适当使用表情符号"
                }
            }
        }

        /**
         * 解析编辑模式
         */
        private fun parseEditMode(savedStateHandle: SavedStateHandle): PromptEditMode {
            val mode = savedStateHandle.get<String>("mode") ?: "global"

            return when (mode) {
                "global" -> {
                    val sceneName = savedStateHandle.get<String>("scene")
                        ?: PromptScene.ANALYZE.name
                    val scene = try {
                        PromptScene.valueOf(sceneName)
                    } catch (e: IllegalArgumentException) {
                        PromptScene.ANALYZE
                    }
                    PromptEditMode.GlobalScene(scene)
                }
                "contact" -> {
                    val contactId = savedStateHandle.get<String>("contactId")
                        ?: throw IllegalArgumentException("contactId is required")
                    val contactName = savedStateHandle.get<String>("contactName")
                        ?.let {
                            try {
                                URLDecoder.decode(it, "UTF-8")
                            } catch (e: Exception) {
                                it
                            }
                        }
                        ?: "联系人"
                    PromptEditMode.ContactCustom(contactId, contactName)
                }
                else -> PromptEditMode.GlobalScene(PromptScene.ANALYZE)
            }
        }
    }
}
