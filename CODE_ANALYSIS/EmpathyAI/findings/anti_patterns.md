# 反模式 (Anti-Patterns)

> 技术债务分析报告 - 反模式识别
> 分析日期: 2026-01-03

---

## 1. 魔数 (Magic Numbers)

### 问题描述
代码中存在大量硬编码的数值常量，缺乏语义化命名。

### 发现的问题

| 文件位置 | 魔数值 | 语义 | 问题等级 |
|---------|-------|------|---------|
| `FloatingWindowService.kt:1413` | `10 * 60 * 1000L` | 10分钟过期时间 | 🟡 中 |
| `FloatingWindowService.kt:1428` | `10 * 1000L` | 10秒处理超时 | 🟡 中 |
| `FloatingWindowService.kt:3138` | `10 * 60 * 1000L` | 10分钟清理延迟 | 🟡 中 |
| `AiProvider.kt:28` | `30000L` | 默认30秒超时 | 🟡 中 |
| `FloatingWindowPreferencesBubbleTest.kt:155,167` | `5 * 60 * 1000`, `15 * 60 * 1000` | 5/15分钟 | 🟡 低 |
| 多处 | `5`, `10`, `30` | 时间/数量阈值 | 🟡 低 |

### 建议措施
```kotlin
// 改为命名常量
companion object {
    private const val EXPIRATION_TIMEOUT_MS = 10 * 60 * 1000L  // 10分钟
    private const val PROCESSING_TIMEOUT_MS = 10 * 1000L       // 10秒
    private const val CLEANUP_DELAY_MS = 10 * 60 * 1000L       // 10分钟
    private const val DEFAULT_TIMEOUT_MS = 30_000L             // 30秒
}
```

---

## 2. 硬编码字符串 (Hard-coded Strings)

### 问题描述
多处使用硬编码的中文字符串，建议统一管理。

### 发现的问题

| 文件位置 | 硬编码内容 | 问题等级 |
|---------|-----------|---------|
| `UserProfileScreen.kt:877` | `"维度名称至少2个字符"` | 🟡 中 |
| `UserProfileScreen.kt:878` | `"维度名称不超过10个字符"` | 🟡 中 |
| `UserProfileScreen.kt:663,671` | `"标签"` 相关文本 | 🟡 低 |
| `AiConfigViewModel.kt:767,789` | 验证提示文本 | 🟡 低 |

### 建议措施
- 提取到 `strings.xml` 资源文件
- 创建专用的 `ErrorMessages` 或 `UiConstants` 对象
- 支持多语言国际化

---

## 3. 过度使用 Log 语句 (Excessive Logging)

### 问题描述
项目中有大量 `android.util.Log` 调用，可能影响性能且增加日志噪音。

### 统计信息

| 指标 | 数值 |
|-----|------|
| 使用 Log 的文件数 | 138 |
| 涉及的模块 | domain, data, presentation, app |

### 发现的问题

| 文件位置 | Log调用密度 | 问题等级 |
|---------|------------|---------|
| `FloatingWindowService.kt` | 高密度 | 🟡 中 |
| `FloatingView.kt` | 高密度 | 🟡 中 |
| `AiRepositoryImpl.kt` | 中密度 | 🟡 低 |

### 建议措施
- 使用统一的 `DebugLogger` 工具类
- 添加日志级别控制
- 在 Release 版本中禁用详细日志

---

## 4. 嵌套回调/协程滥用 (Excessive Nesting)

### 问题描述
部分代码存在深层嵌套的协程调用，增加理解和维护成本。

### 发现的问题

| 文件位置 | 嵌套层级 | 问题等级 |
|---------|---------|---------|
| `FloatingWindowService.kt` 多处 | 4-5层 | 🟡 中 |
| `ContactDetailTabViewModel.kt` | 3-4层 | 🟡 低 |

### 建议措施
- 使用 `suspend` 函数拆分逻辑
- 使用 `coroutineScope` 或 `withContext` 组织代码
- 提取异步逻辑到独立函数

---

## 5. 异常处理不当 (Improper Exception Handling)

### 问题描述
部分异常处理过于宽泛，可能隐藏真实问题。

### 发现的问题

| 文件位置 | 问题描述 | 问题等级 |
|---------|---------|---------|
| 多处 Repository | `catch (Exception e)` 过于宽泛 | 🟡 中 |
| 多处 | 吞掉异常不做处理 | 🟡 低 |

### 建议措施
- 使用具体的异常类型
- 记录异常上下文信息
- 考虑使用 `Result<T>` 类型替代异常

---

## 6. 过度使用 Companion Object

### 统计信息

| 指标 | 数值 |
|-----|------|
| 使用 companion object 的文件数 | 138 |

### 问题分析
- 部分文件存在不必要的 `companion object`
- 某些常量可以直接放在文件顶层

### 建议措施
- 对于不与类绑定的常量，使用顶层声明
- 保持 `companion object` 用于真正的类级常量

---

## 总结

| 反模式类型 | 严重程度 | 修复优先级 |
|-----------|---------|-----------|
| 魔数 (Magic Numbers) | 🟡 中 | P2 |
| 硬编码字符串 | 🟡 中 | P3 |
| 过度使用 Log | 🟡 中 | P3 |
| 嵌套回调/协程 | 🟡 中 | P2 |
| 异常处理不当 | 🟡 中 | P2 |
| 过度使用 Companion Object | 🟡 低 | P3 |

---

## 建议修复计划

### P2 优先级修复
1. 统一时间常量的命名
2. 完善异常处理逻辑
3. 优化协程调用结构

### P3 优先级修复
1. 提取硬编码字符串到资源文件
2. 规范日志使用
3. 精简 companion object 使用

---

*报告生成时间: 2026-01-03*
*工具: Claude Code Technical Debt Analysis*
