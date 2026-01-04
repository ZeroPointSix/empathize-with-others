package com.empathy.ai.data.di

import javax.inject.Qualifier

/**
 * IO Dispatcher 限定符 (TDD-00017/7.2)
 *
 * 设计决策：区分IO和Default调度器便于单元测试
 * - 测试时可注入 TestDispatcher 替换 Dispatchers.IO
 * - 确保测试代码在预期线程执行
 *
 * @see DispatcherModule.provideIoDispatcher()
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Default Dispatcher 限定符 (TDD-00017/7.2)
 *
 * 使用场景：CPU密集型计算操作
 * - 区别于IO调度器（用于文件读写、网络请求）
 * - Default调度器使用共享线程池，适合计算密集型任务
 *
 * @see DispatcherModule.provideDefaultDispatcher()
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
