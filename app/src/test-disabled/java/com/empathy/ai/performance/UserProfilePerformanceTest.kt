package com.empathy.ai.performance

import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.model.UserProfileDimension
import com.empathy.ai.domain.util.UserProfileValidator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * 用户画像性能测试
 *
 * 验证各项操作的性能指标是否达标。
 */
class UserProfilePerformanceTest {

    private lateinit var validator: UserProfileValidator

    @Before
    fun setup() {
        validator = UserProfileValidator()
    }

    // ========== 验证性能测试 ==========

    @Test
    fun `标签验证响应时间应小于10ms`() {
        // Given
        val tag = "测试标签"
        val iterations = 1000

        // When
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                validator.validateTag(tag)
            }
        }

        // Then
        val avgTime = totalTime.toDouble() / iterations
        assertTrue("平均验证时间应小于10ms，实际: ${avgTime}ms", avgTime < 10)
        println("标签验证平均时间: ${avgTime}ms")
    }

    @Test
    fun `维度名称验证响应时间应小于10ms`() {
        // Given
        val dimensionName = "自定义维度"
        val iterations = 1000

        // When
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                validator.validateDimensionName(dimensionName)
            }
        }

        // Then
        val avgTime = totalTime.toDouble() / iterations
        assertTrue("平均验证时间应小于10ms，实际: ${avgTime}ms", avgTime < 10)
        println("维度名称验证平均时间: ${avgTime}ms")
    }

    @Test
    fun `重复检查响应时间应小于10ms`() {
        // Given
        val existingTags = (1..20).map { "标签$it" }
        val newTag = "新标签"
        val iterations = 1000

        // When
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                validator.validateTagNotDuplicate("PERSONALITY_TRAITS", newTag, existingTags)
            }
        }

        // Then
        val avgTime = totalTime.toDouble() / iterations
        assertTrue("平均检查时间应小于10ms，实际: ${avgTime}ms", avgTime < 10)
        println("重复检查平均时间: ${avgTime}ms")
    }

    @Test
    fun `输入清理响应时间应小于10ms`() {
        // Given
        val input = "测试<script>alert('xss')</script>标签"
        val iterations = 1000

        // When
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                validator.sanitizeInput(input)
            }
        }

        // Then
        val avgTime = totalTime.toDouble() / iterations
        assertTrue("平均清理时间应小于10ms，实际: ${avgTime}ms", avgTime < 10)
        println("输入清理平均时间: ${avgTime}ms")
    }

    // ========== 模型操作性能测试 ==========

    @Test
    fun `画像完整度计算响应时间应小于50ms`() {
        // Given
        val profile = createLargeProfile()
        val iterations = 1000

        // When
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                profile.getCompleteness()
            }
        }

        // Then
        val avgTime = totalTime.toDouble() / iterations
        assertTrue("平均计算时间应小于50ms，实际: ${avgTime}ms", avgTime < 50)
        println("完整度计算平均时间: ${avgTime}ms")
    }

    @Test
    fun `标签总数计算响应时间应小于50ms`() {
        // Given
        val profile = createLargeProfile()
        val iterations = 1000

        // When
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                profile.getTotalTagCount()
            }
        }

        // Then
        val avgTime = totalTime.toDouble() / iterations
        assertTrue("平均计算时间应小于50ms，实际: ${avgTime}ms", avgTime < 50)
        println("标签总数计算平均时间: ${avgTime}ms")
    }

    @Test
    fun `获取维度标签响应时间应小于10ms`() {
        // Given
        val profile = createLargeProfile()
        val iterations = 1000

        // When
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                UserProfileDimension.entries.forEach { dimension ->
                    profile.getTagsForDimension(dimension.name)
                }
            }
        }

        // Then
        val avgTime = totalTime.toDouble() / iterations
        assertTrue("平均获取时间应小于10ms，实际: ${avgTime}ms", avgTime < 10)
        println("获取维度标签平均时间: ${avgTime}ms")
    }

    @Test
    fun `isEmpty检查响应时间应小于10ms`() {
        // Given
        val profile = createLargeProfile()
        val iterations = 1000

        // When
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                profile.isEmpty()
            }
        }

        // Then
        val avgTime = totalTime.toDouble() / iterations
        assertTrue("平均检查时间应小于10ms，实际: ${avgTime}ms", avgTime < 10)
        println("isEmpty检查平均时间: ${avgTime}ms")
    }

    // ========== 上下文构建性能测试 ==========
    // 注意：UserProfileContextBuilder需要依赖注入，这些测试移至集成测试

    // ========== 内存占用测试 ==========

    @Test
    fun `大型画像内存占用应小于5MB`() {
        // Given
        val runtime = Runtime.getRuntime()
        runtime.gc()
        val beforeMemory = runtime.totalMemory() - runtime.freeMemory()

        // When - 创建多个大型画像
        val profiles = (1..100).map { createLargeProfile() }

        runtime.gc()
        val afterMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = (afterMemory - beforeMemory) / (1024 * 1024) // MB

        // Then
        assertTrue("内存占用应小于5MB，实际: ${memoryUsed}MB", memoryUsed < 5)
        println("100个大型画像内存占用: ${memoryUsed}MB")

        // 防止profiles被优化掉
        assertTrue(profiles.isNotEmpty())
    }

    @Test
    fun `画像序列化大小应合理`() {
        // Given
        val profile = createLargeProfile()

        // When - 估算序列化大小
        val estimatedSize = estimateProfileSize(profile)

        // Then
        assertTrue("单个画像序列化大小应小于50KB，实际: ${estimatedSize}bytes", estimatedSize < 50 * 1024)
        println("画像估算序列化大小: ${estimatedSize}bytes")
    }

    // ========== 批量操作性能测试 ==========

    @Test
    fun `批量添加标签性能应可接受`() {
        // Given
        var profile = UserProfile()
        val tagsToAdd = (1..20).map { "标签$it" }
        val dimension = "PERSONALITY_TRAITS"

        // When
        val totalTime = measureTimeMillis {
            tagsToAdd.forEach { tag ->
                val currentTags = profile.getTagsForDimension(dimension)
                profile = profile.copy(
                    personalityTraits = currentTags + tag
                )
            }
        }

        // Then
        assertTrue("批量添加20个标签应小于100ms，实际: ${totalTime}ms", totalTime < 100)
        println("批量添加20个标签时间: ${totalTime}ms")
    }

    @Test
    fun `批量删除标签性能应可接受`() {
        // Given
        val initialTags = (1..20).map { "标签$it" }
        var profile = UserProfile(personalityTraits = initialTags)

        // When
        val totalTime = measureTimeMillis {
            initialTags.forEach { tag ->
                val currentTags = profile.personalityTraits
                profile = profile.copy(
                    personalityTraits = currentTags - tag
                )
            }
        }

        // Then
        assertTrue("批量删除20个标签应小于100ms，实际: ${totalTime}ms", totalTime < 100)
        println("批量删除20个标签时间: ${totalTime}ms")
    }

    // ========== 辅助方法 ==========

    private fun createLargeProfile(): UserProfile {
        return UserProfile(
            personalityTraits = (1..20).map { "性格特点$it" },
            values = (1..20).map { "价值观$it" },
            interests = (1..20).map { "兴趣爱好$it" },
            communicationStyle = (1..20).map { "沟通风格$it" },
            socialPreferences = (1..20).map { "社交偏好$it" },
            customDimensions = (1..10).associate { dimIndex ->
                "自定义维度$dimIndex" to (1..20).map { "标签${dimIndex}_$it" }
            }
        )
    }

    private fun estimateProfileSize(profile: UserProfile): Int {
        var size = 0
        
        // 基础维度
        size += profile.personalityTraits.sumOf { it.length * 2 } // UTF-16
        size += profile.values.sumOf { it.length * 2 }
        size += profile.interests.sumOf { it.length * 2 }
        size += profile.communicationStyle.sumOf { it.length * 2 }
        size += profile.socialPreferences.sumOf { it.length * 2 }
        
        // 自定义维度
        profile.customDimensions.forEach { (name, tags) ->
            size += name.length * 2
            size += tags.sumOf { it.length * 2 }
        }
        
        // 对象开销估算
        size += 100 // 基础对象开销
        
        return size
    }
}
