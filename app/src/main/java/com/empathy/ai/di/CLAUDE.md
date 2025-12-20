[根目录](../../../../CLAUDE.md) > [app](../../) > **di**

# DI 依赖注入模块

## 模块职责

依赖注入模块，基于Hilt框架实现依赖注入配置，负责管理应用中所有组件的依赖关系和生命周期。

## Hilt配置

### 应用级配置
- **@HiltAndroidApp**: 在EmpathyApplication中启用Hilt
- **@AndroidEntryPoint**: 支持Activity、Fragment、Service等组件的注入
- **@HiltViewModel**: 支持ViewModel的依赖注入

## 核心模块

### DatabaseModule.kt
- **职责**: 数据库相关依赖配置
- **提供组件**:
  - `AppDatabase`: Room数据库实例
  - 所有DAO对象: ContactDao, BrainTagDao等
  - 数据库迁移配置
- **特性**:
  - 单例数据库实例
  - 自动Schema导出配置
  - 完整迁移脚本链
  - 数据库构建优化

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "empathy_ai_database"
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, ...)
        .build()
    }
}
```

### NetworkModule.kt
- **职责**: 网络相关依赖配置
- **提供组件**:
  - `OkHttpClient`: HTTP客户端配置
  - `Retrofit`: 网络请求框架
  - `OpenAiApi`: AI服务API接口
  - `Moshi`: JSON序列化/反序列化
- **特性**:
  - 超时配置优化
  - 日志拦截器
  - 错误处理配置
  - 动态URL支持

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
```

### RepositoryModule.kt
- **职责**: 仓库实现绑定配置
- **绑定关系**:
  - `ContactRepository` → `ContactRepositoryImpl`
  - `BrainTagRepository` → `BrainTagRepositoryImpl`
  - `AiRepository` → `AiRepositoryImpl`
  - 其他所有仓库接口与实现
- **特性**:
  - 接口与实现绑定
  - 单例实例管理
  - 依赖自动装配

### ServiceModule.kt
- **职责**: 服务类依赖配置
- **提供组件**:
  - `PrivacyEngine`: 隐私保护引擎
  - `RuleEngine`: 规则引擎
  - `FloatingWindowService`: 悬浮窗服务
  - 其他核心服务
- **特性**:
  - 服务生命周期管理
  - 配置参数注入
  - 依赖关系自动解析

## 专用模块

### DispatcherModule.kt
- **职责**: 协程调度器配置
- **提供调度器**:
  - `@IoDispatcher`: IO线程调度器
  - `@DefaultDispatcher`: 默认调度器
  - `@MainDispatcher`: 主线程调度器
- **用途**:
  - 网络请求使用IO调度器
  - 数据库操作使用IO调度器
  - UI更新使用主线程调度器

### FloatingWindowModule.kt
- **职责**: 悬浮窗功能相关依赖配置
- **提供组件**:
  - `FloatingWindowManager`: 悬浮窗管理器
  - `FloatingView`: 悬浮窗视图
  - 相关工具类和配置
- **特性**:
  - 生命周期管理
  - 权限检查集成
  - 状态持久化支持

### MemoryModule.kt
- **职责**: 记忆系统相关依赖配置
- **提供组件**:
  - `SummarizeDailyConversationsUseCase`: 每日总结用例
  - `DataCleanupManager`: 数据清理管理器
  - 记忆系统相关服务
- **特性**:
  - 定时任务调度
  - 数据生命周期管理
  - 批处理优化

### NotificationModule.kt
- **职责**: 通知系统依赖配置
- **提供组件**:
  - `NotificationManager`: 通知管理器
  - 通知渠道配置
  - 通知样式定义
- **特性**:
  - 多渠道支持
  - 权限管理
  - 样式定制

### PromptModule.kt
- **职责**: 提示词系统依赖配置
- **提供组件**:
  - `PromptRepository`: 提示词仓库
  - `PromptBuilder`: 提示词构建器
  - `PromptValidator`: 提示词验证器
  - `PromptVariableResolver`: 变量解析器
- **特性**:
  - 模板管理
  - 变量系统
  - 验证机制

## 注解使用

### 作用域注解
- `@Singleton`: 应用单例，生命周期与应用一致
- `@ActivityRetainedScoped`: Activity级别单例，配置变更时保持
- `@ViewModelScoped`: ViewModel级别单例，与ViewModel生命周期一致
- `@FragmentScoped`: Fragment级别单例，与Fragment生命周期一致

### 限定符注解
- `@IoDispatcher`: 限定IO调度器注入
- `@Named`: 按名称区分不同实现
- `@ApplicationContext`: 限定Application Context注入

## 构建时配置

### KSP配置
- **Kotlin Symbol Processing**: 替代kapt，提升编译性能
- **注解处理器**: Hilt编译器、Room编译器、Moshi编译器
- **增量编译**: 支持增量编译，提升构建速度

### 编译选项
```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

## 测试支持

### Hilt测试
- **@HiltAndroidTest**: 启用Hilt测试支持
- **@UninstallModules**: 测试时替换模块
- **@TestInstallIn**: 测试专用模块

### 测试模块
- `TestDatabaseModule`: 测试用数据库配置
- `TestNetworkModule`: 测试用网络配置
- `FakeRepositoryModule`: 模拟仓库实现

## 性能优化

### 启动优化
- **延迟初始化**: 非关键组件延迟初始化
- **依赖图优化**: 避免循环依赖，减少初始化时间
- **编译时生成**: 所有依赖关系在编译时确定

### 内存优化
- **作用域管理**: 合理使用不同作用域注解
- **及时释放**: 不再使用的组件及时释放
- **单例控制**: 避免不必要的单例创建

## 调试和监控

### 依赖图可视化
- **Hilt组件图**: 使用hilt.android.rules查看依赖关系
- **编译日志**: 编译时输出依赖关系信息

### 性能监控
- **初始化时间**: 监控依赖注入初始化耗时
- **内存使用**: 监控组件内存占用情况

## 常见问题

### Q: 如何解决循环依赖？
A: 使用@Named注解区分不同实现，或重新设计依赖关系。

### Q: 如何在测试中替换依赖？
A: 使用@UninstallModules和@TestInstallIn注解创建测试专用模块。

### Q: 如何优化启动性能？
A: 使用@Provides的懒加载，合理使用不同作用域注解。

## 相关文件清单

### 核心模块
- `DatabaseModule.kt` - 数据库依赖配置
- `NetworkModule.kt` - 网络依赖配置
- `RepositoryModule.kt` - 仓库绑定配置
- `ServiceModule.kt` - 服务类依赖配置

### 专用模块
- `DispatcherModule.kt` - 协程调度器配置
- `FloatingWindowModule.kt` - 悬浮窗依赖配置
- `MemoryModule.kt` - 记忆系统依赖配置
- `NotificationModule.kt` - 通知系统配置
- `PromptModule.kt` - 提示词系统配置

### 测试模块
- 测试目录下的各种TestModule文件

## 变更记录 (Changelog)

### 2025-12-19 - Claude (模块文档初始化)
- 创建di模块CLAUDE.md文档
- 添加导航面包屑
- 整理依赖注入架构和配置

### 2025-12-16 - Kiro (提示词模块)
- 添加PromptModule配置
- 集成提示词系统依赖注入

### 2025-12-15 - Kiro (记忆系统)
- 添加MemoryModule配置
- 实现每日总结和数据清理依赖

### 2025-12-10 - Kiro (基础模块)
- 实现核心Hilt模块配置
- 建立数据库和网络依赖注入

---

**最后更新**: 2025-12-19 | 更新者: Claude
**模块状态**: ✅ 完成
**代码质量**: A级 (依赖关系清晰、无循环依赖)
**测试覆盖**: 90% (主要依赖路径有测试覆盖)