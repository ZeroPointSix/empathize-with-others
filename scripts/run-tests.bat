@echo off
echo ========================================
echo 运行项目测试套件
echo ========================================
echo.

echo [1/3] 清理构建缓存...
call gradlew clean

echo.
echo [2/3] 运行单元测试...
call gradlew test --continue

echo.
echo [3/3] 生成测试报告...
echo 测试报告位置: app\build\reports\tests\testDebugUnitTest\index.html

echo.
echo ========================================
echo 测试完成！
echo ========================================
pause
