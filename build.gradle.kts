// Top-level build file where you can add configuration options common to all sub-projects/modules.

// AGP 版本兼容性检查（在插件加载前执行）
println("🔍 执行 AGP 版本兼容性检查...")

// 检查 gradle.properties 中的废弃配置
val gradleProps = java.util.Properties()
file("gradle.properties").inputStream().use {
    try {
        gradleProps.load(it)
    } catch (e: Exception) {
        println("⚠️  无法读取 gradle.properties: ${e.message}")
    }
}

// AGP 8.x 已废弃的配置项
val deprecatedConfigs = mapOf(
    "android.enableBuildCache" to "已在 AGP 7.0 废弃，8.0 移除，请移除此配置",
    "android.buildcache.max-size" to "已在 AGP 7.0 废弃，请使用 Gradle 构建缓存",
    "android.buildcache.location" to "已在 AGP 7.0 废弃，请使用 Gradle 构建缓存"
)

var hasDeprecatedConfig = false
deprecatedConfigs.forEach { (config, message) ->
    if (gradleProps.containsKey(config)) {
        println("❌ 废弃配置: $config")
        println("   $message")
        hasDeprecatedConfig = true
    }
}

if (hasDeprecatedConfig) {
    println("\n🛠️  修复建议:")
    println("   1. 移除上述废弃配置项")
    println("   2. 使用统一的 Gradle 构建缓存: org.gradle.caching=true")
    println("   3. 参考: https://docs.gradle.org/current/userguide/build_cache.html")
    println("")
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

// 应用版本更新插件
apply<com.empathy.ai.build.VersionUpdatePlugin>()

// 配置版本更新扩展
configure<com.empathy.ai.build.VersionUpdateExtension> {
    defaultStage = "dev"
    autoBackup = true
    maxBackups = 50
    backupRetentionDays = 30
    enableVersionHistory = true
}
