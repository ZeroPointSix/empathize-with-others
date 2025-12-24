package com.empathy.ai.domain.util

/**
 * JSON解析器接口
 *
 * 用于在Domain层抽象JSON解析功能，不依赖具体的JSON库（如Moshi、Gson）。
 * 实现类在Data层提供。
 *
 * @see TDD-00017 Clean Architecture模块化改造技术设计
 */
interface JsonParser {
    /**
     * 将对象序列化为JSON字符串
     *
     * @param obj 要序列化的对象
     * @return JSON字符串
     */
    fun <T> toJson(obj: T): String

    /**
     * 将JSON字符串反序列化为对象
     *
     * @param json JSON字符串
     * @param clazz 目标类型
     * @return 反序列化后的对象，解析失败返回null
     */
    fun <T> fromJson(json: String, clazz: Class<T>): T?

    /**
     * 将JSON字符串解析为Map
     *
     * @param json JSON字符串
     * @return 解析后的Map，解析失败返回null
     */
    fun parseToMap(json: String): Map<String, Any>?
}
