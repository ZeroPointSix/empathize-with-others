# 产品概览

## 🔴 必读文档（开始工作前必须阅读）

**在开始任何工作之前，请务必先阅读以下文档：**

1. **[Rules/RulesReadMe.md](../../Rules/RulesReadMe.md)** - 项目通用规则和文档规范
2. **[WORKSPACE.md](../../WORKSPACE.md)** - 当前工作状态和任务协调

---

## 语言规范

**所有文档和回答必须使用中文。** 代码注释、变量名、类名等保持英文，但所有说明文档、开发指南和与开发者的沟通必须使用中文。

## 产品定位

**共情 AI 助手（Empathy AI）**是一款隐私优先的 Android 社交沟通辅助应用，提供对话分析、沟通建议与多轮咨询能力，帮助用户更好地理解社交场景。

## 核心原则

- **隐私优先**：数据本地存储，AI 调用前强制脱敏
- **本地优先**：本地规则优先，必要时再调用 AI
- **自带密钥（BYOK）**：用户自行配置 AI 服务商与 API Key
- **辅助决策**：提供建议与分析，不替用户决策

## 主要功能（基于当前代码）

1. **AI 分析与建议**：AnalyzeChatUseCase + ChatScreen，支持风险等级与分析结果展示
2. **草稿润色 / 回复生成**：PolishDraftUseCase、GenerateReplyUseCase
3. **安全检查**：CheckDraftUseCase，支持本地规则 + AI 兜底
4. **联系人画像系统**：联系人资料、事实流、标签画像、主题管理
5. **AI 军师多轮对话**：会话管理、流式响应、Markdown 渲染
6. **提示词与系统提示词管理**：PromptScene、PromptEditor、SystemPromptList/Edit
7. **AI 服务商配置**：预设服务商、模型管理、代理配置、用量统计
8. **数据总结与回溯**：DailySummary / ManualSummary 支持手动与自动总结
9. **悬浮窗与权限管理**：FloatingWindowService + 截屏权限管理
10. **隐私与安全存储**：EncryptedSharedPreferences + 数据脱敏开关

## 部分实现/待完善

- **媒体转录**：`AiRepositoryImpl.transcribeMedia()` 仍返回未实现错误，需补充音视频转录链路

## 当前状态（基于项目配置）

- **当前分支**: main
- **当前版本**: v1.14.17 (versionCode: 11417, dev)
- **模块架构**: :domain / :data / :presentation / :app
- **数据库版本**: Room v17（11 张表，迁移 1→17）
- **提示词场景**: ANALYZE / POLISH / REPLY / SUMMARY / AI_ADVISOR（CHECK、EXTRACT 为隐藏废弃）
- **AI 服务商预设**: OpenAI GPT-4 / GPT-3.5、Google Gemini Pro、DeepSeek + 自定义 OpenAI 兼容服务商

---

**文档版本**: 4.0
**最后更新**: 2026-01-21
**更新内容**:
- 基于当前代码结构与配置更新功能与状态描述
