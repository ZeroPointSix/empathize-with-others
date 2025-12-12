package com.empathy.ai.data.observability

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

/**
 * 解析追踪器
 * 
 * 负责跟踪解析请求的完整生命周期
 * 提供详细的执行路径和性能分析
 */
class ParsingTracer private constructor() {
    companion object {
        private const val TAG = "ParsingTracer"
        
        // 最大追踪会话数
        private const val MAX_TRACE_SESSIONS = 1000
        
        // 追踪会话超时时间（毫秒）
        private const val TRACE_SESSION_TIMEOUT = 10 * 60 * 1000L // 10分钟
        
        @Volatile
        private var INSTANCE: ParsingTracer? = null
        
        fun getInstance(): ParsingTracer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ParsingTracer().also { INSTANCE = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 追踪会话存储
    private val activeSessions = ConcurrentHashMap<String, TraceSession>()
    private val completedSessions = CopyOnWriteArrayList<TraceSession>()
    
    // 统计信息
    private val totalSessionsCreated = AtomicLong(0)
    private val totalSessionsCompleted = AtomicLong(0)
    
    // 监听器
    private val traceListeners = CopyOnWriteArrayList<TraceListener>()
    
    init {
        // 启动定期清理任务
        startPeriodicCleanup()
        
        Log.i(TAG, "ParsingTracer 初始化完成")
    }
    
    /**
     * 开始追踪会话
     */
    fun startTraceSession(
        operationType: String,
        requestId: String? = null,
        input: Any? = null,
        metadata: Map<String, Any> = emptyMap()
    ): TraceSession {
        val sessionId = generateSessionId()
        val actualRequestId = requestId ?: sessionId
        
        val session = TraceSession(
            id = sessionId,
            requestId = actualRequestId,
            operationType = operationType,
            startTime = System.currentTimeMillis(),
            input = input,
            metadata = metadata,
            status = TraceStatus.RUNNING,
            spans = mutableListOf(),
            events = mutableListOf()
        )
        
        // 添加到活动会话
        activeSessions[sessionId] = session
        
        // 增加计数器
        totalSessionsCreated.incrementAndGet()
        
        // 记录开始事件
        addTraceEvent(sessionId, TraceEventType.SESSION_STARTED, "追踪会话开始")
        
        // 通知监听器
        notifyTraceSessionStarted(session)
        
        Log.d(TAG, "开始追踪会话: $sessionId ($operationType)")
        
        return session
    }
    
    /**
     * 结束追踪会话
     */
    fun endTraceSession(
        sessionId: String,
        success: Boolean,
        result: Any? = null,
        errorMessage: String? = null
    ) {
        val session = activeSessions.remove(sessionId)
        if (session == null) {
            Log.w(TAG, "追踪会话不存在: $sessionId")
            return
        }
        
        // 更新会话状态
        val endTime = System.currentTimeMillis()
        val duration = endTime - session.startTime
        
        val updatedSession = session.copy(
            endTime = endTime,
            duration = duration,
            result = result,
            errorMessage = errorMessage,
            status = if (success) TraceStatus.SUCCESS else TraceStatus.FAILED
        )
        
        // 添加到已完成会话
        completedSessions.add(updatedSession)
        
        // 增加计数器
        totalSessionsCompleted.incrementAndGet()
        
        // 记录结束事件
        val eventType = if (success) TraceEventType.SESSION_COMPLETED_SUCCESS else TraceEventType.SESSION_COMPLETED_ERROR
        val eventMessage = if (success) {
            "追踪会话成功完成"
        } else {
            "追踪会话失败: $errorMessage"
        }
        addTraceEvent(sessionId, eventType, eventMessage)
        
        // 通知监听器
        notifyTraceSessionCompleted(updatedSession)
        
        Log.d(TAG, "结束追踪会话: $sessionId (成功: $success, 耗时: ${duration}ms)")
    }
    
    /**
     * 开始追踪跨度
     */
    fun startSpan(
        sessionId: String,
        spanName: String,
        spanType: SpanType = SpanType.CUSTOM,
        parentSpanId: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ): String? {
        val session = activeSessions[sessionId] ?: return null
        
        val spanId = generateSpanId()
        val span = TraceSpan(
            id = spanId,
            name = spanName,
            type = spanType,
            parentSpanId = parentSpanId,
            startTime = System.currentTimeMillis(),
            metadata = metadata,
            status = SpanStatus.RUNNING,
            events = mutableListOf()
        )
        
        // 添加到会话
        synchronized(session.spans) {
            session.spans.add(span)
        }
        
        // 记录开始事件
        addSpanEvent(sessionId, spanId, SpanEventType.SPAN_STARTED, "跨度开始: $spanName")
        
        Log.d(TAG, "开始跨度: $spanName ($spanId) 在会话 $sessionId")
        
        return spanId
    }
    
    /**
     * 结束追踪跨度
     */
    fun endSpan(
        sessionId: String,
        spanId: String,
        success: Boolean = true,
        result: Any? = null,
        errorMessage: String? = null
    ) {
        val session = activeSessions[sessionId] ?: return
        
        // 查找跨度
        val span = synchronized(session.spans) {
            session.spans.find { it.id == spanId }
        }
        
        if (span == null) {
            Log.w(TAG, "跨度不存在: $spanId 在会话 $sessionId")
            return
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - span.startTime
        
        // 更新跨度状态
        val updatedSpan = span.copy(
            endTime = endTime,
            duration = duration,
            result = result,
            errorMessage = errorMessage,
            status = if (success) SpanStatus.SUCCESS else SpanStatus.FAILED
        )
        
        // 更新会话中的跨度
        synchronized(session.spans) {
            val index = session.spans.indexOfFirst { it.id == spanId }
            if (index >= 0) {
                session.spans[index] = updatedSpan
            }
        }
        
        // 记录结束事件
        val eventType = if (success) SpanEventType.SPAN_COMPLETED_SUCCESS else SpanEventType.SPAN_COMPLETED_ERROR
        val eventMessage = if (success) {
            "跨度成功完成: ${span.name}"
        } else {
            "跨度失败: ${span.name} - $errorMessage"
        }
        addSpanEvent(sessionId, spanId, eventType, eventMessage)
        
        Log.d(TAG, "结束跨度: ${span.name} ($spanId) (成功: $success, 耗时: ${duration}ms)")
    }
    
    /**
     * 记录追踪事件
     */
    fun addTraceEvent(
        sessionId: String,
        eventType: TraceEventType,
        message: String,
        data: Map<String, Any> = emptyMap()
    ) {
        val session = activeSessions[sessionId] ?: return
        
        val event = TraceEvent(
            id = generateEventId(),
            timestamp = System.currentTimeMillis(),
            type = eventType,
            message = message,
            data = data
        )
        
        synchronized(session.events) {
            session.events.add(event)
        }
        
        Log.d(TAG, "记录追踪事件: $eventType - $message")
    }
    
    /**
     * 记录跨度事件
     */
    fun addSpanEvent(
        sessionId: String,
        spanId: String,
        eventType: SpanEventType,
        message: String,
        data: Map<String, Any> = emptyMap()
    ) {
        val session = activeSessions[sessionId] ?: return
        
        val event = SpanEvent(
            id = generateEventId(),
            timestamp = System.currentTimeMillis(),
            type = eventType,
            message = message,
            data = data
        )
        
        // 添加到跨度事件
        synchronized(session.spans) {
            val span = session.spans.find { it.id == spanId }
            span?.let {
                synchronized(it.events) {
                    it.events.add(event)
                }
            }
        }
        
        Log.d(TAG, "记录跨度事件: $eventType - $message")
    }
    
    /**
     * 记录性能指标
     */
    fun recordPerformanceMetric(
        sessionId: String,
        metricName: String,
        value: Double,
        unit: String = "",
        metadata: Map<String, Any> = emptyMap()
    ) {
        val session = activeSessions[sessionId] ?: return
        
        val metric = PerformanceMetric(
            name = metricName,
            value = value,
            unit = unit,
            timestamp = System.currentTimeMillis(),
            metadata = metadata
        )
        
        // 添加到会话的性能指标
        synchronized(session.performanceMetrics) {
            session.performanceMetrics.add(metric)
        }
        
        Log.d(TAG, "记录性能指标: $metricName = $value $unit")
    }
    
    /**
     * 获取活动会话
     */
    fun getActiveSession(sessionId: String): TraceSession? {
        return activeSessions[sessionId]
    }
    
    /**
     * 获取所有活动会话
     */
    fun getAllActiveSessions(): List<TraceSession> {
        return activeSessions.values.toList()
    }
    
    /**
     * 获取已完成会话
     */
    fun getCompletedSession(sessionId: String): TraceSession? {
        return completedSessions.find { it.id == sessionId }
    }
    
    /**
     * 获取所有已完成会话
     */
    fun getAllCompletedSessions(): List<TraceSession> {
        return completedSessions.toList()
    }
    
    /**
     * 获取追踪统计
     */
    fun getTraceStatistics(): TraceStatistics {
        val now = System.currentTimeMillis()
        val last24Hours = now - 24 * 60 * 60 * 1000L
        
        val recentSessions = completedSessions.filter { it.startTime > last24Hours }
        
        val operationTypeDistribution = completedSessions.groupBy { it.operationType }.mapValues { it.value.size }
        val statusDistribution = completedSessions.groupBy { it.status }.mapValues { it.value.size }
        
        val averageDuration = if (completedSessions.isNotEmpty()) {
            completedSessions.mapNotNull { it.duration }.average()
        } else {
            0.0
        }
        
        return TraceStatistics(
            totalSessionsCreated = totalSessionsCreated.get(),
            totalSessionsCompleted = totalSessionsCompleted.get(),
            activeSessionsCount = activeSessions.size,
            completedSessionsCount = completedSessions.size,
            recent24HoursSessions = recentSessions.size,
            operationTypeDistribution = operationTypeDistribution,
            statusDistribution = statusDistribution,
            averageDuration = averageDuration,
            lastUpdated = now
        )
    }
    
    /**
     * 导出追踪数据
     */
    suspend fun exportTraceData(
        sessionId: String? = null,
        outputPath: String? = null
    ): String = withContext(Dispatchers.IO) {
        val sessions = if (sessionId != null) {
            listOfNotNull(getActiveSession(sessionId), getCompletedSession(sessionId))
        } else {
            getAllActiveSessions() + getAllCompletedSessions()
        }
        
        val exportData = mapOf(
            "sessions" to sessions.map { it.toMap() },
            "metadata" to mapOf(
                "exportedAt" to System.currentTimeMillis(),
                "totalSessions" to sessions.size,
                "version" to "1.0"
            )
        )
        
        JSONObject(exportData).toString(2)
    }
    
    /**
     * 清理超时会话
     */
    fun cleanupTimeoutSessions() {
        val now = System.currentTimeMillis()
        val timeoutSessions = activeSessions.filter { (_, session) ->
            now - session.startTime > TRACE_SESSION_TIMEOUT
        }
        
        timeoutSessions.forEach { (sessionId, session) ->
            // 结束超时会话
            val updatedSession = session.copy(
                endTime = now,
                duration = now - session.startTime,
                status = TraceStatus.TIMEOUT,
                errorMessage = "会话超时"
            )
            
            completedSessions.add(updatedSession)
            activeSessions.remove(sessionId)
            
            Log.w(TAG, "清理超时会话: $sessionId")
        }
    }
    
    /**
     * 添加追踪监听器
     */
    fun addTraceListener(listener: TraceListener) {
        traceListeners.add(listener)
    }
    
    /**
     * 移除追踪监听器
     */
    fun removeTraceListener(listener: TraceListener) {
        traceListeners.remove(listener)
    }
    
    /**
     * 启动定期清理任务
     */
    private fun startPeriodicCleanup() {
        scope.launch {
            while (isActive) {
                try {
                    cleanupTimeoutSessions()
                    delay(60 * 1000L) // 每分钟检查一次
                } catch (e: Exception) {
                    Log.e(TAG, "定期清理任务异常", e)
                    delay(60 * 1000L) // 错误后等待1分钟再重试
                }
            }
        }
    }
    
    /**
     * 通知追踪会话开始
     */
    private fun notifyTraceSessionStarted(session: TraceSession) {
        traceListeners.forEach { it.onTraceSessionStarted(session) }
    }
    
    /**
     * 通知追踪会话完成
     */
    private fun notifyTraceSessionCompleted(session: TraceSession) {
        traceListeners.forEach { it.onTraceSessionCompleted(session) }
    }
    
    /**
     * 生成会话ID
     */
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    /**
     * 生成跨度ID
     */
    private fun generateSpanId(): String {
        return "span_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    /**
     * 生成事件ID
     */
    private fun generateEventId(): String {
        return "event_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    // 数据类定义
    data class TraceSession(
        val id: String,
        val requestId: String,
        val operationType: String,
        val startTime: Long,
        val endTime: Long? = null,
        val duration: Long? = null,
        val input: Any? = null,
        val result: Any? = null,
        val errorMessage: String? = null,
        val metadata: Map<String, Any>,
        val status: TraceStatus,
        val spans: MutableList<TraceSpan>,
        val events: MutableList<TraceEvent>,
        val performanceMetrics: MutableList<PerformanceMetric> = mutableListOf()
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "id" to id,
                "requestId" to requestId,
                "operationType" to operationType,
                "startTime" to startTime,
                "endTime" to endTime,
                "duration" to duration,
                "status" to status.name,
                "metadata" to metadata,
                "spans" to spans.map { it.toMap() },
                "events" to events.map { it.toMap() },
                "performanceMetrics" to performanceMetrics.map { it.toMap() }
            )
        }
    }
    
    data class TraceSpan(
        val id: String,
        val name: String,
        val type: SpanType,
        val parentSpanId: String? = null,
        val startTime: Long,
        val endTime: Long? = null,
        val duration: Long? = null,
        val result: Any? = null,
        val errorMessage: String? = null,
        val metadata: Map<String, Any>,
        val status: SpanStatus,
        val events: MutableList<SpanEvent>
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "id" to id,
                "name" to name,
                "type" to type.name,
                "parentSpanId" to parentSpanId,
                "startTime" to startTime,
                "endTime" to endTime,
                "duration" to duration,
                "status" to status.name,
                "metadata" to metadata,
                "events" to events.map { it.toMap() }
            )
        }
    }
    
    data class TraceEvent(
        val id: String,
        val timestamp: Long,
        val type: TraceEventType,
        val message: String,
        val data: Map<String, Any>
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "id" to id,
                "timestamp" to timestamp,
                "type" to type.name,
                "message" to message,
                "data" to data
            )
        }
    }
    
    data class SpanEvent(
        val id: String,
        val timestamp: Long,
        val type: SpanEventType,
        val message: String,
        val data: Map<String, Any>
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "id" to id,
                "timestamp" to timestamp,
                "type" to type.name,
                "message" to message,
                "data" to data
            )
        }
    }
    
    data class PerformanceMetric(
        val name: String,
        val value: Double,
        val unit: String,
        val timestamp: Long,
        val metadata: Map<String, Any>
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "name" to name,
                "value" to value,
                "unit" to unit,
                "timestamp" to timestamp,
                "metadata" to metadata
            )
        }
    }
    
    data class TraceStatistics(
        val totalSessionsCreated: Long,
        val totalSessionsCompleted: Long,
        val activeSessionsCount: Int,
        val completedSessionsCount: Int,
        val recent24HoursSessions: Int,
        val operationTypeDistribution: Map<String, Int>,
        val statusDistribution: Map<TraceStatus, Int>,
        val averageDuration: Double,
        val lastUpdated: Long
    )
    
    enum class TraceStatus {
        RUNNING,   // 运行中
        SUCCESS,   // 成功完成
        FAILED,    // 失败
        TIMEOUT    // 超时
    }
    
    enum class SpanType {
        PREPROCESSING,    // 预处理
        PARSING,          // 解析
        VALIDATION,       // 验证
        POSTPROCESSING,   // 后处理
        CACHE_ACCESS,     // 缓存访问
        NETWORK_REQUEST,  // 网络请求
        DATABASE_ACCESS,  // 数据库访问
        CUSTOM           // 自定义
    }
    
    enum class SpanStatus {
        RUNNING,   // 运行中
        SUCCESS,   // 成功完成
        FAILED,    // 失败
        CANCELLED  // 已取消
    }
    
    enum class TraceEventType {
        SESSION_STARTED,              // 会话开始
        SESSION_COMPLETED_SUCCESS,     // 会话成功完成
        SESSION_COMPLETED_ERROR,       // 会话错误完成
        SESSION_CANCELLED,             // 会话取消
        CUSTOM                        // 自定义事件
    }
    
    enum class SpanEventType {
        SPAN_STARTED,                 // 跨度开始
        SPAN_COMPLETED_SUCCESS,       // 跨度成功完成
        SPAN_COMPLETED_ERROR,         // 跨度错误完成
        SPAN_CANCELLED,               // 跨度取消
        CUSTOM                        // 自定义事件
    }
    
    /**
     * 追踪监听器接口
     */
    interface TraceListener {
        fun onTraceSessionStarted(session: TraceSession) {}
        fun onTraceSessionCompleted(session: TraceSession) {}
    }
}