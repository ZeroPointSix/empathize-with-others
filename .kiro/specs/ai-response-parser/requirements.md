# 需求文档 - AI 响应解析增强

## 简介

本功能旨在解决多 AI 模型响应格式不一致的问题。不同的 AI 模型对指令遵循程度不同，可能返回中文字段名、包含额外文本、或格式不规范的 JSON。本功能通过多层防御策略，确保系统能够稳定解析各种 AI 响应格式，提升多模型兼容性和系统鲁棒性。

## 术语表

- **System**: 共情 AI 助手应用
- **AI_Provider**: AI 服务提供商（如 OpenAI、DeepSeek、ModelScope 等）
- **Response_Parser**: 响应解析器，负责清洗和解析 AI 返回的内容
- **Field_Mapper**: 字段映射器，负责将中文字段名映射为英文字段名
- **JSON_Cleaner**: JSON 清洗器，负责移除非 JSON 内容
- **Fallback_Handler**: 降级处理器，负责处理解析失败的情况
- **Domain_Model**: 领域模型，如 AnalysisResult、SafetyCheckResult、ExtractedData
- **DTO**: 数据传输对象，用于网络层的数据结构

## 需求

### 需求 1: JSON 响应清洗

**用户故事**: 作为系统开发者，我希望系统能够自动清洗 AI 返回的响应内容，以便提取出纯净的 JSON 数据。

#### 验收标准

1. WHEN AI_Provider 返回包含 Markdown 代码块标记的响应 THEN THE System SHALL 移除所有代码块标记并提取 JSON 内容
2. WHEN AI_Provider 返回包含前后缀文本的响应 THEN THE System SHALL 识别并提取 JSON 对象部分
3. WHEN AI_Provider 返回包含转义字符的响应 THEN THE System SHALL 正确处理转义字符
4. WHEN AI_Provider 返回包含 Unicode 编码的响应 THEN THE System SHALL 正确解码 Unicode 字符
5. WHEN AI_Provider 返回空响应或纯文本响应 THEN THE System SHALL 返回解析错误并使用默认值

### 需求 2: 字段名映射

**用户故事**: 作为系统开发者，我希望系统能够自动将中文字段名映射为英文字段名，以便统一处理不同模型的响应。

#### 验收标准

1. WHEN AI_Provider 返回包含中文字段名的 JSON THEN THE System SHALL 使用精确键值对映射将中文字段名转换为英文字段名
2. WHEN AI_Provider 返回嵌套结构的 JSON THEN THE System SHALL 递归处理所有层级的字段映射
3. WHEN AI_Provider 返回数组类型的字段 THEN THE System SHALL 正确提取数组内容并映射字段
4. WHEN AI_Provider 返回的字段名无法识别 THEN THE System SHALL 记录警告日志并使用默认值
5. WHEN 系统需要新增字段映射规则 THEN THE System SHALL 允许通过配置文件添加映射规则

### 需求 3: AnalysisResult 解析

**用户故事**: 作为系统开发者，我希望系统能够稳定解析 AI 分析结果，以便为用户提供可靠的聊天分析功能。

#### 验收标准

1. WHEN AI_Provider 返回标准格式的 AnalysisResult JSON THEN THE System SHALL 成功解析为 Domain_Model
2. WHEN AI_Provider 返回中文字段名的 AnalysisResult JSON THEN THE System SHALL 通过字段映射成功解析
3. WHEN AI_Provider 返回嵌套结构的分析结果 THEN THE System SHALL 提取关键信息并组合为 strategyAnalysis 字段
4. WHEN AI_Provider 返回的 riskLevel 字段值不规范 THEN THE System SHALL 智能推断风险等级
5. WHEN AI_Provider 返回的 replySuggestion 为数组格式 THEN THE System SHALL 选择优先级最高的建议
6. WHEN AI_Provider 返回使用 reply 字段代替 replySuggestion THEN THE System SHALL 识别并提取 reply 字段作为回复建议
7. WHEN AI_Provider 返回使用 analysis 嵌套对象代替 strategyAnalysis THEN THE System SHALL 从 analysis 对象中提取 emotion、intention、risk 等字段并组合为策略分析

### 需求 4: SafetyCheckResult 解析

**用户故事**: 作为系统开发者，我希望系统能够稳定解析安全检查结果，以便为用户提供可靠的防踩雷功能。

#### 验收标准

1. WHEN AI_Provider 返回标准格式的 SafetyCheckResult JSON THEN THE System SHALL 成功解析为 Domain_Model
2. WHEN AI_Provider 返回中文字段名的 SafetyCheckResult JSON THEN THE System SHALL 通过字段映射成功解析
3. WHEN AI_Provider 返回的 isSafe 字段为字符串类型 THEN THE System SHALL 转换为布尔类型
4. WHEN AI_Provider 返回的 triggeredRisks 字段缺失 THEN THE System SHALL 使用空列表作为默认值
5. WHEN AI_Provider 返回的 suggestion 字段缺失 THEN THE System SHALL 使用默认提示文本

### 需求 5: ExtractedData 解析

**用户故事**: 作为系统开发者，我希望系统能够稳定解析数据提取结果，以便为用户提供可靠的信息萃取功能。

#### 验收标准

1. WHEN AI_Provider 返回标准格式的 ExtractedData JSON THEN THE System SHALL 成功解析为 Domain_Model
2. WHEN AI_Provider 返回中文字段名的 ExtractedData JSON THEN THE System SHALL 通过字段映射成功解析
3. WHEN AI_Provider 返回的 facts 字段为嵌套对象 THEN THE System SHALL 扁平化为 Map 结构
4. WHEN AI_Provider 返回的 redTags 或 greenTags 字段缺失 THEN THE System SHALL 使用空列表作为默认值
5. WHEN AI_Provider 返回的标签内容包含重复项 THEN THE System SHALL 自动去重

### 需求 6: 降级策略

**用户故事**: 作为系统开发者，我希望系统在解析失败时能够优雅降级，以便保证用户体验不受影响。

#### 验收标准

1. WHEN Response_Parser 完全无法解析响应 THEN THE System SHALL 返回包含默认值的 Domain_Model
2. WHEN Response_Parser 部分字段解析失败 THEN THE System SHALL 使用默认值填充缺失字段
3. WHEN Response_Parser 连续多次解析失败 THEN THE System SHALL 记录错误日志并提示用户切换模型
4. WHEN Response_Parser 检测到响应格式异常 THEN THE System SHALL 尝试多种解析策略
5. WHEN Response_Parser 使用降级策略 THEN THE System SHALL 对用户透明，不显示错误提示

### 需求 7: 性能优化

**用户故事**: 作为系统开发者，我希望响应解析过程高效快速，以便不影响用户体验。

#### 验收标准

1. WHEN Response_Parser 处理标准格式响应 THEN THE System SHALL 在 100 毫秒内完成解析
2. WHEN Response_Parser 处理需要清洗的响应 THEN THE System SHALL 在 300 毫秒内完成解析
3. WHEN Response_Parser 处理需要字段映射的响应 THEN THE System SHALL 在 500 毫秒内完成解析
4. WHEN Response_Parser 处理大型响应（超过 10KB）THEN THE System SHALL 在 1 秒内完成解析
5. WHEN Response_Parser 加载字段映射配置 THEN THE System SHALL 在应用启动时一次性加载

### 需求 8: 可扩展性

**用户故事**: 作为系统开发者，我希望解析器易于扩展，以便支持未来新增的 AI 模型和响应格式。

#### 验收标准

1. WHEN 系统需要支持新的 Domain_Model THEN THE System SHALL 允许通过创建新的解析方法扩展
2. WHEN 系统需要支持新的字段映射 THEN THE System SHALL 允许通过配置文件添加新的映射规则
3. WHEN 系统需要支持新的清洗策略 THEN THE System SHALL 允许在 JSON_Cleaner 中添加新的清洗规则
4. WHEN 系统需要调整降级策略 THEN THE System SHALL 允许在代码中修改默认值常量
5. WHEN 系统需要调试解析过程 THEN THE System SHALL 提供详细的日志输出

### 需求 9: 错误处理

**用户故事**: 作为系统开发者，我希望解析器能够提供清晰的错误信息，以便快速定位和修复问题。

#### 验收标准

1. WHEN Response_Parser 遇到 JSON 语法错误 THEN THE System SHALL 记录原始响应和错误位置
2. WHEN Response_Parser 遇到字段类型不匹配 THEN THE System SHALL 记录字段名和期望类型
3. WHEN Response_Parser 遇到必填字段缺失 THEN THE System SHALL 记录缺失字段列表
4. WHEN Response_Parser 遇到未知字段 THEN THE System SHALL 记录警告但继续解析
5. WHEN Response_Parser 遇到编码问题 THEN THE System SHALL 尝试多种编码方式并记录结果

### 需求 10: 现有代码兼容性

**用户故事**: 作为系统开发者，我希望新的解析增强功能能够与现有代码无缝集成，以便不破坏现有功能。

#### 验收标准

1. WHEN 系统升级解析器 THEN THE System SHALL 保持现有 Domain_Model 接口不变
2. WHEN 系统升级解析器 THEN THE System SHALL 保持现有 Repository 接口不变
3. WHEN 系统升级解析器 THEN THE System SHALL 复用现有的 preprocessJsonResponse 方法
4. WHEN 系统升级解析器 THEN THE System SHALL 增强现有的 parseFallbackAnalysisResult 方法
5. WHEN 系统升级解析器 THEN THE System SHALL 保持现有测试用例通过

### 需求 11: 测试覆盖

**用户故事**: 作为系统开发者，我希望解析器有完善的测试覆盖，以便确保功能稳定可靠。

#### 验收标准

1. WHEN 系统运行单元测试 THEN THE System SHALL 覆盖所有解析场景
2. WHEN 系统运行属性测试 THEN THE System SHALL 验证解析器对随机输入的鲁棒性
3. WHEN 系统运行集成测试 THEN THE System SHALL 验证与真实 AI_Provider 的兼容性
4. WHEN 系统运行性能测试 THEN THE System SHALL 验证解析性能满足要求
5. WHEN 系统运行回归测试 THEN THE System SHALL 验证修复不引入新问题

### 需求 12: 提示词增强（第二次增强）

**用户故事**: 作为系统开发者，我希望通过更强的提示词约束来提高 AI 模型返回正确格式的概率，以便减少解析失败的情况。

#### 验收标准

1. WHEN 系统发送分析请求 THEN THE System SHALL 使用包含 JSON Schema 定义的提示词
2. WHEN 系统发送分析请求 THEN THE System SHALL 在提示词中明确禁止使用中文字段名
3. WHEN 系统发送分析请求 THEN THE System SHALL 在提示词中提供多个正确格式示例
4. WHEN 系统发送分析请求 THEN THE System SHALL 在提示词中明确列出所有可能的错误格式
5. WHEN AI_Provider 支持 response_format 参数 THEN THE System SHALL 启用 JSON 模式
6. WHEN 系统发送安全检查请求 THEN THE System SHALL 使用相同的格式约束策略
7. WHEN 系统发送数据提取请求 THEN THE System SHALL 使用相同的格式约束策略

### 需求 13: 字段映射配置增强（第二次增强）

**用户故事**: 作为系统开发者，我希望字段映射配置更加完善和易于维护，以便快速适配新的 AI 模型响应格式。

#### 验收标准

1. WHEN 系统加载字段映射配置 THEN THE System SHALL 从 assets/field_mappings.json 文件读取配置
2. WHEN 配置文件加载失败 THEN THE System SHALL 使用硬编码的默认配置作为后备
3. WHEN 系统需要新增字段映射 THEN THE System SHALL 允许通过修改配置文件添加新映射
4. WHEN 系统执行字段映射 THEN THE System SHALL 支持递归处理嵌套 JSON 结构
5. WHEN 系统执行字段映射 THEN THE System SHALL 记录映射统计信息（成功/失败数量）
6. WHEN 配置文件包含新的中文字段变体 THEN THE System SHALL 自动识别并映射

### 需求 14: 多层保底机制（第二次增强）

**用户故事**: 作为系统开发者，我希望系统有多层保底机制，以便在任何情况下都能返回有效结果。

#### 验收标准

1. WHEN 标准 Moshi 解析失败 THEN THE System SHALL 尝试使用 lenient 模式解析
2. WHEN lenient 模式解析失败 THEN THE System SHALL 尝试使用 Map 解析并手动提取字段
3. WHEN Map 解析失败 THEN THE System SHALL 尝试使用正则表达式提取关键字段
4. WHEN 所有解析策略都失败 THEN THE System SHALL 返回预定义的默认值
5. WHEN 部分字段解析成功 THEN THE System SHALL 使用默认值填充缺失字段
6. WHEN 使用默认值 THEN THE System SHALL 记录详细的降级日志
7. WHEN 连续多次使用默认值 THEN THE System SHALL 提示用户检查 AI 模型配置
