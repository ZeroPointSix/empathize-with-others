# 快速开始指南

> 这是 AI 助手的核心参考文件，保持精简。详细信息请查阅对应的专项文档。

## 语言规范
- 文档和回答：中文
- 代码注释/变量名/类名：英文

## 架构速览

- **模式**: Clean Architecture + MVVM
- **模块**: `:domain` / `:data` / `:presentation` / `:app`
- **依赖方向**: `app` → `data`/`presentation` → `domain`
- **数据流**: UI → ViewModel → UseCase → Repository → Data Source

## 常用命令

### 构建与安装（Windows）
```bash
gradlew.bat assembleDebug

gradlew.bat assembleRelease

gradlew.bat installDebug
```

### 构建与安装（macOS/Linux）
```bash
./gradlew assembleDebug

./gradlew assembleRelease

./gradlew installDebug
```

### 测试
```bash
# 全量单元测试
./gradlew test

# 模块级测试
./gradlew :domain:test
./gradlew :data:test
./gradlew :presentation:test

# 运行单个测试类
./gradlew :presentation:testDebugUnitTest --tests "*BUG00058*"
```

## ADB 调试
```bash
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.empathy.ai/.ui.MainActivity
adb uninstall com.empathy.ai
adb logcat
```

## 新增功能标准流程
1. **Domain**：Model → Repository 接口 → UseCase
2. **Data**：Repository 实现 → DAO/API → DI 绑定
3. **Presentation**：UiState → UiEvent → ViewModel → Screen
4. **App**：应用级依赖注入/服务接入

## Bug 回归测试（现有用例）

- `BUG00058CreateNewSessionTest.kt`
- `BUG00059RegenerateMessageRoleTest.kt`
- `BUG00060SessionManagementTest.kt`
- `BUG00061SessionHistoryNavigationTest.kt`
- `BUG00061SceneSwitchingTest.kt`
- `BUG00061PromptEditorSceneSwitchTest.kt`
- `BUG00063ContactSearchTest.kt`
- `BUG00063VisibilityGateTest.kt`
- `BUG00064ManualSummaryTest.kt`
- `BUG00065FactEditTest.kt`
- `BUG00066EditBrainTagTest.kt`
- `BUG00068AiAdvisorEntryRefreshTest.kt`
- `BUG00068NavigationStackTest.kt`
- `BUG00069AiAdvisorTabRestoreTest.kt`

运行示例：
```bash
./gradlew :presentation:test --tests "*BUG00068*"
```

## 文档位置

### 快速参考文档（steering 目录）
- 产品概览：`.kiro/steering/product.md`
- 技术栈：`.kiro/steering/tech.md`
- 项目结构：`.kiro/steering/structure.md`
- 设置功能：`.kiro/steering/settings-feature.md`
- 当前任务：`WORKSPACE.md`

### 长期文档体系

项目长期文档位于 `文档/项目文档/`：

```
文档/项目文档/
├── README.md              # 项目文档总入口
├── domain/                # 领域层
├── data/                  # 数据层
├── presentation/          # 表现层
├── app/                   # 应用层
└── di/                    # 依赖注入
```
