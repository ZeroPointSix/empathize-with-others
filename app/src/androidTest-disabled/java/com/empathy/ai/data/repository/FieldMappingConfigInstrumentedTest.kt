package com.empathy.ai.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FieldMappingConfig Instrumented 测试
 *
 * 测试配置文件加载功能（需要 Android Context）
 *
 * 测试目标：
 * - 测试配置文件加载成功
 * - 测试配置文件加载失败时使用默认配置
 * - 测试配置缓存机制
 *
 * 需求: 2.5, 8.2
 */
@RunWith(AndroidJUnit4::class)
class FieldMappingConfigInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // 清除缓存，确保每个测试都是独立的
        FieldMappingConfig.clearCache()
    }

    /**
     * 测试配置文件加载成功
     *
     * 验证：
     * - 配置文件可以从 assets 目录加载
     * - 加载的配置包含所有必需的字段映射
     * - 每个字段映射都有对应的中文字段名列表
     */
    @Test
    fun testLoadConfigurationFromAssets() {
        // When
        val mappings = FieldMappingConfig.load(context)

        // Then
        assertNotNull("配置映射不应为 null", mappings)
        assertTrue("配置映射不应为空", mappings.isNotEmpty())

        // 验证包含所有必需的字段
        val expectedFields = setOf(
            "replySuggestion",
            "strategyAnalysis",
            "riskLevel",
            "isSafe",
            "triggeredRisks",
            "suggestion",
            "facts",
            "redTags",
            "greenTags"
        )

        assertEquals("应该包含所有必需的字段映射", expectedFields, mappings.keys)

        // 验证每个字段都有中文映射
        mappings.forEach { (englishField, chineseFields) ->
            assertTrue(
                "字段 $englishField 应该至少有一个中文映射",
                chineseFields.isNotEmpty()
            )
        }

        println("✅ 配置文件加载成功测试通过")
        println("  加载的字段数量: ${mappings.size}")
        mappings.forEach { (english, chinese) ->
            println("  $english -> ${chinese.size} 个中文映射")
        }
    }

    /**
     * 测试配置缓存机制
     *
     * 验证：
     * - 第一次调用 load() 会从文件加载
     * - 后续调用 load() 会使用缓存
     * - 缓存的配置与第一次加载的配置相同
     */
    @Test
    fun testConfigurationCaching() {
        // 第一次加载
        val firstLoad = FieldMappingConfig.load(context)
        assertNotNull("第一次加载不应为 null", firstLoad)

        // 第二次加载（应该使用缓存）
        val secondLoad = FieldMappingConfig.load(context)
        assertNotNull("第二次加载不应为 null", secondLoad)

        // 验证两次加载的结果相同（引用相同，因为使用了缓存）
        assertSame("应该返回相同的缓存实例", firstLoad, secondLoad)

        println("✅ 配置缓存机制测试通过")
        println("  第一次加载: ${firstLoad.size} 个字段")
        println("  第二次加载: ${secondLoad.size} 个字段")
        println("  使用缓存: ${firstLoad === secondLoad}")
    }

    /**
     * 测试清除缓存后重新加载
     *
     * 验证：
     * - clearCache() 可以清除缓存
     * - 清除缓存后再次调用 load() 会重新加载配置
     */
    @Test
    fun testClearCacheAndReload() {
        // 第一次加载
        val firstLoad = FieldMappingConfig.load(context)
        assertNotNull("第一次加载不应为 null", firstLoad)

        // 清除缓存
        FieldMappingConfig.clearCache()

        // 重新加载
        val reloaded = FieldMappingConfig.load(context)
        assertNotNull("重新加载不应为 null", reloaded)

        // 验证内容相同但不是同一个实例
        assertEquals("内容应该相同", firstLoad.keys, reloaded.keys)
        assertNotSame("应该是不同的实例", firstLoad, reloaded)

        println("✅ 清除缓存后重新加载测试通过")
        println("  第一次加载: ${firstLoad.size} 个字段")
        println("  重新加载: ${reloaded.size} 个字段")
        println("  是否为同一实例: ${firstLoad === reloaded}")
    }

    /**
     * 测试配置文件内容的正确性
     *
     * 验证：
     * - 配置文件中的字段映射与预期一致
     * - 每个字段的中文映射列表包含预期的值
     */
    @Test
    fun testConfigurationContent() {
        // When
        val mappings = FieldMappingConfig.load(context)

        // Then - 验证 replySuggestion 映射
        val replySuggestionMappings = mappings["replySuggestion"]
        assertNotNull("replySuggestion 映射不应为 null", replySuggestionMappings)
        assertTrue("应该包含 '回复建议'", replySuggestionMappings!!.contains("回复建议"))
        assertTrue("应该包含 '建议回复'", replySuggestionMappings.contains("建议回复"))
        assertTrue("应该包含 '话术建议'", replySuggestionMappings.contains("话术建议"))

        // 验证 strategyAnalysis 映射
        val strategyAnalysisMappings = mappings["strategyAnalysis"]
        assertNotNull("strategyAnalysis 映射不应为 null", strategyAnalysisMappings)
        assertTrue("应该包含 '策略分析'", strategyAnalysisMappings!!.contains("策略分析"))
        assertTrue("应该包含 '心理分析'", strategyAnalysisMappings.contains("心理分析"))
        assertTrue("应该包含 '军师分析'", strategyAnalysisMappings.contains("军师分析"))

        // 验证 riskLevel 映射
        val riskLevelMappings = mappings["riskLevel"]
        assertNotNull("riskLevel 映射不应为 null", riskLevelMappings)
        assertTrue("应该包含 '风险等级'", riskLevelMappings!!.contains("风险等级"))
        assertTrue("应该包含 '风险级别'", riskLevelMappings.contains("风险级别"))

        // 验证 isSafe 映射
        val isSafeMappings = mappings["isSafe"]
        assertNotNull("isSafe 映射不应为 null", isSafeMappings)
        assertTrue("应该包含 '是否安全'", isSafeMappings!!.contains("是否安全"))
        assertTrue("应该包含 '安全'", isSafeMappings.contains("安全"))

        // 验证 facts 映射
        val factsMappings = mappings["facts"]
        assertNotNull("facts 映射不应为 null", factsMappings)
        assertTrue("应该包含 '事实'", factsMappings!!.contains("事实"))
        assertTrue("应该包含 '事实信息'", factsMappings.contains("事实信息"))

        // 验证 redTags 映射
        val redTagsMappings = mappings["redTags"]
        assertNotNull("redTags 映射不应为 null", redTagsMappings)
        assertTrue("应该包含 '红色标签'", redTagsMappings!!.contains("红色标签"))
        assertTrue("应该包含 '雷区'", redTagsMappings.contains("雷区"))

        // 验证 greenTags 映射
        val greenTagsMappings = mappings["greenTags"]
        assertNotNull("greenTags 映射不应为 null", greenTagsMappings)
        assertTrue("应该包含 '绿色标签'", greenTagsMappings!!.contains("绿色标签"))
        assertTrue("应该包含 '策略'", greenTagsMappings.contains("策略"))

        println("✅ 配置文件内容正确性测试通过")
        println("  所有字段映射都包含预期的中文字段名")
    }

    /**
     * 测试配置文件加载失败时使用默认配置
     *
     * 注意：这个测试无法直接模拟文件不存在的情况，
     * 因为配置文件已经存在于 assets 目录中。
     * 但我们可以验证 getDefaultMappings() 方法的行为。
     */
    @Test
    fun testFallbackToDefaultMappings() {
        // When - 直接调用 getDefaultMappings()
        val defaultMappings = FieldMappingConfig.getDefaultMappings()

        // Then
        assertNotNull("默认配置不应为 null", defaultMappings)
        assertTrue("默认配置不应为空", defaultMappings.isNotEmpty())

        // 验证默认配置包含所有必需的字段
        val expectedFields = setOf(
            "replySuggestion",
            "strategyAnalysis",
            "riskLevel",
            "isSafe",
            "triggeredRisks",
            "suggestion",
            "facts",
            "redTags",
            "greenTags"
        )

        assertEquals("默认配置应该包含所有必需的字段映射", expectedFields, defaultMappings.keys)

        println("✅ 默认配置测试通过")
        println("  默认配置字段数量: ${defaultMappings.size}")
    }

    /**
     * 测试配置文件与默认配置的一致性
     *
     * 验证：
     * - 配置文件加载的结果与默认配置包含相同的字段
     * - 配置文件的内容应该与默认配置一致或更丰富
     */
    @Test
    fun testConfigurationConsistencyWithDefaults() {
        // When
        val loadedMappings = FieldMappingConfig.load(context)
        val defaultMappings = FieldMappingConfig.getDefaultMappings()

        // Then
        assertEquals(
            "配置文件和默认配置应该包含相同的字段",
            defaultMappings.keys,
            loadedMappings.keys
        )

        // 验证每个字段的映射数量
        loadedMappings.forEach { (field, loadedChineseNames) ->
            val defaultChineseNames = defaultMappings[field]
            assertNotNull("默认配置应该包含字段 $field", defaultChineseNames)

            // 配置文件的映射数量应该 >= 默认配置的映射数量
            assertTrue(
                "字段 $field 的配置文件映射数量 (${loadedChineseNames.size}) " +
                        "应该 >= 默认配置映射数量 (${defaultChineseNames!!.size})",
                loadedChineseNames.size >= defaultChineseNames.size
            )
        }

        println("✅ 配置一致性测试通过")
        println("  配置文件字段数量: ${loadedMappings.size}")
        println("  默认配置字段数量: ${defaultMappings.size}")
    }
}
