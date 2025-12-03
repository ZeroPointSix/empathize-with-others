package com.empathy.ai.data.repository

import com.empathy.ai.data.local.dao.ContactDao
import com.empathy.ai.data.local.entity.ContactProfileEntity
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.repository.ContactRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 联系人画像仓库实现类
 *
 * 这是连接Domain层(纯Kotlin)和Data层(Android/SQL)的桥梁,是最关键的胶水层。
 *
 * 工作流程:
 * 1. saveProfile: Domain对象 → 拆包 → 转换 → 封装成Entity → DAO写入
 * 2. getAllProfiles: DAO查询(Flow) → 数据清洗 → 还原 → 返回Domain对象Flow
 *
 * 映射规范(写在文件底部):
 * - toDomain(): Entity → Domain Model
 * - toEntity(): Domain Model → Entity
 */
class ContactRepositoryImpl @Inject constructor(
    private val dao: ContactDao
) : ContactRepository {

    private val moshi = Moshi.Builder().build()
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)

    /**
     * 获取所有联系人画像
     *
     * 1. 对接管道:调用dao.getAllProfiles()拿到Flow<List<Entity>>
     * 2. 数据清洗:使用.map操作符转换数据流
     * 3. 还原:遍历List,把每个Entity转换成Domain Model
     * 4. 交付:最终吐出Flow<List<ContactProfile>>给UseCase
     *
     * @return 联系人画像列表的Flow
     */
    override fun getAllProfiles(): Flow<List<ContactProfile>> {
        return dao.getAllProfiles().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * 根据ID获取单个联系人画像
     *
     * @param id 联系人ID
     * @return 包含联系人画像或null的Result
     */
    override suspend fun getProfile(id: String): Result<ContactProfile?> {
        return try {
            val entity = dao.getProfileById(id)
            Result.success(entity?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 保存联系人画像
     *
     * @param profile 要保存的联系人画像
     * @return 操作结果
     */
    override suspend fun saveProfile(profile: ContactProfile): Result<Unit> {
        return try {
            val entity = profile.toEntity()
            dao.insertOrUpdate(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新联系人的事实字段(增量更新)
     *
     * 场景:AI从聊天记录分析出新爱好,只需更新{"爱好":"滑雪"}
     *
     * 1. 先读取现有联系人数据
     * 2. 合并新的facts到现有的facts中
     * 3. 保存更新后的完整联系人画像
     *
     * @param contactId 联系人ID
     * @param newFacts 新的事实键值对
     * @return 操作结果
     */
    override suspend fun updateContactFacts(
        contactId: String,
        newFacts: Map<String, String>
    ): Result<Unit> {
        return try {
            val existingEntity = dao.getProfileById(contactId)
                ?: return Result.failure(Exception("Contact not found: $contactId"))

            val adapter = moshi.adapter<Map<String, String>>(mapType)
            val existingFacts = existingEntity.factsJson.let {
                if (it.isNotEmpty()) {
                    adapter.fromJson(it) ?: emptyMap()
                } else {
                    emptyMap()
                }
            }

            val updatedFacts = existingFacts.toMutableMap().apply {
                putAll(newFacts)
            }

            val updatedEntity = existingEntity.copy(
                factsJson = adapter.toJson(updatedFacts)
            )

            dao.insertOrUpdate(updatedEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除联系人画像
     *
     * 注意:此方法不会级联删除相关标签,需要在UseCase中协调处理
     *
     * @param id 联系人ID
     * @return 操作结果
     */
    override suspend fun deleteProfile(id: String): Result<Unit> {
        return try {
            dao.deleteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ============================================================================
// 私有映射函数 (写在文件底部)
// 规范:
// 1. 禁止使用Repository类外部暴露Entity
// 2. 必须编写私有的扩展函数toDomain()和toEntity()
// 3. 位置:直接写在RepositoryImpl.kt文件的底部
// ============================================================================

/**
 * Entity → Domain Model 转换
 *
 * 把ContactProfileEntity转换为Domain层的ContactProfile。
 * 核心工作:把factsJson(JSON字符串)还原成Map<String, String>。
 *
 * @return Domain层的ContactProfile对象
 */
private fun ContactProfileEntity.toDomain(): ContactProfile {
    val moshi = Moshi.Builder().build()
    val mapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    val adapter = moshi.adapter<Map<String, String>>(mapType)

    val factsMap = try {
        adapter.fromJson(this.factsJson) ?: emptyMap()
    } catch (e: Exception) {
        emptyMap<String, String>()
    }

    return ContactProfile(
        id = this.id,
        name = this.name,
        targetGoal = this.targetGoal,
        contextDepth = this.contextDepth,
        facts = factsMap
    )
}

/**
 * Domain Model → Entity 转换
 *
 * 把Domain层的ContactProfile转换为ContactProfileEntity。
 * 核心工作:把facts Map转换为JSON字符串(使用Moshi)。
 *
 * @return Data层的ContactProfileEntity对象
 */
private fun ContactProfile.toEntity(): ContactProfileEntity {
    val moshi = Moshi.Builder().build()
    val mapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    val adapter = moshi.adapter<Map<String, String>>(mapType)

    val factsJson = adapter.toJson(this.facts)

    return ContactProfileEntity(
        id = this.id,
        name = this.name,
        targetGoal = this.targetGoal,
        contextDepth = this.contextDepth,
        factsJson = factsJson
    )
}
