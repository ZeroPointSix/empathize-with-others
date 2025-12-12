# ADB (Android Debug Bridge) 安装与使用说明

## 安装位置

- **ADB 安装路径**: `C:\Android\platform-tools\`
- **版本**: Android Debug Bridge version 1.0.41

## 环境变量配置

已配置以下环境变量：
- `ANDROID_HOME`: `C:\Android`
- `PATH`: 已添加 `C:\Android\platform-tools`

## 使用方法

### 1. 在命令提示符 (CMD) 或 PowerShell 中使用

```bash
# 直接使用（已配置环境变量，重新打开终端后生效）
adb connect 127.0.0.1:16384

# 查看版本
adb version

# 查看连接的设备
adb devices
```

### 2. 在 Git Bash 中使用

```bash
# 使用完整路径
"C:\Android\platform-tools\adb.exe" connect 127.0.0.1:16384

# 或使用自定义脚本（已在 ~/bin/adb 创建）
~/bin/adb connect 127.0.0.1:16384
```

### 3. 在当前项目目录中使用

```bash
# 执行连接命令（已验证成功）
"C:\Android\platform-tools\adb.exe" connect 127.0.0.1:16384
```

## 常用 ADB 命令

```bash
# 连接设备
adb connect 127.0.0.1:16384

# 查看已连接的设备
adb devices

# 安装 APK
adb install path/to/app.apk

# 卸载应用
adb uninstall package.name

# 查看日志
adb logcat

# 进入 shell 模式
adb shell

# 传输文件
adb push local_path remote_path
adb pull remote_path local_path
```

## 注意事项

1. **首次使用**: 环境变量已设置，但需要重新打开新的命令行窗口才能生效
2. **权限问题**: 如果遇到权限问题，请以管理员身份运行命令行
3. **防火墙**: 确保防火墙允许 ADB 通过端口 5037 和 16384
4. **安卓模拟器**: 确保模拟器已开启并启用了 ADB 调试

## 故障排除

如果 `adb` 命令无法识别：

1. **重启命令行**: 关闭当前命令行窗口，重新打开
2. **使用完整路径**: `"C:\Android\platform-tools\adb.exe"`
3. **检查环境变量**:
   ```bash
   echo $ANDROID_HOME
   echo $PATH
   ```

---
**安装日期**: 2025-12-07
**版本**: v1.0
**状态**: ✅ 安装完成，连接测试成功