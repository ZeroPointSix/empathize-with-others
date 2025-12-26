---
name: git-operations
description: Git 操作 - 提交规范、分支管理、代码合并、冲突解决。在日常开发中进行版本控制时使用。
---

# Git 操作

## 激活时机

当满足以下条件时自动激活此技能：
- 提交代码变更
- 创建/合并分支
- 解决代码冲突
- 回滚代码版本
- 查看提交历史
- 发布版本

## 提交规范 (Conventional Commits)

### 提交消息格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型

| Type | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | feat: 添加用户认证 |
| `fix` | Bug 修复 | fix: 修复登录超时问题 |
| `docs` | 文档变更 | docs: 更新 API 文档 |
| `style` | 代码格式 | style: 统一缩进为 2 空格 |
| `refactor` | 重构 | refactor: 提取用户服务类 |
| `perf` | 性能优化 | perf: 添加查询缓存 |
| `test` | 测试相关 | test: 添加登录测试 |
| `chore` | 构建/工具 | chore: 更新依赖版本 |
| `revert` | 回滚提交 | revert: 回滚提交 abc123 |

### 提交示例

```bash
# 简单提交
git commit -m "feat: 添加用户注册功能"

# 带详细说明
git commit -m "fix: 修复支付超时问题

- 调整超时时间从 30s 到 60s
- 添加超时重试机制
- 优化错误提示信息

Closes #123"

# 破坏性变更
git commit -m "feat!: 移除旧版 API

BREAKING CHANGE: 不再支持 v1 API，请迁移到 v2"
```

### Scope 范围

常用 scope：
```
- auth: 认证授权
- user: 用户管理
- payment: 支付相关
- database: 数据库
- api: API 接口
- ui: 界面相关
- config: 配置文件
```

## 分支管理

### 分支命名规范

```
feature/功能名称
bugfix/问题描述
hotfix/紧急修复
release/版本号
docs/文档更新
refactor/重构内容
```

### Git Flow 工作流

```
main (生产环境)
  ↑
  ├── release/1.0.0 ←── release 分支
  │
  ├── develop (开发环境)
  │   ↑
  │   ├── feature/user-auth ←── 功能分支
  │   ├── feature/payment
  │   └── bugfix/login-error ←── 修复分支
  │
  └── hotfix/security-patch ←── 紧急修复
```

### 分支操作

```bash
# 创建功能分支
git checkout develop
git pull origin develop
git checkout -b feature/user-auth

# 完成功能后合并
git checkout develop
git merge --no-ff feature/user-auth
git branch -d feature/user-auth

# 创建发布分支
git checkout develop
git checkout -b release/1.0.0

# 合并到 main 和 develop
git checkout main
git merge --no-ff release/1.0.0
git checkout develop
git merge --no-ff release/1.0.0

# 紧急修复
git checkout main
git checkout -b hotfix/security-patch
# ... 修复 ...
git checkout main
git merge --no-ff hotfix/security-patch
git checkout develop
git merge --no-ff hotfix/security-patch
```

## 代码合并

### 合并策略

| 策略 | 说明 | 使用场景 |
|------|------|---------|
| `--no-ff` | 保留分支历史 | 功能分支合并 |
| `--squash` | 压缩为单个提交 | 临时/实验分支 |
| `rebase` | 线性历史 | 个人分支同步 |
| `merge` | 标准合并 | 保持完整历史 |

### Merge 操作

```bash
# 标准合并（保留分支历史）
git checkout main
git merge --no-ff feature/new-feature

# 压缩合并（多个提交压缩为一个）
git checkout main
git merge --squash feature/new-feature
git commit -m "feat: 完成新功能"

# 快进合并（线性历史）
git checkout main
git merge feature/new-feature  # 默认 ff
```

### Rebase 操作

```bash
# 将当前分支变基到 main
git checkout feature
git rebase main

# 交互式变基（整理提交）
git rebase -i HEAD~3

# 变基时保持连续
git rebase --onto main upstream/feature
```

## 冲突解决

### 冲突产生

```
当两个分支修改同一文件的同一行时，Git 无法自动合并
```

### 冲突标记

```
<<<<<<< HEAD
当前分支的代码
=======
要合并分支的代码
>>>>>>> feature-branch
```

### 解决流程

```bash
# 1. 执行合并
git merge feature-branch

# 2. 查看冲突文件
git status

# 3. 手动解决冲突
# 编辑冲突文件，选择保留的代码

# 4. 标记为已解决
git add conflicted-file.txt

# 5. 完成合并
git commit

# 6. 如果需要中止合并
git merge --abort
```

### 冲突解决策略

```bash
# 使用当前分支的代码
git checkout --ours filename.txt

# 使用要合并分支的代码
git checkout --theirs filename.txt

# 使用合并工具
git mergetool
```

## 历史操作

### 查看历史

```bash
# 简洁历史
git log --oneline

# 图形化历史
git log --graph --oneline --all

# 查看某文件历史
git log --follow filename.txt

# 查看提交内容
git show abc123

# 查看分支历史
git log main..feature  # feature 有但 main 没有的
```

### 回滚操作

```bash
# 撤销最后一次提交（保留更改）
git reset --soft HEAD~1

# 撤销最后一次提交（丢弃更改）
git reset --hard HEAD~1

# 撤销多个提交
git reset --hard HEAD~3

# 回滚到指定提交（保留后续更改）
git revert abc123

# 撤销已推送的提交
git revert HEAD
```

### 修改历史

```bash
# 修改最后一次提交消息
git commit --amend

# 修改最后一次提交内容
git add forgotten-file.txt
git commit --amend

# 交互式修改多个提交
git rebase -i HEAD~3

# 命令说明：
# pick: 保留该提交
# reword: 修改提交消息
# edit: 修改提交内容
# squash: 合并到前一个提交
# drop: 删除该提交
```

## 远程操作

### 远程仓库

```bash
# 查看远程仓库
git remote -v

# 添加远程仓库
git remote add origin https://github.com/user/repo.git

# 删除远程仓库
git remote remove origin

# 更改远程仓库 URL
git remote set-url origin https://github.com/user/new-repo.git
```

### 推送拉取

```bash
# 推送分支
git push origin main

# 推送所有分支
git push --all origin

# 推送标签
git push --tags

# 设置上游分支
git push -u origin feature

# 拉取并合并
git pull origin main

# 拉取并变基
git pull --rebase origin main

# 获取但不合并
git fetch origin
```

## 常用场景

### 日常开发流程

```bash
# 1. 同步最新代码
git checkout develop
git pull origin develop

# 2. 创建功能分支
git checkout -b feature/new-feature

# 3. 开发并提交
git add .
git commit -m "feat: 添加核心功能"

# 4. 同步上游变更
git fetch origin develop
git rebase origin/develop

# 5. 推送分支
git push -u origin feature/new-feature

# 6. 创建 Pull Request
# 在 GitHub/GitLab 上操作

# 7. 合并后清理分支
git checkout develop
git pull origin develop
git branch -d feature/new-feature
```

### 紧急修复流程

```bash
# 1. 从 main 创建修复分支
git checkout main
git pull origin main
git checkout -b hotfix/critical-bug

# 2. 修复并提交
git add .
git commit -m "fix: 修复关键安全漏洞"

# 3. 合并到 main
git checkout main
git merge --no-ff hotfix/critical-bug
git tag -a v1.0.1 -m "Hotfix: 修复安全漏洞"
git push origin main --tags

# 4. 合并回 develop
git checkout develop
git merge --no-ff hotfix/critical-bug
git push origin develop

# 5. 删除修复分支
git branch -d hotfix/critical-bug
```

## 最佳实践

### 提交频率
```
✅ 小步提交，频繁提交
❌ 大量改动一次提交
```

### 提交粒度
```
✅ 一个提交做一件事
❌ 一个提交包含多个无关改动
```

### 分支策略
```
✅ 明确的分支命名
✅ 及时删除已合并分支
❌ 分支命名混乱
❌ 积累大量无用分支
```

### 代码推送
```
✅ 推送前拉取最新代码
✅ 解决冲突后再推送
❌ 直接强制推送（除非确定安全）
```

## 相关资源

- `resources/git-workflows.md` - Git 工作流详解
- `resources/conflict-resolution.md` - 冲突解决案例
- `resources/advanced-git.md` - 高级 Git 技巧

---

**技能状态**: 完成 ✅
**推荐工作流**: Git Flow / GitHub Flow
**提交规范**: Conventional Commits
