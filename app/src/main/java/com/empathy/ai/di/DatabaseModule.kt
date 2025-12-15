package com.empathy.ai.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.empathy.ai.data.local.AppDatabase
import com.empathy.ai.data.local.dao.AiProviderDao
import com.empathy.ai.data.local.dao.BrainTagDao
import com.empathy.ai.data.local.dao.ContactDao
import com.empathy.ai.data.local.dao.ConversationLogDao
import com.empathy.ai.data.local.dao.DailySummaryDao
import com.empathy.ai.data.local.dao.FailedSummaryTaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库模块
 *
 * 提供 Room Database 和 DAO 的依赖注入配置。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 数据库迁移: 版本 1 -> 2
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
     * 数据库迁移: 版本 2 -> 3
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
     * 数据库迁移: 版本 3 -> 4
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
            // 索引名称必须与 Entity @Index 注解生成的名称一致
            // Room 自动生成格式: index_{tableName}_{columnName}
            db.execSQL("CREATE INDEX IF NOT EXISTS index_conversation_logs_contact_id ON conversation_logs(contact_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_conversation_logs_timestamp ON conversation_logs(timestamp)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_conversation_logs_is_summarized ON conversation_logs(is_summarized)")

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
            // 索引名称必须与 Entity @Index 注解生成的名称一致
            db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_summaries_contact_id ON daily_summaries(contact_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_summaries_summary_date ON daily_summaries(summary_date)")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_daily_summaries_contact_id_summary_date " +
                    "ON daily_summaries(contact_id, summary_date)"
            )

            // 3. 添加profiles新字段
            db.execSQL("ALTER TABLE profiles ADD COLUMN relationship_score INTEGER NOT NULL DEFAULT 50")
            db.execSQL("ALTER TABLE profiles ADD COLUMN last_interaction_date TEXT")
        }
    }

    /**
     * 数据库迁移: 版本 4 -> 5
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
     * 数据库迁移: 版本 5 -> 6
     * 添加UI扩展字段：
     * - profiles表添加avatar_url字段
     * - brain_tags表添加is_confirmed字段
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 为profiles表添加avatar_url字段
            db.execSQL("ALTER TABLE profiles ADD COLUMN avatar_url TEXT")
            
            // 为brain_tags表添加is_confirmed字段（默认为1=true，表示已确认）
            db.execSQL("ALTER TABLE brain_tags ADD COLUMN is_confirmed INTEGER NOT NULL DEFAULT 1")
        }
    }

    /**
     * 数据库迁移: 版本 6 -> 7
     * 修复性迁移：修复 conversation_logs 表结构
     * 
     * 问题背景：
     * - 某些设备上 conversation_logs 表可能缺少 timestamp 和 is_summarized 字段
     * - 索引名称可能与 Entity 定义不匹配
     * - 外键约束可能缺失
     * 
     * 修复策略：
     * 1. 检测表结构是否完整
     * 2. 如果不完整，重建表并迁移数据
     * 3. 确保索引名称与 Entity @Index 注解一致
     */
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. 检查 conversation_logs 表结构
            val cursor = db.query("PRAGMA table_info(conversation_logs)")
            val columns = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(1)) // column name at index 1
            }
            cursor.close()

            // 2. 如果缺少必要字段，需要重建表
            val needsRebuild = !columns.contains("timestamp") || !columns.contains("is_summarized")
            
            if (needsRebuild) {
                // 备份旧表
                db.execSQL("ALTER TABLE conversation_logs RENAME TO conversation_logs_backup")
                
                // 创建新表（完整结构）
                db.execSQL("""
                    CREATE TABLE conversation_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        contact_id TEXT NOT NULL,
                        user_input TEXT NOT NULL,
                        ai_response TEXT,
                        timestamp INTEGER NOT NULL DEFAULT 0,
                        is_summarized INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(contact_id) REFERENCES profiles(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // 迁移数据（处理可能缺失的字段）
                val backupColumns = mutableSetOf<String>()
                val backupCursor = db.query("PRAGMA table_info(conversation_logs_backup)")
                while (backupCursor.moveToNext()) {
                    backupColumns.add(backupCursor.getString(1))
                }
                backupCursor.close()
                
                // 构建迁移SQL
                val hasTimestamp = backupColumns.contains("timestamp")
                val hasIsSummarized = backupColumns.contains("is_summarized")
                
                val timestampExpr = if (hasTimestamp) "timestamp" else "0"
                val isSummarizedExpr = if (hasIsSummarized) "is_summarized" else "0"
                
                db.execSQL("""
                    INSERT INTO conversation_logs (id, contact_id, user_input, ai_response, timestamp, is_summarized)
                    SELECT id, contact_id, user_input, ai_response, $timestampExpr, $isSummarizedExpr
                    FROM conversation_logs_backup
                """.trimIndent())
                
                // 删除备份表
                db.execSQL("DROP TABLE conversation_logs_backup")
            }
            
            // 3. 修复索引名称（删除旧的，创建新的）
            // 删除可能存在的旧索引（使用旧命名格式）
            db.execSQL("DROP INDEX IF EXISTS idx_conv_contact")
            db.execSQL("DROP INDEX IF EXISTS idx_conv_timestamp")
            db.execSQL("DROP INDEX IF EXISTS idx_conv_summarized")
            
            // 创建正确名称的索引（与 Entity @Index 注解一致）
            db.execSQL("CREATE INDEX IF NOT EXISTS index_conversation_logs_contact_id ON conversation_logs(contact_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_conversation_logs_timestamp ON conversation_logs(timestamp)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_conversation_logs_is_summarized ON conversation_logs(is_summarized)")
            
            // 4. 同样修复 failed_summary_tasks 表的索引
            db.execSQL("DROP INDEX IF EXISTS idx_failed_contact")
            db.execSQL("DROP INDEX IF EXISTS idx_failed_at")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_failed_summary_tasks_contact_id ON failed_summary_tasks(contact_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_failed_summary_tasks_failed_at ON failed_summary_tasks(failed_at)")
        }
    }

    /**
     * 提供 AppDatabase 实例
     *
     * 数据库配置说明（T057/T058）：
     * - 使用完整的迁移脚本链（v1→v2→v3→v4→v5）
     * - 已移除fallbackToDestructiveMigration()，确保数据安全
     * - 如果迁移失败，应用会抛出异常而不是删除数据
     *
     * 迁移历史：
     * - v1→v2: 添加ai_providers表
     * - v2→v3: 添加timeout_ms字段
     * - v3→v4: 添加记忆系统表（conversation_logs, daily_summaries）
     * - v4→v5: 添加failed_summary_tasks表
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "empathy_ai_database"
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7
            )
            // T058: 已移除fallbackToDestructiveMigration()
            // 确保数据安全，迁移失败时抛出异常而不是删除数据
            .build()
    }

    @Provides
    fun provideContactDao(database: AppDatabase): ContactDao = database.contactDao()

    @Provides
    fun provideBrainTagDao(database: AppDatabase): BrainTagDao = database.brainTagDao()

    @Provides
    fun provideAiProviderDao(database: AppDatabase): AiProviderDao = database.aiProviderDao()

    @Provides
    fun provideConversationLogDao(database: AppDatabase): ConversationLogDao =
        database.conversationLogDao()

    @Provides
    fun provideDailySummaryDao(database: AppDatabase): DailySummaryDao =
        database.dailySummaryDao()

    @Provides
    fun provideFailedSummaryTaskDao(database: AppDatabase): FailedSummaryTaskDao =
        database.failedSummaryTaskDao()
}
