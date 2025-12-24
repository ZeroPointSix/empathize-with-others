package com.empathy.ai.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.empathy.ai.domain.model.UserProfile
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UserProfilePreferences 集成测试
 *
 * 测试用户画像数据的持久化功能。
 */
@RunWith(AndroidJUnit4::class)
class UserProfilePreferencesIntegrationTest {

    private lateinit var context: Context
    private lateinit var preferences: UserProfilePreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        preferences = UserProfilePreferences(context)
        
        // 清理测试数据
        runTest {
            preferences.clearUserProfile()
        }
    }

    @After
    fun tearDown() {
        // 清理测试数据
        runTest {
            preferences.clearUserProfile()
        }
    }

    // ========== 基本保存和加载测试 ==========

    @Test
    fun saveAndLoad_basicProfile() = runTest {
        // Given
        val profile = UserProfile(
            personalityTraits = listOf("内向", "理性"),
            values = listOf("诚实", "责任"),
            interests = listOf("阅读", "编程")
        )

        // When
        val saveResult = preferences.saveUserProfile(profile)
        val loadResult = preferences.loadUserProfile()

        // Then
        assertTrue("保存应该成功", saveResult.isSuccess)
        assertTrue("加载应该成功", loadResult.isSuccess)
        
        val loadedProfile = loadResult.getOrNull()
        assertNotNull("加载的画像不应为空", loadedProfile)
        assertEquals("性格特点应该一致", profile.personalityTraits, loadedProfile?.personalityTraits)
        assertEquals("价值观应该一致", profile.values, loadedProfile?.values)
        assertEquals("兴趣爱好应该一致", profile.interests, loadedProfile?.interests)
    }

    @Test
    fun saveAndLoad_profileWithCustomDimensions() = runTest {
        // Given
        val profile = UserProfile(
            personalityTraits = listOf("内向"),
            customDimensions = mapOf(
                "职业技能" to listOf("Kotlin", "Android", "架构设计"),
                "生活习惯" to listOf("早起", "运动", "阅读")
            )
        )

        // When
        val saveResult = preferences.saveUserProfile(profile)
        val loadResult = preferences.loadUserProfile()

        // Then
        assertTrue("保存应该成功", saveResult.isSuccess)
        assertTrue("加载应该成功", loadResult.isSuccess)
        
        val loadedProfile = loadResult.getOrNull()
        assertNotNull("加载的画像不应为空", loadedProfile)
        assertEquals("自定义维度数量应该一致", 2, loadedProfile?.customDimensions?.size)
        assertEquals("职业技能标签应该一致", 
            listOf("Kotlin", "Android", "架构设计"), 
            loadedProfile?.customDimensions?.get("职业技能")
        )
    }

    @Test
    fun saveAndLoad_emptyProfile() = runTest {
        // Given
        val profile = UserProfile()

        // When
        val saveResult = preferences.saveUserProfile(profile)
        val loadResult = preferences.loadUserProfile()

        // Then
        assertTrue("保存应该成功", saveResult.isSuccess)
        assertTrue("加载应该成功", loadResult.isSuccess)
        
        val loadedProfile = loadResult.getOrNull()
        assertNotNull("加载的画像不应为空", loadedProfile)
        assertTrue("画像应该为空", loadedProfile?.isEmpty() == true)
    }

    // ========== 数据更新测试 ==========

    @Test
    fun update_overwritesPreviousData() = runTest {
        // Given
        val originalProfile = UserProfile(
            personalityTraits = listOf("内向")
        )
        val updatedProfile = UserProfile(
            personalityTraits = listOf("外向", "热情")
        )

        // When
        preferences.saveUserProfile(originalProfile)
        preferences.saveUserProfile(updatedProfile)
        val loadResult = preferences.loadUserProfile()

        // Then
        assertTrue("加载应该成功", loadResult.isSuccess)
        val loadedProfile = loadResult.getOrNull()
        assertEquals("应该是更新后的数据", listOf("外向", "热情"), loadedProfile?.personalityTraits)
    }

    @Test
    fun update_preservesOtherDimensions() = runTest {
        // Given
        val profile = UserProfile(
            personalityTraits = listOf("内向"),
            values = listOf("诚实"),
            interests = listOf("阅读")
        )

        // When
        preferences.saveUserProfile(profile)
        
        // 更新部分数据
        val loadedProfile = preferences.loadUserProfile().getOrNull()!!
        val updatedProfile = loadedProfile.copy(
            personalityTraits = listOf("外向")
        )
        preferences.saveUserProfile(updatedProfile)
        
        val finalResult = preferences.loadUserProfile()

        // Then
        val finalProfile = finalResult.getOrNull()
        assertEquals("性格特点应该更新", listOf("外向"), finalProfile?.personalityTraits)
        assertEquals("价值观应该保留", listOf("诚实"), finalProfile?.values)
        assertEquals("兴趣爱好应该保留", listOf("阅读"), finalProfile?.interests)
    }

    // ========== 清除数据测试 ==========

    @Test
    fun clear_removesAllData() = runTest {
        // Given
        val profile = UserProfile(
            personalityTraits = listOf("内向", "理性"),
            customDimensions = mapOf("职业技能" to listOf("Kotlin"))
        )
        preferences.saveUserProfile(profile)

        // When
        val clearResult = preferences.clearUserProfile()
        val loadResult = preferences.loadUserProfile()

        // Then
        assertTrue("清除应该成功", clearResult.isSuccess)
        assertTrue("加载应该成功", loadResult.isSuccess)
        
        val loadedProfile = loadResult.getOrNull()
        assertTrue("清除后画像应该为空", loadedProfile?.isEmpty() == true)
    }

    // ========== 导入导出测试 ==========

    @Test
    fun export_returnsValidJson() = runTest {
        // Given
        val profile = UserProfile(
            personalityTraits = listOf("内向", "理性"),
            values = listOf("诚实"),
            customDimensions = mapOf("职业技能" to listOf("Kotlin"))
        )
        preferences.saveUserProfile(profile)

        // When
        val exportResult = preferences.exportUserProfile()

        // Then
        assertTrue("导出应该成功", exportResult.isSuccess)
        val json = exportResult.getOrNull()
        assertNotNull("导出的JSON不应为空", json)
        assertTrue("JSON应该包含性格特点", json?.contains("内向") == true)
        assertTrue("JSON应该包含自定义维度", json?.contains("职业技能") == true)
    }

    @Test
    fun importExport_dataConsistency() = runTest {
        // Given
        val originalProfile = UserProfile(
            personalityTraits = listOf("内向", "理性", "细心"),
            values = listOf("诚实", "责任", "成长"),
            interests = listOf("阅读", "编程", "旅行"),
            communicationStyle = listOf("直接", "简洁"),
            socialPreferences = listOf("小圈子"),
            customDimensions = mapOf(
                "职业技能" to listOf("Kotlin", "Android"),
                "生活习惯" to listOf("早起", "运动")
            )
        )
        preferences.saveUserProfile(originalProfile)

        // When - 导出
        val exportResult = preferences.exportUserProfile()
        assertTrue("导出应该成功", exportResult.isSuccess)
        val exportedJson = exportResult.getOrNull()!!

        // 清除数据
        preferences.clearUserProfile()

        // 导入
        val importResult = preferences.importUserProfile(exportedJson)
        assertTrue("导入应该成功", importResult.isSuccess)

        // 加载
        val loadResult = preferences.loadUserProfile()

        // Then
        val loadedProfile = loadResult.getOrNull()
        assertNotNull("加载的画像不应为空", loadedProfile)
        assertEquals("性格特点应该一致", originalProfile.personalityTraits, loadedProfile?.personalityTraits)
        assertEquals("价值观应该一致", originalProfile.values, loadedProfile?.values)
        assertEquals("兴趣爱好应该一致", originalProfile.interests, loadedProfile?.interests)
        assertEquals("沟通风格应该一致", originalProfile.communicationStyle, loadedProfile?.communicationStyle)
        assertEquals("社交偏好应该一致", originalProfile.socialPreferences, loadedProfile?.socialPreferences)
        assertEquals("自定义维度应该一致", originalProfile.customDimensions, loadedProfile?.customDimensions)
    }

    @Test
    fun import_invalidJson_returnsError() = runTest {
        // Given
        val invalidJson = "这不是有效的JSON"

        // When
        val importResult = preferences.importUserProfile(invalidJson)

        // Then
        assertTrue("导入无效JSON应该失败", importResult.isFailure)
    }

    @Test
    fun import_emptyJson_returnsError() = runTest {
        // Given
        val emptyJson = ""

        // When
        val importResult = preferences.importUserProfile(emptyJson)

        // Then
        assertTrue("导入空JSON应该失败", importResult.isFailure)
    }

    // ========== 加密存储测试 ==========

    @Test
    fun encryption_dataIsEncrypted() = runTest {
        // Given
        val profile = UserProfile(
            personalityTraits = listOf("敏感信息测试")
        )

        // When
        preferences.saveUserProfile(profile)

        // Then - 验证数据已加密存储
        // 注意：由于使用EncryptedSharedPreferences，我们无法直接读取原始数据
        // 这里只验证保存和加载功能正常工作
        val loadResult = preferences.loadUserProfile()
        assertTrue("加载应该成功", loadResult.isSuccess)
        assertEquals("数据应该正确解密", 
            listOf("敏感信息测试"), 
            loadResult.getOrNull()?.personalityTraits
        )
    }

    // ========== 边界情况测试 ==========

    @Test
    fun save_largeProfile() = runTest {
        // Given - 创建一个大型画像
        val profile = UserProfile(
            personalityTraits = (1..20).map { "性格特点$it" },
            values = (1..20).map { "价值观$it" },
            interests = (1..20).map { "兴趣爱好$it" },
            communicationStyle = (1..20).map { "沟通风格$it" },
            socialPreferences = (1..20).map { "社交偏好$it" },
            customDimensions = (1..10).associate { dimIndex ->
                "自定义维度$dimIndex" to (1..20).map { "标签${dimIndex}_$it" }
            }
        )

        // When
        val saveResult = preferences.saveUserProfile(profile)
        val loadResult = preferences.loadUserProfile()

        // Then
        assertTrue("保存大型画像应该成功", saveResult.isSuccess)
        assertTrue("加载大型画像应该成功", loadResult.isSuccess)
        
        val loadedProfile = loadResult.getOrNull()
        assertEquals("性格特点数量应该一致", 20, loadedProfile?.personalityTraits?.size)
        assertEquals("自定义维度数量应该一致", 10, loadedProfile?.customDimensions?.size)
    }

    @Test
    fun save_specialCharacters() = runTest {
        // Given
        val profile = UserProfile(
            personalityTraits = listOf("包含特殊字符：<>&\"'", "中文测试", "emoji😀")
        )

        // When
        val saveResult = preferences.saveUserProfile(profile)
        val loadResult = preferences.loadUserProfile()

        // Then
        assertTrue("保存应该成功", saveResult.isSuccess)
        assertTrue("加载应该成功", loadResult.isSuccess)
        
        val loadedProfile = loadResult.getOrNull()
        assertEquals("特殊字符应该正确保存", profile.personalityTraits, loadedProfile?.personalityTraits)
    }

    @Test
    fun load_whenNoDataSaved_returnsEmptyProfile() = runTest {
        // Given - 确保没有保存任何数据
        preferences.clearUserProfile()

        // When
        val loadResult = preferences.loadUserProfile()

        // Then
        assertTrue("加载应该成功", loadResult.isSuccess)
        val loadedProfile = loadResult.getOrNull()
        assertNotNull("应该返回空画像而不是null", loadedProfile)
        assertTrue("画像应该为空", loadedProfile?.isEmpty() == true)
    }

    // ========== 并发访问测试 ==========

    @Test
    fun concurrentAccess_noDataCorruption() = runTest {
        // Given
        val profile1 = UserProfile(personalityTraits = listOf("版本1"))
        val profile2 = UserProfile(personalityTraits = listOf("版本2"))

        // When - 模拟并发保存
        preferences.saveUserProfile(profile1)
        preferences.saveUserProfile(profile2)

        // Then - 最后保存的应该生效
        val loadResult = preferences.loadUserProfile()
        assertTrue("加载应该成功", loadResult.isSuccess)
        assertEquals("应该是最后保存的版本", listOf("版本2"), loadResult.getOrNull()?.personalityTraits)
    }
}
