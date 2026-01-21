# 设置功能开发规范

> 最后更新: 2026-01-21

## 🔴 必读文档

**开发设置功能相关代码前，必须先阅读：**

1. **[PRD-00002-设置功能需求](../../文档/开发文档/PRD/PRD-00002-设置功能需求.md)** - 完整需求文档
2. **[WORKSPACE.md](../../WORKSPACE.md)** - 当前工作状态

---

## 当前设置项（基于代码实现）

1. **AI 服务商配置**
   - 当前默认服务商展示与切换
   - 跳转到服务商管理页面（AI 配置）
   - Provider 预设：OpenAI GPT-4 / GPT-3.5、Google Gemini Pro、DeepSeek + 自定义兼容

2. **提示词设置**
   - 使用 `PromptScene.SETTINGS_SCENE_ORDER`
   - 设置页展示场景：ANALYZE、POLISH、REPLY、SUMMARY、AI_ADVISOR
   - 进入提示词编辑器进行模板编辑

3. **隐私保护**
   - 数据掩码开关（默认开启）
   - 本地优先模式开关（默认开启）

4. **AI 分析历史条数**
   - 预设选项：0 / 5 / 10
   - 影响分析时携带历史对话的条数

5. **悬浮窗与截屏权限**
   - 悬浮窗开关与权限引导
   - 截屏权限缓存与连续截屏开关

6. **数据管理**
   - 清除全部数据
   - 清除 AI 军师草稿

7. **关于信息**
   - 应用版本号展示

8. **开发者选项（开发者模式解锁后显示）**
   - 系统提示词编辑入口

### 未实现/未暴露的设置

- 主题切换、语言切换、通知细化配置（当前 UI 未提供入口）

---

## 文件位置（多模块架构）

```
:domain/src/main/kotlin/com/empathy/ai/domain/repository/
└── SettingsRepository.kt
└── FloatingWindowPreferencesRepository.kt

:data/src/main/kotlin/com/empathy/ai/data/
├── repository/settings/SettingsRepositoryImpl.kt
└── local/
    ├── PrivacyPreferences.kt
    ├── ConversationPreferences.kt
    ├── FloatingWindowPreferences.kt
    ├── AiAdvisorPreferences.kt
    └── DeveloperModePreferences.kt

:presentation/src/main/kotlin/com/empathy/ai/presentation/
├── ui/screen/settings/
│   ├── SettingsScreen.kt
│   ├── SettingsUiState.kt
│   ├── SettingsUiEvent.kt
│   ├── SystemPromptListScreen.kt
│   └── SystemPromptEditScreen.kt
├── ui/screen/settings/component/
│   ├── HistoryConversationCountSection.kt
│   ├── PromptSettingsSection.kt
│   └── DeveloperOptionsSection.kt
└── viewmodel/SettingsViewModel.kt
```

---

## 关键实现点

### 1. 设置持久化

- 使用 `SettingsRepositoryImpl`（EncryptedSharedPreferences，失败时降级到普通 SharedPreferences）
- 隐私设置与历史条数通过 `PrivacyPreferences` / `ConversationPreferences` 承载

### 2. 场景列表与提示词入口

- 设置页展示 `PromptScene.SETTINGS_SCENE_ORDER`
- 由 `PromptSettingsSection` 负责场景列表与跳转

### 3. 悬浮窗权限流程

- ViewModel 触发 `pendingPermissionRequest`
- UI 统一处理系统权限跳转，并在完成后清除标志
- 结合可见性门控，避免隐藏 Tab 触发权限跳转

---

## 默认值

- **数据掩码**: 开启 (`true`)
- **本地优先模式**: 开启 (`true`)
- **历史对话条数**: 5
- **悬浮窗开关**: 关闭 (`false`)
- **连续截屏**: 关闭 (`false`)

---

## 相关文档

- [PRD-00002-设置功能需求](../../文档/开发文档/PRD/PRD-00002-设置功能需求.md)
- [product.md](./product.md) - 产品概览
- [structure.md](./structure.md) - 项目结构
- [tech.md](./tech.md) - 技术栈
