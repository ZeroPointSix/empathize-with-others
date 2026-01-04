package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.UserProfile

/**
 * 用户画像仓储接口
 *
 * 业务背景:
   - 用户画像记录用户自身的信息和偏好
   - 用于AI分析时构建用户视角的上下文
   - 与联系人画像形成对照，支持"双向理解"
 *
 * 设计决策:
   - 单例模式：只有一个用户画像
   - 导入导出：支持JSON格式的导入导出
   - 强制刷新：支持强制从存储重新加载
 *
 * 遵循Clean Architecture原则：接口定义在Domain层
 */
interface UserProfileRepository {

    /**
     * 获取用户画像
     *
     * 业务规则:
     * - 如果不存在用户画像，返回成功但profile为默认空值
     * - forceRefresh=true时忽略缓存，强制从存储重新加载
     *
     * @param forceRefresh 是否强制刷新（忽略缓存）
     * @return 获取结果，成功返回UserProfile，失败返回异常
     */
    suspend fun getUserProfile(forceRefresh: Boolean = false): Result<UserProfile>

    /**
     * 更新用户画像
     *
     * 业务规则:
     * - 完全覆盖更新，保留所有字段
     * - 更新时自动更新时间戳
     *
     * @param profile 要更新的用户画像
     * @return 更新结果，成功返回Unit，失败返回异常
     */
    suspend fun updateUserProfile(profile: UserProfile): Result<Unit>

    /**
     * 清除用户画像
     *
     * 业务规则:
     * - 删除所有用户画像数据
       - 不可逆操作，需二次确认
     *
     * @return 清除结果，成功返回Unit，失败返回异常
     */
    suspend fun clearUserProfile(): Result<Unit>

    /**
     * 导出用户画像
     *
     * 用途: 用户备份或迁移数据
     *
     * @return 导出结果，成功返回JSON字符串，失败返回异常
     */
    suspend fun exportUserProfile(): Result<String>

    /**
     * 导入用户画像
     *
     * 业务规则:
     * - 导入前验证JSON格式
     * - 覆盖现有画像
     *
     * @param json JSON格式的用户画像数据
     * @return 导入结果，成功返回Unit，失败返回异常
     */
    suspend fun importUserProfile(json: String): Result<Unit>

    /**
     * 检查是否存在用户画像
     *
     * @return 是否存在用户画像数据
     */
    suspend fun hasUserProfile(): Boolean
}
