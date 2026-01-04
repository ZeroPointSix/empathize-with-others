package com.empathy.ai.data.parser

import android.content.Context

/**
 * FieldMapper 字段映射器接口
 *
 * 业务背景：支持中英文字段名映射，提升AI响应容错性
 *
 * 为什么需要字段映射？
 * - 不同AI服务商可能返回不同格式的字段名
 * - 有的用英文 (suggestions, advice)
 * - 有的用中文 (建议, 意见)
 * - 通过映射层屏蔽差异，保证解析稳定性
 *
 * 设计决策：
 * - 映射配置：英文字段名 → 中文字段名列表
 * - 模糊匹配：支持相似度匹配（阈值0.8）
 * - 链式调用：通过 MappingContext 配置行为
 *
 * 使用场景：
 * - AI响应解析（EnhancedJsonCleaner）
 * - 多语言JSON兼容
 * - API响应格式适配
 *
 * @see EnhancedJsonCleaner 增强版清洗器（使用此接口）
 */
interface FieldMapper {

    /**
     * 映射JSON中的字段名
     *
     * 【处理逻辑】
     * 1. 解析输入JSON
     * 2. 遍历所有字段
     * 3. 如果字段名在映射表中，替换为标准字段名
     * 4. 返回映射后的JSON
     *
     * 【模糊匹配】
     * - 如果 enableFuzzyMatching = true
     * - 字段名相似度 >= fuzzyThreshold (默认0.8) 时也进行映射
     * - 支持处理打字错误或变体写法
     *
     * @param json 原始JSON字符串
     * @param context 映射上下文（控制模糊匹配、日志等）
     * @return 映射后的JSON字符串
     */
    fun mapFields(json: String, context: MappingContext = MappingContext()): String

    /**
     * 添加字段映射
     *
     * 【使用场景】
     * - 运行时动态添加映射
     * - 支持新的AI响应格式
     *
     * @param english 英文字段名（标准字段名）
     * @param chinese 中文字段名列表（需要映射的变体）
     */
    fun addMapping(english: String, chinese: List<String>)

    /**
     * 获取所有映射配置
     *
     * @return 英文字段名 → 中文字段名列表 的映射表
     */
    fun getAllMappings(): Map<String, List<String>>

    /**
     * 清除所有映射配置
     *
     * 【使用场景】
     * - 重置映射表
     * - 切换到不同的映射配置集
     */
    fun clearMappings()
}

/**
 * MappingContext 映射上下文配置
 *
 * 用于控制字段映射的行为参数
 *
 * 配置项说明：
 * - enableFuzzyMatching: 是否启用模糊匹配
 * - fuzzyThreshold: 模糊匹配阈值（0.0-1.0）
 * - enableDetailedLogging: 是否记录详细日志
 * - androidContext: 用于加载配置文件的Android上下文
 * - properties: 自定义属性扩展
 *
 * 设计决策：
 * - 使用 data class 方便创建和复制
 * - 提供 withXxx() 方法支持链式调用
 * - 默认值平衡兼容性和性能
 */
data class MappingContext(
    /**
     * 是否启用模糊匹配
     *
     * 模糊匹配可以处理：
     * - 打字错误（如 "suggetion" → "suggestion"）
     * - 缩写变体（如 "adv" → "advice"）
     * - 拼写差异（如 "color" vs "colour"）
     *
     * 权衡：
     * - 启用可能提高匹配率但增加误匹配风险
     * - 建议在可信数据源上启用
     */
    val enableFuzzyMatching: Boolean = true,

    /**
     * 模糊匹配阈值
     *
     * 取值范围：0.0 - 1.0
     * - 0.8: 宽松匹配（推荐默认值）
     * - 0.9: 严格匹配
     * - 0.6: 更宽松的匹配
     *
     * 计算方式：字符串相似度（如 Levenshtein 距离）
     */
    val fuzzyThreshold: Double = 0.8,

    /**
     * 是否启用详细日志
     *
     * 用于调试映射逻辑：
     * - 记录哪些字段被映射
     * - 记录匹配分数
     * - 记录模糊匹配详情
     *
     * 注意：生产环境建议关闭以减少日志量
     */
    val enableDetailedLogging: Boolean = false,

    /**
     * Android上下文
     *
     * 用于从资源文件或配置文件加载映射：
     * - assets/field_mappings.json
     * - string-array 资源
     *
     * 可选：如果不需要加载配置，可为 null
     */
    val androidContext: Context? = null,

    /**
     * 自定义属性扩展
     *
     用于传递额外的配置参数：
     * - 特定领域的映射规则
     * - 临时启用的映射
     * - 调试标志
     */
    val properties: Map<String, Any> = emptyMap()
) {
    /**
     * 添加自定义属性
     *
     * @param key 属性名
     * @param value 属性值
     * @return 新的 MappingContext 实例
     */
    fun withProperty(key: String, value: Any): MappingContext {
        return copy(properties = properties + (key to value))
    }

    /**
     * 配置模糊匹配
     *
     * @param enabled 是否启用
     * @return 新的 MappingContext 实例
     */
    fun withFuzzyMatching(enabled: Boolean = true): MappingContext {
        return copy(enableFuzzyMatching = enabled)
    }

    /**
     * 配置模糊匹配阈值
     *
     * @param threshold 阈值 (0.0-1.0)
     * @return 新的 MappingContext 实例
     */
    fun withFuzzyThreshold(threshold: Double): MappingContext {
        return copy(fuzzyThreshold = threshold.coerceIn(0.0, 1.0))
    }

    /**
     * 配置详细日志
     *
     * @param enabled 是否启用
     * @return 新的 MappingContext 实例
     */
    fun withDetailedLogging(enabled: Boolean = true): MappingContext {
        return copy(enableDetailedLogging = enabled)
    }

    /**
     * 配置Android上下文
     *
     * @param context Android上下文
     * @return 新的 MappingContext 实例
     */
    fun withAndroidContext(context: Context): MappingContext {
        return copy(androidContext = context)
    }
}
