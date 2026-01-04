package com.empathy.ai.domain.service

import com.empathy.ai.domain.model.ConversationContextConfig
import com.empathy.ai.domain.model.MessageSender
import com.empathy.ai.domain.model.TimestampedMessage
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.SettingsRepository
import com.empathy.ai.domain.util.ConversationContextBuilder
import com.empathy.ai.domain.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 会话上下文服务
 *
 * 统一管理历史对话上下文的构建，供所有UseCase共享使用。
 * 解决三种模式（分析/润色/回复）上下文不共通的问题。
 *
 * 业务背景:
 *   - PRD-00007: 对话上下文连续性增强需求
 *   - 问题: AnalyzeChat/PolishDraft/GenerateReply 各自独立查询历史，重复代码
 *   - 解决: 抽取公共逻辑到统一服务，确保上下文一致性
 *
 * 设计决策:
 *   - 单一职责: 专注于历史上下文的查询和构建
 *   - 可复用: 所有需要历史上下文的UseCase统一调用此服务
 *   - 可配置: 通过SettingsRepository读取用户配置的历史条数
 *   - 降级策略: 任何步骤失败返回空字符串，不影响主流程
 *
 * @see BUG-00015 三种模式上下文不共通问题分析
 * @see TDD-00007 对话上下文连续性增强技术设计
 */
@Singleton
class SessionContextService @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val conversationContextBuilder: ConversationContextBuilder,
    private val settingsRepository: SettingsRepository,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "SessionContextService"
    }

    /**
     * 获取联系人的历史对话上下文
     *
     * 查询数据库中该联系人的最近对话记录，并构建带时间流逝标记的上下文字符串。
     * 所有UseCase（分析/润色/回复）统一调用此方法获取历史上下文。
     *
     * 执行流程:
     *   1. 读取用户配置的历史条数 (SettingsRepository)
     *   2. 如果配置为0，直接返回空，跳过数据库查询
     *   3. 查询最近的对话记录 (ConversationRepository)
     *   4. 转换为 TimestampedMessage (标记发送者为 ME)
     *   5. 构建带时间流逝标记的历史上下文 (ConversationContextBuilder)
     *
     * 降级策略:
     *   - 配置为0: 返回空字符串
     *   - 无历史记录: 返回空字符串
     *   - 转换/构建失败: 返回空字符串 (不抛出异常)
     *
     * @param contactId 联系人ID
     * @return 格式化后的历史对话上下文字符串，如果没有历史或配置为0则返回空字符串
     */
    suspend fun getHistoryContext(contactId: String): String {
        return try {
            // 1. 读取用户配置的历史条数
            val historyCount = settingsRepository.getHistoryConversationCount()
                .getOrDefault(ConversationContextConfig.DEFAULT_HISTORY_COUNT)

            logger.d(TAG, "获取历史上下文: contactId=$contactId, historyCount=$historyCount")

            // 2. 如果配置为0，直接返回空
            if (historyCount <= 0) {
                logger.d(TAG, "历史条数配置为0，跳过历史查询")
                return ""
            }

            // 3. 查询最近的对话记录
            val recentLogs = conversationRepository
                .getRecentConversations(contactId, historyCount)
                .getOrDefault(emptyList())

            if (recentLogs.isEmpty()) {
                logger.d(TAG, "没有找到历史对话记录")
                return ""
            }

            logger.d(TAG, "找到 ${recentLogs.size} 条历史对话记录")

            // 4. 转换为TimestampedMessage
            val messages = recentLogs.mapNotNull { log ->
                try {
                    TimestampedMessage(
                        content = log.userInput,
                        timestamp = log.timestamp,
                        sender = MessageSender.ME
                    )
                } catch (e: Exception) {
                    logger.w(TAG, "跳过无效的对话记录: ${log.id}")
                    null
                }
            }

            // 5. 构建带时间流逝标记的历史上下文
            val historyContext = conversationContextBuilder.buildHistoryContext(messages)
            logger.d(TAG, "历史上下文构建完成，长度: ${historyContext.length}")

            historyContext
        } catch (e: Exception) {
            logger.e(TAG, "获取历史上下文失败，降级为空历史", e)
            ""  // 降级：返回空历史，不影响主流程
        }
    }

    /**
     * 获取历史上下文（带自定义条数）
     *
     * 用于需要自定义历史条数的场景，忽略用户配置。
     *
     * @param contactId 联系人ID
     * @param limit 历史条数限制
     * @return 格式化后的历史对话上下文字符串
     */
    suspend fun getHistoryContext(contactId: String, limit: Int): String {
        return try {
            if (limit <= 0) return ""

            val recentLogs = conversationRepository
                .getRecentConversations(contactId, limit)
                .getOrDefault(emptyList())

            if (recentLogs.isEmpty()) return ""

            val messages = recentLogs.mapNotNull { log ->
                try {
                    TimestampedMessage(
                        content = log.userInput,
                        timestamp = log.timestamp,
                        sender = MessageSender.ME
                    )
                } catch (e: Exception) {
                    logger.w(TAG, "跳过无效的对话记录: ${log.id}")
                    null
                }
            }

            conversationContextBuilder.buildHistoryContext(messages)
        } catch (e: Exception) {
            logger.e(TAG, "获取历史上下文失败", e)
            ""
        }
    }

    /**
     * 检查是否有历史对话
     *
     * 快速检查联系人是否有历史对话记录，用于UI显示或逻辑判断。
     * 仅查询1条记录，通过 isNotEmpty() 判断是否存在，效率优先。
     *
     * 为什么不用 count() > 0?
     *   - count() 需要扫描所有匹配行，效率低
     *   - limit 1 + isNotEmpty() 只需检测是否存在，SQL执行更快
     *
     * @param contactId 联系人ID
     * @return true 如果有历史对话
     */
    suspend fun hasHistoryContext(contactId: String): Boolean {
        return try {
            val recentLogs = conversationRepository
                .getRecentConversations(contactId, 1)
                .getOrDefault(emptyList())
            recentLogs.isNotEmpty()
        } catch (e: Exception) {
            logger.e(TAG, "检查历史对话失败", e)
            false
        }
    }
}
