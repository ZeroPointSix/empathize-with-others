package com.empathy.ai.data.integration

import android.content.Context
import android.util.Log
import com.empathy.ai.data.observability.ObservabilityManager
import com.empathy.ai.data.parser.ResponseParserFacade
import com.empathy.ai.data.parser.AiResponseParserFactory
import kotlinx.coroutines.*

/**
 * AI响应解析器集成管理器
 * 
 * 负责统一管理和初始化所有监控和学习系统组件
 * 提供简单的API来启用和使用增强功能
 * 
 * 功能：
 * 1. 一键初始化所有组件
 * 2. 提供配置驱动的功能开关
 * 3. 管理组件生命周期
 * 4. 提供向后兼容性
 */
class AiResponseParserIntegrationManager private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "AiResponseParserIntegrationManager"
        
        @Volatile
        private var INSTANCE: AiResponseParserIntegrationManager? = null
        
        fun getInstance(context: Context): AiResponseParserIntegrationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AiResponseParserIntegrationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val observabilityManager = ObservabilityManager.getInstance(context)
    
    private var isInitialized = false
    private var isStarted = false
    
    // 配置
    private var enableEnhancedFeatures = true
    private var enableMonitoring = true
    private var enableLearning = true
    private var enableAlerting = true
    private var enableObservability = true
    
    // 解析器实例
    private var enhancedParserFacade: ResponseParserFacade? = null
    private var standardParserFacade: ResponseParserFacade? = null
    
    init {
        Log.i(TAG, "AiResponseParserIntegrationManager 初始化完成")
    }
    
    /**
     * 初始化集成管理器
     */
    fun initialize(
        enableEnhancedFeatures: Boolean = true,
        enableMonitoring: Boolean = true,
        enableLearning: Boolean = true,
        enableAlerting: Boolean = true,
        enableObservability: Boolean = true
    ) {
        if (isInitialized) {
            Log.w(TAG, "集成管理器已初始化")
            return
        }
        
        try {
            // 保存配置
            this.enableEnhancedFeatures = enableEnhancedFeatures
            this.enableMonitoring = enableMonitoring
            this.enableLearning = enableLearning
            this.enableAlerting = enableAlerting
            this.enableObservability = enableObservability
            
            // 初始化可观测性管理器
            if (enableEnhancedFeatures) {
                observabilityManager.initialize()
                
                // 更新配置
                val config = observabilityManager.getConfiguration().copy(
                    enableMonitoring = enableMonitoring,
                    enableLearning = enableLearning,
                    enableAlerting = enableAlerting,
                    enableObservability = enableObservability
                )
                observabilityManager.updateConfiguration(config)
            }
            
            // 创建解析器实例
            createParserInstances()
            
            isInitialized = true
            
            Log.i(TAG, "集成管理器初始化完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "集成管理器初始化失败", e)
            throw e
        }
    }
    
    /**
     * 启动所有组件
     */
    fun start() {
        if (!isInitialized) {
            Log.e(TAG, "集成管理器未初始化，无法启动")
            return
        }
        
        if (isStarted) {
            Log.w(TAG, "集成管理器已启动")
            return
        }
        
        try {
            // 启动可观测性管理器
            if (enableEnhancedFeatures) {
                observabilityManager.start()
            }
            
            isStarted = true
            
            Log.i(TAG, "集成管理器已启动")
            
        } catch (e: Exception) {
            Log.e(TAG, "集成管理器启动失败", e)
            throw e
        }
    }
    
    /**
     * 停止所有组件
     */
    fun stop() {
        if (!isStarted) {
            Log.w(TAG, "集成管理器未启动")
            return
        }
        
        try {
            // 停止可观测性管理器
            if (enableEnhancedFeatures) {
                observabilityManager.stop()
            }
            
            isStarted = false
            
            Log.i(TAG, "集成管理器已停止")
            
        } catch (e: Exception) {
            Log.e(TAG, "集成管理器停止失败", e)
            throw e
        }
    }
    
    /**
     * 获取增强版解析器门面
     */
    fun getEnhancedParserFacade(): ResponseParserFacade? {
        return if (enableEnhancedFeatures) {
            enhancedParserFacade
        } else {
            Log.w(TAG, "增强功能未启用，返回null")
            null
        }
    }
    
    /**
     * 获取标准解析器门面（向后兼容）
     */
    fun getStandardParserFacade(): ResponseParserFacade {
        return standardParserFacade ?: ResponseParserFacade.createDefault()
    }
    
    /**
     * 获取推荐的解析器门面
     * 
     * 根据配置返回最适合的解析器实例
     */
    fun getRecommendedParserFacade(): ResponseParserFacade {
        return if (enableEnhancedFeatures) {
            enhancedParserFacade ?: getStandardParserFacade()
        } else {
            getStandardParserFacade()
        }
    }
    
    /**
     * 获取可观测性管理器
     */
    fun getObservabilityManager(): ObservabilityManager {
        return observabilityManager
    }
    
    /**
     * 获取综合状态报告
     */
    fun getComprehensiveStatusReport() = observabilityManager.getComprehensiveStatusReport()
    
    /**
     * 导出所有数据
     */
    suspend fun exportAllData(outputPath: String? = null): String {
        return observabilityManager.exportAllData(outputPath)
    }
    
    /**
     * 更新配置
     */
    fun updateConfiguration(
        enableEnhancedFeatures: Boolean? = null,
        enableMonitoring: Boolean? = null,
        enableLearning: Boolean? = null,
        enableAlerting: Boolean? = null,
        enableObservability: Boolean? = null
    ) {
        val wasEnhancedEnabled = this.enableEnhancedFeatures
        val wasStarted = isStarted
        
        // 更新配置
        enableEnhancedFeatures?.let { this.enableEnhancedFeatures = it }
        enableMonitoring?.let { this.enableMonitoring = it }
        enableLearning?.let { this.enableLearning = it }
        enableAlerting?.let { this.enableAlerting = it }
        enableObservability?.let { this.enableObservability = it }
        
        try {
            // 如果增强功能状态发生变化
            if (wasEnhancedEnabled != this.enableEnhancedFeatures) {
                if (wasStarted) {
                    // 先停止
                    stop()
                }
                
                // 重新初始化
                createParserInstances()
                
                if (wasStarted) {
                    // 重新启动
                    start()
                }
            } else if (this.enableEnhancedFeatures) {
                // 更新可观测性配置
                val config = observabilityManager.getConfiguration().copy(
                    enableMonitoring = this.enableMonitoring,
                    enableLearning = this.enableLearning,
                    enableAlerting = this.enableAlerting,
                    enableObservability = this.enableObservability
                )
                observabilityManager.updateConfiguration(config)
            }
            
            Log.i(TAG, "配置已更新")
            
        } catch (e: Exception) {
            Log.e(TAG, "配置更新失败", e)
            throw e
        }
    }
    
    /**
     * 获取当前配置
     */
    fun getCurrentConfiguration(): IntegrationConfiguration {
        return IntegrationConfiguration(
            enableEnhancedFeatures = enableEnhancedFeatures,
            enableMonitoring = enableMonitoring,
            enableLearning = enableLearning,
            enableAlerting = enableAlerting,
            enableObservability = enableObservability,
            isInitialized = isInitialized,
            isStarted = isStarted
        )
    }
    
    /**
     * 创建解析器实例
     */
    private fun createParserInstances() {
        // 创建标准解析器（用于向后兼容）
        standardParserFacade = ResponseParserFacade.createDefault()
        
        // 创建增强版解析器
        if (enableEnhancedFeatures) {
            enhancedParserFacade = ResponseParserFacade.createEnhancedDefault(context)
        }
        
        Log.d(TAG, "解析器实例创建完成")
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        try {
            if (isStarted) {
                stop()
            }
            
            scope.cancel()
            
            Log.i(TAG, "资源清理完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "资源清理失败", e)
        }
    }
    
    /**
     * 集成配置数据类
     */
    data class IntegrationConfiguration(
        val enableEnhancedFeatures: Boolean,
        val enableMonitoring: Boolean,
        val enableLearning: Boolean,
        val enableAlerting: Boolean,
        val enableObservability: Boolean,
        val isInitialized: Boolean,
        val isStarted: Boolean
    )
    
    /**
     * 集成监听器接口
     */
    interface IntegrationListener {
        fun onInitialized() {}
        fun onStarted() {}
        fun onStopped() {}
        fun onConfigurationChanged(oldConfig: IntegrationConfiguration, newConfig: IntegrationConfiguration) {}
    }
    
    /**
     * 静态工具方法
     */
    object Utils {
        /**
         * 快速初始化并获取增强版解析器
         */
        fun quickStartEnhancedParser(context: Context): ResponseParserFacade {
            val manager = getInstance(context)
            manager.initialize(
                enableEnhancedFeatures = true,
                enableMonitoring = true,
                enableLearning = true,
                enableAlerting = true,
                enableObservability = true
            )
            manager.start()
            
            return manager.getEnhancedParserFacade() ?: manager.getStandardParserFacade()
        }
        
        /**
         * 快速初始化并获取标准解析器
         */
        fun quickStartStandardParser(context: Context): ResponseParserFacade {
            val manager = getInstance(context)
            manager.initialize(
                enableEnhancedFeatures = false,
                enableMonitoring = false,
                enableLearning = false,
                enableAlerting = false,
                enableObservability = false
            )
            manager.start()
            
            return manager.getStandardParserFacade()
        }
        
        /**
         * 检查是否支持增强功能
         */
        fun isEnhancedFeaturesSupported(): Boolean {
            return try {
                Class.forName("com.empathy.ai.data.observability.ObservabilityManager")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
        }
    }
}