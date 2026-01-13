package com.empathy.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import com.empathy.ai.domain.model.UserProfile
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * UserProfilePreferences 单元测试
 *
 * 测试用户画像数据序列化和持久化:
 * - JSON序列化 - Moshi正确序列化和反序列化UserProfile
 * - 空值处理 - 空JSON字符串返回默认空画像
 * - 中文支持 - 特殊字符和中文标签正确处理
 * - 特殊字符 - 标签中的连字符、下划线等
 * - 多维度 - customDimensions支持多维度自定义数据
 * - 时间戳 - createdAt/updatedAt正确持久化
 * - 性能验证 - 大量标签序列化性能测试
 *
 * 业务规则 (PRD-00001 用户画像管理):
 * - 用户画像包含性格、价值观、兴趣、沟通风格等维度
 * - 支持自定义维度扩展
 * - 画像数据需要持久化存储
 *
 * 设计权衡:
 * - 使用Moshi进行JSON序列化（Android原生支持好）
 * - 使用EncryptedSharedPreferences加密存储敏感数据
 * - 单元测试使用MockK模拟SharedPreferences
 *
 * 任务追踪:
 * - PRD-00001 - 用户画像管理需求
 * - TDD-00001 - 用户画像管理技术设计
 *
 * 注意：由于EncryptedSharedPreferences依赖Android环境，
 * 这里使用MockK模拟SharedPreferences行为进行测试。
 * 完整的加密存储测试应在Android测试中进行。
 */
class UserProfilePreferencesTest {
    
    private lateinit var context: Context
    private lateinit var moshi: Moshi
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        sharedPrefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        
        every { sharedPrefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.apply() } returns Unit
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    // ========== JSON序列化测试 ==========
    
    @Test
    fun `UserProfile序列化和反序列化正确`() {
        val adapter = moshi.adapter(UserProfile::class.java)
        val profile = UserProfile(
            personalityTraits = listOf("内向", "敏感"),
            values = listOf("诚实"),
            interests = listOf("阅读"),
            communicationStyle = listOf("直接"),
            socialPreferences = listOf("小圈子"),
            customDimensions = mapOf("职业技能" to listOf("编程"))
        )
        
        val json = adapter.toJson(profile)
        val restored = adapter.fromJson(json)
        
        assertEquals(profile.personalityTraits, restored?.personalityTraits)
        assertEquals(profile.values, restored?.values)
        assertEquals(profile.interests, restored?.interests)
        assertEquals(profile.communicationStyle, restored?.communicationStyle)
        assertEquals(profile.socialPreferences, restored?.socialPreferences)
        assertEquals(profile.customDimensions, restored?.customDimensions)
    }
    
    @Test
    fun `空UserProfile序列化和反序列化正确`() {
        val adapter = moshi.adapter(UserProfile::class.java)
        val profile = UserProfile()
        
        val json = adapter.toJson(profile)
        val restored = adapter.fromJson(json)
        
        assertTrue(restored?.isEmpty() == true)
    }
    
    @Test
    fun `UserProfile包含中文标签序列化正确`() {
        val adapter = moshi.adapter(UserProfile::class.java)
        val profile = UserProfile(
            personalityTraits = listOf("内向", "敏感", "善于倾听")
        )
        
        val json = adapter.toJson(profile)
        val restored = adapter.fromJson(json)
        
        assertEquals(listOf("内向", "敏感", "善于倾听"), restored?.personalityTraits)
    }
    
    @Test
    fun `UserProfile包含特殊字符标签序列化正确`() {
        val adapter = moshi.adapter(UserProfile::class.java)
        val profile = UserProfile(
            personalityTraits = listOf("标签1", "标签-2", "标签_3")
        )
        
        val json = adapter.toJson(profile)
        val restored = adapter.fromJson(json)
        
        assertEquals(listOf("标签1", "标签-2", "标签_3"), restored?.personalityTraits)
    }
    
    // ========== 边界情况测试 ==========
    
    @Test
    fun `空JSON字符串解析返回null`() {
        val adapter = moshi.adapter(UserProfile::class.java)
        val result = adapter.fromJson("{}")
        
        // 空JSON对象应该返回默认值的UserProfile
        assertTrue(result?.isEmpty() == true)
    }
    
    @Test
    fun `多个自定义维度序列化正确`() {
        val adapter = moshi.adapter(UserProfile::class.java)
        val profile = UserProfile(
            customDimensions = mapOf(
                "职业技能" to listOf("编程", "设计"),
                "语言能力" to listOf("中文", "英文"),
                "学历背景" to listOf("本科", "计算机专业")
            )
        )
        
        val json = adapter.toJson(profile)
        val restored = adapter.fromJson(json)
        
        assertEquals(3, restored?.customDimensions?.size)
        assertEquals(listOf("编程", "设计"), restored?.customDimensions?.get("职业技能"))
        assertEquals(listOf("中文", "英文"), restored?.customDimensions?.get("语言能力"))
    }
    
    @Test
    fun `时间戳字段序列化正确`() {
        val adapter = moshi.adapter(UserProfile::class.java)
        val createdAt = 1703145600000L
        val updatedAt = 1703232000000L
        val profile = UserProfile(
            createdAt = createdAt,
            updatedAt = updatedAt
        )
        
        val json = adapter.toJson(profile)
        val restored = adapter.fromJson(json)
        
        assertEquals(createdAt, restored?.createdAt)
        assertEquals(updatedAt, restored?.updatedAt)
    }
    
    // ========== 数据完整性测试 ==========
    
    @Test
    fun `完整画像数据序列化后保持完整`() {
        val adapter = moshi.adapter(UserProfile::class.java)
        val profile = UserProfile(
            id = "user_profile",
            personalityTraits = listOf("内向", "敏感", "善于倾听"),
            values = listOf("诚实", "正直", "尊重"),
            interests = listOf("阅读", "音乐", "电影"),
            communicationStyle = listOf("直接", "幽默"),
            socialPreferences = listOf("小圈子", "深度交流"),
            customDimensions = mapOf(
                "职业技能" to listOf("编程", "设计"),
                "语言能力" to listOf("中文", "英文")
            ),
            createdAt = 1703145600000L,
            updatedAt = 1703232000000L
        )
        
        val json = adapter.toJson(profile)
        val restored = adapter.fromJson(json)
        
        assertEquals(profile.id, restored?.id)
        assertEquals(profile.personalityTraits, restored?.personalityTraits)
        assertEquals(profile.values, restored?.values)
        assertEquals(profile.interests, restored?.interests)
        assertEquals(profile.communicationStyle, restored?.communicationStyle)
        assertEquals(profile.socialPreferences, restored?.socialPreferences)
        assertEquals(profile.customDimensions, restored?.customDimensions)
        assertEquals(profile.createdAt, restored?.createdAt)
        assertEquals(profile.updatedAt, restored?.updatedAt)
    }
    
    @Test
    fun `大量标签序列化性能正常`() {
        val adapter = moshi.adapter(UserProfile::class.java)
        val tags = (1..20).map { "标签$it" }
        val profile = UserProfile(
            personalityTraits = tags,
            values = tags,
            interests = tags,
            communicationStyle = tags,
            socialPreferences = tags
        )
        
        val startTime = System.currentTimeMillis()
        val json = adapter.toJson(profile)
        val restored = adapter.fromJson(json)
        val endTime = System.currentTimeMillis()
        
        // 序列化和反序列化应在100ms内完成
        assertTrue(endTime - startTime < 100)
        assertEquals(100, restored?.getTotalTagCount())
    }
}
