/**
 * Package com.empathy.ai.data.local 实现了应用数据持久化层
 *
 * 业务背景 (PRD-00003):
 *   - 联系人画像记忆系统需要持久化存储对话记录、每日总结、标签等数据
 *   - 三层记忆架构：对话记录(短期) → 每日总结(中期) → 联系人画像(长期)
 *
 * 设计决策 (TDD-00003):
 *   - 选择 Room 而非 SQLite，因其编译时检查和迁移支持更完善
 *   - 使用 TypeConverter 处理复杂类型（List、Enum、Map）
 *   - 迁移策略：完整 Migration 脚本链，确保用户数据零丢失
 *
 * 任务追踪:
 *   - FD-00003 - 联系人画像记忆系统设计
 *   - TD-00026 - AI军师对话功能（新增 AiAdvisor 会话实体）
 */
package com.empathy.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.empathy.ai.data.local.converter.FactListConverter
import com.empathy.ai.data.local.converter.RoomTypeConverters
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
import com.empathy.ai.data.local.entity.AiAdvisorConversationEntity
import com.empathy.ai.data.local.entity.AiAdvisorMessageBlockEntity
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
 *   - v13→v14: 添加AI军师消息块表（FD-00028流式对话升级）
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
        AiAdvisorConversationEntity::class,
        AiAdvisorMessageBlockEntity::class  // FD-00028: 流式对话消息块
    ],
    version = 14,  // FD-00028: 版本升级
    exportSchema = true // TD-001: 启用Schema导出，用于版本管理和迁移测试
)
@TypeConverters(RoomTypeConverters::class, FactListConverter::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * 获取联系人DAO
     *
     * 【Flow订阅】返回的Flow是"数据监听通道"：
     * - 首次订阅：立即推送当前联系人列表
     * - 数据变动：自动推送新数据
     * - 取消订阅：自动释放资源
     *
     * 这是联系人列表能实时刷新的根本动力。
     */
    abstract fun contactDao(): ContactDao

    /**
     * 获取标签DAO
     *
     * 【实时感知】标签数据通过Flow实时推送：
     * - 用户在"数据喂养"页面添加标签时
     * - 聊天页面的分析卡片立即感知变化
     * - 实现跨页面的数据联动
     */
    abstract fun brainTagDao(): BrainTagDao

    /**
     * 获取AI服务商DAO
     *
     * 【配置管理】存储和管理AI服务商的API配置：
     * - 支持多服务商切换（DeepSeek、OpenAI等）
     * - API密钥加密存储（通过ApiKeyStorage）
     * - 模型列表动态获取
     */
    abstract fun aiProviderDao(): AiProviderDao

    /**
     * 获取对话记录DAO
     *
     * 【短期记忆】对话记录是最细粒度的数据：
     * - 按联系人和日期查询
     * - 支持分页加载（Paging 3）
     * - 定期归档到每日总结
     */
    abstract fun conversationLogDao(): ConversationLogDao

    /**
     * 获取每日总结DAO
     *
     * 【中期记忆】每日总结是对话的聚合：
     * - 支持日期范围查询（v9新增）
     * - 区分自动生成和手动触发
     * - 提供AI分析的上下文输入
     */
    abstract fun dailySummaryDao(): DailySummaryDao

    /**
     * 获取失败任务DAO
     *
     * 【容错机制】失败任务用于重试队列：
     * - 最多重试3次
     * - 7天后自动清理
     * - 避免AI服务暂时不可用导致数据丢失
     */
    abstract fun failedSummaryTaskDao(): FailedSummaryTaskDao

    /**
     * 获取对话主题DAO
     *
     * 【主题管理】用户可为每个联系人设置对话主题：
     * - 支持主题历史和活跃状态
     * - 级联删除：联系人删除时自动删除主题
     * - 复合索引优化查询性能
     */
    abstract fun conversationTopicDao(): ConversationTopicDao

    /**
     * 获取API用量DAO
     *
     * 【用量统计】追踪API调用消耗：
     * - 按服务商分组统计
     * - 支持日期范围查询
     * - 便于成本控制和配额管理
     */
    abstract fun apiUsageDao(): ApiUsageDao

    /**
     * 获取AI军师DAO
     *
     * 【TD-00026】AI军师是独立对话模块：
     * - 支持会话历史管理
     * - 会话与联系人关联
     * - 提供智能对话建议
     */
    abstract fun aiAdvisorDao(): AiAdvisorDao

    /**
     * 获取AI军师消息块DAO
     *
     * 【FD-00028】流式对话消息块管理：
     * - 支持Block-based消息架构
     * - 支持思考过程展示
     * - 支持智能节流更新
     */
    abstract fun aiAdvisorMessageBlockDao(): AiAdvisorMessageBlockDao
}
