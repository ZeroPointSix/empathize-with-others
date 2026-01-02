# 影响范围评估指南 (Impact Analysis Guide)

> ⚠️ **所有探索智能体在开始修改前必须执行影响范围评估**
> 
> 这是支持多智能体并行开发的核心机制。

---

## 核心理念

```
╔══════════════════════════════════════════════════════════════════════════════╗
║                                                                              ║
║  🎯 影响范围评估的价值                                                        ║
║                                                                              ║
║  ┌────────────────────────────────────────────────────────────────────────┐  ║
║  │                                                                        │  ║
║  │   在多智能体并行开发中，最大的风险是文件冲突                              │  ║
║  │   如果两个智能体同时修改同一个文件，会导致合并困难                        │  ║
║  │                                                                        │  ║
║  │   通过预先评估影响范围，我们可以：                                        │  ║
║  │   • 识别可以并行执行的任务                                               │  ║
║  │   • 识别必须串行执行的任务                                               │  ║
║  │   • 提前规划任务分配                                                     │  ║
║  │                                                                        │  ║
║  └────────────────────────────────────────────────────────────────────────┘  ║
║                                                                              ║
║  📋 先评估，再动手                                                           ║
║                                                                              ║
╚══════════════════════════════════════════════════════════════════════════════╝
```

---

## 什么是影响范围评估？

影响范围评估是在**开始任何代码修改之前**，预先分析和记录：

1. **将要修改的文件** - 哪些现有文件需要修改？
2. **将要新增的文件** - 需要创建哪些新文件？
3. **将要删除的文件** - 需要删除哪些文件？
4. **依赖关系** - 这些修改会影响哪些其他模块？
5. **并行性分析** - 这个任务能否与其他任务并行？

---

## 为什么需要影响范围评估？

### 问题场景

```
智能体 A: 修复 ContactRepository 的 Bug
智能体 B: 为 ContactRepository 添加新功能
        │
        ▼
    同时修改同一个文件
        │
        ▼
    合并冲突！工作浪费！
```

### 有影响范围评估的好处

```
任务规划阶段
    │
    ├─► 任务 A: 影响 ContactRepository.kt, ContactDao.kt
    ├─► 任务 B: 影响 ContactRepository.kt, ContactEntity.kt
    │
    ▼
发现冲突：两个任务都修改 ContactRepository.kt
    │
    ▼
决策：任务 A 和 B 必须串行执行
    │
    ▼
避免了合并冲突！
```

---

## 影响范围评估模板

### 在决策日志中记录

每个任务开始前，在 `DECISION_JOURNAL.md` 中添加影响范围评估：

```markdown
## 📊 影响范围评估 (Impact Analysis)

### 评估时间
YYYY-MM-DD HH:MM

### 任务描述
[简要描述要完成的任务]

### 文件影响清单

#### 🔧 将要修改的文件

| 文件路径 | 模块 | 修改类型 | 修改原因 |
|---------|------|---------|---------|
| domain/src/.../ContactRepository.kt | :domain | 接口变更 | 添加新方法 |
| data/src/.../ContactRepositoryImpl.kt | :data | 实现变更 | 实现新方法 |
| presentation/src/.../ContactViewModel.kt | :presentation | 调用变更 | 调用新方法 |

#### ➕ 将要新增的文件

| 文件路径 | 模块 | 文件类型 | 用途 |
|---------|------|---------|------|
| domain/src/.../NewUseCase.kt | :domain | UseCase | 新业务逻辑 |
| domain/src/test/.../NewUseCaseTest.kt | :domain | 测试 | 单元测试 |

#### ➖ 将要删除的文件

| 文件路径 | 模块 | 删除原因 |
|---------|------|---------|
| (无) | - | - |

### 依赖影响分析

#### 上游依赖（谁依赖这些文件）
- `ContactDetailViewModel` 依赖 `ContactRepository`
- `ContactListViewModel` 依赖 `ContactRepository`

#### 下游依赖（这些文件依赖谁）
- `ContactRepositoryImpl` 依赖 `ContactDao`
- `ContactRepositoryImpl` 依赖 `ContactEntity`

### 并行性分析

#### 与其他任务的冲突检查

| 其他任务 | 冲突文件 | 冲突级别 | 建议 |
|---------|---------|---------|------|
| TD-00018 | ContactRepository.kt | 🔴 高 | 串行执行 |
| TD-00019 | (无) | 🟢 无 | 可并行 |
| BUG-00026 | ContactViewModel.kt | 🟡 中 | 协调后并行 |

#### 并行性结论

- ✅ 可与 TD-00019 并行执行
- ❌ 不可与 TD-00018 并行执行（冲突：ContactRepository.kt）
- ⚠️ 与 BUG-00026 需要协调（冲突：ContactViewModel.kt）

### 风险评估

| 风险 | 级别 | 缓解措施 |
|------|------|---------|
| 接口变更导致编译错误 | 中 | 同时更新所有调用方 |
| 数据库迁移 | 低 | 不涉及数据库变更 |
```

---

## 在 PRD 文档中使用

### PRD 任务清单格式

在 PRD 文档的任务清单中，添加影响范围信息：

```markdown
## 任务清单

### 任务分组（按并行性）

#### 🟢 可并行执行的任务组 A

| 任务ID | 任务描述 | 影响文件 | 预估工时 |
|--------|---------|---------|---------|
| T-001 | 添加新Model | domain/model/NewModel.kt | 0.5h |
| T-002 | 添加新UseCase | domain/usecase/NewUseCase.kt | 1h |
| T-003 | 添加新Screen | presentation/screen/NewScreen.kt | 2h |

**并行性说明**：这三个任务涉及不同文件，可以同时分配给不同智能体。

#### 🟡 需要协调的任务组 B

| 任务ID | 任务描述 | 影响文件 | 依赖任务 |
|--------|---------|---------|---------|
| T-004 | 修改Repository接口 | domain/repository/XxxRepository.kt | - |
| T-005 | 实现Repository | data/repository/XxxRepositoryImpl.kt | T-004 |

**并行性说明**：T-005 依赖 T-004 的接口定义，必须在 T-004 完成后执行。

#### 🔴 必须串行的任务组 C

| 任务ID | 任务描述 | 影响文件 | 冲突说明 |
|--------|---------|---------|---------|
| T-006 | 修复Bug A | ContactRepository.kt | - |
| T-007 | 修复Bug B | ContactRepository.kt | 与T-006冲突 |

**并行性说明**：T-006 和 T-007 都修改同一文件，必须串行执行。
```

### 任务依赖图

```
任务依赖关系图：

T-001 ──┐
        ├──► T-004 ──► T-005 ──► T-008
T-002 ──┤
        │
T-003 ──┘

T-006 ──► T-007  (串行，文件冲突)

可并行：[T-001, T-002, T-003] 与 [T-006]
```

---

## 冲突级别定义

| 级别 | 图标 | 定义 | 处理方式 |
|------|------|------|---------|
| 🟢 无冲突 | 绿色 | 涉及完全不同的文件 | 可以并行执行 |
| 🟡 低冲突 | 黄色 | 涉及同一模块但不同文件 | 可以并行，但需要协调 |
| 🟠 中冲突 | 橙色 | 涉及相关联的文件 | 建议串行，或仔细协调 |
| 🔴 高冲突 | 红色 | 涉及同一文件 | 必须串行执行 |

---

## 快速评估流程

### 第一步：列出所有要修改的文件

```bash
# 思考：这个任务需要修改哪些文件？
# 按照 Clean Architecture 分层思考：
# 1. Domain 层：Model? Repository接口? UseCase?
# 2. Data 层：Entity? DAO? Repository实现?
# 3. Presentation 层：ViewModel? Screen? UiState?
# 4. App 层：DI模块? Service?
```

### 第二步：检查与其他任务的冲突

```bash
# 查看其他正在进行的任务
# 检查 WORKSPACE.md 中的当前任务
# 检查其他工作树的影响范围评估
```

### 第三步：记录并行性结论

```markdown
### 并行性结论

本任务：
- ✅ 可与 [任务列表] 并行
- ❌ 不可与 [任务列表] 并行
- ⚠️ 需要与 [任务列表] 协调
```

---

## 与决策日志的关系

影响范围评估是决策日志的**第一部分**：

```
DECISION_JOURNAL.md
├── 📊 影响范围评估 (开始前)
│   ├── 文件影响清单
│   ├── 依赖影响分析
│   └── 并行性分析
│
├── 📋 决策记录 (进行中)
│   ├── 决策 #1
│   ├── 决策 #2
│   └── ...
│
└── 📝 给后续智能体的建议 (结束时)
```

---

## 影响范围评估检查清单

在开始任何修改前，确保完成以下检查：

```markdown
## 影响范围评估检查清单

- [ ] 列出了所有将要修改的文件
- [ ] 列出了所有将要新增的文件
- [ ] 列出了所有将要删除的文件
- [ ] 分析了上游依赖（谁依赖这些文件）
- [ ] 分析了下游依赖（这些文件依赖谁）
- [ ] 检查了与其他任务的冲突
- [ ] 确定了并行性结论
- [ ] 评估了风险
```

---

## 示例：完整的影响范围评估

```markdown
## 📊 影响范围评估 (Impact Analysis)

### 评估时间
2026-01-01 10:30

### 任务描述
为联系人添加"生日提醒"功能

### 文件影响清单

#### 🔧 将要修改的文件

| 文件路径 | 模块 | 修改类型 | 修改原因 |
|---------|------|---------|---------|
| domain/src/.../ContactProfile.kt | :domain | 模型扩展 | 添加birthday字段 |
| domain/src/.../ContactRepository.kt | :domain | 接口扩展 | 添加getBirthdayContacts方法 |
| data/src/.../ContactProfileEntity.kt | :data | 实体扩展 | 添加birthday列 |
| data/src/.../ContactDao.kt | :data | DAO扩展 | 添加生日查询方法 |
| data/src/.../ContactRepositoryImpl.kt | :data | 实现扩展 | 实现新方法 |
| data/src/.../AppDatabase.kt | :data | 迁移 | 添加Migration |
| presentation/src/.../ContactDetailScreen.kt | :presentation | UI扩展 | 显示生日信息 |

#### ➕ 将要新增的文件

| 文件路径 | 模块 | 文件类型 | 用途 |
|---------|------|---------|------|
| domain/src/.../GetBirthdayContactsUseCase.kt | :domain | UseCase | 获取近期生日联系人 |
| domain/src/test/.../GetBirthdayContactsUseCaseTest.kt | :domain | 测试 | 单元测试 |
| presentation/src/.../BirthdayReminderScreen.kt | :presentation | Screen | 生日提醒页面 |
| presentation/src/.../BirthdayReminderViewModel.kt | :presentation | ViewModel | 生日提醒状态管理 |

#### ➖ 将要删除的文件

| 文件路径 | 模块 | 删除原因 |
|---------|------|---------|
| (无) | - | - |

### 依赖影响分析

#### 上游依赖
- `ContactDetailViewModel` 依赖 `ContactProfile`
- `ContactListViewModel` 依赖 `ContactRepository`
- 所有使用 `ContactProfile` 的地方都会受影响

#### 下游依赖
- `ContactProfileEntity` 依赖 Room 数据库
- 需要数据库迁移

### 并行性分析

#### 与其他任务的冲突检查

| 其他任务 | 冲突文件 | 冲突级别 | 建议 |
|---------|---------|---------|------|
| TD-00020 (联系人搜索优化) | ContactDao.kt | 🟡 中 | 协调后并行 |
| BUG-00027 (联系人显示Bug) | ContactDetailScreen.kt | 🔴 高 | 串行执行 |
| TD-00021 (设置界面优化) | (无) | 🟢 无 | 可并行 |

#### 并行性结论

- ✅ 可与 TD-00021 并行执行
- ⚠️ 与 TD-00020 需要协调（都修改 ContactDao.kt）
- ❌ 不可与 BUG-00027 并行执行（都修改 ContactDetailScreen.kt）

### 风险评估

| 风险 | 级别 | 缓解措施 |
|------|------|---------|
| 数据库迁移失败 | 高 | 编写完整的Migration测试 |
| 模型变更导致编译错误 | 中 | 同时更新所有使用ContactProfile的地方 |
| UI布局变化 | 低 | 在多种屏幕尺寸上测试 |

### 评估结论

本任务涉及 **7个修改文件** 和 **4个新增文件**，跨越 **3个模块**。
建议在 BUG-00027 完成后再开始本任务，避免 ContactDetailScreen.kt 的冲突。
```

---

## 总结

**影响范围评估是多智能体并行开发的基础**

- 它让我们在动手前就知道会影响哪些文件
- 它让我们能够识别任务之间的冲突
- 它让我们能够合理规划并行执行的任务
- 它让合并工作变得更加顺畅

**记住**：

```
先评估，再动手
知道影响，才能并行
```
