package com.empathy.ai.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.usecase.SaveProfileUseCase
import com.empathy.ai.presentation.ui.component.contact.ContactFormData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 新建联系人ViewModel
 * 
 * 职责：
 * 1. 处理新建联系人的业务逻辑
 * 2. 将ContactFormData转换为ContactProfile
 * 3. 调用SaveProfileUseCase保存数据
 * 
 * @see RESEARCH-00054 新建联系人页面BUG深度调研报告
 */
@HiltViewModel
class CreateContactViewModel @Inject constructor(
    private val saveProfileUseCase: SaveProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateContactUiState())
    val uiState: StateFlow<CreateContactUiState> = _uiState.asStateFlow()

    /**
     * 保存联系人
     * 
     * @param formData 表单数据
     * @param avatarUri 头像URI（可选）
     * @param facts 事实列表
     */
    fun saveContact(
        formData: ContactFormData,
        avatarUri: Uri?,
        facts: List<Fact>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            try {
                // 将ContactFormData转换为ContactProfile
                val profile = ContactProfile(
                    id = UUID.randomUUID().toString(),
                    name = formData.name,
                    targetGoal = formData.targetGoal.ifBlank { "待设定" },
                    contextDepth = 10,
                    facts = facts,
                    relationshipScore = formData.initialRelationshipScore,
                    avatarUrl = avatarUri?.toString()
                )

                // 调用UseCase保存
                val result = saveProfileUseCase(profile)

                result.onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "保存失败"
                        )
                    }
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
     * 清除错误状态
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 重置保存成功状态
     */
    fun resetSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}

/**
 * 新建联系人UI状态
 */
data class CreateContactUiState(
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)
