# 快速开始指南

> 这是 AI 助手的核心参考文件，保持精简。详细信息请查阅对应的专项文档。

## 语言规范
- 文档和回答：中文
- 代码注释/变量名/类名：英文

## 常用命令

```bash
# 快捷脚本（推荐）
scripts\quick-build.bat          # 快速构建（跳过lint和测试）
scripts\quick-test.bat           # 运行所有单元测试
scripts\quick-test.bat XxxTest   # 运行指定测试类

# 标准 Gradle 命令
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK
./gradlew testDebugUnitTest      # 单元测试
./gradlew connectedAndroidTest   # 设备测试
./gradlew clean                  # 清理构建
./gradlew --stop                 # 停止 Daemon（释放内存）
```

## 架构规范（必须遵守）

### 代码分层
```
用户操作 → Screen → ViewModel → UseCase → Repository → 数据源
                ↓
            UiState/UiEvent（单向数据流）
```

### 新增功能的标准流程
1. **Domain层**：创建 Model → Repository接口 → UseCase
2. **Data层**：实现 Repository → 配置 DAO/API
3. **Presentation层**：UiState → UiEvent → ViewModel → Screen
4. **DI层**：在对应 Module 中注册依赖

### 命名规范
| 类型 | 命名 | 示例 |
|------|------|------|
| UseCase | 动词+名词+UseCase | `EditFactUseCase` |
| ViewModel | 功能+ViewModel | `ContactDetailViewModel` |
| Screen | 功能+Screen | `ContactDetailScreen` |
| UiState | 功能+UiState | `ContactDetailUiState` |
| Repository | 领域+Repository | `ContactRepository` |

## 边界情况检查清单

实现任何功能时，必须考虑：
- [ ] 空值/null 处理
- [ ] 空列表处理
- [ ] 网络错误处理
- [ ] 数据库错误处理
- [ ] 并发/竞态条件
- [ ] 超长文本/边界值
- [ ] 用户取消操作

## 测试要求

- 每个 UseCase 必须有对应的单元测试
- 测试文件命名：`XxxTest.kt`
- 测试方法命名：`` `功能描述_条件_预期结果` ``

## 常见错误模式（避免）

1. **不要**在 ViewModel 中直接调用 Repository
2. **不要**在 Domain 层引入 Android 依赖
3. **不要**忘记处理 Result.failure 情况
4. **不要**在 Composable 中执行耗时操作

## 文档位置

- 产品概览：`.kiro/steering/product.md`
- 技术栈：`.kiro/steering/tech.md`
- 项目结构：`.kiro/steering/structure.md`
- 当前任务：`WORKSPACE.md`
