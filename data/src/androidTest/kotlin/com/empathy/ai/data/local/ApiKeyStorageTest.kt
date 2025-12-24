package com.empathy.ai.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ApiKeyStorage 仪器测试
 *
 * 测试 API Key 的加密存储和读取功能。
 * 由于 EncryptedSharedPreferences 依赖 Android 框架，需要在真实设备或模拟器上运行。
 *
 * 测试覆盖:
 * - 保存和读取 API Key
 * - 删除 API Key
 * - API Key 脱敏显示
 */
@RunWith(AndroidJUnit4::class)
class ApiKeyStorageTest {

    private lateinit var context: Context
    private lateinit var apiKeyStorage: ApiKeyStorage

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        apiKeyStorage = ApiKeyStorage(context)
    }

    @After
    fun tearDown() {
        // 清理测试数据
        apiKeyStorage.delete("test_key_1")
        apiKeyStorage.delete("test_key_2")
        apiKeyStorage.delete(apiKeyStorage.generateKey("test_provider_1"))
    }

    @Test
    fun save_and_get_should_work_correctly() {
        // Given
        val key = "test_key_1"
        val apiKey = "sk-1234567890abcdef"

        // When
        apiKeyStorage.save(key, apiKey)
        val retrieved = apiKeyStorage.get(key)

        // Then
        assertEquals(apiKey, retrieved)
    }

    @Test
    fun get_should_return_null_for_non_existent_key() {
        // Given
        val key = "non_existent_key"

        // When
        val retrieved = apiKeyStorage.get(key)

        // Then
        assertNull(retrieved)
    }

    @Test
    fun delete_should_remove_api_key() {
        // Given
        val key = "test_key_2"
        val apiKey = "sk-test-api-key"
        apiKeyStorage.save(key, apiKey)

        // When
        apiKeyStorage.delete(key)
        val retrieved = apiKeyStorage.get(key)

        // Then
        assertNull(retrieved)
    }

    @Test
    fun mask_should_hide_middle_characters_for_long_keys() {
        // Given
        val apiKey = "sk-1234567890abcdef"

        // When
        val masked = apiKeyStorage.mask(apiKey)

        // Then
        assertEquals("sk-1****cdef", masked)
    }

    @Test
    fun mask_should_return_asterisks_for_short_keys() {
        // Given
        val apiKey = "short"

        // When
        val masked = apiKeyStorage.mask(apiKey)

        // Then
        assertEquals("****", masked)
    }

    @Test
    fun mask_should_handle_exactly_8_characters() {
        // Given
        val apiKey = "12345678"

        // When
        val masked = apiKeyStorage.mask(apiKey)

        // Then
        assertEquals("****", masked)
    }

    @Test
    fun mask_should_handle_9_characters() {
        // Given
        val apiKey = "123456789"

        // When
        val masked = apiKeyStorage.mask(apiKey)

        // Then
        assertEquals("1234****6789", masked)
    }

    @Test
    fun generateKey_should_create_correct_format() {
        // Given
        val providerId = "test_provider_1"

        // When
        val key = apiKeyStorage.generateKey(providerId)

        // Then
        assertEquals("api_key_test_provider_1", key)
    }

    @Test
    fun save_should_overwrite_existing_key() {
        // Given
        val key = "test_key_1"
        val apiKey1 = "sk-old-key"
        val apiKey2 = "sk-new-key"

        // When
        apiKeyStorage.save(key, apiKey1)
        apiKeyStorage.save(key, apiKey2)
        val retrieved = apiKeyStorage.get(key)

        // Then
        assertEquals(apiKey2, retrieved)
    }

    @Test
    fun encryption_should_work_across_multiple_keys() {
        // Given
        val key1 = "test_key_1"
        val key2 = "test_key_2"
        val apiKey1 = "sk-provider-1-key"
        val apiKey2 = "sk-provider-2-key"

        // When
        apiKeyStorage.save(key1, apiKey1)
        apiKeyStorage.save(key2, apiKey2)
        val retrieved1 = apiKeyStorage.get(key1)
        val retrieved2 = apiKeyStorage.get(key2)

        // Then
        assertEquals(apiKey1, retrieved1)
        assertEquals(apiKey2, retrieved2)
    }
}
