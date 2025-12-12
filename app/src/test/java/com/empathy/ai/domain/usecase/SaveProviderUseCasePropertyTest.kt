package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.repository.AiProviderRepository
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * SaveProviderUseCase 属性测试
 *
 * **Feature: flexible-ai-config, Property 1: 配置验证完整性**
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4**
 *
 * 测试空字段验证和 URL 格式验证
 */
class SaveProviderUseCasePropertyTest {

    private lateinit var repository: AiProviderRepository
    private lateinit var useCase: SaveProviderUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = SaveProviderUseCase(repository)
    }

    /**
     * 属性测试：空字段应被拒绝
     *
     * 对于任意服务商配置，如果名称、API 端点或 API Key 为空，则保存操作应被拒绝
     */
    @Test
    fun `property - empty fields should be rejected`() = runTest {
        checkAll(100, providerArb()) { provider ->
            // Mock repository to always succeed
            coEvery { repository.saveProvider(any()) } returns Result.success(Unit)

            // When
            val result = useCase(provider)

            // Then - 如果任何必填字段为空，应该失败
            if (provider.name.isBlank() || 
                provider.baseUrl.isBlank() || 
                provider.apiKey.isBlank() ||
                provider.models.isEmpty()) {
                assertFalse(
                    "Provider with empty fields should be rejected: name='${provider.name}', " +
                    "baseUrl='${provider.baseUrl}', apiKey='${provider.apiKey}', " +
                    "models=${provider.models.size}",
                    result.isSuccess
                )
            }
        }
    }

    /**
     * 属性测试：无效的 URL 格式应被拒绝
     *
     * 对于任意字符串作为 API 端点输入，验证函数应正确识别有效的 HTTP/HTTPS URL 格式
     */
    @Test
    fun `property - invalid URL format should be rejected`() = runTest {
        checkAll(100, providerArb()) { provider ->
            // Mock repository to always succeed
            coEvery { repository.saveProvider(any()) } returns Result.success(Unit)

            // When
            val result = useCase(provider)

            // Then - 如果 URL 不以 http:// 或 https:// 开头，应该失败
            if (provider.baseUrl.isNotBlank() && 
                !provider.baseUrl.startsWith("http://") && 
                !provider.baseUrl.startsWith("https://")) {
                assertFalse(
                    "Provider with invalid URL should be rejected: baseUrl='${provider.baseUrl}'",
                    result.isSuccess
                )
            }
        }
    }

    /**
     * 属性测试：有效配置应被接受
     *
     * 对于任意有效的服务商配置，保存操作应该成功
     */
    @Test
    fun `property - valid configuration should be accepted`() = runTest {
        checkAll(100, validProviderArb()) { provider ->
            // Mock repository to always succeed
            coEvery { repository.saveProvider(any()) } returns Result.success(Unit)

            // When
            val result = useCase(provider)

            // Then - 有效配置应该成功
            assertTrue(
                "Valid provider should be accepted: name='${provider.name}', " +
                "baseUrl='${provider.baseUrl}', apiKey='${provider.apiKey}', " +
                "models=${provider.models.size}",
                result.isSuccess
            )
        }
    }

    /**
     * 属性测试：默认模型必须在模型列表中
     *
     * 对于任意服务商配置，默认模型 ID 必须存在于模型列表中
     */
    @Test
    fun `property - default model must be in model list`() = runTest {
        checkAll(100, providerWithModelsArb()) { provider ->
            // Mock repository to always succeed
            coEvery { repository.saveProvider(any()) } returns Result.success(Unit)

            // When
            val result = useCase(provider)

            // Then - 如果默认模型不在列表中，应该失败
            val hasDefaultModel = provider.models.any { it.id == provider.defaultModelId }
            if (!hasDefaultModel && provider.models.isNotEmpty()) {
                assertFalse(
                    "Provider with invalid default model should be rejected: " +
                    "defaultModelId='${provider.defaultModelId}', " +
                    "models=${provider.models.map { it.id }}",
                    result.isSuccess
                )
            }
        }
    }

    // ========== 生成器 ==========

    /**
     * 生成任意 AiProvider（包括无效的）
     */
    private fun providerArb() = arbitrary {
        val models = Arb.list(modelArb(), 0..5).bind()
        val defaultModelId = if (models.isNotEmpty()) {
            models.random().id
        } else {
            Arb.string(0..20).bind()
        }

        AiProvider(
            id = UUID.randomUUID().toString(),
            name = Arb.string(0..50).bind(),
            baseUrl = Arb.string(0..100).bind(),
            apiKey = Arb.string(0..50).bind(),
            models = models,
            defaultModelId = defaultModelId,
            isDefault = false
        )
    }

    /**
     * 生成有效的 AiProvider
     */
    private fun validProviderArb() = arbitrary {
        val models = Arb.list(modelArb(), 1..5).bind()
        val defaultModel = models.random()

        AiProvider(
            id = UUID.randomUUID().toString(),
            name = Arb.string(1..50).bind(),
            baseUrl = Arb.validUrlArb().bind(),
            apiKey = Arb.string(1..50).bind(),
            models = models,
            defaultModelId = defaultModel.id,
            isDefault = false
        )
    }

    /**
     * 生成带模型的 AiProvider
     */
    private fun providerWithModelsArb() = arbitrary {
        val models = Arb.list(modelArb(), 1..5).bind()
        // 50% 概率使用有效的默认模型，50% 概率使用无效的
        val defaultModelId = if (it.random.nextBoolean()) {
            models.random().id
        } else {
            "invalid-model-${UUID.randomUUID()}"
        }

        AiProvider(
            id = UUID.randomUUID().toString(),
            name = Arb.string(1..50).bind(),
            baseUrl = Arb.validUrlArb().bind(),
            apiKey = Arb.string(1..50).bind(),
            models = models,
            defaultModelId = defaultModelId,
            isDefault = false
        )
    }

    /**
     * 生成 AiModel
     */
    private fun modelArb() = arbitrary {
        AiModel(
            id = "model-${UUID.randomUUID()}",
            displayName = Arb.string(0..30).bind().ifBlank { null }
        )
    }

    /**
     * 生成有效的 URL
     */
    private fun Arb.Companion.validUrlArb() = arbitrary {
        val protocol = if (it.random.nextBoolean()) "https://" else "http://"
        val domain = Arb.string(1..20).bind().replace(" ", "")
        val path = Arb.string(0..30).bind().replace(" ", "")
        "$protocol$domain.com/$path"
    }
}
