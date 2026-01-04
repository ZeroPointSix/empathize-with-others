package com.empathy.ai.domain.usecase

/**
 * 验证异常
 *
 * 用于表示配置验证失败。
 * 继承 Exception，携带验证失败的详细错误信息。
 *
 * 使用场景:
 *   - SaveProviderUseCase: 验证服务商配置时抛出
 *   - AddTagUseCase: 验证标签时抛出 (TagValidationException)
 *
 * @see SaveProviderUseCase 服务商验证
 * @see AddTagUseCase.TagValidationException 标签验证异常
 */
class ValidationException(message: String) : Exception(message)
