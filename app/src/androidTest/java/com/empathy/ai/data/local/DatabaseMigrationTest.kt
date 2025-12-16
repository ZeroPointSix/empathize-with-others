package com.empathy.ai.data.local

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * 数据库迁移测试
 *
 * 测试内容：
 * - 各版本迁移脚本的正确性
 * - 数据完整性验证
 * - 索引创建验证
 *
 * 参考标准：
 * - [TD-00004] T078 DatabaseMigrationTest
 * - [TDD-00004] 联系人画像记忆系统UI架构设计
 * - [TD-001] Room数据库迁移策略
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val testDbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * 迁移脚本: 版本 1 -> 2
     * 添加 ai_providers 表
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ai_providers (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    base_url TEXT NOT NULL,
                    api_key_ref TEXT NOT NULL,
                    models_json TEXT NOT NULL,
                    default_model_id TEXT NOT NULL,
                    is_default INTEGER NOT NULL DEFAULT 0,
                    timeout_ms INTEGER NOT NULL DEFAULT 30000,
                    created_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS idx_ai_providers_is_default 
                ON ai_providers(is_default)
                """.trimIndent()
            )
        }
    }

    /**
     * 迁移脚本: 版本 2 -> 3
     * 为 ai_providers 表添加 timeout_ms 字段
     */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE ai_providers 
                ADD COLUMN timeout_ms INTEGER NOT NULL DEFAULT 30000
                """.trimIndent()
            )
        }
    }

    /**
     * 迁移脚本: 版本 3 -> 4
     * 添加记忆系统相关表和字段
     */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. 创建conversation_logs表
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS conversation_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    contact_id TEXT NOT NULL,
                    user_input TEXT NOT NULL,
                    ai_response TEXT,
                    timestamp INTEGER NOT NULL,
                    is_summarized INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(contact_id) REFERENCES profiles(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_conv_contact ON conversation_logs(contact_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_conv_timestamp ON conversation_logs(timestamp)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_conv_summarized ON conversation_logs(is_summarized)")

            // 2. 创建daily_summaries表
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS daily_summaries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    contact_id TEXT NOT NULL,
                    summary_date TEXT NOT NULL,
                    content TEXT NOT NULL,
                    key_events_json TEXT NOT NULL,
                    relationship_score INTEGER NOT NULL,
                    created_at INTEGER NOT NULL,
                    FOREIGN KEY(contact_id) REFERENCES profiles(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_summary_contact ON daily_summaries(contact_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_summary_date ON daily_summaries(summary_date)")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_summary_contact_date " +
                    "ON daily_summaries(contact_id, summary_date)"
            )

            // 3. 添加profiles新字段
            db.execSQL("ALTER TABLE profiles ADD COLUMN relationship_score INTEGER NOT NULL DEFAULT 50")
            db.execSQL("ALTER TABLE profiles ADD COLUMN last_interaction_date TEXT")
        }
    }

    /**
     * 迁移脚本: 版本 4 -> 5
     * 添加失败任务表
     */
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS failed_summary_tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    contact_id TEXT NOT NULL,
                    summary_date TEXT NOT NULL,
                    failure_reason TEXT NOT NULL,
                    retry_count INTEGER NOT NULL DEFAULT 0,
                    failed_at INTEGER NOT NULL,
                    last_retry_at INTEGER
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_failed_contact ON failed_summary_tasks(contact_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_failed_at ON failed_summary_tasks(failed_at)")
        }
    }

    /**
     * 测试迁移 1 -> 2
     * 验证ai_providers表创建
     */
    @Test
    @Throws(IOException::class)
    fun migrate1To2_createsAiProvidersTable() {
        // 创建版本1数据库
        helper.createDatabase(testDbName, 1).apply {
            // 插入测试数据到profiles表
            execSQL(
                """
                INSERT INTO profiles (id, name, nickname, relationship, notes)
                VALUES ('test_id', 'Test Name', 'Test Nick', 'Friend', 'Test Notes')
                """.trimIndent()
            )
            close()
        }

        // 执行迁移
        val db = helper.runMigrationsAndValidate(testDbName, 2, true, MIGRATION_1_2)

        // 验证ai_providers表存在
        val cursor = db.query("SELECT * FROM ai_providers")
        assertTrue("ai_providers表应该存在", cursor.columnCount > 0)
        cursor.close()

        // 验证原有数据完整
        val profileCursor = db.query("SELECT * FROM profiles WHERE id = 'test_id'")
        assertTrue("原有数据应该保留", profileCursor.moveToFirst())
        assertEquals("Test Name", profileCursor.getString(profileCursor.getColumnIndex("name")))
        profileCursor.close()

        db.close()
    }

    /**
     * 测试迁移 2 -> 3
     * 验证timeout_ms字段添加
     */
    @Test
    @Throws(IOException::class)
    fun migrate2To3_addsTimeoutField() {
        // 创建版本2数据库
        helper.createDatabase(testDbName, 2).apply {
            // 插入测试数据
            execSQL(
                """
                INSERT INTO ai_providers (id, name, base_url, api_key_ref, models_json, default_model_id, is_default, created_at)
                VALUES ('provider_1', 'Test Provider', 'https://api.test.com', 'key_ref', '[]', 'model_1', 1, ${System.currentTimeMillis()})
                """.trimIndent()
            )
            close()
        }

        // 执行迁移
        val db = helper.runMigrationsAndValidate(testDbName, 3, true, MIGRATION_2_3)

        // 验证timeout_ms字段存在且有默认值
        val cursor = db.query("SELECT timeout_ms FROM ai_providers WHERE id = 'provider_1'")
        assertTrue("应该能查询到数据", cursor.moveToFirst())
        assertEquals("默认超时应该是30000ms", 30000, cursor.getInt(0))
        cursor.close()

        db.close()
    }

    /**
     * 测试迁移 3 -> 4
     * 验证记忆系统表创建
     */
    @Test
    @Throws(IOException::class)
    fun migrate3To4_createsMemoryTables() {
        // 创建版本3数据库
        helper.createDatabase(testDbName, 3).apply {
            // 插入测试联系人
            execSQL(
                """
                INSERT INTO profiles (id, name, nickname, relationship, notes)
                VALUES ('contact_1', 'Test Contact', 'TC', 'Friend', 'Notes')
                """.trimIndent()
            )
            close()
        }

        // 执行迁移
        val db = helper.runMigrationsAndValidate(testDbName, 4, true, MIGRATION_3_4)

        // 验证conversation_logs表存在
        val convCursor = db.query("SELECT * FROM conversation_logs")
        assertTrue("conversation_logs表应该存在", convCursor.columnCount > 0)
        convCursor.close()

        // 验证daily_summaries表存在
        val summaryCursor = db.query("SELECT * FROM daily_summaries")
        assertTrue("daily_summaries表应该存在", summaryCursor.columnCount > 0)
        summaryCursor.close()

        // 验证profiles新字段
        val profileCursor = db.query("SELECT relationship_score, last_interaction_date FROM profiles WHERE id = 'contact_1'")
        assertTrue("应该能查询到数据", profileCursor.moveToFirst())
        assertEquals("默认关系分数应该是50", 50, profileCursor.getInt(0))
        profileCursor.close()

        db.close()
    }

    /**
     * 测试迁移 4 -> 5
     * 验证失败任务表创建
     */
    @Test
    @Throws(IOException::class)
    fun migrate4To5_createsFailedTasksTable() {
        // 创建版本4数据库
        helper.createDatabase(testDbName, 4).apply {
            close()
        }

        // 执行迁移
        val db = helper.runMigrationsAndValidate(testDbName, 5, true, MIGRATION_4_5)

        // 验证failed_summary_tasks表存在
        val cursor = db.query("SELECT * FROM failed_summary_tasks")
        assertTrue("failed_summary_tasks表应该存在", cursor.columnCount > 0)
        cursor.close()

        // 验证可以插入数据
        db.execSQL(
            """
            INSERT INTO failed_summary_tasks (contact_id, summary_date, failure_reason, retry_count, failed_at)
            VALUES ('contact_1', '2025-12-15', 'Test failure', 0, ${System.currentTimeMillis()})
            """.trimIndent()
        )

        val insertCursor = db.query("SELECT * FROM failed_summary_tasks WHERE contact_id = 'contact_1'")
        assertTrue("应该能查询到插入的数据", insertCursor.moveToFirst())
        insertCursor.close()

        db.close()
    }

    /**
     * 测试完整迁移链 1 -> 5
     * 验证所有迁移脚本可以连续执行
     */
    @Test
    @Throws(IOException::class)
    fun migrateAll_1To5_succeeds() {
        // 创建版本1数据库
        helper.createDatabase(testDbName, 1).apply {
            // 插入测试数据
            execSQL(
                """
                INSERT INTO profiles (id, name, nickname, relationship, notes)
                VALUES ('contact_1', 'Test Contact', 'TC', 'Friend', 'Notes')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO brain_tags (id, contact_id, content, type, is_confirmed, source)
                VALUES (1, 'contact_1', 'Test Tag', 'RISK_RED', 1, 'manual')
                """.trimIndent()
            )
            close()
        }

        // 执行所有迁移
        val db = helper.runMigrationsAndValidate(
            testDbName, 5, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5
        )

        // 验证原有数据完整
        val profileCursor = db.query("SELECT * FROM profiles WHERE id = 'contact_1'")
        assertTrue("联系人数据应该保留", profileCursor.moveToFirst())
        assertEquals("Test Contact", profileCursor.getString(profileCursor.getColumnIndex("name")))
        profileCursor.close()

        val tagCursor = db.query("SELECT * FROM brain_tags WHERE contact_id = 'contact_1'")
        assertTrue("标签数据应该保留", tagCursor.moveToFirst())
        assertEquals("Test Tag", tagCursor.getString(tagCursor.getColumnIndex("content")))
        tagCursor.close()

        // 验证所有新表存在
        val tables = listOf("ai_providers", "conversation_logs", "daily_summaries", "failed_summary_tasks")
        for (table in tables) {
            val cursor = db.query("SELECT * FROM $table")
            assertTrue("$table 表应该存在", cursor.columnCount > 0)
            cursor.close()
        }

        db.close()
    }

    /**
     * 测试索引创建
     * 验证所有索引正确创建
     */
    @Test
    @Throws(IOException::class)
    fun migration_createsIndexes() {
        // 创建版本1数据库并迁移到版本5
        helper.createDatabase(testDbName, 1).apply {
            close()
        }

        val db = helper.runMigrationsAndValidate(
            testDbName, 5, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5
        )

        // 查询索引信息
        val indexCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name IN " +
                "('ai_providers', 'conversation_logs', 'daily_summaries', 'failed_summary_tasks')"
        )

        val indexes = mutableListOf<String>()
        while (indexCursor.moveToNext()) {
            indexes.add(indexCursor.getString(0))
        }
        indexCursor.close()

        // 验证关键索引存在
        assertTrue("应该有ai_providers索引", indexes.any { it.contains("ai_providers") })
        assertTrue("应该有conversation_logs索引", indexes.any { it.contains("conv") })
        assertTrue("应该有daily_summaries索引", indexes.any { it.contains("summary") })
        assertTrue("应该有failed_summary_tasks索引", indexes.any { it.contains("failed") })

        db.close()
    }

    /**
     * 迁移脚本: 版本 5 -> 6
     * 添加UI扩展字段
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE profiles ADD COLUMN avatar_url TEXT")
            db.execSQL("ALTER TABLE brain_tags ADD COLUMN is_confirmed INTEGER NOT NULL DEFAULT 1")
        }
    }

    /**
     * 迁移脚本: 版本 6 -> 7
     * 修复性迁移
     */
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 修复索引名称
            db.execSQL("DROP INDEX IF EXISTS idx_conv_contact")
            db.execSQL("DROP INDEX IF EXISTS idx_conv_timestamp")
            db.execSQL("DROP INDEX IF EXISTS idx_conv_summarized")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_conversation_logs_contact_id ON conversation_logs(contact_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_conversation_logs_timestamp ON conversation_logs(timestamp)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_conversation_logs_is_summarized ON conversation_logs(is_summarized)")
            db.execSQL("DROP INDEX IF EXISTS idx_failed_contact")
            db.execSQL("DROP INDEX IF EXISTS idx_failed_at")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_failed_summary_tasks_contact_id ON failed_summary_tasks(contact_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_failed_summary_tasks_failed_at ON failed_summary_tasks(failed_at)")
        }
    }

    /**
     * 迁移脚本: 版本 7 -> 8
     * 添加提示词管理系统字段
     */
    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE profiles ADD COLUMN custom_prompt TEXT DEFAULT NULL")
        }
    }

    /**
     * 测试迁移 5 -> 6
     * 验证UI扩展字段添加
     */
    @Test
    @Throws(IOException::class)
    fun migrate5To6_addsUiExtensionFields() {
        // 创建版本5数据库
        helper.createDatabase(testDbName, 5).apply {
            execSQL(
                """
                INSERT INTO profiles (id, name, nickname, relationship, notes, relationship_score)
                VALUES ('contact_1', 'Test', 'T', 'Friend', 'Notes', 50)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO brain_tags (id, contact_id, content, type, source)
                VALUES (1, 'contact_1', 'Test Tag', 'RISK_RED', 'manual')
                """.trimIndent()
            )
            close()
        }

        // 执行迁移
        val db = helper.runMigrationsAndValidate(testDbName, 6, true, MIGRATION_5_6)

        // 验证avatar_url字段存在
        val profileCursor = db.query("SELECT avatar_url FROM profiles WHERE id = 'contact_1'")
        assertTrue("应该能查询avatar_url字段", profileCursor.moveToFirst())
        profileCursor.close()

        // 验证is_confirmed字段存在且有默认值
        val tagCursor = db.query("SELECT is_confirmed FROM brain_tags WHERE id = 1")
        assertTrue("应该能查询is_confirmed字段", tagCursor.moveToFirst())
        assertEquals("默认is_confirmed应该是1", 1, tagCursor.getInt(0))
        tagCursor.close()

        db.close()
    }

    /**
     * 测试迁移 6 -> 7
     * 验证索引修复
     */
    @Test
    @Throws(IOException::class)
    fun migrate6To7_fixesIndexNames() {
        // 创建版本6数据库
        helper.createDatabase(testDbName, 6).apply {
            close()
        }

        // 执行迁移
        val db = helper.runMigrationsAndValidate(testDbName, 7, true, MIGRATION_6_7)

        // 验证新索引存在
        val indexCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'index_conversation_logs%'"
        )
        val indexes = mutableListOf<String>()
        while (indexCursor.moveToNext()) {
            indexes.add(indexCursor.getString(0))
        }
        indexCursor.close()

        assertTrue("应该有conversation_logs索引", indexes.isNotEmpty())
        db.close()
    }

    /**
     * 测试迁移 7 -> 8
     * 验证custom_prompt字段添加
     */
    @Test
    @Throws(IOException::class)
    fun migrate7To8_addsCustomPromptField() {
        // 创建版本7数据库
        helper.createDatabase(testDbName, 7).apply {
            execSQL(
                """
                INSERT INTO profiles (id, name, nickname, relationship, notes, relationship_score)
                VALUES ('contact_1', 'Test', 'T', 'Friend', 'Notes', 50)
                """.trimIndent()
            )
            close()
        }

        // 执行迁移
        val db = helper.runMigrationsAndValidate(testDbName, 8, true, MIGRATION_7_8)

        // 验证custom_prompt字段存在
        val cursor = db.query("SELECT custom_prompt FROM profiles WHERE id = 'contact_1'")
        assertTrue("应该能查询custom_prompt字段", cursor.moveToFirst())
        assertTrue("默认custom_prompt应该是NULL", cursor.isNull(0))
        cursor.close()

        // 验证可以更新custom_prompt
        db.execSQL("UPDATE profiles SET custom_prompt = '自定义提示词' WHERE id = 'contact_1'")
        val updateCursor = db.query("SELECT custom_prompt FROM profiles WHERE id = 'contact_1'")
        assertTrue(updateCursor.moveToFirst())
        assertEquals("自定义提示词", updateCursor.getString(0))
        updateCursor.close()

        db.close()
    }

    /**
     * 测试完整迁移链 1 -> 8
     * 验证所有迁移脚本可以连续执行
     */
    @Test
    @Throws(IOException::class)
    fun migrateAll_1To8_succeeds() {
        // 创建版本1数据库
        helper.createDatabase(testDbName, 1).apply {
            execSQL(
                """
                INSERT INTO profiles (id, name, nickname, relationship, notes)
                VALUES ('contact_1', 'Test Contact', 'TC', 'Friend', 'Notes')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO brain_tags (id, contact_id, content, type, source)
                VALUES (1, 'contact_1', 'Test Tag', 'RISK_RED', 'manual')
                """.trimIndent()
            )
            close()
        }

        // 执行所有迁移
        val db = helper.runMigrationsAndValidate(
            testDbName, 8, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
            MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8
        )

        // 验证原有数据完整
        val profileCursor = db.query("SELECT * FROM profiles WHERE id = 'contact_1'")
        assertTrue("联系人数据应该保留", profileCursor.moveToFirst())
        assertEquals("Test Contact", profileCursor.getString(profileCursor.getColumnIndex("name")))
        profileCursor.close()

        // 验证所有新字段存在
        val fieldCursor = db.query(
            "SELECT avatar_url, custom_prompt, relationship_score FROM profiles WHERE id = 'contact_1'"
        )
        assertTrue(fieldCursor.moveToFirst())
        fieldCursor.close()

        // 验证所有新表存在
        val tables = listOf(
            "ai_providers", "conversation_logs", "daily_summaries", "failed_summary_tasks"
        )
        for (table in tables) {
            val cursor = db.query("SELECT * FROM $table")
            assertTrue("$table 表应该存在", cursor.columnCount > 0)
            cursor.close()
        }

        db.close()
    }

    /**
     * 测试数据完整性
     * 验证迁移后数据关系完整
     */
    @Test
    @Throws(IOException::class)
    fun migration_preservesDataIntegrity() {
        // 创建版本1数据库
        helper.createDatabase(testDbName, 1).apply {
            // 插入联系人
            execSQL(
                """
                INSERT INTO profiles (id, name, nickname, relationship, notes)
                VALUES ('contact_1', 'Alice', 'A', 'Friend', 'Notes')
                """.trimIndent()
            )
            // 插入多个标签
            execSQL(
                """
                INSERT INTO brain_tags (id, contact_id, content, type, is_confirmed, source)
                VALUES (1, 'contact_1', 'Tag 1', 'RISK_RED', 1, 'manual')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO brain_tags (id, contact_id, content, type, is_confirmed, source)
                VALUES (2, 'contact_1', 'Tag 2', 'STRATEGY_GREEN', 0, 'ai')
                """.trimIndent()
            )
            close()
        }

        // 执行所有迁移
        val db = helper.runMigrationsAndValidate(
            testDbName, 5, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5
        )

        // 验证联系人数据
        val profileCursor = db.query("SELECT COUNT(*) FROM profiles")
        profileCursor.moveToFirst()
        assertEquals("应该有1个联系人", 1, profileCursor.getInt(0))
        profileCursor.close()

        // 验证标签数据
        val tagCursor = db.query("SELECT COUNT(*) FROM brain_tags WHERE contact_id = 'contact_1'")
        tagCursor.moveToFirst()
        assertEquals("应该有2个标签", 2, tagCursor.getInt(0))
        tagCursor.close()

        // 验证外键关系（通过插入conversation_log测试）
        db.execSQL(
            """
            INSERT INTO conversation_logs (contact_id, user_input, timestamp, is_summarized)
            VALUES ('contact_1', 'Hello', ${System.currentTimeMillis()}, 0)
            """.trimIndent()
        )

        val convCursor = db.query("SELECT COUNT(*) FROM conversation_logs WHERE contact_id = 'contact_1'")
        convCursor.moveToFirst()
        assertEquals("应该有1条对话记录", 1, convCursor.getInt(0))
        convCursor.close()

        db.close()
    }
}
