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
 * 服务层依赖注入模块
 *
 * 提供悬浮窗服务和通用功能所需的系统级依赖。
 *
 * 业务背景:
 *   - 悬浮窗功能需要系统级服务支持（WindowManager）
 *   - 异步操作需要应用级协程作用域管理
 *
 * 提供依赖:
 *   1. ServiceCoroutineScope - 应用级协程作用域
 *      - 用于需要跨越多个Activity/Fragment的生命周期任务
 *      - 使用SupervisorJob确保子任务失败不影响其他任务
 *      - 主要用于悬浮窗服务的异步操作
 *
 *   2. WindowManager - 系统窗口管理器
 *      - Android系统级服务，用于创建和管理悬浮窗
 *      - 通过Context.WINDOW_SERVICE获取
 *      - 负责悬浮窗的添加、更新、移除操作
 *
 * 设计权衡:
 *   - 为什么使用@ServiceScope限定符？
 *     → 标识这是服务层专用的协程作用域，与UI层分离
 *   - 为什么WindowManager要作为依赖注入？
 *     → 便于在单元测试中Mock，提高可测试性
 *
 * @see TDD-00001 悬浮窗架构设计
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    
    /**
     * 提供应用级协程作用域
     *
     * 用于需要跨越多个Activity/Fragment生命周期的异步任务。
     * 区别于ViewModelScope和MainScope，具有更长的生命周期（与应用同生共死）。
     *
     * 使用场景:
     *   - 悬浮窗服务的异步操作
     *   - 应用启动时的初始化任务
     *   - 全局事件监听和处理
     *
     * 权衡: 使用SupervisorJob而非默认的Job，
     *       确保一个子任务失败不会取消其他正在运行的任务。
     */
    @Provides
    @Singleton
    @ServiceScope
    fun provideServiceCoroutineScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    /**
     * 提供系统WindowManager实例
     *
     * Android系统级服务，用于悬浮窗的创建、更新和移除。
     * 通过@ApplicationContext获取，确保使用应用级上下文。
     *
     * 核心操作:
     *   - addView(): 将悬浮窗视图添加到窗口
     *   - updateViewLayout(): 更新悬浮窗位置和参数
     *   - removeView(): 移除悬浮窗视图
     *
     * 注意: 必须使用ApplicationContext而非ActivityContext，
     *       避免因Activity销毁导致悬浮窗异常。
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
 * 限定符：标识服务层专属的协程作用域
 *
 * 用于区分不同层级的协程作用域:
 *   - @MainDispatcher: 主线程调度器
 *   - @IoDispatcher: IO线程调度器
 *   - @ServiceScope: 服务层协程作用域（应用级生命周期）
 *
 * 使用方式:
 *   @Inject @ServiceScope lateinit var serviceScope: CoroutineScope
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ServiceScope
