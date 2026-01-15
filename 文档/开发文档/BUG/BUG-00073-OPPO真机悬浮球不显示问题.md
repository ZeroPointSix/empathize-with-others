# BUG-00073：OPPO 真机启动后悬浮球不显示

## 文档信息
| 项目 | 内容 |
|------|------|
| 文档编号 | BUG-00073 |
| 问题级别 | P0 |
| 状态 | 已完成 |
| 编写者 | Codex |
| 日期 | 2026-01-15 |
| 关联文档 | PRD-00036、TDD-00036、FD-00036 |

---
## 1. 问题现象
- OPPO Reno12（ColorOS 15 / Android 15）启动后悬浮球始终不可见。
- 设置页提示“悬浮窗服务已启动”，但通知栏无前台服务提示。
- 旧版本可正常显示，截图功能上线后开始出现。

## 2. 复现路径
1. 安装 debug 包并授予悬浮窗权限。
2. 在设置页开启悬浮球功能。
3. 重启 App，悬浮球仍不可见（稳定复现）。

## 3. 现有证据
- Logcat：
  - `SecurityException: Starting FGS with type mediaProjection ... requires ... CAPTURE_VIDEO_OUTPUT / project_media`
  - `降级通知也失败，停止服务`
- 服务在 `startForeground` 阶段被系统阻断，导致视图未创建。

---
## 4. 根因结论
- `FloatingWindowService` 在启动时使用 `foregroundServiceType="specialUse|mediaProjection"`。
- Android 15/ColorOS 15 要求使用 `mediaProjection` 类型的 FGS 时必须具备投屏相关权限或 MediaProjection 授权。
- 启动阶段尚未获取截图授权，`startForeground` 抛出 `SecurityException`，服务被停止，悬浮球无法创建。

---
## 5. 修复方案
- 启动阶段仅声明 `specialUse` 类型，避免触发 `mediaProjection` 权限校验。
- 在 `handleMediaProjectionResult` 授权成功后，动态将 FGS 类型升级为 `specialUse|mediaProjection`。
- 截图结束后回退为 `specialUse`，避免非截图场景持有多余类型。

**关键改动**：`app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt`

---
## 6. 验证结论
- OPPO 真机日志出现 `前台服务启动成功` 与 `悬浮球已显示`。
- 未再出现 `SecurityException`，悬浮球恢复可见。

---
## 7. 影响评估
- **正向影响**：恢复 OPPO 真机悬浮球显示。
- **兼容性**：Android 14/15 的 FGS 类型策略更稳定。
- **潜在风险**：若截图授权回调异常，FGS 类型可能无法升级，但不影响悬浮球显示。

---
## 8. 验收标准
- [x] OPPO 真机启动后悬浮球可见。
- [x] 启动阶段无 `SecurityException`。
- [x] 截图授权后可进入截图流程。

---
## 9. 问题复盘（DeBug 规范）
- **现象**：OPPO 真机悬浮球始终不可见。
- **根因**：启动时使用 `mediaProjection` 类型 FGS，被系统权限校验拦截。
- **方案**：启动阶段剔除 `mediaProjection`，授权后再动态添加。
- **结论**：修复后悬浮球恢复显示。

## 10. 经验教训与效率瓶颈
- **瓶颈**：早期日志抓在模拟器上，缺少真机日志导致定位延迟。
- **瓶颈**：版本号未及时递增，安装被降级拦截。
- **经验**：Android 14/15 对 FGS 类型校验更严格，启动阶段避免携带未授权类型。
- **经验**：遇到“功能不可见”，先确认服务是否被系统杀死，再定位 UI 层问题。
