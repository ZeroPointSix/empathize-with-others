---
name: frontend-development
description: 前端开发 - React/Vue 组件设计、状态管理、路由、样式、性能优化。在开发前端应用时使用。
---

# 前端开发

## 激活时机

当满足以下条件时自动激活此技能：
- 开发 React/Vue/Angular 组件
- 实现前端路由
- 管理应用状态
- 优化前端性能
- 处理 API 调用
- 编写组件样式

## 组件设计原则

### 单一职责

```tsx
// ❌ 组件职责过多
function UserComponent() {
  const [user, setUser] = useState(null);
  const [posts, setPosts] = useState([]);
  const [comments, setComments] = useState([]);

  useEffect(() => {
    fetchUser().then(setUser);
    fetchPosts().then(setPosts);
    fetchComments().then(setComments);
  }, []);

  return (
    <div>
      <UserProfile user={user} />
      <UserPosts posts={posts} />
      <UserComments comments={comments} />
    </div>
  );
}

// ✅ 拆分为职责单一的小组件
function UserProfile({ userId }) {
  const user = useUser(userId);
  return <ProfileCard user={user} />;
}

function UserPosts({ userId }) {
  const posts = useUserPosts(userId);
  return <PostList posts={posts} />;
}

function UserComments({ userId }) {
  const comments = useUserComments(userId);
  return <CommentList comments={comments} />;
}
```

### Props 设计

```tsx
// ✅ 使用接口定义 Props
interface UserCardProps {
  user: User;
  onEdit?: (user: User) => void;
  showEmail?: boolean;
  className?: string;
}

export function UserCard({
  user,
  onEdit,
  showEmail = true,
  className = '',
}: UserCardProps) {
  return (
    <div className={`user-card ${className}`}>
      <h3>{user.name}</h3>
      {showEmail && <p>{user.email}</p>}
      {onEdit && <button onClick={() => onEdit(user)}>Edit</button>}
    </div>
  );
}
```

### 组件模式

```tsx
// 容器展示模式
// UserProfileContainer.tsx - 数据逻辑
export function UserProfileContainer({ userId }: { userId: string }) {
  const user = useUser(userId);
  const { updateUser } = useUserActions();

  if (!user) return <Loading />;

  return <UserProfile user={user} onUpdate={updateUser} />;
}

// UserProfile.tsx - 展示逻辑
export function UserProfile({ user, onUpdate }: UserProfileProps) {
  return (
    <div className="profile">
      <Avatar src={user.avatar} />
      <h1>{user.name}</h1>
      <button onClick={() => onUpdate(user)}>Edit</button>
    </div>
  );
}

// 组合组件模式
export function UserList({ users }: { users: User[] }) {
  return (
    <div className="user-list">
      <UserListHeader count={users.length} />
      <UserListItems users={users} />
      <UserListFooter />
    </div>
  );
}
```

## Hooks 最佳实践

### 自定义 Hooks

```tsx
// 数据获取 Hook
export function useUser(userId: string) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    fetchUser(userId)
      .then(setUser)
      .catch(setError)
      .finally(() => setLoading(false));
  }, [userId]);

  return { user, loading, error };
}

// 表单处理 Hook
export function useForm<T extends Record<string, any>>(
  initialValues: T,
  validate: (values: T) => Record<string, string>
) {
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [touched, setTouched] = useState<Record<string, boolean>>({});

  const handleChange = (name: keyof T) => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    setValues((prev) => ({ ...prev, [name]: e.target.value }));
  };

  const handleBlur = (name: keyof T) => () => {
    setTouched((prev) => ({ ...prev, [name]: true }));
    setErrors(validate(values));
  };

  const handleSubmit = (onSubmit: (values: T) => void) => (
    e: React.FormEvent
  ) => {
    e.preventDefault();
    const validationErrors = validate(values);
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length === 0) {
      onSubmit(values);
    }
  };

  return {
    values,
    errors,
    touched,
    handleChange,
    handleBlur,
    handleSubmit,
  };
}
```

### Hooks 规则

```tsx
// ✅ 只在顶层调用 Hooks
function MyComponent() {
  const [count, setCount] = useState(0);    // ✅ 顶层
  useEffect(() => { ... }, []);            // ✅ 顶层

  if (condition) {
    const [value, setValue] = useState(0); // ❌ 条件中调用
  }
}

// ✅ 只在函数组件中调用
function MyComponent() {
  useCustomHook();  // ✅ 组件中
}

function regularFunction() {
  useCustomHook();  // ❌ 普通函数中调用
}
```

## 状态管理

### Context API

```tsx
// UserContext.tsx
interface UserContextValue {
  user: User | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const UserContext = createContext<UserContextValue | null>(null);

export function UserProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);

  const login = async (email: string, password: string) => {
    const user = await api.login(email, password);
    setUser(user);
  };

  const logout = () => {
    setUser(null);
  };

  return (
    <UserContext.Provider value={{ user, login, logout }}>
      {children}
    </UserContext.Provider>
  );
}

export function useUserContext() {
  const context = useContext(UserContext);
  if (!context) {
    throw new Error('useUserContext must be used within UserProvider');
  }
  return context;
}
```

### 状态管理库选择

| 场景 | 推荐方案 |
|------|---------|
| 小型应用 | Context API + useState |
| 中型应用 | Zustand / Jotai |
| 大型应用 | Redux Toolkit |
| 服务端状态 | React Query / SWR |

## 路由管理

### React Router

```tsx
// App.tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* 公共路由 */}
        <Route path="/" element={<Layout />}>
          <Route index element={<Home />} />
          <Route path="about" element={<About />} />
          <Route path="users" element={<Users />} />

          {/* 动态路由 */}
          <Route path="users/:id" element={<UserDetail />} />

          {/* 嵌套路由 */}
          <Route path="posts" element={<PostsLayout />}>
            <Route index element={<PostList />} />
            <Route path=":id" element={<PostDetail />} />
          </Route>

          {/* 受保护路由 */}
          <Route
            path="dashboard"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />

          {/* 404 */}
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

// ProtectedRoute 组件
function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}
```

## 性能优化

### 代码分割

```tsx
import { lazy, Suspense } from 'react';

// 懒加载组件
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Settings = lazy(() => import('./pages/Settings'));

function App() {
  return (
    <Suspense fallback={<Loading />}>
      <Routes>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/settings" element={<Settings />} />
      </Routes>
    </Suspense>
  );
}
```

### 虚拟化长列表

```tsx
import { useVirtualizer } from '@tanstack/react-virtual';

function VirtualList({ items }: { items: Item[] }) {
  const parentRef = useRef<HTMLDivElement>(null);

  const virtualizer = useVirtualizer({
    count: items.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 50, // 每项高度
  });

  return (
    <div ref={parentRef} style={{ height: '400px', overflow: 'auto' }}>
      <div
        style={{
          height: `${virtualizer.getTotalSize()}px`,
          position: 'relative',
        }}
      >
        {virtualizer.getVirtualItems().map((virtualItem) => (
          <div
            key={virtualItem.key}
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: `${virtualItem.size}px`,
              transform: `translateY(${virtualItem.start}px)`,
            }}
          >
            {items[virtualItem.index].content}
          </div>
        ))}
      </div>
    </div>
  );
}
```

### 图片优化

```tsx
// 懒加载图片
import Image from 'next/image';

// Next.js
<Image
  src="/avatar.jpg"
  alt="Avatar"
  width={200}
  height={200}
  loading="lazy"
  placeholder="blur"
/>

// 普通 React
<img
  src="/avatar.jpg"
  alt="Avatar"
  loading="lazy"
  width={200}
  height={200}
/>
```

## 样式方案

### CSS Modules

```tsx
// UserCard.module.css
.card {
  padding: 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

// UserCard.tsx
import styles from './UserCard.module.css';

export function UserCard({ user }: { user: User }) {
  return (
    <div className={styles.card}>
      <h3 className={styles.title}>{user.name}</h3>
    </div>
  );
}
```

### Tailwind CSS

```tsx
export function UserCard({ user }: { user: User }) {
  return (
    <div className="p-4 border border-gray-200 rounded-lg">
      <h3 className="text-lg font-semibold">{user.name}</h3>
      <p className="text-gray-600">{user.email}</p>
    </div>
  );
}
```

### Styled Components

```tsx
import styled from 'styled-components';

const Card = styled.div`
  padding: 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;

  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }
`;

const Title = styled.h3`
  font-size: 18px;
  font-weight: 600;
`;

export function UserCard({ user }: { user: User }) {
  return (
    <Card>
      <Title>{user.name}</Title>
    </Card>
  );
}
```

## API 调用

### React Query

```tsx
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

// 数据获取
function useUser(userId: string) {
  return useQuery({
    queryKey: ['user', userId],
    queryFn: () => api.getUser(userId),
    staleTime: 5 * 60 * 1000, // 5分钟
    cacheTime: 10 * 60 * 1000, // 10分钟
  });
}

// 数据修改
function useUpdateUser() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (user: User) => api.updateUser(user),
    onSuccess: (data) => {
      // 刷新相关查询
      queryClient.invalidateQueries({ queryKey: ['user', data.id] });
      queryClient.setQueryData(['user', data.id], data);
    },
  });
}
```

### 错误处理

```tsx
function UserProfile({ userId }: { userId: string }) {
  const { data: user, isLoading, error, refetch } = useUser(userId);

  if (isLoading) return <Loading />;
  if (error) return <Error error={error} onRetry={refetch} />;
  if (!user) return <NotFound />;

  return <Profile user={user} />;
}
```

## 最佳实践

### ✅ 应该做的

```tsx
// 1. 组件职责单一
export function UserProfile({ user }: { user: User }) { }

// 2. Props 类型明确
interface Props {
  user: User;
  onUpdate: (user: User) => void;
}

// 3. 使用自定义 Hooks
export function useUser(userId: string) { }

// 4. 错误边界处理
<ErrorBoundary fallback={<ErrorPage />}>
  <App />
</ErrorBoundary>
```

### ❌ 不应该做的

```tsx
// 1. 组件过大
function GiantComponent() {
  // 500+ 行代码 ❌
}

// 2. 在组件中直接调用 API
function UserList() {
  useEffect(() => {
    fetch('/api/users').then(setUsers); // ❌
  }, []);
}

// 3. 在渲染中修改状态
function Counter() {
  const [count, setCount] = useState(0);
  setCount(count + 1); // ❌ 无限循环
}
```

## 相关资源

- `resources/component-patterns.md` - 组件设计模式
- `resources/state-management.md` - 状态管理指南
- `resources/performance.md` - 性能优化详解

---

**技能状态**: 完成 ✅
**支持框架**: React, Vue, Angular, Svelte
**推荐库**: React Query, Zustand, React Router, TanStack Virtual
