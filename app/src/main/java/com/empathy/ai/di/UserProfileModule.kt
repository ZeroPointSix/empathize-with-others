package com.empathy.ai.di

import android.content.Context
import com.empathy.ai.data.local.UserProfileCache
import com.empathy.ai.data.local.UserProfilePreferences
import com.empathy.ai.domain.util.UserProfileContextBuilder
import com.empathy.ai.domain.util.UserProfileValidator
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 用户画像模块
 *
 * 提供用户画像功能相关的依赖注入配置。
 * 包括：
 * - UserProfilePreferences: 加密存储
 * - UserProfileCache: 多级缓存
 * - UserProfileValidator: 验证器
 * - UserProfileContextBuilder: 上下文构建器
 */
@Module
@InstallIn(SingletonComponent::class)
object UserProfileModule {
    
    /**
     * 提供 UserProfilePreferences 单例
     *
     * 使用 EncryptedSharedPreferences 进行加密存储。
     *
     * @param context 应用上下文
     * @param moshi Moshi JSON 解析器
     * @return UserProfilePreferences 实例
     */
    @Provides
    @Singleton
    fun provideUserProfilePreferences(
        @ApplicationContext context: Context,
        moshi: Moshi
    ): UserProfilePreferences {
        return UserProfilePreferences(context, moshi)
    }
    
    /**
     * 提供 UserProfileCache 单例
     *
     * 实现多级缓存机制，提升数据访问性能。
     *
     * @return UserProfileCache 实例
     */
    @Provides
    @Singleton
    fun provideUserProfileCache(): UserProfileCache {
        return UserProfileCache()
    }
    
    /**
     * 提供 UserProfileValidator 单例
     *
     * 用于验证用户画像数据的合法性。
     *
     * @return UserProfileValidator 实例
     */
    @Provides
    @Singleton
    fun provideUserProfileValidator(): UserProfileValidator {
        return UserProfileValidator()
    }
}
