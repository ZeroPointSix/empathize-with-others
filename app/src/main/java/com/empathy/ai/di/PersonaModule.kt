package com.empathy.ai.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 标签画像V2依赖注入模块
 *
 * 本模块为占位模块，保留以便未来扩展。
 *
 * 以下类已通过 @Singleton + @Inject constructor 自动绑定到 Hilt，
 * 无需在此处手动 @Provides，否则会导致双重绑定冲突错误：
 *
 * - CategoryColorAssigner（分类颜色分配器）
 * - FactSearchFilter（Fact搜索过滤器）
 * - GroupFactsByCategoryUseCase（按分类分组用例）
 * - BatchDeleteFactsUseCase（批量删除用例）
 * - BatchMoveFactsUseCase（批量移动用例）
 *
 * Hilt 双重绑定错误示例：
 * "Added variable(s) does not support value initialization"
 *
 * 原因：当类同时有 @Inject constructor 和 Module 中的 @Provides 时，
 * Dagger 会为同一类型生成两个 Provider，导致冲突。
 */
@Module
@InstallIn(SingletonComponent::class)
object PersonaModule {
    // 所有依赖已通过 @Inject constructor 自动绑定
    // 此模块保留以便未来添加需要手动配置的依赖
}
