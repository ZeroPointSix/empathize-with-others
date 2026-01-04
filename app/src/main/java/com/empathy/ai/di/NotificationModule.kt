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
 * AI结果通知管理器依赖注入模块
 *
 * 提供AI处理结果通知相关的依赖注入配置。
 *
 * 业务背景 (TDD-00010 Phase 4: 状态指示与完成通知):
 *   - 悬浮窗最小化后，用户需要知道AI处理的状态和结果
 *   - 通过系统通知传达处理进度和最终结果
 *
 * 通知类型:
 *   - 分析完成通知: 展示聊天分析结果摘要
 *   - 检查完成通知: 展示安全检查结果
 *   - 错误通知: 展示处理失败的错误信息
 *
 * 交互设计:
 *   - 点击通知跳转到对应的结果展示页面
 *   - 支持消息展开/折叠，适配不同内容长度
 *   - 通知优先级根据结果重要性动态调整
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
