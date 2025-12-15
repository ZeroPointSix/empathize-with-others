package com.empathy.ai.domain.util

/**
 * 安全配置常量
 *
 * 定义加密算法、密钥管理等安全相关的配置
 *
 * 设计原则：
 * - 使用Android推荐的加密标准
 * - 密钥存储在AndroidKeyStore中
 * - 支持硬件安全模块（如果可用）
 */
object SecurityConfig {
    
    // ========== 加密算法配置 ==========
    
    /**
     * 加密算法
     * AES/GCM/NoPadding 是Android推荐的对称加密算法
     */
    const val ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding"
    
    /**
     * 密钥算法
     */
    const val KEY_ALGORITHM = "AES"
    
    /**
     * 密钥大小（位）
     * 256位提供足够的安全性
     */
    const val KEY_SIZE = 256
    
    /**
     * GCM认证标签长度（位）
     */
    const val GCM_TAG_LENGTH = 128
    
    /**
     * IV（初始化向量）长度（字节）
     * GCM模式推荐使用12字节IV
     */
    const val IV_LENGTH = 12
    
    // ========== 密钥存储配置 ==========
    
    /**
     * AndroidKeyStore提供者名称
     */
    const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    
    /**
     * 数据加密密钥别名
     */
    const val DATA_KEY_ALIAS = "empathy_ai_data_key"
    
    /**
     * API密钥加密密钥别名
     */
    const val API_KEY_ALIAS = "empathy_ai_api_key"
    
    // ========== 安全策略配置 ==========
    
    /**
     * 密钥有效期（天）
     * -1 表示永不过期
     */
    const val KEY_VALIDITY_DAYS = -1
    
    /**
     * 是否需要用户认证才能使用密钥
     * MVP阶段设为false，简化用户体验
     */
    const val REQUIRE_USER_AUTHENTICATION = false
    
    /**
     * 用户认证有效期（秒）
     * 仅在REQUIRE_USER_AUTHENTICATION为true时有效
     */
    const val USER_AUTHENTICATION_VALIDITY_SECONDS = 300
    
    // ========== 数据保护配置 ==========
    
    /**
     * 敏感数据字段列表
     * 这些字段在存储前需要加密
     */
    val SENSITIVE_FIELDS = listOf(
        "api_key",
        "phone_number",
        "id_card",
        "email",
        "address",
        "bank_account"
    )
    
    /**
     * 日志中需要脱敏的字段
     */
    val LOG_MASKED_FIELDS = listOf(
        "apiKey",
        "password",
        "token",
        "secret"
    )
}
