# SUMMARY-00001-系统提示词优化总结

## 文档元信息

| 项目 | 内容 |
|------|------|
| 文档编号 | SUMMARY-00001 |
| 文档名称 | 系统提示词优化总结 |
| 创建日期 | 2026-01-06 |
| 版本 | v1.0 |
| 作者 | AI助手 |
| 文档类型 | 总结文档 (SUMMARY) |
| 相关需求 | 提示词管理系统 (PRD-00005) |

---

## 目录

- [一、系统提示词的当前实现方式](#一系统提示词的当前实现方式)
  - [1.1 三层分离架构](#11-三层分离架构)
  - [1.2 场景化设计](#12-场景化设计)
  - [1.3 变量系统](#13-变量系统)
  - [1.4 配置管理](#14-配置管理)
  - [1.5 安全机制](#15-安全机制)
  - [1.6 UseCase集成](#16-usecase集成)
- [二、提示词优化的关键点](#二提示词优化的关键点)
  - [2.1 已实施的优化](#21-已实施的优化)
  - [2.2 当前提示词结构示例](#22-当前提示词结构示例)
- [三、建议的优化方向](#三建议的优化方向)
  - [3.1 提示词内容优化](#31-提示词内容优化)
  - [3.2 架构优化](#32-架构优化)
  - [3.3 性能优化](#33-性能优化)
  - [3.4 安全性增强](#34-安全性增强)
  - [3.5 用户体验优化](#35-用户体验优化)
  - [3.6 监控与分析](#36-监控与分析)
- [四、总结](#四总结)
  - [4.1 当前优势](#41-当前优势)
  - [4.2 改进空间](#42-改进空间)
  - [4.3 优先级建议](#43-优先级建议)

---

## 一、系统提示词的当前实现方式

### 1.1 三层分离架构

项目采用了清晰的三层提示词架构，实现了系统约束与用户自定义的分离：

```
系统提示词构建
├── System Header (角色定义与任务说明)
├── User Instruction (用户自定义指令)
│   ├── 全局提示词
│   └── 联系人专属提示词 (优先级更高)
├── Runtime Data (运行时上下文数据)
│   ├── 联系人信息
│   ├── 对话历史
│   └── 事实数据
└── System Footer (输出格式约束)
```

**核心文件**：
- [`PromptBuilder.kt`](../../domain/src/main/kotlin/com/empathy/ai/domain/util/PromptBuilder.kt) - 提示词构建器
- [`SystemPrompts.kt`](../../domain/src/main/kotlin/com/empathy/ai/domain/util/SystemPrompts.kt) - 系统约束（不可修改）
- [`DefaultPrompts.kt`](../../domain/src/main/kotlin/com/empathy/ai/domain/data/local/DefaultPrompts.kt) - 默认用户提示词

### 1.2 场景化设计

项目使用 `PromptScene` 枚举定义了6个场景，已优化为4个核心场景：

| 场景 | 状态 | 说明 | 可用变量 |
|------|------|------|----------|
| ANALYZE | ✅ 保留 | 聊天分析 | contact_name, relationship_status, risk_tags, strategy_tags, facts_count |
| CHECK | ❌ 废弃 | 安全检查（合并到POLISH） | contact_name, risk_tags |
| EXTRACT | ❌ 废弃 | 信息提取（隐藏不展示） | contact_name |
| SUMMARY | ✅ 保留 | 每日总结 | contact_name, relationship_status, facts_count, today_date |
| POLISH | ✅ 保留 | 润色优化（含风险检查） | contact_name, relationship_status, risk_tags |
| REPLY | ✅ 保留 | 生成回复 | contact_name, relationship_status, risk_tags, strategy_tags |

### 1.3 变量系统

**变量上下文** - `PromptContext`：
- 支持的变量：`{{contact_name}}`, `{{relationship_status}}`, `{{risk_tags}}`, `{{strategy_tags}}`, `{{facts_count}}`, `{{today_date}}`
- 从 `ContactProfile` 自动构建，提取雷区和策略标签

**变量解析器** - `PromptVariableResolver`：
- 使用预编译正则表达式：`Regex("\\{\\{(\\w+)\\}\\}", RegexOption.IGNORE_CASE)`
- 移除了结果缓存（CR-00012优化），仅缓存模板变量提取结果
- 性能优化：正则替换在现代Android设备上性能消耗极低（微秒级）

### 1.4 配置管理

**全局配置** - `GlobalPromptConfig`：
- 版本管理：当前版本 v3
- JSON文件存储：`/data/data/com.empathy.ai/files/prompts/global_prompts.json`
- 每个场景配置：`ScenePromptConfig`（userPrompt, enabled, history）
- 历史记录：最多保留3条修改记录

**提示词优先级**：
1. 系统Footer（输出格式约束，最高优先级）
2. 系统Header（角色定义）
3. 联系人专属提示词（优先级高于全局）
4. 全局用户提示词
5. 运行时数据

### 1.5 安全机制

**提示词注入检测** - `PromptSanitizer`：
- 中英文双语检测
- 检测模式：指令覆盖类（ignore、disregard、forget）、角色扮演类（you are now、act as）、限制绕过类（jailbreak）
- 返回警告而非直接拒绝，让用户决定是否继续

**输入验证** - `PromptValidator`：
- 最大长度：1000字符
- 警告阈值：800字符（80%）
- 验证结果：Success / Warning / Error 三种状态
- 变量有效性检查

**备份机制**：
- 配置文件写入前自动备份
- 保留最近3个备份文件
- 配置损坏时自动从备份恢复

### 1.6 UseCase集成

所有AI相关的UseCase都已集成提示词系统：
- `AnalyzeChatUseCase` - 使用 `buildWithTopic()` 方法
- `PolishDraftUseCase` - 使用 `buildWithTopic()` 方法
- `GenerateReplyUseCase` - 使用 `buildWithTopic()` 方法
- `ManualSummaryUseCase` - 使用 `buildSimpleInstruction()` 方法
- `CheckDraftUseCase` - 使用 `buildSimpleInstruction()` 方法

---

## 二、提示词优化的关键点

### 2.1 已实施的优化

1. **场景简化**（FD-00015）：
   - 从6个场景简化为4个核心场景
   - CHECK合并到POLISH，EXTRACT隐藏不展示
   - 减少用户认知负担

2. **性能优化**（CR-00012）：
   - 移除变量解析结果缓存（context数据变化频繁，缓存命中率低）
   - 保留模板变量提取缓存
   - 正则表达式预编译

3. **安全增强**：
   - Prompt Injection检测
   - 备份机制
   - 数据迁移平滑

4. **用户体验优化**：
   - 历史记录功能（最多3条）
   - 联系人专属提示词
   - 字符计数和长度警告

### 2.2 当前提示词结构示例

**ANALYZE场景的完整提示词**：

```
你是一个专业的社交沟通分析助手。
你的任务是分析用户提供的聊天记录，理解对话双方的情绪和意图，
并给出具体、可操作的沟通建议。

【联系人信息】
姓名：{{contact_name}}
关系状态：{{relationship_status}}
雷区标签：{{risk_tags}}
策略标签：{{strategy_tags}}
已记录事实数：{{facts_count}}

【用户自定义指令】
{{用户自定义提示词内容}}

【针对此联系人的特殊指令】
{{联系人专属提示词内容}}

【上下文数据】
{{CONTEXT_DATA_PLACEHOLDER}}

【输出格式要求】
请严格按照以下JSON格式输出，不要添加任何额外内容：
{
  "riskLevel": "SAFE|WARNING|DANGER",
  "emotionAnalysis": "对方情绪分析",
  "intentAnalysis": "对方意图分析",
  "strategyAnalysis": "沟通策略建议",
  "replySuggestion": "具体回复建议文本",
  "riskPoints": ["风险点1", "风险点2"]
}

【重要约束】
- 只输出JSON，不要有任何其他文字
- 不要使用Markdown格式
- riskLevel只能是SAFE、WARNING、DANGER三个值之一
- 以下是用户配置，若与系统格式冲突，以系统格式为准
```

---

## 三、建议的优化方向

### 3.1 提示词内容优化

1. **增强角色定义的精确性**：
   - 当前系统提示词较为通用，可以根据不同关系状态（暧昧期、稳定期、冷淡期）提供更精确的角色定义
   - 建议：在SystemPrompts中根据relationship_status动态调整角色语气

2. **优化变量使用方式**：
   - 当前变量是简单字符串替换，可以增加变量格式化选项
   - 建议：支持条件变量，如 `{{#if risk_tags}}...{{/if}}`

3. **增强示例引导**：
   - 在DefaultPrompts中添加更多具体示例
   - 建议：为每个场景提供2-3个不同风格的示例模板

### 3.2 架构优化

1. **引入提示词模板引擎**：
   - 当前使用简单字符串拼接，可以升级为模板引擎
   - 建议：考虑使用轻量级模板库（如Handlebars风格的模板）

2. **支持提示词组合**：
   - 当前是单一提示词，可以支持模块化组合
   - 建议：将提示词拆分为基础模板 + 场景模块 + 风格模块

3. **提示词版本A/B测试**：
   - 支持不同版本的提示词进行效果对比
   - 建议：在GlobalPromptConfig中支持多版本配置

### 3.3 性能优化

1. **异步加载优化**：
   - 当前配置加载是同步的，可以进一步优化
   - 建议：使用延迟加载（Lazy）避免阻塞应用启动

2. **缓存策略优化**：
   - 当前只缓存模板变量提取，可以增加更多缓存层
   - 建议：缓存构建完成的完整提示词（按scene + contactId组合）

3. **内存优化**：
   - PromptContext包含List类型字段，hashCode计算开销大
   - 建议：考虑使用不可变数据结构优化

### 3.4 安全性增强

1. **增强注入检测**：
   - 当前检测模式有限，可以扩展更多攻击模式
   - 建议：添加更多注入模式检测（如Base64编码的指令）

2. **提示词审计日志**：
   - 记录用户自定义提示词的使用效果
   - 建议：添加提示词效果追踪，用于优化默认提示词

3. **敏感信息过滤**：
   - 确保变量替换不会泄露敏感信息
   - 建议：在PromptVariableResolver中添加敏感信息检测

### 3.5 用户体验优化

1. **提示词预设模板**：
   - 提供多个预设模板供用户选择
   - 建议：添加"专业风"、"幽默风"、"温柔风"等风格模板

2. **智能提示词建议**：
   - 根据用户使用习惯推荐提示词优化
   - 建议：分析AI响应质量，自动推荐优化建议

3. **提示词导入导出**：
   - 支持提示词配置的导入导出
   - 建议：支持JSON格式的配置导入导出，便于分享和备份

### 3.6 监控与分析

1. **提示词效果监控**：
   - 跟踪提示词使用情况和效果
   - 建议：添加PromptAnalytics类记录使用统计

2. **A/B测试框架**：
   - 支持不同提示词版本的对比测试
   - 建议：在PromptBuilder中支持版本切换

3. **错误追踪**：
   - 记录提示词解析和构建错误
   - 建议：增强日志记录，便于问题排查

---

## 四、总结

### 4.1 当前优势

1. **清晰的架构**：三层分离设计，职责明确
2. **场景化设计**：支持不同场景的定制化提示词
3. **变量系统**：灵活的变量替换机制
4. **安全机制**：注入检测、备份恢复
5. **历史记录**：支持撤销和版本回溯

### 4.2 改进空间

1. **提示词内容**：可以更精确、更个性化
2. **模板引擎**：当前简单拼接，可以升级为模板引擎
3. **性能优化**：缓存策略和异步加载可以进一步优化
4. **用户引导**：预设模板和智能建议可以提升用户体验
5. **监控分析**：缺少效果追踪和数据分析

### 4.3 优先级建议

**高优先级**：
1. 增强提示词内容的精确性（根据关系状态调整）
2. 添加预设模板库
3. 优化缓存策略

**中优先级**：
1. 引入模板引擎
2. 增强安全性检测
3. 添加提示词效果监控

**低优先级**：
1. A/B测试框架
2. 提示词导入导出
3. 智能推荐系统

---

## 附录

### 相关文档

- [PRD-00005-提示词管理系统需求.md](PRD-00005-提示词管理系统需求.md)
- [TDD-00005-提示词管理系统技术设计.md](TDD-00005-提示词管理系统技术设计.md)
- [TD-00005-提示词管理系统任务清单.md](TD-00005-提示词管理系统任务清单.md)
- [TD-00015-提示词设置优化任务清单.md](TD-00015-提示词设置优化任务清单.md)

### 核心代码文件

- [`PromptBuilder.kt`](../../domain/src/main/kotlin/com/empathy/ai/domain/util/PromptBuilder.kt)
- [`SystemPrompts.kt`](../../domain/src/main/kotlin/com/empathy/ai/domain/util/SystemPrompts.kt)
- [`DefaultPrompts.kt`](../../domain/src/main/kotlin/com/empathy/ai/domain/data/local/DefaultPrompts.kt)
- [`PromptVariableResolver.kt`](../../domain/src/main/kotlin/com/empathy/ai/domain/util/PromptVariableResolver.kt)
- [`PromptSanitizer.kt`](../../domain/src/main/kotlin/com/empathy/ai/domain/util/PromptSanitizer.kt)
- [`GlobalPromptConfig.kt`](../../domain/src/main/kotlin/com/empathy/ai/domain/data/config/GlobalPromptConfig.kt)

---

**文档结束**
