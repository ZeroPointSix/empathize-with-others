---
name: android-networking
description: Android 网络请求 - Retrofit、OkHttp、网络缓存、离线支持、API 设计。在实现 Android 网络功能时使用。
---

# Android 网络请求

## 激活时机

当满足以下条件时自动激活此技能：
- 实现 API 调用
- 配置网络层
- 处理网络错误
- 实现离线缓存
- 优化网络性能

## Retrofit 配置

### 基础配置

```kotlin
// ApiService 接口
interface ApiService {
    @GET("users")
    suspend fun getUsers(): Response<List<UserDto>>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") userId: String): Response<UserDto>

    @POST("users")
    suspend fun createUser(@Body user: UserDto): Response<UserDto>

    @PUT("users/{id}")
    suspend fun updateUser(
        @Path("id") userId: String,
        @Body user: UserDto
    ): Response<UserDto>

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") userId: String): Response<Unit>
}
```

### Retrofit 实例

```kotlin
object RetrofitClient {
    private const val BASE_URL = "https://api.example.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .addInterceptor(LoggingInterceptor())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
```

### Moshi 配置

```kotlin
// Moshi 实例
val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .add(DateAdapter())
    .build()

// 自定义适配器
class DateAdapter {
    @FromJson
    fun fromJson(timestamp: Long): Date {
        return Date(timestamp * 1000)
    }

    @ToJson
    fun toJson(date: Date): Long {
        return date.time / 1000
    }
}
```

## OkHttp 拦截器

### 认证拦截器

```kotlin
class AuthInterceptor(
    private val tokenProvider: TokenProvider
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenProvider.getToken()

        val requestWithAuth = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(requestWithAuth)
    }
}
```

### 日志拦截器

```kotlin
class LoggingInterceptor : Interceptor {
    private val logger = Logger.getLogger(LoggingInterceptor::class.java.name)

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val t1 = System.nanoTime()
        logger.info("Sending request ${request.url} on ${chain.connection()}\n" +
                "${request.headers}")

        val response = chain.proceed(request)

        val t2 = System.nanoTime()
        logger.info("Received response for ${request.url} in ${(t2 - t1) / 1e9}ms\n" +
                "${response.headers}")

        return response
    }
}
```

### 缓存拦截器

```kotlin
class CacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // 缓存策略
        val cacheControl = request.header("Cache-Control")
        if (cacheControl == null || cacheControl.contains("no-store")) {
            return response
        }

        return response.newBuilder()
            .header("Cache-Control", "public, max-age=604800")  // 7天
            .build()
    }
}
```

## 网络仓库

### Remote DataSource

```kotlin
class UserRemoteDataSource(
    private val apiService: ApiService
) {
    suspend fun getUsers(): Result<List<User>> {
        return try {
            val response = apiService.getUsers()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.map { it.toDomain() })
            } else {
                Result.failure(NetworkException(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): Result<User> {
        return try {
            val response = apiService.getUser(userId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(NetworkException(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Repository 集成

```kotlin
class UserRepositoryImpl(
    private val remoteDataSource: UserRemoteDataSource,
    private val localDataSource: UserLocalDataSource
) : UserRepository {

    override fun getUsers(): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading)

        // 先尝试本地
        val local = localDataSource.getUsers().first()
        if (local.isNotEmpty()) {
            emit(Resource.Success(local))
        }

        // 再从远程获取
        val result = remoteDataSource.getUsers()
        result.onSuccess { users ->
            localDataSource.saveAll(users)
            emit(Resource.Success(users))
        }.onFailure { error ->
            if (local.isEmpty()) {
                emit(Resource.Error(error.message ?: "Unknown error"))
            }
        }
    }
}
```

## 网络状态处理

### Resource 包装类

```kotlin
sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String?, val data: T? = null) : Resource<T>()
}

// 扩展函数
fun <T> Resource<T>.onSuccess(action: (T) -> Unit): Resource<T> {
    if (this is Resource.Success) {
        action(data)
    }
    return this
}

fun <T> Resource<T>.onError(action: (String) -> Unit): Resource<T> {
    if (this is Resource.Error) {
        message?.let { action(it) }
    }
    return this
}
```

### 网络状态检查

```kotlin
class NetworkManager(private val context: Context) {

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun observeNetworkAvailability(): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }
}
```

## 错误处理

### 异常类型

```kotlin
sealed class AppException(message: String) : Exception(message) {
    class NetworkException(message: String) : AppException(message)
    class ServerException(val code: Int, message: String) : AppException(message)
    class NotFoundException(message: String) : AppException(message)
    class UnauthorizedException(message: String = "Unauthorized") : AppException(message)
    class ParseException(message: String) : AppException(message)
}

// 错误处理
fun Throwable.toAppException(): AppException {
    return when (this) {
        is UnknownHostException -> AppException.NetworkException("No internet connection")
        is SocketTimeoutException -> AppException.NetworkException("Connection timeout")
        is HttpException -> {
            when (this.code()) {
                401 -> AppException.UnauthorizedException()
                404 -> AppException.NotFoundException("Resource not found")
                500 -> AppException.ServerException(500, "Server error")
                else -> AppException.ServerException(this.code(), this.message())
            }
        }
        else -> AppException.ParseException(this.message ?: "Unknown error")
    }
}
```

## 离线支持

### 缓存策略

```kotlin
class CachedUserRepository(
    private val remoteDataSource: UserRemoteDataSource,
    private val localDataSource: UserLocalDataSource,
    private val cacheTimeout: Long = 5 * 60 * 1000 // 5分钟
) : UserRepository {

    override fun getUsers(): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading)

        // 检查缓存
        val cached = localDataSource.getUsersWithTimestamp().first()
        val isCacheValid = System.currentTimeMillis() - cached.timestamp < cacheTimeout

        if (cached.users.isNotEmpty() && isCacheValid) {
            emit(Resource.Success(cached.users))
        }

        // 从远程获取
        try {
            val users = remoteDataSource.getUsers().getOrThrow()
            localDataSource.saveAllWithTimestamp(users)
            emit(Resource.Success(users))
        } catch (e: Exception) {
            if (cached.users.isEmpty()) {
                emit(Resource.Error(e.toAppException().message))
            }
        }
    }
}
```

## 文件上传下载

### 文件上传

```kotlin
interface FileApiService {
    @Multipart
    @POST("upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("description") description: RequestBody?
    ): Response<UploadResponse>
}

// 使用
suspend fun uploadFile(file: File, description: String?): Result<UploadResponse> {
    val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
    val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
    val desc = description?.toRequestBody("text/plain".toMediaTypeOrNull())

    return try {
        val response = fileApiService.uploadFile(body, desc)
        if (response.isSuccessful) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("Upload failed"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 文件下载

```kotlin
suspend fun downloadFile(url: String, outputFile: File): Result<File> {
    return try {
        val request = Request.Builder()
            .url(url)
            .build()

        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            return Result.failure(Exception("Download failed: ${response.code}"))
        }

        response.body?.source()?.use { source ->
            outputFile.outputStream().use { output ->
                source.buffer().readAll(output)
            }
        }

        Result.success(outputFile)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

## 最佳实践

### ✅ 应该做的

```
1. 使用协程处理异步操作
2. 实现缓存减少网络请求
3. 检查网络状态
4. 正确处理错误
5. 使用拦截器统一处理
6. 设置合理的超时时间
```

### ❌ 不应该做的

```
1. 在主线程执行网络请求
2. 忽略网络异常
3. 没有缓存策略
4. 硬编码 API 地址
5. 不处理重试逻辑
```

## 相关资源

- `resources/retrofit-guide.md` - Retrofit 完整指南
- `resources/okhttp-interceptors.md` - OkHttp 拦截器
- `resources/offline-support.md` - 离线支持实现

---

**技能状态**: 完成 ✅
**网络库**: Retrofit 2.x, OkHttp 4.x
**JSON 库**: Moshi, Gson
**异步处理**: Kotlin Coroutines, Flow
