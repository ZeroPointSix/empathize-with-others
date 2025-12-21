package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.UserProfile

/**
 * 用户画像仓库接口
 *
 * 定义用户画像数据的访问和操作方法。
 * 遵循Clean Architecture原则，接口定义在Domain层。
 */
interface UserProfileRepository {
    
    /**
     * 获取用户画像
     *
     * @return 获取结果，成功返回UserProfile，失败返回异常
     */
    suspend fun getUserProfile(): Result<UserProfile>
    
    /**
     * 更新用户画像
     *
     * @param profile 要更新的用户画像
     * @return 更新结果，成功返回Unit，失败返回异常
     */
    suspend fun updateUserProfile(profile: UserProfile): Result<Unit>
    
    /**
     * 清除用户画像
     *
     * @return 清除结果，成功返回Unit，失败返回异常
     */
    suspend fun clearUserProfile(): Result<Unit>
    
    /**
     * 导出用户画像
     *
     * @return 导出结果，成功返回JSON字符串，失败返回异常
     */
    suspend fun exportUserProfile(): Result<String>
    
    /**
     * 导入用户画像
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
