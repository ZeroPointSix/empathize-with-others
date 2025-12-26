---
name: documentation
description: 文档编写 - API 文档、架构文档、代码注释、README 编写。在需要记录项目信息、API 接口或架构设计时使用。
---

# 文档编写

## 激活时机

当满足以下条件时自动激活此技能：
- 创建新项目
- 添加 API 接口
- 修改架构设计
- 编写复杂代码
- 发布新版本
- 知识分享

## 文档类型

### 1. 项目文档 (README.md)

```markdown
# 项目名称

简短描述项目是什么，解决什么问题。

## 快速开始

### 环境要求
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+

### 安装
\`\`\`bash
npm install
npm run setup
\`\`\`

### 运行
\`\`\`bash
npm run dev
\`\`\`

## 功能特性

- ✅ 用户认证和授权
- ✅ 实时消息推送
- ✅ 数据可视化

## 目录结构

\`\`\`
src/
├── controllers/    # 控制器
├── services/       # 业务逻辑
├── models/         # 数据模型
└── utils/          # 工具函数
\`\`\`

## 开发指南

### 添加新功能
[步骤说明]

### 运行测试
\`\`\`bash
npm test
\`\`\`

## 部署

### Docker 部署
\`\`\`bash
docker-compose up -d
\`\`\`

## 常见问题

### 问题1
解决方案...

## 贡献指南

欢迎提交 Pull Request！

## 许可证

MIT
```

### 2. API 文档

```markdown
# API 文档

## 用户认证 API

### POST /api/auth/register

注册新用户

**请求头**
\`\`\`
Content-Type: application/json
\`\`\`

**请求体**
\`\`\`json
{
  "username": "string (必填, 3-20字符)",
  "email": "string (必填, 有效邮箱)",
  "password": "string (必填, 8-32字符)"
}
\`\`\`

**成功响应**
\`\`\`json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": "string",
    "username": "string",
    "email": "string",
    "createdAt": "ISO8601"
  }
}
\`\`\`

**错误响应**
\`\`\`json
{
  "code": 400,
  "message": "邮箱已被注册",
  "errors": [
    {
      "field": "email",
      "message": "该邮箱已被使用"
    }
  ]
}
\`\`\`

**示例**
\`\`\`bash
curl -X POST https://api.example.com/auth/register \\
  -H "Content-Type: application/json" \\
  -d '{
    "username": "alice",
    "email": "alice@example.com",
    "password": "SecurePass123"
  }'
\`\`\`
```

### 3. 架构文档

```markdown
# 系统架构文档

## 概述

系统采用微服务架构，包含以下核心服务：

- 用户服务 (User Service)
- 认证服务 (Auth Service)
- 订单服务 (Order Service)
- 支付服务 (Payment Service)

## 架构图

\`\`\`
┌─────────────┐
│   前端应用   │
└──────┬──────┘
       │
       ↓
┌─────────────┐
│  API 网关    │
└──────┬──────┘
       │
   ┌───┴───┬─────┬─────┐
   ↓       ↓     ↓     ↓
┌────┐ ┌────┐ ┌────┐ ┌────┐
│用户│ │认证│ │订单│ │支付│
└────┘ └────┘ └────┘ └────┘
   │       │     │     │
   └───┬───┴─────┴─────┘
       ↓
┌─────────────┐
│  数据中心    │
│ - MySQL     │
│ - Redis     │
│ - RabbitMQ  │
└─────────────┘
\`\`\`

## 服务交互

### 用户注册流程

1. 前端调用用户服务
2. 用户服务创建用户记录
3. 调用认证服务生成 Token
4. 返回 Token 给前端

## 数据模型

### 用户表 (users)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 主键 |
| username | VARCHAR(50) | 用户名 |
| email | VARCHAR(100) | 邮箱 |
| created_at | TIMESTAMP | 创建时间 |
```

### 4. 代码注释

```typescript
/**
 * 用户服务类
 * 负责用户相关的业务逻辑处理
 *
 * @example
 * ```typescript
 * const service = new UserService();
 * const user = await service.findById('123');
 * ```
 */
export class UserService {
  /**
   * 根据用户ID查找用户
   *
   * @param id - 用户ID (UUID格式)
   * @returns 用户对象，未找到返回 null
   * @throws {ValidationError} 当 ID 格式无效时抛出
   *
   * @example
   * ```typescript
   * const user = await userService.findById('a1b2c3d4');
   * if (user) {
   *   console.log(user.name);
   * }
   * ```
   */
  async findById(id: string): Promise<User | null> {
    // 验证 ID 格式
    if (!isValidUUID(id)) {
      throw new ValidationError('Invalid user ID format');
    }

    // 从缓存获取
    const cached = await this.cache.get(`user:${id}`);
    if (cached) {
      return JSON.parse(cached);
    }

    // 从数据库查询
    const user = await this.userRepository.findById(id);
    if (user) {
      // 缓存结果，有效期 1 小时
      await this.cache.set(`user:${id}`, JSON.stringify(user), 3600);
    }

    return user;
  }
}
```

### 5. 变更日志 (CHANGELOG.md)

```markdown
# Changelog

## [2.0.0] - 2024-01-15

### Added
- 新增用户权限管理功能
- 添加 OAuth2 认证支持
- 新增数据导出功能

### Changed
- 升级到 React 18
- 优化数据库查询性能
- 更新 UI 设计风格

### Fixed
- 修复登录超时问题 (#123)
- 修复支付回调处理错误 (#124)
- 修复数据导出乱码问题 (#125)

### Removed
- 移除旧版 API v1
- 移除 unused dependencies

## [1.5.0] - 2023-12-01

### Added
- 新增批量导入功能
- 添加邮件通知功能

### Fixed
- 修复分页计算错误
```

## 文档编写原则

### 受众导向
```
用户文档 → 面向用户，通俗易懂
开发文档 → 面向开发者，技术准确
运维文档 → 面向运维，操作清晰
```

### 准确性
```
- 代码示例必须可运行
- API 说明必须与实际一致
- 命令参数必须完整正确
```

### 及时更新
```
- 代码变更时同步更新文档
- API 修改时更新接口文档
- 架构调整时更新架构图
```

### 清晰简洁
```
- 避免冗余内容
- 使用图表辅助理解
- 提供具体示例
- 说明适用场景
```

## 文档工具

### Markdown 工具
```
- Typora - 本地编辑
- VS Code + Markdown 插件
- Obsidian - 知识管理
```

### API 文档工具
```
- Swagger/OpenAPI - API 规范
- Postman - API 测试和文档
- Insomnia - API 客户端
```

### 代码文档生成
```
- TypeDoc (TypeScript)
- JSDoc (JavaScript)
- Sphinx (Python)
- Javadoc (Java)
```

### 架构图工具
```
- Mermaid - 文本转图表
- PlantUML - UML 图
- Draw.io - 在线绘图
- Excalidraw - 手绘风格
```

## 文档模板

### 功能设计文档

```markdown
# [功能名称] 设计文档

## 背景

[为什么需要这个功能]

## 目标

[功能要达到什么目标]

## 功能描述

[详细的功能说明]

## 技术方案

### 架构设计
[架构图和说明]

### 数据模型
[数据表设计]

### API 设计
[API 接口定义]

## 实现计划

- [ ] 任务1
- [ ] 任务2
- [ ] 任务3

## 测试计划

[测试用例和验收标准]

## 风险评估

[潜在风险和应对措施]
```

### 技术决策文档

```markdown
# 技术选型：[技术名称]

## 背景

[需要解决什么问题]

## 选项对比

| 方案 | 优点 | 缺点 | 评分 |
|------|------|------|------|
| 方案A | ... | ... | 8/10 |
| 方案B | ... | ... | 6/10 |

## 决策

选择方案A，理由：
1. 团队已有经验
2. 社区活跃
3. 性能满足需求

## 风险与缓解

- 风险：...
- 缓解措施：...
```

## 最佳实践

### 文档位置
```
项目根目录/
├── README.md              # 项目说明
├── CHANGELOG.md           # 变更日志
├── docs/
│   ├── architecture.md    # 架构文档
│   ├── api.md             # API 文档
│   ├── development.md     # 开发指南
│   └── deployment.md      # 部署文档
└── src/
    └── modules/
        └── README.md      # 模块说明
```

### 文档维护
```markdown
- [ ] 每次代码变更同步更新文档
- [ ] 定期审查文档准确性
- [ ] 删除过时内容
- [ ] 补充缺失的文档
```

## 相关资源

- `resources/readme-template.md` - README 模板
- `resources/api-doc-template.md` - API 文档模板
- `resources/diagram-guide.md` - 图表绘制指南

---

**技能状态**: 完成 ✅
**推荐格式**: Markdown
**版本控制**: 文档与代码同步更新
