plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)  // 保留KSP用于Room和Moshi
    alias(libs.plugins.kotlin.kapt)  // 使用KAPT处理Hilt
}

android {
    namespace = "com.empathy.ai"
    compileSdk = 35

    signingConfigs {
        create("release") {
            keyAlias = (project.findProperty("RELEASE_KEY_ALIAS") as? String) ?: "empathy-key"
            keyPassword = (project.findProperty("RELEASE_KEY_PASSWORD") as? String) ?: "empathy123"
            storeFile = file((project.findProperty("RELEASE_STORE_FILE") as? String) ?: "../empathy-release-key.jks")
            storePassword = (project.findProperty("RELEASE_STORE_PASSWORD") as? String) ?: "empathy123"
        }
    }

    defaultConfig {
        applicationId = "com.empathy.ai"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

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
            signingConfig = signingConfigs.getByName("release")
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
    }

    // KAPT配置 - 用于Hilt，解决多模块兼容性问题
    kapt {
        correctErrorTypes = true
        arguments {
            arg("dagger.fastInit", "enabled")
            arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
        }
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
    // 使用api暴露presentation模块，确保其类在运行时可见
    implementation(project(":domain"))
    implementation(project(":data"))
    api(project(":presentation"))

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
    kapt(libs.hilt.compiler)
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
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
