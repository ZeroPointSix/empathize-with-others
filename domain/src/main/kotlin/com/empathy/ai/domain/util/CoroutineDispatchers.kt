package com.empathy.ai.domain.util

import kotlinx.coroutines.CoroutineDispatcher

/**
 * 协程调度器接口
 *
 * 用于在Domain层抽象协程调度器，不依赖Android或Hilt。
 * 实现类在Data层或App层提供。
 *
 * @see TDD-00017 Clean Architecture模块化改造技术设计
 */
interface CoroutineDispatchers {
    /**
     * IO调度器
     *
     * 用于IO密集型操作（网络请求、数据库操作、文件读写等）
     */
    val io: CoroutineDispatcher

    /**
     * 默认调度器
     *
     * 用于CPU密集型操作（计算、数据处理等）
     */
    val default: CoroutineDispatcher

    /**
     * 主线程调度器
     *
     * 用于UI操作（仅在Presentation层使用）
     */
    val main: CoroutineDispatcher
}
