package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.SystemPromptScene
import com.empathy.ai.domain.repository.SystemPromptRepository
import com.empathy.ai.domain.util.SystemPrompts
import com.empathy.ai.presentation.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 系统提示词编辑ViewModel
 *
 * PRD-00033: 开发者模式 - 系统提示词编辑
 * 管理单个场景的系统提示词编辑状态和业务逻辑
 */
@HiltViewModel
class SystemPromptEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val systemPromptRepository: SystemPromptRepository
) : ViewModel() {

    private val sceneName: String = savedStateHandle[NavRoutes.SYSTEM_PROMPT_EDIT_ARG_SCENE] ?: ""
    private val scene: SystemPromptScene? = SystemPromptScene.entries.find { it.name == sceneName }

    private val _uiState = MutableStateFlow(SystemPromptEditUiState())
    val uiState: StateFlow<SystemPromptEditUiState> = _uiState.asStateFlow()

    init {
        loadConfig()
    }

    /**
     * 加载场景配置
     */
    private fun loadConfig() {
        val currentScene = scene ?: run {
            _uiState.update { it.copy(error = "无效的场景: $sceneName") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val customHeader = systemPromptRepository.getHeader(currentScene)
                val defaultHeader = SystemPrompts.getHeaderForScene(currentScene)
                val defaultFooter = SystemPrompts.getFooterForScene(currentScene)
                val hasCustom = !customHeader.isNullOrEmpty()
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        scene = currentScene,
                        sceneName = currentScene.displayName,
                        sceneDescription = currentScene.description,
                        header = customHeader ?: "",
                        footer = "", // Footer不可编辑
                        defaultHeader = defaultHeader,
                        defaultFooter = defaultFooter,
                        hasCustomConfig = hasCustom
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
     * 更新Header内容
     */
    fun updateHeader(header: String) {
        _uiState.update { 
            it.copy(
                header = header,
                hasUnsavedChanges = true
            )
        }
    }

    /**
     * 保存配置（仅Header）
     */
    fun save() {
        val currentScene = scene ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val header = _uiState.value.header.ifEmpty { null }
                // Footer不允许自定义，始终传null
                systemPromptRepository.updateScene(currentScene, header, null)
                val hasCustom = !header.isNullOrEmpty()
                _uiState.update { 
                    it.copy(
                        isSaving = false,
                        hasUnsavedChanges = false,
                        hasCustomConfig = hasCustom,
                        successMessage = "保存成功"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "保存失败"
                    )
                }
            }
        }
    }

    /**
     * 重置为默认值
     */
    fun resetToDefault() {
        val currentScene = scene ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                systemPromptRepository.resetScene(currentScene)
                _uiState.update { 
                    it.copy(
                        isSaving = false,
                        header = "",
                        footer = "",
                        hasUnsavedChanges = false,
                        hasCustomConfig = false,
                        successMessage = "已重置为默认值"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "重置失败"
                    )
                }
            }
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 清除成功消息
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

/**
 * 系统提示词编辑UI状态
 */
data class SystemPromptEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val scene: SystemPromptScene? = null,
    val sceneName: String = "",
    val sceneDescription: String = "",
    val header: String = "",
    val footer: String = "",
    val defaultHeader: String = "",
    val defaultFooter: String = "",
    val hasCustomConfig: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
