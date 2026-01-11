package com.empathy.ai.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.empathy.ai.data.di.DatabaseModule
import org.junit.Assume.assumeTrue
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * BUG-00048-V5 Migration测试
 * 
 * 验证MIGRATION_14_15正确创建related_user_message_id列，带有NOT NULL约束
 * 
 * 问题背景：
 * - Entity定义: @ColumnInfo(name = "related_user_message_id", defaultValue = "") → notNull = true
 * - 原Migration: ALTER TABLE ADD COLUMN ... DEFAULT '' → notNull = false
 * - 修复后: ALTER TABLE ADD COLUMN ... NOT NULL DEFAULT '' → notNull = true
 */
@RunWith(AndroidJUnit4::class)
class Migration14To15Test {

    companion object {
        private const val TEST_DB = "migration-14-15-test"
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    private fun hasSchema(version: Int): Boolean {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val path = "com.empathy.ai.data.local.AppDatabase/$version.json"
        return try {
            assets.open(path).close()
            true
        } catch (e: IOException) {
            false
        }
    }

    /**
     * TC-MIG-001: 验证related_user_message_id列的NOT NULL约束
     */
    @Test
    @Throws(IOException::class)
    fun migration14To15_shouldCreateColumnWithNotNullConstraint() {
        assumeTrue("缺少schema 14/15，跳过迁移测试", hasSchema(14) && hasSchema(15))
        // Given: 创建v14数据库
        helper.createDatabase(TEST_DB, 14).apply {
            // 插入测试数据（需要先创建依赖的表数据）
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
            val nameIndex = cursor.getColumnIndex("name")
            val notNullIndex = cursor.getColumnIndex("notnull")
            if (nameIndex >= 0 && cursor.getString(nameIndex) == "related_user_message_id") {
                foundColumn = true
                isNotNull = notNullIndex >= 0 && cursor.getInt(notNullIndex) == 1
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
        assumeTrue("缺少schema 14/15，跳过迁移测试", hasSchema(14) && hasSchema(15))
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
        assumeTrue("缺少schema 14/15，跳过迁移测试", hasSchema(14) && hasSchema(15))
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
     * TC-MIG-004: 验证索引保持完整
     */
    @Test
    @Throws(IOException::class)
    fun migration14To15_shouldPreserveIndices() {
        assumeTrue("缺少schema 14/15，跳过迁移测试", hasSchema(14) && hasSchema(15))
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
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='ai_advisor_conversations'"
        )
        val indices = mutableSetOf<String>()
        while (cursor.moveToNext()) {
            indices.add(cursor.getString(0))
        }
        cursor.close()

        assertTrue("Should have contact_id index", indices.any { it.contains("contact_id") })
        assertTrue("Should have session_id index", indices.any { it.contains("session_id") })
        assertTrue("Should have timestamp index", indices.any { it.contains("timestamp") })
    }

    /**
     * TC-MIG-005: 验证多条数据迁移
     */
    @Test
    @Throws(IOException::class)
    fun migration14To15_shouldMigrateMultipleRecords() {
        assumeTrue("缺少schema 14/15，跳过迁移测试", hasSchema(14) && hasSchema(15))
        // Given: 创建v14数据库并插入多条数据
        helper.createDatabase(TEST_DB, 14).apply {
            execSQL("""
                INSERT INTO profiles (id, name, target_goal, context_depth, facts_json, relationship_score)
                VALUES ('contact-1', 'Test Contact', 'Test Goal', 3, '[]', 50)
            """)
            execSQL("""
                INSERT INTO ai_advisor_sessions (id, contact_id, title, created_at, updated_at, message_count, is_active)
                VALUES ('session-1', 'contact-1', 'Test Session', 1000, 1000, 0, 1)
            """)
            // 插入多条对话记录
            for (i in 1..5) {
                execSQL("""
                    INSERT INTO ai_advisor_conversations 
                    (id, contact_id, session_id, message_type, content, timestamp, created_at, send_status)
                    VALUES ('msg-$i', 'contact-1', 'session-1', '${if (i % 2 == 1) "USER" else "AI"}', 'Message $i', ${1000 + i * 100}, ${1000 + i * 100}, 'SUCCESS')
                """)
            }
            close()
        }

        // When: 执行Migration
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 15, true,
            DatabaseModule.MIGRATION_14_15
        )

        // Then: 验证所有数据都有related_user_message_id字段
        val cursor = db.query("SELECT id, related_user_message_id FROM ai_advisor_conversations")
        var count = 0
        while (cursor.moveToNext()) {
            count++
            val relatedId = cursor.getString(1)
            assertEquals("All records should have empty related_user_message_id", "", relatedId)
        }
        cursor.close()
        assertEquals("Should have 5 records", 5, count)
    }
}
