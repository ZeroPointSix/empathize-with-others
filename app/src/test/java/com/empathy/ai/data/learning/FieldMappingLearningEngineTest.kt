package com.empathy.ai.data.learning

import org.junit.Before
import org.junit.Test
import org.junit.After
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment

/**
 * 字段映射学习引擎测试
 * 
 * 测试字段映射学习功能
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FieldMappingLearningEngineTest {
    
    private lateinit var learningEngine: FieldMappingLearningEngine
    private val context = RuntimeEnvironment.getApplication()
    
    @Before
    fun setUp() {
        learningEngine = FieldMappingLearningEngine.getInstance()
        // 重置学习状态
        learningEngine.resetLearningState()
    }
    
    @After
    fun tearDown() {
        // 清理测试数据
        learningEngine.resetLearningState()
    }
    
    @Test
    fun `test collectMappingData - successful mapping`() {
        // 模拟成功的字段映射
        val json = """{"回复建议": "这是建议的回复内容"}"""
        val result = mapOf("replySuggestion" to "这是建议的回复内容")
        val context = createTestContext()
        
        // 收集映射数据
        learningEngine.collectMappingData(json, result, context)
        
        // 获取学习统计
        val stats = learningEngine.getLearningStatistics()
        
        assertEquals(1, stats.totalMappings)
        assertEquals(1, stats.successfulLearning)
        assertEquals(0, stats.failedLearning)
    }
    
    @Test
    fun `test collectMappingData - multiple mappings`() {
        // 模拟多个字段映射
        val json = """{"回复建议": "回复内容", "策略分析": "分析内容"}"""
        val result = mapOf(
            "replySuggestion" to "回复内容",
            "strategyAnalysis" to "分析内容"
        )
        val context = createTestContext()
        
        // 收集映射数据
        learningEngine.collectMappingData(json, result, context)
        
        // 获取学习统计
        val stats = learningEngine.getLearningStatistics()
        
        assertEquals(2, stats.totalMappings)
        assertEquals(2, stats.successfulLearning)
        assertEquals(0, stats.failedLearning)
    }
    
    @Test
    fun `test getMappingSuggestion - existing mapping`() {
        // 先收集映射数据
        val json = """{"回复建议": "回复内容"}"""
        val result = mapOf("replySuggestion" to "回复内容")
        val context = createTestContext()
        learningEngine.collectMappingData(json, result, context)
        
        // 获取映射建议
        val suggestions = learningEngine.getMappingSuggestion("回复建议")
        
        assertNotNull(suggestions)
        assertEquals("replySuggestion", suggestions!!.targetField)
        assertTrue(suggestions.confidence > 0.0)
    }
    
    @Test
    fun `test getMappingSuggestion - non-existing mapping`() {
        // 获取不存在的映射建议
        val suggestions = learningEngine.getMappingSuggestion("未知字段")
        
        assertNull(suggestions)
    }
    
    @Test
    fun `test getMappingSuggestion - multiple suggestions`() {
        // 收集多次映射数据，建立置信度
        repeat(5) {
            val json = """{"回复建议": "回复内容 $it"}"""
            val result = mapOf("replySuggestion" to "回复内容 $it")
            val context = createTestContext()
            learningEngine.collectMappingData(json, result, context)
        }
        
        // 收集其他映射数据
        repeat(2) {
            val json = """{"建议回复": "回复内容 $it"}"""
            val result = mapOf("replySuggestion" to "回复内容 $it")
            val context = createTestContext()
            learningEngine.collectMappingData(json, result, context)
        }
        
        // 获取映射建议
        val suggestions1 = learningEngine.getMappingSuggestion("回复建议")
        val suggestions2 = learningEngine.getMappingSuggestion("建议回复")
        
        assertNotNull(suggestions1)
        assertNotNull(suggestions2)
        assertEquals("replySuggestion", suggestions1!!.targetField)
        assertEquals("replySuggestion", suggestions2!!.targetField)
        
        // "回复建议"的置信度应该更高，因为出现次数更多
        assertTrue(suggestions1.confidence > suggestions2!!.confidence)
    }
    
    @Test
    fun `test confirmMapping`() {
        // 先收集映射数据
        val json = """{"回复建议": "回复内容"}"""
        val result = mapOf("replySuggestion" to "回复内容")
        val context = createTestContext()
        learningEngine.collectMappingData(json, result, context)
        
        // 确认映射
        learningEngine.confirmMapping("回复建议", "replySuggestion")
        
        // 获取映射建议
        val suggestions = learningEngine.getMappingSuggestion("回复建议")
        
        assertNotNull(suggestions)
        assertEquals("replySuggestion", suggestions!!.targetField)
        // 确认后的置信度应该很高
        assertTrue(suggestions.confidence > 0.9)
    }
    
    @Test
    fun `test rejectMapping`() {
        // 先收集映射数据
        val json = """{"回复建议": "回复内容"}"""
        val result = mapOf("replySuggestion" to "回复内容")
        val context = createTestContext()
        learningEngine.collectMappingData(json, result, context)
        
        // 拒绝映射
        learningEngine.rejectMapping("回复建议", "replySuggestion")
        
        // 获取映射建议
        val suggestions = learningEngine.getMappingSuggestion("回复建议")
        
        // 拒绝后应该不再建议这个映射
        assertNull(suggestions)
    }
    
    @Test
    fun `test addCustomMapping`() {
        // 添加自定义映射
        learningEngine.addCustomMapping("自定义字段", "customField")
        
        // 获取映射建议
        val suggestions = learningEngine.getMappingSuggestion("自定义字段")
        
        assertNotNull(suggestions)
        assertEquals("customField", suggestions!!.targetField)
        // 自定义映射的置信度应该是1.0
        assertEquals(1.0, suggestions.confidence, 0.01)
    }
    
    @Test
    fun `test removeCustomMapping`() {
        // 添加自定义映射
        learningEngine.addCustomMapping("自定义字段", "customField")
        
        // 移除自定义映射
        learningEngine.removeCustomMapping("自定义字段")
        
        // 获取映射建议
        val suggestions = learningEngine.getMappingSuggestion("自定义字段")
        
        assertNull(suggestions)
    }
    
    @Test
    fun `test getLearningStatistics`() {
        // 收集多次映射数据
        repeat(5) {
            val json = """{"回复建议": "回复内容 $it"}"""
            val result = mapOf("replySuggestion" to "回复内容 $it")
            val context = createTestContext()
            learningEngine.collectMappingData(json, result, context)
        }
        
        // 添加和确认一些映射
        learningEngine.confirmMapping("回复建议", "replySuggestion")
        learningEngine.addCustomMapping("自定义字段", "customField")
        
        // 获取学习统计
        val stats = learningEngine.getLearningStatistics()
        
        assertEquals(6, stats.totalMappings) // 5个学习 + 1个自定义
        assertEquals(5, stats.successfulLearning)
        assertEquals(0, stats.failedLearning)
        assertEquals(1, stats.confirmedMappings)
        assertEquals(1, stats.customMappings)
        assertTrue(stats.averageConfidence > 0.0)
    }
    
    @Test
    fun `test exportMappings`() {
        // 添加一些映射
        learningEngine.addCustomMapping("字段1", "field1")
        learningEngine.addCustomMapping("字段2", "field2")
        
        // 导出映射
        val exportedMappings = learningEngine.exportMappings()
        
        assertEquals(2, exportedMappings.size)
        assertTrue(exportedMappings.containsKey("字段1"))
        assertTrue(exportedMappings.containsKey("字段2"))
        assertEquals("field1", exportedMappings["字段1"])
        assertEquals("field2", exportedMappings["字段2"])
    }
    
    @Test
    fun `test importMappings`() {
        // 准备导入数据
        val importData = mapOf(
            "导入字段1" to "importField1",
            "导入字段2" to "importField2"
        )
        
        // 导入映射
        learningEngine.importMappings(importData)
        
        // 验证导入结果
        val suggestions1 = learningEngine.getMappingSuggestion("导入字段1")
        val suggestions2 = learningEngine.getMappingSuggestion("导入字段2")
        
        assertNotNull(suggestions1)
        assertNotNull(suggestions2)
        assertEquals("importField1", suggestions1!!.targetField)
        assertEquals("importField2", suggestions2!!.targetField)
    }
    
    @Test
    fun `test resetLearningState`() {
        // 添加一些映射
        learningEngine.addCustomMapping("字段1", "field1")
        learningEngine.collectMappingData("""{"回复建议": "内容"}""", mapOf("replySuggestion" to "内容"), createTestContext())
        
        // 重置学习状态
        learningEngine.resetLearningState()
        
        // 验证重置结果
        val suggestions1 = learningEngine.getMappingSuggestion("字段1")
        val suggestions2 = learningEngine.getMappingSuggestion("回复建议")
        val stats = learningEngine.getLearningStatistics()
        
        assertNull(suggestions1)
        assertNull(suggestions2)
        assertEquals(0, stats.totalMappings)
        assertEquals(0, stats.successfulLearning)
        assertEquals(0, stats.customMappings)
    }
    
    @Test
    fun `test concurrent learning`() {
        // 测试并发学习
        val threads = (1..5).map { threadId ->
            Thread {
                repeat(10) {
                    val json = """{"并发字段$threadId": "内容$threadId-$it"}"""
                    val result = mapOf("concurrentField$threadId" to "内容$threadId-$it")
                    val context = createTestContext()
                    learningEngine.collectMappingData(json, result, context)
                }
            }.also { it.start() }
        }
        
        // 等待所有线程完成
        threads.forEach { it.join() }
        
        // 验证学习结果
        val stats = learningEngine.getLearningStatistics()
        
        assertEquals(50, stats.totalMappings) // 5 threads * 10 records
        assertEquals(50, stats.successfulLearning)
        assertEquals(0, stats.failedLearning)
    }
    
    @Test
    fun `test fuzzy matching`() {
        // 添加一些相似的映射
        learningEngine.collectMappingData("""{"回复建议": "内容"}""", mapOf("replySuggestion" to "内容"), createTestContext())
        learningEngine.collectMappingData("""{"回复建议": "内容"}""", mapOf("replySuggestion" to "内容"), createTestContext())
        learningEngine.collectMappingData("""{"回复建议": "内容"}""", mapOf("replySuggestion" to "内容"), createTestContext())
        
        // 测试模糊匹配
        val suggestions1 = learningEngine.getMappingSuggestion("回复建议")
        val suggestions2 = learningEngine.getMappingSuggestion("回复建議") // 繁体字
        val suggestions3 = learningEngine.getMappingSuggestion("回复建议 ") // 带空格
        
        assertNotNull(suggestions1)
        assertNotNull(suggestions2)
        assertNotNull(suggestions3)
        
        // 模糊匹配的置信度可能略低，但应该仍然匹配
        assertTrue(suggestions1!!.confidence > 0.0)
        assertTrue(suggestions2!!.confidence > 0.0)
        assertTrue(suggestions3!!.confidence > 0.0)
    }
    
    @Test
    fun `test learning confidence calculation`() {
        // 收集多次映射数据
        repeat(10) {
            val json = """{"高频字段": "内容$it"}"""
            val result = mapOf("highFrequencyField" to "内容$it")
            val context = createTestContext()
            learningEngine.collectMappingData(json, result, context)
        }
        
        // 收集少量其他映射数据
        repeat(2) {
            val json = """{"低频字段": "内容$it"}"""
            val result = mapOf("lowFrequencyField" to "内容$it")
            val context = createTestContext()
            learningEngine.collectMappingData(json, result, context)
        }
        
        // 获取映射建议
        val highFreqSuggestions = learningEngine.getMappingSuggestion("高频字段")
        val lowFreqSuggestions = learningEngine.getMappingSuggestion("低频字段")
        
        assertNotNull(highFreqSuggestions)
        assertNotNull(lowFreqSuggestions)
        
        // 高频字段的置信度应该更高
        assertTrue(highFreqSuggestions!!.confidence > lowFreqSuggestions!!.confidence)
        assertTrue(highFreqSuggestions.confidence > 0.8) // 10次出现应该有高置信度
        assertTrue(lowFreqSuggestions.confidence < 0.5) // 2次出现应该有低置信度
    }
    
    /**
     * 创建测试上下文
     */
    private fun createTestContext() = com.empathy.ai.data.parser.ParsingContext(
        operationId = "test_${System.currentTimeMillis()}",
        modelName = "test-model",
        operationType = "test-operation"
    )
}