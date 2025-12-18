package com.empathy.ai.di

import android.content.Context
import com.empathy.ai.notification.AiResultNotificationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 通知模块
 *
 * 提供通知相关的依赖注入
 *
 * @see TDD-00010 悬浮球状态指示与拖动技术设计
 */
@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    /**
     * 提供AI结果通知管理器
     *
     * @param context 应用上下文
     * @return AiResultNotificationManager 单例
     */
    @Provides
    @Singleton
    fun provideAiResultNotificationManager(
        @ApplicationContext context: Context
    ): AiResultNotificationManager {
        return AiResultNotificationManager(context)
    }
}
