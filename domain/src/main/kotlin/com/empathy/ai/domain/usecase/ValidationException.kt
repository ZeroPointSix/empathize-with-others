package com.empathy.ai.domain.usecase

/**
 * 验证异常
 *
 * 用于表示配置验证失败
 */
class ValidationException(message: String) : Exception(message)
