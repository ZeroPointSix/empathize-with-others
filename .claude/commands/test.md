---
description: 调用 Test 子代理 - 高级测试架构师，直接生成测试代码文件
argument-hint: [测试目标代码路径/功能描述]
---

# 🧪 Test 子代理调用

> **核心原则**: 直接生成测试代码文件，不生成报告。测试是架构级的质量保障。

## 测试目标

$ARGUMENTS

---

## ⚡ 执行模式：直接写入

**重要**: 本命令直接创建/修改测试文件，不生成任何报告文档。

执行完成后：
1. ✅ 测试文件已创建/更新 (如 `xxx_test.go`)
2. ✅ 在聊天中简要说明生成了哪些测试
3. ❌ 不生成 markdown 报告
4. ❌ 不输出"输出模板"

---

## 🔴 执行前强制读取

### 1. 子代理定义
读取 `subagent/Test.md` 获取完整的测试协议

### 2. 项目规范
- `Rules/RulesReadMe.md` - 项目通用规则
- `Rules/CORE_PRINCIPLES.md` - 核心原则

### 3. 技能系统
- `skills/test-driven-development/SKILL.md` - TDD 实践
- `skills/verification/SKILL.md` - 功能验收

---

## 📋 执行流程

### Phase 1: 上下文收集 (快速)

1. **检查 PRD 目录**: `文档/开发文档/PRD/` - 提取验收标准 (AC)
2. **检查 TDD 目录**: `文档/开发文档/TDD/` - 提取边界条件
3. **检查 FD 目录**: `文档/开发文档/FD/` - 提取任务测试要求
4. **分析目标代码** - 可测试性和依赖分析

---

### Phase 2: 测试分析 (内部决策)

**内部完成，不输出**：
- 识别参数化测试机会
- 识别属性测试机会
- 从 AC 提取测试场景
- 选择最优测试策略

---

### Phase 3: 直接生成测试文件 ⚠️ 核心

**直接使用文件编辑工具创建测试文件**

#### 测试文件位置
- 单元测试: 与源文件同目录，命名 `xxx_test.go`
- 集成测试: `test/integration/` 目录

#### 测试代码规范

```go
// Test_FunctionName_Scenario_ExpectedResult 测试 [功能描述]
//
// 业务规则 (PRD-00001/AC-002): [验收标准描述]
// 任务: FD-00001/T003
func Test_FunctionName_Scenario_ExpectedResult(t *testing.T) {
    // Arrange
    // ...
    
    // Act
    // ...
    
    // Assert
    // ...
}
```

#### 优先使用 Table-Driven Tests

```go
func Test_FunctionName(t *testing.T) {
    // 测试数据来源: PRD-00001/AC-002
    tests := []struct {
        name     string
        input    interface{}
        expected interface{}
    }{
        {"边界值测试", input1, expected1},
        {"正常流程", input2, expected2},
    }
    
    for _, tt := range tests {
        t.Run(tt.name, func(t *testing.T) {
            // ...
        })
    }
}
```

---

### 测试层次

```
📊 测试金字塔
├── 单元测试 (Unit Tests) - 70%
├── 集成测试 (Integration Tests) - 20%
└── E2E 测试 (End-to-End Tests) - 10%
```

---

### 🚫 绝对禁止

1. **禁止生成报告** - 直接写入测试文件
2. **禁止输出模板** - 不要输出 markdown 报告模板
3. **禁止冗余测试** - 使用参数化测试合并相似用例
4. **禁止脱离文档** - 测试注释必须关联 PRD/TDD/FD

---

## ✅ 执行完成标准

执行完成后，在聊天中简要回复：

```
已生成测试文件 [测试文件路径]：
- [N] 个单元测试 (覆盖 AC-001, AC-002...)
- [M] 个集成测试
- 使用了 table-driven tests
```

**不要输出任何 markdown 报告模板！**

---

## 📝 示例用法

```
/test kit/internal/service/pull_service.go
```

执行结果：直接创建 `kit/internal/service/pull_service_test.go`
