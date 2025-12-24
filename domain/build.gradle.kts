/**
 * Domain模块构建配置
 * 
 * 这是Clean Architecture的核心层，必须保持纯Kotlin，不依赖Android SDK。
 * 
 * 依赖规则：
 * - ✅ 可以依赖：纯Kotlin库（Coroutines、标准库）
 * - ❌ 不能依赖：Android SDK、:data模块、:presentation模块
 */
plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // 只允许纯Kotlin依赖
    implementation(libs.kotlinx.coroutines.core)
    
    // JSR-330 标准注解（javax.inject）- 纯Java库，不依赖Android
    implementation("javax.inject:javax.inject:1")
    
    // 测试依赖
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
