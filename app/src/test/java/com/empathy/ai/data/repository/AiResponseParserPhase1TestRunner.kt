package com.empathy.ai.data.repository

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * AI响应解析器阶段1改进测试套件
 * 
 * 运行所有与阶段1改进相关的测试，验证改进效果
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // 核心解析测试
    AiResponseParserCorePropertySimpleTest::class,
    AiResponseParserCorePropertyTest::class,
    
    // 字段映射测试
    ChineseFieldNameMappingSimpleTest::class,
    
    // JSON预处理测试
    PreprocessJsonOptimizationTest::class,
    
    // 各数据类型增强测试
    AnalysisResultEnhancementTest::class,
    SafetyCheckResultEnhancementTest::class,
    ExtractedDataEnhancementTest::class,
    
    // 性能测试
    AiResponseParserPerformancePropertyTest::class,
    AiResponseParserPerformanceBenchmarkTest::class,
    
    // 边界情况测试
    AiResponseParserEdgeCasePropertyTest::class,
    
    // 阶段1改进综合测试
    AiResponseParserPhase1ImprovementTest::class
)
class AiResponseParserPhase1TestRunner