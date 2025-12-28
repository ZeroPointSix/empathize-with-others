---
name: verification
description: 完成前验证 - 功能验收、回归测试、部署检查、上线准备。在任务完成后、发布前或交接前使用。
---

# 完成前验证

## 激活时机

当满足以下条件时自动激活此技能：
- 完成开发任务准备提交
- 发布版本前质量检查
- 代码合并前验证
- 功能验收测试
- 交接前准备检查

## 验证框架

### 四维验证

```
┌─────────────────────────────────────┐
│  1. 功能验证 (Functionality)         │
│     核心功能是否正常工作             │
└───────────────┬─────────────────────┘
                ↓
┌─────────────────────────────────────┐
│  2. 质量验证 (Quality)              │
│     代码质量、性能、安全性           │
└───────────────┬─────────────────────┘
                ↓
┌─────────────────────────────────────┐
│  3. 集成验证 (Integration)          │
│     与其他模块/系统是否正常集成      │
└───────────────┬─────────────────────┘
                ↓
┌─────────────────────────────────────┐
│  4. 交付验证 (Delivery)             │
│     文档、配置、部署是否完整         │
└─────────────────────────────────────┘
```

## 功能验证

### 验收清单

```markdown
### 核心功能
- [ ] 所有需求功能已实现
- [ ] 正常路径测试通过
- [ ] 边缘情况测试通过
- [ ] 错误处理测试通过

### 用户交互
- [ ] 交互流程符合设计
- [ ] 错误提示清晰友好
- [ ] 加载状态有反馈
- [ ] 操作结果有确认

### 兼容性
- [ ] 主流浏览器测试
- [ ] 不同设备测试
- [ ] 不同分辨率测试
- [ ] 向后兼容验证
```

### 功能测试脚本

```typescript
// 功能验证测试示例
describe('功能验证', () => {
  describe('用户登录', () => {
    it('应该成功登录有效用户', async () => {
      const response = await request(app)
        .post('/api/auth/login')
        .send({ email: 'test@example.com', password: 'Password123' });

      expect(response.status).toBe(200);
      expect(response.body.token).toBeDefined();
    });

    it('应该拒绝无效密码', async () => {
      const response = await request(app)
        .post('/api/auth/login')
        .send({ email: 'test@example.com', password: 'wrong' });

      expect(response.status).toBe(401);
    });

    it('应该处理网络错误', async () => {
      // 模拟数据库错误
      jest.spyOn(UserRepository, 'findByEmail')
        .mockRejectedValue(new Error('Database error'));

      const response = await request(app)
        .post('/api/auth/login')
        .send({ email: 'test@example.com', password: 'Password123' });

      expect(response.status).toBe(500);
      expect(response.body.error).toBeDefined();
    });
  });
});
```

## 质量验证

### 代码质量检查

```bash
# 1. Lint 检查
npm run lint

# 2. 类型检查
npm run type-check

# 3. 代码格式检查
npm run format:check

# 4. 代码复杂度检查
npx complexity-report . --max-complexity 10

# 5. 安全漏洞扫描
npm audit
```

### 测试覆盖率

```bash
# 运行测试并生成覆盖率报告
npm test -- --coverage

# 检查覆盖率目标
# Statement: >80%
# Branch: >75%
# Function: >80%
# Line: >80%
```

### 性能验证

```typescript
// 性能基准测试
describe('性能验证', () => {
  it('登录响应时间应小于500ms', async () => {
    const start = Date.now();
    await request(app)
      .post('/api/auth/login')
      .send({ email: 'test@example.com', password: 'Password123' });
    const duration = Date.now() - start;

    expect(duration).toBeLessThan(500);
  });

  it('列表查询应处理10000条数据', async () => {
    const response = await request(app)
      .get('/api/users')
      .query({ page: 1, limit: 10000 });

    expect(response.status).toBe(200);
    expect(response.body.data).toHaveLength(10000);
  });
});
```

### 安全检查

```markdown
### 安全验证清单
- [ ] 输入验证完整
- [ ] SQL 注入防护
- [ ] XSS 防护
- [ ] CSRF 防护
- [ ] 认证授权正确
- [ ] 敏感数据加密
- [ ] 无硬编码密钥
- [ ] 安全头配置
- [ ] 依赖无已知漏洞
```

## 集成验证

### API 集成测试

```typescript
describe('API 集成测试', () => {
  describe('用户注册登录流程', () => {
    it('应该完成完整的注册登录流程', async () => {
      // 1. 注册
      const registerResponse = await request(app)
        .post('/api/auth/register')
        .send({
          email: 'newuser@example.com',
          password: 'Password123',
          username: 'newuser'
        });
      expect(registerResponse.status).toBe(201);

      // 2. 登录
      const loginResponse = await request(app)
        .post('/api/auth/login')
        .send({
          email: 'newuser@example.com',
          password: 'Password123'
        });
      expect(loginResponse.status).toBe(200);
      expect(loginResponse.body.token).toBeDefined();

      // 3. 使用 Token 访问受保护资源
      const profileResponse = await request(app)
        .get('/api/users/me')
        .set('Authorization', `Bearer ${loginResponse.body.token}`);
      expect(profileResponse.status).toBe(200);
      expect(profileResponse.body.email).toBe('newuser@example.com');
    });
  });
});
```

### 依赖服务验证

```markdown
### 外部服务验证
- [ ] 数据库连接正常
- [ ] 缓存服务正常
- [ ] 消息队列正常
- [ ] 第三方 API 可用
- [ ] 文件存储正常
- [ ] 邮件服务正常

### 降级验证
- [ ] 数据库故障时降级方案工作
- [ ] 外部 API 故障时缓存可用
- [ ] 消息队列故障时有备选方案
```

## 交付验证

### 文档检查

```markdown
### 代码文档
- [ ] 复杂逻辑有注释
- [ ] 公共 API 有文档
- [ ] README 更新完整
- [ ] 变更日志更新

### 技术文档
- [ ] API 文档完整
- [ ] 架构文档更新
- [ ] 部署文档更新
- [ ] 环境变量文档

### 用户文档
- [ ] 用户手册更新
- [ ] 操作指南更新
- [ ] FAQ 更新
- [ ] 已知问题说明
```

### 配置检查

```markdown
### 环境配置
- [ ] 环境变量定义完整
- [ ] .env.example 更新
- [ ] 敏感配置不提交
- [ ] 配置默认值合理

### 构建配置
- [ ] package.json 版本更新
- [ ] 依赖版本锁定
- [ ] 构建脚本正确
- [ ] Docker 配置更新

### 部署配置
- [ ] CI/CD 配置更新
- [ ] 部署脚本正确
- [ ] 健康检查配置
- [ ] 监控配置更新
```

### 发布检查

```markdown
### 版本发布
- [ ] 版本号符合语义化
- [ ] CHANGELOG.md 更新
- [ ] Git 标签创建
- [ ] 发布说明准备

### 回滚准备
- [ ] 数据库迁移可回滚
- [ ] 代码回滚步骤明确
- [ ] 回滚时间窗口确定
- [ ] 回滚通知准备
```

## 验证流程

### 自动化验证

```yaml
# .github/workflows/verification.yml
name: Verification

on:
  pull_request:
    branches: [main]

jobs:
  verify:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v3

      - name: Setup
        run: npm ci

      - name: Lint
        run: npm run lint

      - name: Type Check
        run: npm run type-check

      - name: Unit Tests
        run: npm test

      - name: Coverage
        run: npm test -- --coverage

      - name: Build
        run: npm run build

      - name: E2E Tests
        run: npm run test:e2e
```

### 手动验证

```markdown
## 验证步骤

### 1. 环境准备
- [ ] 拉取最新代码
- [ ] 安装依赖
- [ ] 配置环境变量
- [ ] 启动依赖服务

### 2. 功能验证
- [ ] 按测试用例执行
- [ ] 记录验证结果
- [ ] 收集问题

### 3. 问题修复
- [ ] 分类问题优先级
- [ ] 修复关键问题
- [ ] 回归验证

### 4. 交付准备
- [ ] 文档更新
- [ ] 配置检查
- [ ] 部署准备
```

## 验证报告模板

```markdown
# 验证报告

## 基本信息
- **项目/功能**：[名称]
- **验证人员**：[姓名]
- **验证时间**：[日期]
- **版本**：[版本号]

## 验证结果

### 功能验证 ✅/❌
- [x] 核心功能正常
- [x] 边缘情况处理
- [ ] 性能达标

**问题**：
1. [问题描述]
2. [问题描述]

### 质量验证 ✅/❌
- [x] 代码规范通过
- [x] 测试覆盖率达标
- [ ] 安全扫描通过

**问题**：
1. [问题描述]

### 集成验证 ✅/❌
- [x] 内部模块集成正常
- [x] 外部服务集成正常
- [x] 数据兼容性正常

**问题**：
1. [问题描述]

### 交付验证 ✅/❌
- [x] 文档完整
- [x] 配置正确
- [x] 部署就绪

**问题**：
1. [问题描述]

## 总体评估

**验证状态**：✅ 通过 / ⚠️ 有条件通过 / ❌ 不通过

**阻塞问题**：[数量]个

**建议**：
1. [改进建议]
2. [改进建议]

## 后续行动

- [ ] 修复关键问题
- [ ] 完成文档更新
- [ ] 准备发布
- [ ] 安排监控
```

## 常见问题

### 验证不通过

```
问题1：核心功能有 Bug
处理：
- 立即修复
- 回归测试
- 重新验证

问题2：性能不达标
处理：
- 性能分析
- 优化代码
- 调整方案
```

### 时间压力

```
策略1：分批发布
- 核心功能先发布
- 次要功能后续发布

策略2：功能降级
- 降低非关键功能
- 保证核心质量

策略3：增加监控
- 更密集监控
- 快速响应问题
```

## 最佳实践

### ✅ 应该做的

```
1. 提前制定验证计划
2. 自动化可重复的检查
3. 记录所有发现的问题
4. 优先级排序处理
5. 验证通过才交付
```

### ❌ 不应该做的

```
1. 跳过验证节省时间
2. 忽略小问题
3. 无测试依据的验证
4. 最后一刻才验证
5. 验证和修复混在一起
```

## 相关资源

- `resources/checklists.md` - 各类验证清单
- `templates/verification-report.md` - 验证报告模板
- `resources/automation.md` - 自动化验证指南

---

**技能状态**: 完成 ✅
**核心理念**: 质量优先，验证充分
**关键原则**: 不验证不交付
