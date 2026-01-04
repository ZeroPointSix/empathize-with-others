package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.usecase.GetAdvisorSessionsUseCase
import com.empathy.ai.domain.usecase.GetAllContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI军师主界面ViewModel
 *
 * 管理AI军师主界面的状态，包括联系人列表和最近会话。
 */
@HiltViewModel
class AiAdvisorViewModel @Inject constructor(
    private val getAllContactsUseCase: GetAllContactsUseCase,
    private val getAdvisorSessionsUseCase: GetAdvisorSessionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAdvisorUiState())
    val uiState: StateFlow<AiAdvisorUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    /**
     * 加载联系人列表
     */
    fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                getAllContactsUseCase().collect { contacts ->
                    _uiState.update { it.copy(contacts = contacts, isLoading = false) }
                    // Load recent sessions for each contact
                    loadRecentSessions(contacts)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "加载联系人失败")
                }
            }
        }
    }

    /**
     * 加载最近会话
     */
    private suspend fun loadRecentSessions(contacts: List<ContactProfile>) {
        val sessionsMap = mutableMapOf<String, AiAdvisorSession?>()
        contacts.forEach { contact ->
            getAdvisorSessionsUseCase(contact.id).onSuccess { sessions ->
                sessionsMap[contact.id] = sessions.firstOrNull()
            }
        }
        _uiState.update { it.copy(recentSessions = sessionsMap) }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadContacts()
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * AI军师主界面UI状态
 */
data class AiAdvisorUiState(
    val isLoading: Boolean = false,
    val contacts: List<ContactProfile> = emptyList(),
    val recentSessions: Map<String, AiAdvisorSession?> = emptyMap(),
    val error: String? = null
)
