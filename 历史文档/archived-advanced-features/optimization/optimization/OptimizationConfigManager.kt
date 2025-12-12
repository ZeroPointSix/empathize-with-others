package com.empathy.ai.data.optimization

import android.content.Context
import com.empathy.ai.data.domain.model.AiResponseParserMetrics
import com.empathy.ai.data.resource.CacheConfig
import com.empathy.ai.data.resource.PoolConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * 优化配置管理器
 * 管理配置驱动的优化策略
 */
class OptimizationConfigManager private constructor(
    private val context: Context,
    private val metrics: AiResponseParserMetrics
) {
    companion object {
        @Volatile
        private var INSTANCE: OptimizationConfigManager? = null
        
        fun getInstance(context: Context, metrics: AiResponseParserMetrics): OptimizationConfigManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OptimizationConfigManager(context.applicationContext, metrics).also { INSTANCE = it }
            }
        }
        
        private const val CONFIG_FILE_NAME = "optimization_config.json"
        private const val DEFAULT_CONFIG_VERSION = 1
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 配置状态
    private val _configState = MutableStateFlow(ConfigState())
    val configState: StateFlow<ConfigState> = _configState.asStateFlow()
    
    // 当前配置
    private val currentConfig = AtomicReference<OptimizationConfig>()
    
    // 配置模板
    private val configTemplates = ConcurrentHashMap<String, OptimizationConfig>()
    
    // 配置历史
    private val configHistory = mutableListOf<ConfigVersion>()
    
    init {
        initializeConfigTemplates()
        loadConfiguration()
        startConfigMonitoring()
    }
    
    /**
     * 初始化配置模板
     */
    private fun initializeConfigTemplates() {
        // 高性能配置模板
        configTemplates["high_performance"] = OptimizationConfig(
            name = "高性能配置",
            description = "针对高性能场景优化的配置",
            version = DEFAULT_CONFIG_VERSION,
            memoryConfig = MemoryConfig(
                enableOptimization = true,
                gcOptimizationLevel = GcOptimizationLevel.AGGRESSIVE,
                memoryPressureThreshold = 0.7f,
                cleanupInterval = 30000L
            ),
            cacheConfig = CacheConfig(
                maxSize = 2000,
                defaultExpireTime = 600000L, // 10分钟
                evictionPolicy = com.empathy.ai.data.resource.EvictionPolicy.LRU
            ),
            poolConfig = PoolConfig(
                minSize = 10,
                maxSize = 50,
                createTimeout = 3000L
            ),
            performanceConfig = PerformanceConfig(
                enableAdaptiveOptimization = true,
                optimizationInterval = 60000L, // 1分钟
                regressionDetectionThreshold = 5.0,
                enablePredictiveOptimization = true
            ),
            monitoringConfig = MonitoringConfig(
                enableContinuousMonitoring = true,
                monitoringInterval = 30000L, // 30秒
                alertThreshold = 0.8f,
                enableTrendAnalysis = true
            )
        )
        
        // 低功耗配置模板
        configTemplates["low_power"] = OptimizationConfig(
            name = "低功耗配置",
            description = "针对低功耗场景优化的配置",
            version = DEFAULT_CONFIG_VERSION,
            memoryConfig = MemoryConfig(
                enableOptimization = true,
                gcOptimizationLevel = GcOptimizationLevel.CONSERVATIVE,
                memoryPressureThreshold = 0.5f,
                cleanupInterval = 120000L
            ),
            cacheConfig = CacheConfig(
                maxSize = 500,
                defaultExpireTime = 300000L, // 5分钟
                evictionPolicy = com.empathy.ai.data.resource.EvictionPolicy.FIFO
            ),
            poolConfig = PoolConfig(
                minSize = 3,
                maxSize = 10,
                createTimeout = 10000L
            ),
            performanceConfig = PerformanceConfig(
                enableAdaptiveOptimization = false,
                optimizationInterval = 300000L, // 5分钟
                regressionDetectionThreshold = 10.0,
                enablePredictiveOptimization = false
            ),
            monitoringConfig = MonitoringConfig(
                enableContinuousMonitoring = true,
                monitoringInterval = 120000L, // 2分钟
                alertThreshold = 0.6f,
                enableTrendAnalysis = false
            )
        )
        
        // 平衡配置模板
        configTemplates["balanced"] = OptimizationConfig(
            name = "平衡配置",
            description = "平衡性能和资源使用的配置",
            version = DEFAULT_CONFIG_VERSION,
            memoryConfig = MemoryConfig(
                enableOptimization = true,
                gcOptimizationLevel = GcOptimizationLevel.MODERATE,
                memoryPressureThreshold = 0.6f,
                cleanupInterval = 60000L
            ),
            cacheConfig = CacheConfig(
                maxSize = 1000,
                defaultExpireTime = 300000L, // 5分钟
                evictionPolicy = com.empathy.ai.data.resource.EvictionPolicy.LRU
            ),
            poolConfig = PoolConfig(
                minSize = 5,
                maxSize = 20,
                createTimeout = 5000L
            ),
            performanceConfig = PerformanceConfig(
                enableAdaptiveOptimization = true,
                optimizationInterval = 120000L, // 2分钟
                regressionDetectionThreshold = 7.5,
                enablePredictiveOptimization = true
            ),
            monitoringConfig = MonitoringConfig(
                enableContinuousMonitoring = true,
                monitoringInterval = 60000L, // 1分钟
                alertThreshold = 0.7f,
                enableTrendAnalysis = true
            )
        )
        
        // 开发环境配置模板
        configTemplates["development"] = OptimizationConfig(
            name = "开发环境配置",
            description = "适用于开发环境的配置",
            version = DEFAULT_CONFIG_VERSION,
            memoryConfig = MemoryConfig(
                enableOptimization = true,
                gcOptimizationLevel = GcOptimizationLevel.DEBUG,
                memoryPressureThreshold = 0.8f,
                cleanupInterval = 15000L
            ),
            cacheConfig = CacheConfig(
                maxSize = 100,
                defaultExpireTime = 60000L, // 1分钟
                evictionPolicy = com.empathy.ai.data.resource.EvictionPolicy.LRU
            ),
            poolConfig = PoolConfig(
                minSize = 2,
                maxSize = 5,
                createTimeout = 2000L
            ),
            performanceConfig = PerformanceConfig(
                enableAdaptiveOptimization = true,
                optimizationInterval = 30000L, // 30秒
                regressionDetectionThreshold = 5.0,
                enablePredictiveOptimization = false
            ),
            monitoringConfig = MonitoringConfig(
                enableContinuousMonitoring = true,
                monitoringInterval = 15000L, // 15秒
                alertThreshold = 0.9f,
                enableTrendAnalysis = true
            )
        )
    }
    
    /**
     * 加载配置
     */
    private fun loadConfiguration() {
        try {
            val configFile = File(context.filesDir, CONFIG_FILE_NAME)
            
            if (configFile.exists()) {
                val configJson = configFile.readText()
                val config = parseConfigFromJson(configJson)
                currentConfig.set(config)
                
                metrics.recordOptimizationConfigLoaded(config.name, config.version)
            } else {
                // 使用默认配置
                val defaultConfig = configTemplates["balanced"]!!
                currentConfig.set(defaultConfig)
                saveConfiguration(defaultConfig)
                
                metrics.recordOptimizationConfigLoaded(defaultConfig.name, defaultConfig.version)
            }
            
            updateConfigState()
        } catch (e: Exception) {
            metrics.recordOptimizationConfigError("配置加载错误", e)
            
            // 使用默认配置作为后备
            val fallbackConfig = configTemplates["balanced"]!!
            currentConfig.set(fallbackConfig)
            updateConfigState()
        }
    }
    
    /**
     * 保存配置
     */
    private fun saveConfiguration(config: OptimizationConfig) {
        try {
            val configFile = File(context.filesDir, CONFIG_FILE_NAME)
            val configJson = convertConfigToJson(config)
            
            configFile.writeText(configJson)
            
            // 添加到历史记录
            configHistory.add(
                ConfigVersion(
                    version = config.version,
                    timestamp = System.currentTimeMillis(),
                    configName = config.name,
                    configJson = configJson
                )
            )
            
            // 限制历史记录数量
            if (configHistory.size > 10) {
                configHistory.removeAt(0)
            }
            
            metrics.recordOptimizationConfigSaved(config.name, config.version)
        } catch (e: Exception) {
            metrics.recordOptimizationConfigError("配置保存错误", e)
        }
    }
    
    /**
     * 启动配置监控
     */
    private fun startConfigMonitoring() {
        scope.launch {
            while (isActive) {
                try {
                    checkConfigChanges()
                    delay(60000L) // 每分钟检查一次
                } catch (e: Exception) {
                    metrics.recordOptimizationConfigError("配置监控错误", e)
                }
            }
        }
    }
    
    /**
     * 检查配置变化
     */
    private fun checkConfigChanges() {
        val configFile = File(context.filesDir, CONFIG_FILE_NAME)
        
        if (configFile.exists()) {
            val configJson = configFile.readText()
            val config = parseConfigFromJson(configJson)
            val current = currentConfig.get()
            
            if (config != current) {
                currentConfig.set(config)
                applyConfiguration(config)
                updateConfigState()
                
                metrics.recordOptimizationConfigChanged(config.name, config.version)
            }
        }
    }
    
    /**
     * 应用配置
     */
    private suspend fun applyConfiguration(config: OptimizationConfig) {
        // 应用内存配置
        applyMemoryConfig(config.memoryConfig)
        
        // 应用缓存配置
        applyCacheConfig(config.cacheConfig)
        
        // 应用资源池配置
        applyPoolConfig(config.poolConfig)
        
        // 应用性能配置
        applyPerformanceConfig(config.performanceConfig)
        
        // 应用监控配置
        applyMonitoringConfig(config.monitoringConfig)
        
        metrics.recordOptimizationConfigApplied(config.name)
    }
    
    /**
     * 应用内存配置
     */
    private suspend fun applyMemoryConfig(config: MemoryConfig) {
        // 这里应该应用实际的内存配置
        // 例如：调整内存优化器的参数
        metrics.recordMemoryConfigApplied(config.enableOptimization, config.gcOptimizationLevel.name)
    }
    
    /**
     * 应用缓存配置
     */
    private suspend fun applyCacheConfig(config: CacheConfig) {
        // 这里应该应用实际的缓存配置
        // 例如：调整缓存大小和策略
        metrics.recordCacheConfigApplied(config.maxSize, config.evictionPolicy.name)
    }
    
    /**
     * 应用资源池配置
     */
    private suspend fun applyPoolConfig(config: PoolConfig) {
        // 这里应该应用实际的资源池配置
        // 例如：调整池大小和超时时间
        metrics.recordPoolConfigApplied(config.minSize, config.maxSize)
    }
    
    /**
     * 应用性能配置
     */
    private suspend fun applyPerformanceConfig(config: PerformanceConfig) {
        // 这里应该应用实际的性能配置
        // 例如：启用/禁用自适应优化
        metrics.recordPerformanceConfigApplied(config.enableAdaptiveOptimization, config.optimizationInterval)
    }
    
    /**
     * 应用监控配置
     */
    private suspend fun applyMonitoringConfig(config: MonitoringConfig) {
        // 这里应该应用实际的监控配置
        // 例如：调整监控间隔和阈值
        metrics.recordMonitoringConfigApplied(config.enableContinuousMonitoring, config.monitoringInterval)
    }
    
    /**
     * 从JSON解析配置
     */
    private fun parseConfigFromJson(configJson: String): OptimizationConfig {
        val jsonObject = JSONObject(configJson)
        
        return OptimizationConfig(
            name = jsonObject.optString("name", "custom"),
            description = jsonObject.optString("description", "自定义配置"),
            version = jsonObject.optInt("version", DEFAULT_CONFIG_VERSION),
            memoryConfig = parseMemoryConfig(jsonObject.optJSONObject("memoryConfig")),
            cacheConfig = parseCacheConfig(jsonObject.optJSONObject("cacheConfig")),
            poolConfig = parsePoolConfig(jsonObject.optJSONObject("poolConfig")),
            performanceConfig = parsePerformanceConfig(jsonObject.optJSONObject("performanceConfig")),
            monitoringConfig = parseMonitoringConfig(jsonObject.optJSONObject("monitoringConfig"))
        )
    }
    
    /**
     * 解析内存配置
     */
    private fun parseMemoryConfig(jsonObject: JSONObject?): MemoryConfig {
        if (jsonObject == null) return MemoryConfig()
        
        return MemoryConfig(
            enableOptimization = jsonObject.optBoolean("enableOptimization", true),
            gcOptimizationLevel = GcOptimizationLevel.valueOf(
                jsonObject.optString("gcOptimizationLevel", "MODERATE")
            ),
            memoryPressureThreshold = jsonObject.optDouble("memoryPressureThreshold", 0.6f).toFloat(),
            cleanupInterval = jsonObject.optLong("cleanupInterval", 60000L)
        )
    }
    
    /**
     * 解析缓存配置
     */
    private fun parseCacheConfig(jsonObject: JSONObject?): CacheConfig {
        if (jsonObject == null) return CacheConfig()
        
        return CacheConfig(
            maxSize = jsonObject.optInt("maxSize", 1000),
            defaultExpireTime = jsonObject.optLong("defaultExpireTime", 300000L),
            evictionPolicy = com.empathy.ai.data.resource.EvictionPolicy.valueOf(
                jsonObject.optString("evictionPolicy", "LRU")
            )
        )
    }
    
    /**
     * 解析资源池配置
     */
    private fun parsePoolConfig(jsonObject: JSONObject?): PoolConfig {
        if (jsonObject == null) return PoolConfig()
        
        return PoolConfig(
            minSize = jsonObject.optInt("minSize", 5),
            maxSize = jsonObject.optInt("maxSize", 20),
            createTimeout = jsonObject.optLong("createTimeout", 5000L)
        )
    }
    
    /**
     * 解析性能配置
     */
    private fun parsePerformanceConfig(jsonObject: JSONObject?): PerformanceConfig {
        if (jsonObject == null) return PerformanceConfig()
        
        return PerformanceConfig(
            enableAdaptiveOptimization = jsonObject.optBoolean("enableAdaptiveOptimization", true),
            optimizationInterval = jsonObject.optLong("optimizationInterval", 120000L),
            regressionDetectionThreshold = jsonObject.optDouble("regressionDetectionThreshold", 7.5),
            enablePredictiveOptimization = jsonObject.optBoolean("enablePredictiveOptimization", true)
        )
    }
    
    /**
     * 解析监控配置
     */
    private fun parseMonitoringConfig(jsonObject: JSONObject?): MonitoringConfig {
        if (jsonObject == null) return MonitoringConfig()
        
        return MonitoringConfig(
            enableContinuousMonitoring = jsonObject.optBoolean("enableContinuousMonitoring", true),
            monitoringInterval = jsonObject.optLong("monitoringInterval", 60000L),
            alertThreshold = jsonObject.optDouble("alertThreshold", 0.7f).toFloat(),
            enableTrendAnalysis = jsonObject.optBoolean("enableTrendAnalysis", true)
        )
    }
    
    /**
     * 将配置转换为JSON
     */
    private fun convertConfigToJson(config: OptimizationConfig): String {
        val jsonObject = JSONObject()
        
        jsonObject.put("name", config.name)
        jsonObject.put("description", config.description)
        jsonObject.put("version", config.version)
        
        // 内存配置
        val memoryConfigJson = JSONObject()
        memoryConfigJson.put("enableOptimization", config.memoryConfig.enableOptimization)
        memoryConfigJson.put("gcOptimizationLevel", config.memoryConfig.gcOptimizationLevel.name)
        memoryConfigJson.put("memoryPressureThreshold", config.memoryConfig.memoryPressureThreshold)
        memoryConfigJson.put("cleanupInterval", config.memoryConfig.cleanupInterval)
        jsonObject.put("memoryConfig", memoryConfigJson)
        
        // 缓存配置
        val cacheConfigJson = JSONObject()
        cacheConfigJson.put("maxSize", config.cacheConfig.maxSize)
        cacheConfigJson.put("defaultExpireTime", config.cacheConfig.defaultExpireTime)
        cacheConfigJson.put("evictionPolicy", config.cacheConfig.evictionPolicy.name)
        jsonObject.put("cacheConfig", cacheConfigJson)
        
        // 资源池配置
        val poolConfigJson = JSONObject()
        poolConfigJson.put("minSize", config.poolConfig.minSize)
        poolConfigJson.put("maxSize", config.poolConfig.maxSize)
        poolConfigJson.put("createTimeout", config.poolConfig.createTimeout)
        jsonObject.put("poolConfig", poolConfigJson)
        
        // 性能配置
        val performanceConfigJson = JSONObject()
        performanceConfigJson.put("enableAdaptiveOptimization", config.performanceConfig.enableAdaptiveOptimization)
        performanceConfigJson.put("optimizationInterval", config.performanceConfig.optimizationInterval)
        performanceConfigJson.put("regressionDetectionThreshold", config.performanceConfig.regressionDetectionThreshold)
        performanceConfigJson.put("enablePredictiveOptimization", config.performanceConfig.enablePredictiveOptimization)
        jsonObject.put("performanceConfig", performanceConfigJson)
        
        // 监控配置
        val monitoringConfigJson = JSONObject()
        monitoringConfigJson.put("enableContinuousMonitoring", config.monitoringConfig.enableContinuousMonitoring)
        monitoringConfigJson.put("monitoringInterval", config.monitoringConfig.monitoringInterval)
        monitoringConfigJson.put("alertThreshold", config.monitoringConfig.alertThreshold)
        monitoringConfigJson.put("enableTrendAnalysis", config.monitoringConfig.enableTrendAnalysis)
        jsonObject.put("monitoringConfig", monitoringConfigJson)
        
        return jsonObject.toString(2)
    }
    
    /**
     * 更新配置状态
     */
    private fun updateConfigState() {
        val config = currentConfig.get()
        val currentState = _configState.value
        val newState = currentState.copy(
            currentConfigName = config.name,
            currentConfigVersion = config.version,
            availableTemplates = configTemplates.keys.toList(),
            lastUpdateTime = System.currentTimeMillis()
        )
        _configState.value = newState
    }
    
    /**
     * 获取当前配置
     */
    fun getCurrentConfiguration(): OptimizationConfig {
        return currentConfig.get() ?: configTemplates["balanced"]!!
    }
    
    /**
     * 应用配置模板
     */
    suspend fun applyConfigurationTemplate(templateName: String) {
        val template = configTemplates[templateName]
            ?: throw IllegalArgumentException("未找到配置模板: $templateName")
        
        currentConfig.set(template)
        saveConfiguration(template)
        applyConfiguration(template)
        updateConfigState()
    }
    
    /**
     * 更新配置
     */
    suspend fun updateConfiguration(config: OptimizationConfig) {
        currentConfig.set(config)
        saveConfiguration(config)
        applyConfiguration(config)
        updateConfigState()
    }
    
    /**
     * 获取配置模板
     */
    fun getConfigurationTemplate(templateName: String): OptimizationConfig? {
        return configTemplates[templateName]
    }
    
    /**
     * 获取所有配置模板
     */
    fun getAllConfigurationTemplates(): Map<String, OptimizationConfig> {
        return configTemplates.toMap()
    }
    
    /**
     * 获取配置历史
     */
    fun getConfigHistory(): List<ConfigVersion> {
        return configHistory.toList()
    }
    
    /**
     * 恢复配置版本
     */
    suspend fun restoreConfigVersion(version: Int) {
        val configVersion = configHistory.find { it.version == version }
            ?: throw IllegalArgumentException("未找到配置版本: $version")
        
        val config = parseConfigFromJson(configVersion.configJson)
        currentConfig.set(config)
        applyConfiguration(config)
        updateConfigState()
        
        metrics.recordOptimizationConfigRestored(config.name, version)
    }
    
    /**
     * 导出配置
     */
    fun exportConfiguration(): String {
        val config = currentConfig.get()
        return convertConfigToJson(config)
    }
    
    /**
     * 导入配置
     */
    suspend fun importConfiguration(configJson: String) {
        try {
            val config = parseConfigFromJson(configJson)
            currentConfig.set(config)
            saveConfiguration(config)
            applyConfiguration(config)
            updateConfigState()
            
            metrics.recordOptimizationConfigImported(config.name)
        } catch (e: Exception) {
            metrics.recordOptimizationConfigError("配置导入错误", e)
            throw IllegalArgumentException("配置格式无效", e)
        }
    }
    
    /**
     * 验证配置
     */
    fun validateConfiguration(config: OptimizationConfig): List<ConfigValidationError> {
        val errors = mutableListOf<ConfigValidationError>()
        
        // 验证内存配置
        if (config.memoryConfig.memoryPressureThreshold < 0.0f || config.memoryConfig.memoryPressureThreshold > 1.0f) {
            errors.add(
                ConfigValidationError(
                    field = "memoryPressureThreshold",
                    message = "内存压力阈值必须在0.0-1.0之间",
                    severity = ValidationErrorSeverity.ERROR
                )
            )
        }
        
        // 验证缓存配置
        if (config.cacheConfig.maxSize <= 0) {
            errors.add(
                ConfigValidationError(
                    field = "maxSize",
                    message = "缓存最大大小必须大于0",
                    severity = ValidationErrorSeverity.ERROR
                )
            )
        }
        
        // 验证资源池配置
        if (config.poolConfig.minSize >= config.poolConfig.maxSize) {
            errors.add(
                ConfigValidationError(
                    field = "poolSize",
                    message = "资源池最小大小必须小于最大大小",
                    severity = ValidationErrorSeverity.ERROR
                )
            )
        }
        
        // 验证性能配置
        if (config.performanceConfig.optimizationInterval < 1000L) {
            errors.add(
                ConfigValidationError(
                    field = "optimizationInterval",
                    message = "优化间隔不能小于1秒",
                    severity = ValidationErrorSeverity.WARNING
                )
            )
        }
        
        return errors
    }
    
    /**
     * 获取配置状态
     */
    fun getConfigState(): ConfigState {
        return _configState.value
    }
    
    /**
     * 销毁配置管理器
     */
    fun destroy() {
        scope.cancel()
        configTemplates.clear()
        configHistory.clear()
        
        INSTANCE = null
    }
}

/**
 * 优化配置
 */
data class OptimizationConfig(
    val name: String = "custom",
    val description: String = "自定义配置",
    val version: Int = 1,
    val memoryConfig: MemoryConfig = MemoryConfig(),
    val cacheConfig: CacheConfig = CacheConfig(),
    val poolConfig: PoolConfig = PoolConfig(),
    val performanceConfig: PerformanceConfig = PerformanceConfig(),
    val monitoringConfig: MonitoringConfig = MonitoringConfig()
)

/**
 * 内存配置
 */
data class MemoryConfig(
    val enableOptimization: Boolean = true,
    val gcOptimizationLevel: GcOptimizationLevel = GcOptimizationLevel.MODERATE,
    val memoryPressureThreshold: Float = 0.6f,
    val cleanupInterval: Long = 60000L
)

/**
 * 性能配置
 */
data class PerformanceConfig(
    val enableAdaptiveOptimization: Boolean = true,
    val optimizationInterval: Long = 120000L,
    val regressionDetectionThreshold: Double = 7.5,
    val enablePredictiveOptimization: Boolean = true
)

/**
 * 监控配置
 */
data class MonitoringConfig(
    val enableContinuousMonitoring: Boolean = true,
    val monitoringInterval: Long = 60000L,
    val alertThreshold: Float = 0.7f,
    val enableTrendAnalysis: Boolean = true
)

/**
 * 配置状态
 */
data class ConfigState(
    val currentConfigName: String = "balanced",
    val currentConfigVersion: Int = 1,
    val availableTemplates: List<String> = emptyList(),
    val lastUpdateTime: Long = 0L
)

/**
 * 配置版本
 */
data class ConfigVersion(
    val version: Int,
    val timestamp: Long,
    val configName: String,
    val configJson: String
)

/**
 * 配置验证错误
 */
data class ConfigValidationError(
    val field: String,
    val message: String,
    val severity: ValidationErrorSeverity
)

/**
 * GC优化级别
 */
enum class GcOptimizationLevel {
    CONSERVATIVE,
    MODERATE,
    AGGRESSIVE,
    DEBUG
}

/**
 * 验证错误严重程度
 */
enum class ValidationErrorSeverity {
    WARNING,
    ERROR,
    CRITICAL
}

/**
 * 配置工厂
 */
object ConfigFactory {
    
    /**
     * 创建自定义配置
     */
    fun createCustomConfig(
        name: String,
        description: String = "自定义配置",
        builder: ConfigBuilder.() -> Unit
    ): OptimizationConfig {
        val configBuilder = ConfigBuilder(name, description)
        configBuilder.builder()
        return configBuilder.build()
    }
}

/**
 * 配置构建器
 */
class ConfigBuilder(
    private val name: String,
    private val description: String
) {
    private var memoryConfig: MemoryConfig = MemoryConfig()
    private var cacheConfig: CacheConfig = CacheConfig()
    private var poolConfig: PoolConfig = PoolConfig()
    private var performanceConfig: PerformanceConfig = PerformanceConfig()
    private var monitoringConfig: MonitoringConfig = MonitoringConfig()
    
    fun memoryConfig(config: MemoryConfig) {
        this.memoryConfig = config
    }
    
    fun cacheConfig(config: CacheConfig) {
        this.cacheConfig = config
    }
    
    fun poolConfig(config: PoolConfig) {
        this.poolConfig = config
    }
    
    fun performanceConfig(config: PerformanceConfig) {
        this.performanceConfig = config
    }
    
    fun monitoringConfig(config: MonitoringConfig) {
        this.monitoringConfig = config
    }
    
    fun build(): OptimizationConfig {
        return OptimizationConfig(
            name = name,
            description = description,
            memoryConfig = memoryConfig,
            cacheConfig = cacheConfig,
            poolConfig = poolConfig,
            performanceConfig = performanceConfig,
            monitoringConfig = monitoringConfig
        )
    }
}