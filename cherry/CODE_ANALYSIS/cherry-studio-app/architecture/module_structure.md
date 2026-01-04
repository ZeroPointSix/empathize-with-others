# 模块结构分析 - Cherry Studio 表现层

## 1. 目录组织架构

```
src/
├── componentsV2/              # 新版组件系统 (核心)
│   ├── base/                  # 基础UI组件
│   │   ├── Text/
│   │   ├── IconButton/
│   │   ├── Dialog/
│   │   ├── Skeleton/
│   │   └── index.ts
│   ├── layout/                # 布局组件
│   │   ├── YStack/
│   │   ├── XStack/
│   │   ├── ZStack/
│   │   ├── SafeAreaContainer/
│   │   └── index.ts
│   ├── icons/                 # 图标组件
│   │   ├── src/               # SVG 源文件
│   │   ├── components/        # 图标封装组件
│   │   └── index.ts
│   └── features/              # 功能特性组件
│       ├── ChatScreen/        # 聊天界面
│       │   ├── ChatContent/
│       │   ├── MessageInput/
│       │   ├── TopicList/
│       │   └── hooks/
│       ├── Sheet/             # 底部抽屉
│       │   ├── ModelSheet/
│       │   ├── SettingSheet/
│       │   └── BaseSheet/
│       └── Assistant/         # 助手相关
│           └── AssistantCard/
├── screens/                   # 屏幕组件
│   ├── home/
│   │   ├── HomeScreen.tsx
│   │   └── messages/
│   │       ├── messages.tsx
│   │       └── blocks/
│   │           ├── TextBlock.tsx
│   │           ├── ImageBlock.tsx
│   │           ├── ErrorBlock.tsx
│   │           └── ...
│   ├── settings/
│   │   ├── SettingsScreen.tsx
│   │   └── components/
│   └── assistant/
│       ├── AssistantScreen.tsx
│       └── components/
├── hooks/                     # 全局自定义Hooks
│   ├── useMessages.ts
│   ├── useMessageActions.tsx
│   ├── useMessageBlocks.ts
│   ├── useTopics.ts
│   ├── useTheme.ts
│   └── ...
├── navigators/                # 导航配置
│   ├── AppNavigator.tsx
│   ├── HomeNavigator.tsx
│   └── SettingsNavigator.tsx
└── types/                     # 类型定义
```

## 2. 组件分类与职责

### 2.1 基础组件层 (base/)

| 组件 | 文件路径 | 职责 |
|-----|---------|------|
| Text | `base/Text/index.tsx` | 文本渲染，支持样式组合 |
| IconButton | `base/IconButton/index.tsx` | 图标按钮交互组件 |
| Dialog | `base/Dialog/index.tsx` | 对话框弹窗 |
| Skeleton | `base/Skeleton/index.tsx` | 骨架屏加载状态 |
| Loading | `base/Loading/index.tsx` | 加载指示器 |

**设计原则**: 原子化、纯展示、无业务逻辑

### 2.2 布局组件层 (layout/)

| 组件 | 文件路径 | 特点 |
|-----|---------|------|
| YStack | `layout/YStack/index.tsx` | 垂直布局，forwardRef 优化 |
| XStack | `layout/XStack/index.tsx` | 水平布局 |
| ZStack | `layout/ZStack/index.tsx` | 层级布局 |
| SafeAreaContainer | `layout/SafeAreaContainer/index.tsx` | 安全区域适配 |

**代码示例** (`layout/YStack/index.tsx:11-15`):
```typescript
const YStack = forwardRef<View, YStackProps>(({ className = '', ...rest }, ref) => {
  const composed = cn('flex-col', className)
  return <View ref={ref} className={composed} {...rest} />
})
```

### 2.3 功能组件层 (features/)

#### ChatScreen 功能模块

```
ChatScreen/
├── ChatContent/           # 聊天内容区域
│   ├── index.tsx
│   ├── ChatContent.tsx
│   └── hooks/
├── MessageInput/          # 消息输入组件
│   ├── components/
│   │   ├── Root.tsx       # 复合组件根
│   │   ├── DefaultLayout.tsx
│   │   ├── Main.tsx
│   │   ├── InputArea.tsx
│   │   └── ToolButton.tsx
│   └── hooks/
│       ├── useFileAttachments.ts
│       ├── useTextInput.ts
│       └── useMentions.ts
├── TopicList/             # 话题列表
│   ├── index.tsx
│   ├── TopicList.tsx
│   └── hooks/
└── hooks/
    └── useChatScreen.ts
```

#### Sheet 功能模块

```
Sheet/
├── BaseSheet/             # 基础抽屉
├── ModelSheet/            # 模型选择
│   ├── index.tsx
│   ├── ModelSheet.tsx
│   ├── ModelList.tsx
│   └── hooks/
├── SettingSheet/          # 设置抽屉
└── AssistantSheet/        # 助手选择
```

## 3. 屏幕组件结构 (screens/)

### 3.1 HomeScreen 消息模块

**文件**: `src/screens/home/messages/messages.tsx`

```
messages/
├── messages.tsx           # 主屏幕容器
└── blocks/                # 消息块类型
    ├── TextBlock.tsx      # 文本消息
    ├── ImageBlock.tsx     # 图片消息
    ├── CodeBlock.tsx      # 代码消息
    ├── ErrorBlock.tsx     # 错误消息 (578行 - 需拆分)
    ├── LoadingBlock.tsx   # 加载状态
    └── ...
```

### 3.2 设置页面模块

**文件**: `src/screens/settings/`

```
settings/
├── SettingsScreen.tsx     # 设置主页面
└── components/            # 设置项组件
    ├── SettingsItem.tsx
    ├── SettingsGroup.tsx
    └── LanguageSetting.tsx
```

## 4. 自定义 Hooks 结构 (hooks/)

### 4.1 按功能分类

| 分类 | Hooks | 职责 |
|-----|-------|------|
| 消息相关 | `useMessages`, `useMessageActions`, `useMessageBlocks` | 消息 CRUD 和处理 |
| 话题相关 | `useTopics`, `useTopicActions` | 话题管理 |
| 助手相关 | `useAssistants`, `useAssistantActions` | 助手配置 |
| 文件相关 | `useFileAttachments`, `useFileUpload` | 文件处理 |
| UI 相关 | `useTheme`, `useDialog`, `useToast` | UI 状态管理 |

### 4.2 Hook 依赖关系

```
useChatScreen (顶层组合)
├── useMessages
├── useTopics
└── useMessageInput (复合组件)
    ├── useFileAttachments
    ├── useTextInput
    └── useMentions
```

## 5. 模块间依赖关系

### 5.1 依赖方向

```
screens/ (用户界面)
    ↓ 依赖
componentsV2/features/ (功能组件)
    ↓ 依赖
componentsV2/base/ (基础组件)
    ↓ 依赖
hooks/ (业务逻辑)
    ↓ 依赖
db/schema (数据层)
```

### 5.2 跨层依赖问题

| 问题 | 位置 | 影响 |
|-----|------|------|
| Hook 直接访问数据库 | `hooks/useMessages.ts:14-18` | 违反分层原则 |
| 组件内业务逻辑 | `componentsV2/**/components/*.tsx` | 难以测试 |

## 6. 组件大小分布

### 6.1 组件行数统计

| 分类 | 文件数 | 平均行数 | 最大文件 |
|-----|-------|---------|---------|
| 基础组件 | 5 | ~30 | 50 |
| 布局组件 | 4 | ~25 | 40 |
| 功能组件 | 15 | ~150 | 578 (ErrorBlock) |
| 屏幕组件 | 10 | ~100 | 200 |
| Hooks | 39 | ~60 | 150 |

### 6.2 问题组件

| 组件 | 文件路径 | 行数 | 问题 |
|-----|---------|------|------|
| ErrorBlock | `screens/home/messages/blocks/ErrorBlock.tsx` | 578 | 职责过多 |
| ChatContent | `componentsV2/features/ChatScreen/ChatContent/ChatContent.tsx` | ~300 | 需拆分 |
| Messages | `screens/home/messages/Messages.tsx` | ~250 | 需优化 |

## 7. 组件职责分析

### 7.1 良好示例

**MessageInput 组件** (`componentsV2/features/ChatScreen/MessageInput/`)

```
Root.tsx           -> 状态组合 + Context 提供
DefaultLayout.tsx  -> 默认布局展示
Main.tsx           -> 主要交互区域
InputArea.tsx      -> 输入区域
ToolButton.tsx     -> 工具按钮
hooks/             -> 状态逻辑分离
```

**优点**:
- ✅ 职责清晰分离
- ✅ 状态逻辑在 Hooks 中
- ✅ 支持自定义布局

### 7.2 需改进示例

**ErrorBlock 组件** (`screens/home/messages/blocks/ErrorBlock.tsx`)

**问题**:
- ❌ 578 行代码，职责过多
- ❌ UI 渲染与错误处理逻辑混合
- ❌ 状态管理与副作用未分离

**建议拆分**:
```
ErrorBlock.tsx              (主组件)
├── ErrorHeader.tsx         (头部)
├── ErrorContent.tsx        (内容)
├── ErrorActions.tsx        (操作)
├── useErrorState.ts        (状态)
└── useErrorActions.ts      (行为)
```

## 8. 组件设计规范建议

### 8.1 文件大小限制

| 类型 | 建议行数 | 最大行数 |
|-----|---------|---------|
| 基础组件 | < 50 | 80 |
| 布局组件 | < 40 | 60 |
| 功能组件 | < 150 | 200 |
| 屏幕组件 | < 200 | 300 |
| Hooks | < 100 | 150 |

### 8.2 组件结构模板

```typescript
// ✅ 推荐组件结构
import React from 'react'
import { View, Text } from 'react-native'
import { useCustomHook } from './hooks/useCustomHook'

interface ComponentProps {
  // Props 类型定义
}

export const Component: React.FC<ComponentProps> = ({ prop1, prop2 }) => {
  // 1. Hook 调用
  const { state, actions } = useCustomHook(prop1)

  // 2. 渲染辅助函数
  const renderContent = () => {
    // 辅助渲染逻辑
  }

  // 3. 主渲染
  return (
    <View>
      {renderContent()}
    </View>
  )
}
```

## 9. 目录结构健康度评估

| 指标 | 评分 | 说明 |
|-----|------|------|
| 职责分离 | 8/10 | 基础/布局/功能分层清晰 |
| 命名规范 | 9/10 | 目录和文件命名一致 |
| 依赖管理 | 6/10 | 存在跨层依赖 |
| 组件大小 | 5/10 | 存在巨型组件 |
| 可测试性 | 7/10 | Hooks 模式利于测试 |
| 复用性 | 8/10 | 基础组件复用性高 |

**总体评分**: 7.2/10
