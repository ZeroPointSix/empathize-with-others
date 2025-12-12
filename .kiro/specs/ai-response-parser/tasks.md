# 实施计划 - AI 响应解析增强（优化版）

## Phase 1: 基础增强（1-2天）

- [x] 1. 创建配置文件和默认值





  - 创建字段映射配置文件
  - 创建配置加载逻辑
  - 定义默认值常量
  - _需求: 2.5, 6.1, 6.2, 8.2_

- [x] 1.1 创建字段映射配置文件


  - 在 `app/src/main/assets/` 目录创建 `field_mappings.json`
  - 定义 AnalysisResult 字段映射（replySuggestion、strategyAnalysis、riskLevel）
  - 定义 SafetyCheckResult 字段映射（isSafe、triggeredRisks、suggestion）


  - 定义 ExtractedData 字段映射（facts、redTags、greenTags）
  - _需求: 2.5, 8.2_



- [x] 1.2 创建 FieldMappingConfig 对象

  - 在 AiRepositoryImpl.kt 中创建 FieldMappingConfig 单例对象
  - 实现 load() 方法从 assets 加载配置文件
  - 实现配置缓存逻辑（启动时加载一次）
  - 实现 getDefaultMappings() 方法提供硬编码的默认配置




  - 添加错误处理和日志记录
  - _需求: 2.5, 8.2_

- [x] 1.3 创建 DefaultValues 对象


  - 在 AiRepositoryImpl.kt 中创建 DefaultValues 单例对象
  - 定义 ANALYSIS_RESULT 默认值
  - 定义 SAFETY_CHECK_RESULT 默认值
  - 定义 EXTRACTED_DATA 默认值
  - _需求: 6.1, 6.2_



- [x] 2. 优化现有解析方法





  - 增强 preprocessJsonResponse 方法
  - 优化 mapChineseFieldNames 方法
  - _需求: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4_

- [x] 2.1 增强 preprocessJsonResponse 方法


  - 简化正则表达式，提高性能
  - 优化字符串操作（使用 StringBuilder）
  - 添加最终验证步骤 validateAndFixJson()
  - 优化日志输出（限制长度，条件输出）
  - 保持现有的清洗逻辑（移除 Markdown、提取 JSON、修复格式错误）
  - _需求: 1.1, 1.2, 1.3, 1.4_

- [x] 2.2 优化 mapChineseFieldNames 方法


  - 移除 intelligentFieldMapping() 方法
  - 使用 FieldMappingConfig 加载配置文件
  - 实现精确字段映射（遍历配置进行替换）
  - 简化映射逻辑，移除复杂的正则匹配
  - 添加映射统计日志
  - _需求: 2.1, 2.2, 2.3, 2.4_

## Phase 2: 解析增强（2-3天）


- [x] 3. 增强 AnalysisResult 解析





  - 优化 parseAnalysisResult 方法
  - 增强 parseFallbackAnalysisResult 方法
  - 支持嵌套结构、riskLevel 推断、数组格式处理
  - _需求: 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 3.1 优化 parseAnalysisResult 方法


  - 优化标准解析路径（直接使用 Moshi）
  - 使用 lenient() 模式提高容错性
  - 解析失败时调用 parseFallbackAnalysisResult
  - 保持现有的日志输出
  - _需求: 3.1_

- [x] 3.2 增强 parseFallbackAnalysisResult 方法


  - 添加默认值支持（解析失败返回 DefaultValues.ANALYSIS_RESULT）
  - 增强 extractReplySuggestion 方法（支持数组格式，选择 priority 为 high 的建议）
  - 增强 extractStrategyAnalysis 方法（支持嵌套结构提取和组合）
  - 增强 extractRiskLevel 方法（支持多种格式：SAFE/WARNING/DANGER、low/medium/high、智能推断）
  - 统一错误处理和日志记录
  - _需求: 3.2, 3.3, 3.4, 3.5, 6.1, 6.2_

- [x] 4. 增强 SafetyCheckResult 解析





  - 优化 parseSafetyCheckResult 方法
  - 增强 parseFallbackSafetyCheckResult 方法
  - 支持布尔类型转换、默认值填充
  - _需求: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 4.1 优化 parseSafetyCheckResult 方法


  - 优化标准解析路径
  - 使用 lenient() 模式
  - 解析失败时调用 parseFallbackSafetyCheckResult
  - _需求: 4.1_

- [x] 4.2 增强 parseFallbackSafetyCheckResult 方法（如果不存在则创建）


  - 添加默认值支持（解析失败返回 DefaultValues.SAFETY_CHECK_RESULT）
  - 实现布尔类型转换（支持 "true"/"false"、1/0、"是"/"否"）
  - 实现默认值填充（triggeredRisks 缺失时使用空列表，suggestion 缺失时使用默认文本）
  - 统一错误处理和日志记录
  - _需求: 4.2, 4.3, 4.4, 4.5, 6.1, 6.2_

- [x] 5. 增强 ExtractedData 解析






  - 优化 parseExtractedData 方法
  - 增强 parseFallbackExtractedData 方法（如果不存在则创建）
  - 支持 facts 扁平化、标签去重、默认值填充
  - _需求: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 5.1 优化 parseExtractedData 方法


  - 优化标准解析路径
  - 使用 lenient() 模式
  - 解析失败时调用 parseFallbackExtractedData
  - _需求: 5.1_

- [x] 5.2 创建 parseFallbackExtractedData 方法


  - 添加默认值支持（解析失败返回 DefaultValues.EXTRACTED_DATA）
  - 实现 facts 扁平化（将嵌套对象转换为 Map<String, String>）
  - 实现标签去重（对 redTags 和 greenTags 去重，保持原始顺序）
  - 实现默认值填充（facts/redTags/greenTags 缺失时使用空值）
  - 统一错误处理和日志记录
  - _需求: 5.2, 5.3, 5.4, 5.5, 6.1, 6.2_


## Phase 3: 测试和优化（1-2天）

- [x] 6. 编写单元测试






  - 测试配置加载
  - 测试 JSON 清洗
  - 测试字段映射
  - 测试解析方法
  - _需求: 11.1_

- [x] 6.1 测试 FieldMappingConfig


  - 测试配置文件加载成功
  - 测试配置文件加载失败时使用默认配置
  - 测试配置缓存机制
  - _需求: 2.5, 8.2_



- [x] 6.2 测试 preprocessJsonResponse
  - 测试 Markdown 标记移除（\`\`\`json、\`\`\`）
  - 测试 JSON 对象提取（包含前后缀文本）
  - 测试常见格式错误修复（多余逗号、缺失逗号、转义字符）
  - 测试边缘情况（空字符串、纯文本、超大响应）
  - _需求: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 6.3 测试 mapChineseFieldNames

  - 测试精确字段映射（配置文件中的所有映射）
  - 测试未知字段处理（不在配置中的字段）
  - 测试嵌套结构映射
  - _需求: 2.1, 2.2, 2.3, 2.4_

- [x] 6.4 测试 parseAnalysisResult 和 parseFallbackAnalysisResult

  - 测试标准格式解析
  - 测试中文字段解析
  - 测试嵌套结构提取
  - 测试 riskLevel 智能推断（SAFE/WARNING/DANGER、low/medium/high）
  - 测试 replySuggestion 数组格式处理
  - 测试默认值返回
  - _需求: 3.1, 3.2, 3.3, 3.4, 3.5, 6.1, 6.2_

- [x] 6.5 测试 parseSafetyCheckResult 和 parseFallbackSafetyCheckResult

  - 测试标准格式解析
  - 测试中文字段解析
  - 测试布尔类型转换（"true"/"false"、1/0、"是"/"否"）
  - 测试默认值填充（triggeredRisks、suggestion）
  - 测试默认值返回
  - _需求: 4.1, 4.2, 4.3, 4.4, 4.5, 6.1, 6.2_

- [x] 6.6 测试 parseExtractedData 和 parseFallbackExtractedData

  - 测试标准格式解析
  - 测试中文字段解析
  - 测试 facts 扁平化
  - 测试标签去重
  - 测试默认值填充
  - 测试默认值返回
  - _需求: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2_

- [x] 7. 编写属性测试（可选）








  - 测试核心属性
  - 测试性能属性
  - 测试边缘情况
  - _需求: 11.2_

- [x] 7.1 测试核心属性





  - Property 1: Markdown 清洗一致性
  - Property 2: JSON 提取正确性
  - Property 3: 字段映射完整性
  - Property 4: AnalysisResult 解析成功率
  - Property 5: 降级策略有效性
  - _需求: 1.1, 1.2, 2.1, 3.1, 3.2, 6.1, 6.2_

- [x] 7.2 测试性能属性



  - Property 6: 常规响应解析性能（≤ 300ms）
  - Property 7: 复杂响应解析性能（≤ 500ms）
  - Property 8: 大型响应解析性能（≤ 1000ms）
  - _需求: 7.1, 7.2, 7.3_

- [x] 7.3 测试边缘情况属性



  - Property 9: 空响应处理
  - Property 10: 字段缺失处理
  - _需求: 1.5, 4.4, 4.5, 5.4_

- [x] 8. 性能优化




  - 分析性能瓶颈
  - 优化关键路径
  - 验证性能指标
  - _需求: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 8.1 性能分析


  - 使用 Android Profiler 分析解析性能
  - 识别性能瓶颈（正则表达式、字符串操作、JSON 解析）
  - 记录基准性能数据
  - _需求: 7.1, 7.2, 7.3, 7.4_

- [x] 8.2 性能优化


  - 优化正则表达式（简化或替换为字符串操作）
  - 优化字符串操作（使用 StringBuilder）
  - 优化配置加载（启动时缓存）
  - 限制日志输出长度
  - _需求: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 8.3 性能验证


  - 测量优化后的解析时间
  - 验证性能指标（常规 ≤ 300ms、复杂 ≤ 500ms、大型 ≤ 1000ms）
  - 测量内存使用增长（≤ 10%）
  - _需求: 7.1, 7.2, 7.3, 7.4_

- [x] 9. 集成测试和回归测试



  - 测试与真实 AI 模型的兼容性
  - 运行回归测试
  - 验证向后兼容性
  - _需求: 10.1, 10.2, 10.3, 10.4, 10.5, 11.3_

- [x] 9.1 真实 AI 模型测试


  - 测试 OpenAI 模型（gpt-3.5-turbo、gpt-4）
  - 测试 DeepSeek 模型（deepseek-chat）
  - 测试 ModelScope 模型（Qwen/Qwen3-Coder-480B-A35B-Instruct）
  - 验证解析成功率 ≥ 95%
  - _需求: 11.3_

- [x] 9.2 回归测试


  - 运行现有测试用例（确保 113/114 通过）
  - 手动测试核心功能（聊天分析、安全检查、数据提取）
  - 验证现有功能不受影响
  - _需求: 10.5_

- [x] 9.3 向后兼容性验证


  - 验证 Domain Model 接口不变


  - 验证 Repository 接口不变
  - 验证现有调用方式不变
  - _需求: 10.1, 10.2, 10.3, 10.4_

- [x] 10. 修复新格式字段提取问题


  - 修复 extractReplySuggestion 方法，添加对 reply 字段的支持
  - 修复 extractStrategyAnalysis 方法，添加对 analysis 嵌套对象的支持
  - 更新字段映射配置文件，添加 reply 到 replySuggestion 的映射
  - _需求: 3.6, 3.7_



- [x] 10.1 增强 extractReplySuggestion 方法
  - 添加对 reply 字段的检查（AI 模型常用此字段名）
  - 添加对 response 字段的检查
  - 添加对 answer 字段的检查
  - 确保字段检查顺序合理（标准字段优先）


  - _需求: 3.6_

- [x] 10.2 增强 extractStrategyAnalysis 方法
  - 添加对 analysis 嵌套对象的支持


  - 从 analysis 对象中提取 emotion、intention、risk 字段
  - 将提取的信息组合为完整的策略分析文本



  - _需求: 3.7_

- [x] 10.3 更新字段映射配置文件
  - 在 field_mappings.json 中添加 reply、response、answer 到 replySuggestion 的映射
  - 添加 analysis 相关字段的映射
  - _需求: 2.5, 3.6, 3.7_

- [x] 11. Checkpoint - 确保所有测试通过
  - 确保所有测试通过，如有问题请询问用户

## Phase 4: 第二次增强（提示词增强 + 字段映射配置 + 多层保底）

- [ ] 12. 提示词增强
  - 增强 SYSTEM_ANALYZE 提示词，添加 JSON Schema 定义和更多错误示例
  - 增强 SYSTEM_CHECK 提示词，添加格式约束
  - 增强 SYSTEM_EXTRACT 提示词，添加格式约束
  - _需求: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7_

- [ ] 12.1 增强 SYSTEM_ANALYZE 提示词
  - 添加 JSON Schema 定义，明确字段类型和约束
  - 添加更多错误格式示例（中文字段名、嵌套结构、错误风险等级）
  - 添加更多正确格式示例
  - 强调禁止使用 markdown 代码块标记
  - _需求: 12.1, 12.2, 12.3, 12.4_

- [ ] 12.2 增强 SYSTEM_CHECK 提示词
  - 添加 JSON Schema 定义
  - 添加错误格式示例
  - 强调 isSafe 必须是布尔类型
  - _需求: 12.6_

- [ ] 12.3 增强 SYSTEM_EXTRACT 提示词
  - 添加 JSON Schema 定义
  - 添加错误格式示例
  - 强调 facts 必须是扁平的键值对
  - _需求: 12.7_

- [ ] 13. 字段映射配置增强
  - 更新 field_mappings.json 配置文件
  - 增强 FieldMappingConfig 对象
  - 添加 riskLevel 映射和 boolean 映射
  - _需求: 13.1, 13.2, 13.3, 13.4, 13.5, 13.6_

- [ ] 13.1 更新 field_mappings.json 配置文件
  - 添加版本号和更新时间
  - 扩展 replySuggestion 映射（添加 reply、response、answer 等）
  - 扩展 strategyAnalysis 映射（添加 strategy、analysis 等）
  - 添加 riskLevelMapping（SAFE/WARNING/DANGER 的各种变体）
  - 添加 booleanMapping（true/false 的各种变体）
  - _需求: 13.1, 13.3, 13.6_

- [ ] 13.2 增强 FieldMappingConfig 对象
  - 添加 FieldMappings 数据类，包含所有映射配置
  - 实现配置文件加载和缓存逻辑
  - 实现默认配置作为后备
  - 添加加载状态跟踪，避免重复加载
  - _需求: 13.1, 13.2_

- [ ] 13.3 增强字段映射方法
  - 使用新的配置文件进行字段映射
  - 支持递归处理嵌套 JSON 结构
  - 添加映射统计日志
  - _需求: 13.4, 13.5_

- [ ] 14. 多层保底机制
  - 实现 5 层解析策略链
  - 添加正则表达式提取作为最后手段
  - 增强默认值使用日志
  - _需求: 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 14.7_

- [ ] 14.1 实现多层解析策略
  - Level 1: 标准 Moshi 解析
  - Level 2: Moshi lenient 模式解析
  - Level 3: Map 解析 + 字段映射
  - Level 4: 正则表达式提取
  - Level 5: 默认值返回
  - _需求: 14.1, 14.2, 14.3, 14.4_

- [ ] 14.2 实现正则表达式提取方法
  - 创建 extractFieldByRegex 方法
  - 支持多个正则模式匹配
  - 支持中英文字段名提取
  - _需求: 14.3_

- [ ] 14.3 增强默认值使用日志
  - 记录使用默认值的原因
  - 记录原始响应内容（脱敏后）
  - 统计连续使用默认值的次数
  - 连续多次使用默认值时提示用户
  - _需求: 14.5, 14.6, 14.7_

- [ ]* 15. 第二次增强测试
  - 测试增强后的提示词效果
  - 测试增强后的字段映射
  - 测试多层保底机制
  - _需求: 11.1, 11.2_

- [ ]* 15.1 测试提示词增强效果
  - 使用真实 AI 模型测试新提示词
  - 验证返回格式正确率提升
  - 记录不同模型的表现差异
  - _需求: 12.1, 12.2, 12.3, 12.4_

- [ ]* 15.2 测试字段映射增强
  - 测试新增的字段映射（reply、response、answer 等）
  - 测试 riskLevel 映射（中文风险等级）
  - 测试 boolean 映射（中文布尔值）
  - _需求: 13.1, 13.6_

- [ ]* 15.3 测试多层保底机制
  - 测试各层解析策略的触发条件
  - 测试正则表达式提取的准确性
  - 测试默认值返回的正确性
  - 验证解析成功率 ≥ 98%
  - _需求: 14.1, 14.2, 14.3, 14.4, 14.5_

- [ ] 16. Checkpoint - 确保第二次增强测试通过
  - 确保所有测试通过，如有问题请询问用户

## 任务说明

### 优先级

**高优先级**（必须完成）:
- Phase 1: 创建配置文件和优化现有方法
- Phase 2: 增强解析方法
- Phase 3: 单元测试、集成测试、回归测试

**中优先级**（建议完成）:
- Phase 3: 性能优化

**低优先级**（可选）:
- Phase 3: 属性测试

### 实施顺序

1. **先配置后代码**：先创建配置文件，再修改代码
2. **先简单后复杂**：先优化 preprocessJsonResponse 和 mapChineseFieldNames，再增强解析方法
3. **先核心后边缘**：先处理 AnalysisResult，再处理 SafetyCheckResult 和 ExtractedData
4. **先功能后测试**：先实现功能，再编写测试
5. **先单元后集成**：先单元测试，再集成测试

### 注意事项

- 保持现有接口不变
- 复用现有代码，避免大规模重构
- 每个任务完成后运行相关测试
- 遇到问题及时询问用户
- 记录详细的日志便于调试
