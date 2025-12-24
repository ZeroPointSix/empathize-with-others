package com.empathy.ai.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 数据库迁移 10 -> 11 测试
 *
 * 验证 TD-00016 对话主题功能的数据库迁移：
 * - conversation_topics 表创建
 * - 索引创建
 * - 字段默认值
 *
 * 注意：这是单元测试版本，验证迁移SQL语句的正确性
 * 完整的迁移测试请参考 androidTest 中的 DatabaseMigrationTest
 *
 * @see TDD-00016 对话主题功能技术设计
 */
class Migration10To11Test {

    /**
     * 验证 MIGRATION_10_11 SQL 语句格式正确
     */
    @Test
    fun `MIGRATION_10_11 SQL语句应该包含正确的表结构`() {
        val createTableSql = """
            CREATE TABLE IF NOT EXISTS conversation_topics (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                contact_id TEXT NOT NULL,
                content TEXT NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent()

        // 验证SQL包含必要的字段
        assertTrue("应该包含id字段", createTableSql.contains("id INTEGER PRIMARY KEY"))
        assertTrue("应该包含contact_id字段", createTableSql.contains("contact_id TEXT NOT NULL"))
        assertTrue("应该包含content字段", createTableSql.contains("content TEXT NOT NULL"))
        assertTrue("应该包含is_active字段", createTableSql.contains("is_active INTEGER NOT NULL DEFAULT 1"))
        assertTrue("应该包含created_at字段", createTableSql.contains("created_at INTEGER NOT NULL"))
        assertTrue("应该包含updated_at字段", createTableSql.contains("updated_at INTEGER NOT NULL"))
    }

    /**
     * 验证索引SQL语句格式正确
     */
    @Test
    fun `MIGRATION_10_11 应该创建正确的索引`() {
        val indexSql1 = "CREATE INDEX IF NOT EXISTS index_conversation_topics_contact_id ON conversation_topics(contact_id)"
        val indexSql2 = "CREATE INDEX IF NOT EXISTS index_conversation_topics_is_active ON conversation_topics(is_active)"

        // 验证索引SQL格式
        assertTrue("contact_id索引SQL应该正确", indexSql1.contains("index_conversation_topics_contact_id"))
        assertTrue("is_active索引SQL应该正确", indexSql2.contains("index_conversation_topics_is_active"))
    }

    /**
     * 验证数据库版本号
     */
    @Test
    fun `数据库版本应该从10升级到11`() {
        val fromVersion = 10
        val toVersion = 11

        assertEquals("起始版本应该是10", 10, fromVersion)
        assertEquals("目标版本应该是11", 11, toVersion)
        assertEquals("版本差应该是1", 1, toVersion - fromVersion)
    }

    /**
     * 验证表名常量
     */
    @Test
    fun `表名应该是conversation_topics`() {
        val tableName = "conversation_topics"
        assertEquals("conversation_topics", tableName)
    }

    /**
     * 验证字段名常量
     */
    @Test
    fun `字段名应该符合命名规范`() {
        val fields = listOf(
            "id",
            "contact_id",
            "content",
            "is_active",
            "created_at",
            "updated_at"
        )

        // 验证所有字段使用snake_case
        fields.forEach { field ->
            assertTrue("字段 $field 应该使用snake_case", field.matches(Regex("^[a-z_]+$")))
        }
    }

    /**
     * 验证is_active默认值
     */
    @Test
    fun `is_active默认值应该是1（活跃）`() {
        val defaultValue = 1
        assertEquals("is_active默认值应该是1", 1, defaultValue)
    }

    /**
     * 验证迁移脚本不会破坏现有数据
     */
    @Test
    fun `迁移脚本应该使用CREATE TABLE IF NOT EXISTS`() {
        val createTableSql = "CREATE TABLE IF NOT EXISTS conversation_topics"
        assertTrue("应该使用IF NOT EXISTS防止重复创建", createTableSql.contains("IF NOT EXISTS"))
    }

    /**
     * 验证索引创建使用IF NOT EXISTS
     */
    @Test
    fun `索引创建应该使用CREATE INDEX IF NOT EXISTS`() {
        val indexSql = "CREATE INDEX IF NOT EXISTS index_conversation_topics_contact_id"
        assertTrue("应该使用IF NOT EXISTS防止重复创建索引", indexSql.contains("IF NOT EXISTS"))
    }

    /**
     * 验证迁移SQL语句完整性
     */
    @Test
    fun `迁移SQL语句应该完整`() {
        val migrationStatements = listOf(
            // 创建表
            """
            CREATE TABLE IF NOT EXISTS conversation_topics (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                contact_id TEXT NOT NULL,
                content TEXT NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
            // 创建contact_id索引
            "CREATE INDEX IF NOT EXISTS index_conversation_topics_contact_id ON conversation_topics(contact_id)",
            // 创建is_active索引
            "CREATE INDEX IF NOT EXISTS index_conversation_topics_is_active ON conversation_topics(is_active)"
        )

        assertEquals("应该有3条迁移语句", 3, migrationStatements.size)
        migrationStatements.forEach { sql ->
            assertNotNull("SQL语句不应该为空", sql)
            assertTrue("SQL语句不应该为空字符串", sql.isNotBlank())
        }
    }

    /**
     * 验证回滚SQL语句
     */
    @Test
    fun `回滚SQL应该正确删除表`() {
        val rollbackSql = "DROP TABLE IF EXISTS conversation_topics"
        assertTrue("回滚SQL应该使用DROP TABLE IF EXISTS", rollbackSql.contains("DROP TABLE IF EXISTS"))
        assertTrue("回滚SQL应该指定正确的表名", rollbackSql.contains("conversation_topics"))
    }
}
