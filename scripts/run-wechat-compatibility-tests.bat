@echo off
REM 微信兼容性测试执行脚本
REM 
REM 用途：自动化执行微信兼容性相关的测试
REM 
REM 使用方法：
REM   1. 确保设备已连接并授予必要权限
REM   2. 运行此脚本：scripts\run-wechat-compatibility-tests.bat
REM   3. 查看测试结果

echo ========================================
echo 微信兼容性测试执行脚本
echo ========================================
echo.

REM 1. 运行单元测试
echo [1/5] 运行 WeChatDetector 单元测试...
call gradlew :app:testDebugUnitTest --tests "*WeChatDetectorTest"
if %ERRORLEVEL% NEQ 0 (
    echo 错误: WeChatDetector 测试失败
    exit /b 1
)
echo ✓ WeChatDetector 测试通过
echo.

REM 2. 构建 APK
echo [2/5] 构建 Debug APK...
call gradlew assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo 错误: APK 构建失败
    exit /b 1
)
echo ✓ APK 构建成功
echo.

REM 3. 检查设备连接
echo [3/5] 检查设备连接...
adb devices
if %ERRORLEVEL% NEQ 0 (
    echo 错误: 未检测到设备，请连接 Android 设备
    exit /b 1
)
echo ✓ 设备已连接
echo.

REM 4. 安装 APK
echo [4/5] 安装 APK 到设备...
call gradlew installDebug
if %ERRORLEVEL% NEQ 0 (
    echo 错误: APK 安装失败
    exit /b 1
)
echo ✓ APK 安装成功
echo.

REM 5. 显示测试指南
echo [5/5] 准备手动测试...
echo.
echo ========================================
echo 手动测试步骤
echo ========================================
echo.
echo 请按照以下步骤进行手动测试：
echo.
echo 1. 打开共情 AI 助手应用
echo 2. 进入"设置"页面
echo 3. 配置 AI 服务（输入 API Key）
echo 4. 创建一个测试联系人
echo 5. 启用悬浮窗功能
echo 6. 打开微信应用
echo 7. 执行测试用例（参考 docs/03-测试文档/微信兼容性测试指南.md）
echo.
echo 详细测试指南：docs\03-测试文档\微信兼容性测试指南.md
echo.
echo ========================================
echo 测试准备完成！
echo ========================================

pause
