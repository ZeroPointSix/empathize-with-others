package com.empathy.ai.di

import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.usecase.TestProxyConnectionUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 代理配置模块
 *
 * 提供代理相关UseCase的依赖注入配置
 *
 * @see TD-00025 Phase 5: 网络代理实现
 */
@Module
@InstallIn(SingletonComponent::class)
object ProxyModule {

    /**
     * 提供测试代理连接UseCase
     */
    @Provides
    @Singleton
    fun provideTestProxyConnectionUseCase(
        repository: AiProviderRepository
    ): TestProxyConnectionUseCase {
        return TestProxyConnectionUseCase(repository)
    }
}
