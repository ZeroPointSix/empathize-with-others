package com.empathy.ai.data.di

import android.util.Log
import com.empathy.ai.data.remote.api.OpenAiApi
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
import javax.inject.Singleton

/**
 * 网络模块
 *
 * 提供 Retrofit、OkHttp 和 API 接口的依赖注入配置。
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
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * 提供 Moshi 实例
     *
     * 配置 Moshi 以支持 Kotlin 数据类的 JSON 序列化/反序列化。
     *
     * @return Moshi 实例
     */
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    /**
     * 提供 OkHttpClient
     *
     * 配置 HTTP 客户端的超时时间和日志拦截器。
     * 
     * 超时策略：
     * - HTTP 层超时应该比协程超时短，让 HTTP 层先处理超时
     * - 协程超时作为最后的兜底机制
     * - 这样可以优雅地处理网络超时，而不是强制取消请求
     *
     * @return OkHttpClient 实例
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val clientBuilder = OkHttpClient.Builder()

        // 超时设置 (LLM 应用需要较长的超时时间)
        // 注意：这些超时应该比协程超时短，让 HTTP 层先超时
        clientBuilder
            .connectTimeout(15, TimeUnit.SECONDS)  // 连接超时:15秒
            .readTimeout(45, TimeUnit.SECONDS)     // 读取超时:45秒 (比协程超时短5-10秒)
            .writeTimeout(15, TimeUnit.SECONDS)    // 写入超时:15秒

        // 日志拦截器 - 使用BASIC级别，避免依赖BuildConfig
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("OkHttp", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        clientBuilder.addInterceptor(loggingInterceptor)

        return clientBuilder.build()
    }

    /**
     * 提供 Retrofit
     *
     * 配置 Retrofit 使用 OkHttp 和 Moshi 进行网络请求和 JSON 转换。
     *
     * @param okHttpClient OkHttpClient 实例
     * @param moshi Moshi 实例
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
}
