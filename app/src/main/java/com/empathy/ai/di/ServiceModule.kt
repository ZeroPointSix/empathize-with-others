package com.empathy.ai.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt 模块：提供服务层依赖
 * 
 * 职责：
 * - 提供应用级协程作用域
 * - 提供 WindowManager 实例
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    
    /**
     * 提供应用级协程作用域
     * 用于悬浮窗服务的异步操作
     */
    @Provides
    @Singleton
    @ServiceScope
    fun provideServiceCoroutineScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.Main + SupervisorJob())
    }
    
    /**
     * 提供 WindowManager 实例
     * 用于管理悬浮窗视图
     */
    @Provides
    @Singleton
    fun provideWindowManager(
        @ApplicationContext context: Context
    ): android.view.WindowManager {
        return context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
    }
}

/**
 * 限定符：标识服务层的协程作用域
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ServiceScope
