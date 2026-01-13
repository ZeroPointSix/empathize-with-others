package com.empathy.ai.data.di

import android.util.Log
import com.empathy.ai.data.local.ProxyPreferences
import com.empathy.ai.data.remote.SseStreamReader
import com.empathy.ai.data.remote.api.OpenAiApi
import com.empathy.ai.data.remote.model.MessageDtoContentJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * 网络模块
 *
 * 提供 Retrofit、OkHttp 和 API 接口的依赖注入配置。
 *
 * 业务背景:
 *   - FD: FD-00025 [AI配置功能完善]
 *   - TDD: TDD-00025 [AI配置功能完善技术设计]
 *
 * 设计决策:
 *   - 选择 Retrofit + OkHttp 组合 → Android 生态成熟方案，兼容性最好
 *   - 超时策略分层 → HTTP层和协程层双重保障
 *   - Moshi 替代 Gson → Kotlin 反射支持更好
 *
 * 核心配置:
 * 1. **超时设置**: LLM 响应较慢,需要设置较长的超时时间
 *    - connectTimeout = 30秒 (连接超时)
 *    - readTimeout = 60秒 (读取超时,AI生成回复可能需要20-40秒)
 *    - writeTimeout = 30秒 (写入超时)
 *
 * 2. **日志拦截器**: 仅在 DEBUG 模式下记录请求和响应的完整内容
 *    - 方便调试时查看发送的 JSON 和接收的 JSON
 *    - 正式发布时自动关闭详细日志
 *
 * 3. **Moshi 配置**: 支持 Kotlin 数据类
 *    - 添加 KotlinJsonAdapterFactory 处理 Kotlin 特性
 *    - 如默认值为 null、空集合等
 *
 * 4. **代理支持** (TD-00025): 支持HTTP/HTTPS/SOCKS代理
 *    - 通过OkHttpClientFactory动态配置代理
 *    - 支持代理认证
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * 提供 Moshi 实例
     *
     * 【技术选型】KotlinJsonAdapterFactory vs 标准Adapter
     * - 标准Adapter：无法处理 Kotlin 默认值、空值等特性
     * - KotlinJsonAdapterFactory：完整支持 Kotlin 特性
     *   * 默认参数值
     *   * 可空类型
     *   * data class 的 copy() 方法生成的字段
     *
     * @return Moshi 实例
     */
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(MessageDtoContentJsonAdapterFactory())
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    /**
     * 提供 OkHttpClientFactory
     *
     * TD-00025: 支持动态代理配置
     *
     * @param proxyPreferences 代理配置存储
     * @return OkHttpClientFactory 实例
     */
    @Provides
    @Singleton
    fun provideOkHttpClientFactory(
        proxyPreferences: ProxyPreferences
    ): OkHttpClientFactory {
        return OkHttpClientFactory(proxyPreferences)
    }

    /**
     * 提供 OkHttpClient
     *
     * 配置 HTTP 客户端的超时时间和日志拦截器。
     *
     * 【超时策略设计】三层超时保障
     * ┌────────────────────────────────────────────────────────────────────┐
     * │ Layer 1: OkHttp Timeout (HTTP层)                                  │
     * │   - connectTimeout = 30秒: 建立连接最大时间                       │
     * │   - readTimeout = 60秒: 读取响应最大时间（AI生成可能耗时）        │
     * │   - writeTimeout = 30秒: 发送请求最大时间                         │
     * │   【优势】HTTP层超时可被 OkHttp 正确处理和重试                    │
     * ├────────────────────────────────────────────────────────────────────┤
     * │ Layer 2: Coroutine Timeout (协程层)                               │
     * │   - withTimeoutOrNull() 作为最后兜底                              │
     * │   【优势】防止长期阻塞协程，导致资源泄漏                          │
     * ├────────────────────────────────────────────────────────────────────┤
     * │ Layer 3: Repository Timeout (业务层)                              │
     * │   - AiRepositoryImpl 中的超时控制                                 │
     * │   【优势】根据不同操作类型设置不同超时                            │
     * └────────────────────────────────────────────────────────────────────┘
     *
     * 【设计理由】为什么 HTTP 层超时应该比协程层短？
     * - HTTP 层超时可以触发 OkHttp 的重试机制
     * - 协程层超时直接取消请求，可能导致请求被强制中断
     * - 优雅降级：让 HTTP 层先处理超时，而不是直接取消
     *
     * TD-00025: 通过OkHttpClientFactory获取客户端，支持代理配置
     *
     * @param factory OkHttpClientFactory 实例
     * @return OkHttpClient 实例
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(factory: OkHttpClientFactory): OkHttpClient {
        return factory.getClient()
    }

    /**
     * 提供 Retrofit
     *
     * 配置 Retrofit 使用 OkHttp 和 Moshi 进行网络请求和 JSON 转换。
     *
     * 【设计决策】baseUrl 占位符策略
     * - 实际 URL 通过 @Url 注解在运行时指定
     * - baseUrl 只作为编译期占位符
     * - 确保 Retrofit 实例可复用，无需为每个服务商创建实例
     *
     * 【备选方案】
     * ┌─────────────────────────────────────────────────────────────────────┐
     * │ 方案A: 占位符 baseUrl (当前)          │ 方案B: 每个服务商独立Retrofit │
     * ├─────────────────────────────────────────────────────────────────────┤
     * │ ✅ 单例，复用连接池                    │ ❌ 连接池无法复用             │
     * │ ✅ 内存占用小                         │ ❌ 内存占用大                 │
     * │ ⚠️ URL 正确性由调用方保证             │ ✅ 编译期检查 URL 格式        │
     * └─────────────────────────────────────────────────────────────────────┘
     *
     * @param okHttpClient OkHttpClient 实例，包含超时和代理配置
     * @param moshi Moshi 实例，用于 JSON 序列化
     * @return Retrofit 实例
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            // baseUrl 在这里是占位符,实际使用 @Url 注解动态指定
            .baseUrl("https://api.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    /**
     * 提供 OpenAiApi
     *
     * @param retrofit Retrofit 实例
     * @return OpenAiApi 接口实例
     */
    @Provides
    @Singleton
    fun provideOpenAiApi(retrofit: Retrofit): OpenAiApi {
        return retrofit.create(OpenAiApi::class.java)
    }

    // ==================== FD-00028: SSE流式响应支持 ====================

    /**
     * 提供 SSE 专用 OkHttpClient
     *
     * SSE (Server-Sent Events) 需要特殊的超时配置：
     * - readTimeout = 0: 无限读取超时，因为SSE是长连接
     * - connectTimeout = 30秒: 连接超时保持正常
     * - writeTimeout = 30秒: 写入超时保持正常
     *
     * 业务背景 (FD-00028):
     * - 流式响应需要保持长连接
     * - AI生成可能需要较长时间
     * - 不能因为读取超时而中断流式响应
     *
     * @param factory OkHttpClientFactory 实例，用于获取代理配置
     * @return SSE专用OkHttpClient实例
     */
    @Provides
    @Singleton
    @Named("sse")
    fun provideSseOkHttpClient(factory: OkHttpClientFactory): OkHttpClient {
        // 基于现有客户端配置，但修改超时设置
        return factory.getClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)  // SSE需要无限读取超时
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * 提供 SseStreamReader
     *
     * SSE流式读取器，用于处理AI流式响应。
     *
     * @param okHttpClient SSE专用OkHttpClient
     * @return SseStreamReader实例
     *
     * @see FD-00028 AI军师流式对话升级功能设计
     */
    @Provides
    @Singleton
    fun provideSseStreamReader(
        @Named("sse") okHttpClient: OkHttpClient
    ): SseStreamReader {
        return SseStreamReader(okHttpClient)
    }
}
