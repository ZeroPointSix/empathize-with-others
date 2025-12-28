# Skills 通用开发技能集 (扩展版)

> 一套适用于任何项目的 Claude Code 技能集合，涵盖代码审查、架构设计、测试、调试、编码规范、重构、Git 操作、文档编写、错误跟踪、数据库操作、API 设计、前端/后端开发、中间件、性能优化、安全、头脑风暴、计划编写和完成前验证、以及深度代码分析。

---

## 📁 完整目录结构

```
skills/
├── skill-rules.json              # 技能配置文件
├── README.md                     # 本文件
│
├── code-review/                  # 📋 代码审查
├── architecture-design/          # 🏗️ 架构设计
├── test-driven-development/      # 🧪 测试驱动开发
├── debugging/                    # 🐛 系统化调试
├── coding-standards/             # 📏 编码规范
├── refactoring/                  # 🔧 代码重构
├── git-operations/               # 📦 Git 操作
├── documentation/                # 📚 文档编写
├── claude-code-skill-diagnostic/ # 🔍 Claude Code Skill 诊断
│
├── error-tracking/               # 📊 错误跟踪
├── database-operations/          # 🗄️ 数据库操作
├── api-design/                   # 🔌 API 设计
├── frontend-development/         # ⚛️ 前端开发
├── backend-development/          # 🔧 后端开发
├── middleware/                   # 🔗 中间件开发
│
├── performance-optimization/     # ⚡ 性能优化
├── security/                     # 🔒 安全最佳实践
├── brainstorming/                # 💡 头脑风暴
├── planning/                     # 📝 计划编写
├── verification/                 # ✅ 完成前验证
│
├── code-analysis-controller/     # 🎮 代码分析 GoT 控制器
├── code-architecture-analyzer/   # 🏛️ 代码架构分析器
├── code-dependency-tracer/       # 🔗 代码依赖追踪器
├── code-pattern-detector/        # 🔍 代码模式检测器
├── code-quality-analyzer/        # 📊 代码质量分析器
├── code-question-refiner/        # 🎯 代码问题细化器
├── code-research-executor/       # 🚀 代码研究执行器
└── code-synthesizer/             # 🧩 代码综合分析器
```

---

## 🎯 27 个核心技能

### 基础技能 (8个)

| 技能 | 用途 |
|------|------|
| **code-review** | 审查代码质量、架构、安全性 |
| **architecture-design** | 设计分层架构、技术选型 |
| **test-driven-development** | TDD 实践、测试策略 |
| **debugging** | 根因分析、问题定位 |
| **coding-standards** | 命名规范、代码风格 |
| **refactoring** | 改进代码质量、清理技术债 |
| **git-operations** | 提交规范、分支管理 |
| **documentation** | API 文档、架构文档 |
| **claude-code-skill-diagnostic** | Claude Code 技能安装故障诊断与修复 |

### 技术技能 (6个)

| 技能 | 用途 |
|------|------|
| **error-tracking** | Sentry 集成、错误捕获、性能监控 |
| **database-operations** | Prisma ORM、查询优化、事务处理 |
| **api-design** | RESTful 规范、接口设计、版本管理 |
| **frontend-development** | React/Vue 组件、状态管理、路由 |
| **backend-development** | Node.js/Express、分层架构、异步处理 |
| **middleware** | 认证中间件、验证中间件、日志中间件 |

### 专项技能 (5个)

| 技能 | 用途 |
|------|------|
| **performance-optimization** | 代码优化、缓存策略、并发处理 |
| **security** | 输入验证、认证授权、数据加密 |
| **brainstorming** | 需求分析、方案设计、创意发散 |
| **planning** | 任务拆解、里程碑规划、风险管理 |
| **verification** | 功能验收、回归测试、部署检查 |

### 深度分析技能 (8个)

| 技能 | 用途 |
|------|------|
| **code-analysis-controller** | GoT 图分析控制器，管理代码分析状态和图操作 |
| **code-architecture-analyzer** | 识别设计模式、评估模块耦合、分析分层结构 |
| **code-dependency-tracer** | 追踪第三方库、内部依赖、循环依赖、版本兼容性 |
| **code-pattern-detector** | 识别设计模式使用、检测反模式、分析错误处理 |
| **code-quality-analyzer** | 检测代码复杂度、可维护性、代码异味、命名规范 |
| **code-question-refiner** | 将原始代码分析问题细化为结构化的深度研究任务 |
| **code-research-executor** | 执行完整的 7 阶段代码深度分析流程 |
| **code-synthesizer** | 将多个分析智能体的发现综合成连贯的结构化报告 |



## 📊 技能分类

### 按开发阶段

```
项目启动:
  ├── architecture-design (架构设计)
  ├── brainstorming (头脑风暴)
  └── planning (计划编写)

开发过程:
  ├── frontend-development (前端)
  ├── backend-development (后端)
  ├── api-design (API 设计)
  ├── database-operations (数据库)
  ├── middleware (中间件)
  ├── coding-standards (编码规范)
  └── test-driven-development (测试)

质量保证:
  ├── code-review (代码审查)
  ├── debugging (调试)
  ├── verification (完成前验证)
  └── security (安全检查)

部署运维:
  ├── error-tracking (错误监控)
  ├── performance-optimization (性能优化)
  ├── git-operations (版本控制)
  └── documentation (文档)

持续改进:
  ├── refactoring (重构)
  └── planning (迭代规划)

深度分析:
  ├── code-question-refiner (问题细化)
  ├── code-research-executor (分析执行)
  ├── code-analysis-controller (GoT 控制器)
  ├── code-architecture-analyzer (架构分析)
  ├── code-dependency-tracer (依赖追踪)
  ├── code-pattern-detector (模式检测)
  ├── code-quality-analyzer (质量分析)
  └── code-synthesizer (结果综合)
```

### 按技术领域

```
前端开发:
  ├── frontend-development (React/Vue)
  ├── performance-optimization (性能优化)
  └── coding-standards (编码规范)

后端开发:
  ├── backend-development (Node.js/Express)
  ├── api-design (RESTful API)
  ├── database-operations (数据库)
  └── middleware (中间件)

全栈通用:
  ├── architecture-design (架构)
  ├── security (安全)
  ├── test-driven-development (测试)
  ├── debugging (调试)
  └── error-tracking (监控)

代码分析:
  ├── code-analysis-controller (GoT 控制器)
  ├── code-architecture-analyzer (架构分析)
  ├── code-dependency-tracer (依赖追踪)
  ├── code-pattern-detector (模式检测)
  ├── code-quality-analyzer (质量分析)
  ├── code-question-refiner (问题细化)
  ├── code-research-executor (研究执行)
  └── code-synthesizer (结果综合)
```

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
1. debugging (问题诊断)
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

### 场景3：性能优化

```
1. performance-optimization (性能分析)
   ↓
2. database-operations (数据库优化)
   ↓
3. frontend-development (前端优化)
   ↓
4. backend-development (后端优化)
   ↓
5. refactoring (代码重构)
   ↓
6. verification (验证效果)
```

### 场景4：项目重构

```
1. architecture-design (新架构设计)
   ↓
2. planning (重构计划)
   ↓
3. refactoring (执行重构)
   ├── backend-development (后端重构)
   ├── frontend-development (前端重构)
   └── database-operations (数据库优化)
   ↓
4. code-review (审查重构)
   ↓
5. test-driven-development (回归测试)
   ↓
6. documentation (更新文档)
```

### 场景5：代码深度分析

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

- **quality**: 质量相关 (code-review, refactoring, verification)
- **design**: 设计相关 (architecture-design, api-design)
- **testing**: 测试相关 (test-driven-development, verification)
- **debugging**: 调试相关 (debugging)
- **standards**: 规范相关 (coding-standards)
- **workflow**: 工作流相关 (git-operations, planning)
- **documentation**: 文档相关 (documentation)
- **monitoring**: 监控相关 (error-tracking)
- **backend**: 后端相关 (backend-development, database-operations, api-design, middleware)
- **frontend**: 前端相关 (frontend-development)
- **optimization**: 优化相关 (performance-optimization)
- **security**: 安全相关 (security)
- **planning**: 规划相关 (brainstorming, planning)
- **code-analysis**: 代码深度分析相关 (code-analysis-controller, code-architecture-analyzer, code-dependency-tracer, code-pattern-detector, code-quality-analyzer, code-question-refiner, code-research-executor, code-synthesizer)

---

## 📚 每个技能包含

```
skill-name/
├── SKILL.md              # 技能主文件
├── resources/            # 参考资源
│   ├── topic1.md
│   └── topic2.md
└── templates/            # 模板文件
    ├── template1.md
    └── template2.md
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
4. debugging (调试技巧)
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

