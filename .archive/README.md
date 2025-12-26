# 归档目录 (Archive)

> 📦 存放不再使用的旧文件和配置，按归档时间组织

## 目录结构

```
.archive/
├── logs/          # 历史日志文件
├── old-tools/     # 过时的AI工具配置
├── temp/          # 临时文件
└── README.md      # 本文件
```

## 归档记录

### 2025-12-25 - 根目录清理

#### 归档的日志文件 (logs/)
- `build_log.txt` - Gradle构建日志
- `compile_log.txt` - Kotlin编译日志
- `full_log.txt` - 完整日志
- `main_compile.txt` - 主模块编译日志
- `plain_log.txt` - 纯文本日志
- `test_log.txt` - 测试日志
- `hs_err_pid144168.log` - JVM错误日志 (144KB)
- `replay_pid144168.log` - JVM重放日志 (2.1MB)

**总计**: 8个文件，约2.3MB

#### 归档的旧工具配置 (old-tools/)
- `.gemini/` - Gemini AI工具配置
- `.kilocode/` - Kilocode工具配置
- `.specify/` - Specify工具配置
- `.Roo/` - Roo代码审查工具配置（已被.claude取代）

**说明**: 这些工具已被当前使用的Claude Code工具链替代

#### 归档的临时文件 (temp/)
- `HTML/` - 临时导出的HTML文件

## 清理原因

1. **日志文件**: 占用根目录空间，影响项目结构清晰度
2. **旧工具配置**: 已替换为当前工具链，保留配置会造成混淆
3. **临时文件**: 非项目核心文件，应该归档或删除

## 恢复方法

如果需要恢复某个文件，可以使用以下命令：

```bash
# 恢复日志文件
cp .archive/logs/<filename> .

# 恢复工具配置
cp -r .archive/old-tools/<toolname> .

# 恢复临时文件
cp -r .archive/temp/<dirname> .
```

## 定期清理

建议每季度检查一次归档目录，删除超过6个月的旧日志文件。

---

**归档时间**: 2025-12-25
**归档人**: Claude AI Assistant
**归档版本**: v1.0
