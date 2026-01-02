# 版本号和图标自动更新指南

> 本文档介绍如何使用版本号和图标自动更新系统。

## 目录

- [快速开始](#快速开始)
- [命令行参数](#命令行参数)
- [发布阶段](#发布阶段)
- [Gradle任务](#gradle任务)
- [CI/CD集成](#cicd集成)
- [配置文件](#配置文件)
- [故障排除](#故障排除)

---

## 快速开始

### 1. 预览版本变更

在执行实际更新前，建议先预览将要发生的变更：

```bash
# Windows
.\gradlew updateVersionAndIcon --dry-run

# Linux/Mac
./gradlew updateVersionAndIcon --dry-run
```

### 2. 执行版本更新

```bash
# 使用默认阶段(dev)更新
.\gradlew updateVersionAndIcon

# 指定发布阶段
.\gradlew updateVersionAndIcon --stage=beta
```

### 3. 使用发布脚本（推荐）

```bash
# Windows
scripts\release.bat --stage=beta

# Linux/Mac
./scripts/release.sh --stage=beta
```

---

## 命令行参数

### 主任务参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `--stage=<阶段>` | 发布阶段 | `dev` |
| `--dry-run` | 预览模式，不实际执行更新 | `false` |
| `--force` | 强制更新，忽略未提交的更改 | `false` |

### 示例

```bash
# 预览beta版本更新
.\gradlew updateVersionAndIcon --stage=beta --dry-run

# 强制更新到production
.\gradlew updateVersionAndIcon --stage=production --force

# 仅更新版本号
.\gradlew updateVersion --stage=dev

# 仅更新图标
.\gradlew updateIcon --stage=beta
```

---

## 发布阶段

系统支持4个发布阶段，每个阶段有不同的图标和标识：

| 阶段 | 说明 | 图标后缀 | 角标文字 |
|------|------|----------|----------|
| `dev` | 开发版 | 🔧 | DEV |
| `test` | 测试版 | 🧪 | TEST |
| `beta` | 预发布版 | 🚀 | BETA |
| `production` | 正式版 | ✨ | (无) |

### 图标资源目录

```
assets/icons/
├── dev/           # 开发版图标
├── test/          # 测试版图标
├── beta/          # 预发布版图标
└── production/    # 正式版图标
```

---

## Gradle任务

### 可用任务列表

| 任务名 | 说明 |
|--------|------|
| `updateVersionAndIcon` | 主任务：更新版本号和图标 |
| `updateVersion` | 仅更新版本号 |
| `updateIcon` | 仅更新图标 |
| `analyzeCommits` | 分析Git提交，预览版本变更 |
| `rollbackVersion` | 回滚到上一个版本 |
| `showCurrentVersion` | 显示当前版本信息 |
| `initIconResources` | 初始化图标资源目录 |

### 任务详情

#### updateVersionAndIcon

主任务，执行完整的版本更新流程：

1. 分析Git提交
2. 计算新版本号
3. 创建备份
4. 更新版本号
5. 更新图标
6. 更新版本历史

```bash
.\gradlew updateVersionAndIcon --stage=beta
```

#### analyzeCommits

分析自上次标签以来的所有提交，预览版本变更：

```bash
.\gradlew analyzeCommits
```

输出示例：
```
📊 分析Git提交...
   当前版本: 1.0.0
   找到 15 个新提交

📋 提交分类:
   ✨ 新功能: 5 个
   🐛 Bug修复: 8 个
   📝 文档更新: 2 个

📈 版本变更预测:
   当前版本: 1.0.0
   预测版本: 1.1.0
```

#### rollbackVersion

回滚到上一个备份的版本：

```bash
.\gradlew rollbackVersion
```

#### showCurrentVersion

显示当前版本信息：

```bash
.\gradlew showCurrentVersion
```

输出示例：
```
📋 当前版本信息
═══════════════════════════════════════
   版本号: 1.1.0
   版本代码: 10100
   发布阶段: beta
   图标阶段: beta

📚 最近版本历史 (最近5条)
───────────────────────────────────────
   1.1.0 (beta) - 2025-01-01T10:00:00Z
   1.0.0 (production) - 2024-12-25T12:00:00Z
```

---

## CI/CD集成

### GitHub Actions

项目已配置GitHub Actions工作流，位于 `.github/workflows/version-update.yml`。

#### 自动触发

推送到`main`分支时自动执行版本更新（排除文档更改）。

#### 手动触发

1. 进入GitHub仓库的Actions页面
2. 选择"Version Update"工作流
3. 点击"Run workflow"
4. 选择发布阶段和其他选项
5. 点击"Run workflow"执行

#### 工作流参数

| 参数 | 说明 | 选项 |
|------|------|------|
| stage | 发布阶段 | dev, test, beta, production |
| dry_run | 预览模式 | true/false |
| force | 强制更新 | true/false |

### 本地CI模拟

使用发布脚本模拟CI流程：

```bash
# Windows
scripts\release.bat --stage=beta

# Linux/Mac
./scripts/release.sh --stage=beta
```

---

## 配置文件

### gradle.properties

版本号定义在 `gradle.properties` 中：

```properties
# 应用版本
APP_VERSION_NAME=1.0.0
APP_VERSION_CODE=10000
APP_RELEASE_STAGE=dev
```

### config/icon-mapping.json

图标映射配置：

```json
{
  "version": 1,
  "defaultStage": "production",
  "iconSets": {
    "dev": {
      "sourceDir": "assets/icons/dev",
      "files": ["ic_launcher.png", "ic_launcher_round.png", "ic_launcher_foreground.png"]
    },
    "test": { ... },
    "beta": { ... },
    "production": { ... }
  }
}
```

### config/version-history.json

版本历史记录（自动生成）：

```json
{
  "schemaVersion": 1,
  "currentVersion": {
    "version": "1.1.0",
    "versionCode": 10100,
    "stage": "beta",
    "updatedAt": "2025-01-01T10:00:00Z"
  },
  "history": [...]
}
```

---

## 故障排除

### 常见问题

#### 1. "没有新的提交，跳过版本更新"

**原因**: 自上次标签以来没有新的提交。

**解决方案**:
- 确保有新的提交
- 使用 `--force` 参数强制更新

#### 2. "gradle.properties not found"

**原因**: 项目根目录缺少 `gradle.properties` 文件。

**解决方案**:
创建 `gradle.properties` 并添加版本属性：
```properties
APP_VERSION_NAME=1.0.0
APP_VERSION_CODE=10000
APP_RELEASE_STAGE=dev
```

#### 3. "源图标目录不存在"

**原因**: 图标资源目录未创建或路径错误。

**解决方案**:
```bash
# 初始化图标资源目录
.\gradlew initIconResources
```

#### 4. "存在未提交的更改"

**原因**: 工作目录有未提交的更改。

**解决方案**:
- 提交或暂存更改
- 使用 `--force` 参数强制执行

#### 5. Git命令执行失败

**原因**: Git未安装或不在PATH中。

**解决方案**:
- 确保Git已安装
- 确保Git在系统PATH中
- 在Git仓库目录中执行命令

### 回滚操作

如果版本更新出现问题，可以回滚到上一个版本：

```bash
.\gradlew rollbackVersion
```

### 查看备份

备份文件存储在 `backups/version-update/` 目录中：

```
backups/version-update/
├── backup-2025-01-01-10-00-00/
│   ├── gradle.properties
│   ├── config/
│   └── metadata.json
└── icons-1704067200000/
    ├── mipmap-mdpi/
    ├── mipmap-hdpi/
    └── ...
```

### 日志和调试

启用详细日志：

```bash
.\gradlew updateVersionAndIcon --info
```

启用调试日志：

```bash
.\gradlew updateVersionAndIcon --debug
```

---

## 版本号规则

### 语义化版本

版本号遵循 [语义化版本 2.0.0](https://semver.org/lang/zh-CN/) 规范：

```
MAJOR.MINOR.PATCH
```

- **MAJOR**: 不兼容的API修改
- **MINOR**: 向下兼容的功能性新增
- **PATCH**: 向下兼容的问题修正

### 提交类型与版本变更

| 提交类型 | 版本变更 | 示例 |
|----------|----------|------|
| `feat!` / `fix!` | MAJOR | `feat!: 重构API` |
| `feat` | MINOR | `feat: 添加新功能` |
| `fix` / `perf` | PATCH | `fix: 修复bug` |
| `docs` / `style` / `refactor` / `test` / `chore` | 无变更 | `docs: 更新文档` |

### 提交消息格式

遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

示例：
```
feat(auth): 添加用户登录功能

- 实现用户名密码登录
- 添加记住密码功能
- 集成OAuth2.0

Closes #123
```

---

## 相关文档

- [TDD-00024 技术设计文档](../文档/开发文档/TDD/TDD-00024-图标和版本号自动更新技术设计.md)
- [TD-00024 任务清单](../文档/开发文档/TD/TD-00024-图标和版本号自动更新任务清单.md)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [语义化版本](https://semver.org/lang/zh-CN/)

---

**文档版本**: 1.0  
**最后更新**: 2025-01-01  
**作者**: Kiro
