package com.empathy.ai.app

import com.empathy.ai.domain.model.SystemPromptScene
import com.empathy.ai.domain.repository.SystemPromptRepository
import com.empathy.ai.domain.util.SystemPrompts
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 系统提示词配置提供者实现
 *
 * PRD-00033: 开发者模式 - 系统提示词编辑
 * 从SystemPromptRepository读取用户自定义的系统提示词配置
 */
@Singleton
class SystemPromptConfigProvider @Inject constructor(
    private val systemPromptRepository: SystemPromptRepository
) : SystemPrompts.ConfigProvider {

    override suspend fun getCustomHeader(scene: SystemPromptScene): String {
        return systemPromptRepository.getHeader(scene) ?: ""
    }

    override suspend fun getCustomFooter(scene: SystemPromptScene): String {
        // Footer不支持自定义，始终返回空
        return ""
    }
}
