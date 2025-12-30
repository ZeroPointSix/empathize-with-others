# 探索分支成果合并审查指南

## 概述

多智能体探索系统产生的成果需要经过审查才能合并回主分支。本指南定义了审查标准和合并策略。

## 成果分类

### 📄 A类：文档和报告（低风险）
- 探索报告
- 分析文档
- 架构建议
- Bug 报告

**合并策略**：直接合并到 `文档/开发文档/MA/` 目录

### 🧪 B类：测试用例（中风险）
- 新增的单元测试
- 边界情况测试
- 集成测试

**合并策略**：需要人工审查

### 🐛 C类：Bug 修复（中高风险）
- 代码修复
- 逻辑修正
- 异常处理

**合并策略**：需要人工审查 + 测试验证

### 🔧 D类：代码重构（高风险）
- 架构调整
- 大规模重构
- 接口变更

**合并策略**：需要详细 Code Review

---

## 测试用例审查标准

### 审查问题清单

当 AI 生成的测试用例发现了"Bug"时，需要回答以下问题：

#### 1. 测试意图是否正确？
```
□ 测试的是产品需求定义的行为
□ 测试的是接口契约
□ 测试的是边界情况
□ 测试的是异常处理
```

#### 2. 测试理解是否正确？
```
□ AI 正确理解了代码的设计意图
□ AI 正确理解了业务逻辑
□ AI 没有误解方法的用途
□ AI 没有测试实现细节
```

#### 3. 测试质量如何？
```
□ 测试是独立的，不依赖执行顺序
□ 测试是可重复的
□ 测试有明确的断言
□ 测试命名清晰
```

### 判断矩阵

| 测试意图 | 代码行为 | 结论 |
|---------|---------|------|
| ✅ 正确 | ❌ 错误 | **修复代码**，保留测试 |
| ❌ 错误 | ✅ 正确 | **丢弃测试**，代码不变 |
| ✅ 正确 | ⚠️ 有歧义 | **讨论设计**，可能两边都改 |
| ⚠️ 有歧义 | ⚠️ 有歧义 | **需要澄清需求**，暂不合并 |

---

## 合并流程

### 步骤 1：智能体生成成果清单

智能体完成探索后，必须生成成果清单：

```markdown
## 成果清单

### A类（直接合并）
- [ ] 报告：TEST-20241230-边界测试.md

### B类（需审查测试）
- [ ] 新增测试：ContactRepositoryTest.kt（5个测试）
- [ ] 新增测试：AnalyzeChatUseCaseTest.kt（3个测试）

### C类（需审查修复）
- [ ] 修复：ContactRepository.getById() 空值处理

### D类（需详细Review）
- 无
```

### 步骤 2：人工审查

用户根据成果清单进行审查：

```bash
# 查看分支差异
git diff main..explore/test-xxx

# 查看特定文件
git diff main..explore/test-xxx -- path/to/file.kt
```

### 步骤 3：选择性合并

根据审查结果，选择合适的合并方式：

#### 方式 1：合并整个分支（全部通过审查）
```bash
git checkout main
git merge explore/test-xxx
```

#### 方式 2：Cherry-pick 特定提交
```bash
git cherry-pick <commit-hash>
```

#### 方式 3：只合并特定文件（最常用）
```bash
# 只合并文档文件
git checkout explore/test-xxx -- 文档/开发文档/MA/BUGFIX/xxx.md

# 只合并某个目录下的所有文件
git checkout explore/test-xxx -- 文档/开发文档/MA/

# 合并多个指定文件
git checkout explore/test-xxx -- file1.md file2.md
```

#### 方式 4：交互式选择（推荐）
```bash
# 查看分支有哪些改动
git diff --name-only main..explore/test-xxx

# 输出示例：
# 文档/开发文档/MA/BUGFIX/xxx.md     ← 想要
# src/main/kotlin/SomeCode.kt         ← 不想要
# src/test/kotlin/SomeTest.kt         ← 不确定

# 然后逐个选择要合并的文件
git checkout explore/test-xxx -- 文档/开发文档/MA/BUGFIX/xxx.md
```

---

## 🎯 选择性合并详细指南

### 场景：只要文档，不要代码

这是最常见的场景。探索分支可能产生了：
- ✅ 有价值的分析报告
- ❌ 不确定的代码修改
- ❓ 需要审查的测试

**操作步骤**：

```bash
# 1. 确保在主分支
git checkout main

# 2. 查看探索分支的所有改动文件
git diff --name-only main..explore/xxx

# 3. 筛选出文档文件
git diff --name-only main..explore/xxx | grep "文档/"
git diff --name-only main..explore/xxx | grep "\.md$"

# 4. 只合并文档目录
git checkout explore/xxx -- "文档/开发文档/MA/"

# 5. 提交合并的文档
git add .
git commit -m "合并探索报告：xxx"
```

### 场景：只要测试，不要修复代码

```bash
# 1. 查看测试文件
git diff --name-only main..explore/xxx | grep "Test\.kt$"

# 2. 只合并测试文件
git checkout explore/xxx -- src/test/kotlin/path/to/SomeTest.kt

# 3. 提交
git add .
git commit -m "合并测试用例：xxx"
```

### 场景：合并报告 + 部分代码

```bash
# 1. 先合并所有文档
git checkout explore/xxx -- "文档/"

# 2. 再选择性合并代码
git checkout explore/xxx -- src/main/kotlin/specific/File.kt

# 3. 提交
git add .
git commit -m "合并探索成果：报告 + 部分修复"
```

---

## 📋 快速合并检查清单

在合并前，按以下清单检查：

```markdown
## 合并前检查

### 文档类（直接合并）
- [ ] 报告是否自包含？（删除分支后仍能理解）
- [ ] 报告是否有价值？（不是空洞的模板）
- [ ] 路径是否正确？（应在 文档/开发文档/MA/ 下）

### 测试类（需审查）
- [ ] 测试意图是否正确？
- [ ] 测试是否能通过？
- [ ] 测试是否有价值？

### 代码类（谨慎合并）
- [ ] 修改是否必要？
- [ ] 是否有副作用？
- [ ] 是否需要更多测试？
```

---

## 🔧 实用 Git 命令速查

### 查看改动

```bash
# 查看所有改动的文件名
git diff --name-only main..explore/xxx

# 查看改动的统计（增删行数）
git diff --stat main..explore/xxx

# 查看特定文件的改动内容
git diff main..explore/xxx -- path/to/file

# 只看新增的文件
git diff --name-only --diff-filter=A main..explore/xxx

# 只看修改的文件
git diff --name-only --diff-filter=M main..explore/xxx
```

### 选择性合并

```bash
# 合并单个文件
git checkout explore/xxx -- path/to/file

# 合并整个目录
git checkout explore/xxx -- path/to/directory/

# 合并匹配模式的文件（需要 shell 展开）
git checkout explore/xxx -- $(git diff --name-only main..explore/xxx | grep "\.md$")
```

### 撤销错误合并

```bash
# 撤销未提交的合并
git checkout HEAD -- path/to/file

# 撤销已提交的合并（创建新提交）
git revert <commit-hash>
```

### 步骤 4：清理 Worktree

```bash
git worktree remove ../project-explore-test-xxx
git branch -d explore/test-xxx
```

---

## 测试用例合并决策树

```
发现测试失败
    │
    ▼
测试的是什么？
    │
    ├─► 产品需求定义的行为
    │       │
    │       ▼
    │   代码符合需求吗？
    │       │
    │       ├─► 不符合 → 【修复代码，保留测试】
    │       │
    │       └─► 符合 → 【测试写错了，丢弃测试】
    │
    ├─► 接口契约/文档
    │       │
    │       ▼
    │   代码符合契约吗？
    │       │
    │       ├─► 不符合 → 【修复代码，保留测试】
    │       │
    │       └─► 符合 → 【测试理解错了，丢弃测试】
    │
    └─► 实现细节
            │
            ▼
        【丢弃测试】- 不应该测试实现细节
```

---

## 智能体报告要求

### 测试探索报告必须包含

```markdown
## 发现的问题

### 问题 1：[问题标题]

**测试代码**：
```kotlin
@Test
fun `xxx`() { ... }
```

**失败原因**：
[描述为什么测试失败]

**我的判断**：
- [ ] 代码有 Bug，需要修复
- [ ] 测试理解错误，建议丢弃
- [ ] 设计有歧义，需要讨论

**理由**：
[解释判断的理由]

**建议操作**：
[具体的修复建议或丢弃理由]
```

---

## 最佳实践

### 1. 测试应该测行为，不测实现
```kotlin
// ❌ 错误：测试实现细节
@Test
fun `repository uses HashMap internally`() { ... }

// ✅ 正确：测试行为
@Test
fun `repository returns null when contact not found`() { ... }
```

### 2. 测试应该基于需求，不基于猜测
```kotlin
// ❌ 错误：AI 猜测的行为
@Test
fun `analyzeChat should return exactly 3 suggestions`() { ... }

// ✅ 正确：基于需求的行为
@Test
fun `analyzeChat should return non-empty suggestions`() { ... }
```

### 3. 边界测试是最有价值的
```kotlin
// ✅ 有价值：边界情况
@Test
fun `getContact_whenIdIsEmpty_returnsNull`() { ... }

@Test
fun `saveContact_whenNameExceedsMaxLength_throwsException`() { ... }
```

---

## 总结

| 成果类型 | 风险等级 | 合并策略 | 审查要求 |
|---------|---------|---------|---------|
| 文档报告 | 低 | 直接合并 | 无 |
| 测试用例 | 中 | 选择性合并 | 检查测试意图 |
| Bug 修复 | 中高 | 审查后合并 | Code Review |
| 代码重构 | 高 | 谨慎合并 | 详细 Review |

**核心原则**：
1. 报告永远保留（记录探索过程）
2. 测试需要判断意图是否正确
3. 代码修改需要人工审查
4. 不确定的先不合并
