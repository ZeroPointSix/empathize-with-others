package com.empathy.ai.domain.model

/**
 * 悬浮窗操作类型
 * 
 * 定义用户可以通过悬浮窗触发的操作
 */
enum class ActionType {
    /**
     * 帮我分析
     * 调用 AnalyzeChatUseCase 进行聊天分析
     */
    ANALYZE,
    
    /**
     * 帮我检查
     * 调用 CheckDraftUseCase 进行安全检查
     */
    CHECK
}
