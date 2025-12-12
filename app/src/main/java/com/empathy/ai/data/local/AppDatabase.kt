package com.empathy.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.empathy.ai.data.local.converter.RoomTypeConverters
import com.empathy.ai.data.local.dao.AiProviderDao
import com.empathy.ai.data.local.dao.BrainTagDao
import com.empathy.ai.data.local.dao.ContactDao
import com.empathy.ai.data.local.entity.AiProviderEntity
import com.empathy.ai.data.local.entity.BrainTagEntity
import com.empathy.ai.data.local.entity.ContactProfileEntity

/**
 * 应用数据库配置类
 *
 * 这是所有数据库组件的总装类,负责:
 * 1. 挂载Entity(Step 2创建的实体类)
 * 2. 挂载TypeConverter(Step 3创建的转换器)
 * 3. 声明版本号
 * 4. 提供DAO访问接口
 *
 * 版本控制:
 * - 当前版本: 1 (MVP阶段)
 * - 升级策略:MVP阶段如果修改表结构,直接卸载重装APP即可,
 *   无需编写Migration脚本。正式发布后才需要Migration。
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
        AiProviderEntity::class
    ],
    version = 2,
    exportSchema = false // MVP阶段不导出schema,减少构建复杂度
)
@TypeConverters(RoomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * 获取联系人DAO
     *
     * @return ContactDao实例
     */
    abstract fun contactDao(): ContactDao

    /**
     * 获取标签DAO
     *
     * @return BrainTagDao实例
     */
    abstract fun brainTagDao(): BrainTagDao

    /**
     * 获取AI服务商DAO
     *
     * @return AiProviderDao实例
     */
    abstract fun aiProviderDao(): AiProviderDao
}
