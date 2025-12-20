# BUG-00025: AI响应JSON解析失败问题分析

> **创建时间**: 2025-12-20  
> **发现者**: Roo  
> **严重程度**: 🔴 高  
> **状态**: ✅ 已修复

## 📋 问题概述

AI处理结果失败，出现JSON解析错误，导致手动总结功能无法正常工作。

**后续发现的关联问题**：
- 总结内容未同步到事实流和标签系统
- 对话记录软删除逻辑存在但需验证

## 🐛 错误现象

### 错误日志
```
2025-12-20 12:59:36.824  3821-3853  ManualSummaryUseCase    com.empathy.ai                       E  解析AI响应失败 (Ask Gemini)
                                                                                                   com.squareup.moshi.JsonDataException: Required value 'action' missing at $.newTags[1]
                                                                                                   	at com.squareup.moshi.internal.Util.missingProperty(Util.java:660)
                                                                                                   	at com.empathy.ai.data.remote.model.TagUpdateDtoJsonAdapter.fromJson(TagUpdateDtoJsonAdapter.kt:52)
```

### 影响范围
- **功能影响**: 手动触发AI总结功能完全失效
- **用户体验**: 用户无法生成联系人关系总结，降级到本地统计
- **数据影响**: AI分析的丰富洞察（标签、事实、关键事件）无法获取

## 🔍 根因分析

### 问题定位
通过分析错误日志和相关代码，发现问题出现在`TagUpdateDto`类的JSON解析过程中。

### 根本原因
**提示词与 DTO 定义不一致 + 解析方法缺乏容错**

1. **提示词定义（SUMMARY_FOOTER）**：
```json
"newTags": [{"content": "标签内容", "type": "RISK_RED或STRATEGY_GREEN"}]
```
**注意：提示词中未要求 action 字段**

2. **TagUpdateDto 定义**：
```kotlin
@JsonClass(generateAdapter = true)
data class TagUpdateDto(
    @Json(name = "action")
    val action: String,  // ❌ 必需字段，但提示词未要求
    
    @Json(name = "type")
    val type: String,
    
    @Json(name = "content")
    val content: String
)
```

3. **AI 实际返回**：
```json
"newTags": [
  {"content": "情绪拉扯型沟通", "type": "RISK_RED"},
  {"content": "借家庭阻力逃避决策", "type": "RISK_RED"}
]
```
**AI 按提示词返回，缺少 action 字段**

4. **解析方法问题**：
原 `parseAiResponse()` 直接使用 Moshi 严格解析，无容错机制

### 问题链条
1. 提示词未要求 action 字段
2. AI 按提示词返回（无 action）
3. Moshi 严格校验发现必需字段缺失
4. 抛出 `JsonDataException`
5. 触发降级方案，使用本地统计

## 🛠️ 修复方案

### 采用方案：容错解析（仿照 AiRepositoryImpl.parseAnalysisResult）

**设计原则**：不修改 DTO 数据结构，在解析层增加容错逻辑

**修改文件**：`ManualSummaryUseCase.kt`

**核心改动**：

1. **重构 `parseAiResponse()` 方法**：
   - 先尝试直接解析（严格模式）
   - 失败后使用字段映射（宽松模式）

2. **新增 `extractJsonFromResponse()` 方法**：
   - 支持提取 Markdown 代码块中的 JSON
   - 支持提取带前后文字的 JSON

3. **新增 `parseAiResponseWithFallback()` 方法**：
   - 手动解析 JSON Map
   - 为缺失字段提供默认值

4. **新增 `parseTagUpdates()` 方法**（关键）：
   - 为缺失的 action 字段提供默认值 "ADD"
   - 这是解决问题的核心逻辑

**代码示例**：
```kotlin
/**
 * 解析 tags 字段（核心容错逻辑）
 *
 * 为缺失的 action 字段提供默认值 "ADD"
 */
private fun parseTagUpdates(raw: Any?): List<TagUpdateDto> {
    val list = raw as? List<Map<String, Any>> ?: return emptyList()
    return list.mapNotNull { item ->
        val content = item["content"] as? String ?: return@mapNotNull null
        val type = item["type"] as? String ?: return@mapNotNull null
        // 关键：为缺失的 action 字段提供默认值 "ADD"
        val action = (item["action"] as? String) ?: "ADD"
        TagUpdateDto(action = action, type = type, content = content)
    }
}
```

### 为什么这样修能从机制上避免问题

1. **兼容性**：不修改 DTO 结构，保持与其他模块的兼容
2. **容错性**：即使 AI 返回格式不完全符合预期，也能正确解析
3. **一致性**：与 `AiRepositoryImpl.parseAnalysisResult` 采用相同的容错模式
4. **可扩展性**：未来如果 AI 返回其他非标准格式，可以在字段映射中轻松添加支持

## 📋 修复验证清单

### 阶段一：JSON 解析修复
- [x] `parseAiResponse()` 重构为容错解析模式
- [x] 新增 `extractJsonFromResponse()` 支持多种 JSON 格式
- [x] 新增 `parseAiResponseWithFallback()` 手动字段映射
- [x] 新增 `parseTagUpdates()` 为 action 提供默认值
- [x] 新增 `parseKeyEvents()` 支持多种 importance 格式
- [x] 新增 `parseFacts()` 解析事实列表
- [x] 代码无语法错误

### 阶段二：数据同步修复
- [x] 新增 `BrainTagRepository` 依赖注入
- [x] 新增 `syncSummaryDataToContact()` 数据同步方法
- [x] 新增 `syncFactsToContact()` 事实同步到 ContactProfile
- [x] 新增 `syncTagsToContact()` 标签同步到 BrainTag 表
- [x] 更新 `SummaryModule.kt` 依赖注入配置
- [x] 更新单元测试以包含新依赖
- [x] 单元测试通过

### 阶段三：LazyColumn key 重复闪退修复
- [x] 修改 `buildTimelineItems()` 中 UserFact 的 ID 生成逻辑
- [x] 修改 `addFactToStream()` 中的 ID 生成逻辑保持一致
- [x] 在 `ContactDetailTabScreen` 添加 `RefreshData` 事件
- [x] 新增 `ContactDetailTabViewModelKeyTest.kt` 单元测试
- [x] 单元测试通过（4个测试用例全部通过）

### 待验证
- [ ] 手动总结功能恢复正常（待实际测试）
- [ ] AI 响应能正确转换为 DailySummary 对象（待实际测试）
- [ ] 新事实正确显示在事实流标签页（待实际测试）
- [ ] 新标签正确显示在标签画像标签页（待实际测试）
- [ ] 关系分数正确更新（待实际测试）
- [ ] 总结后返回联系人界面不再闪退（待实际测试）

## 📊 影响评估

### 正面影响
- ✅ 修复手动总结功能的 JSON 解析问题
- ✅ 提升 AI 响应解析的健壮性
- ✅ 与现有架构模式保持一致
- ✅ 新增数据同步逻辑，确保 AI 总结内容正确写入事实流和标签系统
- ✅ 关系分数自动更新

### 风险评估
- ⚠️ 需要实际测试验证修复效果
- ⚠️ 容错解析可能掩盖 AI 返回格式的其他问题
- ⚠️ 数据同步失败不会阻止总结保存，但会记录警告日志

## 🔧 修复内容详情

### 1. JSON 解析容错（已完成）
- 重构 `parseAiResponse()` 方法，支持严格模式和容错模式
- 为缺失的 `action` 字段提供默认值 "ADD"
- 支持 `importance` 字段的数字和字符串两种格式

### 2. 数据同步逻辑（新增）
**问题**：原 `ManualSummaryUseCase` 只保存 `DailySummary` 到数据库，缺少以下关键逻辑：
- ❌ 将 `newFacts` 同步到 `ContactProfile.facts`
- ❌ 将 `newTags` 同步到 `BrainTag` 表
- ❌ 更新 `ContactProfile.relationshipScore`

**修复**：新增 `syncSummaryDataToContact()` 方法，在保存总结后执行数据同步：
```kotlin
private suspend fun syncSummaryDataToContact(
    profile: ContactProfile,
    summary: DailySummary
) {
    // 1. 同步新事实到联系人画像
    if (summary.newFacts.isNotEmpty()) {
        syncFactsToContact(profile, summary.newFacts)
    }

    // 2. 同步新标签到BrainTag表
    if (summary.updatedTags.isNotEmpty()) {
        syncTagsToContact(profile.id, summary.updatedTags)
    }

    // 3. 更新关系分数
    if (summary.relationshipScoreChange != 0) {
        val newScore = (profile.relationshipScore + summary.relationshipScoreChange)
            .coerceIn(0, 100)
        contactRepository.updateRelationshipScore(profile.id, newScore)
    }
}
```

### 3. 依赖注入更新
- `ManualSummaryUseCase` 新增 `BrainTagRepository` 依赖
- `SummaryModule.kt` 更新 `provideManualSummaryUseCase()` 方法

### 4. LazyColumn key 重复闪退修复

**问题**：总结后返回联系人界面闪退
```
java.lang.IllegalArgumentException: Key "1766222391131" was already used
```

**根因**：`ContactDetailTabViewModel.buildTimelineItems()` 中 `UserFact` 的 ID 生成方式可能产生重复 key：
```kotlin
// 原代码（有问题）
id = "fact_${fact.timestamp}_${fact.key.hashCode()}"
```
当多个 facts 在同一毫秒创建且 key 的 hashCode 相同时，会产生重复 ID。

**修复**：使用索引确保唯一性
```kotlin
// 修复后
facts.forEachIndexed { index, fact ->
    id = "fact_${index}_${fact.timestamp}_${fact.key.hashCode().toUInt()}"
}
```

**修改文件**：
- `ContactDetailTabViewModel.kt`：修改 `buildTimelineItems()` 和 `addFactToStream()` 的 ID 生成逻辑
- `ContactDetailTabScreen.kt`：在 `navigateToTimeline` 时添加 `RefreshData` 事件
- 新增 `ContactDetailTabViewModelKeyTest.kt`：4个测试用例验证 key 唯一性

## 📚 相关文档

- [PRD-00011: 手动触发AI总结功能产品需求文档](../PRD/PRD-00011-手动触发AI总结功能产品需求文档.md)
- [TDD-00011: 手动触发AI总结功能技术设计](../TDD/TDD-00011-手动触发AI总结功能技术设计.md)

---

**最后更新**: 2025-12-20  
**修复者**: Kiro  
**修复内容**: 
1. JSON 解析容错机制
2. 数据同步逻辑（Facts → ContactProfile, Tags → BrainTag, RelationshipScore 更新）
3. LazyColumn key 重复闪退修复（使用索引确保 ID 唯一性）
**修复提交**: 待提交