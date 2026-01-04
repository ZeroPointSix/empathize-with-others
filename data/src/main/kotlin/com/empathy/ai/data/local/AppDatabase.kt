package com.empathy.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.empathy.ai.data.local.converter.FactListConverter
import com.empathy.ai.data.local.converter.RoomTypeConverters
import com.empathy.ai.data.local.dao.AiAdvisorDao
import com.empathy.ai.data.local.dao.AiProviderDao
import com.empathy.ai.data.local.dao.ApiUsageDao
import com.empathy.ai.data.local.dao.BrainTagDao
import com.empathy.ai.data.local.dao.ContactDao
import com.empathy.ai.data.local.dao.ConversationLogDao
import com.empathy.ai.data.local.dao.ConversationTopicDao
import com.empathy.ai.data.local.dao.DailySummaryDao
import com.empathy.ai.data.local.dao.FailedSummaryTaskDao
import com.empathy.ai.data.local.entity.AiAdvisorConversationEntity
import com.empathy.ai.data.local.entity.AiAdvisorSessionEntity
import com.empathy.ai.data.local.entity.AiProviderEntity
import com.empathy.ai.data.local.entity.ApiUsageEntity
import com.empathy.ai.data.local.entity.BrainTagEntity
import com.empathy.ai.data.local.entity.ContactProfileEntity
import com.empathy.ai.data.local.entity.ConversationLogEntity
import com.empathy.ai.data.local.entity.ConversationTopicEntity
import com.empathy.ai.data.local.entity.DailySummaryEntity
import com.empathy.ai.data.local.entity.FailedSummaryTaskEntity

/**
 * 应用数据库配置类
 *
 * 这是所有数据库组件的总装类,负责:
 * 1. 挂载Entity(Step 2创建的实体类)
 * 2. 挂载TypeConverter(Step 3创建的转换器)
 * 3. 声明版本号
 * 4. 提供DAO访问接口
 *
 * 版本控制（TD-001完善）:
 * - 当前版本: 11 (对话主题功能)
 * - 升级策略: 使用完整的Migration脚本链，确保用户数据安全
 * - Schema导出: 已启用，用于版本管理和迁移测试
 * - 迁移历史:
 *   - v1→v2: 添加ai_providers表
 *   - v2→v3: 添加timeout_ms字段
 *   - v3→v4: 添加记忆系统表（conversation_logs, daily_summaries）
 *   - v4→v5: 添加failed_summary_tasks表
 *   - v5→v6: 添加UI扩展字段（avatar_url, is_confirmed）
 *   - v6→v7: 修复性迁移（修复conversation_logs表结构和索引名称）
 *   - v7→v8: 添加提示词管理系统字段（custom_prompt）
 *   - v8→v9: 扩展daily_summaries表支持手动总结（start_date, end_date, summary_type, generation_source, conversation_count, generated_at）
 *   - v9→v10: 添加编辑追踪字段（is_user_modified, last_modified_time, original_* 等）
 *   - v10→v11: 添加conversation_topics表（对话主题功能）
 *   - v11→v12: 添加API用量统计表和AI服务商高级选项字段（TD-00025）
 *   - v12→v13: 添加AI军师对话功能表（TD-00026）
 *
 * @property entities 数据库包含的实体类列表
 * @property version 数据库版本号
 * @property exportSchema 是否导出数据库schema(JSON文件,用于版本管理和测试)
 * @property typeConverters TypeConverter类列表
 */
@Database(
    entities = [
        ContactProfileEntity::class,
        BrainTagEntity::class,
        AiProviderEntity::class,
        ConversationLogEntity::class,
        DailySummaryEntity::class,
        FailedSummaryTaskEntity::class,
        ConversationTopicEntity::class,
        ApiUsageEntity::class,
        AiAdvisorSessionEntity::class,
        AiAdvisorConversationEntity::class
    ],
    version = 13,
    exportSchema = true // TD-001: 启用Schema导出，用于版本管理和迁移测试
)
@TypeConverters(RoomTypeConverters::class, FactListConverter::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * 获取联系人DAO
     */
    abstract fun contactDao(): ContactDao

    /**
     * 获取标签DAO
     */
    abstract fun brainTagDao(): BrainTagDao

    /**
     * 获取AI服务商DAO
     */
    abstract fun aiProviderDao(): AiProviderDao

    /**
     * 获取对话记录DAO
     */
    abstract fun conversationLogDao(): ConversationLogDao

    /**
     * 获取每日总结DAO
     */
    abstract fun dailySummaryDao(): DailySummaryDao

    /**
     * 获取失败任务DAO
     */
    abstract fun failedSummaryTaskDao(): FailedSummaryTaskDao

    /**
     * 获取对话主题DAO
     */
    abstract fun conversationTopicDao(): ConversationTopicDao

    /**
     * 获取API用量DAO
     */
    abstract fun apiUsageDao(): ApiUsageDao

    /**
     * 获取AI军师DAO
     */
    abstract fun aiAdvisorDao(): AiAdvisorDao
}
