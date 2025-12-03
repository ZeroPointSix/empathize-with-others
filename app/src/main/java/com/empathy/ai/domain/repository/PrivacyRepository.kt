package com.empathy.ai.domain.repository

/**
 * 隐私规则仓库接口
 *
 * 服务对象: PrivacyEngine (脱敏引擎)
 */
interface PrivacyRepository {
    /**
     * 获取所有隐私映射规则
     *
     * 用于构造正则替换器
     * Map 示例: {"真实姓名" : "[NAME_01]", "手机号" : "[PHONE_01]"}
     *
     * @return 包含所有映射规则的 Result
     */
    suspend fun getPrivacyMapping(): Result<Map<String, String>>

    /**
     * 添加新的隐私映射规则
     *
     * 用户手动标记或自动识别
     *
     * @param original 原始敏感信息
     * @param mask 脱敏后的占位符
     * @return 操作结果
     */
    suspend fun addRule(original: String, mask: String): Result<Unit>

    /**
     * 移除隐私映射规则
     *
     * @param original 原始敏感信息
     * @return 操作结果
     */
    suspend fun removeRule(original: String): Result<Unit>
}
