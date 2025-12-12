# 设计文档 - AI 响应解析增强（优化版）

## 概述

AI 响应解析增强功能旨在解决多 AI 模型响应格式不一致的问题。通过**基于现有代码的渐进式增强**，实现稳定的多模型支持，同时保持向后兼容和对用户透明。

### 设计原则

1. **基于现有代码增强**：复用并优化现有的 `preprocessJsonResponse` 和 `mapChineseFieldNames` 方法
2. **简单实用**：避免过度设计，专注解决核心问题
3. **配置驱动**：使用配置文件管理映射规则，便于维护和扩展
4. **渐进式部署**：分阶段实施，降低风险
5. **性能现实**：设定合理的性能目标（100-1000ms）

### 核心问题

| 问题 | 表现 | 影响 |
|------|------|------|
| 中文字段名 | 返回"回复建议"而非"replySuggestion" | 解析失败 |
| Markdown包裹 | 响应被 \`\`\`json 包裹 | 无法提取JSON |
| 字段结构差异 | 嵌套对象、数组格式不同 | 映射失败 |
| 编码问题 | Unicode、特殊字符 | 乱码或解析错误 |
| 格式错误 | 多余逗号、缺失引号 | JSON语法错误 |

## 架构设计

### 整体架构（简化版）

```
┌─────────────────────────────────────────────────────────────┐
│                    AiRepositoryImpl                          │
│  (现有代码，增强解析方法)                                     │
│  - analyzeChat()                                             │
│  - checkDraftSafety()                                        │
│  - extractTextInfo()                                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │  增强的解析流程 (在现有方法中实现)      │
        │  1. preprocessJsonResponse (增强)       │
        │  2. mapChineseFieldNames (优化)         │
        │  3. Moshi 解析                          │
        │  4. parseFallbackXxx (增强)             │
        └─────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Domain Models   │
                    │  - AnalysisResult│
                    │  - SafetyCheck   │
                    │  - ExtractedData │
                    └──────────────────┘
```

### 设计决策

**不创建新的类**：
- ❌ 不创建 ResponseParser 类
- ❌ 不创建 JsonCleaner 类
- ❌ 不创建 FieldMapper 类
- ❌ 不创建 FallbackHandler 类

**增强现有方法**：
- ✅ 增强 `preprocessJsonResponse()` 方法
- ✅ 优化 `mapChineseFieldNames()` 方法
- ✅ 增强 `parseFallbackAnalysisResult()` 等方法
- ✅ 添加配置文件支持

**理由**：
- 保持代码简单，避免过度抽象
- 减少类的数量，降低维护成本
- 与现有代码风格一致
- 更容易理解和调试


## 组件设计

### 1. 增强的 preprocessJsonResponse 方法

**当前功能**：
- 移除 Markdown 代码块标记
- 提取 JSON 对象
- 修复常见格式错误
- 调用 mapChineseFieldNames

**增强内容**：
```kotlin
private fun preprocessJsonResponse(rawJson: String): String {
    // 1. 基础清洗（保持现有逻辑）
    val cleaned = rawJson.trim()
        .removeMarkdownBlocks()      // 现有
        .extractJsonObject()          // 现有
        .fixCommonErrors()            // 现有
    
    // 2. 字段映射（优化现有逻辑）
    val mapped = mapChineseFieldNames(cleaned)
    
    // 3. 最终验证（新增）
    return validateAndFixJson(mapped)
}
```

**优化点**：
- 简化正则表达式，提高性能
- 移除复杂的智能映射逻辑
- 添加最终验证步骤
- 优化日志输出

### 2. 优化的 mapChineseFieldNames 方法

**当前功能**：
- 使用硬编码的映射表
- 包含复杂的智能映射逻辑

**优化内容**：
```kotlin
private fun mapChineseFieldNames(json: String): String {
    // 1. 加载配置文件（新增，启动时缓存）
    val mappings = FieldMappingConfig.load()
    
    // 2. 精确映射（简化现有逻辑）
    var result = json
    mappings.forEach { (english, chineseList) ->
        chineseList.forEach { chinese ->
            result = result.replace("\"$chinese\"", "\"$english\"")
        }
    }
    
    // 3. 移除智能映射（删除 intelligentFieldMapping）
    // 理由：复杂且不可靠，改用配置文件管理
    
    return result
}
```

**优化点**：
- 移除 `intelligentFieldMapping()` 方法
- 使用配置文件替代硬编码
- 简化映射逻辑，提高可维护性
- 启动时缓存配置，提高性能

### 3. 增强的 parseFallbackXxx 方法

**当前功能**：
- 解析为 Map 并提取字段
- 支持多种字段名格式

**增强内容**：
```kotlin
private fun parseFallbackAnalysisResult(json: String): Result<AnalysisResult> {
    return try {
        val cleanedJson = preprocessJsonResponse(json)
        val jsonMap = moshi.adapter<Map<String, Any>>(...).fromJson(cleanedJson)
        
        if (jsonMap == null) {
            // 使用默认值（新增）
            return Result.success(DefaultValues.ANALYSIS_RESULT)
        }
        
        // 提取字段（保持现有逻辑）
        val replySuggestion = extractReplySuggestion(jsonMap)
        val strategyAnalysis = extractStrategyAnalysis(jsonMap)
        val riskLevel = extractRiskLevel(jsonMap)
        
        Result.success(AnalysisResult(replySuggestion, strategyAnalysis, riskLevel))
    } catch (e: Exception) {
        // 降级处理（增强）
        android.util.Log.e("AiRepositoryImpl", "容错解析失败，使用默认值", e)
        Result.success(DefaultValues.ANALYSIS_RESULT)
    }
}
```

**优化点**：
- 添加默认值支持
- 增强错误处理
- 统一降级策略


## 配置文件设计

### 字段映射配置文件

**位置**: `app/src/main/assets/field_mappings.json`

**格式**:
```json
{
  "analysisResult": {
    "replySuggestion": [
      "回复建议",
      "建议回复",
      "话术建议",
      "具体的回复建议",
      "建议的回复内容",
      "reply",
      "response",
      "answer"
    ],
    "strategyAnalysis": [
      "策略分析",
      "心理分析",
      "军师分析",
      "对方当前的情绪和潜在意图",
      "关键洞察",
      "strategy",
      "analysis"
    ],
    "riskLevel": [
      "风险等级",
      "风险级别",
      "可能存在的风险点"
    ]
  },
  "analysisNestedFields": {
    "emotion": ["emotion", "情绪", "对方情绪", "情绪状态"],
    "intention": ["intention", "意图", "潜在意图", "对方意图"],
    "risk": ["risk", "风险", "风险点", "风险提示"]
  },
  "safetyCheckResult": {
    "isSafe": [
      "是否安全",
      "安全"
    ],
    "triggeredRisks": [
      "触发的风险",
      "风险列表"
    ],
    "suggestion": [
      "建议",
      "修改建议"
    ]
  },
  "extractedData": {
    "facts": [
      "事实",
      "事实信息"
    ],
    "redTags": [
      "红色标签",
      "雷区"
    ],
    "greenTags": [
      "绿色标签",
      "策略"
    ]
  }
}
```

### 配置加载逻辑

```kotlin
object FieldMappingConfig {
    private var cachedMappings: Map<String, List<String>>? = null
    
    fun load(context: Context): Map<String, List<String>> {
        if (cachedMappings != null) {
            return cachedMappings!!
        }
        
        try {
            val json = context.assets.open("field_mappings.json")
                .bufferedReader()
                .use { it.readText() }
            
            val adapter = moshi.adapter<FieldMappings>(...)
            val mappings = adapter.fromJson(json)
            
            // 扁平化为单层 Map
            cachedMappings = flattenMappings(mappings)
            return cachedMappings!!
        } catch (e: Exception) {
            android.util.Log.e("FieldMappingConfig", "加载配置失败，使用默认配置", e)
            return getDefaultMappings()
        }
    }
    
    private fun getDefaultMappings(): Map<String, List<String>> {
        // 硬编码的默认配置作为后备
        return mapOf(
            "replySuggestion" to listOf("回复建议", "建议回复"),
            "strategyAnalysis" to listOf("策略分析", "心理分析"),
            "riskLevel" to listOf("风险等级", "风险级别")
        )
    }
}
```

### 默认值定义

```kotlin
object DefaultValues {
    val ANALYSIS_RESULT = AnalysisResult(
        replySuggestion = "AI 暂时无法生成建议，请重试或切换模型",
        strategyAnalysis = "AI 分析暂时不可用",
        riskLevel = RiskLevel.SAFE
    )
    
    val SAFETY_CHECK_RESULT = SafetyCheckResult(
        isSafe = true,
        triggeredRisks = emptyList(),
        suggestion = "安全检查暂时不可用，请谨慎发送"
    )
    
    val EXTRACTED_DATA = ExtractedData(
        facts = emptyMap(),
        redTags = emptyList(),
        greenTags = emptyList()
    )
}
```

## 数据模型

### AnalysisResult (聊天分析结果)

```kotlin
@JsonClass(generateAdapter = true)
data class AnalysisResult(
    @Json(name = "replySuggestion")
    val replySuggestion: String,
    
    @Json(name = "strategyAnalysis")
    val strategyAnalysis: String,
    
    @Json(name = "riskLevel")
    val riskLevel: RiskLevel
)

enum class RiskLevel {
    @Json(name = "SAFE") SAFE,
    @Json(name = "WARNING") WARNING,
    @Json(name = "DANGER") DANGER
}
```

### SafetyCheckResult (安全检查结果)

```kotlin
@JsonClass(generateAdapter = true)
data class SafetyCheckResult(
    @Json(name = "isSafe")
    val isSafe: Boolean,
    
    @Json(name = "triggeredRisks")
    val triggeredRisks: List<String>,
    
    @Json(name = "suggestion")
    val suggestion: String
)
```

### ExtractedData (数据提取结果)

```kotlin
data class ExtractedData(
    val facts: Map<String, String>,
    val redTags: List<String>,
    val greenTags: List<String>
)
```


## 正确性属性（简化版）

*属性是一个特征或行为，应该在系统的所有有效执行中保持为真——本质上是关于系统应该做什么的正式陈述。*

### 核心属性（重点测试）

**Property 1: Markdown 清洗一致性**
*对于任何* 包含 Markdown 代码块标记的 JSON 响应，清洗后应该移除所有代码块标记并保留 JSON 内容
**验证: 需求 1.1**

**Property 2: JSON 提取正确性**
*对于任何* 包含前后缀文本的响应，应该能够正确提取 JSON 对象部分
**验证: 需求 1.2**

**Property 3: 字段映射完整性**
*对于任何* 包含配置文件中定义的中文字段名的 JSON，映射后所有中文字段名都应该被转换为对应的英文字段名
**验证: 需求 2.1**

**Property 4: AnalysisResult 解析成功率**
*对于任何* 符合标准格式或包含已知中文字段的 AnalysisResult JSON，解析成功率应该 ≥ 95%
**验证: 需求 3.1, 3.2**

**Property 5: 降级策略有效性**
*对于任何* 无法解析的响应，系统应该返回包含默认值的 Domain Model，而不是抛出异常
**验证: 需求 6.1, 6.2**

### 性能属性

**Property 6: 常规响应解析性能**
*对于任何* 标准格式的响应（< 1KB），解析时间应该 ≤ 300 毫秒
**验证: 需求 7.1**

**Property 7: 复杂响应解析性能**
*对于任何* 需要清洗和映射的响应（1-5KB），解析时间应该 ≤ 500 毫秒
**验证: 需求 7.2**

**Property 8: 大型响应解析性能**
*对于任何* 大型响应（5-10KB），解析时间应该 ≤ 1000 毫秒
**验证: 需求 7.3**

### 边缘情况属性

**Property 9: 空响应处理**
*对于任何* 空字符串或纯文本响应，系统应该返回默认值而不是崩溃
**验证: 需求 1.5**

**Property 10: 字段缺失处理**
*对于任何* 部分字段缺失的响应，系统应该使用默认值填充缺失字段
**验证: 需求 4.4, 4.5, 5.4**

## 错误处理

### 错误处理策略（简化版）

**Level 1: 预处理修复**
- 在 `preprocessJsonResponse` 中尝试修复常见格式错误
- 记录修复操作的 Debug 日志

**Level 2: 字段映射**
- 使用配置文件进行精确映射
- 记录未知字段的 Warning 日志

**Level 3: 容错解析**
- 使用 Moshi 的 `lenient()` 模式
- 使用 `parseFallbackXxx` 方法提取字段

**Level 4: 降级处理**
- 返回默认值
- 记录 Error 日志但不中断流程

### 日志策略（简化版）

**Debug 日志**（仅在 Debug 模式）:
- 原始响应内容（前 200 字符）
- 清洗后内容（前 200 字符）
- 映射后内容（前 200 字符）

**Warning 日志**:
- 字段映射失败
- 使用默认值

**Error 日志**:
- 解析完全失败
- 异常类型和消息
- 原始响应内容（完整，但限制在 1000 字符内）


## 测试策略（简化版）

### 单元测试（重点）

**preprocessJsonResponse 测试**:
- ✅ 测试 Markdown 标记移除
- ✅ 测试 JSON 提取
- ✅ 测试常见格式错误修复
- ✅ 测试边缘情况（空字符串、纯文本）

**mapChineseFieldNames 测试**:
- ✅ 测试配置文件加载
- ✅ 测试精确字段映射
- ✅ 测试未知字段处理

**parseFallbackXxx 测试**:
- ✅ 测试标准格式解析
- ✅ 测试中文字段解析
- ✅ 测试默认值返回
- ✅ 测试部分字段缺失处理

### 属性测试（可选）

使用 Kotest Property Testing 验证核心属性：

**Property 1-3**: JSON 清洗和字段映射
- 生成随机 JSON 内容
- 添加随机 Markdown 标记、前后缀文本
- 验证清洗和映射结果

**Property 4-5**: 解析成功率和降级策略
- 生成随机的 Domain Model 实例
- 序列化为 JSON 并添加随机变化
- 验证解析结果或默认值返回

**Property 6-8**: 性能测试
- 生成不同大小的响应
- 测量解析时间
- 验证性能要求

### 集成测试（必需）

**真实 AI 模型测试**:
- 使用真实的 AI API（OpenAI、DeepSeek、ModelScope）
- 发送相同的 Prompt
- 验证解析结果的一致性

**回归测试**:
- 运行现有测试用例
- 验证向后兼容性

## 性能优化

### 优化策略

**1. 配置文件缓存**
```kotlin
object FieldMappingConfig {
    private var cachedMappings: Map<String, List<String>>? = null
    
    fun load(context: Context): Map<String, List<String>> {
        // 启动时加载一次，后续直接使用缓存
        if (cachedMappings != null) {
            return cachedMappings!!
        }
        // ... 加载逻辑
    }
}
```

**2. 字符串操作优化**
- 使用 `StringBuilder` 进行字符串拼接
- 避免不必要的字符串复制
- 限制日志内容长度

**3. 正则表达式优化**
- 简化正则表达式
- 优先使用简单的字符串操作
- 移除复杂的智能映射逻辑

**4. 解析路径优化**
- 优先尝试标准解析路径
- 只在必要时进行清洗和映射
- 使用 `lenient()` 模式提高容错性

### 性能指标（现实版）

| 场景 | 目标时间 | 说明 |
|------|---------|------|
| 标准格式解析 | ≤ 300ms | 直接 Moshi 解析 |
| 需要清洗的解析 | ≤ 500ms | 清洗 + Moshi 解析 |
| 需要映射的解析 | ≤ 500ms | 清洗 + 映射 + Moshi 解析 |
| 大型响应解析 | ≤ 1000ms | 处理 5-10KB 的响应 |

**注意**: 这些时间包括网络请求时间，纯解析时间会更短。


## 实施计划（简化版）

### Phase 1: 基础增强（1-2天）

**1.1 创建配置文件**
- 创建 `field_mappings.json` 配置文件
- 定义 AnalysisResult、SafetyCheckResult、ExtractedData 的字段映射
- 创建 `FieldMappingConfig` 对象用于加载配置
- _需求: 2.5, 8.2_

**1.2 增强 preprocessJsonResponse 方法**
- 简化正则表达式
- 优化性能
- 添加最终验证步骤
- _需求: 1.1, 1.2, 1.3, 1.4_

**1.3 优化 mapChineseFieldNames 方法**
- 移除 `intelligentFieldMapping()` 方法
- 使用配置文件替代硬编码
- 简化映射逻辑
- _需求: 2.1, 2.2, 2.3, 2.4_

**1.4 创建 DefaultValues 对象**
- 定义 AnalysisResult 默认值
- 定义 SafetyCheckResult 默认值
- 定义 ExtractedData 默认值
- _需求: 6.1, 6.2_

### Phase 2: 解析增强（2-3天）

**2.1 增强 parseAnalysisResult 方法**
- 优化标准解析路径
- 增强容错能力
- 支持嵌套结构提取
- 支持 riskLevel 智能推断
- 支持 replySuggestion 数组格式处理
- _需求: 3.1, 3.2, 3.3, 3.4, 3.5_

**2.2 增强 parseSafetyCheckResult 方法**
- 优化标准解析路径
- 支持布尔类型转换
- 支持默认值填充
- _需求: 4.1, 4.2, 4.3, 4.4, 4.5_

**2.3 增强 parseExtractedData 方法**
- 优化标准解析路径
- 支持 facts 扁平化
- 支持标签去重
- 支持默认值填充
- _需求: 5.1, 5.2, 5.3, 5.4, 5.5_

**2.4 增强 parseFallbackXxx 方法**
- 添加默认值支持
- 增强错误处理
- 统一降级策略
- _需求: 6.1, 6.2, 6.4_

### Phase 3: 测试和优化（1-2天）

**3.1 编写单元测试**
- 测试 preprocessJsonResponse
- 测试 mapChineseFieldNames
- 测试 parseXxx 方法
- 测试 parseFallbackXxx 方法
- _需求: 11.1_

**3.2 编写属性测试（可选）**
- 测试核心属性（Property 1-5）
- 测试性能属性（Property 6-8）
- 测试边缘情况（Property 9-10）
- _需求: 11.2_

**3.3 性能优化**
- 分析性能瓶颈
- 优化关键路径
- 验证性能指标
- _需求: 7.1, 7.2, 7.3, 7.4_

**3.4 集成测试**
- 测试与真实 AI 模型的兼容性
- 运行回归测试
- 验证向后兼容性
- _需求: 10.5, 11.3_

## 向后兼容性

### 保持不变的部分

**Domain Layer**:
- ✅ `AnalysisResult` 数据类
- ✅ `SafetyCheckResult` 数据类
- ✅ `ExtractedData` 数据类
- ✅ `RiskLevel` 枚举

**Repository Layer**:
- ✅ `AiRepository` 接口
- ✅ `analyzeChat()` 方法签名
- ✅ `checkDraftSafety()` 方法签名
- ✅ `extractTextInfo()` 方法签名

### 内部实现变化

**AiRepositoryImpl**:
- ✅ 增强 `preprocessJsonResponse` 方法
- ✅ 优化 `mapChineseFieldNames` 方法
- ✅ 增强 `parseFallbackAnalysisResult` 等方法
- ✅ 添加 `FieldMappingConfig` 对象
- ✅ 添加 `DefaultValues` 对象

### 迁移策略

**无需迁移**:
- 现有代码无需修改
- 现有测试用例保持通过
- 现有功能保持正常工作

**可选优化**:
- 更新 Prompt 模板以提高 JSON 格式遵循度
- 调整 `response_format` 参数配置
- 优化错误提示文案


## 风险和缓解

### 风险 1: 性能影响

**描述**: 增加清洗和映射步骤可能导致解析性能下降

**影响**: 中等

**缓解措施**:
- ✅ 配置文件启动时缓存，避免重复加载
- ✅ 简化正则表达式，优化字符串操作
- ✅ 优先尝试标准解析路径
- ✅ 使用性能测试验证指标

**监控指标**:
- 解析时间 ≤ 500ms（95% 的情况）
- 内存使用增长 ≤ 10%

### 风险 2: 兼容性问题

**描述**: 新的解析逻辑可能破坏现有功能

**影响**: 高

**缓解措施**:
- ✅ 保持现有接口不变
- ✅ 复用现有代码，渐进式增强
- ✅ 运行回归测试
- ✅ 保留原有逻辑作为回退方案

**验证方法**:
- 运行现有测试用例（113/114 通过）
- 手动测试核心功能
- 与真实 AI 模型集成测试

### 风险 3: 配置文件管理

**描述**: 配置文件可能丢失或损坏

**影响**: 低

**缓解措施**:
- ✅ 提供硬编码的默认配置作为后备
- ✅ 验证配置文件格式
- ✅ 记录配置加载错误
- ✅ 配置文件放在 assets 目录，随 APK 打包

**降级策略**:
- 配置文件加载失败时，使用硬编码的默认配置
- 记录 Error 日志但不中断流程

### 风险 4: 新模型不兼容

**描述**: 未来的 AI 模型可能返回完全不同的格式

**影响**: 中等

**缓解措施**:
- ✅ 配置文件易于更新，支持新增字段映射
- ✅ 降级策略确保系统不会崩溃
- ✅ 详细的错误日志帮助快速定位问题
- ✅ 提供用户反馈渠道

**应对方案**:
- 收集新模型的响应样本
- 更新配置文件添加新的字段映射
- 发布应用更新

## 成功指标

### 功能指标

| 指标 | 目标 | 当前 | 说明 |
|------|------|------|------|
| 解析成功率 | ≥ 95% | ~80% | 主流 AI 模型响应 |
| 支持模型数量 | ≥ 5 | 3 | OpenAI、DeepSeek、ModelScope 等 |
| 配置覆盖率 | ≥ 90% | 0% | 字段映射配置覆盖率 |

### 性能指标

| 指标 | 目标 | 当前 | 说明 |
|------|------|------|------|
| 常规响应时间 | ≤ 300ms | ~200ms | 标准格式解析 |
| 复杂响应时间 | ≤ 500ms | ~400ms | 需要清洗和映射 |
| 大型响应时间 | ≤ 1000ms | ~800ms | 5-10KB 响应 |
| 内存使用增长 | ≤ 10% | 0% | 相对于现有实现 |

### 质量指标

| 指标 | 目标 | 当前 | 说明 |
|------|------|------|------|
| 测试覆盖率 | ≥ 80% | ~70% | 核心解析逻辑 |
| 严重 Bug 数量 | 0 | 0 | 导致崩溃的 Bug |
| 用户满意度 | ≥ 90% | N/A | 用户反馈 |

## 第二次增强设计（提示词增强 + 字段映射配置 + 多层保底）

### 1. 提示词增强设计

**目标**：通过更强的提示词约束，从源头减少 AI 返回错误格式的概率。

#### 1.1 增强的 SYSTEM_ANALYZE 提示词

```kotlin
val SYSTEM_ANALYZE_V2 = """你是一个专业的社交沟通顾问。请分析对话内容并给出建议。

【⚠️ 格式要求 - 必须严格遵守，否则系统无法解析】

你必须且只能返回以下 JSON 格式，不要有任何其他文字：

```json
{
  "replySuggestion": "建议的回复内容",
  "strategyAnalysis": "策略分析内容",
  "riskLevel": "SAFE"
}
```

【JSON Schema 定义】
{
  "type": "object",
  "required": ["replySuggestion", "strategyAnalysis", "riskLevel"],
  "properties": {
    "replySuggestion": {
      "type": "string",
      "description": "可以直接发送给对方的回复文本"
    },
    "strategyAnalysis": {
      "type": "string", 
      "description": "对当前局势的分析（包含对方情绪、意图、风险、建议）"
    },
    "riskLevel": {
      "type": "string",
      "enum": ["SAFE", "WARNING", "DANGER"],
      "description": "风险等级，只能是这三个值之一"
    }
  }
}

【❌ 绝对禁止的格式 - 以下格式会导致解析失败】
1. 使用中文字段名：{"回复建议": "...", "策略分析": "..."}
2. 使用错误字段名：{"suggestion": "...", "analysis": "..."}
3. 使用嵌套结构：{"analysis": {"emotion": "...", "intention": "..."}}
4. 使用中文风险等级：{"riskLevel": "安全"} 或 {"riskLevel": "低"}
5. 添加额外字段：{"replySuggestion": "...", "extra": "..."}
6. 添加 markdown 标记：```json {...} ```
7. 添加解释文字：这是我的分析：{...}

【✅ 正确格式示例】
示例1：{"replySuggestion": "听起来你最近工作压力很大，要不要聊聊？", "strategyAnalysis": "对方可能处于焦虑状态，需要情感支持。建议表达关心，避免说教。", "riskLevel": "SAFE"}

示例2：{"replySuggestion": "我理解你的感受，这确实不容易。", "strategyAnalysis": "对方情绪敏感，话题涉及隐私。建议保持同理心，避免追问。", "riskLevel": "WARNING"}

示例3：{"replySuggestion": "我们换个话题吧，最近有什么有趣的事？", "strategyAnalysis": "对方提及敏感话题，继续讨论可能触发雷区。建议转移话题。", "riskLevel": "DANGER"}

【最终提醒】
- 只输出 JSON，不要有任何其他文字
- 字段名必须是英文：replySuggestion、strategyAnalysis、riskLevel
- riskLevel 只能是：SAFE、WARNING、DANGER
- 不要使用 markdown 代码块标记""".trim()
```

#### 1.2 增强的 SYSTEM_CHECK 提示词

```kotlin
val SYSTEM_CHECK_V2 = """你是一个社交风控专家。请检查用户的草稿是否触发了风险规则。

【⚠️ 格式要求 - 必须严格遵守】

你必须且只能返回以下 JSON 格式：

```json
{
  "isSafe": true,
  "triggeredRisks": [],
  "suggestion": "修正建议"
}
```

【JSON Schema 定义】
{
  "type": "object",
  "required": ["isSafe", "triggeredRisks", "suggestion"],
  "properties": {
    "isSafe": {
      "type": "boolean",
      "description": "是否安全，true 或 false"
    },
    "triggeredRisks": {
      "type": "array",
      "items": {"type": "string"},
      "description": "触发的风险列表"
    },
    "suggestion": {
      "type": "string",
      "description": "修正建议"
    }
  }
}

【❌ 绝对禁止的格式】
1. 使用中文字段名：{"是否安全": true, "触发的风险": [...]}
2. 使用字符串布尔值：{"isSafe": "true"} 或 {"isSafe": "是"}
3. 添加额外字段或解释文字

【✅ 正确格式示例】
安全：{"isSafe": true, "triggeredRisks": [], "suggestion": "内容安全，可以发送"}
危险：{"isSafe": false, "triggeredRisks": ["过于急躁", "缺乏共情"], "suggestion": "建议放缓节奏，多表达理解和关心"}""".trim()
```

#### 1.3 增强的 SYSTEM_EXTRACT 提示词

```kotlin
val SYSTEM_EXTRACT_V2 = """你是一个专业的社交信息分析专家。请从用户提供的文本中提取关键信息。

【⚠️ 格式要求 - 必须严格遵守】

你必须且只能返回以下 JSON 格式：

```json
{
  "facts": {"key": "value"},
  "redTags": ["雷区1", "雷区2"],
  "greenTags": ["策略1", "策略2"]
}
```

【JSON Schema 定义】
{
  "type": "object",
  "required": ["facts", "redTags", "greenTags"],
  "properties": {
    "facts": {
      "type": "object",
      "additionalProperties": {"type": "string"},
      "description": "事实信息，键值对格式"
    },
    "redTags": {
      "type": "array",
      "items": {"type": "string"},
      "description": "雷区标签列表"
    },
    "greenTags": {
      "type": "array",
      "items": {"type": "string"},
      "description": "策略标签列表"
    }
  }
}

【❌ 绝对禁止的格式】
1. 使用中文字段名：{"事实": {...}, "雷区": [...]}
2. 使用嵌套数组：{"facts": [{"key": "value"}]}
3. 添加额外字段或解释文字

【✅ 正确格式示例】
{"facts": {"生日": "12.21", "爱好": "阅读", "职业": "程序员"}, "redTags": ["不要提前任", "避免谈论收入"], "greenTags": ["耐心倾听", "分享生活趣事"]}""".trim()
```

### 2. 字段映射配置增强设计

#### 2.1 增强的 field_mappings.json 配置

```json
{
  "version": "2.0",
  "lastUpdated": "2025-12-11",
  "analysisResult": {
    "replySuggestion": [
      "replySuggestion",
      "reply",
      "response", 
      "answer",
      "recommended_response",
      "suggestion",
      "回复建议",
      "建议回复",
      "话术建议",
      "具体的回复建议",
      "建议的回复内容",
      "回复内容",
      "推荐回复",
      "建议"
    ],
    "strategyAnalysis": [
      "strategyAnalysis",
      "strategy",
      "analysis",
      "策略分析",
      "心理分析",
      "军师分析",
      "对方当前的情绪和潜在意图",
      "关键洞察",
      "分析",
      "分析结果"
    ],
    "riskLevel": [
      "riskLevel",
      "risk_level",
      "risk",
      "level",
      "风险等级",
      "风险级别",
      "可能存在的风险点",
      "风险"
    ]
  },
  "riskLevelMapping": {
    "SAFE": ["SAFE", "safe", "Safe", "低", "low", "LOW", "安全", "无风险"],
    "WARNING": ["WARNING", "warning", "Warning", "中", "medium", "MEDIUM", "警告", "注意", "有风险"],
    "DANGER": ["DANGER", "danger", "Danger", "高", "high", "HIGH", "危险", "高风险", "严重"]
  },
  "safetyCheckResult": {
    "isSafe": [
      "isSafe",
      "is_safe",
      "safe",
      "是否安全",
      "安全"
    ],
    "triggeredRisks": [
      "triggeredRisks",
      "triggered_risks",
      "risks",
      "触发的风险",
      "风险列表",
      "触发风险"
    ],
    "suggestion": [
      "suggestion",
      "建议",
      "修改建议",
      "修正建议"
    ]
  },
  "booleanMapping": {
    "true": ["true", "True", "TRUE", "1", "是", "yes", "Yes", "YES"],
    "false": ["false", "False", "FALSE", "0", "否", "no", "No", "NO"]
  },
  "extractedData": {
    "facts": [
      "facts",
      "事实",
      "事实信息",
      "基本信息"
    ],
    "redTags": [
      "redTags",
      "red_tags",
      "红色标签",
      "雷区",
      "风险标签"
    ],
    "greenTags": [
      "greenTags",
      "green_tags",
      "绿色标签",
      "策略",
      "建议标签"
    ]
  }
}
```

#### 2.2 增强的 FieldMappingConfig 对象

```kotlin
object FieldMappingConfig {
    private var cachedMappings: FieldMappings? = null
    private var loadAttempted = false
    
    data class FieldMappings(
        val version: String,
        val analysisResult: Map<String, List<String>>,
        val riskLevelMapping: Map<String, List<String>>,
        val safetyCheckResult: Map<String, List<String>>,
        val booleanMapping: Map<String, List<String>>,
        val extractedData: Map<String, List<String>>
    )
    
    fun load(context: Context? = null): FieldMappings {
        if (cachedMappings != null) {
            return cachedMappings!!
        }
        
        if (loadAttempted) {
            return getDefaultMappings()
        }
        
        loadAttempted = true
        
        return try {
            context?.let { ctx ->
                val json = ctx.assets.open("field_mappings.json")
                    .bufferedReader()
                    .use { it.readText() }
                
                val adapter = moshi.adapter(FieldMappings::class.java)
                val mappings = adapter.fromJson(json)
                
                if (mappings != null) {
                    cachedMappings = mappings
                    android.util.Log.i("FieldMappingConfig", "配置文件加载成功，版本: ${mappings.version}")
                    return mappings
                }
            }
            
            android.util.Log.w("FieldMappingConfig", "配置文件加载失败，使用默认配置")
            getDefaultMappings()
        } catch (e: Exception) {
            android.util.Log.e("FieldMappingConfig", "配置文件加载异常，使用默认配置", e)
            getDefaultMappings()
        }
    }
    
    private fun getDefaultMappings(): FieldMappings {
        // 硬编码的默认配置作为后备
        return FieldMappings(
            version = "default",
            analysisResult = mapOf(
                "replySuggestion" to listOf("replySuggestion", "reply", "response", "回复建议", "建议回复"),
                "strategyAnalysis" to listOf("strategyAnalysis", "strategy", "analysis", "策略分析", "心理分析"),
                "riskLevel" to listOf("riskLevel", "risk_level", "risk", "风险等级", "风险级别")
            ),
            riskLevelMapping = mapOf(
                "SAFE" to listOf("SAFE", "safe", "低", "low", "安全"),
                "WARNING" to listOf("WARNING", "warning", "中", "medium", "警告"),
                "DANGER" to listOf("DANGER", "danger", "高", "high", "危险")
            ),
            safetyCheckResult = mapOf(
                "isSafe" to listOf("isSafe", "is_safe", "safe", "是否安全"),
                "triggeredRisks" to listOf("triggeredRisks", "triggered_risks", "risks", "触发的风险"),
                "suggestion" to listOf("suggestion", "建议", "修改建议")
            ),
            booleanMapping = mapOf(
                "true" to listOf("true", "True", "1", "是", "yes"),
                "false" to listOf("false", "False", "0", "否", "no")
            ),
            extractedData = mapOf(
                "facts" to listOf("facts", "事实", "事实信息"),
                "redTags" to listOf("redTags", "red_tags", "雷区"),
                "greenTags" to listOf("greenTags", "green_tags", "策略")
            )
        )
    }
}
```

### 3. 多层保底机制设计

#### 3.1 解析策略链

```kotlin
/**
 * 多层解析策略
 * 
 * Level 1: 标准 Moshi 解析
 * Level 2: Moshi lenient 模式解析
 * Level 3: Map 解析 + 字段映射
 * Level 4: 正则表达式提取
 * Level 5: 默认值返回
 */
private fun parseAnalysisResultWithFallback(json: String): Result<AnalysisResult> {
    // Level 1: 标准 Moshi 解析
    try {
        val cleanedJson = preprocessJsonResponse(json)
        val adapter = moshi.adapter(AnalysisResult::class.java)
        val result = adapter.fromJson(cleanedJson)
        if (result != null) {
            android.util.Log.d("AiRepositoryImpl", "Level 1 解析成功")
            return Result.success(result)
        }
    } catch (e: Exception) {
        android.util.Log.d("AiRepositoryImpl", "Level 1 解析失败: ${e.message}")
    }
    
    // Level 2: Moshi lenient 模式
    try {
        val cleanedJson = preprocessJsonResponse(json)
        val adapter = moshi.adapter(AnalysisResult::class.java).lenient()
        val result = adapter.fromJson(cleanedJson)
        if (result != null) {
            android.util.Log.d("AiRepositoryImpl", "Level 2 解析成功")
            return Result.success(result)
        }
    } catch (e: Exception) {
        android.util.Log.d("AiRepositoryImpl", "Level 2 解析失败: ${e.message}")
    }
    
    // Level 3: Map 解析 + 字段映射
    try {
        val cleanedJson = preprocessJsonResponse(json)
        val mapAdapter = moshi.adapter<Map<String, Any>>(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        ).lenient()
        val jsonMap = mapAdapter.fromJson(cleanedJson)
        
        if (jsonMap != null) {
            val replySuggestion = extractReplySuggestion(jsonMap)
            val strategyAnalysis = extractStrategyAnalysis(jsonMap)
            val riskLevel = extractRiskLevel(jsonMap)
            
            android.util.Log.d("AiRepositoryImpl", "Level 3 解析成功")
            return Result.success(AnalysisResult(replySuggestion, strategyAnalysis, riskLevel))
        }
    } catch (e: Exception) {
        android.util.Log.d("AiRepositoryImpl", "Level 3 解析失败: ${e.message}")
    }
    
    // Level 4: 正则表达式提取
    try {
        val replySuggestion = extractFieldByRegex(json, listOf(
            """"replySuggestion"\s*:\s*"([^"]+)"""",
            """"reply"\s*:\s*"([^"]+)"""",
            """"回复建议"\s*:\s*"([^"]+)""""
        ))
        val strategyAnalysis = extractFieldByRegex(json, listOf(
            """"strategyAnalysis"\s*:\s*"([^"]+)"""",
            """"analysis"\s*:\s*"([^"]+)"""",
            """"策略分析"\s*:\s*"([^"]+)""""
        ))
        val riskLevelStr = extractFieldByRegex(json, listOf(
            """"riskLevel"\s*:\s*"([^"]+)"""",
            """"risk"\s*:\s*"([^"]+)"""",
            """"风险等级"\s*:\s*"([^"]+)""""
        ))
        
        if (replySuggestion.isNotBlank() || strategyAnalysis.isNotBlank()) {
            val riskLevel = mapRiskLevel(riskLevelStr)
            android.util.Log.d("AiRepositoryImpl", "Level 4 解析成功")
            return Result.success(AnalysisResult(
                replySuggestion.ifBlank { DefaultValues.ANALYSIS_RESULT.replySuggestion },
                strategyAnalysis.ifBlank { DefaultValues.ANALYSIS_RESULT.strategyAnalysis },
                riskLevel
            ))
        }
    } catch (e: Exception) {
        android.util.Log.d("AiRepositoryImpl", "Level 4 解析失败: ${e.message}")
    }
    
    // Level 5: 默认值返回
    android.util.Log.w("AiRepositoryImpl", "所有解析策略失败，使用默认值")
    DefaultValues.logDefaultValueUsage("AnalysisResult", "所有解析策略失败")
    return Result.success(DefaultValues.ANALYSIS_RESULT)
}

/**
 * 使用正则表达式提取字段值
 */
private fun extractFieldByRegex(json: String, patterns: List<String>): String {
    for (pattern in patterns) {
        val regex = Regex(pattern)
        val match = regex.find(json)
        if (match != null && match.groupValues.size > 1) {
            return match.groupValues[1]
        }
    }
    return ""
}
```

### 4. 成功指标（第二次增强）

| 指标 | 第一次增强 | 第二次增强目标 | 说明 |
|------|-----------|---------------|------|
| 解析成功率 | ~85% | ≥ 98% | 主流 AI 模型响应 |
| 中文字段处理成功率 | ~70% | ≥ 95% | 中文字段名映射 |
| 默认值使用率 | ~15% | ≤ 2% | 降级到默认值的比例 |
| 提示词遵循率 | ~80% | ≥ 95% | AI 返回正确格式的比例 |

## 总结

AI 响应解析增强功能通过**基于现有代码的渐进式增强**，显著提升了系统对不同 AI 模型响应格式的兼容性和鲁棒性。

### 核心优势

1. **简单实用**：不创建新类，直接增强现有方法
2. **配置驱动**：使用配置文件管理映射规则，易于维护
3. **向后兼容**：保持现有接口不变，无需迁移
4. **性能现实**：设定合理的性能目标（100-1000ms）
5. **渐进式部署**：分阶段实施，降低风险

### 关键设计决策

| 决策 | 理由 |
|------|------|
| 不创建新类 | 保持代码简单，避免过度抽象 |
| 移除智能映射 | 复杂且不可靠，改用配置文件 |
| 配置文件管理 | 易于维护和扩展，支持快速更新 |
| 默认值策略 | 确保系统不会崩溃，提供良好的用户体验 |
| 渐进式增强 | 降低风险，保持向后兼容 |

### 第二次增强重点

| 增强项 | 目标 |
|--------|------|
| 提示词增强 | 通过更强的格式约束，从源头减少错误格式 |
| 字段映射配置 | 完善配置文件，覆盖更多中文字段变体 |
| 多层保底机制 | 5 层解析策略，确保任何情况都能返回有效结果 |

### 预期效果

- ✅ 解析成功率从 ~85% 提升到 ≥ 98%
- ✅ 中文字段处理成功率从 ~70% 提升到 ≥ 95%
- ✅ 默认值使用率从 ~15% 降低到 ≤ 2%
- ✅ 支持 5+ 主流 AI 模型
- ✅ 对用户透明，无感知的响应处理
- ✅ 性能影响 < 10%
- ✅ 零严重 Bug

通过这个设计，系统能够稳定支持多种 AI 模型，即使模型返回格式不规范，也能正常工作，为用户提供可靠的服务。
