# BUG-00070：悬浮球显示异常（多显示屏切换）

## 文档信息
| 项目 | 内容 |
|------|------|
| 文档编号 | BUG-00070 |
| 问题级别 | P0 |
| 状态 | 进行中 |
| 编写者 | Codex |
| 日期 | 2026-01-13 |
| 关联文档 | TSG-00070、PRD-00010、TDD-00010、FD-00010、PRD-00036、TDD-00036、FD-00036 |

---

## 1. 问题现象
- 第一阶段：悬浮球在桌面或其他 App 可见，但在本 App 内不可见。
- 当前阶段：悬浮球在本 App 内可见，但切换到其他 App 后不可见。
- 设备与环境：MuMu（多 displayId）与 OPPO 真机均可复现。

## 2. 复现路径
1. 安装 debug 包，授予悬浮窗权限。
2. 从设置开启悬浮窗功能。
3. 进入 App 主界面，观察悬浮球可见。
4. 切换到其他 App，悬浮球不可见。

## 3. 现有证据（更新）
- `dumpsys window windows`：`com.empathy.ai` 与悬浮层均在 `displayId=36`，其他应用在 `displayId=0`。
- 日志：出现 `App前台 displayId=36`，但未出现 `App后台` 或 `rebindToDisplay`。
- 结论：悬浮层始终绑定 App 的 display，未切回默认 display。
- 2026-01-14 测试：MuMu 上 `dumpsys` 显示 App/悬浮窗位于 `displayId=2`，系统其他应用在 `displayId=0`；logcat 未出现 `App后台(Process)`/`UI隐藏`/`rebindToDisplay`，说明前后台监听未触发。

---

## 4. 根因推导
### 4.1 机制与规范分析
- WindowManager 在多显示屏环境绑定特定 Display。
- 若服务绑定 App 的 displayId，则悬浮层固定在 App 所在 Display。
- 当 App 进入后台，其他应用位于默认 display 0，悬浮层不在同一 display，因而不可见。

### 4.2 根因结论
**根因：悬浮层绑定了 App 的 displayId，但 App 进入后台后未切回默认 display，导致在其他应用界面不可见。**

---

## 5. 解决方案对比

### 方案 A（保守）
**内容**：固定使用 App 的 displayId。
**优点**：App 内显示稳定。
**缺点**：切换到其他应用不可见。

### 方案 B（推荐）
**内容**：动态切换 display 绑定。前台绑定当前 Activity 的 displayId，后台切回 `Display.DEFAULT_DISPLAY`；在服务内增加兜底心跳检测当前前台任务的 displayId，不在本 App 时强制 rebind 到默认 display，避免前后台监听失效。
**实现建议**：
- 使用 `ProcessLifecycleOwner` 监听 `ON_START/ON_STOP` 切换 display。
- 在 `onTrimMemory(TRIM_MEMORY_UI_HIDDEN)` 兜底切回默认 display。
- 发生切换时调用 `rebindToDisplay()` 重建 WindowManager。
- 在服务内定时检测前台任务包名和显示屏，若不在 App 且当前绑定非默认 display，则强制切回默认 display（双保险）。
**优势**：App 内与外部应用均可见，适配多显示屏与分屏环境。
**Trade-off**：需要更严谨的生命周期与幂等处理。

**最终推荐：方案 B。**

---

## 6. 影响评估
- **正向影响**：解决 App 内与其他应用可见性冲突，恢复截图入口与悬浮窗交互完整性。
- **潜在风险**：切换时机不稳导致闪烁或重建频繁。
- **兼容性**：多显示屏设备、模拟器、分屏模式。

---

## 7. 验收标准
- [ ] App 内悬浮球可见。
- [ ] 切换到其他 App 后悬浮球可见。
- [ ] App 前后台切换触发 display 绑定切换。
- [ ] 悬浮球拖动、最小化与恢复功能无回归。

---

## 8. 问题复盘（DeBug 规范）
## 问题记录
- **现象**: App 内可见，其他应用不可见。
- **根因**: 悬浮层绑定 App displayId，后台未切回默认 display。
- **方案 A**: 固定绑定 App display（外部不可见）。
- **方案 B**: 前台绑定 Activity display，后台切回默认 display（推荐）。
- **结论**: 采用方案 B。
- **关联文档**: PRD-00010 / TDD-00010 / FD-00010 / TSG-00070 / PRD-00036 / TDD-00036 / FD-00036
