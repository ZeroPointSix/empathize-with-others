package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.presentation.util.DebugLogger

/**
 * 调试日志辅助类，用于批量删除标签问题调查
 */
object BatchDeleteDebugLogger {
    
    /**
     * 记录批量删除操作开始
     */
    fun logBatchDeleteStart(contactId: String, factIds: List<String>) {
        DebugLogger.d(
            "BatchDeleteDebug",
            "========== 批量删除操作开始 =========="
        )
        DebugLogger.d(
            "BatchDeleteDebug",
            "联系人ID: $contactId"
        )
        DebugLogger.d(
            "BatchDeleteDebug",
            "要删除的Fact数量: ${factIds.size}"
        )
        factIds.forEachIndexed { index, factId ->
            DebugLogger.d(
                "BatchDeleteDebug",
                "  [$index] factId: $factId"
            )
        }
    }
    
    /**
     * 记录批量删除操作成功
     */
    fun logBatchDeleteSuccess(deletedCount: Int) {
        DebugLogger.d(
            "BatchDeleteDebug",
            "批量删除成功，删除数量: $deletedCount"
        )
    }
    
    /**
     * 记录批量删除操作失败
     */
    fun logBatchDeleteFailure(error: String) {
        DebugLogger.e(
            "BatchDeleteDebug",
            "批量删除失败: $error"
        )
    }
    
    /**
     * 记录loadContactDetail调用
     */
    fun logLoadContactDetail(contactId: String) {
        DebugLogger.d(
            "BatchDeleteDebug",
            "调用loadContactDetail刷新数据，联系人ID: $contactId"
        )
    }
    
    /**
     * 记录数据库外键级联删除警告
     */
    fun logCascadeDeleteWarning() {
        DebugLogger.w(
            "BatchDeleteDebug",
            "警告：检测到可能的级联删除风险！ConversationLogEntity和DailySummaryEntity都有CASCADE外键约束"
        )
        DebugLogger.w(
            "BatchDeleteDebug",
            "如果ContactProfileEntity被意外删除，所有关联的对话记录和总结将被级联删除"
        )
    }
    
    /**
     * 记录updateFacts调用
     */
    fun logUpdateFacts(contactId: String, factsCount: Int) {
        DebugLogger.d(
            "BatchDeleteDebug",
            "调用updateFacts，联系人ID: $contactId, Facts数量: $factsCount"
        )
    }
    
    /**
     * 记录deleteProfile调用（危险操作）
     */
    fun logDeleteProfileCall(contactId: String, stackTrace: String) {
        DebugLogger.e(
            "BatchDeleteDebug",
            "========== 危险操作检测 =========="
        )
        DebugLogger.e(
            "BatchDeleteDebug",
            "检测到deleteProfile调用！这将级联删除所有对话记录和总结！"
        )
        DebugLogger.e(
            "BatchDeleteDebug",
            "联系人ID: $contactId"
        )
        DebugLogger.e(
            "BatchDeleteDebug",
            "调用栈: $stackTrace"
        )
    }
}