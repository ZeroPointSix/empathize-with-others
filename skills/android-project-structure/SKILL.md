---
name: android-project-structure
description: Android 项目结构 - 标准项目目录组织、模块化设计、资源管理、多模块配置。在创建新 Android 项目或重构项目结构时使用。
---

# Android 项目结构

## 激活时机

当满足以下条件时自动激活此技能：
- 创建新的 Android 项目
- 重构现有项目结构
- 添加新模块
- 组织代码和资源文件

## 标准项目结构

### 单模块项目结构

```
app/
├── manifest/                    # AndroidManifest.xml
├── kotlin/                      # Kotlin 源代码
│   └── com/example/app/
│       ├── ui/                  # UI 相关
│       │   ├── main/
│       │   ├── home/
│       │   └── adapters/
│       ├── data/                # 数据层
│       │   ├── model/
│       │   ├── repository/
│       │   └── source/
│       │       ├── local/
│       │       └── remote/
│       ├── domain/              # 领域层
│       │   ├── model/
│       │   ├── usecase/
│       │   └── repository/
│       ├── di/                  # 依赖注入
│       └── utils/               # 工具类
├── java/                        # Java 源代码（如需要）
├── res/                         # 资源文件
│   ├── drawable/
│   ├── layout/
│   ├── mipmap/
│   ├── values/
│   ├── navigation/
│   └── menu/
└── assets/                      # 资产文件
```

### 多模块项目结构

```
project/
├── app/                         # 主应用模块
├── core/                        # 核心模块
│   ├── ui/                      # UI 基础组件
│   ├── data/                    # 数据基础
│   └── domain/                  # 领域基础
├── feature/                     # 功能模块
│   ├── home/
│   ├── profile/
│   └── settings/
├── common/                      # 通用模块
│   ├── utils/
│   ├── resources/
│   └── design-system/
└── buildSrc/                    # 构建脚本
```

## 模块职责

### app 模块（主应用）

```
职责：
- 应用入口
- 依赖注入配置
- 导航配置
- Application 类

不包含：
- 业务逻辑
- 数据实现
```

### core 模块（核心基础）

```
core/ui/
├── theme/           # 主题配置
├── components/      # 通用 UI 组件
└── extensions/      # UI 扩展

core/data/
├── local/           # 本地数据源
├── remote/          # 远程数据源
└── repository/      # 仓库实现

core/domain/
├── model/           # 领域模型
├── repository/      # 仓库接口
└── usecase/         # 通用用例
```

### feature 模块（功能模块）

```
feature/home/
├── presentation/    # UI 层
│   ├── HomeScreen.kt
│   ├── HomeViewModel.kt
│   └── HomeAdapter.kt
├── domain/          # 领域层
│   ├── model/
│   ├── repository/
│   └── usecase/
└── di/              # 依赖注入
```

## 包组织原则

### 按功能分包

```
✅ 推荐：按功能分包
com.example.app
├── feature
│   ├── home
│   │   ├── HomeActivity.kt
│   │   ├── HomeViewModel.kt
│   │   └── HomeAdapter.kt
│   ├── profile
│   └── settings
├── shared
│   ├── ui
│   └── utils
```

### 按层分包

```
✅ 可选：按层分包
com.example.app
├── ui
│   ├── activities
│   ├── fragments
│   ├── adapters
│   └── viewmodels
├── data
│   ├── model
│   ├── repository
│   └── source
└── domain
    ├── model
    ├── usecase
    └── repository
```

## 资源组织

### res 目录结构

```
res/
├── drawable/              # 图片资源
│   ├── ic_launcher.xml
│   └── divider.xml
├── layout/                # 布局文件
│   ├── activity_main.xml
│   ├── fragment_home.xml
│   └── item_user.xml
├── values/                # 值资源
│   ├── strings.xml        # 字符串
│   ├── colors.xml         # 颜色
│   ├── dimens.xml         # 尺寸
│   ├── styles.xml         # 样式
│   └── themes.xml         # 主题
├── navigation/            # 导航图
│   └── nav_graph.xml
├── menu/                  # 菜单
│   ├── main_menu.xml
│   └── context_menu.xml
└── xml/                   # XML 配置
    ├── network_config.xml
    └── file_paths.xml
```

### 资源命名规范

```
图片资源：
ic_图标名称_尺寸.xml/ic_图标名称_尺寸.png
示例：ic_home_24.xml, ic_launcher_foreground.xml

布局资源：
类型_组件名称.xml
示例：activity_main.xml, fragment_home.xml, item_user.xml

菜单资源：
menu_用途.xml
示例：menu_main.xml, menu_context.xml

值资源：
按照类型组织在对应文件中
```

## 模块化最佳实践

### 模块依赖规则

```
依赖方向：
app → feature → core
feature → core
不允：core → feature
不允：feature → feature（尽量减少）
```

### 模块 gradle 配置

```kotlin
// build.gradle.kts (模块级)

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") // 如果使用 KSP
}

android {
    namespace = "com.example.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    // 模块间依赖
    implementation(project(":core:ui"))
    implementation(project(":core:data"))

    // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // 测试
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
```

## 常见目录模式

### MVVM 架构目录

```
feature/profile/
├── presentation/
│   ├── ProfileScreen.kt
│   ├── ProfileViewModel.kt
│   └── ProfileUiState.kt
├── domain/
│   ├── GetProfileUseCase.kt
│   ├── UpdateProfileUseCase.kt
│   └── model/
│       └── Profile.kt
└── di/
    └── ProfileModule.kt
```

### Compose 项目目录

```
feature/home/
├── ui/
│   ├── HomeScreen.kt
│   ├── HomeViewModel.kt
│   └── components/
│       ├── HomeHeader.kt
│       └── HomeListItem.kt
├── domain/
│   └── usecase/
│       └── GetHomeDataUseCase.kt
└── model/
    └── HomeItem.kt
```

## 代码组织技巧

### 文件命名

```
Activity：XxxActivity.kt
Fragment：XxxFragment.kt
ViewModel：XxxViewModel.kt
Adapter：XxxAdapter.kt
UseCase：XxxUseCase.kt
Repository：XxxRepository.kt
Model：Xxx.kt / XxxEntity.kt / XxxDto.kt
```

### 文件组织

```kotlin
// 单一文件：类 + 相关扩展
// UserProfile.kt
data class UserProfile(
    val id: String,
    val name: String
)

fun UserProfile.toDisplayModel(): UserDisplayModel = ...

// 或多个相关类放在一个文件
// UserModels.kt
data class UserProfile(...)
data class UserListItem(...)
data class UserDetailItem(...)
```

## 最佳实践

### ✅ 应该做的

```
1. 功能内聚：相关功能放在一起
2. 单一职责：每个模块职责明确
3. 依赖单向：避免循环依赖
4. 资源分类：按类型和功能组织
5. 命名统一：遵循 Android 命名规范
```

### ❌ 不应该做的

```
1. 不要创建过深的包结构（建议≤3层）
2. 不要在不同模块间直接访问内部类
3. 不要在 app 模块放业务逻辑
4. 不要把所有代码放在一个包中
5. 不要使用默认包名
```

## 相关资源

- `resources/module-structure.md` - 模块结构详解
- `resources/resource-management.md` - 资源管理指南
- `templates/gradle-config.kts` - Gradle 配置模板

---

**技能状态**: 完成 ✅
**适用架构**: MVVM, MVI, Clean Architecture
**推荐插件**: Kotlin Android Extensions, KAPT, KSP
