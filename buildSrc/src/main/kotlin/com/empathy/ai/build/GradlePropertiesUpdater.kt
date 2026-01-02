package com.empathy.ai.build

import java.io.File
import java.util.Properties

/**
 * gradle.properties 版本更新器
 * 用于Android项目和Gradle项目
 * 
 * @see TDD-00024 4.5.2 GradlePropertiesUpdater实现
 */
class GradlePropertiesUpdater : VersionFileUpdater {
    
    override val name = "Gradle Properties"
    override val supportedFiles = listOf("gradle.properties")
    
    companion object {
        // Android项目版本键
        private const val KEY_VERSION_NAME = "APP_VERSION_NAME"
        private const val KEY_VERSION_CODE = "APP_VERSION_CODE"
        private const val KEY_RELEASE_STAGE = "APP_RELEASE_STAGE"
        
        // 通用版本键（用于非Android模块）
        private const val KEY_VERSION = "VERSION"
        private const val KEY_LIB_VERSION = "LIB_VERSION"
    }
    
    override fun supports(file: File): Boolean {
        return file.name == "gradle.properties" && file.exists()
    }
    
    override fun updateVersion(file: File, version: SemanticVersion): Boolean {
        if (!supports(file)) return false
        
        var content = file.readText()
        var updated = false
        
        // 更新 APP_VERSION_NAME（Android项目）
        if (content.contains(KEY_VERSION_NAME)) {
            content = content.replace(
                Regex("""$KEY_VERSION_NAME=.+"""),
                "$KEY_VERSION_NAME=$version"
            )
            updated = true
        }
        
        // 更新 APP_VERSION_CODE（Android项目）
        if (content.contains(KEY_VERSION_CODE)) {
            content = content.replace(
                Regex("""$KEY_VERSION_CODE=\d+"""),
                "$KEY_VERSION_CODE=${version.toVersionCode()}"
            )
            updated = true
        }
        
        // 更新 VERSION（通用）
        if (content.contains("$KEY_VERSION=")) {
            content = content.replace(
                Regex("""$KEY_VERSION=.+"""),
                "$KEY_VERSION=$version"
            )
            updated = true
        }
        
        // 更新 LIB_VERSION（库模块）
        if (content.contains("$KEY_LIB_VERSION=")) {
            content = content.replace(
                Regex("""$KEY_LIB_VERSION=.+"""),
                "$KEY_LIB_VERSION=$version"
            )
            updated = true
        }
        
        if (updated) {
            file.writeText(content)
        }
        
        return updated
    }
    
    override fun readVersion(file: File): SemanticVersion? {
        if (!supports(file)) return null
        
        val properties = Properties().apply {
            file.inputStream().use { load(it) }
        }
        
        // 优先读取 APP_VERSION_NAME
        val versionName = properties.getProperty(KEY_VERSION_NAME)
            ?: properties.getProperty(KEY_VERSION)
            ?: properties.getProperty(KEY_LIB_VERSION)
            ?: return null
        
        return try {
            SemanticVersion.parse(versionName)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 更新版本号和发布阶段
     * @param file 目标文件
     * @param version 新版本号
     * @param stage 发布阶段
     * @return 是否更新成功
     */
    fun updateVersionWithStage(file: File, version: SemanticVersion, stage: ReleaseStage): Boolean {
        if (!supports(file)) return false
        
        var content = file.readText()
        var updated = false
        
        // 更新版本名称
        if (content.contains(KEY_VERSION_NAME)) {
            content = content.replace(
                Regex("""$KEY_VERSION_NAME=.+"""),
                "$KEY_VERSION_NAME=$version"
            )
            updated = true
        }
        
        // 更新版本代码
        if (content.contains(KEY_VERSION_CODE)) {
            content = content.replace(
                Regex("""$KEY_VERSION_CODE=\d+"""),
                "$KEY_VERSION_CODE=${version.toVersionCode()}"
            )
            updated = true
        }
        
        // 更新发布阶段
        if (content.contains(KEY_RELEASE_STAGE)) {
            content = content.replace(
                Regex("""$KEY_RELEASE_STAGE=.+"""),
                "$KEY_RELEASE_STAGE=${stage.name.lowercase()}"
            )
            updated = true
        }
        
        if (updated) {
            file.writeText(content)
        }
        
        return updated
    }
    
    /**
     * 读取当前发布阶段
     * @param file 目标文件
     * @return 发布阶段，如果无法读取则返回默认值
     */
    fun readStage(file: File): ReleaseStage {
        if (!supports(file)) return ReleaseStage.DEFAULT
        
        val properties = Properties().apply {
            file.inputStream().use { load(it) }
        }
        
        val stageName = properties.getProperty(KEY_RELEASE_STAGE) ?: return ReleaseStage.DEFAULT
        return ReleaseStage.fromString(stageName)
    }
}
