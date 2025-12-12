# 需求文档 - 灵活的 AI 模型配置

## 简介

本功能旨在提供灵活的 AI 模型配置能力，允许用户自定义 AI 服务商和模型选择。当前实现只支持固定的几个服务商（OpenAI、DeepSeek），用户无法添加自定义服务商或选择不同的模型。通过本功能，用户可以根据自己的需求配置任意兼容 OpenAI API 格式的服务商，选择不同的模型，以获得最佳效果。

## 术语表

- **AI 服务商 (AI Provider)**: 提供 AI API 服务的平台（如 OpenAI、DeepSeek、Google、自定义服务）
- **模型 (Model)**: AI 服务商提供的具体模型（如 gpt-4、deepseek-chat、gemini-pro）
- **API 端点 (API Endpoint)**: AI 服务的 HTTP 请求地址
- **API Key**: 用于身份验证的密钥
- **模型参数 (Model Parameters)**: 控制 AI 行为的参数（temperature、max_tokens、top_p 等）
- **默认配置 (Default Configuration)**: 用户设置的首选服务商和模型
- **配置模板 (Configuration Template)**: 预设的常用服务商配置

## 需求

### 需求 1：服务商管理

**用户故事**：作为用户，我希望可以添加、编辑和删除 AI 服务商配置，以便使用不同的 AI 服务。

#### 验收标准

1. WHEN 用户进入 AI 配置页面 THEN 系统 SHALL 显示所有已配置的服务商列表
2. WHEN 用户点击"添加服务商"按钮 THEN 系统 SHALL 显示服务商配置表单
3. WHEN 用户填写服务商信息并保存 THEN 系统 SHALL 验证配置并添加到列表
4. WHEN 用户长按某个服务商 THEN 系统 SHALL 显示编辑和删除选项
5. WHEN 用户删除服务商时 THEN 系统 SHALL 显示确认对话框防止误操作

### 需求 2：服务商配置

**用户故事**：作为用户，我希望可以配置服务商的详细信息，包括名称、API 端点、API Key 等。

#### 验收标准

1. WHEN 用户配置服务商时 THEN 系统 SHALL 要求输入服务商名称（必填）
2. WHEN 用户配置服务商时 THEN 系统 SHALL 要求输入 API 端点 URL（必填）
3. WHEN 用户配置服务商时 THEN 系统 SHALL 要求输入 API Key（必填）
4. WHEN 用户输入 API 端点时 THEN 系统 SHALL 验证 URL 格式是否正确
5. WHEN 用户保存配置时 THEN 系统 SHALL 使用加密存储保护 API Key

### 需求 3：配置模板

**用户故事**：作为用户，我希望系统提供常用服务商的配置模板，这样我不需要手动输入所有信息。

#### 验收标准

1. WHEN 用户点击"添加服务商"时 THEN 系统 SHALL 显示预设模板列表（OpenAI、DeepSeek、自定义）
2. WHEN 用户选择 OpenAI 模板 THEN 系统 SHALL 自动填充 API 端点为 "https://api.openai.com/v1"
3. WHEN 用户选择 DeepSeek 模板 THEN 系统 SHALL 自动填充 API 端点为 "https://api.deepseek.com/v1"
4. WHEN 用户选择自定义模板 THEN 系统 SHALL 提供空白表单供用户填写

### 需求 4：模型选择

**用户故事**：作为用户，我希望可以为每个服务商配置多个可用模型，并选择默认使用的模型。

#### 验收标准

1. WHEN 用户配置服务商时 THEN 系统 SHALL 允许添加多个模型名称
2. WHEN 用户添加模型时 THEN 系统 SHALL 要求输入模型 ID（如 gpt-4、deepseek-chat）
3. WHEN 用户添加模型时 THEN 系统 SHALL 允许输入模型显示名称（可选）
4. WHEN 用户配置多个模型时 THEN 系统 SHALL 要求选择一个作为默认模型
5. WHEN 用户使用 AI 功能时 THEN 系统 SHALL 默认使用选定的模型


### 需求 5：连接测试

**用户故事**：作为用户，我希望在保存配置前可以测试连接，确保配置正确可用。

#### 验收标准

1. WHEN 用户配置服务商时 THEN 系统 SHALL 提供"测试连接"按钮
2. WHEN 用户点击测试连接时 THEN 系统 SHALL 发送测试请求到配置的 API 端点
3. WHEN 测试成功时 THEN 系统 SHALL 显示成功提示
4. WHEN 测试失败时 THEN 系统 SHALL 显示基本错误信息
5. WHEN 测试进行中时 THEN 系统 SHALL 显示加载指示器并禁用保存按钮



### 需求 9：配置导入导出

**用户故事**：作为用户，我希望可以导出和导入配置，方便在多个设备间同步或备份配置。

#### 验收标准

1. WHEN 用户在配置页面点击"导出配置"时 THEN 系统 SHALL 生成 JSON 格式的配置文件
2. WHEN 导出配置时 THEN 系统 SHALL 加密敏感信息（API Key）
3. WHEN 用户点击"导入配置"时 THEN 系统 SHALL 显示文件选择器
4. WHEN 用户选择配置文件时 THEN 系统 SHALL 验证文件格式和内容
5. WHEN 导入成功时 THEN 系统 SHALL 合并配置并提示用户确认


### 需求 6：错误处理

**用户故事**：作为用户，我希望当配置有问题时能得到清晰的错误提示和解决建议。

#### 验收标准

1. WHEN API Key 无效时 THEN 系统 SHALL 显示"API Key 无效，请检查并重新输入"
2. WHEN API 端点无法访问时 THEN 系统 SHALL 显示"无法连接到服务器，请检查网络或端点地址"
3. WHEN 模型不存在时 THEN 系统 SHALL 显示"模型不可用，请选择其他模型或联系服务商"
4. WHEN 请求超时时 THEN 系统 SHALL 显示"请求超时，请检查网络连接或稍后重试"
5. WHEN 配额用尽时 THEN 系统 SHALL 显示"API 配额已用尽，请充值或切换其他服务商"

### 需求 12：安全性

**用户故事**：作为用户，我希望我的 API Key 和配置信息得到安全保护，不会被泄露。

#### 验收标准

1. WHEN 用户输入 API Key 时 THEN 系统 SHALL 使用密码输入框隐藏内容
2. WHEN 系统保存 API Key 时 THEN 系统 SHALL 使用 EncryptedSharedPreferences 加密存储
3. WHEN 系统显示已保存的 API Key 时 THEN 系统 SHALL 只显示前 4 位和后 4 位字符
4. WHEN 用户导出配置时 THEN 系统 SHALL 提示用户配置文件包含敏感信息
5. WHEN 应用卸载时 THEN 系统 SHALL 自动删除所有加密存储的配置数据
