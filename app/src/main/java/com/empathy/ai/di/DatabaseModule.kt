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
     * 提供 AppDatabase 实例
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "empathy_ai_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration()
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
