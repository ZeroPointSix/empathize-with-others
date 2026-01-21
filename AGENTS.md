# Repository Guidelines

## 项目结构与模块组织
项目采用 Clean Architecture + MVVM，多模块拆分如下：`domain/`（纯 Kotlin 业务模型与 UseCase）、`data/`（Room/Retrofit 与仓库实现）、`presentation/`（Compose UI 与 ViewModel）、`app/`（应用入口与系统服务）。构建与版本管理在 `build.gradle.kts`、`settings.gradle.kts`、`buildSrc/`、`config/` 中。资源与文档位于 `assets/`、`文档/`、`Rules/`。测试代码通常在 `*/src/test/` 与 `*/src/androidTest/`。

## 构建、测试与开发命令
常用命令（Windows 示例）：
```bash
.\gradlew assembleDebug
.\gradlew installDebug
.\gradlew test
.\gradlew :domain:test
.\gradlew :data:test
.\gradlew :presentation:test
.\gradlew connectedAndroidTest
.\gradlew lint
.\gradlew ktlintCheck
.\gradlew ktlintFormat
```
AI 请求日志可用 `scripts\ai-debug.bat` 与 `scripts\ai-debug-full.bat`。macOS/Linux 将 `.\gradlew` 替换为 `./gradlew`。

## 编码风格与命名规范
遵循 Kotlin 官方规范，默认 4 空格缩进。类用 `PascalCase`，函数/变量用 `camelCase`，测试类以 `*Test` 结尾。状态管理使用 `StateFlow + UiState`，错误处理使用 `Result<T>`，ViewModel 使用 `viewModelScope`，禁止 `GlobalScope`。优先复用 `presentation/ui/component/` 组件，`Ios*` 组件用于统一 iOS 风格。文档与沟通使用中文，代码注释使用英文。

## 测试指南
单元测试覆盖核心业务逻辑，位于 `*/src/test/`；仪器测试在 `*/src/androidTest/`，需连接设备。Room 迁移测试位置参考 `app/src/androidTest-disabled/`。运行单模块测试使用 `:module:test`，UI/设备测试使用 `connectedAndroidTest`。

## 提交与 PR 规范
提交信息采用约定式提交：`feat:`、`fix:`、`docs:`、`chore:`，可选作用域（如 `fix(ci): ...`），必要时在描述中带上需求/缺陷编号（如 `PRD-00036`）。PR 需包含变更说明、关联文档/Issue、测试结果与 UI 截图（若涉及界面）。开始任务前更新 `WORKSPACE.md`，记录当前任务与测试状态。

## 安全与配置提示
本项目为本地优先应用，API Key 与隐私数据仅存本机。配置 SDK 路径：复制 `local.properties.example` 为 `local.properties`。签名配置：复制 `keystore.example.properties` 为 `key.properties`，严禁提交真实密钥。
