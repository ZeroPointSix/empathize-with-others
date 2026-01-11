package com.empathy.ai.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.empathy.ai.data.local.ApiKeyStorage
import com.empathy.ai.data.local.AppDatabase
import com.empathy.ai.data.local.ProxyPreferences
import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * AiProviderRepository 测试
 *
 * **Feature: flexible-ai-config, Property 3: 服务商保存往返一致性**
 * **Feature: flexible-ai-config, Property 4: 模型列表完整性**
 * **Validates: Requirements 1.3, 4.1, 4.2, 4.3**
 *
 * 测试服务商保存和读取的一致性，以及模型列表的完整性
 */
@RunWith(AndroidJUnit4::class)
class AiProviderRepositoryPropertyTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var apiKeyStorage: ApiKeyStorage
    private lateinit var proxyPreferences: ProxyPreferences
    private lateinit var moshi: Moshi
    private lateinit var repository: AiProviderRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // 创建内存数据库
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()

        // 创建依赖
        apiKeyStorage = ApiKeyStorage(context)
        proxyPreferences = ProxyPreferences(context)
        moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        // 创建 Repository
        repository = AiProviderRepositoryImpl(
            dao = database.aiProviderDao(),
            apiKeyStorage = apiKeyStorage,
            proxyPreferences = proxyPreferences,
            moshi = moshi
        )
    }

    @After
    fun tearDown() {
        database.close()
        // 清理测试数据
        // Note: ApiKeyStorage 的清理在各个测试中完成
    }

    /**
     * Property 3: 服务商保存往返一致性
     *
     * 对于任意有效的服务商配置，保存后再读取应返回等价的配置
     */
    @Test
    fun property3_provider_save_and_retrieve_should_be_consistent() = runBlocking {
        // Given - 创建多个测试服务商
        val testProviders = listOf(
            createValidProvider("Provider 1", 2),
            createValidProvider("Provider 2", 3),
            createValidProvider("Provider 3", 5)
        )

        testProviders.forEach { provider ->
            try {
                // When - 保存服务商
                val saveResult = repository.saveProvider(provider)
                assertTrue("Save should succeed", saveResult.isSuccess)

                // Then - 读取服务商
                val retrieveResult = repository.getProvider(provider.id)
                assertTrue("Retrieve should succeed", retrieveResult.isSuccess)

                val retrieved = retrieveResult.getOrNull()
                assertNotNull("Retrieved provider should not be null", retrieved)

                // 验证往返一致性
                assertEquals("ID should match", provider.id, retrieved?.id)
                assertEquals("Name should match", provider.name, retrieved?.name)
                assertEquals("BaseUrl should match", provider.baseUrl, retrieved?.baseUrl)
                assertEquals("ApiKey should match", provider.apiKey, retrieved?.apiKey)
                assertEquals("DefaultModelId should match", provider.defaultModelId, retrieved?.defaultModelId)
                assertEquals("IsDefault should match", provider.isDefault, retrieved?.isDefault)
                assertEquals("Models size should match", provider.models.size, retrieved?.models?.size)

                // 验证模型列表
                provider.models.forEachIndexed { index, model ->
                    assertEquals("Model[$index].id should match", model.id, retrieved?.models?.get(index)?.id)
                    assertEquals("Model[$index].displayName should match", model.displayName, retrieved?.models?.get(index)?.displayName)
                }
            } finally {
                // 清理测试数据
                repository.deleteProvider(provider.id)
            }
        }
    }

    /**
     * Property 4: 模型列表完整性
     *
     * 对于任意服务商配置，添加的所有模型都应在保存后能够正确读取
     */
    @Test
    fun property4_all_models_should_be_preserved_after_save() = runBlocking {
        // Given - 创建带不同数量模型的服务商
        val testProviders = listOf(
            createValidProvider("Provider with 2 models", 2),
            createValidProvider("Provider with 5 models", 5),
            createValidProvider("Provider with 10 models", 10)
        )

        testProviders.forEach { provider ->
            try {
                // When - 保存服务商
                val saveResult = repository.saveProvider(provider)
                assertTrue("Save should succeed", saveResult.isSuccess)

                // Then - 读取服务商
                val retrieveResult = repository.getProvider(provider.id)
                assertTrue("Retrieve should succeed", retrieveResult.isSuccess)

                val retrieved = retrieveResult.getOrNull()
                assertNotNull("Retrieved provider should not be null", retrieved)

                // 验证模型列表完整性
                assertEquals("Models count should match", provider.models.size, retrieved?.models?.size)

                // 验证每个模型都存在
                provider.models.forEach { originalModel ->
                    val foundModel = retrieved?.models?.find { it.id == originalModel.id }
                    assertNotNull("Model ${originalModel.id} should exist", foundModel)
                    assertEquals("Model displayName should match", originalModel.displayName, foundModel?.displayName)
                }

                // 验证默认模型存在
                val defaultModel = retrieved?.models?.find { it.id == provider.defaultModelId }
                assertNotNull("Default model should exist in models list", defaultModel)
            } finally {
                // 清理测试数据
                repository.deleteProvider(provider.id)
            }
        }
    }

    /**
     * 测试：删除服务商应同时删除 API Key
     */
    @Test
    fun delete_provider_should_also_delete_api_key() = runBlocking {
        // Given - 创建并保存服务商
        val provider = createValidProvider("Test Provider", 2)
        repository.saveProvider(provider)

        // When - 删除服务商
        val deleteResult = repository.deleteProvider(provider.id)
        assertTrue("Delete should succeed", deleteResult.isSuccess)

        // Then - 验证数据库记录已删除
        val retrieveResult = repository.getProvider(provider.id)
        assertTrue("Retrieve should succeed", retrieveResult.isSuccess)
        assertNull("Provider should be null after delete", retrieveResult.getOrNull())

        // 验证 API Key 已删除
        val apiKeyRef = apiKeyStorage.generateKey(provider.id)
        val apiKey = apiKeyStorage.get(apiKeyRef)
        assertNull("API Key should be deleted", apiKey)
    }

    /**
     * 测试：设置默认服务商应清除其他默认标记
     */
    @Test
    fun set_default_provider_should_clear_other_default_flags() = runBlocking {
        // Given - 创建两个服务商
        val provider1 = createValidProvider("Provider 1", 2)
        val provider2 = createValidProvider("Provider 2", 2)

        repository.saveProvider(provider1)
        repository.saveProvider(provider2)

        // When - 设置 provider1 为默认
        repository.setDefaultProvider(provider1.id)

        // Then - 验证只有 provider1 是默认
        val allProviders = repository.getAllProviders().first()
        val defaultProviders = allProviders.filter { it.isDefault }
        assertEquals("Should have exactly one default provider", 1, defaultProviders.size)
        assertEquals("Provider1 should be default", provider1.id, defaultProviders[0].id)

        // When - 设置 provider2 为默认
        repository.setDefaultProvider(provider2.id)

        // Then - 验证只有 provider2 是默认
        val allProviders2 = repository.getAllProviders().first()
        val defaultProviders2 = allProviders2.filter { it.isDefault }
        assertEquals("Should have exactly one default provider", 1, defaultProviders2.size)
        assertEquals("Provider2 should be default", provider2.id, defaultProviders2[0].id)

        // 清理
        repository.deleteProvider(provider1.id)
        repository.deleteProvider(provider2.id)
    }

    // ========== 辅助方法 ==========

    /**
     * 创建有效的服务商（用于测试）
     *
     * @param name 服务商名称
     * @param modelCount 模型数量
     */
    private fun createValidProvider(name: String, modelCount: Int): AiProvider {
        val models = (1..modelCount).map { index ->
            AiModel("model-$index", "Model $index")
        }
        return AiProvider(
            id = UUID.randomUUID().toString(),
            name = name,
            baseUrl = "https://api.test.com/v1",
            apiKey = "sk-test-${UUID.randomUUID()}",
            models = models,
            defaultModelId = models[0].id,
            isDefault = false
        )
    }
}
