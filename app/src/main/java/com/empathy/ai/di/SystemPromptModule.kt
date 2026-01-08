package com.empathy.ai.di

import com.empathy.ai.app.SystemPromptConfigProvider
import com.empathy.ai.domain.util.SystemPrompts
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 系统提示词配置依赖注入模块
 *
 * PRD-00033: 开发者模式 - 系统提示词编辑
 * 
 * 将SystemPrompts.ConfigProvider接口绑定到SystemPromptConfigProvider实现。
 * 
 * 架构意义:
 *   - Domain层定义ConfigProvider接口，保持业务逻辑层对数据层无依赖
 *   - App层提供SystemPromptConfigProvider实现，从SystemPromptRepository读取配置
 *   - 通过@Binds实现跨模块依赖注入
 * 
 * 设计权衡:
 *   - 为什么ConfigProvider在Domain层定义？
 *     → SystemPrompts是Domain层工具类，需要在Domain层定义接口
 *   - 为什么实现在App层而非Data层？
 *     → 需要在Application.onCreate()中初始化，App层更合适
 * 
 * 使用场景:
 *   - EmpathyApplication在onCreate()中注入ConfigProvider
 *   - PromptBuilder通过SystemPrompts.getHeaderAsync()获取自定义配置
 *   - 开发者模式下用户修改系统提示词后立即生效
 * 
 * @see PRD-00033 开发者模式与系统提示词编辑功能需求
 * @see TDD-00033 开发者模式与系统提示词编辑技术设计
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SystemPromptModule {

    /**
     * 绑定SystemPrompts.ConfigProvider接口到SystemPromptConfigProvider实现
     */
    @Binds
    @Singleton
    abstract fun bindSystemPromptConfigProvider(
        impl: SystemPromptConfigProvider
    ): SystemPrompts.ConfigProvider
}
