---
name: android-database
description: Android 数据库 - Room、数据表设计、DAO、数据库迁移、数据类型转换。在实现本地数据存储时使用。
---

# Android 数据库

## 激活时机

当满足以下条件时自动激活此技能：
- 设计本地数据库
- 实现 Room 数据库
- 数据库迁移
- 数据类型转换
- 数据库性能优化

## Room 基础

### Entity 定义

```kotlin
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["name"])
    ]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)
```

### 关系定义

```kotlin
// 一对多关系
@Entity(tableName = "posts")
data class Post(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content")
    val content: String?

)

// 嵌入对象
data class Address(
    val street: String,
    val city: String,
    val zipCode: String
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    @Embedded(prefix = "address_")
    val address: Address?
)
```

### DAO 定义

```kotlin
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAll(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :id")
    fun getById(id: Long): Flow<User?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE is_active = 1")
    fun getActiveUsers(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<User>)

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM users")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserWithPosts(id: Long): Flow<UserWithPosts?>

    // 分页查询
    @Query("SELECT * FROM users LIMIT :limit OFFSET :offset")
    suspend fun getPaged(limit: Int, offset: Int): List<User>
}
```

### 关系查询

```kotlin
// 关系数据类
data class UserWithPosts(
    @Embedded val user: User,
    @Relation(
        parentColumn = "id",
        entityColumn = "user_id"
    )
    val posts: List<Post>
)

// 使用
@Dao
interface UserDao {
    @Transaction
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserWithPosts(id: Long): Flow<UserWithPosts?>
}
```

## Database 定义

```kotlin
@Database(
    entities = [User::class, Post::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao

    companion object {
        private const val DATABASE_NAME = "app_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // 仅开发环境
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

## 类型转换

### TypeConverter

```kotlin
class Converters {
    // Date <-> Long
    @TypeConverter
    fun fromDate(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun toDate(date: Date?): Long? {
        return date?.time
    }

    // List<String> <-> String (JSON)
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return Gson().fromJson(value, object : TypeToken<List<String>>() {}.type)
    }
}
```

## 数据库迁移

### 迁移定义

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 添加新列
        database.execSQL(
            "ALTER TABLE users ADD COLUMN avatar_url TEXT"
        )

        // 创建新表
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS posts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                user_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                content TEXT,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // 创建索引
        database.execSQL(
            "CREATE INDEX index_posts_user_id ON posts(user_id)"
        )
    }
}

// 使用
Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
    .build()
```

### 复杂迁移

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. 创建新表
        database.execSQL(
            """
            CREATE TABLE users_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                email TEXT NOT NULL,
                name TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                is_active INTEGER NOT NULL,
                age INTEGER
            )
            """.trimIndent()
        )

        // 2. 复制数据
        database.execSQL(
            """
            INSERT INTO users_new (id, email, name, created_at, is_active)
            SELECT id, email, name, created_at, is_active FROM users
            """.trimIndent()
        )

        // 3. 删除旧表
        database.execSQL("DROP TABLE users")

        // 4. 重命名新表
        database.execSQL("ALTER TABLE users_new RENAME TO users")
    }
}
```

## Repository 实现

```kotlin
class UserRepositoryImpl(
    private val userDao: UserDao,
    private val userRemoteDataSource: UserRemoteDataSource
) : UserRepository {

    override fun getUsers(): Flow<List<User>> {
        return userDao.getAll()
            .onStart { refreshUsers() }
    }

    override fun getUserById(id: Long): Flow<User?> {
        return userDao.getById(id)
    }

    override suspend fun insertUser(user: User) {
        userDao.insert(user)
    }

    override suspend fun updateUser(user: User) {
        userDao.update(user)
    }

    override suspend fun deleteUser(user: User) {
        userDao.delete(user)
    }

    private suspend fun refreshUsers() {
        withContext(Dispatchers.IO) {
            val remoteUsers = userRemoteDataSource.getUsers()
            userDao.insertAll(remoteUsers)
        }
    }
}
```

## Paging 3 集成

### Paging DataSource

```kotlin
@OptIn(ExperimentalPagingApi::class)
class UserPagingSource(
    private val apiService: ApiService,
    private val database: AppDatabase
) : PagingSource<Int, User>() {

    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, User> {
        val page = params.key ?: 1
        return try {
            val users = apiService.getUsers(page, params.loadSize)

            LoadResult.Page(
                data = users,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (users.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, User>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
```

### RemoteMediator

```kotlin
@OptIn(ExperimentalPagingApi::class)
class UserRemoteMediator(
    private val apiService: ApiService,
    private val database: AppDatabase
) : RemoteMediator<Int, User>() {

    override suspend fun initialize(): InitializeAction {
        // 缓存有效期 30 分钟
        val cacheTimeout = 30 * 60 * 1000L
        return InitializeAction(
            skipRefresh = System.currentTimeMillis() - cacheTimeout < 0L
        )
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, User>,
        remoteKey: RemoteKey?
    ): MediatorResult {
        return try {
            val page = when (loadType) {
                LoadType.REFRESH -> 1
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val lastItem = state.lastItemOrNull()
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                    // 获取下一页
                    lastItem.id + 1
                }
            }

            val users = apiService.getUsers(page, state.config.pageSize)

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    userDao.deleteAll()
                }
                userDao.insertAll(users)
            }

            MediatorResult.Success(endOfPaginationReached = users.isEmpty())
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
```

## 数据库测试

### 内存数据库测试

```kotlin
@RunWith(AndroidJUnit4::class)
class UserDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).build()
        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetUser() = runTest {
        val user = User(
            name = "Alice",
            email = "alice@example.com"
        )

        userDao.insert(user)
        val retrieved = userDao.getById(user.id)

        assertNotNull(retrieved)
        assertEquals("Alice", retrieved?.name)
    }
}
```

## 最佳实践

### ✅ 应该做的

```
1. 使用 Flow 返回数据变化
2. 在 IO 调度器执行数据库操作
3. 实现适当的迁移策略
4. 使用索引优化查询
5. 分页加载大数据集
6. 使用事务保证一致性
```

### ❌ 不应该做的

```
1. 在主线程执行数据库操作
2. 在 UI 层直接访问数据库
3. 忽略迁移错误
4. 没有索引的复杂查询
5. 一次性加载大量数据
```

## 相关资源

- `resources/room-guide.md` - Room 完整指南
- `resources/migration-patterns.md` - 迁移模式
- `resources/database-testing.md` - 数据库测试

---

**技能状态**: 完成 ✅
**数据库框架**: Room
**ORM 版本**: Room 2.x
**分页库**: Paging 3
