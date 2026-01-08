package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.KnowledgeQueryRequest
import com.empathy.ai.domain.model.KnowledgeQueryResponse
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.util.Logger
import com.empathy.ai.domain.util.PromptBuilder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 知识查询用例
 *
 * 核心业务流: 快速问答功能
 *
 * 触发场景: 用户点击悬浮窗的 [💡 快速问答] Tab并提交查询
 *
 * 功能: 对用户输入的内容进行知识查询，返回Markdown格式的解释和相关推荐
 *
 * 核心逻辑：
 * 1. 验证查询内容（非空、长度限制）
 * 2. 检查AI服务商配置
 * 3. 构建系统提示词
 * 4. 调用AI进行知识查询
 * 5. 返回格式化的知识响应
 *
 * 错误处理：
 * - 查询内容无效：返回验证错误
 * - 未配置AI服务商：返回配置错误
 * - AI调用失败：返回调用错误
 *
 * @see PRD-00031 悬浮窗快速知识回答功能需求
 * @see TDD-00031 悬浮窗快速知识回答功能技术设计
 */
@Singleton
class QueryKnowledgeUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val aiProviderRepository: AiProviderRepository,
    private val promptBuilder: PromptBuilder,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "QueryKnowledgeUseCase"
    }

    /**
     * 执行知识查询
     *
     * @param request 知识查询请求
     * @return 知识查询结果
     */
    suspend operator fun invoke(request: KnowledgeQueryRequest): Result<KnowledgeQueryResponse> {
        return try {
            logger.d(TAG, "开始知识查询: ${request.getSummary()}")
            
            // 1. 前置检查 - 查询内容验证
            if (!request.isValid()) {
                val errorMessage = request.getValidationError() 
                    ?: "查询内容无效：内容为空或超出${request.maxLength}字符限制"
                logger.w(TAG, "查询内容验证失败: $errorMessage")
                return Result.failure(IllegalArgumentException(errorMessage))
            }
            
            // 2. 前置检查 - AI服务商配置
            val defaultProvider = aiProviderRepository.getDefaultProvider().getOrNull()
            if (defaultProvider == null) {
                logger.w(TAG, "未配置默认AI服务商")
                return Result.failure(IllegalStateException("请先配置AI服务商"))
            }
            
            logger.d(TAG, "使用AI服务商: ${defaultProvider.name}, 模型: ${defaultProvider.getDefaultModel()?.id ?: defaultProvider.defaultModelId}")
            
            // 3. 构建系统提示词
            val systemInstruction = promptBuilder.buildKnowledgePrompt()
            
            // 4. 调用AI进行知识查询
            val content = request.getCleanedContent()
            logger.d(TAG, "调用AI查询知识，内容长度: ${content.length}")
            
            val result = aiRepository.queryKnowledge(
                provider = defaultProvider,
                content = content,
                systemInstruction = systemInstruction
            )
            
            // 5. 处理结果
            result.fold(
                onSuccess = { response ->
                    logger.d(TAG, "知识查询成功，内容长度: ${response.content.length}, 推荐数: ${response.recommendations.size}")
                    Result.success(response)
                },
                onFailure = { error ->
                    logger.e(TAG, "知识查询失败", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.e(TAG, "知识查询异常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 简化调用方式 - 直接传入查询内容
     *
     * @param content 查询内容
     * @return 知识查询结果
     */
    suspend operator fun invoke(content: String): Result<KnowledgeQueryResponse> {
        return invoke(KnowledgeQueryRequest(content = content))
    }
}
