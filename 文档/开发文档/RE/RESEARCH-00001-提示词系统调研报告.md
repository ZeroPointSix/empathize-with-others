# RESEARCH-00001 提示词系统调研报告

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档编号 | RESEARCH-00001 |
| 创建日期 | 2025-12-16 |
| 状态 | 调研完成 |
| 目的 | 调研当前提示词实现，为提示词管理系统设计提供依据 |

---

## 1. 调研范围

本次调研覆盖以下文件：
- `app/src/main/java/com/empathy/ai/domain/util/PromptTemplates.kt`
- `app/src/main/java/com/empathy/ai/data/repository/AiRepositoryImpl.kt`
- `app/src/main/java/com/empathy/ai/domain/usecase/AnalyzeChatUseCase.kt`
- `app/src/main/java/com/empathy/ai/domain/util/ContextBuilder.kt`
- `app/src/main/java/com/empathy/ai/domain/util/AiSummaryProcessor.kt`

---

## 2. 当前提示词分类

### 2.1 按功能场景分类

| 场景 | 位置 | 系统指令 | JSON Schema | Function Calling |
|------|------|----------|-------------|------------------|
| 聊天分析 | AiRepositoryImpl | SYSTEM_ANALYZE | ✅ 内嵌 | TOOL_ANALYZE_CHAT |
| 安全检查 | AiRepositoryImpl | SYSTEM_CHECK | ✅ 内嵌 | TOOL_CHECK_SAFETY |
| 信息提取 | AiRepositoryImpl | SYSTEM_EXTRACT | ✅ 内嵌 | TOOL_EXTRACT_INFO |
| 每日总结 | PromptTemplates | SUMMARY_SYSTEM_INSTRUCTION | SUMMARY_JSON_SCHEMA | ❌ |

### 2.2 按提示词类型分类

#### A. 系统指令（System Instruction）
定义AI的角色、行为规范和输出约束。

| 名称 | 位置 | 长度 | 用途 |
|------|------|------|------|
| SYSTEM_ANALYZE | AiRepositoryImpl:124 | ~1500字符 | 聊天分析角色定义 |
| SYSTEM_CHECK | AiRepositoryImpl:152 | ~800字符 | 安全检查角色定义 |
| SYSTEM_EXTRACT | AiRepositoryImpl:176 | ~800字符 | 信息提取角色定义 |
| SYSTEM_ANALYZE_FC | AiRepositoryImpl:280 | ~300字符 | Function Calling简化版 |
| SYSTEM_CHECK_FC | AiRepositoryImpl:290 | ~150字符 | Function Calling简化版 |
| SYSTEM_EXTRACT_FC | AiRepositoryImpl:294 | ~150字符 | Function Calling简化版 |
| SUMMARY_SYSTEM_INSTRUCTION | PromptTemplates:27 | ~400字符 | 每日总结角色定义 |
| buildSystemInstruction() | AnalyzeChatUseCase:196 | ~300字符 | 业务层系统指令 |

#### B. JSON Schema
定义AI输出的结构化格式。

| 名称 | 字段 | 用途 |
|------|------|------|
| 分析结果Schema | replySuggestion, strategyAnalysis, riskLevel | 聊天分析输出 |
| 安全检查Schema | isSafe, triggeredRisks, suggestion | 安全检查输出 |
| 信息提取Schema | facts, redTags, greenTags | 信息提取输出 |
| SUMMARY_JSON_SCHEMA | newFacts, updatedFacts, deletedFactKeys, newTags, relationshipScoreChange, keyEvents, summary | 每日总结输出 |

#### C. Function Calling 工具定义
用于结构化输出的工具定义。

| 名称 | 函数名 | 参数 |
|------|--------|------|
| TOOL_ANALYZE_CHAT | generate_analysis_result | replySuggestion, strategyAnalysis, riskLevel |
| TOOL_CHECK_SAFETY | generate_safety_result | isSafe, triggeredRisks, suggestion |
| TOOL_EXTRACT_INFO | generate_extracted_data | facts, redTags, greenTags |

#### D. Prompt模板
动态构建用户提示词的模板。

| 名称 | 位置 | 参数 |
|------|------|------|
| buildSummaryPrompt() | PromptTemplates:52 | contactName, targetGoal, relationshipScore, factsSection, conversationsSection |
| buildAnalysisContext() | ContextBuilder:101 | profile, brainTags, conversationHistory |
| buildPrompt() | AnalyzeChatUseCase:196 | targetGoal, facts, redTags, greenTags, conversationHistory |

---

## 3. 提示词内容详情

### 3.1 聊天分析系统指令 (SYSTEM_ANALYZE)

```
你是一个专业的社交沟通顾问。分析用户的聊天内容并给出建议。

【⚠️ 极其重要：你必须严格按照以下格式返回 JSON，不允许任何变化】

你的回复必须是且只能是以下格式的 JSON（不要添加任何其他字段）：
{"replySuggestion":"你建议的回复内容","strategyAnalysis":"你对对方情绪和意图的分析","riskLevel":"SAFE"}

【字段说明】
- replySuggestion: 建议用户如何回复对方（字符串）
- strategyAnalysis: 分析对方的情绪、意图和沟通策略（字符串）
- riskLevel: 风险等级，只能是 SAFE、WARNING 或 DANGER 三个值之一

【禁止事项】
❌ 禁止使用 analysis、risk_assessment、response_suggestions 等其他字段名
❌ 禁止使用嵌套结构
❌ 禁止添加任何额外字段
❌ 禁止返回 Markdown 格式
❌ 禁止返回多行 JSON
❌ 禁止在 JSON 前后添加任何文字

【正确示例】
{"replySuggestion":"谢谢你的关心，我也很高兴认识你","strategyAnalysis":"对方表达了好感，情绪积极，建议真诚回应","riskLevel":"SAFE"}

【错误示例 - 绝对不要这样返回】
{"analysis":{"emotional_state":"..."},"risk_assessment":{...}} ← 错误！字段名不对
{"replySuggestion":"...","strategyAnalysis":"...","riskLevel":"SAFE","extra":"..."} ← 错误！多了字段

【通用 JSON 格式规则】
- 不要返回任何 Markdown 格式（如 ```json、**、等）
- 不要返回任何中文注释或说明
- 不要返回任何换行符或格式化
- 只返回一行有效的 JSON 对象
- 不要在 JSON 前后添加任何文本
- 不要返回多个 JSON 对象
- 必须是有效的 JSON，可以被 JSON 解析器直接解析
```

### 3.2 安全检查系统指令 (SYSTEM_CHECK)

```
你是一个社交风控专家。检查用户的草稿是否触发了风险规则。

【⚠️ 极其重要：你必须严格按照以下格式返回 JSON，不允许任何变化】

你的回复必须是且只能是以下格式的 JSON（不要添加任何其他字段）：
{"isSafe":true,"triggeredRisks":[],"suggestion":"内容安全，可以发送"}

【字段说明】
- isSafe: 是否安全（布尔值：true 或 false）
- triggeredRisks: 触发的风险规则列表（数组，如果安全则为空数组 []）
- suggestion: 修正建议（字符串）

【禁止事项】
❌ 禁止使用其他字段名
❌ 禁止使用嵌套结构
❌ 禁止添加任何额外字段
❌ 禁止返回 Markdown 格式
❌ 禁止返回多行 JSON
```

### 3.3 信息提取系统指令 (SYSTEM_EXTRACT)

```
你是一个专业的社交信息分析专家。从文本中提取关键信息。

【⚠️ 极其重要：你必须严格按照以下格式返回 JSON，不允许任何变化】

你的回复必须是且只能是以下格式的 JSON（不要添加任何其他字段）：
{"facts":{"birthday":"12月21日","hobby":"阅读"},"redTags":["不要提前任"],"greenTags":["耐心倾听"]}

【字段说明】
- facts: 提取的事实信息（对象，Key 用英文如 birthday/hobby/job，Value 是中文内容）
- redTags: 雷区标签，需要避免的话题（数组）
- greenTags: 策略标签，推荐的沟通方式（数组）
```

### 3.4 每日总结系统指令 (SUMMARY_SYSTEM_INSTRUCTION)

```
你是一个专业的社交关系分析助手。

请分析提供的对话记录，提取关键信息并生成总结。

要求：
1. 识别对话中透露的新事实（如爱好、工作、家庭等）
2. 识别需要注意的雷区（敏感话题、禁忌等）
3. 识别有效的沟通策略
4. 评估关系变化趋势
5. 提取关键事件

必须以JSON格式返回结果，严格遵循指定的Schema。
关系分数变化范围：-10到+10
重要性等级：1-5（5最重要）
```

---

## 4. 数据流分析

### 4.1 聊天分析流程

```
用户点击"帮我分析"
    ↓
AnalyzeChatUseCase.invoke()
    ↓
buildPrompt() → 构建用户提示词（包含目标、Facts、标签、对话历史）
    ↓
buildSystemInstruction() → 构建系统指令（业务层）
    ↓
AiRepository.analyzeChat()
    ↓
选择策略: Function Calling / Response Format / Prompt Only
    ↓
使用 SYSTEM_ANALYZE 或 SYSTEM_ANALYZE_FC
    ↓
API调用 → 解析响应 → AnalysisResult
```

### 4.2 每日总结流程

```
定时任务触发
    ↓
AiSummaryProcessor.process()
    ↓
ContextBuilder.buildSummaryPrompt() → 使用 PromptTemplates.buildSummaryPrompt()
    ↓
AiRepository.generateText()
    ↓
使用 SUMMARY_SYSTEM_INSTRUCTION
    ↓
API调用 → 解析响应 → AiSummaryResponse
```

---

## 5. 当前实现的问题

### 5.1 硬编码问题

| 问题 | 影响 | 严重程度 |
|------|------|----------|
| 提示词硬编码在代码中 | 修改需要重新编译发布 | 高 |
| 无法动态调整 | 无法根据用户反馈快速优化 | 高 |
| 缺乏版本管理 | 无法追踪提示词变更历史 | 中 |
| 无法A/B测试 | 无法测试不同提示词效果 | 中 |

### 5.2 架构问题

| 问题 | 影响 | 严重程度 |
|------|------|----------|
| 提示词分散在多个文件 | 维护困难，容易遗漏 | 中 |
| 系统指令和业务指令混合 | 职责不清晰 | 中 |
| 缺乏统一的提示词接口 | 扩展困难 | 中 |

### 5.3 用户体验问题

| 问题 | 影响 | 严重程度 |
|------|------|----------|
| 用户无法自定义提示词 | 无法满足个性化需求 | 高 |
| 无法添加额外指令 | 灵活性不足 | 中 |

---

## 6. 提示词分类建议

### 6.1 系统提示词（不可修改）

这些提示词是系统核心功能的基础，必须保持稳定：

| 类型 | 内容 | 原因 |
|------|------|------|
| JSON格式约束 | 输出格式要求、字段定义 | 确保解析器能正确解析 |
| Function Calling定义 | 工具定义、参数Schema | 确保结构化输出 |
| 安全约束 | 禁止事项、格式规则 | 确保输出可控 |

### 6.2 用户提示词（可自定义）

这些提示词可以让用户根据自己的需求调整：

| 类型 | 内容 | 示例 |
|------|------|------|
| 角色定义 | AI的人设和风格 | "你是一个温柔体贴的恋爱顾问" |
| 分析偏好 | 分析的侧重点 | "重点关注对方的情绪变化" |
| 回复风格 | 建议回复的语气 | "回复要幽默风趣，不要太正式" |
| 额外指令 | 用户自定义的补充 | "如果对方提到加班，要表示关心" |

---

## 7. 关键约束（必须保留）

### 7.1 输出格式约束

以下约束是系统正常运行的基础，**绝对不能被用户修改**：

```
1. 必须返回有效的JSON对象
2. 必须包含指定的字段（replySuggestion, strategyAnalysis, riskLevel等）
3. riskLevel只能是SAFE、WARNING、DANGER三个值之一
4. 不能返回Markdown格式
5. 不能返回多行JSON
```

### 7.2 Function Calling约束

```
1. 函数名必须与代码中定义的一致
2. 参数Schema必须与代码中定义的一致
3. required字段必须保持不变
```

---

## 8. 下一步建议

### 8.1 需要与用户讨论的问题

1. **提示词存储方式**
   - 选项A: SharedPreferences（简单，适合少量配置）
   - 选项B: Room数据库（结构化，支持版本管理）
   - 选项C: 本地JSON文件（易于导入导出）

2. **用户自定义范围**
   - 是否允许用户修改AI角色定义？
   - 是否允许用户添加额外的分析指令？
   - 是否允许用户调整回复风格？

3. **默认提示词恢复**
   - 用户修改后是否可以一键恢复默认？
   - 是否需要保留用户的历史修改记录？

4. **提示词模板**
   - 是否提供多套预设模板供用户选择？
   - 模板示例：温柔型、理性型、幽默型等

### 8.2 技术实现建议

1. 创建 `PromptRepository` 接口统一管理提示词
2. 创建 `PromptEntity` 数据库实体存储提示词
3. 在设置界面添加"提示词管理"入口
4. 实现提示词的CRUD操作
5. 实现系统提示词和用户提示词的合并逻辑

---

## 9. 附录

### 9.1 相关文件清单

| 文件 | 行数 | 提示词数量 |
|------|------|------------|
| PromptTemplates.kt | 85 | 2 |
| AiRepositoryImpl.kt | 1246 | 6 |
| AnalyzeChatUseCase.kt | 220 | 2 |
| ContextBuilder.kt | 200 | 1 |
| AiSummaryProcessor.kt | 220 | 0（引用PromptTemplates） |

### 9.2 提示词字符统计

| 提示词 | 字符数 | 备注 |
|--------|--------|------|
| SYSTEM_ANALYZE | ~1500 | 包含JSON约束 |
| SYSTEM_CHECK | ~800 | 包含JSON约束 |
| SYSTEM_EXTRACT | ~800 | 包含JSON约束 |
| SUMMARY_SYSTEM_INSTRUCTION | ~400 | 相对简洁 |
| COMMON_JSON_RULES | ~300 | 通用规则 |
