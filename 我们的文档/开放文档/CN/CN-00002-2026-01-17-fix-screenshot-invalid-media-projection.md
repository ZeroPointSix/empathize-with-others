# 修复二次截图 Invalid MediaProjection 与本地重试链路

<analysis>
已汇总本轮会话的用户意图、日志定位、关键代码改动、构建与安装状态，以及未完成的验证项，便于继续推进排查与修复。
</analysis>

<plan>
# Session Handoff Plan

## 1. Primary Request and Intent
- 继续后续任务：定位“第一次截图成功后，第二次截图失败”的根因并修复。
- 要求：优先在本地获取日志/信息，不要反复要求用户提供。
- 增补规则：在 Rules 中新增“本地优先获取信息”的规则。
- 构建与安装：编译 Debug 并安装到 MuMu 供用户测试。

## 2. Key Technical Concepts
- Android MediaProjection/VirtualDisplay
- 前台服务类型 `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION`
- 悬浮窗截图流程与遮罩选区
- Kotlin 协程与异常处理
- ADB logcat 本地日志抓取

## 3. Files and Code Sections
### app/src/main/java/com/empathy/ai/service/FloatingWindowService.kt
- **Why important**: 截图入口、授权处理、选区截图与失败提示逻辑都在此文件。
- **Changes made**:
  - `startScreenshotFlow()` 在进入截图前强制 `updateForegroundServiceType(includeMediaProjection=true)`。
  - `captureSelection()` 捕获 `SecurityException`，触发本地重建 MediaProjection 并重试一次；失败则提示重新授权。
  - 新增 `retryCaptureWithRestoredProjection()`，封装重建/重试逻辑。
  - 增强流程日志、附件日志与错误提示。
- **Code snippet**:
```kotlin
private fun captureSelection(rect: Rect) {
    ...
    val helper = screenshotCaptureHelper ?: ScreenshotCaptureHelper(this@FloatingWindowService)
    var invalidProjection = false
    val attachment = try {
        helper.captureRegion(projection, rect)
    } catch (e: SecurityException) {
        invalidProjection = true
        android.util.Log.e("FloatingWindowService", "captureSelection: invalid media projection", e)
        val retry = retryCaptureWithRestoredProjection(helper, rect)
        if (retry == null) {
            android.util.Log.w("FloatingWindowService", "captureSelection: restore projection failed, request permission")
            floatingWindowPreferences.clearScreenshotPermission()
            requestMediaProjectionPermission()
        }
        retry
    }
    if (attachment == null) {
        val message = if (invalidProjection) "截图权限已失效，请重新授权" else "截图失败，请重试"
        android.widget.Toast.makeText(this@FloatingWindowService, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    ...
}

private suspend fun retryCaptureWithRestoredProjection(
    helper: ScreenshotCaptureHelper,
    rect: Rect
): ScreenshotAttachment? {
    releaseMediaProjection()
    if (!restoreMediaProjectionFromCache()) return null
    val projection = mediaProjection ?: return null
    return try {
        helper.captureRegion(projection, rect)
    } catch (e: SecurityException) {
        android.util.Log.e("FloatingWindowService", "retryCaptureWithRestoredProjection failed", e)
        null
    }
}
```

### app/src/main/java/com/empathy/ai/domain/util/ScreenshotCaptureHelper.kt
- **Why important**: MediaProjection 捕获与裁剪落盘核心实现。
- **Changes made**:
  - 增加完整日志（屏幕参数、createVirtualDisplay、裁剪、落盘）。
  - 遇到 `SecurityException` 直接抛出给上层处理（用于重试/授权逻辑）。
- **Code snippet**:
```kotlin
} catch (e: SecurityException) {
    Log.e("ScreenshotCapture", "captureRegion failed ts=$startMs", e)
    throw e
} catch (e: Exception) {
    Log.e("ScreenshotCapture", "captureRegion failed ts=$startMs", e)
    null
}
```

### Rules/RulesReadMe.md
- **Why important**: 项目最高优先级规则文件。
- **Changes made**: 新增规则 8：所有信息优先本地获取，无法获取才请求用户协助。

### WORKSPACE.md
- **Why important**: 多 Agent 工作记录。
- **Changes made**: 记录 BUG-00072-LOG 任务开始与更新时间。

### gradle.properties / config/version-history.json
- **Why important**: 版本号由 updateVersion 自动维护。
- **Changes made**: 版本已从 1.8.0 → 1.11.0，versionCode 从 10800 → 11100（dev）。

## 4. Problem Solving
- 已定位第二次截图失败的根因：`SecurityException: Invalid media projection`（`createVirtualDisplay` 处抛出）。
- 已尝试修复：
  - 进入截图前显式开启 MediaProjection 前台服务类型。
  - 失败时本地重建 MediaProjection 并重试一次；失败后提示重新授权并重新发起授权流程。
- 当前待验证：修复后是否仍可复现第二次截图失败。

## 5. Pending Tasks
- 需要在 MuMu 上再次验证：第一次截图→发送→第二次截图是否成功。
- 若仍失败，进一步分析日志，考虑强制每次截图重新授权或调整前台服务类型切换策略。

## 6. Current Work
- 已完成修复并构建/安装到 MuMu：
  - `./gradlew.bat updateVersion` → 1.11.0
  - `./gradlew.bat assembleDebug`
  - `adb -s 127.0.0.1:7555 install -r app/build/outputs/apk/debug/app-debug.apk`
- 本地日志读取并过滤：确认第二次截图 `Invalid media projection` 仍会出现（历史日志）。

## 7. Optional Next Step
- 在 MuMu 复测二次截图链路并抓取 logcat；若仍失败，基于新日志继续调整 MediaProjection 生命周期与前台服务类型切换。
</plan>
