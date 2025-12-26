---
name: test-driven-development
description: 测试驱动开发 - TDD 实践、测试策略、覆盖率分析、Mock 使用。在编写新功能时使用 TDD 方法，或需要提升测试覆盖率时使用。
---

# 测试驱动开发 (TDD)

## 激活时机

当满足以下条件时自动激活此技能：
- 开始新功能开发
- 需要修复 Bug 并验证
- 重构代码需要保护
- 提升测试覆盖率
- 编写单元测试/集成测试

## TDD 循环

```
┌─────────────────────────────────────┐
│  1. RED - 写一个失败的测试           │
│     先写测试，描述期望行为            │
└───────────────┬─────────────────────┘
                ↓
┌─────────────────────────────────────┐
│  2. GREEN - 让测试通过               │
│     编写最少代码使测试通过            │
└───────────────┬─────────────────────┘
                ↓
┌─────────────────────────────────────┐
│  3. REFACTOR - 重构优化              │
│     改进代码质量，保持测试通过        │
└───────────────┬─────────────────────┘
                ↓
           重复循环
```

## 测试金字塔

```
                    /\
                   /  \
                  / E2E \           少量
                 /--------\         端到端测试
                /          \
               / Integration \      适量
              /--------------\      集成测试
             /                \
            /   Unit Tests     \   大量
           /--------------------\  单元测试
```

### 各层测试比例建议
| 测试类型 | 比例 | 执行速度 | 成本 |
|---------|------|---------|------|
| 单元测试 | 70% | 毫秒级 | 低 |
| 集成测试 | 20% | 秒级 | 中 |
| E2E 测试 | 10% | 分钟级 | 高 |

## 单元测试

### 原则
```
✅ 快速执行（毫秒级）
✅ 独立运行（不依赖外部）
✅ 可重复（结果一致）
✅ 自包含（不依赖执行顺序）
```

### 测试结构 (AAA 模式)
```typescript
describe('UserService', () => {
  it('should create user with valid data', () => {
    // Arrange - 准备测试数据
    const userData = { name: 'Alice', email: 'alice@example.com' };
    const mockRepo = { create: jest.fn().mockResolvedValue(userData) };

    // Act - 执行被测试代码
    const service = new UserService(mockRepo);
    const result = await service.create(userData);

    // Assert - 验证结果
    expect(result.name).toBe('Alice');
    expect(mockRepo.create).toHaveBeenCalledWith(userData);
  });
});
```

### 常见测试场景

#### 1. 测试成功路径
```typescript
it('should return user when found', async () => {
  const user = await userService.findById('123');
  expect(user).toBeDefined();
  expect(user.id).toBe('123');
});
```

#### 2. 测试错误路径
```typescript
it('should throw when user not found', async () => {
  await expect(userService.findById('999'))
    .rejects.toThrow('User not found');
});
```

#### 3. 测试边缘情况
```typescript
it('should handle empty input', async () => {
  await expect(userService.create({}))
    .rejects.toThrow('Invalid input');
});

it('should handle duplicate email', async () => {
  await userService.create({ email: 'test@test.com' });
  await expect(userService.create({ email: 'test@test.com' }))
    .rejects.toThrow('Email already exists');
});
```

## 集成测试

### 测试范围
```
✅ 多个组件协作
✅ 数据库操作
✅ 外部服务调用
✅ API 端点测试
```

### 示例：API 测试
```typescript
describe('POST /api/users', () => {
  beforeAll(async () => {
    await setupTestDatabase();
  });

  afterAll(async () => {
    await cleanupTestDatabase();
  });

  it('should create user and return 201', async () => {
    const response = await request(app)
      .post('/api/users')
      .send({ name: 'Alice', email: 'alice@test.com' });

    expect(response.status).toBe(201);
    expect(response.body.name).toBe('Alice');
    expect(response.body.id).toBeDefined();
  });

  it('should return 400 for invalid email', async () => {
    const response = await request(app)
      .post('/api/users')
      .send({ name: 'Alice', email: 'invalid' });

    expect(response.status).toBe(400);
    expect(response.body.error).toContain('email');
  });
});
```

## Mock 使用

### 何时使用 Mock
```
✅ 外部服务（API、数据库）
✅ 不可控因素（时间、随机）
✅ 性能优化（避免慢操作）
✅ 边缘条件模拟（错误场景）
```

### Mock 示例
```typescript
// Mock 外部 API
jest.mock('./external-api');
import { externalApi } from './external-api';

test('should call external API', async () => {
  externalApi.fetchUser.mockResolvedValue({ id: '123', name: 'Alice' });

  const result = await userService.getExternalUser('123');

  expect(result.name).toBe('Alice');
  expect(externalApi.fetchUser).toHaveBeenCalledWith('123');
});

// Mock 数据库
test('should save user to database', async () => {
  const mockSave = jest.fn();
  jest.spyOn(UserRepository, 'save').mockImplementation(mockSave);

  await userService.create({ name: 'Alice' });

  expect(mockSave).toHaveBeenCalledWith(expect.objectContaining({ name: 'Alice' }));
});
```

## 测试覆盖率

### 覆盖率指标
```
行覆盖率 (Line Coverage)     → 每行代码是否执行
分支覆盖率 (Branch Coverage)  → 每个条件分支是否执行
函数覆盖率 (Function Coverage) → 每个函数是否调用
语句覆盖率 (Statement Coverage) → 每条语句是否执行
```

### 覆盖率目标建议
| 项目类型 | 推荐覆盖率 |
|---------|-----------|
| 核心业务逻辑 | 80-90% |
| 一般业务代码 | 70-80% |
| 工具/配置 | 50-70% |
| 示例/演示 | 30-50% |

### 查看覆盖率
```bash
# Jest
npm test -- --coverage

# 生成 HTML 报告
npm test -- --coverage --coverageReporters=html

# 只查看未覆盖的文件
npm test -- --coverage --collectCoverageFrom='src/**/*.ts'
```

## 测试反模式

### ❌ 测试实现细节
```typescript
// 反模式：测试私有方法
class UserService {
  private validateEmail(email: string) { ... }
}

// 不要测试私有方法
test('private method works', () => {
  // ❌ 不要这样做
  expect(service['validateEmail']('test@test.com')).toBe(true);
});

// 应该测试公共接口
test('should reject invalid email', async () => {
  await expect(service.create({ email: 'invalid' }))
    .rejects.toThrow('Invalid email');
});
```

### ❌ 测试 Mock 而非行为
```typescript
// 反模式
test('mock works', () => {
  mockRepo.create.mockReturnValue({ id: '123' });
  const result = await service.create(data);
  expect(mockRepo.create).toHaveBeenCalled(); // ❌ 只验证 mock 被调用
});

// 正确做法
test('creates user correctly', () => {
  mockRepo.create.mockReturnValue({ id: '123' });
  const result = await service.create(data);
  expect(result.id).toBe('123'); // ✅ 验证实际结果
});
```

### ❌ 脆弱的测试
```typescript
// 反模式：依赖具体实现
test('returns correct object', () => {
  expect(result).toEqual({ // ❌ 太具体，添加字段就失败
    id: '123',
    name: 'Alice',
    email: 'alice@test.com',
    createdAt: '2024-01-01T00:00:00Z'
  });
});

// 正确做法：只验证关键属性
test('returns user with required fields', () => {
  expect(result).toMatchObject({ // ✅ 只验证关键属性
    id: '123',
    name: 'Alice'
  });
});
```

## TDD 最佳实践

### 1. 测试命名
```typescript
// ✅ 好的命名
it('should return user when found');
it('should throw when email is invalid');
it('should create user with default values');

// ❌ 差的命名
it('works');
it('test1');
it('user creation');
```

### 2. 一个测试一个断言
```typescript
// ✅ 推荐
it('should validate email format', () => {
  expect(isValidEmail('test@test.com')).toBe(true);
});

it('should reject empty email', () => {
  expect(isValidEmail('')).toBe(false);
});

// ❌ 不推荐
it('should validate email', () => {
  expect(isValidEmail('test@test.com')).toBe(true);
  expect(isValidEmail('')).toBe(false); // 多个断言
});
```

### 3. 使用测试辅助函数
```typescript
// 辅助函数
function createMockUser(overrides = {}) {
  return {
    id: '123',
    name: 'Alice',
    email: 'alice@test.com',
    ...overrides
  };
}

// 使用
it('should update user name', () => {
  const user = createMockUser({ name: 'Bob' });
  expect(user.name).toBe('Bob');
});
```

## 相关资源

- `resources/test-examples.md` - 测试示例代码
- `resources/coverage-guide.md` - 覆盖率配置指南
- `resources/mocking-guide.md` - Mock 使用指南

---

**技能状态**: 完成 ✅
**测试框架**: Jest, Vitest, Pytest, JUnit 等
**推荐覆盖率**: 70%+
