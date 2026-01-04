package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.repository.AiProviderRepository
import javax.inject.Inject

/**
 * 保存服务商用例
 *
 * 验证并保存 AI 服务商配置。
 *
 * 业务背景:
 *   - TD-00025: AI配置功能完善
 *   - 场景: 用户新增或编辑AI服务商配置
 *
 * 验证规则（严格模式）:
 *   1. 名称不能为空
 *   2. URL 必须以 http:// 或 https:// 开头
 *   3. API Key 不能为空
 *   4. 至少包含一个模型
 *   5. 默认模型 ID 必须在模型列表中
 *
 * 设计权衡:
 *   - 为什么严格验证? 防止保存无效配置导致运行时崩溃
 *   - 验证失败返回 ValidationException，便于UI展示具体错误
 *
 * @see AiProvider 服务商数据模型
 * @see ValidationException 验证异常
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
