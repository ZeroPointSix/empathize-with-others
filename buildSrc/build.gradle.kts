/**
 * buildSrc 模块构建配置
 * 
 * 用于版本号自动更新和图标切换功能
 * 
 * @see TDD-00024 图标和版本号自动更新技术设计
 */
plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "2.0.21"
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    // Kotlin 序列化 - 用于JSON配置文件解析
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // 测试依赖
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.21")
}

// 配置 Kotlin 编译选项
kotlin {
    jvmToolchain(17)
}

// 配置测试任务
tasks.test {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
