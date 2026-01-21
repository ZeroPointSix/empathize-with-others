package com.empathy.ai.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.empathy.ai.data.di.DatabaseModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * PRD-00037 Migration测试
 *
 * 验证MIGRATION_16_17添加contact_info与avatar_color_seed字段。
 */
@RunWith(AndroidJUnit4::class)
class Migration16To17Test {

    companion object {
        private const val TEST_DB = "migration-16-17-test"
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
     * TC-MIG-001: 验证新字段存在并回填头像颜色种子
     */
    @Test
    @Throws(IOException::class)
    fun migration16To17_shouldAddContactInfoAndBackfillAvatarColorSeed() {
        assumeTrue("缺少schema 16/17，跳过迁移测试", hasSchema(16) && hasSchema(17))
        helper.createDatabase(TEST_DB, 16).apply {
            execSQL(
                """
                INSERT INTO profiles (id, name, target_goal, context_depth, facts_json, relationship_score)
                VALUES ('contact-1', 'Test Contact', 'Test Goal', 3, '[]', 50)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 17, true,
            DatabaseModule.MIGRATION_16_17
        )

        val cursor = db.query(
            "SELECT contact_info, avatar_color_seed FROM profiles WHERE id = 'contact-1'"
        )
        assertTrue(cursor.moveToFirst())
        assertTrue("contact_info should be NULL", cursor.isNull(0))
        val expectedSeed = "Test Contact".hashCode()
        assertEquals("avatar_color_seed should be derived from name hash", expectedSeed, cursor.getInt(1))
        cursor.close()
    }

    /**
     * TC-MIG-002: 验证迁移后可写入新字段
     */
    @Test
    @Throws(IOException::class)
    fun migration16To17_shouldAllowInsertingContactInfo() {
        assumeTrue("缺少schema 16/17，跳过迁移测试", hasSchema(16) && hasSchema(17))
        helper.createDatabase(TEST_DB, 16).apply {
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 17, true,
            DatabaseModule.MIGRATION_16_17
        )

        db.execSQL(
            """
            INSERT INTO profiles 
            (id, name, target_goal, context_depth, facts_json, relationship_score, contact_info, avatar_color_seed)
            VALUES ('contact-2', 'Contact 2', 'Goal', 3, '[]', 50, '123456', 3)
            """.trimIndent()
        )

        val cursor = db.query(
            "SELECT contact_info, avatar_color_seed FROM profiles WHERE id = 'contact-2'"
        )
        assertTrue(cursor.moveToFirst())
        assertEquals("123456", cursor.getString(0))
        assertEquals(3, cursor.getInt(1))
        cursor.close()
    }
}
