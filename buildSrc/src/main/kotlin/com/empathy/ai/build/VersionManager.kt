package com.empathy.ai.build

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.util.Properties

/**
 * 版本管理器
 * 负责读取和更新gradle.properties中的版本信息
 * 
 * @property projectDir 项目根目录
 * 
 * @see TDD-00024 4.2.3 VersionManager版本管理器
 */
class VersionManager(private val projectDir: File) {
    
    private val gradlePropertiesFile = File(projectDir, "gradle.properties")
    private val versionHistoryFile = File(projectDir, "config/version-history.json")
    
    companion object {
        private const val KEY_VERSION_NAME = "APP_VERSION_NAME"
        private const val KEY_VERSION_CODE = "APP_VERSION_CODE"
        private const val KEY_RELEASE_STAGE = "APP_RELEASE_STAGE"
    }
    
    private val json = Json { 
        prettyPrint = true 
        ignoreUnknownKeys = true
    }
    
    /**
     * 获取当前版本
     * @return 当前版本号
     * @throws IllegalStateException 如果版本配置不存在
     */
    fun getCurrentVersion(): SemanticVersion {
        if (!gradlePropertiesFile.exists()) {
            throw IllegalStateException("gradle.properties not found at: ${gradlePropertiesFile.absolutePath}")
        }
        
        val properties = Properties().apply {
            gradlePropertiesFile.inputStream().use { load(it) }
        }
        
        val versionName = properties.getProperty(KEY_VERSION_NAME)
            ?: throw IllegalStateException("$KEY_VERSION_NAME not found in gradle.properties")
        
        return SemanticVersion.parse(versionName)
    }
    
    /**
     * 安全获取当前版本，失败时返回默认版本
     */
    fun getCurrentVersionOrDefault(): SemanticVersion {
        return try {
            getCurrentVersion()
        } catch (e: Exception) {
            SemanticVersion.DEFAULT
        }
    }
    
    /**
     * 获取当前发布阶段
     */
    fun getCurrentStage(): ReleaseStage {
        if (!gradlePropertiesFile.exists()) {
            return ReleaseStage.DEFAULT
        }
        
        val properties = Properties().apply {
            gradlePropertiesFile.inputStream().use { load(it) }
        }
        
        val stageName = properties.getProperty(KEY_RELEASE_STAGE) ?: return ReleaseStage.DEFAULT
        return ReleaseStage.fromString(stageName)
    }
    
    /**
     * 获取当前版本代码
     */
    fun getCurrentVersionCode(): Int {
        if (!gradlePropertiesFile.exists()) {
            return SemanticVersion.DEFAULT.toVersionCode()
        }
        
        val properties = Properties().apply {
            gradlePropertiesFile.inputStream().use { load(it) }
        }
        
        return properties.getProperty(KEY_VERSION_CODE)?.toIntOrNull()
            ?: getCurrentVersionOrDefault().toVersionCode()
    }
    
    /**
     * 更新版本号
     * @param newVersion 新版本号
     * @param stage 发布阶段
     */
    fun updateVersion(newVersion: SemanticVersion, stage: ReleaseStage) {
        if (!gradlePropertiesFile.exists()) {
            throw IllegalStateException("gradle.properties not found")
        }
        
        val content = gradlePropertiesFile.readText()
        val versionCode = newVersion.toVersionCode()
        
        var updatedContent = content
        
        // 更新版本名称
        updatedContent = if (updatedContent.contains("$KEY_VERSION_NAME=")) {
            updatedContent.replace(
                Regex("""$KEY_VERSION_NAME=.+"""),
                "$KEY_VERSION_NAME=$newVersion"
            )
        } else {
            "$updatedContent\n$KEY_VERSION_NAME=$newVersion"
        }
        
        // 更新版本代码
        updatedContent = if (updatedContent.contains("$KEY_VERSION_CODE=")) {
            updatedContent.replace(
                Regex("""$KEY_VERSION_CODE=\d+"""),
                "$KEY_VERSION_CODE=$versionCode"
            )
        } else {
            "$updatedContent\n$KEY_VERSION_CODE=$versionCode"
        }
        
        // 更新发布阶段
        updatedContent = if (updatedContent.contains("$KEY_RELEASE_STAGE=")) {
            updatedContent.replace(
                Regex("""$KEY_RELEASE_STAGE=.+"""),
                "$KEY_RELEASE_STAGE=${stage.name.lowercase()}"
            )
        } else {
            "$updatedContent\n$KEY_RELEASE_STAGE=${stage.name.lowercase()}"
        }
        
        gradlePropertiesFile.writeText(updatedContent)
    }
    
    /**
     * 更新版本历史记录
     */
    fun updateVersionHistory(
        newVersion: SemanticVersion,
        stage: ReleaseStage,
        commits: List<ParsedCommit>
    ) {
        val historyDir = versionHistoryFile.parentFile
        if (!historyDir.exists()) {
            historyDir.mkdirs()
        }
        
        val history = if (versionHistoryFile.exists()) {
            try {
                json.decodeFromString<VersionHistory>(versionHistoryFile.readText())
            } catch (e: Exception) {
                VersionHistory()
            }
        } else {
            VersionHistory()
        }
        
        val changelog = VersionCalculator().generateChangelog(commits)
        val now = Instant.now().toString()
        
        val newEntry = VersionHistoryEntry(
            version = newVersion.toString(),
            versionCode = newVersion.toVersionCode(),
            stage = stage.name.lowercase(),
            date = now,
            commits = commits.map { it.subject },
            changelog = changelog
        )
        
        val updatedHistory = history.copy(
            currentVersion = VersionInfo(
                version = newVersion.toString(),
                versionCode = newVersion.toVersionCode(),
                stage = stage.name.lowercase(),
                updatedAt = now
            ),
            history = listOf(newEntry) + history.history.take(49) // 保留最近50条
        )
        
        versionHistoryFile.writeText(json.encodeToString(VersionHistory.serializer(), updatedHistory))
    }
    
    /**
     * 获取版本历史
     */
    fun getVersionHistory(): VersionHistory {
        if (!versionHistoryFile.exists()) {
            return VersionHistory()
        }
        
        return try {
            json.decodeFromString(versionHistoryFile.readText())
        } catch (e: Exception) {
            VersionHistory()
        }
    }
    
    /**
     * 检查版本配置是否有效
     */
    fun isConfigValid(): Boolean {
        return try {
            getCurrentVersion()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取版本信息摘要
     */
    fun getVersionSummary(): String {
        return try {
            val version = getCurrentVersion()
            val stage = getCurrentStage()
            val code = getCurrentVersionCode()
            """
            |版本信息:
            |  版本名称: $version
            |  版本代码: $code
            |  发布阶段: ${stage.displayName}
            """.trimMargin()
        } catch (e: Exception) {
            "无法获取版本信息: ${e.message}"
        }
    }
}

/**
 * 版本历史数据结构
 */
@Serializable
data class VersionHistory(
    val schemaVersion: Int = 1,
    val currentVersion: VersionInfo? = null,
    val history: List<VersionHistoryEntry> = emptyList()
)

/**
 * 当前版本信息
 */
@Serializable
data class VersionInfo(
    val version: String,
    val versionCode: Int,
    val stage: String,
    val updatedAt: String
)

/**
 * 版本历史条目
 */
@Serializable
data class VersionHistoryEntry(
    val version: String,
    val versionCode: Int,
    val stage: String,
    val date: String,
    val commits: List<String>,
    val changelog: String
)
