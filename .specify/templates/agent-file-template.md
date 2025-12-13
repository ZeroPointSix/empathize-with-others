# [项目名称] 开发指南

自动从所有功能计划生成。最后更新: [日期]

---

## 项目概述

**项目名称**: 共情 AI 助手  
**技术栈**: Android (Kotlin) + Jetpack Compose + Clean Architecture  
**最低 SDK**: 24 (Android 7.0)  
**目标 SDK**: 35

## 活跃技术

[从所有 TDD 文件提取]

- **语言**: Kotlin 2.0.21
- **构建**: Gradle 8.13 + Kotlin DSL
- **UI**: Jetpack Compose + Material Design 3
- **架构**: Clean Architecture + MVVM + Hilt
- **数据**: Room + Retrofit + Moshi
- **异步**: Kotlin Coroutines + Flow

## 项目结构

```text
com.empathy.ai/
├── app/                    # 应用入口
├── domain/                 # 领域层（纯 Kotlin）
│   ├── model/              # 业务实体
│   ├── repository/         # 仓库接口
│   ├── usecase/            # 用例
│   └── service/            # 领域服务
├── data/                   # 数据层
│   ├── local/              # 本地存储
│   ├── remote/             # 网络层
│   └── repository/         # 仓库实现
├── presentation/           # 表现层
│   ├── ui/                 # UI 组件
│   ├── viewmodel/          # ViewModel
│   └── navigation/         # 导航
└── di/                     # 依赖注入
```

## 常用命令

```bash
# 构建
./gradlew assembleDebug
./gradlew assembleRelease

# 测试
./gradlew test
./gradlew testDebugUnitTest

# 代码质量
./gradlew lint
./gradlew clean
```

## 代码规范

### Kotlin

- 属性: `camelCase`
- 常量: `UPPER_SNAKE_CASE`
- Composable: `PascalCase`

### 数据库

- 表名: `snake_case` 复数形式
- 列名: `snake_case`
- 始终使用 `@ColumnInfo(name = "...")`

### 文件命名

- Entity 后缀: `ContactProfileEntity.kt`
- ViewModel 后缀: `ChatViewModel.kt`
- UiState 后缀: `ChatUiState.kt`
- UseCase 后缀: `AnalyzeChatUseCase.kt`

## 文档规范

### 开发过程文档

存放路径: `文档/开发文档/`

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 需求文档 | PRD-xxxxx-yyyy | PRD-00001-悬浮窗最小化 |
| 功能文档 | FD-xxxxx-yyyy | FD-00001-悬浮窗最小化 |
| 技术设计 | TDD-xxxxx-yyyy | TDD-00001-悬浮窗架构设计 |
| 测试计划 | TP-xxxxx-yyyy | TP-00001-悬浮窗测试计划 |

### 项目长期文档

存放路径: `文档/长期文档/`

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 规范文档 | SD-xxxxx-yyyy | SD-00001-代码规范 |
| 架构文档 | AD-xxxxx-yyyy | AD-00001-悬浮窗架构设计 |
| API 文档 | API-xxxxx-yyyy | API-00001-AI分析接口 |
| 用户手册 | UM-xxxxx-yyyy | UM-00001-悬浮窗使用指南 |

## 最近变更

[最近3个功能及其添加的内容]

<!-- 手动添加开始 -->
<!-- 手动添加结束 -->
