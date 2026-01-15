# DI (依赖注入) 模块文档

> [根目录](../../../CLAUDE.md) > [项目文档](../README.md) > **di**

## 模块职责

DI模块负责应用的依赖注入配置，使用Hilt框架：
- **依赖管理**: 管理所有组件的生命周期
- **模块配置**: 定义依赖提供规则
- **作用域控制**: 设置组件的作用域
- **编译时生成**: 确保类型安全和性能

## 核心模块

### DatabaseModule
- **文件**: `DatabaseModule.kt`
- **职责**: 数据库相关依赖
- **提供**:
  - Room数据库实例
  - DAO接口实现
  - 数据库转换器

### NetworkModule
- **文件**: `NetworkModule.kt`
- **职责**: 网络相关依赖
- **提供**:
  - Retrofit实例
  - OkHttp客户端
  - API服务接口
  - JSON解析器

### RepositoryModule
- **文件**: `RepositoryModule.kt`
- **职责**: 仓库层依赖绑定
- **绑定**:
  - 接口到实现类的映射
  - Repository实例

### ServiceModule
- **文件**: `ServiceModule.kt`
- **职责**: 领域服务依赖
- **提供**:
  - 业务服务实例
  - 工具类依赖

### FloatingWindowModule
- **文件**: `FloatingWindowModule.kt`
- **职责**: 悬浮窗相关依赖
- **提供**:
  - 悬浮窗服务
  - 权限管理

## 作用域定义

### ApplicationScope
- **生命周期**: 应用级别
- **用途**: 单例组件
- **示例**: 数据库、网络客户端

### ActivityRetainedScope
- **生命周期**: Activity保留
- **用途**: ViewModel等组件
- **特点**: 配置变更时保留

### FragmentScope
- **生命周期**: Fragment级别
- **用途**: Fragment特定依赖
- **示例**: Fragment适配器

### ViewScope
- **生命周期**: View级别
- **用途**: Compose相关
- **示例**: 状态持有者

## 配置示例

### 数据库模块
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "empathy_database"
        ).build()
    }

    @Provides
    fun provideContactDao(database: AppDatabase): ContactDao {
        return database.contactDao()
    }
}
```

### 网络模块
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
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }
}
```

## 最佳实践

### 1. 模块组织
- 按功能划分模块
- 避免循环依赖
- 保持模块内聚

### 2. 作用域使用
- 合理选择作用域
- 避免内存泄漏
- 考虑生命周期

### 3. 延迟初始化
- 使用@Provides提供延迟初始化
- 考虑启动性能
- 按需加载

## 特殊配置

### API密钥注入
- 使用EncryptedSharedPreferences
- 运行时动态提供
- 安全访问控制

### 测试配置
- 测试替换实现
- Mock依赖注入
- 测试数据准备

## 相关文件清单

### 核心模块
- `DatabaseModule.kt` - 数据库依赖
- `NetworkModule.kt` - 网络依赖
- `RepositoryModule.kt` - 仓库绑定
- `ServiceModule.kt` - 服务依赖
- `FloatingWindowModule.kt` - 悬浮窗依赖

### 配置文件
- `Hilt_Application.kt` - Hilt应用配置

## 变更记录

### 2025-12-21 - 初始创建
- 创建DI模块文档
- 记录核心模块配置
- 定义最佳实践指南