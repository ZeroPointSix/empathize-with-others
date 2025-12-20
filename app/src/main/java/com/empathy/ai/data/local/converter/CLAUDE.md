# Data Local Converter - 类型转换器模块

[根目录](../../../../../../../CLAUDE.md) > [app](../../../../../) > [data](../../../) > [local](../../) > [converter](../) > **converter**

## 模块职责

Data Local Converter模块提供Room数据库的类型转换器，负责在复杂对象类型和SQLite支持的基本类型之间进行转换。SQLite只支持基本数据类型（如String、Int、Long等），无法直接存储复杂对象，转换器解决了这一限制。

## 转换器设计原则

### 1. 性能优先
- 使用Moshi进行JSON序列化，性能优异
- 与Retrofit共享序列化库，减少依赖
- 避免反射操作

### 2. 容错设计
- 解析失败时返回默认值
- 不会因损坏数据导致应用崩溃
- 提供合理的降级策略

### 3. 类型安全
- 编译时类型检查
- 避免运行时类型错误
- 明确的转换规则

## 核心转换器

### 1. RoomTypeConverters - 通用类型转换器
- **文件**: `RoomTypeConverters.kt`
- **职责**: 提供通用的类型转换方法

**支持的转换类型**:
```kotlin
class RoomTypeConverters {
    private val moshi = Moshi.Builder().build()
    private val mapType = Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        String::class.java
    )

    // Map<String, String> ↔ JSON String
    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>

    // TagType(Enum) ↔ String
    @TypeConverter
    fun fromTagType(value: TagType?): String

    @TypeConverter
    fun toTagType(value: String?): TagType
}
```

**转换示例**:
```kotlin
// Map转换
val map = mapOf("hobby" to "fishing", "age" to "30")
val json = fromStringMap(map)
// 结果: "{\"hobby\":\"fishing\",\"age\":\"30\"}"

// 枚举转换
val tagType = TagType.RISK_RED
val string = fromTagType(tagType)
// 结果: "RISK_RED"
```

**容错机制**:
- JSON解析失败时返回空Map
- 枚举值不存在时返回默认值
- null值处理逻辑完善

### 2. FactListConverter - 事实列表转换器
- **文件**: `FactListConverter.kt`
- **职责**: 专门处理事实列表的序列化/反序列化

**特殊设计**:
- 针对Fact对象优化序列化策略
- 处理事实的特殊字段（如时间戳）
- 支持部分数据的恢复

**转换方法**:
```kotlin
class FactListConverter {
    private val moshi = Moshi.Builder()
        .add(DateJsonAdapter())
        .build()

    // List<Fact> ↔ JSON String
    @TypeConverter
    fun fromFactList(facts: List<Fact>?): String

    @TypeConverter
    fun toFactList(json: String?): List<Fact>
}
```

## 转换器配置

### 数据库中使用
```kotlin
@Database(
    entities = [ContactProfileEntity::class, ...],
    version = 8,
    autoMigrations = [...],
    exportSchema = true
)
@TypeConverters(
    RoomTypeConverters::class,
    FactListConverter::class
)
abstract class AppDatabase : RoomDatabase()
```

### 实体类中使用
```kotlin
@Entity(tableName = "profiles")
data class ContactProfileEntity(
    @PrimaryKey
    val id: String,

    val name: String,

    // 使用转换器的字段
    @ColumnInfo(name = "extra_data")
    val extraData: Map<String, String> = emptyMap(),

    @ColumnInfo(name = "tag_type")
    val tagType: TagType = TagType.STRATEGY_GREEN,

    @ColumnInfo(name = "facts_json")
    val facts: List<Fact> = emptyList()
)
```

## 性能优化

### 1. Moshi优化
```kotlin
// 使用单例Moshi实例
object MoshiProvider {
    val instance: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(DateAdapter())
            .build()
    }
}
```

### 2. 缓存策略
- 缓存TypeAdapter实例
- 复用序列化配置
- 避免重复创建对象

### 3. 延迟初始化
- 转换器按需初始化
- 减少启动时间开销

## 错误处理策略

### 1. JSON解析错误
```kotlin
@TypeConverter
fun toFactList(json: String?): List<Fact> {
    return try {
        if (json.isNullOrEmpty()) emptyList()
        else adapter.fromJson(json) ?: emptyList()
    } catch (e: JsonDataException) {
        // 记录错误日志
        Log.e("FactListConverter", "Failed to parse facts", e)
        // 返回空列表，避免崩溃
        emptyList()
    }
}
```

### 2. 版本兼容性
```kotlin
@TypeConverter
fun toTagType(value: String?): TagType {
    return if (value.isNullOrEmpty()) {
        TagType.STRATEGY_GREEN
    } else {
        try {
            TagType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            // 处理旧版本的枚举值
            when (value.uppercase()) {
                "OLD_RISK" -> TagType.RISK_RED
                "OLD_STRATEGY" -> TagType.STRATEGY_GREEN
                else -> TagType.STRATEGY_GREEN
            }
        }
    }
}
```

## 测试策略

### 单元测试
```kotlin
@Test
fun testMapConversion() {
    val original = mapOf("key1" to "value1", "key2" to "value2")
    val json = converter.fromStringMap(original)
    val restored = converter.toStringMap(json)
    assertEquals(original, restored)
}

@Test
fun testEnumConversion() {
    val original = TagType.RISK_RED
    val string = converter.fromTagType(original)
    val restored = converter.toTagType(string)
    assertEquals(original, restored)
}

@Test
fun testErrorHandling() {
    // 测试损坏的JSON
    val result = converter.toStringMap("{\"invalid\": json}")
    assertEquals(emptyMap<String, String>(), result)
}
```

### 集成测试
- 测试与Room数据库的集成
- 验证实体字段的正确存储和读取
- 测试数据迁移场景

## 最佳实践

### 1. 类型选择
- 优先使用基本类型
- 复杂对象转换为JSON
- 避免过度嵌套

### 2. 性能考虑
- 避免频繁序列化/反序列化
- 批量操作时注意内存使用
- 使用高效的JSON库

### 3. 可维护性
- 保持转换逻辑简单
- 提供清晰的文档
- 处理边界情况

## 常见问题

### Q: 转换器会影响查询性能吗？
A: 会有一定影响，但通常可以忽略。大量数据查询时考虑优化数据结构。

### Q: 如何处理嵌套对象？
A: 建议扁平化数据结构，或使用关系型设计而不是嵌套JSON。

### Q: 自定义类型如何转换？
A: 创建专用的TypeConverter，使用Moshi自定义Adapter。

## 扩展指南

### 添加新的转换器
```kotlin
class CustomTypeConverters {
    @TypeConverter
    fun fromCustomType(value: CustomType?): String {
        // 实现序列化逻辑
    }

    @TypeConverter
    fun toCustomType(value: String?): CustomType {
        // 实现反序列化逻辑
    }
}

// 在数据库中注册
@TypeConverters(
    RoomTypeConverters::class,
    CustomTypeConverters::class
)
```

## 相关文件清单

- `RoomTypeConverters.kt` - 通用类型转换器
- `FactListConverter.kt` - 事实列表专用转换器

## 变更记录 (Changelog)

### 2025-12-20 - Claude (模块文档创建)
- **创建data/local/converter模块文档**
- **记录类型转换器的设计和使用**
- **提供性能优化和错误处理策略**