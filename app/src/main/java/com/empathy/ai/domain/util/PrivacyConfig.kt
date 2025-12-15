package com.empathy.ai.domain.util

/**
 * 隐私保护配置
 *
 * 定义敏感数据处理、数据保留策略等隐私相关配置
 *
 * 设计原则：
 * - 隐私优先：默认启用所有保护措施
 * - 最小化数据：只保留必要的数据
 * - 用户控制：用户可以随时删除数据
 */
object PrivacyConfig {
    
    // ========== 敏感数据定义 ==========
    
    /**
     * 敏感数据类型
     */
    enum class SensitiveDataType(
        val displayName: String,
        val pattern: String,
        val maskPattern: String
    ) {
        /**
         * 手机号码
         * 匹配：13x, 14x, 15x, 16x, 17x, 18x, 19x 开头的11位数字
         */
        PHONE_NUMBER(
            displayName = "手机号",
            pattern = "1[3-9]\\d{9}",
            maskPattern = "***"
        ),
        
        /**
         * 身份证号
         * 匹配：18位身份证号（包含X结尾）
         */
        ID_CARD(
            displayName = "身份证号",
            pattern = "[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]",
            maskPattern = "***"
        ),
        
        /**
         * 邮箱地址
         */
        EMAIL(
            displayName = "邮箱",
            pattern = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
            maskPattern = "***@***.***"
        ),
        
        /**
         * 银行卡号
         * 匹配：16-19位数字
         */
        BANK_CARD(
            displayName = "银行卡号",
            pattern = "\\d{16,19}",
            maskPattern = "****"
        ),
        
        /**
         * 地址
         * 匹配：包含省/市/区/县/街/路/号等关键词的文本
         */
        ADDRESS(
            displayName = "地址",
            pattern = ".*(省|市|区|县|街|路|号|栋|单元|室).*",
            maskPattern = "[地址已隐藏]"
        )
    }
    
    // ========== 数据保留策略 ==========
    
    /**
     * 对话记录保留天数
     * 超过此天数的对话记录将被自动清理
     * -1 表示永不清理
     */
    const val CONVERSATION_RETENTION_DAYS = 90
    
    /**
     * 每日总结保留天数
     */
    const val SUMMARY_RETENTION_DAYS = 365
    
    /**
     * 失败任务保留天数
     */
    const val FAILED_TASK_RETENTION_DAYS = 30
    
    /**
     * 日志保留天数
     */
    const val LOG_RETENTION_DAYS = 7
    
    // ========== 数据清理配置 ==========
    
    /**
     * 自动清理间隔（小时）
     */
    const val AUTO_CLEANUP_INTERVAL_HOURS = 24
    
    /**
     * 清理任务执行时间（小时，24小时制）
     * 默认凌晨3点执行，减少对用户的影响
     */
    const val CLEANUP_HOUR = 3
    
    /**
     * 单次清理最大记录数
     * 避免一次性删除过多数据导致性能问题
     */
    const val MAX_CLEANUP_BATCH_SIZE = 1000
    
    // ========== 脱敏配置 ==========
    
    /**
     * 是否默认启用数据脱敏
     */
    const val DEFAULT_MASKING_ENABLED = true
    
    /**
     * 是否默认启用本地优先模式
     */
    const val DEFAULT_LOCAL_FIRST_ENABLED = true
    
    /**
     * 脱敏占位符
     */
    const val MASK_PLACEHOLDER = "***"
    
    /**
     * 部分脱敏时保留的字符数（前后各保留）
     */
    const val PARTIAL_MASK_KEEP_LENGTH = 3
    
    // ========== 导出配置 ==========
    
    /**
     * 导出数据时是否包含敏感信息
     */
    const val EXPORT_INCLUDE_SENSITIVE = false
    
    /**
     * 导出文件加密
     */
    const val EXPORT_ENCRYPTED = true
    
    // ========== 辅助方法 ==========
    
    /**
     * 获取所有敏感数据类型的正则表达式
     */
    fun getAllPatterns(): Map<SensitiveDataType, Regex> {
        return SensitiveDataType.entries.associateWith { 
            Regex(it.pattern) 
        }
    }
    
    /**
     * 检测文本中的敏感数据类型
     */
    fun detectSensitiveTypes(text: String): Set<SensitiveDataType> {
        return SensitiveDataType.entries.filter { type ->
            Regex(type.pattern).containsMatchIn(text)
        }.toSet()
    }
    
    /**
     * 对文本进行脱敏处理
     */
    fun maskText(text: String, types: Set<SensitiveDataType> = SensitiveDataType.entries.toSet()): String {
        var result = text
        types.forEach { type ->
            result = result.replace(Regex(type.pattern), type.maskPattern)
        }
        return result
    }
    
    /**
     * 部分脱敏（保留前后几位）
     */
    fun partialMask(text: String, keepLength: Int = PARTIAL_MASK_KEEP_LENGTH): String {
        if (text.length <= keepLength * 2) {
            return MASK_PLACEHOLDER
        }
        val prefix = text.take(keepLength)
        val suffix = text.takeLast(keepLength)
        val maskLength = text.length - keepLength * 2
        return "$prefix${"*".repeat(maskLength)}$suffix"
    }
}
