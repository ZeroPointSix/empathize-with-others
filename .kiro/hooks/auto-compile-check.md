---
event: onFileSave
filePattern: "**/*.kt"
description: 保存 Kotlin 文件时自动检查编译错误
---

# 自动编译检查 Hook

当保存 Kotlin 文件时，自动执行以下操作：

## 触发条件
- 文件扩展名为 `.kt`
- 文件位于 `app/src/main/` 或 `app/src/test/` 目录下

## 执行动作

### 1. 快速语法检查
使用 `getDiagnostics` 工具检查当前文件的编译错误。

### 2. 如果有错误
- 在聊天中显示错误摘要
- 提供修复建议

### 3. 如果无错误
- 静默通过，不打扰用户

## 注意事项
- 不执行完整构建，只做语法检查
- 不阻塞用户操作
