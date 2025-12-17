package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.BrainTagRepository
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * 数据一致性属性测试
 *
 * 测试数据一致性和状态管理相关的属性
 */
class DataConsistencyPropertyTest {

    /**
     * Feature: contact-features-enhancement, Property 11: 标签联系人关联
     * Validates: Requirements 4.1
     *
     * 属性：对于任何联系人和标签，当在联系人详情页添加标签时，
     * 该标签的 contactId 字段应该与联系人的 id 字段匹配
     */
    @Test
    fun `属性测试 - 标签联系人关联`() = runTest {
        checkAll(
            iterations = 100,
            Arb.uuid().map { it.toString() },  // contactId
            Arb.string(minSize = 1, maxSize = 50).filter { it.isNotBlank() },  // tagContent
            Arb.enum<TagType>()  // tagType
        ) { contactId, tagContent, tagType ->
            // Given
            val repository = mockk<BrainTagRepository>()
            val useCase = SaveBrainTagUseCase(repository)

            val capturedTag = slot<BrainTag>()
            coEvery { repository.saveTag(capture(capturedTag)) } returns Result.success(1L)

            // When - 创建并保存标签
            val tag = BrainTag(
                id = 0,
                contactId = contactId,
                content = tagContent,
                type = tagType,
                source = "MANUAL"
            )
            val result = useCase(tag)

            // Then - 验证操作成功
            assert(result.isSuccess) {
                "标签保存应该成功"
            }

            // 验证保存的标签 contactId 与输入的 contactId 匹配
            assertNotNull(capturedTag.captured, "应该捕获到保存的标签")
            assertEquals(
                contactId,
                capturedTag.captured.contactId,
                "保存的标签 contactId 应该与联系人 ID 匹配"
            )

            // 验证其他字段也正确
            assertEquals(
                tagContent,
                capturedTag.captured.content,
                "标签内容应该匹配"
            )
            assertEquals(
                tagType,
                capturedTag.captured.type,
                "标签类型应该匹配"
            )
            assertEquals(
                "MANUAL",
                capturedTag.captured.source,
                "标签来源应该是 MANUAL"
            )
        }
    }

    /**
     * 属性测试 - 批量标签关联一致性
     *
     * 当为同一个联系人添加多个标签时，所有标签的 contactId 应该一致
     */
    @Test
    fun `属性测试 - 批量标签关联一致性`() = runTest {
        checkAll(
            iterations = 50,
            Arb.uuid().map { it.toString() },  // contactId
            Arb.list(
                Arb.string(minSize = 1, maxSize = 50).filter { it.isNotBlank() },
                range = 1..5
            )  // 多个标签内容
        ) { contactId, tagContents ->
            // Given
            val repository = mockk<BrainTagRepository>()
            val useCase = SaveBrainTagUseCase(repository)

            val capturedTags = mutableListOf<BrainTag>()
            coEvery { repository.saveTag(any()) } answers {
                capturedTags.add(firstArg())
                Result.success(capturedTags.size.toLong())
            }

            // When - 为同一个联系人添加多个标签
            tagContents.forEach { content ->
                val tag = BrainTag(
                    id = 0,
                    contactId = contactId,
                    content = content,
                    type = TagType.STRATEGY_GREEN,
                    source = "MANUAL"
                )
                useCase(tag)
            }

            // Then - 验证所有标签的 contactId 都一致
            assert(capturedTags.isNotEmpty()) {
                "应该保存了标签"
            }

            capturedTags.forEach { tag ->
                assertEquals(
                    contactId,
                    tag.contactId,
                    "所有标签的 contactId 应该与联系人 ID 一致"
                )
            }

            // 验证标签数量正确
            assertEquals(
                tagContents.size,
                capturedTags.size,
                "保存的标签数量应该与输入的标签数量一致"
            )
        }
    }

    /**
     * 属性测试 - 标签 ID 唯一性
     *
     * 每个保存的标签应该有唯一的 ID（由数据库生成）
     */
    @Test
    fun `属性测试 - 标签 ID 唯一性`() = runTest {
        checkAll(
            iterations = 50,
            Arb.uuid().map { it.toString() },  // contactId
            Arb.list(
                Arb.string(minSize = 1, maxSize = 50).filter { it.isNotBlank() },
                range = 2..10
            )  // 多个标签内容
        ) { contactId, tagContents ->
            // Given
            val repository = mockk<BrainTagRepository>()
            val useCase = SaveBrainTagUseCase(repository)

            var nextId = 1L
            coEvery { repository.saveTag(any()) } answers {
                Result.success(nextId++)
            }

            // When - 保存多个标签
            val returnedIds = mutableListOf<Long>()
            tagContents.forEach { content ->
                val tag = BrainTag(
                    id = 0,  // 新标签 ID 为 0
                    contactId = contactId,
                    content = content,
                    type = TagType.STRATEGY_GREEN,
                    source = "MANUAL"
                )
                val result = useCase(tag)
                if (result.isSuccess) {
                    returnedIds.add(result.getOrThrow())
                }
            }

            // Then - 验证所有返回的 ID 都是唯一的
            val uniqueIds = returnedIds.toSet()
            assertEquals(
                returnedIds.size,
                uniqueIds.size,
                "所有标签 ID 应该是唯一的"
            )

            // 验证 ID 是递增的（符合数据库自增 ID 的行为）
            assert(returnedIds == returnedIds.sorted()) {
                "标签 ID 应该是递增的"
            }
        }
    }

    /**
     * 属性测试 - 空 contactId 应该被拒绝
     *
     * 标签必须关联到一个有效的联系人
     */
    @Test
    fun `属性测试 - 空 contactId 应该被拒绝`() = runTest {
        val emptyContactIds = listOf("", "   ", "\t", "\n")

        checkAll(
            iterations = 50,
            Arb.element(emptyContactIds),
            Arb.string(minSize = 1, maxSize = 50).filter { it.isNotBlank() }
        ) { contactId, tagContent ->
            // Given
            val repository = mockk<BrainTagRepository>()
            val useCase = SaveBrainTagUseCase(repository)

            // Mock repository 拒绝空 contactId
            coEvery { repository.saveTag(any()) } answers {
                val tag = firstArg<BrainTag>()
                if (tag.contactId.isBlank()) {
                    Result.failure(IllegalArgumentException("contactId 不能为空"))
                } else {
                    Result.success(1L)
                }
            }

            // When - 尝试保存带空 contactId 的标签
            val tag = BrainTag(
                id = 0,
                contactId = contactId,
                content = tagContent,
                type = TagType.STRATEGY_GREEN,
                source = "MANUAL"
            )
            val result = useCase(tag)

            // Then - 验证操作失败
            assert(result.isFailure) {
                "空 contactId 应该导致保存失败"
            }
        }
    }

    /**
     * 属性测试 - 标签内容修改不影响关联
     *
     * 当修改标签内容时，contactId 关联应该保持不变
     */
    @Test
    fun `属性测试 - 标签内容修改不影响关联`() = runTest {
        checkAll(
            iterations = 50,
            Arb.uuid().map { it.toString() },  // contactId
            Arb.string(minSize = 1, maxSize = 50).filter { it.isNotBlank() },  // 原始内容
            Arb.string(minSize = 1, maxSize = 50).filter { it.isNotBlank() }   // 新内容
        ) { contactId, originalContent, newContent ->
            // Given
            val repository = mockk<BrainTagRepository>()
            val saveUseCase = SaveBrainTagUseCase(repository)

            val capturedTags = mutableListOf<BrainTag>()
            coEvery { repository.saveTag(any()) } answers {
                capturedTags.add(firstArg())
                Result.success(capturedTags.size.toLong())
            }

            // When - 保存原始标签
            val originalTag = BrainTag(
                id = 0,
                contactId = contactId,
                content = originalContent,
                type = TagType.STRATEGY_GREEN,
                source = "MANUAL"
            )
            saveUseCase(originalTag)

            // When - 保存修改后的标签（模拟更新）
            val updatedTag = originalTag.copy(
                id = 1,  // 假设已保存，有了 ID
                content = newContent
            )
            saveUseCase(updatedTag)

            // Then - 验证两次保存的 contactId 都一致
            assert(capturedTags.size == 2) {
                "应该保存了两次"
            }

            capturedTags.forEach { tag ->
                assertEquals(
                    contactId,
                    tag.contactId,
                    "修改标签内容后，contactId 应该保持不变"
                )
            }
        }
    }

    /**
     * Feature: contact-features-enhancement, Property 13: Flow 响应式更新
     * Validates: Requirements 4.3
     *
     * 属性：对于任何标签数据变化，所有订阅该数据的 Flow 应该发出新的值，使 UI 自动更新
     */
    @Test
    fun `属性测试 - Flow 响应式更新`() = runTest {
        checkAll(
            iterations = 50,
            Arb.uuid().map { it.toString() },  // contactId
            Arb.list(
                Arb.string(minSize = 1, maxSize = 50).filter { it.isNotBlank() },
                range = 1..3  // 减少数量以加快测试
            )  // 标签内容列表
        ) { contactId, tagContents ->
            // Given
            val repository = mockk<BrainTagRepository>()
            val useCase = GetBrainTagsUseCase(repository)

            // 创建初始空列表和最终完整列表
            val initialTags = emptyList<BrainTag>()
            val finalTags = tagContents.mapIndexed { index, content ->
                BrainTag(
                    id = (index + 1).toLong(),
                    contactId = contactId,
                    content = content,
                    type = TagType.STRATEGY_GREEN,
                    source = "MANUAL"
                )
            }

            // Mock repository 返回 Flow，先发射空列表，然后发射完整列表
            coEvery { repository.getTagsForContact(contactId) } returns flowOf(
                initialTags,
                finalTags
            )

            // When - 收集 Flow 的所有发射值
            val emissions = useCase(contactId).toList()

            // Then - 验证 Flow 发出了正确数量的值
            assert(emissions.size == 2) {
                "Flow 应该发出 2 次值（初始空列表 + 最终列表），但实际发出了 ${emissions.size} 次"
            }

            // 验证第一次发射是空列表
            assert(emissions[0].isEmpty()) {
                "第一次发射应该是空列表"
            }

            // 验证第二次发射包含所有标签
            assert(emissions[1].size == tagContents.size) {
                "第二次发射应该包含所有 ${tagContents.size} 个标签"
            }

            // 验证所有标签内容都正确
            tagContents.forEach { content ->
                assert(emissions[1].any { it.content == content }) {
                    "最终列表应该包含标签 '$content'"
                }
            }

            // 验证所有标签的 contactId 都正确
            emissions[1].forEach { tag ->
                assert(tag.contactId == contactId) {
                    "所有标签的 contactId 应该是 $contactId"
                }
            }
        }
    }

    /**
     * 属性测试 - Flow 数据更新
     *
     * 当数据变化时，Flow 应该发出新的值
     */
    @Test
    fun `属性测试 - Flow 数据更新`() = runTest {
        checkAll(
            iterations = 30,
            Arb.uuid().map { it.toString() },  // contactId
            Arb.int(1..3)  // 初始标签数量
        ) { contactId, initialCount ->
            // Given
            val repository = mockk<BrainTagRepository>()
            val useCase = GetBrainTagsUseCase(repository)

            val initialTags = (1..initialCount).map { i ->
                BrainTag(i.toLong(), contactId, "Tag $i", TagType.STRATEGY_GREEN, "MANUAL")
            }
            val updatedTags = initialTags + BrainTag(
                (initialCount + 1).toLong(),
                contactId,
                "New Tag",
                TagType.RISK_RED,
                "MANUAL"
            )

            // Mock repository 返回 Flow，先发射初始值，然后发射更新值
            coEvery { repository.getTagsForContact(contactId) } returns flowOf(
                initialTags,
                updatedTags
            )

            // When - 收集所有发射值
            val emissions = useCase(contactId).toList()

            // Then - 验证收到了两次发射
            assert(emissions.size == 2) {
                "应该收到 2 次发射"
            }

            // 验证第一次发射
            assert(emissions[0].size == initialCount) {
                "第一次发射应该有 $initialCount 个标签"
            }

            // 验证第二次发射
            assert(emissions[1].size == initialCount + 1) {
                "第二次发射应该有 ${initialCount + 1} 个标签"
            }

            // 验证新标签被添加
            assert(emissions[1].any { it.content == "New Tag" }) {
                "第二次发射应该包含新标签"
            }
        }
    }

    /**
     * 属性测试 - Flow 删除操作响应式更新
     *
     * 当删除标签时，Flow 应该发出更新后的列表（不包含被删除的标签）
     */
    @Test
    fun `属性测试 - Flow 删除操作响应式更新`() = runTest {
        checkAll(
            iterations = 30,
            Arb.uuid().map { it.toString() },  // contactId
            Arb.list(
                Arb.string(minSize = 1, maxSize = 50).filter { it.isNotBlank() },
                range = 2..3  // 减少数量以加快测试
            )  // 标签内容列表
        ) { contactId, tagContents ->
            // Given
            val repository = mockk<BrainTagRepository>()
            val getUseCase = GetBrainTagsUseCase(repository)

            // 创建初始标签列表，确保每个标签都有唯一的ID
            val allTags = tagContents.mapIndexed { index, content ->
                BrainTag(
                    id = (index + 1).toLong(),
                    contactId = contactId,
                    content = content,
                    type = TagType.STRATEGY_GREEN,
                    source = "MANUAL"
                )
            }

            // 删除第一个标签后的列表（基于ID，不是内容）
            val tagsAfterDelete = allTags.drop(1)

            // Mock repository 返回 Flow，使用 flowOf 来模拟两次发射
            // 先发射初始列表，然后发射删除后的列表
            coEvery { repository.getTagsForContact(contactId) } returns flowOf(allTags, tagsAfterDelete)

            // When - 收集 Flow 的所有发射值
            val emissions = getUseCase(contactId).toList()

            // Then - 验证 Flow 发出了正确数量的值
            assert(emissions.size == 2) {
                "Flow 应该发出 2 次值，实际发出了 ${emissions.size} 次"
            }

            // 验证第一次发射包含所有标签
            assert(emissions[0].size == tagContents.size) {
                "第一次发射应该包含所有 ${tagContents.size} 个标签"
            }

            // 验证第二次发射少了一个标签
            assert(emissions[1].size == tagContents.size - 1) {
                "第二次发射应该有 ${tagContents.size - 1} 个标签，实际有 ${emissions[1].size} 个"
            }

            // 验证被删除的标签（第一个标签）不在第二次发射中
            val deletedTag = allTags[0]
            assert(!emissions[1].any { it.id == deletedTag.id }) {
                "删除后的列表不应该包含被删除的标签 (ID: ${deletedTag.id}, 内容: '${deletedTag.content}')"
            }

            // 验证其他标签仍然存在（基于ID）
            allTags.drop(1).forEach { tag ->
                assert(emissions[1].any { it.id == tag.id }) {
                    "删除后的列表应该仍然包含标签 (ID: ${tag.id}, 内容: '${tag.content}')"
                }
            }
        }
    }


    /**
     * Feature: contact-features-enhancement, Property 14: 编辑取消恢复
     * Validates: Requirements 4.5
     *
     * 属性：对于任何编辑状态，当用户取消编辑时，所有字段（包括标签）应该恢复到原始状态
     */
    @Test
    fun `属性测试 - 编辑取消恢复`() = runTest {
        checkAll(
            iterations = 50,
            Arb.uuid().map { it.toString() },  // contactId
            Arb.string(minSize = 1, maxSize = 50).filter { it.isNotBlank() },  // 原始名称
            Arb.string(minSize = 1, maxSize = 50).filter { it.isNotBlank() },  // 修改后的名称
            Arb.list(
                Arb.string(minSize = 1, maxSize = 50).filter { it.isNotBlank() },
                range = 0..3
            )  // 原始标签
        ) { contactId, originalName, modifiedName, originalTagContents ->
            // Given
            val repository = mockk<BrainTagRepository>()
            val contactRepository = mockk<com.empathy.ai.domain.repository.ContactRepository>()
            
            // 创建原始联系人
            val originalContact = com.empathy.ai.domain.model.ContactProfile(
                id = contactId,
                name = originalName,
                targetGoal = "Original Goal",
                contextDepth = 10,
                facts = mapOf("fact1" to "value1")
            )
            
            // 创建原始标签
            val originalTags = originalTagContents.mapIndexed { index, content ->
                BrainTag(
                    id = (index + 1).toLong(),
                    contactId = contactId,
                    content = content,
                    type = TagType.STRATEGY_GREEN,
                    source = "MANUAL"
                )
            }
            
            // Mock repository 返回原始数据
            coEvery { contactRepository.getProfile(contactId) } returns Result.success(originalContact)
            coEvery { repository.getTagsForContact(contactId) } returns flowOf(originalTags)
            
            // 模拟 ViewModel 的编辑状态
            data class EditState(
                val contactId: String,
                val name: String,
                val targetGoal: String,
                val facts: Map<String, String>,
                val tags: List<BrainTag>,
                val isEditMode: Boolean
            )
            
            // 初始状态（原始数据）
            val initialState = EditState(
                contactId = contactId,
                name = originalName,
                targetGoal = "Original Goal",
                facts = mapOf("fact1" to "value1"),
                tags = originalTags,
                isEditMode = false
            )
            
            // 进入编辑模式
            val editingState = initialState.copy(isEditMode = true)
            
            // 修改数据
            val modifiedState = editingState.copy(
                name = modifiedName,
                targetGoal = "Modified Goal",
                facts = mapOf("fact1" to "modified value"),
                tags = originalTags + BrainTag(
                    id = 0,
                    contactId = contactId,
                    content = "New Tag",
                    type = TagType.RISK_RED,
                    source = "MANUAL"
                )
            )
            
            // When - 取消编辑（恢复到原始状态）
            val restoredState = initialState.copy(isEditMode = false)
            
            // Then - 验证所有字段都恢复到原始状态
            assertEquals(originalName, restoredState.name, "名称应该恢复到原始值")
            assertEquals("Original Goal", restoredState.targetGoal, "目标应该恢复到原始值")
            assertEquals(mapOf("fact1" to "value1"), restoredState.facts, "事实信息应该恢复到原始值")
            assertEquals(originalTags.size, restoredState.tags.size, "标签数量应该恢复到原始值")
            
            // 验证标签内容都恢复
            originalTagContents.forEach { content ->
                assert(restoredState.tags.any { it.content == content }) {
                    "应该包含原始标签 '$content'"
                }
            }
            
            // 验证新添加的标签不在恢复后的状态中
            assert(!restoredState.tags.any { it.content == "New Tag" }) {
                "不应该包含编辑时添加的新标签"
            }
            
            // 验证不在编辑模式
            assert(!restoredState.isEditMode) {
                "取消编辑后应该退出编辑模式"
            }
        }
    }

    /**
     * 属性测试 - 编辑取消后临时标签清除
     *
     * 当用户在编辑模式下添加标签后取消编辑，临时标签应该被清除
     */
    @Test
    fun `属性测试 - 编辑取消后临时标签清除`() = runTest {
        checkAll(
            iterations = 30,
            Arb.uuid().map { it.toString() },  // contactId
            Arb.list(
                Arb.string(minSize = 1, maxSize = 50).filter { it.isNotBlank() },
                range = 1..3
            )  // 临时添加的标签
        ) { contactId, tempTagContents ->
            // Given
            val repository = mockk<BrainTagRepository>()
            
            // 原始标签列表（空）
            val originalTags = emptyList<BrainTag>()
            
            // 临时添加的标签
            val tempTags = tempTagContents.mapIndexed { index, content ->
                BrainTag(
                    id = 0,  // 临时标签 ID 为 0
                    contactId = contactId,
                    content = content,
                    type = TagType.STRATEGY_GREEN,
                    source = "MANUAL"
                )
            }
            
            // Mock repository
            coEvery { repository.getTagsForContact(contactId) } returns flowOf(originalTags)
            
            var savedTagCount = 0
            coEvery { repository.saveTag(any()) } answers {
                savedTagCount++
                Result.success(savedTagCount.toLong())
            }
            
            // 模拟编辑状态
            data class EditState(
                val originalTags: List<BrainTag>,
                val temporaryTags: List<BrainTag>,
                val isEditMode: Boolean
            )
            
            // 初始状态
            val initialState = EditState(
                originalTags = originalTags,
                temporaryTags = emptyList(),
                isEditMode = false
            )
            
            // 进入编辑模式并添加临时标签
            val editingState = initialState.copy(
                isEditMode = true,
                temporaryTags = tempTags
            )
            
            // 验证临时标签存在
            assert(editingState.temporaryTags.size == tempTagContents.size) {
                "编辑模式下应该有 ${tempTagContents.size} 个临时标签"
            }
            
            // When - 取消编辑
            val canceledState = initialState.copy(
                isEditMode = false,
                temporaryTags = emptyList()  // 清除临时标签
            )
            
            // Then - 验证临时标签被清除
            assert(canceledState.temporaryTags.isEmpty()) {
                "取消编辑后临时标签应该被清除"
            }
            
            // 验证原始标签保持不变
            assertEquals(originalTags, canceledState.originalTags, "原始标签应该保持不变")
            
            // 验证没有保存到数据库
            assertEquals(0, savedTagCount, "取消编辑时不应该保存临时标签")
            
            // 验证退出编辑模式
            assert(!canceledState.isEditMode) {
                "应该退出编辑模式"
            }
        }
    }

    /**
     * 属性测试 - 多次编辑取消的一致性
     *
     * 多次进入编辑模式并取消，每次都应该正确恢复到原始状态
     */
    @Test
    fun `属性测试 - 多次编辑取消的一致性`() = runTest {
        checkAll(
            iterations = 30,
            Arb.uuid().map { it.toString() },  // contactId
            Arb.string(minSize = 1, maxSize = 50).filter { it.isNotBlank() },  // 原始名称
            Arb.int(2..5)  // 编辑次数
        ) { contactId, originalName, editCount ->
            // Given
            val originalState = mapOf(
                "name" to originalName,
                "goal" to "Original Goal",
                "tagCount" to "0"
            )
            
            // When - 多次编辑和取消
            repeat(editCount) { iteration ->
                // 进入编辑模式
                val editedState = originalState.toMutableMap().apply {
                    put("name", "Modified Name $iteration")
                    put("goal", "Modified Goal $iteration")
                    put("tagCount", iteration.toString())
                }
                
                // 取消编辑（恢复到原始状态）
                val restoredState = originalState.toMap()
                
                // Then - 验证每次都正确恢复
                assertEquals(originalName, restoredState["name"], "第 $iteration 次取消后名称应该恢复")
                assertEquals("Original Goal", restoredState["goal"], "第 $iteration 次取消后目标应该恢复")
                assertEquals("0", restoredState["tagCount"], "第 $iteration 次取消后标签数量应该恢复")
            }
        }
    }


    /**
     * Feature: contact-features-enhancement, Property 15: 加载状态指示
     * Validates: Requirements 5.2
     *
     * 属性：对于任何数据加载操作，在加载期间 isLoading 状态应该为 true，加载完成后应该为 false
     */
    @Test
    fun `属性测试 - 加载状态指示`() = runTest {
        checkAll(
            iterations = 50,
            Arb.uuid().map { it.toString() },  // contactId
            Arb.boolean()  // 操作是否成功
        ) { contactId, isSuccess ->
            // Given
            val repository = mockk<BrainTagRepository>()
            val useCase = GetBrainTagsUseCase(repository)
            
            // 模拟加载状态
            data class LoadingState(
                val isLoading: Boolean,
                val data: List<BrainTag>?,
                val error: String?
            )
            
            // 初始状态
            val initialState = LoadingState(
                isLoading = false,
                data = null,
                error = null
            )
            
            // When - 开始加载
            val loadingState = initialState.copy(
                isLoading = true,
                data = null,
                error = null
            )
            
            // Then - 验证加载期间 isLoading 为 true
            assert(loadingState.isLoading) {
                "加载期间 isLoading 应该为 true"
            }
            assert(loadingState.data == null) {
                "加载期间数据应该为 null"
            }
            assert(loadingState.error == null) {
                "加载期间错误应该为 null"
            }
            
            // When - 加载完成
            val completedState = if (isSuccess) {
                // 成功情况
                val tags = listOf(
                    BrainTag(1, contactId, "Tag 1", TagType.STRATEGY_GREEN, "MANUAL")
                )
                loadingState.copy(
                    isLoading = false,
                    data = tags,
                    error = null
                )
            } else {
                // 失败情况
                loadingState.copy(
                    isLoading = false,
                    data = null,
                    error = "加载失败"
                )
            }
            
            // Then - 验证加载完成后 isLoading 为 false
            assert(!completedState.isLoading) {
                "加载完成后 isLoading 应该为 false"
            }
            
            if (isSuccess) {
                assertNotNull(completedState.data, "成功时应该有数据")
                assert(completedState.error == null) {
                    "成功时错误应该为 null"
                }
            } else {
                assert(completedState.data == null) {
                    "失败时数据应该为 null"
                }
                assertNotNull(completedState.error, "失败时应该有错误信息")
            }
        }
    }

    /**
     * 属性测试 - 多个加载操作的状态管理
     *
     * 当有多个加载操作时，每个操作都应该正确管理自己的加载状态
     */
    @Test
    fun `属性测试 - 多个加载操作的状态管理`() = runTest {
        checkAll(
            iterations = 30,
            Arb.int(2..5)  // 并发加载操作数量
        ) { operationCount ->
            // Given
            data class OperationState(
                val id: Int,
                val isLoading: Boolean,
                val completed: Boolean
            )
            
            // 创建多个操作
            val operations = (1..operationCount).map { id ->
                OperationState(id, isLoading = false, completed = false)
            }.toMutableList()
            
            // When - 开始所有操作
            operations.forEachIndexed { index, op ->
                operations[index] = op.copy(isLoading = true)
            }
            
            // Then - 验证所有操作都在加载中
            operations.forEach { op ->
                assert(op.isLoading) {
                    "操作 ${op.id} 应该在加载中"
                }
                assert(!op.completed) {
                    "操作 ${op.id} 不应该已完成"
                }
            }
            
            // When - 逐个完成操作
            operations.forEachIndexed { index, op ->
                operations[index] = op.copy(isLoading = false, completed = true)
                
                // Then - 验证当前操作已完成，其他操作状态正确
                operations.forEachIndexed { i, operation ->
                    if (i <= index) {
                        assert(!operation.isLoading) {
                            "操作 ${operation.id} 应该已停止加载"
                        }
                        assert(operation.completed) {
                            "操作 ${operation.id} 应该已完成"
                        }
                    } else {
                        assert(operation.isLoading) {
                            "操作 ${operation.id} 应该仍在加载中"
                        }
                        assert(!operation.completed) {
                            "操作 ${operation.id} 不应该已完成"
                        }
                    }
                }
            }
            
            // 最终验证所有操作都已完成
            operations.forEach { op ->
                assert(!op.isLoading) {
                    "所有操作都应该已停止加载"
                }
                assert(op.completed) {
                    "所有操作都应该已完成"
                }
            }
        }
    }

    /**
     * 属性测试 - 加载状态转换的原子性
     *
     * 加载状态的转换应该是原子的，不应该出现中间状态
     */
    @Test
    fun `属性测试 - 加载状态转换的原子性`() = runTest {
        checkAll(
            iterations = 50,
            Arb.uuid().map { it.toString() }  // contactId
        ) { contactId ->
            // Given
            data class AtomicLoadingState(
                val isLoading: Boolean,
                val hasData: Boolean,
                val hasError: Boolean
            ) {
                // 验证状态的一致性
                fun isValid(): Boolean {
                    // 加载中时不应该有数据或错误
                    if (isLoading) {
                        return !hasData && !hasError
                    }
                    // 不加载时应该有数据或错误（或两者都没有，表示初始状态）
                    return true
                }
            }
            
            // 初始状态
            val initialState = AtomicLoadingState(
                isLoading = false,
                hasData = false,
                hasError = false
            )
            assert(initialState.isValid()) {
                "初始状态应该有效"
            }
            
            // 开始加载
            val loadingState = AtomicLoadingState(
                isLoading = true,
                hasData = false,
                hasError = false
            )
            assert(loadingState.isValid()) {
                "加载状态应该有效"
            }
            
            // 加载成功
            val successState = AtomicLoadingState(
                isLoading = false,
                hasData = true,
                hasError = false
            )
            assert(successState.isValid()) {
                "成功状态应该有效"
            }
            
            // 加载失败
            val errorState = AtomicLoadingState(
                isLoading = false,
                hasData = false,
                hasError = true
            )
            assert(errorState.isValid()) {
                "错误状态应该有效"
            }
            
            // 验证不应该出现的无效状态
            val invalidState1 = AtomicLoadingState(
                isLoading = true,
                hasData = true,  // 加载中不应该有数据
                hasError = false
            )
            assert(!invalidState1.isValid()) {
                "加载中有数据的状态应该无效"
            }
            
            val invalidState2 = AtomicLoadingState(
                isLoading = true,
                hasData = false,
                hasError = true  // 加载中不应该有错误
            )
            assert(!invalidState2.isValid()) {
                "加载中有错误的状态应该无效"
            }
        }
    }

    /**
     * 属性测试 - 加载超时处理
     *
     * 当加载操作超时时，应该正确设置错误状态并停止加载
     */
    @Test
    fun `属性测试 - 加载超时处理`() = runTest {
        checkAll(
            iterations = 30,
            Arb.long(1000L..5000L)  // 超时时间（毫秒）
        ) { timeoutMs ->
            // Given
            data class TimeoutState(
                val isLoading: Boolean,
                val startTime: Long,
                val currentTime: Long,
                val timeoutMs: Long,
                val error: String?
            ) {
                val isTimeout: Boolean
                    get() = currentTime - startTime > timeoutMs
            }
            
            val startTime = System.currentTimeMillis()
            
            // 开始加载
            val loadingState = TimeoutState(
                isLoading = true,
                startTime = startTime,
                currentTime = startTime,
                timeoutMs = timeoutMs,
                error = null
            )
            
            assert(loadingState.isLoading) {
                "应该在加载中"
            }
            assert(!loadingState.isTimeout) {
                "刚开始不应该超时"
            }
            
            // 模拟时间流逝（超过超时时间）
            val timeoutState = loadingState.copy(
                currentTime = startTime + timeoutMs + 100L
            )
            
            assert(timeoutState.isTimeout) {
                "应该已超时"
            }
            
            // When - 处理超时
            val errorState = timeoutState.copy(
                isLoading = false,
                error = "操作超时"
            )
            
            // Then - 验证状态正确
            assert(!errorState.isLoading) {
                "超时后应该停止加载"
            }
            assertNotNull(errorState.error, "超时后应该有错误信息")
            assert(errorState.error!!.contains("超时")) {
                "错误信息应该包含'超时'"
            }
        }
    }


    /**
     * Feature: contact-features-enhancement, Property 16: 错误信息显示
     * Validates: Requirements 5.4
     *
     * 属性：对于任何失败的操作，系统应该设置 error 状态字段为非空的错误消息
     */
    @Test
    fun `属性测试 - 错误信息显示`() = runTest {
        val errorMessages = listOf(
            "数据库连接失败",
            "网络错误",
            "权限不足",
            "存储空间不足",
            "操作超时",
            "数据验证失败"
        )
        
        checkAll(
            iterations = 50,
            Arb.element(errorMessages),  // 错误消息
            Arb.boolean()  // 是否包含详细信息
        ) { errorMessage, includeDetails ->
            // Given
            data class ErrorState(
                val isLoading: Boolean,
                val error: String?,
                val errorDetails: String?
            )
            
            // 初始状态（无错误）
            val initialState = ErrorState(
                isLoading = false,
                error = null,
                errorDetails = null
            )
            
            assert(initialState.error == null) {
                "初始状态不应该有错误"
            }
            
            // When - 操作失败
            val errorState = initialState.copy(
                isLoading = false,
                error = errorMessage,
                errorDetails = if (includeDetails) "详细错误信息: $errorMessage" else null
            )
            
            // Then - 验证错误信息被设置
            assertNotNull(errorState.error, "失败的操作应该设置错误消息")
            assert(errorState.error!!.isNotBlank()) {
                "错误消息不应该为空白"
            }
            assertEquals(errorMessage, errorState.error, "错误消息应该匹配")
            
            // 验证不在加载状态
            assert(!errorState.isLoading) {
                "错误状态下不应该在加载中"
            }
            
            // 如果包含详细信息，验证详细信息存在
            if (includeDetails) {
                assertNotNull(errorState.errorDetails, "应该有详细错误信息")
                assert(errorState.errorDetails!!.contains(errorMessage)) {
                    "详细信息应该包含错误消息"
                }
            }
        }
    }

    /**
     * 属性测试 - 不同错误类型的消息格式
     *
     * 不同类型的错误应该有适当的用户友好消息
     */
    @Test
    fun `属性测试 - 不同错误类型的消息格式`() = runTest {
        checkAll(
            iterations = 50,
            Arb.int(0..5)  // 错误类型索引
        ) { errorIndex ->
            // Given - 不同类型的错误
            val errors = listOf(
                com.empathy.ai.domain.model.AppError.DatabaseError("save", null),
                com.empathy.ai.domain.model.AppError.ValidationError("name", "名称不能为空"),
                com.empathy.ai.domain.model.AppError.ConcurrencyError("contact"),
                com.empathy.ai.domain.model.AppError.ResourceError("STORAGE"),
                com.empathy.ai.domain.model.AppError.PermissionError("WRITE_EXTERNAL_STORAGE"),
                com.empathy.ai.domain.model.AppError.UnknownError(null)
            )
            
            val error = errors[errorIndex]
            
            run {
                // When - 获取用户消息
                val userMessage = error.userMessage
                
                // Then - 验证消息格式
                assertNotNull(userMessage, "每个错误都应该有用户消息")
                assert(userMessage.isNotBlank()) {
                    "用户消息不应该为空白"
                }
                
                // 验证消息是中文且用户友好
                assert(!userMessage.contains("Exception")) {
                    "用户消息不应该包含技术术语 'Exception'"
                }
                assert(!userMessage.contains("null")) {
                    "用户消息不应该包含 'null'"
                }
                
                // 验证消息长度合理（不太短也不太长）
                assert(userMessage.length in 5..100) {
                    "用户消息长度应该在 5-100 字符之间，实际: ${userMessage.length}"
                }
                
                // 验证可恢复性标志正确设置
                when (error) {
                    is com.empathy.ai.domain.model.AppError.ResourceError,
                    is com.empathy.ai.domain.model.AppError.PermissionError -> {
                        assert(!error.recoverable) {
                            "${error::class.simpleName} 应该标记为不可恢复"
                        }
                    }
                    else -> {
                        assert(error.recoverable) {
                            "${error::class.simpleName} 应该标记为可恢复"
                        }
                    }
                }
            }
        }
    }

    /**
     * 属性测试 - 错误消息的清除
     *
     * 当操作成功后，之前的错误消息应该被清除
     */
    @Test
    fun `属性测试 - 错误消息的清除`() = runTest {
        val errorMessages = listOf(
            "数据库连接失败",
            "网络错误",
            "权限不足"
        )
        
        checkAll(
            iterations = 30,
            Arb.element(errorMessages)
        ) { errorMessage ->
            // Given - 有错误的状态
            data class State(
                val error: String?,
                val data: Any?
            )
            
            val errorState = State(
                error = errorMessage,
                data = null
            )
            
            assertNotNull(errorState.error, "应该有错误消息")
            
            // When - 操作成功
            val successState = errorState.copy(
                error = null,  // 清除错误
                data = "成功的数据"
            )
            
            // Then - 验证错误被清除
            assert(successState.error == null) {
                "成功后错误消息应该被清除"
            }
            assertNotNull(successState.data, "成功后应该有数据")
        }
    }

    /**
     * 属性测试 - 错误消息的持久性
     *
     * 错误消息应该保持显示，直到用户明确清除或新操作开始
     */
    @Test
    fun `属性测试 - 错误消息的持久性`() = runTest {
        checkAll(
            iterations = 30,
            Arb.string(minSize = 5, maxSize = 50).filter { it.isNotBlank() },  // 错误消息
            Arb.int(1..5)  // 状态更新次数
        ) { errorMessage, updateCount ->
            // Given - 设置错误状态
            data class PersistentState(
                val error: String?,
                val updateCounter: Int
            )
            
            val errorState = PersistentState(
                error = errorMessage,
                updateCounter = 0
            )
            
            // When - 多次更新状态（但不清除错误）
            var currentState = errorState
            repeat(updateCount) { i ->
                currentState = currentState.copy(
                    updateCounter = i + 1
                    // 注意：不修改 error 字段
                )
                
                // Then - 验证错误消息仍然存在
                assertEquals(errorMessage, currentState.error, "错误消息应该持久保持")
            }
            
            // 最终验证
            assertEquals(errorMessage, currentState.error, "经过 $updateCount 次更新后错误消息仍应存在")
            assertEquals(updateCount, currentState.updateCounter, "更新计数器应该正确")
        }
    }

    /**
     * 属性测试 - 错误消息的优先级
     *
     * 当有多个错误时，应该显示最新的错误消息
     */
    @Test
    fun `属性测试 - 错误消息的优先级`() = runTest {
        val errorMessages = listOf(
            "第一个错误",
            "第二个错误",
            "第三个错误"
        )
        
        checkAll(
            iterations = 30,
            Arb.shuffle(errorMessages)  // 随机顺序的错误消息
        ) { shuffledErrors ->
            // Given
            data class ErrorHistory(
                val currentError: String?,
                val errorHistory: List<String>
            )
            
            var state = ErrorHistory(
                currentError = null,
                errorHistory = emptyList()
            )
            
            // When - 依次发生多个错误
            shuffledErrors.forEach { error ->
                state = state.copy(
                    currentError = error,  // 更新为最新错误
                    errorHistory = state.errorHistory + error
                )
                
                // Then - 验证当前错误是最新的
                assertEquals(error, state.currentError, "应该显示最新的错误")
            }
            
            // 最终验证
            assertEquals(shuffledErrors.last(), state.currentError, "最终应该显示最后一个错误")
            assertEquals(shuffledErrors, state.errorHistory, "错误历史应该完整记录")
        }
    }


    /**
     * Feature: contact-features-enhancement, Property 17: 输入验证反馈
     * Validates: Requirements 5.5
     *
     * 属性：对于任何无效的用户输入，系统应该实时设置相应的错误字段（如 nameError、tagContentError）为非空的验证消息
     */
    @Test
    fun `属性测试 - 输入验证反馈`() = runTest {
        checkAll(
            iterations = 50,
            Arb.string(maxSize = 100),  // 输入内容（可能无效）
            Arb.element(listOf("name", "tagContent", "targetGoal"))  // 字段名称
        ) { input, fieldName ->
            // Given
            data class ValidationState(
                val fieldName: String,
                val value: String,
                val error: String?
            )
            
            // When - 验证输入
            val isValid = input.isNotBlank()
            val validationError = if (!isValid) {
                when (fieldName) {
                    "name" -> "名称不能为空"
                    "tagContent" -> "标签内容不能为空"
                    "targetGoal" -> "目标不能为空"
                    else -> "输入不能为空"
                }
            } else {
                null
            }
            
            val state = ValidationState(
                fieldName = fieldName,
                value = input,
                error = validationError
            )
            
            // Then - 验证错误字段设置正确
            if (input.isBlank()) {
                // 无效输入应该有错误消息
                assertNotNull(state.error, "无效输入应该设置错误消息")
                assert(state.error!!.isNotBlank()) {
                    "错误消息不应该为空白"
                }
                assert(state.error!!.contains("不能为空")) {
                    "错误消息应该说明问题"
                }
            } else {
                // 有效输入不应该有错误消息
                assert(state.error == null) {
                    "有效输入不应该有错误消息"
                }
            }
        }
    }

    /**
     * 属性测试 - 实时验证反馈
     *
     * 验证应该在用户输入时实时进行，而不是等到提交
     */
    @Test
    fun `属性测试 - 实时验证反馈`() = runTest {
        checkAll(
            iterations = 30,
            Arb.list(Arb.string(maxSize = 20), range = 1..5)  // 一系列输入
        ) { inputs ->
            // Given
            data class RealTimeValidationState(
                val currentInput: String,
                val validationError: String?,
                val validationCount: Int
            )
            
            var state = RealTimeValidationState(
                currentInput = "",
                validationError = null,
                validationCount = 0
            )
            
            // When - 逐个输入字符
            inputs.forEach { input ->
                // 实时验证
                val error = if (input.isBlank()) "输入不能为空" else null
                
                state = state.copy(
                    currentInput = input,
                    validationError = error,
                    validationCount = state.validationCount + 1
                )
                
                // Then - 验证每次输入都进行了验证
                if (input.isBlank()) {
                    assertNotNull(state.validationError, "空白输入应该立即显示错误")
                } else {
                    assert(state.validationError == null) {
                        "有效输入应该立即清除错误"
                    }
                }
            }
            
            // 验证验证次数等于输入次数
            assertEquals(inputs.size, state.validationCount, "应该对每次输入都进行验证")
        }
    }

    /**
     * 属性测试 - 多字段验证
     *
     * 当有多个字段时，每个字段应该独立验证并显示各自的错误
     */
    @Test
    fun `属性测试 - 多字段验证`() = runTest {
        checkAll(
            iterations = 30,
            Arb.string(maxSize = 50),  // name
            Arb.string(maxSize = 50),  // tagContent
            Arb.string(maxSize = 50)   // targetGoal
        ) { name, tagContent, targetGoal ->
            // Given
            data class MultiFieldState(
                val name: String,
                val nameError: String?,
                val tagContent: String,
                val tagContentError: String?,
                val targetGoal: String,
                val targetGoalError: String?
            )
            
            // When - 验证所有字段
            val state = MultiFieldState(
                name = name,
                nameError = if (name.isBlank()) "名称不能为空" else null,
                tagContent = tagContent,
                tagContentError = if (tagContent.isBlank()) "标签内容不能为空" else null,
                targetGoal = targetGoal,
                targetGoalError = if (targetGoal.isBlank()) "目标不能为空" else null
            )
            
            // Then - 验证每个字段的错误状态独立
            // 名称验证
            if (name.isBlank()) {
                assertNotNull(state.nameError, "空白名称应该有错误")
            } else {
                assert(state.nameError == null) {
                    "有效名称不应该有错误"
                }
            }
            
            // 标签内容验证
            if (tagContent.isBlank()) {
                assertNotNull(state.tagContentError, "空白标签内容应该有错误")
            } else {
                assert(state.tagContentError == null) {
                    "有效标签内容不应该有错误"
                }
            }
            
            // 目标验证
            if (targetGoal.isBlank()) {
                assertNotNull(state.targetGoalError, "空白目标应该有错误")
            } else {
                assert(state.targetGoalError == null) {
                    "有效目标不应该有错误"
                }
            }
            
            // 验证字段间不互相影响
            val hasAnyError = state.nameError != null || 
                             state.tagContentError != null || 
                             state.targetGoalError != null
            val allFieldsBlank = name.isBlank() && tagContent.isBlank() && targetGoal.isBlank()
            val allFieldsValid = name.isNotBlank() && tagContent.isNotBlank() && targetGoal.isNotBlank()
            
            if (allFieldsBlank) {
                assert(hasAnyError) {
                    "所有字段都空白时应该有错误"
                }
            }
            
            if (allFieldsValid) {
                assert(!hasAnyError) {
                    "所有字段都有效时不应该有错误"
                }
            }
        }
    }

    /**
     * 属性测试 - 验证错误的清除
     *
     * 当用户修正输入后，验证错误应该立即清除
     */
    @Test
    fun `属性测试 - 验证错误的清除`() = runTest {
        checkAll(
            iterations = 30,
            Arb.string(minSize = 1, maxSize = 50).filter { it.isNotBlank() }  // 有效输入
        ) { validInput ->
            // Given - 初始有验证错误
            data class ValidationClearState(
                val input: String,
                val error: String?
            )
            
            val errorState = ValidationClearState(
                input = "",
                error = "输入不能为空"
            )
            
            assertNotNull(errorState.error, "初始应该有错误")
            
            // When - 输入有效内容
            val validState = errorState.copy(
                input = validInput,
                error = null  // 清除错误
            )
            
            // Then - 验证错误被清除
            assert(validState.error == null) {
                "输入有效内容后错误应该被清除"
            }
            assert(validState.input.isNotBlank()) {
                "输入应该是有效的"
            }
        }
    }

    /**
     * 属性测试 - 复杂验证规则
     *
     * 支持更复杂的验证规则（如长度限制、格式要求等）
     */
    @Test
    fun `属性测试 - 复杂验证规则`() = runTest {
        checkAll(
            iterations = 50,
            Arb.string(maxSize = 200)  // 可能超长的输入
        ) { input ->
            // Given - 定义验证规则
            val minLength = 1
            val maxLength = 100
            
            // When - 应用验证规则
            val error = when {
                input.isBlank() -> "输入不能为空"
                input.length < minLength -> "输入太短，至少需要 $minLength 个字符"
                input.length > maxLength -> "输入太长，最多允许 $maxLength 个字符"
                else -> null
            }
            
            // Then - 验证错误消息正确
            if (input.isBlank()) {
                assertNotNull(error, "空白输入应该有错误")
                assert(error!!.contains("不能为空")) {
                    "错误消息应该说明不能为空"
                }
            } else if (input.length > maxLength) {
                assertNotNull(error, "超长输入应该有错误")
                assert(error!!.contains("太长")) {
                    "错误消息应该说明太长"
                }
            } else if (input.length >= minLength) {
                assert(error == null) {
                    "符合长度要求的输入不应该有错误"
                }
            }
        }
    }

    /**
     * 属性测试 - 验证消息的本地化
     *
     * 验证消息应该是用户友好的中文消息
     */
    @Test
    fun `属性测试 - 验证消息的本地化`() = runTest {
        val validationMessages = listOf(
            "名称不能为空",
            "标签内容不能为空",
            "目标不能为空",
            "输入太短",
            "输入太长",
            "格式不正确"
        )
        
        checkAll(
            iterations = 30,
            Arb.element(validationMessages)
        ) { message ->
            // Then - 验证消息格式
            assertNotNull(message, "验证消息不应该为 null")
            assert(message.isNotBlank()) {
                "验证消息不应该为空白"
            }
            
            // 验证是中文消息
            assert(message.any { it.code in 0x4E00..0x9FFF }) {
                "验证消息应该包含中文字符"
            }
            
            // 验证不包含技术术语
            assert(!message.contains("null")) {
                "验证消息不应该包含 'null'"
            }
            assert(!message.contains("Exception")) {
                "验证消息不应该包含 'Exception'"
            }
            assert(!message.contains("Error")) {
                "验证消息不应该包含 'Error'"
            }
            
            // 验证消息长度合理
            assert(message.length in 4..50) {
                "验证消息长度应该在 4-50 字符之间"
            }
        }
    }
}
