# Data Layer - README

## 目录说明

此目录负责数据的获取与持久化,遵循 Clean Architecture 的分层原则。

```
data/
├── local/                  # [本地数据] - Room Database 相关
│   ├── converter/          # Type Converters (类型转换器)
│   │   └── RoomTypeConverters.kt
│   ├── dao/                # Data Access Objects (数据访问对象)
│   │   ├── BrainTagDao.kt
│   │   └── ContactDao.kt
│   ├── entity/             # Database Entities (数据库实体)
│   │   ├── BrainTagEntity.kt
│   │   └── ContactProfileEntity.kt
│   └── AppDatabase.kt      # 数据库配置类
├── remote/                 # [远程数据] - Retrofit API 服务 (Phase 3 添加)
├── repository/             # [仓库实现] - Repository 接口的落地实现
│   ├── BrainTagRepositoryImpl.kt
│   └── ContactRepositoryImpl.kt
└── model/                  # [数据模型] - DTOs (如需要)

```

## 核心组件

### 1. Room Database

- **数据库文件**: `empathy_ai_database`
- **版本**: 1 (MVP阶段)
- **迁移策略**: `fallbackToDestructiveMigration()` (MVP阶段简化处理)

### 2. Entity 表结构

#### profiles (联系人画像表)
| 列名 | 类型 | 说明 |
|------|------|------|
| id | TEXT (PRIMARY KEY) | 联系人唯一标识 |
| name | TEXT | 显示名称 |
| target_goal | TEXT | 攻略目标 |
| context_depth | INTEGER | 上下文深度 |
| facts_json | TEXT | 事实数据 (JSON格式) |

#### brain_tags (策略标签表)
| 列名 | 类型 | 说明 |
|------|------|------|
| id | INTEGER (PRIMARY KEY, AUTOINCREMENT) | 标签ID |
| contact_id | TEXT (INDEX) | 外键:联系人ID |
| content | TEXT | 标签内容 |
| tag_type | TEXT | 类型: RISK_RED/STRATEGY_GREEN |
| source | TEXT | 来源: MANUAL/AI_INFERRED |

### 3. DAO 接口

#### ContactDao
- `getAllProfiles(): Flow<List<ContactProfileEntity>>` - 查询所有联系人(响应式)
- `getProfileById(id: String): ContactProfileEntity?` - 根据ID查询
- `insertOrUpdate(entity: ContactProfileEntity)` - 插入或更新
- `deleteById(id: String)` - 删除联系人

#### BrainTagDao
- `getTagsByContactId(contactId: String): Flow<List<BrainTagEntity>>` - 查询某人的标签(响应式)
- `getAllRedFlags(): List<BrainTagEntity>` - 查询所有雷区标签
- `insertTag(entity: BrainTagEntity): Long` - 插入标签
- `deleteTag(id: Long)` - 删除标签
- `deleteTagsByContactId(contactId: String)` - 删除某人的所有标签

### 4. Type Converters

使用 Moshi 进行 JSON 序列化/反序列化:

- `Map<String, String>` ↔ JSON String
- `TagType` (Enum) ↔ String

### 5. Repository 实现

- **ContactRepositoryImpl**: 实现联系人相关数据操作
- **BrainTagRepositoryImpl**: 实现策略标签相关数据操作

### 6. Hilt Modules

- **DatabaseModule**: 提供 Database 和 DAO 实例
- **RepositoryModule**: 绑定 Repository 接口与实现

## 设计原则

1. **单一职责**: 每个类只做一件事
2. **响应式查询**: 使用 Flow 实现数据变更自动推送
3. **Upsert 策略**: 插入时使用 REPLACE 简化逻辑
4. **类型安全**: 使用 Type Converters 确保数据类型安全
5. **错误处理**: 所有 I/O 操作返回 Result<T> 包裹

## 使用示例

```kotlin
// 在 ViewModel 中使用
@HiltViewModel
class ContactViewModel @Inject constructor(
    private val contactRepository: ContactRepository
) : ViewModel() {

    // 观察联系人列表 (自动刷新)
    val contacts = contactRepository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // 保存联系人
    fun saveContact(profile: ContactProfile) {
        viewModelScope.launch {
            contactRepository.saveProfile(profile)
                .onSuccess { /* 成功处理 */ }
                .onFailure { /* 错误处理 */ }
        }
    }
}
```

## 注意事项

1. **MVP 阶段**: 表结构变更时,卸载重装 APP 即可,无需 Migration
2. **数据转换**: Repository 层负责 Domain Model 和 Entity 的转换
3. **错误处理**: 使用 Result<T> 统一处理成功/失败场景
4. **协程**: DAO 的写操作使用 suspend 函数,必须在协程中调用

## 待开发 (Phase 3)

- [ ] Remote API Service (Retrofit)
- [ ] Network Response DTOs
- [ ] Repository 的网络数据源集成

---

**最后更新**: 2025-12-03
**维护者**: hushaokang
**文档版本**: v1.0.0
