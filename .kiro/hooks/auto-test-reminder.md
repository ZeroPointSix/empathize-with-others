---
event: onFileSave
filePattern: "**/domain/usecase/*.kt"
description: 保存 UseCase 时提醒运行相关测试
---

# UseCase 测试提醒 Hook

当保存 UseCase 文件时，自动提醒运行相关测试。

## 触发条件
- 文件路径匹配 `**/domain/usecase/*.kt`
- 文件名以 `UseCase.kt` 结尾

## 执行动作

### 1. 查找对应测试文件
根据 UseCase 名称，在 `app/src/test/` 目录下查找对应的测试文件。

例如：
- `EditFactUseCase.kt` → `EditFactUseCaseTest.kt`

### 2. 如果测试文件存在
提示用户：
```
💡 检测到 UseCase 修改，建议运行测试：
scripts\quick-test.bat EditFactUseCaseTest
```

### 3. 如果测试文件不存在
提示用户：
```
⚠️ 未找到对应测试文件，是否需要生成测试骨架？
```

## 可选操作
用户可以选择：
- 立即运行测试
- 生成测试骨架
- 忽略提醒
