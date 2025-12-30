# 项目规范参考

> 本文档为多智能体探索系统提供项目规范参考，所有智能体必须遵循这些规范。

## 必读文档

在开始任何工作前，智能体必须阅读：

1. **CLAUDE.md** - 项目主文档（如果存在）
2. **.kiro/steering/** - 项目 steering 文件
3. **WORKSPACE.md** - 当前工作状态
4. **Rules/RulesReadMe.md** - 项目通用规则

## 架构规范

### Clean Architecture 层级

```
:domain/        # 纯 Kotlin 模块 - 无 Android 依赖
:data/          # Android Library - 依赖 :domain
:presentation/  # Android Library - 依赖 :domain
:app/           # Application - 依赖所有模块
```

### 依赖方向

```
app → data/presentation → domain
```

**禁止**：
- domain 层依赖 Android 框架
- domain 层依赖 data 或 presentation 层
- presentation 层依赖 data 层

### 代码分层

```
用户操作 → Screen → ViewModel → UseCase → Repository → 数据源
                ↓
            UiState/UiEvent（单向数据流）
```

## 命名规范

### 文件命名

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| UseCase | 动词+名词+UseCase | `EditFactUseCase.kt` |
| ViewModel | 功能+ViewModel | `ContactDetailViewModel.kt` |
| Screen | 功能+Screen | `ContactDetailScreen.kt` |
| UiState | 功能+UiState | `ContactDetailUiState.kt` |
| UiEvent | 功能+UiEvent | `ContactDetailUiEvent.kt` |
| Repository | 领域+Repository | `ContactRepository.kt` |
| Entity | 名词+Entity | `ContactProfileEntity.kt` |

### 数据库命名

- 表名：`snake_case` 复数形式（`contact_profiles`）
- 列名：`snake_case`（`contact_id`）
- 使用 `@ColumnInfo(name = "...")` 解耦

### Kotlin 命名

- 属性：`camelCase`（`contactId`）
- 常量：`UPPER_SNAKE_CASE`（`MAX_RETRY_COUNT`）
- Composable：`PascalCase`（`ChatScreen`）

## 语言规范

- **文档和回答**：中文
- **代码注释/变量名/类名**：英文

## 测试规范

### 测试文件命名

- 测试文件：`XxxTest.kt`
- 测试方法：`` `功能描述_条件_预期结果` ``

### 测试要求

- 每个 UseCase 必须有对应的单元测试
- 修复 Bug 必须添加回归测试
- 不能破坏现有测试

## 边界情况检查清单

实现任何功能时，必须考虑：

- [ ] 空值/null 处理
- [ ] 空列表处理
- [ ] 网络错误处理
- [ ] 数据库错误处理
- [ ] 并发/竞态条件
- [ ] 超长文本/边界值
- [ ] 用户取消操作

## 常见错误模式（避免）

1. **不要**在 ViewModel 中直接调用 Repository（应通过 UseCase）
2. **不要**在 Domain 层引入 Android 依赖
3. **不要**忘记处理 Result.failure 情况
4. **不要**在 Composable 中执行耗时操作

## 构建命令

```bash
# 快速构建
./gradlew assembleDebug

# 运行测试
./gradlew testDebugUnitTest

# 清理构建
./gradlew clean
```

## 报告保存位置

所有探索报告保存到 `文档/开发文档/MA/` 目录：

```
文档/开发文档/MA/
├── BUGFIX/           # Bug 修复探索报告
├── FEATURE/          # 功能开发探索报告
├── FREE/             # 自由探索报告
├── ARCH/             # 架构审查报告
├── TEST/             # 测试探索报告
└── MANAGE/           # 管理审查报告
```

## 报告命名规范

```
[类型]-[日期]-[简短描述].md

示例：
BUGFIX-20241230-悬浮窗崩溃修复.md
FEATURE-20241230-新增用户画像功能.md
ARCH-20241230-数据层架构审查.md
```

