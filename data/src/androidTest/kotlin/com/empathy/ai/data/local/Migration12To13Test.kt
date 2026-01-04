package com.empathy.ai.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.empathy.ai.data.di.DatabaseModule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * 数据库迁移测试：v12 → v13
 *
 * 测试AI军师功能相关表的创建：
 * - ai_advisor_sessions 会话表
 * - ai_advisor_conversations 对话记录表
 */
@RunWith(AndroidJUnit4::class)
class Migration12To13Test {

    private val testDbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate12To13_createsAiAdvisorSessionsTable() {
        // Create database at version 12
        helper.createDatabase(testDbName, 12).apply {
            close()
        }

        // Run migration and validate
        val db = helper.runMigrationsAndValidate(
            testDbName,
            13,
            true,
            DatabaseModule.MIGRATION_12_13
        )

        // Verify ai_advisor_sessions table exists
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='ai_advisor_sessions'"
        )
        assert(cursor.count == 1) { "ai_advisor_sessions table should exist" }
        cursor.close()

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate12To13_createsAiAdvisorConversationsTable() {
        // Create database at version 12
        helper.createDatabase(testDbName, 12).apply {
            close()
        }

        // Run migration and validate
        val db = helper.runMigrationsAndValidate(
            testDbName,
            13,
            true,
            DatabaseModule.MIGRATION_12_13
        )

        // Verify ai_advisor_conversations table exists
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='ai_advisor_conversations'"
        )
        assert(cursor.count == 1) { "ai_advisor_conversations table should exist" }
        cursor.close()

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate12To13_createsSessionsIndexes() {
        // Create database at version 12
        helper.createDatabase(testDbName, 12).apply {
            close()
        }

        // Run migration and validate
        val db = helper.runMigrationsAndValidate(
            testDbName,
            13,
            true,
            DatabaseModule.MIGRATION_12_13
        )

        // Verify indexes exist for ai_advisor_sessions
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='ai_advisor_sessions'"
        )
        // Should have at least contact_id, created_at, updated_at indexes
        assert(cursor.count >= 3) { "ai_advisor_sessions should have at least 3 indexes" }
        cursor.close()

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate12To13_createsConversationsIndexes() {
        // Create database at version 12
        helper.createDatabase(testDbName, 12).apply {
            close()
        }

        // Run migration and validate
        val db = helper.runMigrationsAndValidate(
            testDbName,
            13,
            true,
            DatabaseModule.MIGRATION_12_13
        )

        // Verify indexes exist for ai_advisor_conversations
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='ai_advisor_conversations'"
        )
        // Should have at least contact_id, session_id, timestamp indexes
        assert(cursor.count >= 3) { "ai_advisor_conversations should have at least 3 indexes" }
        cursor.close()

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate12To13_sessionsTableHasCorrectColumns() {
        // Create database at version 12
        helper.createDatabase(testDbName, 12).apply {
            close()
        }

        // Run migration and validate
        val db = helper.runMigrationsAndValidate(
            testDbName,
            13,
            true,
            DatabaseModule.MIGRATION_12_13
        )

        // Verify columns in ai_advisor_sessions
        val cursor = db.query("PRAGMA table_info(ai_advisor_sessions)")
        val columns = mutableListOf<String>()
        while (cursor.moveToNext()) {
            columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()

        val expectedColumns = listOf(
            "id", "contact_id", "title", "created_at", "updated_at", "message_count", "is_active"
        )
        expectedColumns.forEach { column ->
            assert(columns.contains(column)) { "Column $column should exist in ai_advisor_sessions" }
        }

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate12To13_conversationsTableHasCorrectColumns() {
        // Create database at version 12
        helper.createDatabase(testDbName, 12).apply {
            close()
        }

        // Run migration and validate
        val db = helper.runMigrationsAndValidate(
            testDbName,
            13,
            true,
            DatabaseModule.MIGRATION_12_13
        )

        // Verify columns in ai_advisor_conversations
        val cursor = db.query("PRAGMA table_info(ai_advisor_conversations)")
        val columns = mutableListOf<String>()
        while (cursor.moveToNext()) {
            columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()

        val expectedColumns = listOf(
            "id", "contact_id", "session_id", "message_type", "content",
            "timestamp", "created_at", "send_status"
        )
        expectedColumns.forEach { column ->
            assert(columns.contains(column)) { "Column $column should exist in ai_advisor_conversations" }
        }

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate12To13_canInsertAndQuerySession() {
        // Create database at version 12
        helper.createDatabase(testDbName, 12).apply {
            close()
        }

        // Run migration
        val db = helper.runMigrationsAndValidate(
            testDbName,
            13,
            true,
            DatabaseModule.MIGRATION_12_13
        )

        // Insert a test session (without foreign key check for simplicity)
        db.execSQL("""
            INSERT INTO ai_advisor_sessions (id, contact_id, title, created_at, updated_at, message_count, is_active)
            VALUES ('test-session-1', 'test-contact-1', 'Test Session', 1704067200000, 1704067200000, 0, 1)
        """)

        // Query and verify
        val cursor = db.query("SELECT * FROM ai_advisor_sessions WHERE id = 'test-session-1'")
        assert(cursor.count == 1) { "Should be able to query inserted session" }
        cursor.moveToFirst()
        assert(cursor.getString(cursor.getColumnIndexOrThrow("title")) == "Test Session")
        cursor.close()

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate12To13_canInsertAndQueryConversation() {
        // Create database at version 12
        helper.createDatabase(testDbName, 12).apply {
            close()
        }

        // Run migration
        val db = helper.runMigrationsAndValidate(
            testDbName,
            13,
            true,
            DatabaseModule.MIGRATION_12_13
        )

        // Insert test data (without foreign key check for simplicity)
        db.execSQL("""
            INSERT INTO ai_advisor_sessions (id, contact_id, title, created_at, updated_at, message_count, is_active)
            VALUES ('test-session-1', 'test-contact-1', 'Test Session', 1704067200000, 1704067200000, 0, 1)
        """)

        db.execSQL("""
            INSERT INTO ai_advisor_conversations (id, contact_id, session_id, message_type, content, timestamp, created_at, send_status)
            VALUES ('test-conv-1', 'test-contact-1', 'test-session-1', 'USER', 'Hello AI', 1704067200000, 1704067200000, 'SUCCESS')
        """)

        // Query and verify
        val cursor = db.query("SELECT * FROM ai_advisor_conversations WHERE id = 'test-conv-1'")
        assert(cursor.count == 1) { "Should be able to query inserted conversation" }
        cursor.moveToFirst()
        assert(cursor.getString(cursor.getColumnIndexOrThrow("content")) == "Hello AI")
        assert(cursor.getString(cursor.getColumnIndexOrThrow("message_type")) == "USER")
        cursor.close()

        db.close()
    }
}
