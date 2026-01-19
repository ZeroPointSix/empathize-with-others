import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)  // 保留KSP用于Room和Moshi
}

val releaseSigningRequired = project.requiresReleaseSigning()
val releaseSigning = project.loadReleaseSigningCredentials()
val enforceReleaseSigning = (project.findProperty("RELEASE_SIGNING_STRICT") as? String)
    ?.toBoolean()
    ?: false

if (releaseSigningRequired && releaseSigning == null) {
    if (enforceReleaseSigning) {
        throw GradleException(
            "Release signing config is missing. Configure RELEASE_* properties via local gradle.properties or environment variables."
        )
    } else {
        logger.warn(
            "Release signing credentials are missing. Falling back to debug keystore for release build. " +
                "Set RELEASE_SIGNING_STRICT=true to enforce real release signing."
        )
    }
}

android {
    namespace = "com.empathy.ai"
    compileSdk = 35

    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                keyAlias = releaseSigning.keyAlias
                keyPassword = releaseSigning.keyPassword
                storeFile = file(releaseSigning.storeFilePath)
                storePassword = releaseSigning.storePassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.empathy.ai"
        minSdk = 24
        targetSdk = 35
        
        // 从gradle.properties读取版本号 (TD-00024)
        val appVersionName = project.findProperty("APP_VERSION_NAME") as? String ?: "1.0.0"
        val appVersionCode = (project.findProperty("APP_VERSION_CODE") as? String)?.toIntOrNull() ?: 10000
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (releaseSigning != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
        }

        // 开发专用变体 - AS 运行时可以选择
        create("dev") {
            initWith(getByName("debug"))
            isMinifyEnabled = false
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            matchingFallbacks += listOf("debug")

            // AGP 8.7.3 兼容的基础优化
            // 主要通过 applicationIdSuffix 区分版本，避免配置冲突
        }
    }

    compileOptions {
        // 启用 coreLibraryDesugaring 以支持 java.time API (minSdk=24)
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
        // 禁用不需要的功能，加速编译
        viewBinding = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Android 资源配置（AGP 8.x 兼容）
    androidResources {
        // AGP 8.x 中许多配置选项已被简化或移除
        // 使用默认配置以确保稳定性
    }

    // Room Schema导出配置（TD-001）
    // 用于数据库版本管理和迁移测试
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.generateKotlin", "true")
        arg("dagger.fastInit", "enabled")
        arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
    }

    // 单元测试配置
    // 允许Android框架方法返回默认值，避免"Method not mocked"错误
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            // 并行执行测试 - 利用多核CPU
            all {
                it.maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
            }
        }
    }

    // 开发时禁用 Lint 检查（可选，节省时间）
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    // 依赖解析优化（AGP 8.7.3 兼容方式）
    // 注意：AndroidXSupportLibraryComponent 在 AGP 8.x 中已更改

}

dependencies {
    // 模块依赖
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":presentation"))

    // Desugaring (Java 8+ API support for minSdk=24)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.process)

    // Compose BOM (统一管理Compose版本)
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose UI
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Compose Material3
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.runtime)

    // Compose Integration
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Hilt (依赖注入) - 使用KAPT替代KSP，解决多模块兼容性问题
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room (本地数据库) - app模块需要Room用于DI配置
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)
    androidTestImplementation(libs.androidx.room.testing)

    // Paging (分页加载)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Retrofit & Networking (网络请求) - app模块需要用于DI配置
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Moshi (JSON解析) - app模块需要用于DI配置
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)

    // Coroutines (协程)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Security (加密存储) - app模块需要用于DI配置
    implementation(libs.androidx.security.crypto)

    // FFmpeg Kit (音视频处理) - 暂时注释,Phase 2 添加
    // implementation(libs.ffmpeg.kit.full)

    // Material Design
    implementation(libs.material)

    // Coil (图片加载)
    implementation(libs.coil.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}

data class SigningCredentials(
    val storeFilePath: String,
    val keyAlias: String,
    val keyPassword: String,
    val storePassword: String
)

fun Project.loadReleaseSigningCredentials(): SigningCredentials? {
    fun resolve(name: String): String? {
        val projectValue = (findProperty(name) as? String)?.takeIf { it.isNotBlank() }
        val envValue = System.getenv(name)?.takeIf { it.isNotBlank() }
        return projectValue ?: envValue
    }

    val keyAlias = resolve("RELEASE_KEY_ALIAS") ?: return null
    val keyPassword = resolve("RELEASE_KEY_PASSWORD") ?: return null
    val storePassword = resolve("RELEASE_STORE_PASSWORD") ?: return null
    val storePath = resolve("RELEASE_STORE_FILE") ?: return null
    val storeFile = rootProject.file(storePath)
    val credentials = SigningCredentials(storeFile.absolutePath, keyAlias, keyPassword, storePassword)
    credentials.validate()
    return credentials
}

fun Project.requiresReleaseSigning(): Boolean {
    val requestedTasks = gradle.startParameter.taskNames
    if (requestedTasks.isEmpty()) {
        return false
    }

    return requestedTasks.any { task ->
        val normalized = task.lowercase()
        normalized.contains("release") ||
            normalized.contains("bundle") ||
            normalized.contains("publish") ||
            normalized.contains("upload")
    }
}

fun SigningCredentials.validate() {
    val file = File(storeFilePath)
    if (!file.exists()) {
        throw GradleException("Release keystore not found at $storeFilePath")
    }
    try {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        FileInputStream(file).use { input ->
            keyStore.load(input, storePassword.toCharArray())
        }
        if (!keyStore.containsAlias(keyAlias)) {
            throw GradleException("Release keystore is missing alias '$keyAlias'")
        }
    } catch (e: Exception) {
        throw GradleException("Invalid release keystore configuration: ${e.message}", e)
    }
}
