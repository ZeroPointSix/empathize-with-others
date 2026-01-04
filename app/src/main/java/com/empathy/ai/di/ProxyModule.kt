package com.empathy.ai.di

import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.usecase.TestProxyConnectionUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 代理配置依赖注入模块
 *
 * 提供网络代理测试功能的依赖注入配置。
 *
 * 业务背景 (TD-00025 Phase 5):
 *   - 部分用户因网络限制需要通过代理访问AI服务
 *   - 提供代理连接测试功能，确保代理配置正确
 *
 * 功能说明:
 *   - TestProxyConnectionUseCase: 测试代理服务器连通性
 *   - 支持HTTP/SOCKS5代理协议
 *   - 测试超时时间可配置（默认10秒）
 *
 * 用户场景:
 *   1. 用户在设置页面配置代理后，点击"测试连接"
 *   2. 系统尝试通过代理建立到AI服务的连接
 *   3. 返回测试结果（成功/失败及错误信息）
 *
 * 权衡 (TD-00025):
 *   - 为什么单独提供代理测试UseCase？
 *     → 分离关注点，便于复用和单独测试
 *   - 为什么不在AI服务调用时自动测试？
 *     → 显式测试让用户明确知道代理是否可用
 *
 * @see TD-00025 AI配置功能完善技术设计
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
