# TE-00028-V10-BUG-00048-V5-Migration测试用例

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档编号 | TE-00028-V10 |
| 创建日期 | 2026-01-06 |
| 关联BUG | BUG-00048 V5 |
| 关联任务 | TD-00028 AI军师流式对话功能 |
| 测试类型 | Migration测试 + 人工测试 |

---

## 1. 测试目标

验证BUG-00048 V5修复后：
1. MIGRATION_14_15正确创建`related_user_message_id`列，带有`NOT NULL`约束
2. 现有数据正确迁移
3. 应用正常启动，无Migration错误

---

## 2. 单元测试用例

### 2.1 Migration14To15Test.kt

```kotlin
package com.empathy.ai.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.empathy.ai.data.di.DatabaseModule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * BUG-00048-V5 Migration测试
 * 
 * 验证MIGRATION_14_15正确创建related_user_message_id列
 */
@RunWith(AndroidJUnit4::class)
class Migration14To15Test {

    companion object {
        private const val TEST_DB = "migration-test"
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * TC-MIG-001: 验证related_user_message_id列的NOT NULL约束
     */
    @Test
    @Throws(IOException::class)
    fun migration14To15_shouldCreateColumnWithNotNullConstraint() {
        // Given: 创建v14数据库
        helper.createDatabase(TEST_DB, 14).apply {
            // 插入测试数据
            execSQL("""
                INSERT INTO profiles (id, name, target_goal, context_depth, facts_json, relationship_score)
                VALUES ('contact-1', 'Test Contact', 'Test Goal', 3, '[]', 50)
            """)
            execSQL("""
                INSERT INTO ai_advisor_sessions (id, contact_id, title, created_at, updated_at, message_count, is_active)
                VALUES ('session-1', 'contact-1', 'Test Session', 1000, 1000, 0, 1)
            """)
            execSQL("""
                INSERT INTO ai_advisor_conversations 
                (id, contact_id, session_id, message_type, content, timestamp, created_at, send_status)
                VALUES ('msg-1', 'contact-1', 'session-1', 'USER', 'Hello', 1000, 1000, 'SUCCESS')
            """)
            close()
        }

        // When: 执行Migration
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 15, true, 
            DatabaseModule.MIGRATION_14_15
        )

        // Then: 验证列约束
        val cursor = db.query("PRAGMA table_info(ai_advisor_conversations)")
        var foundColumn = false
        var isNotNull = false
        
        while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndex("name"))
            if (name == "related_user_message_id") {
                foundColumn = true
                isNotNull = cursor.getInt(cursor.getColumnIndex("notnull")) == 1
            }
        }
        cursor.close()

        assertTrue("related_user_message_id column should exist", foundColumn)
        assertTrue("related_user_message_id should be NOT NULL", isNotNull)
    }

    /**
     * TC-MIG-002: 验证现有数据迁移后related_user_message_id为空字符串
     */
    @Test
    @Throws(IOException::class)
    fun migration14To15_existingDataShouldHaveEmptyRelatedUserMessageId() {
        // Given: 创建v14数据库并插入数据
        helper.createDatabase(TEST_DB, 14).apply {
            execSQL("""
                INSERT INTO profiles (id, name, target_goal, context_depth, facts_json, relationship_score)
                VALUES ('contact-1', 'Test Contact', 'Test Goal', 3, '[]', 50)
            """)
            execSQL("""
                INSERT INTO ai_advisor_sessions (id, contact_id, title, created_at, updated_at, message_count, is_active)
                VALUES ('session-1', 'contact-1', 'Test Session', 1000, 1000, 0, 1)
            """)
            execSQL("""
                INSERT INTO ai_advisor_conversations 
                (id, contact_id, session_id, message_type, content, timestamp, created_at, send_status)
                VALUES ('msg-1', 'contact-1', 'session-1', 'USER', 'Hello', 1000, 1000, 'SUCCESS')
            """)
            close()
        }

        // When: 执行Migration
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 15, true,
            DatabaseModule.MIGRATION_14_15
        )

        // Then: 验证数据迁移
        val cursor = db.query(
            "SELECT related_user_message_id FROM ai_advisor_conversations WHERE id = 'msg-1'"
        )
        assertTrue("Should have data", cursor.moveToFirst())
        assertEquals("related_user_message_id should be empty string", "", cursor.getString(0))
        cursor.close()
    }

    /**
     * TC-MIG-003: 验证Migration后可以插入新数据
     */
    @Test
    @Throws(IOException::class)
    fun migration14To15_shouldAllowInsertingNewData() {
        // Given: 创建v14数据库
        helper.createDatabase(TEST_DB, 14).apply {
            execSQL("""
                INSERT INTO profiles (id, name, target_goal, context_depth, facts_json, relationship_score)
                VALUES ('contact-1', 'Test Contact', 'Test Goal', 3, '[]', 50)
            """)
            execSQL("""
                INSERT INTO ai_advisor_sessions (id, contact_id, title, created_at, updated_at, message_count, is_active)
                VALUES ('session-1', 'contact-1', 'Test Session', 1000, 1000, 0, 1)
            """)
            close()
        }

        // When: 执行Migration
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 15, true,
            DatabaseModule.MIGRATION_14_15
        )

        // Then: 插入新数据应该成功
        db.execSQL("""
            INSERT INTO ai_advisor_conversations 
            (id, contact_id, session_id, message_type, content, timestamp, created_at, send_status, related_user_message_id)
            VALUES ('msg-new', 'contact-1', 'session-1', 'AI', 'Response', 2000, 2000, 'SUCCESS', 'msg-user-1')
        """)

        val cursor = db.query(
            "SELECT related_user_message_id FROM ai_advisor_conversations WHERE id = 'msg-new'"
        )
        assertTrue(cursor.moveToFirst())
        assertEquals("msg-user-1", cursor.getString(0))
        cursor.close()
    }

    /**
     * TC-MIG-004: 验证不能插入NULL值到related_user_message_id
     */
    @Test
    @Throws(IOException::class)
    fun migration14To15_shouldNotAllowNullRelatedUserMessageId() {
        // Given: 创建v14数据库
        helper.createDatabase(TEST_DB, 14).apply {
            execSQL("""
                INSERT INTO profiles (id, name, target_goal, context_depth, facts_json, relationship_score)
                VALUES ('contact-1', 'Test Contact', 'Test Goal', 3, '[]', 50)
            """)
            execSQL("""
                INSERT INTO ai_advisor_sessions (id, contact_id, title, created_at, updated_at, message_count, is_active)
                VALUES ('session-1', 'contact-1', 'Test Session', 1000, 1000, 0, 1)
            """)
            close()
        }

        // When: 执行Migration
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 15, true,
            DatabaseModule.MIGRATION_14_15
        )

        // Then: 插入NULL值应该失败
        try {
            db.execSQL("""
                INSERT INTO ai_advisor_conversations 
                (id, contact_id, session_id, message_type, content, timestamp, created_at, send_status, related_user_message_id)
                VALUES ('msg-null', 'contact-1', 'session-1', 'AI', 'Response', 2000, 2000, 'SUCCESS', NULL)
            """)
            fail("Should throw exception when inserting NULL into NOT NULL column")
        } catch (e: Exception) {
            // Expected: NOT NULL constraint violation
            assertTrue(e.message?.contains("NOT NULL") == true || e.message?.contains("null") == true)
        }
    }

    /**
     * TC-MIG-005: 验证索引保持完整
     */
    @Test
    @Throws(IOException::class)
    fun migration14To15_shouldPreserveIndices() {
        // Given: 创建v14数据库
        helper.createDatabase(TEST_DB, 14).apply {
            close()
        }

        // When: 执行Migration
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 15, true,
            DatabaseModule.MIGRATION_14_15
        )

        // Then: 验证索引存在
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='ai_advisor_conversations'")
        val indices = mutableSetOf<String>()
        while (cursor.moveToNext()) {
            indices.add(cursor.getString(0))
        }
        cursor.close()

        assertTrue("Should have contact_id index", indices.any { it.contains("contact_id") })
        assertTrue("Should have session_id index", indices.any { it.contains("session_id") })
        assertTrue("Should have timestamp index", indices.any { it.contains("timestamp") })
    }
}
```

---

## 3. 人工测试用例

### 3.1 安装测试

| 测试ID | 测试场景 | 操作步骤 | 预期结果 | 实际结果 | 状态 |
|--------|----------|----------|----------|----------|------|
| MT-001 | 全新安装 | 1. 卸载应用<br>2. 安装修复后版本<br>3. 启动应用 | 应用正常启动，无错误 | | ⬜ |
| MT-002 | v14升级到v15 | 1. 安装v14版本<br>2. 使用AI军师功能创建对话<br>3. 升级到修复后版本<br>4. 启动应用 | 应用正常启动，数据保留 | | ⬜ |
| MT-003 | 验证数据完整性 | 1. 完成MT-002<br>2. 打开AI军师对话<br>3. 检查历史消息 | 所有历史消息正常显示 | | ⬜ |

### 3.2 功能测试

| 测试ID | 测试场景 | 操作步骤 | 预期结果 | 实际结果 | 状态 |
|--------|----------|----------|----------|----------|------|
| MT-004 | 发送新消息 | 1. 打开AI军师对话<br>2. 发送新消息<br>3. 等待AI回复 | 消息正常发送和接收 | | ⬜ |
| MT-005 | 终止后重新生成 | 1. 发送消息<br>2. AI回复时点击停止<br>3. 点击重新生成 | 使用正确的用户输入重新生成 | | ⬜ |
| MT-006 | 多轮对话 | 1. 进行3轮对话<br>2. 检查所有消息 | 所有消息正常显示，角色正确 | | ⬜ |

---

## 4. 测试执行记录

### 4.1 单元测试结果

| 测试ID | 测试名称 | 结果 | 备注 |
|--------|----------|------|------|
| TC-MIG-001 | NOT NULL约束验证 | ⬜ 待执行 | |
| TC-MIG-002 | 数据迁移验证 | ⬜ 待执行 | |
| TC-MIG-003 | 新数据插入验证 | ⬜ 待执行 | |
| TC-MIG-004 | NULL值拒绝验证 | ⬜ 待执行 | |
| TC-MIG-005 | 索引完整性验证 | ⬜ 待执行 | |

### 4.2 人工测试结果

| 测试ID | 执行日期 | 执行人 | 结果 | 备注 |
|--------|----------|--------|------|------|
| MT-001 | 2026-01-06 | Kiro | ✅ 通过 | 清除数据后全新安装，应用正常启动，无Migration错误 |
| MT-002 | | | ⬜ 待执行 | 需要v14版本APK进行升级测试 |
| MT-003 | | | ⬜ 待执行 | |
| MT-004 | | | ⬜ 待执行 | |
| MT-005 | | | ⬜ 待执行 | |
| MT-006 | | | ⬜ 待执行 | |

---

## 5. 测试结果汇总

| 测试类型 | 总数 | 通过 | 失败 | 待执行 |
|----------|------|------|------|--------|
| 单元测试 | 5 | 0 | 0 | 5 |
| 人工测试 | 6 | 1 | 0 | 5 |
| **总计** | **11** | **1** | **0** | **10** |

---

## 6. 验证清单

- [ ] MIGRATION_14_15脚本已修改，添加NOT NULL约束
- [ ] 单元测试全部通过
- [ ] 全新安装测试通过
- [ ] 升级安装测试通过
- [ ] AI军师功能正常工作
- [ ] 终止后重新生成功能正常

---

**文档版本**: 1.0
**创建日期**: 2026-01-06
**测试状态**: ⬜ 待执行
