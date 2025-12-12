# 真实 AI 模型集成测试指南

## 概述

本文档提供了如何测试 AI 响应解析器与真实 AI 模型兼容性的详细指南。

## 测试目标

1. 验证解析器能够处理不同 AI 模型的响应格式
2. 确保解析成功率 ≥ 95%
3. 测试多个服务商和模型的兼容性

## 测试环境

### 服务商配置

#### 1. x666.me
- **Base URL**: `https://x666.me`
- **API Key**: `sk-BaN9AcQXfzNmMy12i4nYpQonxm78loqFrQbt4LGBzlxMKpVD`
- **可用模型**:
  - `gpt-4o-mini`
  - `gpt-4.1-mini`
  - `gpt-4.1-nano`
  - `gemini-2.5-flash`

#### 2. ModelScope
- **Base URL**: `https://api-inference.modelscope.cn`
- **API Key**: `ms-fc7e77c9-c7cd-4b41-81e1-a567e01e49b5`
- **可用模型**:
  - `Qwen/Qwen3-235B-A22B-Thinking-2507`
  - `Qwen/Qwen3-Coder-480B-A35B-Instruct`
  - `Qwen/Qwen3-235B-A22B-Instruct-2507`
  - `MiniMax/MiniMax-M1-80k`
  - `deepseek-ai/DeepSeek-R1`

## 测试方法

### 方法 1: 使用集成测试（推荐）

已创建的集成测试文件：`app/src/test/java/com/empathy/ai/integration/RealAiModelIntegrationTest.kt`

#### 运行测试

```bash
# 运行所有集成测试
./gradlew testDebugUnitTest --tests "*RealAiModelIntegrationTest*"

# 或者运行特定测试
./gradlew testDebugUnitTest --tests "*RealAiModelIntegrationTest.test AnalysisResult parsing with real models*"
```

#### 测试内容

1. **测试 1: 列出预配置的模型**
   - 显示将要测试的所有模型
   - 验证配置正确

2. **测试 2: AnalysisResult 解析测试**
   - 对每个模型发送相同的测试 Prompt
   - 验证能够正确解析响应
   - 计算解析成功率

3. **测试 3: SafetyCheckResult 解析测试**
   - 测试安全检查功能
   - 验证能够正确解析响应

### 方法 2: 使用应用手动测试

#### 步骤 1: 配置 AI 服务商

1. 打开应用
2. 进入设置页面
3. 添加 AI 服务商配置：
   - 名称: `x666.me`
   - Base URL: `https://x666.me`
   - API Key: `sk-BaN9AcQXfzNmMy12i4nYpQonxm78loqFrQbt4LGBzlxMKpVD`
   - 默认模型: `gpt-4o-mini`

#### 步骤 2: 测试聊天分析

1. 创建一个测试联系人
2. 添加一些聊天记录
3. 点击"帮我分析"按钮
4. 验证：
   - 能够成功获取 AI 响应
   - 响应能够正确解析
   - 显示回复建议、策略分析和风险等级

#### 步骤 3: 测试安全检查

1. 在输入框输入测试文本
2. 点击"帮我检查"按钮
3. 验证：
   - 能够成功获取 AI 响应
   - 响应能够正确解析
   - 显示安全检查结果

#### 步骤 4: 测试数据提取

1. 准备一段测试文本
2. 使用数据提取功能
3. 验证：
   - 能够成功获取 AI 响应
   - 响应能够正确解析
   - 正确提取事实、雷区和策略

#### 步骤 5: 切换模型测试

重复步骤 2-4，但使用不同的模型：
- `gpt-4.1-mini`
- `gpt-4.1-nano`
- `gemini-2.5-flash`

#### 步骤 6: 测试 ModelScope

1. 添加 ModelScope 服务商配置
2. 重复步骤 2-5，使用 ModelScope 的模型

## 测试用例

### 测试用例 1: 标准格式响应

**输入 Prompt**:
```
【对话上下文】
用户: 今天钓鱼没收获，有点沮丧

【联系人画像】
姓名: 李四
爱好: 钓鱼
雷区: 不要提钱
策略: 多夸他衣品好

请分析这段对话并给出建议。
```

**预期响应格式**:
```json
{
  "replySuggestion": "...",
  "strategyAnalysis": "...",
  "riskLevel": "SAFE"
}
```

**验证点**:
- ✅ 响应包含所有必需字段
- ✅ 字段类型正确
- ✅ 能够成功解析为 AnalysisResult

### 测试用例 2: 中文字段响应

**预期响应格式**:
```json
{
  "回复建议": "...",
  "策略分析": "...",
  "风险等级": "安全"
}
```

**验证点**:
- ✅ 能够识别中文字段名
- ✅ 能够正确映射为英文字段
- ✅ 能够成功解析为 AnalysisResult

### 测试用例 3: Markdown 包裹的响应

**预期响应格式**:
```
```json
{
  "replySuggestion": "...",
  "strategyAnalysis": "...",
  "riskLevel": "SAFE"
}
```
```

**验证点**:
- ✅ 能够移除 Markdown 标记
- ✅ 能够提取 JSON 内容
- ✅ 能够成功解析为 AnalysisResult

### 测试用例 4: 嵌套结构响应

**预期响应格式**:
```json
{
  "replySuggestion": {
    "content": "...",
    "priority": "high"
  },
  "strategyAnalysis": {
    "emotion": "...",
    "insights": ["...", "..."]
  },
  "riskLevel": "SAFE"
}
```

**验证点**:
- ✅ 能够提取嵌套结构中的内容
- ✅ 能够组合为简单字符串
- ✅ 能够成功解析为 AnalysisResult

## 成功标准

### 解析成功率
- **目标**: ≥ 95%
- **计算方法**: (成功解析的响应数 / 总响应数) × 100%

### 性能指标
- **常规响应**: ≤ 300ms
- **复杂响应**: ≤ 500ms
- **大型响应**: ≤ 1000ms

### 功能完整性
- ✅ 所有字段都能正确解析
- ✅ 默认值机制正常工作
- ✅ 降级策略有效

## 测试记录

### 测试日期: ___________

#### x666.me 测试结果

| 模型 | 测试次数 | 成功次数 | 成功率 | 备注 |
|------|---------|---------|--------|------|
| gpt-4o-mini | | | | |
| gpt-4.1-mini | | | | |
| gpt-4.1-nano | | | | |
| gemini-2.5-flash | | | | |

#### ModelScope 测试结果

| 模型 | 测试次数 | 成功次数 | 成功率 | 备注 |
|------|---------|---------|--------|------|
| Qwen/Qwen3-235B-A22B-Thinking-2507 | | | | |
| Qwen/Qwen3-Coder-480B-A35B-Instruct | | | | |
| Qwen/Qwen3-235B-A22B-Instruct-2507 | | | | |
| MiniMax/MiniMax-M1-80k | | | | |
| deepseek-ai/DeepSeek-R1 | | | | |

### 总体统计

- **总测试次数**: _______
- **总成功次数**: _______
- **总成功率**: _______%
- **是否达标**: ☐ 是 ☐ 否

## 常见问题

### Q1: 测试失败怎么办？

**A**: 检查以下几点：
1. API Key 是否正确
2. 网络连接是否正常
3. 模型名称是否正确
4. 查看错误日志，确定失败原因

### Q2: 如何调试解析失败？

**A**: 
1. 启用详细日志：在 `AiRepositoryImpl` 中查看日志输出
2. 检查原始响应内容
3. 验证字段映射配置是否正确
4. 检查是否触发了降级策略

### Q3: 某个模型一直失败怎么办？

**A**:
1. 确认模型是否可用（可能已下线）
2. 检查模型名称是否正确
3. 尝试使用其他模型
4. 联系服务商确认模型状态

## 注意事项

1. **API 配额**: 注意 API 调用次数限制，避免超出配额
2. **测试间隔**: 建议在测试之间添加 1-2 秒延迟，避免请求过快
3. **网络环境**: 确保网络连接稳定
4. **日志记录**: 保存测试日志以便后续分析

## 下一步

完成测试后：
1. 填写测试记录表格
2. 计算总体成功率
3. 如果成功率 < 95%，分析失败原因并优化
4. 更新测试报告
5. 提交测试结果

## 参考资料

- [AI 响应解析增强设计文档](design.md)
- [需求文档](requirements.md)
- [回归测试报告](regression-test-report.md)
