# Skills 通用开发技能集

> 一套适用于任何项目的 Claude Code 技能集合，涵盖代码审查、架构设计、测试、调试、编码规范、重构、Git 操作、文档编写、错误跟踪、数据库操作、API 设计、前端/后端开发、中间件、性能优化、安全、头脑风暴、计划编写和完成前验证、深度代码分析、插件开发、文档处理等 42 个技能。

---

## 📊 技能概览

```
总计: 42 个技能
版本: 2.0
```

### 按类别统计

| 类别 | 数量 | 技能 |
|------|------|------|
| 📊 **代码分析** | 9个 | controller, architecture-analyzer, dependency-tracer, pattern-detector, quality-analyzer, question-refiner, research-executor, synthesizer, move-code-quality |
| 🔌 **插件开发** | 6个 | agent-identifier, hook-development, mcp-integration, rule-identifier, skill-development |
| 🔍 **代码审查** | 4个 | code-review, clojure-review, docs-review, reviewing-changes |
| 🛠️ **后端开发** | 4个 | backend-development, database-operations, api-design, middleware |
| 🧪 **测试** | 3个 | test-driven-development, verification, webapp-testing |
| 🐛 **调试** | 2个 | debugging-strategies, claude-code-skill-diagnostic |
| 🎨 **前端** | 2个 | frontend-development, frontend-design |
| 🤖 **Android** | 2个 | jetpack-compose, kotlin-java-standards |
| 📄 **文档处理** | 2个 | pdf, xlsx |
| 🏗️ **架构设计** | 2个 | architecture-design, brainstorming |
| 📋 **其他** | 8个 | coding-standards, refactoring, git-operations, documentation, error-tracking, performance-optimization, security, planning |

---

## 📁 完整目录结构

```
skills/
├── skill-rules.json              # 技能配置文件
├── README.md                     # 本文件
│
├── 📊 代码分析类 (9个)
│   ├── code-analysis-controller/     # GoT 图分析控制器
│   ├── code-architecture-analyzer/   # 代码架构分析器
│   ├── code-dependency-tracer/       # 代码依赖追踪器
│   ├── code-pattern-detector/        # 代码模式检测器
│   ├── code-quality-analyzer/        # 代码质量分析器
│   ├── code-question-refiner/        # 代码问题细化器
│   ├── code-research-executor/       # 代码研究执行器
│   ├── code-synthesizer/             # 代码综合分析器
│   └── move-code-quality/            # Move 代码质量检查器
│
├── 🔌 插件开发类 (6个)
│   ├── agent-identifier/             # Agent 开发
│   ├── hook-development/             # Hook 开发
│   ├── mcp-integration/              # MCP 集成
│   ├── rule-identifier/              # Hookify 规则编写
│   └── skill-development/            # 技能开发
│
├── 🔍 代码审查类 (4个)
│   ├── code-review/                  # 通用代码审查
│   ├── clojure-review/               # Clojure 代码审查
│   ├── docs-review/                  # 文档审查
│   └── reviewing-changes/            # 变更审查 (Bitwarden Android)
│
├── 🛠️ 后端开发类 (4个)
│   ├── backend-development/          # 后端开发
│   ├── database-operations/          # 数据库操作
│   ├── api-design/                   # API 设计
│   └── middleware/                   # 中间件开发
│
├── 🧪 测试类 (3个)
│   ├── test-driven-development/      # 测试驱动开发
│   ├── verification/                 # 完成前验证
│   └── webapp-testing/               # Web 应用测试
│
├── 🐛 调试类 (2个)
│   ├── debugging-strategies/         # 调试策略
│   └── claude-code-skill-diagnostic/ # Claude Code 技能诊断
│
├── 🎨 前端类 (2个)
│   ├── frontend-development/         # 前端开发
│   └── frontend-design/              # 前端设计
│
├── 🤖 Android 类 (2个)
│   ├── jetpack-compose/              # Jetpack Compose
│   └── kotlin-java-standards/        # Kotlin/Java 编码规范
│
├── 📄 文档处理类 (2个)
│   ├── pdf/                          # PDF 处理
│   └── xlsx/                         # Excel 处理
│
├── 🏗️ 架构设计类 (2个)
│   ├── architecture-design/          # 架构设计
│   └── brainstorming/                # 头脑风暴
│
└── 📋 其他技能 (8个)
    ├── coding-standards/             # 编码规范
    ├── refactoring/                  # 代码重构
    ├── git-operations/               # Git 操作
    ├── documentation/                # 文档编写
    ├── error-tracking/               # 错误跟踪
    ├── performance-optimization/     # 性能优化
    ├── security/                     # 安全最佳实践
    └── planning/                     # 计划编写
```

---

## 🎯 核心技能详解

### 📊 代码分析类 (9个)

| 技能 | 用途 |
|------|------|
| **code-analysis-controller** | GoT 图分析控制器，管理代码分析状态和图操作 (Generate, Aggregate, Refine, Score) |
| **code-architecture-analyzer** | 识别设计模式、评估模块耦合、分析分层结构、检测架构违规 |
| **code-dependency-tracer** | 追踪第三方库、内部依赖、循环依赖、版本兼容性 |
| **code-pattern-detector** | 识别设计模式使用、检测反模式、分析错误处理模式 |
| **code-quality-analyzer** | 检测代码复杂度、可维护性、代码异味、命名规范 |
| **code-question-refiner** | 将原始代码分析问题细化为结构化的深度研究任务 |
| **code-research-executor** | 执行完整的 7 阶段代码深度分析流程 |
| **code-synthesizer** | 将多个分析智能体的发现综合成连贯的结构化报告 |
| **move-code-quality** | Move 语言代码质量检查器，检查 Move 2024 Edition 合规性 |

### 🔌 插件开发类 (6个)

| 技能 | 用途 |
|------|------|
| **agent-identifier** | 创建 Claude Code 插件的 Agent，指导结构、系统提示词设计 |
| **hook-development** | 为插件创建和实现 hooks，包括验证、策略、上下文集成 |
| **mcp-integration** | 集成 Model Context Protocol 服务器，实现外部工具和服务集成 |
| **rule-identifier** | 创建 hookify 规则，配置规则语法与模式 |
| **skill-development** | 为插件创建有效技能，指导结构和最佳实践 |

### 🔍 代码审查类 (4个)

| 技能 | 用途 |
|------|------|
| **code-review** | 通用代码审查，审查代码质量、架构、安全性 |
| **clojure-review** | Clojure/ClojureScript 代码审查 (Metabase 标准) |
| **docs-review** | 文档审查，检查文档变更是否符合风格指南 |
| **reviewing-changes** | Bitwarden Android 特定的代码审查工作流 |

### 🛠️ 后端开发类 (4个)

| 技能 | 用途 |
|------|------|
| **backend-development** | Node.js/Express、分层架构、异步处理 |
| **database-operations** | Prisma ORM、查询优化、事务处理 |
| **api-design** | RESTful 规范、接口设计、版本管理 |
| **middleware** | 认证、验证、日志中间件 |

### 🧪 测试类 (3个)

| 技能 | 用途 |
|------|------|
| **test-driven-development** | TDD 实践、测试策略、覆盖率分析 |
| **verification** | 功能验收、回归测试、部署检查 |
| **webapp-testing** | 使用 Playwright 测试本地 web 应用 |

### 🐛 调试类 (2个)

| 技能 | 用途 |
|------|------|
| **debugging-strategies** | 系统化调试技术、性能分析工具、根因分析方法 |
| **claude-code-skill-diagnostic** | Claude Code 技能安装故障诊断与修复 |

### 🎨 前端类 (2个)

| 技能 | 用途 |
|------|------|
| **frontend-development** | React/Vue 组件、状态管理、路由、性能优化 |
| **frontend-design** | 创建独特的生产级前端界面，避免通用 AI 美学 |

### 🤖 Android 类 (2个)

| 技能 | 用途 |
|------|------|
| **jetpack-compose** | 声明式 UI、状态管理、布局组件、动画 |
| **kotlin-java-standards** | Kotlin/Java 编码规范、命名规范、惯用法 |

### 📄 文档处理类 (2个)

| 技能 | 用途 |
|------|------|
| **pdf** | 提取文本/表格、创建/合并/拆分 PDF、表单填写 |
| **xlsx** | 创建/编辑电子表格、数据分析、公式处理、零错误输出 |

### 🏗️ 架构设计类 (2个)

| 技能 | 用途 |
|------|------|
| **architecture-design** | 分层架构、模块划分、技术选型 |
| **brainstorming** | 需求分析、方案设计、创意发散 |

### 📋 其他技能 (8个)

| 技能 | 用途 | 类别 |
|------|------|------|
| **coding-standards** | 命名规范、代码风格、最佳实践 | standards |
| **refactoring** | 改进代码质量、清理技术债 | quality |
| **git-operations** | 提交规范、分支管理、代码合并 | workflow |
| **documentation** | API 文档、架构文档、代码注释 | documentation |
| **error-tracking** | Sentry 集成、错误捕获、监控 | monitoring |
| **performance-optimization** | 代码优化、缓存策略、并发处理 | optimization |
| **security** | 输入验证、认证授权、数据加密 | security |
| **planning** | 任务拆解、里程碑规划、风险管理 | planning |

---

## 🔄 技能协作场景

### 场景1：新功能开发

```
1. brainstorming (需求分析)
   ↓
2. architecture-design (架构设计)
   ↓
3. planning (任务规划)
   ↓
4. api-design (API 设计)
   ↓
5. backend-development (后端开发)
   ├── database-operations (数据库)
   └── middleware (中间件)
   ↓
6. frontend-development (前端开发)
   ↓
7. test-driven-development (测试)
   ↓
8. code-review (代码审查)
   ↓
9. verification (完成前验证)
```

### 场景2：Bug 修复

```
1. debugging-strategies (问题诊断)
   ↓
2. error-tracking (查看日志)
   ↓
3. coding-standards (代码检查)
   ↓
4. refactoring (修复并优化)
   ↓
5. test-driven-development (添加测试)
   ↓
6. verification (验证修复)
```

### 场景3：代码深度分析

```
1. code-question-refiner (问题细化)
   ↓ 生成结构化分析提示词
2. code-research-executor (执行分析)
   ├── code-architecture-analyzer (架构维度)
   ├── code-quality-analyzer (质量维度)
   ├── code-dependency-tracer (依赖维度)
   ├── code-pattern-detector (模式维度)
   └── code-analysis-controller (GoT 协调)
   ↓ 多智能体并行分析
3. code-synthesizer (结果综合)
   ↓ 整合发现、解决矛盾
4. planning (制定改进计划)
   ↓ 根据分析结果优先级排序
5. refactoring (执行改进)
   ↓ 按优先级逐项实施
6. verification (验证改进效果)
```

### 场景4：插件开发

```
1. agent-identifier (创建 Agent)
   ↓ 定义 Agent 结构和触发条件
2. skill-development (创建技能)
   ↓ 编写 SKILL.md 和资源
3. hook-development (添加 Hooks)
   ↓ 实现验证和策略
4. mcp-integration (集成外部服务)
   ↓ 连接 MCP 服务器
5. rule-identifier (配置规则)
   ↓ 设置 hookify 规则
6. webapp-testing (测试)
   ↓ 验证插件功能
7. documentation (编写文档)
```

---

## 🛠️ 技能配置

### skill-rules.json

```json
{
  "version": "2.0",
  "skills": [
    {
      "name": "skill-name",
      "description": "技能描述",
      "category": "category-name"
    }
  ]
}
```

### 技能类别

- **code-analysis**: 代码深度分析
- **plugin-dev**: 插件开发
- **review**: 代码审查
- **backend**: 后端开发
- **testing**: 测试
- **debugging**: 调试
- **frontend**: 前端开发
- **android**: Android 开发
- **document-processing**: 文档处理
- **design**: 设计相关
- **standards**: 规范相关
- **workflow**: 工作流相关
- **documentation**: 文档相关
- **monitoring**: 监控相关
- **optimization**: 优化相关
- **security**: 安全相关
- **planning**: 规划相关

---

## 📚 技能结构

```
skill-name/
├── SKILL.md              # 技能主文件 (必需)
├── resources/            # 参考资源 (可选)
│   ├── topic1.md
│   └── topic2.md
├── scripts/              # 脚本文件 (可选)
│   └── helper.py
├── examples/             # 示例文件 (可选)
│   └── example.md
└── templates/            # 模板文件 (可选)
    └── template.md
```

---

## 🎓 学习路径

### 初级开发者

```
1. coding-standards (编码规范)
2. git-operations (Git 操作)
3. test-driven-development (测试基础)
4. documentation (文档编写)
```

### 中级开发者

```
1. architecture-design (架构设计)
2. backend-development (后端开发)
3. frontend-development (前端开发)
4. debugging-strategies (调试技巧)
5. code-review (代码审查)
```

### 高级开发者

```
1. performance-optimization (性能优化)
2. security (安全实践)
3. refactoring (重构艺术)
4. planning (项目规划)
5. brainstorming (技术决策)
```

### 专家级 (代码分析专家)

```
1. code-question-refiner (问题细化技巧)
2. code-analysis-controller (GoT 图分析协调)
3. code-architecture-analyzer (架构深度分析)
4. code-quality-analyzer (质量评估)
5. code-synthesizer (综合分析报告)
```

### 插件开发者

```
1. agent-identifier (Agent 开发)
2. skill-development (技能开发)
3. hook-development (Hook 开发)
4. mcp-integration (MCP 集成)
5. rule-identifier (规则编写)
```

---

## 🔗 相关资源

- [Claude Code 文档](https://docs.claude.com/claude-code)
- [技能开发指南](./skill-development/skill-development/SKILL.md)
- [插件开发指南](./agent-identifier/agent-identifier/SKILL.md)

---

## 📝 更新日志

### v2.0 (当前版本)
- 新增 15 个技能
- 删除重复和低质量技能
- 优化技能分类和索引
- 总计 42 个技能

### 主要新增技能
- **代码分析类**: 9 个专业分析工具
- **插件开发类**: 5 个插件开发工具
- **文档处理类**: PDF 和 Excel 处理
- **前端设计**: 独特的前端界面设计
- **Web 测试**: Playwright 自动化测试

---

**版本**: 2.0 | **技能总数**: 42 | **最后更新**: 2024-12
