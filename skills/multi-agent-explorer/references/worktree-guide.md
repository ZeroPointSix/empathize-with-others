# Git Worktree 快速指南

> 本文档提供 Git Worktree 的快速使用指南，帮助用户管理多个探索工作树。

## 什么是 Git Worktree？

Git Worktree 允许你在同一个仓库中创建多个工作目录，每个目录可以检出不同的分支。这对于并行开发非常有用。

## 基本命令

### 列出所有工作树

```bash
git worktree list
```

输出示例：
```
/path/to/main-repo          abc1234 [main]
/path/to/explore-bugfix     def5678 [explore/bugfix-20241230]
/path/to/explore-feature    ghi9012 [explore/feature-20241230]
```

### 创建新工作树

```bash
# 创建工作树并新建分支
git worktree add <path> -b <branch-name>

# 示例：创建 Bug 修复探索工作树
git worktree add ../explore-bugfix-20241230 -b explore/bugfix-20241230

# 示例：创建功能开发探索工作树
git worktree add ../explore-feature-20241230 -b explore/feature-20241230
```

### 删除工作树

```bash
# 删除工作树（保留分支）
git worktree remove <path>

# 示例
git worktree remove ../explore-bugfix-20241230

# 强制删除（如果有未提交的更改）
git worktree remove --force ../explore-bugfix-20241230
```

### 清理过期工作树

```bash
# 清理已删除目录的工作树记录
git worktree prune
```

## 探索工作树命名规范

### 分支命名

```
explore/<type>-<date>[-<description>]
```

示例：
- `explore/bugfix-20241230`
- `explore/feature-20241230-user-profile`
- `explore/free-20241230`
- `explore/test-20241230`

### 目录命名

```
../explore-<type>-<date>[-<description>]
```

示例：
- `../explore-bugfix-20241230`
- `../explore-feature-20241230-user-profile`
- `../explore-free-20241230`
- `../explore-test-20241230`

## 快速创建脚本

### Windows (PowerShell)

```powershell
# 创建 Bug 修复探索工作树
$date = Get-Date -Format "yyyyMMdd"
git worktree add "../explore-bugfix-$date" -b "explore/bugfix-$date"
cd "../explore-bugfix-$date"

# 创建功能开发探索工作树
$date = Get-Date -Format "yyyyMMdd"
git worktree add "../explore-feature-$date" -b "explore/feature-$date"
cd "../explore-feature-$date"

# 创建自由探索工作树
$date = Get-Date -Format "yyyyMMdd"
git worktree add "../explore-free-$date" -b "explore/free-$date"
cd "../explore-free-$date"

# 创建测试探索工作树
$date = Get-Date -Format "yyyyMMdd"
git worktree add "../explore-test-$date" -b "explore/test-$date"
cd "../explore-test-$date"
```

### Linux/macOS (Bash)

```bash
# 创建 Bug 修复探索工作树
date=$(date +%Y%m%d)
git worktree add "../explore-bugfix-$date" -b "explore/bugfix-$date"
cd "../explore-bugfix-$date"

# 创建功能开发探索工作树
date=$(date +%Y%m%d)
git worktree add "../explore-feature-$date" -b "explore/feature-$date"
cd "../explore-feature-$date"

# 创建自由探索工作树
date=$(date +%Y%m%d)
git worktree add "../explore-free-$date" -b "explore/free-$date"
cd "../explore-free-$date"

# 创建测试探索工作树
date=$(date +%Y%m%d)
git worktree add "../explore-test-$date" -b "explore/test-$date"
cd "../explore-test-$date"
```

## 工作流程

### 1. 创建探索工作树

```bash
# 在主仓库目录中
git worktree add ../explore-bugfix-20241230 -b explore/bugfix-20241230
```

### 2. 进入工作树

```bash
cd ../explore-bugfix-20241230
```

### 3. 启动智能体

在新终端中启动 Claude Code，使用对应的 slash 命令：
- `/explore-bugfix` - Bug 修复探索
- `/explore-feature` - 功能开发探索
- `/explore-free` - 自由探索
- `/explore-arch` - 架构审查
- `/explore-test` - 测试扩展
- `/explore-manage` - 工作树管理

### 4. 完成后处理

#### 如果要合并

```bash
# 回到主仓库
cd ../main-repo

# 合并探索分支
git merge explore/bugfix-20241230

# 删除工作树
git worktree remove ../explore-bugfix-20241230

# 删除分支（可选）
git branch -d explore/bugfix-20241230
```

#### 如果不合并

```bash
# 直接删除工作树
git worktree remove ../explore-bugfix-20241230

# 删除分支
git branch -D explore/bugfix-20241230
```

## 注意事项

### 1. 工作树位置

- 工作树目录应该在主仓库的同级目录
- 不要在主仓库内部创建工作树

```
parent-directory/
├── main-repo/              # 主仓库
├── explore-bugfix-20241230/  # 探索工作树
├── explore-feature-20241230/ # 探索工作树
└── explore-free-20241230/    # 探索工作树
```

### 2. 分支限制

- 每个分支只能在一个工作树中检出
- 不能在工作树中检出主仓库当前的分支

### 3. 资源管理

- 每个工作树占用独立的磁盘空间
- 定期清理不需要的工作树
- 使用 `git worktree prune` 清理过期记录

### 4. IDE 支持

- 每个工作树可以用独立的 IDE 窗口打开
- 建议为每个工作树使用独立的终端

## 常见问题

### Q: 工作树和分支的关系？

A: 工作树是一个独立的工作目录，检出特定的分支。删除工作树不会删除分支，删除分支也不会删除工作树目录。

### Q: 如何在工作树之间共享更改？

A: 在一个工作树中提交更改后，可以在另一个工作树中通过 `git fetch` 和 `git merge` 获取更改。

### Q: 工作树中的 .git 是什么？

A: 工作树中的 `.git` 是一个文件（不是目录），指向主仓库的 `.git` 目录。

### Q: 如何查看工作树的状态？

A: 使用 `git worktree list` 查看所有工作树，使用 `git status` 在特定工作树中查看状态。

