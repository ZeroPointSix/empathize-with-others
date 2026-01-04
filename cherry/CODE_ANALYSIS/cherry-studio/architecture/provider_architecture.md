# Provider 架构设计

## 一、双版本 Provider 设计模式

Cherry Studio 采用**渐进式重构策略**，同时维护 Legacy 和 Modern 两个版本的 Provider：

```typescript
// src/aiCore/index.ts
export { default } from './legacy/index'           // Legacy 默认导出
export { default as ModernAiProvider } from './index_new'  // Modern 新版
```

### 1.1 设计动机

- **Legacy Provider**: 支持完整的 LLM 服务商集成，包括认证、流式响应、模型列表等
- **Modern Provider**: 基于 `@cherrystudio/ai-core` 和 AI SDK，提供更现代化的实现
- **渐进迁移**: 允许团队逐步将功能迁移到 Modern Provider，同时保持 Legacy 作为降级方案

### 1.2 目录结构

```
src/aiCore/
├── index.ts              # 导出入口，双版本共存
├── index_new.ts          # ModernAiProvider 实现
├── legacy/               # Legacy Provider
│   ├── index.ts
│   ├── LegacyAiProvider.ts
│   └── clients/
│       ├── BaseApiClient.ts
│       ├── OpenAIClient.ts
│       ├── AnthropicClient.ts
│       ├── GoogleClient.ts
│       └── ...
├── provider/             # Provider 配置转换
│   ├── factory.ts        # AI SDK Provider 工厂
│   └── config.ts         # 配置转换逻辑
└── chunk/                # Chunk 处理
    └── AiSdkToChunkAdapter.ts
```

## 二、ModernAiProvider 核心实现

### 2.1 类结构 (`index_new.ts`)

```typescript
export default class ModernAiProvider {
  private legacyProvider: LegacyAiProvider  // 降级方案
  private localProvider: AiSdkProvider | null
  private config: ModernAiProviderConfig

  async completions(modelId: string, params: StreamTextParams, config: ModernAiProviderConfig) {
    // 1. 准备 Provider 配置
    await prepareSpecialProviderConfig(this.actualProvider, this.config)

    // 2. 创建本地 Provider
    this.localProvider = await createAiSdkProvider(this.config)

    // 3. 构建中间件
    const middlewares = buildAiSdkMiddlewares(config)

    // 4. 创建模型（支持图像生成模型）
    let model = config.isImageGenerationEndpoint
      ? this.localProvider.imageModel(modelId)
      : wrapLanguageModel({ model: this.localProvider.languageModel(modelId), middleware: middlewares })

    // 5. 执行完成请求
    return await this.modernCompletions(model, params, config)
  }

  private async modernCompletions(model, params, config) {
    const executor = createExecutor(this.config.providerId, this.config.options, plugins)

    // 流式处理
    if (config.onChunk) {
      const adapter = new AiSdkToChunkAdapter(config.onChunk, config.mcpTools, accumulate)
      const streamResult = await executor.streamText({ ...params, model })
      return { getText: () => adapter.processStream(streamResult) }
    }
    // 非流式
    const result = await executor.streamText({ ...params, model })
    await result.consumeStream()
    return { getText: () => result.text }
  }
}
```

### 2.2 配置准备

```typescript
// 特殊 Provider 配置处理
async function prepareSpecialProviderConfig(provider: Provider, config: ModernAiProviderConfig) {
  // 处理需要特殊配置的 Provider（如 Ollama 等本地服务）
}

// AI SDK 中间件构建
function buildAiSdkMiddlewares(config: ModernAiProviderConfig): Middleware[] {
  return [
    // 认证中间件
    createAuthMiddleware(config.apiKey),
    // 日志中间件
    createLoggingMiddleware(),
    // 工具调用中间件
    createToolMiddleware(config.mcpTools),
    // ...
  ]
}
```

## 三、Provider 工厂系统

### 3.1 配置转换 (`provider/config.ts`)

将 Cherry Studio 的 Provider 配置转换为 AI SDK 格式：

```typescript
export function providerToAiSdkConfig(provider: Provider, model?: Model) {
  return {
    providerId: provider.type,
    options: {
      baseURL: provider.apiHost,
      apiKey: provider.apiKey,
      // 其他配置...
    },
    modelId: model?.id,
    modelCapabilities: {
      supportsStreaming: model?.capabilities?.streaming ?? true,
      supportsImages: model?.capabilities?.imageInput ?? false,
      supportsTools: model?.capabilities?.tools ?? false,
      maxTokens: model?.config?.maxOutputTokens,
    }
  }
}
```

### 3.2 Provider 工厂 (`provider/factory.ts`)

创建 AI SDK Provider 实例：

```typescript
export async function createAiSdkProvider(config: AiSdkConfig): Promise<AiSdkProvider> {
  switch (config.providerId) {
    case 'openai':
      return createOpenAI(config.options)
    case 'anthropic':
      return createAnthropic(config.options)
    case 'google':
      return createGoogle(config.options)
    case 'groq':
      return createGroq(config.options)
    case 'mistral':
      return createMistral(config.options)
    // 更多 Provider...
    default:
      throw new Error(`Unsupported provider: ${config.providerId}`)
  }
}
```

## 四、Legacy BaseApiClient 抽象

### 4.1 抽象类设计

```typescript
// src/aiCore/legacy/clients/BaseApiClient.ts
export abstract class BaseApiClient<...> implements ApiClient<...> {
  // 核心抽象方法 - 子类必须实现
  abstract createCompletions(payload, options): Promise<TRawOutput>
  abstract generateImage(params): Promise<string[]>
  abstract listModels(): Promise<SdkModel[]>

  // 转换器方法
  abstract getRequestTransformer(): RequestTransformer
  abstract getResponseChunkTransformer(ctx): ResponseChunkTransformer

  // 工具转换
  abstract convertMcpToolsToSdkTools(mcpTools: MCPTool[]): TSdkSpecificTool[]
  abstract convertSdkToolCallToMcp(toolCall, mcpTools): MCPTool
}
```

### 4.2 客户端实现示例

```
legacy/clients/
├── OpenAIClient.ts      // OpenAI 实现
├── AnthropicClient.ts   // Anthropic 实现
├── GoogleClient.ts      // Google Gemini 实现
├── AzureClient.ts       // Azure OpenAI 实现
├── BedrockClient.ts     // AWS Bedrock 实现
├── OllamaClient.ts      // Ollama 本地实现
└── ...
```

## 五、Provider 支持矩阵

| Provider | 流式支持 | 工具调用 | 图像生成 | 模型列表 |
|----------|----------|----------|----------|----------|
| OpenAI | ✅ | ✅ | ✅ | ✅ |
| Anthropic | ✅ | ✅ | ✅ | ✅ |
| Google | ✅ | ✅ | ✅ | ✅ |
| Groq | ✅ | ✅ | ❌ | ✅ |
| Mistral | ✅ | ✅ | ✅ | ✅ |
| Azure | ✅ | ✅ | ✅ | ✅ |
| Bedrock | ✅ | ✅ | ✅ | ✅ |
| Ollama | ✅ | ❌ | ✅ | ✅ |

## 六、扩展新 Provider

### 6.1 实现步骤

1. **创建客户端**: 在 `legacy/clients/` 下创建新的客户端类
2. **继承 BaseApiClient**: 实现所有抽象方法
3. **注册 Provider**: 在 Provider 工厂中注册新类型
4. **添加类型定义**: 在类型系统中添加对应枚举

### 6.2 示例代码结构

```typescript
// legacy/clients/NewProviderClient.ts
export default class NewProviderClient extends BaseApiClient<NewProviderConfig> {
  protected async createCompletions(payload: CompletionRequest): Promise<CompletionResponse> {
    // 实现 API 调用
  }

  protected getRequestTransformer(): RequestTransformer {
    // 请求格式转换
  }

  protected getResponseChunkTransformer(ctx): ResponseChunkTransformer {
    // 响应 Chunk 转换
  }

  protected convertMcpToolsToSdkTools(mcpTools: MCPTool[]): SdkTool[] {
    // 工具格式转换
  }
}
```

## 七、架构优势

1. **统一接口**: 无论底层 Provider 如何差异，对外提供统一 API
2. **可扩展性**: 新增 Provider 只需实现接口，无需修改核心逻辑
3. **渐进迁移**: 可以在不完全重写的情况下引入新实现
4. **测试友好**: 可以轻松 mock Provider 进行单元测试
5. **降级策略**: Modern Provider 可以回退到 Legacy Provider
