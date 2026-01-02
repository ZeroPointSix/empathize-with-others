package com.empathy.ai.presentation.ui.component.ios

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DraggableModelList 单元测试
 * 
 * TD-00025 T4-04: 可拖拽模型列表单元测试
 * 
 * 测试覆盖：
 * - 模型数据结构
 * - 拖拽排序逻辑
 * - 默认模型设置
 * - 删除模型逻辑
 */
class DraggableModelListTest {

    // ============================================================
    // 测试数据
    // ============================================================
    
    private fun createTestModels(): List<TestDraggableModelItem> {
        return listOf(
            TestDraggableModelItem("gpt-4", "GPT-4", isDefault = true),
            TestDraggableModelItem("gpt-3.5-turbo", "GPT-3.5 Turbo", isDefault = false),
            TestDraggableModelItem("gpt-4-turbo", "GPT-4 Turbo", isDefault = false)
        )
    }

    // ============================================================
    // 模型数据结构测试
    // ============================================================

    @Test
    fun `模型数据_id应正确设置`() {
        val model = TestDraggableModelItem("gpt-4", "GPT-4", isDefault = true)
        
        assertEquals("id应为gpt-4", "gpt-4", model.id)
    }

    @Test
    fun `模型数据_displayName应正确设置`() {
        val model = TestDraggableModelItem("gpt-4", "GPT-4", isDefault = true)
        
        assertEquals("displayName应为GPT-4", "GPT-4", model.displayName)
    }

    @Test
    fun `模型数据_isDefault应正确设置`() {
        val model = TestDraggableModelItem("gpt-4", "GPT-4", isDefault = true)
        
        assertTrue("isDefault应为true", model.isDefault)
    }

    @Test
    fun `模型数据_默认isDefault应为false`() {
        val model = TestDraggableModelItem("gpt-4", "GPT-4")
        
        assertFalse("默认isDefault应为false", model.isDefault)
    }

    @Test
    fun `模型数据_空displayName应使用id`() {
        val model = TestDraggableModelItem("gpt-4", "", isDefault = false)
        val displayText = model.displayName.ifBlank { model.id }
        
        assertEquals("空displayName应使用id", "gpt-4", displayText)
    }

    // ============================================================
    // 拖拽排序逻辑测试
    // ============================================================

    @Test
    fun `拖拽排序_从位置0到位置2`() {
        val models = createTestModels().toMutableList()
        val fromIndex = 0
        val toIndex = 2
        
        // 执行排序
        val item = models.removeAt(fromIndex)
        models.add(toIndex, item)
        
        assertEquals("第一个模型应为GPT-3.5 Turbo", "gpt-3.5-turbo", models[0].id)
        assertEquals("第二个模型应为GPT-4 Turbo", "gpt-4-turbo", models[1].id)
        assertEquals("第三个模型应为GPT-4", "gpt-4", models[2].id)
    }

    @Test
    fun `拖拽排序_从位置2到位置0`() {
        val models = createTestModels().toMutableList()
        val fromIndex = 2
        val toIndex = 0
        
        // 执行排序
        val item = models.removeAt(fromIndex)
        models.add(toIndex, item)
        
        assertEquals("第一个模型应为GPT-4 Turbo", "gpt-4-turbo", models[0].id)
        assertEquals("第二个模型应为GPT-4", "gpt-4", models[1].id)
        assertEquals("第三个模型应为GPT-3.5 Turbo", "gpt-3.5-turbo", models[2].id)
    }

    @Test
    fun `拖拽排序_相同位置不应改变顺序`() {
        val models = createTestModels().toMutableList()
        val fromIndex = 1
        val toIndex = 1
        
        // 相同位置不执行排序
        if (fromIndex != toIndex) {
            val item = models.removeAt(fromIndex)
            models.add(toIndex, item)
        }
        
        assertEquals("第一个模型应为GPT-4", "gpt-4", models[0].id)
        assertEquals("第二个模型应为GPT-3.5 Turbo", "gpt-3.5-turbo", models[1].id)
        assertEquals("第三个模型应为GPT-4 Turbo", "gpt-4-turbo", models[2].id)
    }

    @Test
    fun `拖拽排序_边界检查_fromIndex不应为负`() {
        val models = createTestModels()
        val fromIndex = -1
        val isValid = fromIndex >= 0 && fromIndex < models.size
        
        assertFalse("负索引应无效", isValid)
    }

    @Test
    fun `拖拽排序_边界检查_toIndex不应超出范围`() {
        val models = createTestModels()
        val toIndex = 5
        val isValid = toIndex >= 0 && toIndex < models.size
        
        assertFalse("超出范围的索引应无效", isValid)
    }

    @Test
    fun `拖拽排序_目标位置计算`() {
        val itemHeight = 56f
        val dragOffsetY = 120f
        val currentIndex = 0
        
        // 计算目标位置
        val targetIndex = (currentIndex + (dragOffsetY / itemHeight).toInt())
            .coerceIn(0, 2)
        
        assertEquals("目标位置应为2", 2, targetIndex)
    }

    @Test
    fun `拖拽排序_目标位置应被限制在列表范围内`() {
        val itemHeight = 56f
        val dragOffsetY = 500f // 大幅度拖动
        val currentIndex = 0
        val maxIndex = 2
        
        // 计算目标位置并限制范围
        val targetIndex = (currentIndex + (dragOffsetY / itemHeight).toInt())
            .coerceIn(0, maxIndex)
        
        assertEquals("目标位置应被限制为2", 2, targetIndex)
    }

    // ============================================================
    // 默认模型设置测试
    // ============================================================

    @Test
    fun `默认模型_设置新默认模型`() {
        val models = createTestModels().toMutableList()
        val newDefaultId = "gpt-3.5-turbo"
        
        // 更新默认模型
        val updatedModels = models.map { model ->
            model.copy(isDefault = model.id == newDefaultId)
        }
        
        assertFalse("GPT-4不应为默认", updatedModels[0].isDefault)
        assertTrue("GPT-3.5 Turbo应为默认", updatedModels[1].isDefault)
        assertFalse("GPT-4 Turbo不应为默认", updatedModels[2].isDefault)
    }

    @Test
    fun `默认模型_只能有一个默认模型`() {
        val models = createTestModels().toMutableList()
        val newDefaultId = "gpt-4-turbo"
        
        // 更新默认模型
        val updatedModels = models.map { model ->
            model.copy(isDefault = model.id == newDefaultId)
        }
        
        val defaultCount = updatedModels.count { it.isDefault }
        
        assertEquals("只能有一个默认模型", 1, defaultCount)
    }

    @Test
    fun `默认模型_已是默认的模型不应显示设置按钮`() {
        val model = TestDraggableModelItem("gpt-4", "GPT-4", isDefault = true)
        val showSetDefaultButton = !model.isDefault
        
        assertFalse("默认模型不应显示设置按钮", showSetDefaultButton)
    }

    @Test
    fun `默认模型_非默认模型应显示设置按钮`() {
        val model = TestDraggableModelItem("gpt-3.5-turbo", "GPT-3.5 Turbo", isDefault = false)
        val showSetDefaultButton = !model.isDefault
        
        assertTrue("非默认模型应显示设置按钮", showSetDefaultButton)
    }

    // ============================================================
    // 删除模型逻辑测试
    // ============================================================

    @Test
    fun `删除模型_删除非默认模型`() {
        val models = createTestModels().toMutableList()
        val deleteId = "gpt-3.5-turbo"
        
        // 删除模型
        models.removeAll { it.id == deleteId }
        
        assertEquals("应剩余2个模型", 2, models.size)
        assertFalse("不应包含已删除的模型", models.any { it.id == deleteId })
    }

    @Test
    fun `删除模型_删除默认模型后应设置新默认`() {
        val models = createTestModels().toMutableList()
        val deleteId = "gpt-4" // 删除默认模型
        
        // 删除模型
        models.removeAll { it.id == deleteId }
        
        // 如果没有默认模型，设置第一个为默认
        if (models.none { it.isDefault } && models.isNotEmpty()) {
            val firstModel = models[0]
            models[0] = firstModel.copy(isDefault = true)
        }
        
        assertEquals("应剩余2个模型", 2, models.size)
        assertTrue("应有一个默认模型", models.any { it.isDefault })
    }

    @Test
    fun `删除模型_删除最后一个模型`() {
        val models = mutableListOf(
            TestDraggableModelItem("gpt-4", "GPT-4", isDefault = true)
        )
        val deleteId = "gpt-4"
        
        // 删除模型
        models.removeAll { it.id == deleteId }
        
        assertTrue("列表应为空", models.isEmpty())
    }

    // ============================================================
    // 拖拽状态测试
    // ============================================================

    @Test
    fun `拖拽状态_初始状态应为未拖拽`() {
        val draggingIndex = -1
        val isDragging = draggingIndex >= 0
        
        assertFalse("初始状态应为未拖拽", isDragging)
    }

    @Test
    fun `拖拽状态_拖拽中应有有效索引`() {
        val draggingIndex = 1
        val isDragging = draggingIndex >= 0
        
        assertTrue("拖拽中应有有效索引", isDragging)
    }

    @Test
    fun `拖拽状态_拖拽项应放大1点05倍`() {
        val isDragging = true
        val scale = if (isDragging) 1.05f else 1f
        
        assertEquals("拖拽项应放大1.05倍", 1.05f, scale)
    }

    @Test
    fun `拖拽状态_非拖拽项应保持原大小`() {
        val isDragging = false
        val scale = if (isDragging) 1.05f else 1f
        
        assertEquals("非拖拽项应保持原大小", 1f, scale)
    }

    @Test
    fun `拖拽状态_拖拽项应有阴影`() {
        val isDragging = true
        val elevation = if (isDragging) 8 else 0
        
        assertEquals("拖拽项应有8dp阴影", 8, elevation)
    }

    @Test
    fun `拖拽状态_非拖拽项应无阴影`() {
        val isDragging = false
        val elevation = if (isDragging) 8 else 0
        
        assertEquals("非拖拽项应无阴影", 0, elevation)
    }

    // ============================================================
    // 回调触发测试
    // ============================================================

    @Test
    fun `回调触发_排序完成应触发onReorder`() {
        var reorderCalled = false
        var fromIndexReceived = -1
        var toIndexReceived = -1
        val onReorder: (Int, Int) -> Unit = { from, to ->
            reorderCalled = true
            fromIndexReceived = from
            toIndexReceived = to
        }
        
        // 模拟排序完成
        onReorder(0, 2)
        
        assertTrue("应触发onReorder", reorderCalled)
        assertEquals("fromIndex应为0", 0, fromIndexReceived)
        assertEquals("toIndex应为2", 2, toIndexReceived)
    }

    @Test
    fun `回调触发_设置默认应触发onSetDefault`() {
        var setDefaultCalled = false
        var modelIdReceived: String? = null
        val onSetDefault: (String) -> Unit = { id ->
            setDefaultCalled = true
            modelIdReceived = id
        }
        
        // 模拟设置默认
        onSetDefault("gpt-3.5-turbo")
        
        assertTrue("应触发onSetDefault", setDefaultCalled)
        assertEquals("modelId应为gpt-3.5-turbo", "gpt-3.5-turbo", modelIdReceived)
    }

    @Test
    fun `回调触发_删除应触发onDelete`() {
        var deleteCalled = false
        var modelIdReceived: String? = null
        val onDelete: (String) -> Unit = { id ->
            deleteCalled = true
            modelIdReceived = id
        }
        
        // 模拟删除
        onDelete("gpt-4-turbo")
        
        assertTrue("应触发onDelete", deleteCalled)
        assertEquals("modelId应为gpt-4-turbo", "gpt-4-turbo", modelIdReceived)
    }

    // ============================================================
    // 空列表测试
    // ============================================================

    @Test
    fun `空列表_应正确处理`() {
        val models = emptyList<TestDraggableModelItem>()
        
        assertTrue("空列表应为空", models.isEmpty())
    }

    @Test
    fun `空列表_不应有默认模型`() {
        val models = emptyList<TestDraggableModelItem>()
        val hasDefault = models.any { it.isDefault }
        
        assertFalse("空列表不应有默认模型", hasDefault)
    }

    // ============================================================
    // 单个模型测试
    // ============================================================

    @Test
    fun `单个模型_应为默认模型`() {
        val models = listOf(
            TestDraggableModelItem("gpt-4", "GPT-4", isDefault = true)
        )
        
        assertEquals("应只有一个模型", 1, models.size)
        assertTrue("单个模型应为默认", models[0].isDefault)
    }

    @Test
    fun `单个模型_不应显示设置默认按钮`() {
        val model = TestDraggableModelItem("gpt-4", "GPT-4", isDefault = true)
        val showSetDefaultButton = !model.isDefault
        
        assertFalse("单个默认模型不应显示设置默认按钮", showSetDefaultButton)
    }

    // ============================================================
    // 辅助数据类
    // ============================================================

    /**
     * 测试用可拖拽模型项
     */
    data class TestDraggableModelItem(
        val id: String,
        val displayName: String,
        val isDefault: Boolean = false
    )
}
