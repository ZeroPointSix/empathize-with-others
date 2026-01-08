package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.SystemPromptScene
import com.empathy.ai.domain.repository.SystemPromptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 系统提示词列表ViewModel
 *
 * PRD-00033: 开发者模式 - 系统提示词编辑
 * 管理系统提示词场景列表的状态和业务逻辑
 */
@HiltViewModel
class SystemPromptListViewModel @Inject constructor(
    private val systemPromptRepository: SystemPromptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SystemPromptListUiState())
    val uiState: StateFlow<SystemPromptListUiState> = _uiState.asStateFlow()

    init {
        loadScenes()
    }

    /**
     * 加载所有场景及其自定义状态
     */
    private fun loadScenes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val customizedScenes = systemPromptRepository.getCustomizedScenes()
                val scenes = SystemPromptScene.entries.map { scene ->
                    SceneItem(
                        scene = scene,
                        displayName = scene.displayName,
                        description = scene.description,
                        hasCustomConfig = scene in customizedScenes
                    )
                }
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        scenes = scenes
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
            }
        }
    }

    /**
     * 刷新场景列表
     */
    fun refresh() {
        loadScenes()
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * 系统提示词列表UI状态
 */
data class SystemPromptListUiState(
    val isLoading: Boolean = false,
    val scenes: List<SceneItem> = emptyList(),
    val error: String? = null
)

/**
 * 场景列表项
 */
data class SceneItem(
    val scene: SystemPromptScene,
    val displayName: String,
    val description: String,
    val hasCustomConfig: Boolean
)
