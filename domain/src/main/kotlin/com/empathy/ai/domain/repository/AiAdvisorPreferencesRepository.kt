package com.empathy.ai.domain.repository

/**
 * AI军师偏好设置仓库接口
 *
 * 定义AI军师偏好设置的存储和读取操作，用于实现自动恢复上次联系人功能。
 *
 * ## 业务背景 (PRD-00029)
 * - 自动恢复上次联系人：进入AI军师时自动加载上次使用的联系人
 * - 首次使用引导：无历史联系人时进入联系人选择页面
 * - 会话连续性：支持恢复上次会话（可选）
 *
 * ## 设计决策 (TDD-00029)
 * - 接口定义在:domain模块，实现在:data模块
 * - 遵循Clean Architecture依赖规则
 *
 * @see com.empathy.ai.data.local.AiAdvisorPreferences 实现类
 */
interface AiAdvisorPreferencesRepository {

    /**
     * 获取上次使用的联系人ID
     *
     * 业务规则 (PRD-00029/US-003):
     * - 返回null表示首次使用，应导航到联系人选择页面
     * - 返回非空值表示有历史记录，应自动恢复该联系人
     *
     * @return 联系人ID，如果不存在返回null
     */
    fun getLastContactId(): String?

    /**
     * 保存上次使用的联系人ID
     *
     * 调用时机 (TDD-00029):
     * - 用户选择联系人时
     * - 进入对话界面时
     * - 切换联系人时
     *
     * @param contactId 联系人ID
     */
    fun setLastContactId(contactId: String)

    /**
     * 获取上次使用的会话ID
     *
     * 业务规则 (PRD-00029):
     * - 当前版本不加载历史会话内容，仅用于记录
     * - 后续版本可用于恢复上次会话
     *
     * @return 会话ID，如果不存在返回null
     */
    fun getLastSessionId(): String?

    /**
     * 保存上次使用的会话ID
     *
     * @param sessionId 会话ID，传null时清除记录
     */
    fun setLastSessionId(sessionId: String?)

    /**
     * 清除所有偏好设置
     *
     * 使用场景:
     * - 用户登出
     * - 清除应用数据
     * - 重置AI军师状态
     */
    fun clear()
}
