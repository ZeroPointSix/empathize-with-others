# 探索经验库

> 这是多智能体探索系统的经验积累文档。所有探索智能体在完成工作后，应将有价值的经验记录到这里。

## 使用说明

### 谁应该写入这个文件？
- 所有探索智能体在发现有价值的经验时
- worktree-manager 在审查时发现的通用问题
- 人类用户在审查报告后的总结

### 什么样的经验值得记录？
- ✅ 可复用的解决方案
- ✅ 常见的坑和避免方法
- ✅ 项目特有的约束和规范
- ✅ 有效的调试技巧
- ❌ 一次性的具体实现细节
- ❌ 已经在项目文档中记录的内容

---

## 架构经验

### ARCH-001: Domain 层必须保持纯净
**来源**: architecture-reviewer
**日期**: 待填写
**经验**: 
- domain 模块不能有任何 Android 依赖
- 如果需要 Context，应该通过接口抽象
- 日志使用 domain 层定义的 Logger 接口，由 app 层实现

### ARCH-002: Repository 返回 Result 类型
**来源**: bugfix-explorer
**日期**: 待填写
**经验**:
- 所有 Repository 方法应返回 `Result<T>` 而不是可空类型
- 这样可以强制调用方处理错误情况
- 区分"数据不存在"和"操作失败"两种情况

---

## Bug 修复经验

### BUG-001: 空值处理模式
**来源**: bugfix-explorer
**日期**: 待填写
**经验**:
- 在 Repository 层统一处理空值，不要让 null 传播到 ViewModel
- 使用 `?: throw NotFoundException()` 或 `Result.failure()` 模式
- 调用方使用 `onSuccess/onFailure` 处理结果

### BUG-002: 协程异常处理
**来源**: bugfix-explorer
**日期**: 待填写
**经验**:
- ViewModel 中的协程要用 `try-catch` 或 `runCatching`
- 不要让异常直接崩溃应用
- 使用 `_uiState.update { it.copy(error = message) }` 显示错误

---

## 测试经验

### TEST-001: 测试应该测行为不测实现
**来源**: test-explorer
**日期**: 待填写
**经验**:
- ❌ 错误：测试内部使用了 HashMap
- ✅ 正确：测试当 ID 不存在时返回 null
- 测试接口契约，不测试实现细节

### TEST-002: Mock 的正确使用
**来源**: test-explorer
**日期**: 待填写
**经验**:
- 使用 `coEvery` 而不是 `every` 来 mock suspend 函数
- 使用 `coVerify` 验证 suspend 函数调用
- 不要过度 mock，只 mock 外部依赖

---

## 功能开发经验

### FEAT-001: 新功能开发顺序
**来源**: feature-explorer
**日期**: 待填写
**经验**:
1. Domain 层：Model → Repository 接口 → UseCase
2. Data 层：Entity → DAO → Repository 实现
3. Presentation 层：UiState → UiEvent → ViewModel → Screen
4. DI 配置：在对应 Module 中注册

### FEAT-002: Compose UI 状态管理
**来源**: feature-explorer
**日期**: 待填写
**经验**:
- 使用 `StateFlow` 而不是 `LiveData`
- 使用 `UiState` 数据类封装所有 UI 状态
- 使用 `UiEvent` 密封类处理用户事件

---

## 自由探索经验

### FREE-001: [待填写]
**来源**: free-explorer
**日期**: 待填写
**经验**:
[待填写]

---

## 经验模板

```markdown
### [类别]-[编号]: [简短标题]
**来源**: [智能体名称]
**日期**: YYYY-MM-DD
**验证状态**: ✅ 已验证 / ⚠️ 待验证 / ❌ 已失效
**经验**:
[详细描述经验内容]

**示例代码**（如果适用）:
```kotlin
// 代码示例
```

**相关报告**: [报告文件路径]
```

---

## 更新日志

| 日期 | 更新内容 | 更新者 |
|------|----------|--------|
| 2024-12-30 | 创建经验库文档 | 系统 |
