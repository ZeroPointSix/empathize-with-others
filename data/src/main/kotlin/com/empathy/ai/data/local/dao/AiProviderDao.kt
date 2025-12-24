package com.empathy.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.empathy.ai.data.local.entity.AiProviderEntity
import kotlinx.coroutines.flow.Flow

/**
 * AI 服务商数据访问对象 (DAO)
 *
 * 设计原则:
 * - 查询要响应式 (Reactive): 返回 Flow, 数据变动自动推送
 * - 写入要简单粗暴 (Upsert): 使用 REPLACE 策略, 简化上层逻辑
 * - 默认服务商唯一性: 通过业务逻辑保证, 不在数据库层强制
 *
 * 核心操作:
 * 1. 查询所有服务商 (getAllProviders) - 返回 Flow<List>, UI 自动刷新
 * 2. 查询默认服务商 (getDefaultProvider) - 返回 Flow, 监听默认服务商变化
 * 3. 插入或更新 (insertOrUpdate) - 冲突时覆盖, 无需单独 update
 * 4. 设置默认服务商 (setDefaultProvider) - 先清除所有默认标记, 再设置新的
 *
 * @see AiProviderEntity
 */
@Dao
interface AiProviderDao {

    /**
     * 查询所有服务商
     *
     * 返回 Flow<List<AiProviderEntity>>, 这意味着不仅仅是一次查询,
     * 而是一个"长连接管道"。只要 ai_providers 表有任何变动, 这个 Flow 会自动
     * 吐出最新数据。这是 MVVM 架构 UI 自动刷新的根本动力。
     *
     * @return 服务商实体列表的 Flow
     */
    @Query("SELECT * FROM ai_providers ORDER BY created_at DESC")
    fun getAllProviders(): Flow<List<AiProviderEntity>>

    /**
     * 根据 ID 查询单个服务商
     *
     * @param id 服务商唯一标识
     * @return 服务商实体
     */
    @Query("SELECT * FROM ai_providers WHERE id = :id")
    suspend fun getProviderById(id: String): AiProviderEntity?

    /**
     * 查询默认服务商
     *
     * 返回 Flow, 监听默认服务商的变化。
     * 注意: 业务逻辑应保证最多只有一个服务商的 is_default = 1
     *
     * @return 默认服务商实体的 Flow (可能为 null)
     */
    @Query("SELECT * FROM ai_providers WHERE is_default = 1 LIMIT 1")
    fun getDefaultProvider(): Flow<AiProviderEntity?>

    /**
     * 插入或更新服务商
     *
     * 使用 OnConflictStrategy.REPLACE 策略:
     * - 如果 ID 不存在, 执行插入
     * - 如果 ID 已存在, 覆盖更新旧数据
     * 这大大简化了上层逻辑, 无需区分 insert 和 update。
     *
     * 注意: 这是挂起函数 (suspend), 必须在协程中调用。
     *
     * @param entity 服务商实体
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: AiProviderEntity)

    /**
     * 根据 ID 删除服务商
     *
     * @param id 要删除的服务商 ID
     */
    @Query("DELETE FROM ai_providers WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * 清除所有服务商的默认标记
     *
     * 用于设置新的默认服务商前, 先清除旧的默认标记。
     * 这样可以保证默认服务商的唯一性。
     */
    @Query("UPDATE ai_providers SET is_default = 0")
    suspend fun clearAllDefaultFlags()

    /**
     * 设置指定服务商为默认
     *
     * @param id 要设置为默认的服务商 ID
     */
    @Query("UPDATE ai_providers SET is_default = 1 WHERE id = :id")
    suspend fun setDefaultById(id: String)
}
