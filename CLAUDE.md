# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

**共情AI助手** - 基于 Android 平台的智能社交沟通辅助应用，采用 Clean Architecture + MVVM 架构模式。

### 核心原则
- **零后端 (Zero-Backend)**: 无服务器，无用户账户体系
- **BYOK**: AI能力通过用户自备API密钥直连第三方服务
- **隐私优先**: 敏感数据本地脱敏，密钥硬件级加密存储
- **无感接入**: 悬浮窗+无障碍服务与宿主App交互

### 技术栈
- Kotlin 2.0.21 + K2编译器
- Gradle 8.13 + AGP 8.7.3
- Jetpack Compose BOM 2024.12.01
- Hilt 2.52 + Room v11
- 最低SDK: 24, 目标SDK: 35

## 常用命令

```bash
# 构建与测试
./gradlew assembleDebug          # Debug构建
./gradlew assembleRelease        # Release构建
./gradlew test                   # 单元测试
./gradlew :presentation:test     # presentation模块测试
./gradlew :domain:test           # domain模块测试

# 调试脚本
scripts\logcat.bat -e            # ERROR日志
scripts\quick-error.bat          # 快速查看最近错误
scripts\ai-debug.bat             # AI请求调试

# ADB调试
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.empathy.ai/.ui.MainActivity
```

## 架构结构

```
共情AI助手 (根)
├── app/           应用入口、DI配置、Android服务
├── domain/        纯Kotlin业务层，无Android依赖
│   ├── model/     业务模型
│   ├── repository 仓库接口
│   └── usecase/   业务用例
├── data/          数据层实现
│   ├── local/     Room数据库、SharedPreferences
│   ├── remote/    Retrofit网络请求
│   └── repository 仓库实现
└── presentation/  UI层
    ├── ui/        Compose组件
    ├── viewmodel/ ViewModel
    └── navigation 导航系统
```

### 依赖方向
`app` → `data/presentation` → `domain`（严格单向依赖）

### 关键约束
- **domain层禁止引入Android依赖**（Room, Context, etc.）
- 所有API定义在domain层，data层实现
- 敏感数据处理使用 `PrivacyEngine`
- 错误处理统一使用 `Result<T>` 类型

## 模块文档

详细架构信息参考各模块文档：
- [domain/CLAUDE.md](./domain/CLAUDE.md) - 领域层详细文档
- [data/CLAUDE.md](./data/CLAUDE.md) - 数据层详细文档
- [presentation/CLAUDE.md](./presentation/CLAUDE.md) - 表现层详细文档
- [app/.../CLAUDE.md](./app/src/main/java/com/empathy/ai/app/CLAUDE.md) - 应用层详细文档

## 编码规范

- 遵循 SOLID、KISS、DRY、YAGNI 原则
- 所有公共API必须有KDoc注释
- 使用 `Result<T>` 统一处理成功/失败
- 测试文件命名: `XxxTest.kt` / `XxxSpec.kt`
