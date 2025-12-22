---
description: 智能测试 - 自动识别并运行相关测试
---

# 智能测试命令

根据当前上下文自动识别并运行相关测试。

## 使用方式

```
/QuickTest                    # 运行当前文件相关的测试
/QuickTest EditFactUseCase    # 运行指定类的测试
/QuickTest --all              # 运行所有单元测试
/QuickTest --changed          # 只运行修改文件相关的测试
```

## 执行流程

### 1. 识别测试范围

**如果指定了类名：**
- 直接查找 `{类名}Test.kt` 文件

**如果使用 --changed：**
- 获取 git 修改的文件列表
- 为每个修改的源文件查找对应测试

**如果无参数：**
- 使用当前打开的文件
- 查找对应的测试文件

### 2. 执行测试

使用快速测试脚本：
```cmd
scripts\quick-test.bat {TestClassName}
```

### 3. 解析结果

分析测试输出，提供简洁的结果摘要：
```
✅ 测试通过: 15/15
   - EditFactUseCaseTest: 8 passed
   - ContentValidatorTest: 7 passed
   
⏱️ 耗时: 12.3s
```

或者失败时：
```
❌ 测试失败: 13/15
   
失败用例:
1. EditFactUseCaseTest.`编辑空内容应返回错误`
   Expected: ValidationError
   Actual: Success
   
2. ContentValidatorTest.`超长文本应被截断`
   Expected: 1000 chars
   Actual: 1500 chars

💡 建议: 检查 ContentValidator.kt 第 45 行的长度限制逻辑
```

## 快捷操作

测试完成后提供快捷操作：
- `[重新运行]` - 再次运行相同测试
- `[运行全部]` - 运行所有单元测试
- `[查看详情]` - 显示完整测试日志
