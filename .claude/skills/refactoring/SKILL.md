---
name: refactoring
description: 代码重构 - 改进代码质量、清理技术债、优化性能、提升可维护性。在代码变得难以维护或需要优化时使用。
---

# 代码重构

## 激活时机

当满足以下条件时自动激活此技能：
- 添加新功能变得困难
- 代码难以理解和修改
- 存在明显的技术债
- 性能需要优化
- 代码审查发现质量问题

## 重构原则

### 重构定义
```
重构是：
✅ 改善代码内部结构
✅ 不改变外部行为
✅ 提高可读性和可维护性

重构不是：
❌ 修复 Bug
❌ 添加新功能
❌ 改变 API 接口
```

### 安全重构流程
```
1. 编写测试保护
2. 小步重构
3. 频繁运行测试
4. 提交每个安全步骤
```

## 常见重构模式

### 提取函数 (Extract Function)

**问题**：函数过长，职责混乱

```typescript
// ❌ 重构前
function processOrder(order: Order) {
  // 验证订单
  if (!order.customerId) throw new Error('No customer');
  if (!order.items || order.items.length === 0) throw new Error('No items');
  if (order.total <= 0) throw new Error('Invalid total');

  // 计算折扣
  let discount = 0;
  if (order.total > 1000) discount = 0.1;
  if (order.total > 5000) discount = 0.15;
  if (order.customerType === 'VIP') discount += 0.05;

  // 保存订单
  const savedOrder = db.orders.create(order);

  // 发送通知
  emailService.send(savedOrder.customerEmail, 'Order confirmed');
  smsService.send(savedOrder.customerPhone, 'Order confirmed');

  return savedOrder;
}

// ✅ 重构后
function processOrder(order: Order) {
  validateOrder(order);
  const finalTotal = applyDiscount(order.total, order.customerType);
  const savedOrder = saveOrder({ ...order, total: finalTotal });
  sendNotifications(savedOrder);
  return savedOrder;
}

function validateOrder(order: Order) {
  if (!order.customerId) throw new Error('No customer');
  if (!order.items?.length) throw new Error('No items');
  if (order.total <= 0) throw new Error('Invalid total');
}

function applyDiscount(total: number, customerType: string): number {
  let discount = 0;
  if (total > 1000) discount = 0.1;
  if (total > 5000) discount = 0.15;
  if (customerType === 'VIP') discount += 0.05;
  return total * (1 - discount);
}

function saveOrder(order: Order): Order {
  return db.orders.create(order);
}

function sendNotifications(order: Order) {
  emailService.send(order.customerEmail, 'Order confirmed');
  smsService.send(order.customerPhone, 'Order confirmed');
}
```

### 提取变量 (Extract Variable)

**问题**：表达式复杂，难以理解

```typescript
// ❌ 重构前
if (user.age >= 18 && user.hasValidId && user.isRegistered && !user.hasViolation) {
  allowService();
}

// ✅ 重构后
const isAdult = user.age >= 18;
const hasValidDocuments = user.hasValidId && user.isRegistered;
const hasGoodRecord = !user.hasViolation;
const isEligible = isAdult && hasValidDocuments && hasGoodRecord;

if (isEligible) {
  allowService();
}
```

### 内联函数 (Inline Function)

**问题**：函数过简单，不如直接使用

```typescript
// ❌ 重构前
function isAdult(age: number): boolean {
  return age >= 18;
}

if (isAdult(user.age)) { }

// ✅ 重构后
if (user.age >= 18) { }
```

### 以函数对象取代函数 (Replace Function with Method)

**问题**：全局函数属于特定对象

```typescript
// ❌ 重构前
function calculateDiscount(order: Order): number {
  return order.total * 0.1;
}

function validateOrder(order: Order): boolean {
  return order.total > 0;
}

// ✅ 重构后
class Order {
  calculateDiscount(): number {
    return this.total * 0.1;
  }

  validate(): boolean {
    return this.total > 0;
  }
}
```

### 用多态替代条件语句 (Replace Conditional with Polymorphism)

**问题**：大量 switch/if-else

```typescript
// ❌ 重构前
function calculatePay(employee: Employee): number {
  switch (employee.type) {
    case 'engineer':
      return employee.salary * 1.2;
    case 'manager':
      return employee.salary * 1.5 + employee.bonus;
    case 'sales':
      return employee.salary * 1.3 + employee.commission;
    default:
      return employee.salary;
  }
}

// ✅ 重构后
abstract class Employee {
  abstract calculatePay(): number;
}

class Engineer extends Employee {
  calculatePay(): number {
    return this.salary * 1.2;
  }
}

class Manager extends Employee {
  calculatePay(): number {
    return this.salary * 1.5 + this.bonus;
  }
}

class Sales extends Employee {
  calculatePay(): number {
    return this.salary * 1.3 + this.commission;
  }
}
```

### 提取接口 (Extract Interface)

**问题**：类承担多个职责，依赖具体实现

```typescript
// ❌ 重构前
class OrderProcessor {
  constructor(
    private database: MySQLDatabase,
    private email: SendGridEmail
  ) { }

  process(order: Order) {
    this.database.save(order);
    this.email.send(order.customerEmail, 'Confirmed');
  }
}

// ✅ 重构后
interface Database {
  save(entity: any): Promise<void>;
}

interface EmailService {
  send(to: string, subject: string): Promise<void>;
}

class OrderProcessor {
  constructor(
    private database: Database,
    private email: EmailService
  ) { }

  process(order: Order) {
    this.database.save(order);
    this.email.send(order.customerEmail, 'Confirmed');
  }
}
```

## 重构检查清单

### 开始前
```markdown
- [ ] 有测试覆盖吗？
- [ ] 测试能通过吗？
- [ ] 了解现有代码结构吗？
- [ ] 明确重构目标吗？
```

### 重构中
```markdown
- [ ] 小步进行
- [ ] 频繁运行测试
- [ ] 提交每个安全点
- [ ] 保持测试通过
```

### 完成后
```markdown
- [ ] 所有测试通过
- [ ] 回归测试无问题
- [ ] 代码更清晰
- [ ] 性能无退化
```

## 性能重构

### N+1 查询优化

```typescript
// ❌ N+1 问题
async function getUsersWithPosts() {
  const users = await db.user.findMany();
  for (const user of users) {
    user.posts = await db.post.findMany({ where: { userId: user.id } });
  }
  return users;
}

// ✅ 一次查询
async function getUsersWithPosts() {
  const users = await db.user.findMany({
    include: { posts: true }
  });
  return users;
}
```

### 缓存添加

```typescript
// ❌ 无缓存
async function getUser(id: string) {
  return await db.user.findUnique({ where: { id } });
}

// ✅ 添加缓存
async function getUser(id: string) {
  const cached = await cache.get(`user:${id}`);
  if (cached) return JSON.parse(cached);

  const user = await db.user.findUnique({ where: { id } });
  if (user) {
    await cache.set(`user:${id}`, JSON.stringify(user), 3600);
  }
  return user;
}
```

### 批量操作

```typescript
// ❌ 逐个处理
for (const item of items) {
  await db.item.create({ data: item });
}

// ✅ 批量处理
await db.item.createMany({ data: items });
```

## 重构优先级

### 高优先级
```
- 阻碍新功能添加
- 存在安全风险
- 性能严重问题
- 频繁出 Bug
```

### 中优先级
```
- 代码难以理解
- 测试难以编写
- 维护成本高
```

### 低优先级
```
- 代码风格不一致
- 可以改进但可工作
- 非关键路径
```

## 重构反模式

### ❌ 过度抽象
```typescript
// 过度设计
abstract class AbstractOrderProcessorFactory {
  abstract createProcessor(): OrderProcessor;
}

// 不如简单直接
class OrderProcessor {
  process(order: Order) { }
}
```

### ❌ 重构和功能混合
```typescript
// 不要同时重构和添加功能
function process(order: Order) {
  // 重构：提取验证逻辑
  // 新功能：添加优惠券逻辑  ❌ 分开进行
}
```

### ❌ 无测试保护
```
没有测试就重构 = 破坏现有功能
```

## 相关资源

- `resources/refactoring-patterns.md` - 完整重构模式列表
- `resources/legacy-code-refactoring.md` - 遗留代码重构指南
- `resources/performance-refactoring.md` - 性能优化技巧

---

**技能状态**: 完成 ✅
**关键前提**: 必须有测试保护
**推荐书籍**: 《重构：改善既有代码的设计》
