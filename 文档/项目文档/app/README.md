# App 模块文档

> [根目录](../../../CLAUDE.md) > [项目文档](../README.md) > **app**

## 模块职责

App模块是应用的入口点，负责：
- 应用程序生命周期管理
- 全局组件初始化
- Android Application配置
- Hilt依赖注入设置

## 关键文件

### EmpathyApplication.kt
- **路径**: `app/src/main/java/com/empathy/ai/app/EmpathyApplication.kt`
- **职责**:
  - Application类实现
  - 全局初始化逻辑
  - Hilt组件配置
  - 异常处理初始化

### MainActivity.kt
- **路径**: `app/src/main/java/com/empathy/ai/presentation/ui/MainActivity.kt`
- **职责**:
  - 主Activity实现
  - Compose主题设置
  - 导航系统集成
  - 权限请求处理

## 依赖配置

### Gradle配置
- **编译SDK**: 35
- **最低SDK**: 24
- **目标SDK**: 35
- **版本号**: 1.0.0
- **版本代码**: 1

### 签名配置
- **Debug**: 使用默认debug keystore
- **Release**: 配置了release签名
  - keyAlias: empathy-key
  - storeFile: empathy-release-key.jks

### 主要依赖
- Kotlin 2.0.21
- Compose BOM 2024.12.01
- Hilt 2.52
- AndroidX Core
- Material3

## 初始化顺序

1. **Application初始化**
   - 创建Hilt组件
   - 初始化全局依赖

2. **主Activity启动**
   - 设置Compose主题
   - 初始化导航
   - 检查必要权限

## 注意事项

- Application类需要继承HiltApplication
- 所有Activity必须通过Hilt注入依赖
- ProGuard配置已优化，保持必要的反射类

## 相关文件清单

### 源代码文件
- `EmpathyApplication.kt` - 应用程序主类

### 配置文件
- `build.gradle.kts` - 模块构建配置
- `proguard-rules.pro` - 代码混淆规则

## 变更记录

### 2025-12-21 - 初始创建
- 创建app模块文档
- 记录关键文件和配置信息