# Remote Layer - 网络模块 (Remote Module)

## 目录结构

```
data/remote/
├── api/              # Retrofit API 接口
│   └── OpenAiApi.kt
├── model/            # 数据传输对象 (DTOs)
│   ├── ChatRequestDto.kt
│   ├── ChatResponseDto.kt
│   └── MessageDto.kt
└── README.md         # 本文档
```

## 模块概述

网络模块实现了与 AI 服务商(OpenAI、DeepSeek等)的通信功能,使用 **Retrofit + Moshi** 架构。

遵循 **OpenAI Chat Completion API** 标准接口,支持多服务商切换。

## 核心设计

### 1. 传输层协议 (DTOs)

#### MessageDto
消息单元,符合 OpenAI 标准格式。

```kotlin
{
  "role": "system|user|assistant",
  "content": "消息内容"
}
```

#### ChatRequestDto
AI 聊天请求,包含模型、消息列表、温度参数等。

```kotlin
{
  "model": "gpt-3.5-turbo",
  "messages": [...],
  "temperature": 0.7,
  "stream": false
}
```

**参数说明**:
- `model`: 模型名称 (gpt-3.5-turbo, deepseek-chat, gpt-4)
- `messages`: 消息列表,包含系统指令和历史上下文
- `temperature`: 温度参数 (0.0-2.0),控制随机性。0.7 为通用场景推荐值
- `stream`: 是否使用流式响应。MVP 阶段设为 false,简化处理

#### ChatResponseDto
AI 返回的响应,包含回复内容和 Token 使用情况。

```kotlin
{
  "id": "chatcmpl-123",
  "choices": [
    {
      "message": {
        "role": "assistant",
        "content": "回复内容"
      }
    }
  ],
  "usage": {
    "prompt_tokens": 10,
    "completion_tokens": 20,
    "total_tokens": 30
  }
}
```

### 2. 接口层设计 (OpenAiApi)

使用 Retrofit 定义 API 接口,核心特性:

#### 动态 URL (@Url 注解)
解决多服务商切换问题:

```kotlin
@POST
suspend fun chatCompletion(
    @Url fullUrl: String,              // 运行时动态指定完整 URL
    @HeaderMap headers: Map<String, String>,
    @Body request: ChatRequestDto
): ChatResponseDto
```

**使用示例**:

```kotlin
// OpenAI
val openAiUrl = "https://api.openai.com/v1/chat/completions"
val openAiHeaders = mapOf("Authorization" to "Bearer $openAiKey")
api.chatCompletion(openAiUrl, openAiHeaders, request)

// DeepSeek
val deepSeekUrl = "https://api.deepseek.com/chat/completions"
val deepSeekHeaders = mapOf("Authorization" to "Bearer $deepSeekKey")
api.chatCompletion(deepSeekUrl, deepSeekHeaders, request)
```

### 3. 网络配置 (NetworkModule)

OkHttp 配置针对 LLM 场景优化:

#### 超时设置 (Timeouts)

| 参数 | 时间 | 说明 |
|------|------|------|
| connectTimeout | 30秒 | 连接超时 |
| readTimeout | 60秒 | 读取超时 (AI生成回复 20-40秒) |
| writeTimeout | 30秒 | 写入超时 |

**为什么需要这么长的超时?**
- LLM 生成长回复可能需要 20-40 秒
- 如果超时太短,用户会经常看到 `SocketTimeoutException`

#### 日志拦截器 (LoggingInterceptor)

```kotlin
if (BuildConfig.DEBUG) {
    // DEBUG 模式:记录请求和响应的完整内容
    level = HttpLoggingInterceptor.Level.BODY
} else {
    // 发布模式:只记录基本信息
    level = HttpLoggingInterceptor.Level.BASIC
}
```

**注意**:在正式发布版本中,详细日志可能泄露 API Key,建议关闭或脱敏处理。

### 4. 业务落地 (AiRepositoryImpl)

工作流程 Pipeline:

```
1. 路由选择 → 2. 鉴权注入 → 3. 数据转换 → 4. 调用 API → 5. 解析响应
```

#### 1. 路由选择 (Routing)
根据服务商选择对应的 URL:

```kotlin
val url = when (provider) {
    "OpenAI" -> "https://api.openai.com/v1/chat/completions"
    "DeepSeek" -> "https://api.deepseek.com/chat/completions"
    else -> "https://api.openai.com/v1/chat/completions"
}
```

#### 2. 鉴权注入 (Auth)
构造包含 API Key 的请求头:

```kotlin
val headers = mapOf(
    "Authorization" to "Bearer $apiKey",
    "Content-Type" to "application/json"
)
```

**安全提醒**:API Key 应该加密存储,从 EncryptedSharedPreferences 读取。

#### 3. 数据转换 (Mapping)
将 Domain 数据转换为 MessageDto 列表:

```kotlin
val messages = listOf(
    MessageDto(role = "system", content = systemInstruction),
    MessageDto(role = "user", content = promptContext)
)
```

#### 4. 调用与解析
调用 API 并处理响应:

```kotlin
val response = api.chatCompletion(url, headers, request)
val content = response.choices.first().message?.content
```

## 使用示例

### 分析聊天上下文

```kotlin
class AnalyzeChatUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(
        profile: ContactProfile,
        tags: List<BrainTag>,
        context: List<ChatMessage>
    ): Result<AnalysisResult> {
        // 1. 构建 Prompt
        val prompt = buildPrompt(profile, tags, context)

        // 2. 调用 AI 分析
        return aiRepository.analyzeChat(prompt, systemInstruction = "")
    }
}
```

### 检查草稿安全性

```kotlin
class CheckDraftUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(
        draft: String,
        riskRules: List<String>
    ): Result<SafetyCheckResult> {
        return aiRepository.checkDraftSafety(draft, riskRules)
    }
}
```

## Phase 2 待实现功能

### 1. SettingsRepository 集成

从 EncryptedSharedPreferences 读取:
- AI Provider (OpenAI / DeepSeek / Claude)
- API Key (加密存储)
- Base URL (自定义部署)

### 2. 媒体转录 (Transcribe Media)

集成:
- **FFmpeg** (提取音频/视频帧)
- **ASR** 服务 (语音转文字)
- **OCR** 服务 (图片文字识别)

## 设计优势

1. **灵活性**: 通过 `@Url` 完美解决多服务商切换
2. **稳定性**: 加长超时时间解决 AI 响应慢的问题
3. **隔离性**: DTO 隔离网络层变化对业务层的影响
4. **可扩展性**: 轻松添加新的 AI 服务商

## 注意事项

### API Key 安全

⚠️ **MVP 阶段**:在 `AiRepositoryImpl` 中硬编码 API Key:

```kotlin
const val API_KEY_OPENAI = "YOUR_OPENAI_API_KEY_HERE"
```

**Phase 2 必须**:迁移到 EncryptedSharedPreferences。

### Token 消耗

AI API 按 Token 计费,注意:
- 长对话上下文会快速消耗 Token
- 图片输入(Vision)消耗更多 Token
- 建议实现 Token 使用统计

### 错误处理

常见错误类型:
- `SocketTimeoutException`:超时,检查网络或增加超时时间
- `HttpException`:HTTP 错误(401 鉴权失败、429 频率限制)
- `JsonDataException`:JSON 解析失败,检查返回格式

## 性能优化建议

1. **连接池复用**:OkHttp 默认复用 TCP 连接
2. **请求去重**:短时间内相同请求可以缓存
3. **批量处理**:合并多个小请求为一个大请求
4. **本地缓存**:常用回复可以本地缓存

## 参考链接

- [OpenAI Chat Completion API](https://platform.openai.com/docs/guides/text-generation/chat-completions-api)
- [Retrofit Documentation](https://square.github.io/retrofit/)
- [Moshi Documentation](https://github.com/square/moshi)
- [OkHttp Recipes](https://square.github.io/okhttp/recipes/)

---

**最后更新**: 2025-12-03
**维护者**: hushaokang
**版本**: v1.0.0 (Phase 1 - MVP)
