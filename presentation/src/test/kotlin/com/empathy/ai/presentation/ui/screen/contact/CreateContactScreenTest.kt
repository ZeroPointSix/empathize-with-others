package com.empathy.ai.presentation.ui.screen.contact

import com.empathy.ai.presentation.ui.component.contact.ContactFormData
import com.empathy.ai.presentation.ui.component.contact.RelationshipType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CreateContactScreen 单元测试
 * 
 * TD-00020 T072: 测试表单验证、头像选择、提交流程
 * 
 * 关键测试场景:
 * - 必填字段验证（姓名不能为空）
 * - 头像选择和预览
 * - 完成按钮启用/禁用状态
 */
class CreateContactScreenTest {

    // ============================================================
    // ContactFormData 测试
    // ============================================================

    @Test
    fun `ContactFormData default values are correct`() {
        val formData = ContactFormData()
        
        assertEquals("", formData.name)
        assertEquals("", formData.nickname)
        assertEquals(RelationshipType.FRIEND, formData.relationshipType)
        assertEquals("", formData.notes)
    }

    @Test
    fun `ContactFormData with name is valid`() {
        val formData = ContactFormData(name = "张三")
        
        assertEquals("张三", formData.name)
        assertTrue(formData.isValid)
    }

    @Test
    fun `ContactFormData with empty name is invalid`() {
        val formData = ContactFormData(name = "")
        
        assertEquals("", formData.name)
        assertFalse(formData.isValid)
    }

    @Test
    fun `ContactFormData with blank name is invalid`() {
        val formData = ContactFormData(name = "   ")
        
        assertFalse(formData.isValid)
    }

    @Test
    fun `ContactFormData with all fields is valid`() {
        val formData = ContactFormData(
            name = "张三",
            nickname = "小张",
            relationshipType = RelationshipType.FAMILY,
            notes = "这是备注"
        )
        
        assertEquals("张三", formData.name)
        assertEquals("小张", formData.nickname)
        assertEquals(RelationshipType.FAMILY, formData.relationshipType)
        assertEquals("这是备注", formData.notes)
        assertTrue(formData.isValid)
    }

    // ============================================================
    // RelationshipType 测试
    // ============================================================

    @Test
    fun `RelationshipType enum has correct values`() {
        val types = RelationshipType.values()
        
        assertEquals(5, types.size)
        assertTrue(types.contains(RelationshipType.FRIEND))
        assertTrue(types.contains(RelationshipType.FAMILY))
        assertTrue(types.contains(RelationshipType.COLLEAGUE))
        assertTrue(types.contains(RelationshipType.PARTNER))
        assertTrue(types.contains(RelationshipType.OTHER))
    }

    @Test
    fun `RelationshipType FRIEND has correct display name`() {
        assertEquals("朋友", RelationshipType.FRIEND.displayName)
    }

    @Test
    fun `RelationshipType FAMILY has correct display name`() {
        assertEquals("家人", RelationshipType.FAMILY.displayName)
    }

    @Test
    fun `RelationshipType COLLEAGUE has correct display name`() {
        assertEquals("同事", RelationshipType.COLLEAGUE.displayName)
    }

    @Test
    fun `RelationshipType PARTNER has correct display name`() {
        assertEquals("伴侣", RelationshipType.PARTNER.displayName)
    }

    @Test
    fun `RelationshipType OTHER has correct display name`() {
        assertEquals("其他", RelationshipType.OTHER.displayName)
    }

    // ============================================================
    // 表单验证测试
    // ============================================================

    @Test
    fun `isDoneEnabled returns true when name is not blank`() {
        val formData = ContactFormData(name = "张三")
        
        assertTrue(formData.isDoneEnabled)
    }

    @Test
    fun `isDoneEnabled returns false when name is blank`() {
        val formData = ContactFormData(name = "")
        
        assertFalse(formData.isDoneEnabled)
    }

    @Test
    fun `isDoneEnabled returns false when name is whitespace only`() {
        val formData = ContactFormData(name = "   ")
        
        assertFalse(formData.isDoneEnabled)
    }

    // ============================================================
    // 表单数据更新测试
    // ============================================================

    @Test
    fun `updateName creates new instance with updated name`() {
        val original = ContactFormData(name = "张三")
        val updated = original.copy(name = "李四")
        
        assertEquals("张三", original.name)
        assertEquals("李四", updated.name)
    }

    @Test
    fun `updateNickname creates new instance with updated nickname`() {
        val original = ContactFormData(nickname = "小张")
        val updated = original.copy(nickname = "小李")
        
        assertEquals("小张", original.nickname)
        assertEquals("小李", updated.nickname)
    }

    @Test
    fun `updateRelationshipType creates new instance with updated type`() {
        val original = ContactFormData(relationshipType = RelationshipType.FRIEND)
        val updated = original.copy(relationshipType = RelationshipType.FAMILY)
        
        assertEquals(RelationshipType.FRIEND, original.relationshipType)
        assertEquals(RelationshipType.FAMILY, updated.relationshipType)
    }

    @Test
    fun `updateNotes creates new instance with updated notes`() {
        val original = ContactFormData(notes = "原始备注")
        val updated = original.copy(notes = "新备注")
        
        assertEquals("原始备注", original.notes)
        assertEquals("新备注", updated.notes)
    }

    // ============================================================
    // 边界值测试
    // ============================================================

    @Test
    fun `ContactFormData with very long name is valid`() {
        val longName = "张".repeat(100)
        val formData = ContactFormData(name = longName)
        
        assertEquals(100, formData.name.length)
        assertTrue(formData.isValid)
    }

    @Test
    fun `ContactFormData with single character name is valid`() {
        val formData = ContactFormData(name = "张")
        
        assertEquals(1, formData.name.length)
        assertTrue(formData.isValid)
    }

    @Test
    fun `ContactFormData with unicode name is valid`() {
        val formData = ContactFormData(name = "张三🎉")
        
        assertTrue(formData.isValid)
    }

    @Test
    fun `ContactFormData with special characters in name is valid`() {
        val formData = ContactFormData(name = "张-三_李")
        
        assertTrue(formData.isValid)
    }

    // ============================================================
    // 数据完整性测试
    // ============================================================

    @Test
    fun `ContactFormData equality works correctly`() {
        val formData1 = ContactFormData(
            name = "张三",
            nickname = "小张",
            relationshipType = RelationshipType.FRIEND,
            notes = "备注"
        )
        val formData2 = ContactFormData(
            name = "张三",
            nickname = "小张",
            relationshipType = RelationshipType.FRIEND,
            notes = "备注"
        )
        
        assertEquals(formData1, formData2)
    }

    @Test
    fun `ContactFormData inequality works correctly`() {
        val formData1 = ContactFormData(name = "张三")
        val formData2 = ContactFormData(name = "李四")
        
        assertFalse(formData1 == formData2)
    }
}
