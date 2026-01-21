package com.empathy.ai.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.empathy.ai.data.local.AppDatabase
import com.empathy.ai.data.local.dao.AiAdvisorDao
import com.empathy.ai.data.local.dao.AiAdvisorMessageBlockDao
import com.empathy.ai.data.local.dao.AiProviderDao
import com.empathy.ai.data.local.dao.ApiUsageDao
import com.empathy.ai.data.local.dao.BrainTagDao
import com.empathy.ai.data.local.dao.ContactDao
import com.empathy.ai.data.local.dao.ConversationLogDao
import com.empathy.ai.data.local.dao.ConversationTopicDao
import com.empathy.ai.data.local.dao.DailySummaryDao
import com.empathy.ai.data.local.dao.FailedSummaryTaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DatabaseModule 实现了数据持久化层的依赖注入配置 (TDD-00017/7.1)
 *
 * 业务背景：
 * - 数据层负责所有数据访问、存储和远程调用
 * - 采用 Room 实现本地数据持久化
 * - 支持完整的数据库迁移链（v1→v13，共13次增量迁移）
 *
 * 设计决策：
 * - TDD-00017/7.1: DI 模块归属 data 模块，提供 Room 数据库实例
 * - 已移除 fallbackToDestructiveMigration() 确保数据安全
 * - 迁移失败时应用抛出异常而不是删除数据（用户需手动处理）
 *
 * 迁移历史总览：
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 版本   │ 功能增量                      │ 关联文档                             │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │ v1→v2 │ AI服务商表 ai_providers       │ PRD-00025 AI配置功能完善             │
 * │ v2→v3 │ timeout_ms字段                │ TDD-00025 AI配置功能完善技术设计     │
 * │ v3→v4 │ 记忆系统表（对话日志、每日总结）│ PRD-00003 联系人画像记忆系统         │
 * │ v4→v5 │ 失败任务表 failed_summary_    │ -                                   │
 * │       │ tasks                        │                                     │
 * │ v5→v6 │ UI扩展字段（avatar_url等）    │ -                                   │
 * │ v6→v7 │ 修复性迁移（表结构修复）       │ -                                   │
 * │ v7→v8 │ 提示词管理系统字段             │ PRD-00005 提示词管理系统             │
 * │ v8→v9 │ 手动总结功能扩展               │ PRD-00011 手动触发AI总结功能         │
 * │ v9→v10│ 编辑追踪字段                   │ PRD-00012 事实流内容编辑功能         │
 * │ v10→v11│ 对话主题表 conversation_     │ PRD-00016 对话主题功能               │
 * │       │ topics                       │                                     │
 * │ v11→v12│ API用量统计表、温度/Token字段 │ TDD-00025 AI配置功能完善技术设计     │
 * │ v12→v13│ AI军师对话表（TD-00026）     │ PRD-00026 AI军师对话功能             │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 架构位置：
 * - 属于 data 模块（Clean Architecture 数据层）
 * - 提供 AppDatabase 单例和所有 DAO 实例
 * - 被 app 模块的 Hilt 入口组装
 *
 * @see AppDatabase Room 数据库主类（定义所有表结构）
 * @see TDD-00017 Clean Architecture 模块化改造技术设计
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
     * 数据库迁移: 版本 7 -> 8
     * 添加提示词管理系统字段：
     * - profiles表添加custom_prompt字段（联系人专属提示词）
     */
    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 为profiles表添加custom_prompt字段
            db.execSQL("ALTER TABLE profiles ADD COLUMN custom_prompt TEXT DEFAULT NULL")
        }
    }

    /**
     * 数据库迁移: 版本 8 -> 9
     * 扩展daily_summaries表支持手动总结功能：
     * - 添加start_date字段（范围总结开始日期）
     * - 添加end_date字段（范围总结结束日期）
     * - 添加summary_type字段（总结类型：DAILY/CUSTOM_RANGE）
     * - 添加generation_source字段（生成来源：AUTO/MANUAL）
     * - 添加conversation_count字段（分析的对话数量）
     * - 添加generated_at字段（生成时间戳）
     * - 创建summary_type和generation_source索引
     */
    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 添加新字段
            db.execSQL(
                "ALTER TABLE daily_summaries ADD COLUMN start_date TEXT DEFAULT NULL"
            )
            db.execSQL(
                "ALTER TABLE daily_summaries ADD COLUMN end_date TEXT DEFAULT NULL"
            )
            db.execSQL(
                "ALTER TABLE daily_summaries ADD COLUMN summary_type TEXT NOT NULL DEFAULT 'DAILY'"
            )
            db.execSQL(
                "ALTER TABLE daily_summaries ADD COLUMN generation_source TEXT NOT NULL DEFAULT 'AUTO'"
            )
            db.execSQL(
                "ALTER TABLE daily_summaries ADD COLUMN conversation_count INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE daily_summaries ADD COLUMN generated_at INTEGER NOT NULL DEFAULT 0"
            )

            // 创建新索引
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_daily_summaries_summary_type ON daily_summaries(summary_type)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_daily_summaries_generation_source ON daily_summaries(generation_source)"
            )
        }
    }

    /**
     * 数据库迁移: 版本 9 -> 10
     * 添加编辑追踪字段支持事实流内容编辑功能：
     *
     * profiles表新增字段：
     * - is_name_user_modified: 姓名是否被用户修改过
     * - is_goal_user_modified: 目标是否被用户修改过
     * - name_last_modified_time: 姓名最后修改时间
     * - goal_last_modified_time: 目标最后修改时间
     * - original_name: 原始姓名
     * - original_goal: 原始目标
     *
     * conversation_logs表新增字段：
     * - is_user_modified: 是否被用户修改过
     * - last_modified_time: 最后修改时间
     * - original_user_input: 原始用户输入
     *
     * daily_summaries表新增字段：
     * - is_user_modified: 是否被用户修改过
     * - last_modified_time: 最后修改时间
     * - original_content: 原始内容
     */
    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. 扩展 profiles 表
            db.execSQL(
                "ALTER TABLE profiles ADD COLUMN is_name_user_modified INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE profiles ADD COLUMN is_goal_user_modified INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE profiles ADD COLUMN name_last_modified_time INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE profiles ADD COLUMN goal_last_modified_time INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE profiles ADD COLUMN original_name TEXT DEFAULT NULL"
            )
            db.execSQL(
                "ALTER TABLE profiles ADD COLUMN original_goal TEXT DEFAULT NULL"
            )

            // 2. 扩展 conversation_logs 表
            db.execSQL(
                "ALTER TABLE conversation_logs ADD COLUMN is_user_modified INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE conversation_logs ADD COLUMN last_modified_time INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE conversation_logs ADD COLUMN original_user_input TEXT DEFAULT NULL"
            )

            // 3. 扩展 daily_summaries 表
            db.execSQL(
                "ALTER TABLE daily_summaries ADD COLUMN is_user_modified INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE daily_summaries ADD COLUMN last_modified_time INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE daily_summaries ADD COLUMN original_content TEXT DEFAULT NULL"
            )
        }
    }

    /**
     * 数据库迁移: 版本 10 -> 11
     * 添加对话主题功能（TD-00016）：
     *
     * 新增conversation_topics表：
     * - id: 主题唯一标识（主键）
     * - contact_id: 关联的联系人ID（外键）
     * - content: 主题内容
     * - created_at: 创建时间戳
     * - updated_at: 更新时间戳
     * - is_active: 是否为活跃主题
     *
     * 索引：
     * - index_conversation_topics_contact_id: 按联系人ID查询
     * - index_conversation_topics_contact_id_is_active: 按联系人ID和活跃状态查询
     */
    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 创建conversation_topics表
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS conversation_topics (
                    id TEXT PRIMARY KEY NOT NULL,
                    contact_id TEXT NOT NULL,
                    content TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    is_active INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY (contact_id) REFERENCES profiles(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            // 创建索引
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_conversation_topics_contact_id 
                ON conversation_topics(contact_id)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_conversation_topics_contact_id_is_active 
                ON conversation_topics(contact_id, is_active)
                """.trimIndent()
            )
        }
    }

    /**
     * 数据库迁移: 版本 11 -> 12
     * TD-00025: AI配置功能完善
     *
     * 变更内容：
     * 1. ai_providers表添加temperature字段（生成温度，默认0.7）
     * 2. ai_providers表添加max_tokens字段（最大Token数，默认4096）
     * 3. 新增api_usage_records表（API用量统计）
     *
     * api_usage_records表结构：
     * - id: 记录唯一标识（主键）
     * - provider_id: 服务商ID
     * - provider_name: 服务商名称
     * - model_id: 模型ID
     * - model_name: 模型名称
     * - prompt_tokens: 输入Token数
     * - completion_tokens: 输出Token数
     * - total_tokens: 总Token数
     * - request_time_ms: 请求耗时
     * - is_success: 是否成功
     * - error_message: 错误信息
     * - created_at: 创建时间戳
     *
     * 索引：
     * - index_api_usage_records_provider_id: 按服务商查询
     * - index_api_usage_records_model_id: 按模型查询
     * - index_api_usage_records_created_at: 按时间查询
     * - index_api_usage_records_is_success: 按成功状态查询
     */
    private val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. 为ai_providers表添加temperature字段
            db.execSQL(
                "ALTER TABLE ai_providers ADD COLUMN temperature REAL NOT NULL DEFAULT 0.7"
            )

            // 2. 为ai_providers表添加max_tokens字段
            db.execSQL(
                "ALTER TABLE ai_providers ADD COLUMN max_tokens INTEGER NOT NULL DEFAULT 4096"
            )

            // 3. 创建api_usage_records表
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS api_usage_records (
                    id TEXT PRIMARY KEY NOT NULL,
                    provider_id TEXT NOT NULL,
                    provider_name TEXT NOT NULL,
                    model_id TEXT NOT NULL,
                    model_name TEXT NOT NULL,
                    prompt_tokens INTEGER NOT NULL DEFAULT 0,
                    completion_tokens INTEGER NOT NULL DEFAULT 0,
                    total_tokens INTEGER NOT NULL DEFAULT 0,
                    request_time_ms INTEGER NOT NULL DEFAULT 0,
                    is_success INTEGER NOT NULL DEFAULT 1,
                    error_message TEXT,
                    created_at INTEGER NOT NULL
                )
                """.trimIndent()
            )

            // 4. 创建索引
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_api_usage_records_provider_id ON api_usage_records(provider_id)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_api_usage_records_model_id ON api_usage_records(model_id)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_api_usage_records_created_at ON api_usage_records(created_at)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_api_usage_records_is_success ON api_usage_records(is_success)"
            )
        }
    }

    /**
     * 数据库迁移: 版本 12 -> 13
     * TD-00026: AI军师对话功能
     *
     * 变更内容：
     * 1. 新增ai_advisor_sessions表（AI军师会话）
     * 2. 新增ai_advisor_conversations表（AI军师对话记录）
     *
     * ai_advisor_sessions表结构：
     * - id: 会话唯一标识（主键）
     * - contact_id: 联系人ID（外键）
     * - title: 会话标题
     * - created_at: 创建时间戳
     * - updated_at: 更新时间戳
     * - message_count: 消息数量
     * - is_active: 是否为活跃会话
     *
     * ai_advisor_conversations表结构：
     * - id: 对话唯一标识（主键）
     * - contact_id: 联系人ID（外键）
     * - session_id: 会话ID（外键）
     * - message_type: 消息类型（USER/AI）
     * - content: 消息内容
     * - timestamp: 时间戳
     * - created_at: 创建时间戳
     * - send_status: 发送状态
     *
     * 索引：
     * - index_ai_advisor_sessions_contact_id: 按联系人查询会话
     * - index_ai_advisor_sessions_created_at: 按创建时间查询
     * - index_ai_advisor_sessions_updated_at: 按更新时间查询
     * - index_ai_advisor_conversations_contact_id: 按联系人查询对话
     * - index_ai_advisor_conversations_session_id: 按会话查询对话
     * - index_ai_advisor_conversations_timestamp: 按时间查询对话
     */
    // 使用internal修饰符，允许测试模块访问
    internal val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. 创建ai_advisor_sessions表
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ai_advisor_sessions (
                    id TEXT PRIMARY KEY NOT NULL,
                    contact_id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    message_count INTEGER NOT NULL DEFAULT 0,
                    is_active INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY (contact_id) REFERENCES profiles(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            // 2. 创建ai_advisor_sessions索引
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_ai_advisor_sessions_contact_id ON ai_advisor_sessions(contact_id)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_ai_advisor_sessions_created_at ON ai_advisor_sessions(created_at)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_ai_advisor_sessions_updated_at ON ai_advisor_sessions(updated_at)"
            )

            // 3. 创建ai_advisor_conversations表
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ai_advisor_conversations (
                    id TEXT PRIMARY KEY NOT NULL,
                    contact_id TEXT NOT NULL,
                    session_id TEXT NOT NULL,
                    message_type TEXT NOT NULL,
                    content TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    created_at INTEGER NOT NULL,
                    send_status TEXT NOT NULL DEFAULT 'SUCCESS',
                    FOREIGN KEY (contact_id) REFERENCES profiles(id) ON DELETE CASCADE,
                    FOREIGN KEY (session_id) REFERENCES ai_advisor_sessions(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            // 4. 创建ai_advisor_conversations索引
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_ai_advisor_conversations_contact_id ON ai_advisor_conversations(contact_id)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_ai_advisor_conversations_session_id ON ai_advisor_conversations(session_id)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_ai_advisor_conversations_timestamp ON ai_advisor_conversations(timestamp)"
            )
        }
    }

    /**
     * 数据库迁移 v13 → v14
     *
     * FD-00028: AI军师流式对话升级功能
     *
     * 变更内容：
     * 1. 新增ai_advisor_message_blocks表，支持Block-based消息架构
     * 2. 为现有AI消息创建默认MAIN_TEXT Block（数据迁移）
     *
     * 新增表：
     * - ai_advisor_message_blocks: 消息块表
     *
     * 新增索引：
     * - index_ai_advisor_message_blocks_message_id: 按消息ID查询
     */
    @Suppress("ClassName")
    internal val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. 创建消息块表
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ai_advisor_message_blocks (
                    id TEXT PRIMARY KEY NOT NULL,
                    message_id TEXT NOT NULL,
                    type TEXT NOT NULL,
                    status TEXT NOT NULL,
                    content TEXT NOT NULL,
                    metadata TEXT,
                    created_at INTEGER NOT NULL,
                    FOREIGN KEY (message_id) 
                        REFERENCES ai_advisor_conversations(id) 
                        ON DELETE CASCADE
                )
                """.trimIndent()
            )

            // 2. 创建索引（优化按消息ID查询）
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_ai_advisor_message_blocks_message_id 
                ON ai_advisor_message_blocks(message_id)
                """.trimIndent()
            )

            // 3. 数据迁移：为现有AI消息创建默认Block
            // 注意：使用 || 进行字符串拼接（SQLite语法）
            db.execSQL(
                """
                INSERT INTO ai_advisor_message_blocks 
                    (id, message_id, type, status, content, created_at)
                SELECT 
                    id || '_main_block',
                    id,
                    'MAIN_TEXT',
                    'SUCCESS',
                    content,
                    created_at
                FROM ai_advisor_conversations
                WHERE message_type = 'AI'
                """.trimIndent()
            )
        }
    }

    /**
     * 数据库迁移 v14 → v15
     *
     * BUG-00048-V4: 终止后重新生成消息角色错误修复
     * BUG-00048-V5: 修复NOT NULL约束不匹配问题
     *
     * 变更内容：
     * 1. ai_advisor_conversations表添加related_user_message_id字段
     *    用于关联AI消息与其对应的用户消息，确保重新生成时能正确获取原始用户输入
     *
     * 修复问题：
     * - 用户终止AI生成后点击"重新生成"时，被停止的内容被错误地显示为用户消息
     * - 根因：lastUserInput未持久化，应用重启/ViewModel重建后丢失
     * - 解决方案：通过relatedUserMessageId关联用户消息，实现三重保障获取用户输入
     *
     * V5修复：
     * - 问题：ALTER TABLE ADD COLUMN默认创建允许NULL的列，与Entity定义不匹配
     * - 解决：添加NOT NULL约束，确保与Entity定义一致
     */
    @Suppress("ClassName")
    internal val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // BUG-00048-V5: 添加NOT NULL约束，确保与Entity定义一致
            // Entity定义: @ColumnInfo(name = "related_user_message_id", defaultValue = "")
            // 需要: notNull = true
            db.execSQL(
                """
                ALTER TABLE ai_advisor_conversations 
                ADD COLUMN related_user_message_id TEXT NOT NULL DEFAULT ''
                """.trimIndent()
            )
        }
    }

    /**
     * 数据库迁移 v15 → v16
     *
     * BUG-00060: 会话管理增强功能
     *
     * 变更内容：
     * 1. ai_advisor_sessions表添加is_pinned字段
     *    用于支持会话置顶功能，置顶会话在列表中优先显示
     *
     * 功能说明：
     * - 空会话复用：避免重复创建空会话
     * - 会话自动命名：用第一条消息作为标题
     * - 会话重命名：支持手动修改会话名称
     * - 会话置顶：支持置顶重要会话
     */
    @Suppress("ClassName")
    internal val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // BUG-00060: 添加is_pinned字段，支持会话置顶功能
            // Entity定义: @ColumnInfo(name = "is_pinned", defaultValue = "0")
            db.execSQL(
                """
                ALTER TABLE ai_advisor_sessions 
                ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )
        }
    }

    /**
     * 数据库迁移 v16 → v17
     *
     * PRD-00037: 联系方式与头像颜色字段
     *
     * 变更内容：
     * 1. profiles表添加contact_info字段（联系方式）
     * 2. profiles表添加avatar_color_seed字段（默认头像颜色索引）
     */
    @Suppress("ClassName")
    internal val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE profiles
                ADD COLUMN contact_info TEXT
                """.trimIndent()
            )
            db.execSQL(
                """
                ALTER TABLE profiles
                ADD COLUMN avatar_color_seed INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )
            backfillAvatarColorSeed(db)
        }
    }

    /**
     * 提供 AppDatabase 实例
     *
     * 数据库配置说明（T057/T058）：
     * - 使用完整的迁移脚本链（v1→v2→v3→v4→v5→v6→v7→v8→v9→v10→v11→v12→v13→v14→v15→v16）
     * - 已移除fallbackToDestructiveMigration()，确保数据安全
     * - 如果迁移失败，应用会抛出异常而不是删除数据
     *
     * 迁移历史：
     * - v1→v2: 添加ai_providers表
     * - v2→v3: 添加timeout_ms字段
     * - v3→v4: 添加记忆系统表（conversation_logs, daily_summaries）
     * - v4→v5: 添加failed_summary_tasks表
     * - v5→v6: 添加UI扩展字段（avatar_url, is_confirmed）
     * - v6→v7: 修复性迁移（修复conversation_logs表结构和索引名称）
     * - v7→v8: 添加提示词管理系统字段（custom_prompt）
     * - v8→v9: 扩展daily_summaries表支持手动总结
     * - v9→v10: 添加编辑追踪字段支持事实流内容编辑
     * - v10→v11: 添加conversation_topics表（对话主题功能）
     * - v11→v12: 添加API用量统计表和AI服务商高级选项字段（TD-00025）
     * - v12→v13: 添加AI军师对话功能表（TD-00026）
     * - v13→v14: 添加AI军师消息块表（FD-00028流式对话升级）
     * - v14→v15: 添加related_user_message_id字段（BUG-00048-V4修复）
     * - v15→v16: 添加is_pinned字段（BUG-00060会话管理增强）
     * - v16→v17: 添加联系方式与头像颜色字段（PRD-00037）
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
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14,  // FD-00028: 流式对话升级
                MIGRATION_14_15,  // BUG-00048-V4: 终止后重新生成消息角色错误修复
                MIGRATION_15_16,  // BUG-00060: 会话管理增强（置顶功能）
                MIGRATION_16_17   // PRD-00037: 联系方式与头像颜色字段
            )
            // T058: 已移除fallbackToDestructiveMigration()
            // 确保数据安全，迁移失败时抛出异常而不是删除数据
            .build()
    }

    @Provides
    fun provideContactDao(database: AppDatabase): ContactDao = database.contactDao()

    @Provides
    fun provideBrainTagDao(database: AppDatabase): BrainTagDao = database.brainTagDao()

    private fun backfillAvatarColorSeed(db: SupportSQLiteDatabase) {
        db.compileStatement("UPDATE profiles SET avatar_color_seed = ? WHERE id = ?").use {
            val cursor = db.query("SELECT id, name FROM profiles WHERE avatar_color_seed = 0")
            cursor.use { rows ->
                val idIndex = rows.getColumnIndexOrThrow("id")
                val nameIndex = rows.getColumnIndexOrThrow("name")
                while (rows.moveToNext()) {
                    val id = rows.getString(idIndex)
                    val name = rows.getString(nameIndex).orEmpty()
                    val seed = name.hashCode()
                    it.bindLong(1, seed.toLong())
                    it.bindString(2, id)
                    it.executeUpdateDelete()
                    it.clearBindings()
                }
            }
        }
    }

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

    @Provides
    fun provideConversationTopicDao(database: AppDatabase): ConversationTopicDao =
        database.conversationTopicDao()

    @Provides
    fun provideApiUsageDao(database: AppDatabase): ApiUsageDao =
        database.apiUsageDao()

    @Provides
    fun provideAiAdvisorDao(database: AppDatabase): AiAdvisorDao =
        database.aiAdvisorDao()

    @Provides
    fun provideAiAdvisorMessageBlockDao(database: AppDatabase): AiAdvisorMessageBlockDao =
        database.aiAdvisorMessageBlockDao()
}
