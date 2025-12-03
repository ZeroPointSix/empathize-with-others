package com.empathy.ai.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application 入口
 *
 * 使用 @HiltAndroidApp 注解启用 Hilt 依赖注入
 */
@HiltAndroidApp
class EmpathyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 应用初始化逻辑
    }
}
