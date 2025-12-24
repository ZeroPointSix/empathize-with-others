package com.empathy.ai.data.di

import javax.inject.Qualifier

/**
 * IO Dispatcher 注解
 *
 * 用于标记需要在IO线程执行的操作（文件读写、网络请求等）
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Default Dispatcher 注解
 *
 * 用于标记需要在默认线程池执行的CPU密集型操作
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
