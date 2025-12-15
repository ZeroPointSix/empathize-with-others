package com.empathy.ai.domain.util

import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.domain.repository.FailedTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 隐私数据处理器
 *
 * 负责敏感数据的处理、清理和脱敏
 *
 * 职责：
 * - 自动清理过期数据
 * - 敏感数据脱敏
 * - 数据导出前处理
 * - 用户数据删除
 *
 * 设计原则：
 * - 隐私优先：默认启用所有保护措施
 * - 最小化数据：只保留必要的数据
 * - 用户控制：用户可以随时删除数据
 */
@Singleton
class PrivacyDataHandler @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val failedTaskRepository: FailedTaskRepository,
    private val dataEncryption: DataEncryption
) {
    
    // ========== 数据脱敏 ==========
    
    /**
     * 对文本进行脱敏处理
     *
     * @param text 原始文本
     * @param types 要脱敏的敏感数据类型，默认全部
     * @return 脱敏后的文本
     */
    fun maskSensitiveData(
        text: String,
        types: Set<PrivacyConfig.SensitiveDataType> = PrivacyConfig.SensitiveDataType.entries.toSet()
    ): String {
        return PrivacyConfig.maskText(text, types)
    }
    
    /**
     * 检测文本中的敏感数据
     *
     * @param text 要检测的文本
     * @return 检测到的敏感数据类型集合
     */
    fun detectSensitiveData(text: String): Set<PrivacyConfig.SensitiveDataType> {
        return PrivacyConfig.detectSensitiveTypes(text)
    }
    
    /**
     * 部分脱敏（保留前后几位）
     *
     * @param text 原始文本
     * @param keepLength 前后保留的字符数
     * @return 部分脱敏后的文本
     */
    fun partialMask(text: String, keepLength: Int = PrivacyConfig.PARTIAL_MASK_KEEP_LENGTH): String {
        return PrivacyConfig.partialMask(text, keepLength)
    }
    
    // ========== 数据加密 ==========
    
    /**
     * 加密敏感数据
     */
    fun encryptData(data: String): Result<String> {
        return dataEncryption.encrypt(data)
    }
    
    /**
     * 解密数据
     */
    fun decryptData(encryptedData: String): Result<String> {
        return dataEncryption.decrypt(encryptedData)
    }
    
    // ========== 数据清理 ==========
    
    /**
     * 清理过期数据
     *
     * 根据配置的保留策略清理过期的对话记录、总结和失败任务
     *
     * @return 清理结果，包含各类数据的清理数量
     */
    suspend fun cleanupExpiredData(): CleanupResult = withContext(Dispatchers.IO) {
        val result = CleanupResult()
        
        try {
            // 1. 清理过期对话记录
            if (PrivacyConfig.CONVERSATION_RETENTION_DAYS > 0) {
                val conversationCutoff = calculateCutoffTime(PrivacyConfig.CONVERSATION_RETENTION_DAYS)
                conversationRepository.cleanupOldSummarizedLogs(conversationCutoff)
                    .onSuccess { result.conversationsDeleted = it }
            }
            
            // 2. 清理过期每日总结
            if (PrivacyConfig.SUMMARY_RETENTION_DAYS > 0) {
                val summaryCutoff = calculateCutoffTime(PrivacyConfig.SUMMARY_RETENTION_DAYS)
                dailySummaryRepository.cleanupOldSummaries(summaryCutoff)
                    .onSuccess { result.summariesDeleted = it }
            }
            
            // 3. 清理过期失败任务
            if (PrivacyConfig.FAILED_TASK_RETENTION_DAYS > 0) {
                val taskCutoff = calculateCutoffTime(PrivacyConfig.FAILED_TASK_RETENTION_DAYS)
                failedTaskRepository.cleanupOldTasks(taskCutoff)
                    .onSuccess { result.failedTasksDeleted = it }
            }
            
            result.success = true
        } catch (e: Exception) {
            result.success = false
            result.error = e.message
        }
        
        result
    }
    
    /**
     * 清理指定联系人的所有数据
     *
     * @param contactId 联系人ID
     * @return 清理结果
     */
    suspend fun cleanupContactData(contactId: String): CleanupResult = withContext(Dispatchers.IO) {
        val result = CleanupResult()
        
        try {
            // 删除对话记录
            conversationRepository.deleteByContactId(contactId)
                .onSuccess { result.conversationsDeleted = it }
            
            // 删除每日总结
            dailySummaryRepository.deleteByContactId(contactId)
                .onSuccess { result.summariesDeleted = it }
            
            // 删除失败任务
            failedTaskRepository.deleteByContactId(contactId)
                .onSuccess { result.failedTasksDeleted = it }
            
            result.success = true
        } catch (e: Exception) {
            result.success = false
            result.error = e.message
        }
        
        result
    }
    
    /**
     * 清理所有用户数据
     *
     * ⚠️ 警告：此操作不可逆
     *
     * @return 清理结果
     */
    suspend fun cleanupAllData(): CleanupResult = withContext(Dispatchers.IO) {
        val result = CleanupResult()
        
        try {
            // 删除所有对话记录（使用清理方法，传入未来时间戳删除所有）
            val futureTimestamp = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * 10 // 10年后
            conversationRepository.cleanupOldSummarizedLogs(futureTimestamp)
                .onSuccess { result.conversationsDeleted = it }
            
            // 删除所有每日总结
            dailySummaryRepository.cleanupOldSummaries(futureTimestamp)
                .onSuccess { result.summariesDeleted = it }
            
            // 删除所有失败任务
            failedTaskRepository.cleanupOldTasks(futureTimestamp)
                .onSuccess { result.failedTasksDeleted = it }
            
            result.success = true
        } catch (e: Exception) {
            result.success = false
            result.error = e.message
        }
        
        result
    }
    
    // ========== 数据导出 ==========
    
    /**
     * 准备导出数据（脱敏处理）
     *
     * @param data 原始数据
     * @param includeSensitive 是否包含敏感信息
     * @return 处理后的数据
     */
    fun prepareForExport(data: String, includeSensitive: Boolean = false): String {
        return if (includeSensitive) {
            data
        } else {
            maskSensitiveData(data)
        }
    }
    
    // ========== 辅助方法 ==========
    
    /**
     * 计算截止时间
     *
     * @param daysAgo 多少天前
     * @return 截止时间戳（毫秒）
     */
    private fun calculateCutoffTime(daysAgo: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * 检查是否应该执行自动清理
     *
     * @param lastCleanupTime 上次清理时间
     * @return true 如果应该执行清理
     */
    fun shouldRunAutoCleanup(lastCleanupTime: Long): Boolean {
        val intervalMillis = PrivacyConfig.AUTO_CLEANUP_INTERVAL_HOURS * 60 * 60 * 1000L
        return System.currentTimeMillis() - lastCleanupTime >= intervalMillis
    }
    
    /**
     * 检查当前是否是清理时间
     */
    fun isCleanupTime(): Boolean {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) == PrivacyConfig.CLEANUP_HOUR
    }
}

/**
 * 清理结果
 */
data class CleanupResult(
    var success: Boolean = false,
    var conversationsDeleted: Int = 0,
    var summariesDeleted: Int = 0,
    var failedTasksDeleted: Int = 0,
    var error: String? = null
) {
    val totalDeleted: Int
        get() = conversationsDeleted + summariesDeleted + failedTasksDeleted
    
    override fun toString(): String {
        return if (success) {
            "清理完成: 对话记录${conversationsDeleted}条, 总结${summariesDeleted}条, 失败任务${failedTasksDeleted}条"
        } else {
            "清理失败: $error"
        }
    }
}
