package com.empathy.ai.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.empathy.ai.data.local.AppDatabase
import com.empathy.ai.data.local.dao.AiProviderDao
import com.empathy.ai.data.local.dao.BrainTagDao
import com.empathy.ai.data.local.dao.ContactDao
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
 *
 * @constructor 创建数据库模块
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
            // 创建 ai_providers 表
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS ai_providers (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    base_url TEXT NOT NULL,
                    api_key_ref TEXT NOT NULL,
                    models_json TEXT NOT NULL,
                    default_model_id TEXT NOT NULL,
                    is_default INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())
            
            // 创建索引以优化默认服务商查询
            db.execSQL("""
                CREATE INDEX IF NOT EXISTS idx_ai_providers_is_default 
                ON ai_providers(is_default)
            """.trimIndent())
        }
    }

    /**
     * 提供 AppDatabase 实例
     *
     * 使用单例模式确保整个应用共享同一个数据库实例。
     *
     * @param context 应用上下文
     * @return AppDatabase 实例
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "empathy_ai_database"
        )
            .addMigrations(MIGRATION_1_2)
            // MVP阶段简化:如果表结构变更,卸载重装APP即可
            // 正式发布后需要添加 Migration 策略
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * 提供 ContactDao
     *
     * @param database AppDatabase 实例
     * @return ContactDao 实例
     */
    @Provides
    fun provideContactDao(database: AppDatabase): ContactDao {
        return database.contactDao()
    }

    /**
     * 提供 BrainTagDao
     *
     * @param database AppDatabase 实例
     * @return BrainTagDao 实例
     */
    @Provides
    fun provideBrainTagDao(database: AppDatabase): BrainTagDao {
        return database.brainTagDao()
    }

    /**
     * 提供 AiProviderDao
     *
     * @param database AppDatabase 实例
     * @return AiProviderDao 实例
     */
    @Provides
    fun provideAiProviderDao(database: AppDatabase): AiProviderDao {
        return database.aiProviderDao()
    }
}
