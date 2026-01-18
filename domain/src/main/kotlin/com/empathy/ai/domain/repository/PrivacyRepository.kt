package com.empathy.ai.domain.repository

/**
 * 隐私规则仓储接口
 *
 * 业务背景:
 * - 隐私保护是应用的核心原则（Privacy First）
 * - 敏感数据必须在本地脱敏后才能发送给AI
 * - 采用"先脱敏，后发送"的策略，保护用户隐私
 *
 * 设计决策:
 * - 混合脱敏策略：用户标记规则 + 自动检测（姓名、手机号、邮箱等）
 * - 占位符格式：[TYPE_序号]，如[NAME_01]、[PHONE_02]
 * - 可逆转换：支持脱敏和还原，用于显示和发送不同场景
 *
 * 服务对象: PrivacyEngine (脱敏引擎)
 */
interface PrivacyRepository {

    /**
     * 获取所有隐私映射规则
     *
     * 用途: 用于构造正则替换器
     *
     * Map示例:
     * - {"张三" : "[NAME_01]", "13000000000" : "[PHONE_01]"}
     * - 自动检测规则内置，不在此返回
     *
     * @return 包含所有映射规则的Result
     */
    suspend fun getPrivacyMapping(): Result<Map<String, String>>

    /**
     * 添加隐私映射规则
     *
     * 业务规则:
     * - 用户手动标记或自动识别（首次出现时）
     * - 同一敏感信息对应唯一的占位符
     * - 相同占位符可对应多个原始值（需特殊处理）
     *
     * @param original 原始敏感信息
     * @param mask 脱敏后的占位符
     * @return 操作结果
     */
    suspend fun addRule(original: String, mask: String): Result<Unit>

    /**
     * 移除隐私映射规则
     *
     * 业务规则:
     * - 用户可删除自定义的隐私规则
     * - 自动检测规则无法删除
     *
     * @param original 原始敏感信息
     * @return 操作结果
     */
    suspend fun removeRule(original: String): Result<Unit>

    /**
     * 对文本进行脱敏处理
     *
     * 设计权衡:
     * - 混合策略：用户标记规则 + 自动检测（Regex）
     * - 自动检测：姓名、手机号、邮箱、身份证号等
     * - 保证AI收到的数据不包含原始敏感信息
     *
     * @param text 原始文本
     * @return 脱敏后的文本
     */
    suspend fun maskText(text: String): String

    /**
     * 对文本进行还原处理
     *
     * 业务规则:
     * - 将脱敏后的占位符还原为原始内容
     * - 用于用户界面显示完整信息
     * - 还原时需要映射表支持
     *
     * @param maskedText 脱敏后的文本
     * @return 还原后的文本
     */
    suspend fun unmaskText(maskedText: String): String
}
