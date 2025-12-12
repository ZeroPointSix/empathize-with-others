package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.repository.AiProviderRepository
import javax.inject.Inject

/**
 * 保存服务商用例
 *
 * 验证并保存 AI 服务商配置
 *
 * 验证规则：
 * - 名称不能为空
 * - URL 必须以 http:// 或 https:// 开头
 * - API Key 不能为空
 * - 至少包含一个模型
 * - 默认模型 ID 必须在模型列表中
 */
class SaveProviderUseCase @Inject constructor(
    private val repository: AiProviderRepository
) {
    /**
     * 执行保存操作
     *
     * @param provider 要保存的服务商
     * @return Result 表示操作成功或失败
     */
    suspend operator fun invoke(provider: AiProvider): Result<Unit> {
        // 验证名称
        if (provider.name.isBlank()) {
            return Result.failure(ValidationException("请输入服务商名称"))
        }
        
        // 验证 URL 格式
        if (provider.baseUrl.isBlank()) {
            return Result.failure(ValidationException("请输入 API 端点"))
        }
        if (!provider.baseUrl.startsWith("http://") && !provider.baseUrl.startsWith("https://")) {
            return Result.failure(ValidationException("请输入有效的 URL（必须以 http:// 或 https:// 开头）"))
        }
        
        // 验证 API Key
        if (provider.apiKey.isBlank()) {
            return Result.failure(ValidationException("请输入 API Key"))
        }
        
        // 验证模型列表
        if (provider.models.isEmpty()) {
            return Result.failure(ValidationException("请至少添加一个模型"))
        }
        
        // 验证默认模型
        if (provider.models.none { it.id == provider.defaultModelId }) {
            return Result.failure(ValidationException("默认模型必须在模型列表中"))
        }
        
        // 保存到仓库
        return repository.saveProvider(provider)
    }
}

/**
 * 验证异常
 *
 * 用于表示配置验证失败
 */
class ValidationException(message: String) : Exception(message)
