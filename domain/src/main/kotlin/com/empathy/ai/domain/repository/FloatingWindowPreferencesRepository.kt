package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.FloatingBubbleState
import com.empathy.ai.domain.model.FloatingWindowState
import com.empathy.ai.domain.model.FloatingWindowUiState
import com.empathy.ai.domain.model.MinimizedRequestInfo

interface FloatingWindowPreferencesRepository {
    fun saveState(state: FloatingWindowState)
    fun loadState(): FloatingWindowState
    fun saveEnabled(isEnabled: Boolean)
    fun isEnabled(): Boolean
    fun clear()
    fun saveButtonPosition(x: Int, y: Int)
    fun getButtonX(): Int
    fun getButtonY(): Int
    fun getButtonPosition(): Pair<Int, Int>
    fun saveIndicatorPosition(x: Int, y: Int)
    fun getIndicatorPosition(): Pair<Int, Int>
    fun saveRequestInfo(requestInfo: MinimizedRequestInfo): Boolean
    fun getRequestInfo(): MinimizedRequestInfo?
    fun clearRequestInfo()
    fun saveSelectedTab(tabName: String)
    fun saveSelectedTab(tab: ActionType)
    fun getSelectedTab(): String
    fun getSelectedTabAsActionType(): ActionType
    fun saveLastContactId(contactId: String)
    fun getLastContactId(): String?
    fun saveInputText(text: String)
    fun getInputText(): String
    fun saveUiState(tabName: String, contactId: String?, inputText: String)
    fun saveUiState(state: FloatingWindowUiState)
    fun restoreUiState(): Triple<String, String?, String>
    fun restoreUiStateAsObject(): FloatingWindowUiState?
    fun clearSavedUiState()
    fun hasSavedUiState(): Boolean
    fun saveBubblePosition(x: Int, y: Int)
    fun getBubblePosition(defaultX: Int, defaultY: Int): Pair<Int, Int>
    fun saveBubbleState(state: FloatingBubbleState)
    fun getBubbleState(): FloatingBubbleState
    fun saveMinimizeState(requestInfo: String)
    fun getMinimizeStateIfValid(): String?
    fun clearMinimizeState()
    fun hasValidMinimizeState(): Boolean
    fun saveDisplayMode(mode: String)
    fun getDisplayMode(): String
    fun shouldStartAsBubble(): Boolean
    companion object {
        const val DISPLAY_MODE_BUBBLE = "BUBBLE"
        const val DISPLAY_MODE_DIALOG = "DIALOG"
    }
}
