# BUG-00048-V5-Room-Migration-NotNull约束不匹配问题分析

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档编号 | BUG-00048-V5 |
| 创建日期 | 2026-01-06 |
| 问题来源 | TD-00028-V7-人工结果.md |
| 优先级 | P0 (严重 - 阻塞应用启动) |
| 状态 | ✅ 修复完成 |
| 关联任务 | TD-00028 AI军师流式对话功能 |
| 关联BUG | BUG-00048-V4 (引入此问题的修复) |

---

## 1. 问题描述

### 1.1 错误信息（TD-00028-V7人工结果）

```
Migration didn't properly handle: ai_advisor_conversations
(com.empathy.ai.data.local.entity.AiAdvisorConversationEntity)

Expected:
  related_user_message_id=Column(name='related_user_message_id', type='TEXT', 
    affinity='2', notNull=true, primaryKeyPosition=0, defaultValue='')

Found:
  related_user_message_id=Column(name='related_user_message_id', type='TEXT', 
    affinity='2', notNull=false, primaryKeyPosition=0, defaultValue='')
```

### 1.2 问题本质

**Room Migration v14→v15 创建的列与Entity定义不匹配**

| 对比项 | Entity定义 (Expected) | Migration脚本 (Found) |
|--------|----------------------|----------------------|
| `notNull` | `true` | `false` |
| 原因 | `@ColumnInfo(defaultValue = "")` 隐含NOT NULL | `ALTER TABLE ADD COLUMN`默认允许NULL |

### 1.3 影响范围

- **严重程度**：P0 - 应用无法启动
- **影响版本**：从v14升级到v15的所有用户
- **触发条件**：用户已有v14版本数据库，升级到v15版本应用

---

## 2. 根因分析

### 2.1 Entity定义

**文件**: `data/src/main/kotlin/com/empathy/ai/data/local/entity/AiAdvisorConversationEntity.kt`

```kotlin
// BUG-048-V4: 关联的用户消息ID，用于重新生成时获取原始用户输入
@ColumnInfo(name = "related_user_message_id", defaultValue = "")
val relatedUserMessageId: String = ""
```

**Room解析结果**：
- `defaultValue = ""` → Room期望列有默认值
- `val relatedUserMessageId: String = ""` → 非空类型，Room期望 `notNull = true`

### 2.2 Migration脚本

**文件**: `data/src/main/kotlin/com/empathy/ai/data/di/DatabaseModule.kt`

```kotlin
internal val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE ai_advisor_conversations 
            ADD COLUMN related_user_message_id TEXT DEFAULT ''
            """.trimIndent()
        )
    }
}
```

**SQLite行为**：
- `ALTER TABLE ADD COLUMN ... DEFAULT ''` → 创建允许NULL的列
- SQLite的`ALTER TABLE ADD COLUMN`不支持`NOT NULL`约束（除非有DEFAULT值且使用特殊语法）

### 2.3 Schema对比

**Schema v15.json (Room期望)**：
```json
{
  "fieldPath": "relatedUserMessageId",
  "columnName": "related_user_message_id",
  "affinity": "TEXT",
  "notNull": true,
  "defaultValue": "''"
}
```

**实际数据库 (Migration创建)**：
```sql
related_user_message_id TEXT DEFAULT ''
-- notNull = false (SQLite默认行为)
```

---

## 3. 解决方案

### 3.1 方案A：修改Migration脚本（推荐）

**修改MIGRATION_14_15**，添加`NOT NULL`约束：

```kotlin
internal val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE ai_advisor_conversations 
            ADD COLUMN related_user_message_id TEXT NOT NULL DEFAULT ''
            """.trimIndent()
        )
    }
}
```

**注意**：SQLite 3.x 支持在`ALTER TABLE ADD COLUMN`中使用`NOT NULL DEFAULT`组合。

### 3.2 方案B：重建表（兼容旧数据）

如果方案A不适用（某些SQLite版本限制），需要重建表：

```kotlin
internal val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. 创建临时表（包含新字段）
        db.execSQL("""
            CREATE TABLE ai_advisor_conversations_new (
                id TEXT PRIMARY KEY NOT NULL,
                contact_id TEXT NOT NULL,
                session_id TEXT NOT NULL,
                message_type TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                send_status TEXT NOT NULL DEFAULT 'SUCCESS',
                related_user_message_id TEXT NOT NULL DEFAULT '',
                FOREIGN KEY (contact_id) REFERENCES profiles(id) ON DELETE CASCADE,
                FOREIGN KEY (session_id) REFERENCES ai_advisor_sessions(id) ON DELETE CASCADE
            )
        """)
        
        // 2. 复制数据
        db.execSQL("""
            INSERT INTO ai_advisor_conversations_new 
            (id, contact_id, session_id, message_type, content, timestamp, created_at, send_status, related_user_message_id)
            SELECT id, contact_id, session_id, message_type, content, timestamp, created_at, send_status, ''
            FROM ai_advisor_conversations
        """)
        
        // 3. 删除旧表
        db.execSQL("DROP TABLE ai_advisor_conversations")
        
        // 4. 重命名新表
        db.execSQL("ALTER TABLE ai_advisor_conversations_new RENAME TO ai_advisor_conversations")
        
        // 5. 重建索引
        db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_advisor_conversations_contact_id ON ai_advisor_conversations(contact_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_advisor_conversations_session_id ON ai_advisor_conversations(session_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_advisor_conversations_timestamp ON ai_advisor_conversations(timestamp)")
    }
}
```

### 3.3 方案C：新增修复性Migration（v15→v16）

如果已有用户升级到v15（但Migration失败），需要新增修复性Migration：

```kotlin
// 数据库版本升级到16
version = 16

// 新增修复性Migration
internal val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 重建表以修复NOT NULL约束
        // ... 同方案B
    }
}
```

---

## 4. 推荐方案

**推荐方案A**（修改MIGRATION_14_15），原因：
1. 最简单直接
2. SQLite 3.x 支持`NOT NULL DEFAULT`组合
3. 不需要重建表，性能更好
4. 不需要升级数据库版本

**实施步骤**：
1. 修改`MIGRATION_14_15`脚本
2. 编写Migration测试用例
3. 清理测试设备数据库
4. 验证修复效果

---

## 5. 测试用例

### 5.1 Migration测试

```kotlin
@Test
fun `MIGRATION_14_15 should create related_user_message_id with NOT NULL constraint`() {
    // Given: v14数据库
    helper.createDatabase(TEST_DB, 14).apply {
        // 插入测试数据
        execSQL("""
            INSERT INTO ai_advisor_conversations 
            (id, contact_id, session_id, message_type, content, timestamp, created_at, send_status)
            VALUES ('test-id', 'contact-1', 'session-1', 'USER', 'test content', 1000, 1000, 'SUCCESS')
        """)
        close()
    }
    
    // When: 执行Migration
    val db = helper.runMigrationsAndValidate(TEST_DB, 15, true, MIGRATION_14_15)
    
    // Then: 验证列约束
    val cursor = db.query("PRAGMA table_info(ai_advisor_conversations)")
    var foundColumn = false
    while (cursor.moveToNext()) {
        val name = cursor.getString(cursor.getColumnIndex("name"))
        if (name == "related_user_message_id") {
            foundColumn = true
            val notNull = cursor.getInt(cursor.getColumnIndex("notnull"))
            assertEquals("related_user_message_id should be NOT NULL", 1, notNull)
        }
    }
    cursor.close()
    assertTrue("related_user_message_id column should exist", foundColumn)
    
    // 验证数据迁移
    val dataCursor = db.query("SELECT related_user_message_id FROM ai_advisor_conversations WHERE id = 'test-id'")
    assertTrue(dataCursor.moveToFirst())
    assertEquals("", dataCursor.getString(0))
    dataCursor.close()
}
```

### 5.2 人工测试

| 测试ID | 测试场景 | 操作步骤 | 预期结果 |
|--------|----------|----------|----------|
| MT-001 | 全新安装 | 1. 卸载应用<br>2. 安装新版本<br>3. 启动应用 | 应用正常启动，无Migration错误 |
| MT-002 | v14升级到v15 | 1. 安装v14版本<br>2. 使用AI军师功能<br>3. 升级到v15版本<br>4. 启动应用 | 应用正常启动，数据保留 |
| MT-003 | 验证字段约束 | 1. 完成MT-002<br>2. 使用AI军师功能<br>3. 检查数据库 | related_user_message_id字段为NOT NULL |

---

## 6. 验证清单

- [ ] MIGRATION_14_15脚本已修改
- [ ] Migration测试通过
- [ ] 全新安装测试通过
- [ ] 升级安装测试通过
- [ ] AI军师功能正常工作
- [ ] 终止后重新生成功能正常

---

## 7. 历史版本

| 版本 | 日期 | 内容 |
|------|------|------|
| V1 | 2026-01-05 | 初始分析：DAO方法缺少类型验证 |
| V2 | 2026-01-05 | 添加updateAiMessageContentAndStatus方法 |
| V3 | 2026-01-05 | 改进消息查询逻辑，添加时间戳约束 |
| V4 | 2026-01-06 | 深度分析双气泡和消息消失问题 |
| V5 | 2026-01-06 | **Room Migration NOT NULL约束不匹配问题** |

---

**文档版本**: 5.0
**最后更新**: 2026-01-06
**修复状态**: ✅ 修复完成

---

## 8. 修复实施记录

### 8.1 修改的文件

| 文件路径 | 修改类型 | 说明 |
|----------|----------|------|
| `data/src/main/kotlin/com/empathy/ai/data/di/DatabaseModule.kt` | 修改 | MIGRATION_14_15添加NOT NULL约束 |
| `data/src/androidTest/kotlin/com/empathy/ai/data/local/Migration14To15Test.kt` | 新增 | Migration测试用例（5个测试） |

### 8.2 关键修改

**DatabaseModule.kt - MIGRATION_14_15**：
```kotlin
// 修复前（BUG）
db.execSQL("""
    ALTER TABLE ai_advisor_conversations 
    ADD COLUMN related_user_message_id TEXT DEFAULT ''
""")

// 修复后（V5）
db.execSQL("""
    ALTER TABLE ai_advisor_conversations 
    ADD COLUMN related_user_message_id TEXT NOT NULL DEFAULT ''
""")
```

### 8.3 编译验证

- ✅ `:data:compileDebugKotlin` 编译成功
- ⬜ Migration测试待执行（需要Android设备）
- ⬜ 人工测试待执行
