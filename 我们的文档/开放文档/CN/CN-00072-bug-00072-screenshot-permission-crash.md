# Fix BUG-00072 Screenshot Permission Crash

<analysis>
用户核心诉求是继续修复 BUG-00072（截图权限申请/黑框缺失/崩溃），并生成可交接的文档。前序已经完成权限请求迁移到设置页、日志补齐、版本更新与多次真机验证。本轮重点是读取 OPPO 崩溃日志并定位根因，发现崩溃来自 ScreenshotPermissionActivity 在 onActivityResult 里直接调用 getMediaProjection，系统要求必须先有 mediaProjection 前台服务类型，导致 SecurityException。已移除 Activity 里 getMediaProjection 的调用，改为由 FloatingWindowService 更新前台服务类型后再调用 getMediaProjection。同步更新了 BUG 文档和 WORKSPACE，版本更新至 1.7.0 并重新构建安装。仍需用户在 OPPO 上复测：设置页授权 → 返回开关 → 悬浮窗截图是否出现黑色遮罩并生成缩略图。如果仍失败，需要再抓 logcat。
</analysis>

<plan>
# Session Handoff Plan

## 1. Primary Request and Intent
- 修复 BUG-00072：截图权限申请方式（设置页统一授权）、截图黑色遮罩框不出现、悬浮窗截图无响应。
- 发生崩溃后要求读取日志并解决。
- 要求更新文档、编写测试用例、编译并安装真机验证，完成后提交 Git。
- 当前请求：继续修复并生成交接文档，方便下一位继续跟进。

## 2. Key Technical Concepts
- Android MediaProjection 权限与授权 Intent
- 前台服务类型 `ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION`
- Activity/Service 权限回传链路（ScreenshotPermissionActivity → FloatingWindowService）
- SharedPreferences（floating_window_prefs）内存权限缓存
- OPPO 真机日志与 `adb logcat`
- Hilt 注入 Activity（@AndroidEntryPoint）
- Gradle 版本更新任务 `updateVersion`，版本号落盘

## 3. Files and Code Sections
### app/src/main/java/com/empathy/ai/ui/ScreenshotPermissionActivity.kt
- **Why important**: 截图权限授权中转 Activity，崩溃源头。
- **Changes made**: 移除 Activity 内 `getMediaProjection` 调用，避免 SecurityException；仅保存授权结果并回传 Service。
- **Code snippet**:
```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_CODE) {
        val requestSource = intent.getStringExtra(MediaProjectionPermissionConstants.EXTRA_REQUEST_SOURCE)
        android.util.Log.d(
            "ScreenshotPermission",
            "截图权限返回 resultCode=$resultCode, data=${data != null}, source=$requestSource"
        )
        floatingWindowPreferences.saveMediaProjectionPermission(resultCode, data)
        val resultIntent = Intent(this, FloatingWindowService::class.java).apply {
            action = FloatingWindowService.ACTION_MEDIA_PROJECTION_RESULT
            putExtra(FloatingWindowService.EXTRA_RESULT_CODE, resultCode)
            putExtra(FloatingWindowService.EXTRA_RESULT_DATA, data)
            putExtra(MediaProjectionPermissionConstants.EXTRA_REQUEST_SOURCE, requestSource)
        }
        startService(resultIntent)
        finish()
    }
}
```

### app/src/main/java/com/empathy/ai/service/FloatingWindowService.kt
- **Why important**: 截图入口与 MediaProjection 实例化位置。
- **Changes made**: 在 Service 中先 `updateForegroundServiceType(includeMediaProjection = true)`，再调用 `getMediaProjection`；失败则回退并提示。
- **Code snippet**:
```kotlin
updateForegroundServiceType(includeMediaProjection = true)
mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
if (mediaProjection == null) {
    android.widget.Toast.makeText(this, "截图权限初始化失败", android.widget.Toast.LENGTH_SHORT).show()
    updateForegroundServiceType(includeMediaProjection = false)
    return
}
beginScreenshotSession()
```

### data/src/main/kotlin/com/empathy/ai/data/local/FloatingWindowPreferences.kt
- **Why important**: 截图权限缓存与设置页开关状态来源。
- **Changes made**: 移除 Intent 序列化落盘（含 Binder/FD 会失败），改为内存缓存；清理持久化字段。
- **Code snippet**:
```kotlin
fun saveMediaProjectionPermission(resultCode: Int, data: Intent?) {
    if (resultCode != Activity.RESULT_OK || data == null) {
        clearMediaProjectionPermissionInternal()
        return
    }
    mediaProjectionPermissionCache = MediaProjectionPermission(resultCode, data)
    clearMediaProjectionPermissionPersisted()
    android.util.Log.d(TAG, "MediaProjection 授权已缓存（仅内存）")
}
```

### 文档/开发文档/BUG/BUG-00072-截图黑屏排查尝试记录.md
- **Why important**: 记录根因、日志证据、修复方案与验收标准。
- **Changes made**: 补充 OPPO 崩溃日志与 SecurityException 原因，更新修复方案（只在 Service 获取 MediaProjection）。

### WORKSPACE.md
- **Why important**: 跟踪当前任务进度与测试记录。
- **Changes made**: 记录崩溃原因、测试失败与版本更新状态。

### gradle.properties / config/version-history.json
- **Why important**: 每次编译/安装必须更新版本号。
- **Changes made**: 版本已更新到 1.7.0 / 10700，并记录到版本历史。

## 4. Problem Solving
- **问题 1：授权 Intent 无法落盘**
  - 日志显示 `Parcel.marshall` 失败，因 Intent 内含 Binder/FD。
  - 处理：不再序列化，改为进程内缓存。
- **问题 2：OPPO 真机崩溃**
  - 日志：`SecurityException: Media projections require a foreground service of type ...`
  - 根因：在 `ScreenshotPermissionActivity` 中直接 `getMediaProjection`。
  - 修复：移除 Activity 里的调用，改由 Service 更新 FGS 类型后再调用。

## 5. Pending Tasks
- 在 OPPO 真机验证 1.7.0：
  1) 设置页授权后开关是否开启。
  2) 悬浮窗截图是否出现黑色遮罩并生成缩略图。
- 如仍失败：抓取 `adb logcat -d -s ScreenshotPermission FloatingWindowService FloatingWindowPrefs`。
- 确认未跟踪文件 `文档/开发文档/MA/MANAGE/MANAGE-20260115-worktree-manager.md` 是否需要纳入提交。

## 6. Current Work
- 已读取崩溃日志，定位 `ScreenshotPermissionActivity` 中 `getMediaProjection` 触发 SecurityException。
- 已修复并重新构建/安装到 OPPO（versionName=1.7.0 / versionCode=10700）。
- 最新改动已提交：移除崩溃调用并更新版本。
- 正等待用户复测截图流程。

## 7. Optional Next Step
- 让用户完成真机复测；若仍失败，继续抓日志并调整截图权限缓存或前台服务流程。
</plan>
