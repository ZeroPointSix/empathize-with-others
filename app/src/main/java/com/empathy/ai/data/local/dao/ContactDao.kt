package com.empathy.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.empathy.ai.data.local.entity.ContactProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * 联系人数据访问对象 (DAO)
 *
 * 设计原则:
 * - 查询要响应式 (Reactive): 返回Flow,数据变动自动推送
 * - 写入要简单粗暴 (Upsert): 使用REPLACE策略,简化上层逻辑
 *
 * 核心操作:
 * 1. 查询所有联系人(getAllProfiles) - 返回Flow<List>,UI自动刷新
 * 2. 插入或更新(insertOrUpdate) - 冲突时覆盖,无需单独update
 *
 * @see ContactProfileEntity
 */
@Dao
interface ContactDao {

    /**
     * 查询所有联系人
     *
     * 返回Flow<List<ContactProfileEntity>>,这意味着不仅仅是一次查询,
     * 而是一个"长连接管道"。只要profiles表有任何变动,这个Flow会自动
     * 吐出最新数据。这是MVVM架构UI自动刷新的根本动力。
     *
     * @return 联系人实体列表的Flow
     */
    @Query("SELECT * FROM profiles")
    fun getAllProfiles(): Flow<List<ContactProfileEntity>>

    /**
     * 根据ID查询单个联系人
     *
     * @param id 联系人唯一标识
     * @return 联系人实体
     */
    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: String): ContactProfileEntity?

    /**
     * 插入或更新联系人
     *
     * 使用OnConflictStrategy.REPLACE策略:
     * - 如果ID不存在,执行插入
     * - 如果ID已存在,覆盖更新旧数据
     * 这大大简化了上层逻辑,无需区分insert和update。
     *
     * 注意:这是挂起函数(suspend),必须在协程中调用。
     *
     * @param entity 联系人实体
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: ContactProfileEntity)

    /**
     * 根据ID删除联系人
     *
     * @param id 要删除的联系人ID
     */
    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: String)
}
