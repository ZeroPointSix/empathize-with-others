@echo ==========================================
@echo AGP 版本兼容性检查工具
@echo ==========================================

echo 检查当前构建环境...
echo.

echo [1/4] 检查 Gradle 版本...
call gradlew --version

echo.
echo [2/4] 检查 AGP 版本和依赖...
call gradlew buildEnvironment --configuration=compileClasspath

echo.
echo [3/4] 检查废弃配置警告...
call gradlew help --warning-mode all

echo.
echo [4/4] 验证构建配置...
call gradlew projects

echo.
echo ==========================================
echo 兼容性检查完成！
echo ==========================================
echo 如果看到 android.enableBuildCache 相关警告，请参考：
echo https://developer.android.com/studio/releases/gradle-plugin
echo.

pause