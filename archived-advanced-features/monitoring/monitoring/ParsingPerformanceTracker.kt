package com.empathy.ai.data.monitoring

import android.util.Log
import com.empathy.ai.data.parser.ParsingContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 解析性能跟踪器
 * 
 * 负责跟踪单个解析请求的完整生命周期
 * 记录各个阶段的耗时和性能数据
 */
class ParsingPerformanceTracker {
    
    companion object {
        private const val TAG = "ParsingPerformanceTracker"
        
        // 性能阈值配置（毫秒）
        private const val CLEANING_THRESHOLD = 100L
        private const val MAPPING_THRESHOLD = 50L
        private const val PARSING_THRESHOLD = 200L
        private const val FALLBACK_THRESHOLD = 300L
        
        // 跟踪会话保留时间（毫秒）
        private const val SESSION_RETENTION_PERIOD = 30 * 60 * 1000L // 30分钟
    }
    
    // 活跃的解析会话
    private val activeSessions = ConcurrentHashMap<String, ParsingSession>()
    
    // 已完成的解析会话
    private val completedSessions = ConcurrentHashMap<String, ParsingSession>()
    
    // 会话ID生成器
    private val sessionIdGenerator = AtomicLong(0)
    
    /**
     * 开始跟踪解析会话
     */
    fun startTracking(context: ParsingContext): String {
        val sessionId = generateSessionId()
        val session = ParsingSession(
            sessionId = sessionId,
            operationId = context.operationId,
            operationType = context.operationType,
            modelName = context.modelName,
            startTime = System.nanoTime(),
            enableDetailedLogging = context.enableDetailedLogging
        )
        
        activeSessions[sessionId] = session
        
        if (context.enableDetailedLogging) {
            Log.d(TAG, "开始跟踪解析会话: ID=$sessionId, 操作=${context.operationType}, 模型=${context.modelName}")
        }
        
        return sessionId
    }
    
    /**
     * 记录阶段开始
     */
    fun recordPhaseStart(sessionId: String, phase: ParsingPhase) {
        val session = activeSessions[sessionId]
        if (session == null) {
            Log.w(TAG, "尝试记录不存在的会话: $sessionId")
            return
        }
        
        val phaseTiming = PhaseTiming(
            phase = phase,
            startTime = System.nanoTime()
        )
        
        session.currentPhase = phase
        session.phaseTimings[phase] = phaseTiming
        
        if (session.enableDetailedLogging) {
            Log.d(TAG, "会话 $sessionId 开始阶段: ${phase.name}")
        }
    }
    
    /**
     * 记录阶段完成
     */
    fun recordPhaseEnd(sessionId: String, phase: ParsingPhase, success: Boolean, error: Throwable? = null) {
        val session = activeSessions[sessionId]
        if (session == null) {
            Log.w(TAG, "尝试记录不存在的会话: $sessionId")
            return
        }
        
        val phaseTiming = session.phaseTimings[phase]
        if (phaseTiming == null) {
            Log.w(TAG, "尝试记录未开始的阶段: $sessionId, ${phase.name}")
            return
        }
        
        val endTime = System.nanoTime()
        val duration = endTime - phaseTiming.startTime
        val durationMs = duration / 1_000_000
        
        phaseTiming.endTime = endTime
        phaseTiming.duration = duration
        phaseTiming.durationMs = durationMs
        phaseTiming.success = success
        phaseTiming.error = error
        
        // 检查性能阈值
        checkPhasePerformanceThresholds(sessionId, phase, durationMs)
        
        if (session.enableDetailedLogging) {
            val status = if (success) "成功" else "失败"
            Log.d(TAG, "会话 $sessionId 阶段 ${phase.name} $status, 耗时: ${durationMs}ms")
        }
    }
    
    /**
     * 记录自定义指标
     */
    fun recordCustomMetric(sessionId: String, metricName: String, value: Any) {
        val session = activeSessions[sessionId]
        if (session == null) {
            Log.w(TAG, "尝试记录不存在的会话: $sessionId")
            return
        }
        
        session.customMetrics[metricName] = value
        
        if (session.enableDetailedLogging) {
            Log.d(TAG, "会话 $sessionId 记录自定义指标: $metricName=$value")
        }
    }
    
    /**
     * 完成跟踪会话
     */
    fun completeTracking(sessionId: String, finalResult: TrackingResult) {
        val session = activeSessions.remove(sessionId)
        if (session == null) {
            Log.w(TAG, "尝试完成不存在的会话: $sessionId")
            return
        }
        
        val endTime = System.nanoTime()
        session.endTime = endTime
        session.totalDuration = endTime - session.startTime
        session.totalDurationMs = session.totalDuration / 1_000_000
        session.finalResult = finalResult
        
        // 移动到已完成会话
        completedSessions[sessionId] = session
        
        // 记录总体性能
        recordOverallPerformance(session)
        
        if (session.enableDetailedLogging) {
            Log.d(TAG, "完成跟踪会话: ID=$sessionId, 总耗时: ${session.totalDurationMs}ms, 结果: ${finalResult.name}")
        }
    }
    
    /**
     * 获取会话详情
     */
    fun getSession(sessionId: String): ParsingSession? {
        return activeSessions[sessionId] ?: completedSessions[sessionId]
    }
    
    /**
     * 获取所有活跃会话
     */
    fun getActiveSessions(): List<ParsingSession> {
        return activeSessions.values.toList()
    }
    
    /**
     * 获取已完成会话
     */
    fun getCompletedSessions(limit: Int = 100): List<ParsingSession> {
        return completedSessions.values
            .sortedByDescending { it.endTime }
            .take(limit)
    }
    
    /**
     * 获取性能统计
     */
    fun getPerformanceStatistics(): PerformanceStatistics {
        val allSessions = (activeSessions.values + completedSessions.values).toList()
        
        val totalSessions = allSessions.size
        val successfulSessions = allSessions.count { it.finalResult == TrackingResult.SUCCESS }
        val failedSessions = totalSessions - successfulSessions
        val successRate = if (totalSessions > 0) successfulSessions.toDouble() / totalSessions else 0.0
        
        val completedSessionList = allSessions.filter { it.endTime > 0 }
        val avgDurationMs = if (completedSessionList.isNotEmpty()) {
            completedSessionList.sumOf { it.totalDurationMs } / completedSessionList.size
        } else {
            0L
        }
        
        val phaseStatistics = calculatePhaseStatistics(allSessions)
        
        return PerformanceStatistics(
            totalSessions = totalSessions,
            successfulSessions = successfulSessions,
            failedSessions = failedSessions,
            successRate = successRate,
            averageDurationMs = avgDurationMs,
            phaseStatistics = phaseStatistics
        )
    }
    
    /**
     * 清理过期会话
     */
    fun cleanupExpiredSessions() {
        val cutoffTime = System.currentTimeMillis() - SESSION_RETENTION_PERIOD
        
        // 清理已完成的会话
        completedSessions.entries.removeIf { (_, session) ->
            session.endTimeMs < cutoffTime
        }
        
        // 检查是否有长时间运行的活跃会话
        val now = System.currentTimeMillis()
        activeSessions.values.forEach { session ->
            val runningTimeMs = now - session.startTimeMs
            if (runningTimeMs > SESSION_RETENTION_PERIOD) {
                Log.w(TAG, "发现长时间运行的会话: ${session.sessionId}, 已运行${runningTimeMs}ms")
            }
        }
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "清理过期会话完成")
        }
    }
    
    /**
     * 生成会话ID
     */
    private fun generateSessionId(): String {
        return "track_${sessionIdGenerator.incrementAndGet()}_${System.currentTimeMillis()}"
    }
    
    /**
     * 检查阶段性能阈值
     */
    private fun checkPhasePerformanceThresholds(sessionId: String, phase: ParsingPhase, durationMs: Long) {
        val threshold = when (phase) {
            ParsingPhase.CLEANING -> CLEANING_THRESHOLD
            ParsingPhase.MAPPING -> MAPPING_THRESHOLD
            ParsingPhase.PARSING -> PARSING_THRESHOLD
            ParsingPhase.FALLBACK -> FALLBACK_THRESHOLD
            ParsingPhase.OVERALL -> 0L // 不检查总体阶段
        }
        
        if (threshold > 0L && durationMs > threshold) {
            Log.w(TAG, "性能警告: 会话 $sessionId 阶段 ${phase.name} 耗时 ${durationMs}ms 超过阈值 ${threshold}ms")
        }
    }
    
    /**
     * 记录总体性能
     */
    private fun recordOverallPerformance(session: ParsingSession) {
        // 这里可以将性能数据发送到外部监控系统
        // 例如：发送到日志系统、指标收集系统等
        
        if (session.totalDurationMs > 1000) {
            Log.w(TAG, "慢解析警告: 会话 ${session.sessionId} 总耗时 ${session.totalDurationMs}ms")
        }
    }
    
    /**
     * 计算阶段统计
     */
    private fun calculatePhaseStatistics(sessions: List<ParsingSession>): Map<ParsingPhase, PhaseStatistics> {
        val phaseStats = mutableMapOf<ParsingPhase, MutableList<Long>>()
        
        sessions.forEach { session ->
            session.phaseTimings.forEach { (phase, timing) ->
                if (timing.durationMs > 0) {
                    phaseStats.getOrPut(phase) { mutableListOf() }.add(timing.durationMs)
                }
            }
        }
        
        return phaseStats.mapValues { (_, durations) ->
            val avgDuration = durations.average().toLong()
            val minDuration = durations.minOrNull() ?: 0L
            val maxDuration = durations.maxOrNull() ?: 0L
            val count = durations.size
            
            PhaseStatistics(
                count = count,
                averageDurationMs = avgDuration,
                minDurationMs = minDuration,
                maxDurationMs = maxDuration
            )
        }
    }
    
    /**
     * 解析会话
     */
    data class ParsingSession(
        val sessionId: String,
        val operationId: String,
        val operationType: String,
        val modelName: String,
        val startTime: Long,
        val enableDetailedLogging: Boolean = false
    ) {
        val startTimeMs = startTime / 1_000_000
        var endTime: Long = 0
        val endTimeMs: Long get() = endTime / 1_000_000
        var totalDuration: Long = 0
        var totalDurationMs: Long = 0
        var currentPhase: ParsingPhase? = null
        var finalResult: TrackingResult? = null
        
        val phaseTimings = mutableMapOf<ParsingPhase, PhaseTiming>()
        val customMetrics = mutableMapOf<String, Any>()
    }
    
    /**
     * 阶段计时
     */
    data class PhaseTiming(
        val phase: ParsingPhase,
        val startTime: Long
    ) {
        var endTime: Long = 0
        var duration: Long = 0
        var durationMs: Long = 0
        var success: Boolean = false
        var error: Throwable? = null
    }
    
    /**
     * 性能统计
     */
    data class PerformanceStatistics(
        val totalSessions: Int,
        val successfulSessions: Int,
        val failedSessions: Int,
        val successRate: Double,
        val averageDurationMs: Long,
        val phaseStatistics: Map<ParsingPhase, PhaseStatistics>
    )
    
    /**
     * 阶段统计
     */
    data class PhaseStatistics(
        val count: Int,
        val averageDurationMs: Long,
        val minDurationMs: Long,
        val maxDurationMs: Long
    )
    
    /**
     * 解析阶段
     */
    enum class ParsingPhase {
        CLEANING,    // JSON清洗
        MAPPING,     // 字段映射
        PARSING,     // 解析
        FALLBACK,     // 降级处理
        OVERALL       // 总体
    }
    
    /**
     * 跟踪结果
     */
    enum class TrackingResult {
        SUCCESS,      // 成功
        FAILURE,      // 失败
        TIMEOUT,      // 超时
        CANCELLED     // 取消
    }
}