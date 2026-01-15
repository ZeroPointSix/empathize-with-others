## BUG-00068: AI军师入口与设置回退及非Tab性能覆盖问题

### 1. 基本信息
- **优先级**: P0 (问题1/2), P1 (问题3)
- **影响范围**: AI军师入口页、设置 -> AI配置/提示词编辑返回链路、非Tab页面切换性能
- **首次发现**: 2026-01-10
- **状态**: 分析中（待补充日志/复现环境）

### 2. 关联文档
- PRD-00034-界面切换性能优化-页面缓存方案
- TDD-00034-界面切换性能优化-页面缓存方案技术设计
- FD-00034-界面切换性能优化-页面缓存方案功能设计
- PRD-00025-AI配置功能完善
- TDD-00025-AI配置功能完善技术设计
- FD-00025-AI配置功能完善功能设计
- PRD-00026-AI军师对话功能需求
- TDD-00026-AI军师对话功能技术设计
- FD-00026-AI军师对话功能设计

### 3. 现象描述
1) **问题1**: 从联系人切换到AI军师，首次正常显示，之后再次切回不显示内容（空白/仅背景）。
2) **问题2**: 从设置 -> AI配置或编辑提示词设置返回时，期望回到设置页，实际回到联系人列表。
3) **问题3**: 仅底部导航页面做了缓存优化，联系人详情、AI知识、设置更深层页面等未同步优化。

### 4. 复现步骤 (待补充设备/版本)
#### 问题1
1. 打开应用，保持底部Tab可见。
2. 切换到“AI军师”Tab。
3. 首次可进入聊天/联系人选择。
4. 切回“联系人”Tab。
5. 再切回“AI军师”Tab，出现空白或不显示。

#### 问题2
1. 打开“设置”Tab。
2. 进入“AI配置”或“提示词设置”页面。
3. 点击返回。
4. 实际返回联系人列表，而非设置页。

#### 问题3
1. 从联系人进入联系人详情。
2. 进入AI知识或设置的深层页面。
3. 观察切换性能与黑屏/重建现象。

### 5. 期望结果
- 问题1: 每次切换到“AI军师”Tab均显示有效内容，并自动根据上次联系人选择跳转。
- 问题2: 由设置页进入的子页面返回时应回到设置页。
- 问题3: 非Tab页面切换应有一致的性能优化策略（减少重建、避免黑屏）。

### 6. 实际结果
- 问题1: AI军师入口页在第二次进入时无导航触发，界面空白。
- 问题2: 返回时被导航栈带回联系人列表。
- 问题3: 仅Tab页面做缓存，其它页面仍按默认NavHost重建和动画。

### 7. 初步根因假设
#### 问题1 (AI军师入口空白)
- **疑似原因**: `AiAdvisorEntryViewModel` 仅在 init 时计算导航目标，进入一次后 `navigationTarget` 被重置为 null，配合 `BottomNavScaffold` 的页面缓存，二次进入不会重新触发导航逻辑，导致入口页仅剩背景。
- **相关代码**:
  - `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/advisor/AiAdvisorScreen.kt`
  - `presentation/src/main/kotlin/com/empathy/ai/presentation/viewmodel/AiAdvisorEntryViewModel.kt`
  - `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/navigation/BottomNavScaffold.kt`

#### 问题2 (设置返回错误)
- **疑似原因**: 非Tab导航栈的起点为 `CONTACT_LIST`，从设置Tab进入 `AI_CONFIG` / 提示词编辑后，返回 `navigateUp()` 会回到 `CONTACT_LIST` 而非设置，因为设置Tab并未入栈。
- **相关代码**:
  - `app/src/main/java/com/empathy/ai/ui/MainActivity.kt`
  - `presentation/src/main/kotlin/com/empathy/ai/presentation/navigation/NavGraph.kt`
  - `presentation/src/main/kotlin/com/empathy/ai/presentation/navigation/NonTabNavGraph.kt`

#### 问题3 (性能覆盖不一致)
- **疑似原因**: 缓存策略只覆盖 `BottomNavScaffold` 的三大Tab，非Tab页面仍使用默认 `NavHost` 动画与重建逻辑，未引入统一缓存/复用策略。
- **相关代码**:
  - `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/navigation/BottomNavScaffold.kt`
  - `app/src/main/java/com/empathy/ai/ui/MainActivity.kt`

### 8. 方案对比 (Dual Solution Protocol)
#### 方案A (保守修复)
- **内容**:
  - 问题1: 在 `AiAdvisorScreen` 进入时手动触发一次 `checkNavigationTarget` 或增加 `onResume` 刷新。
  - 问题2: 在 `AI_CONFIG` / 提示词编辑返回逻辑中手动跳转 `Settings` Tab。
  - 问题3: 局部优化常用非Tab页面（联系人详情、AI知识）。
- **优点**: 侵入少、改动小、风险低。
- **缺点**: 仍存在“入口页仅做导航”的空白体验；返回逻辑分散且易回归；性能策略不统一。

#### 方案B (推荐方案)
- **内容**:
  - 问题1: 将 AI军师Tab的“入口页”改为稳定可见内容（例如：联系人选择/最近会话列表）并在 Tab 可见时刷新导航目标；避免纯导航页在缓存下空白。
  - 问题2: 建立“Tab来源返回策略”统一处理（例如：从Tab进入非Tab页面时记录来源Tab并在返回时恢复），并在 NavGraph 侧统一处理返回逻辑。
  - 问题3: 扩展缓存策略到非Tab关键页面，或引入轻量级 `SaveableStateHolder`/`rememberSaveable` 缓存栈，减少重建与黑屏。
- **权衡**: 实施范围更大，需要回归更多页面与导航路径。

### 9. 影响评估
- 导航路径调整可能影响已有 `navigateUp()` 行为，需回归涉及设置、AI军师、联系人详情的链路。
- 页面缓存扩展可能增加内存占用，需要明确缓存上限与释放策略。

### 10. 待补充信息 (STAR Gate)
- **Situation/Task**: 具体业务场景与目标版本。
- **Action**: 已尝试的排查动作（例如清缓存、切换账号、不同设备）。
- **Result**: 具体logcat或堆栈信息。

