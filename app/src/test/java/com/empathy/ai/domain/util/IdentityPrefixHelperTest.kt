package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.ActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * IdentityPrefixHelper 单元测试
 *
 * 测试身份前缀工具类的所有功能：
 * - addPrefix：添加身份前缀
 * - parse：解析身份前缀
 * - stripAllPrefixes：去除所有前缀
 * - rebuildWithPrefix：重建前缀
 * - hasPrefix：检查是否有前缀
 *
 * @see IdentityPrefixHelper
 * @see PRD-00008 输入内容身份识别与双向对话历史需求
 */
class IdentityPrefixHelperTest {

    // ========== addPrefix 测试 ==========

    @Test
    fun `addPrefix should add CONTACT prefix for ANALYZE action`() {
        val result = IdentityPrefixHelper.addPrefix("你好", ActionType.ANALYZE)
        assertEquals("【对方说】：你好", result)
    }

    @Test
    fun `addPrefix should add USER prefix for CHECK action`() {
        val result = IdentityPrefixHelper.addPrefix("你好", ActionType.CHECK)
        assertEquals("【我正在回复】：你好", result)
    }

    @Test
    fun `addPrefix should not duplicate prefix when already has same prefix`() {
        val result = IdentityPrefixHelper.addPrefix(
            "${IdentityPrefixHelper.PREFIX_CONTACT}你好",
            ActionType.ANALYZE
        )
        assertEquals("${IdentityPrefixHelper.PREFIX_CONTACT}你好", result)
    }

    @Test
    fun `addPrefix should replace different prefix with correct one`() {
        val result = IdentityPrefixHelper.addPrefix(
            "${IdentityPrefixHelper.PREFIX_USER}你好",
            ActionType.ANALYZE
        )
        assertEquals("${IdentityPrefixHelper.PREFIX_CONTACT}你好", result)
    }

    @Test
    fun `addPrefix should handle empty content`() {
        val result = IdentityPrefixHelper.addPrefix("", ActionType.ANALYZE)
        assertEquals("", result)
    }

    @Test
    fun `addPrefix should handle blank content`() {
        val result = IdentityPrefixHelper.addPrefix("   ", ActionType.ANALYZE)
        assertEquals("   ", result)
    }

    @Test
    fun `addPrefix should handle multiline content`() {
        val content = "第一行\n第二行\n第三行"
        val result = IdentityPrefixHelper.addPrefix(content, ActionType.ANALYZE)
        assertEquals("${IdentityPrefixHelper.PREFIX_CONTACT}第一行\n第二行\n第三行", result)
    }

    // ========== addPrefixByRole 测试 ==========

    @Test
    fun `addPrefixByRole should add CONTACT prefix for CONTACT role`() {
        val result = IdentityPrefixHelper.addPrefixByRole(
            "你好",
            IdentityPrefixHelper.IdentityRole.CONTACT
        )
        assertEquals("${IdentityPrefixHelper.PREFIX_CONTACT}你好", result)
    }

    @Test
    fun `addPrefixByRole should add USER prefix for USER role`() {
        val result = IdentityPrefixHelper.addPrefixByRole(
            "你好",
            IdentityPrefixHelper.IdentityRole.USER
        )
        assertEquals("${IdentityPrefixHelper.PREFIX_USER}你好", result)
    }

    @Test
    fun `addPrefixByRole should not add prefix for LEGACY role`() {
        val result = IdentityPrefixHelper.addPrefixByRole(
            "你好",
            IdentityPrefixHelper.IdentityRole.LEGACY
        )
        assertEquals("你好", result)
    }

    // ========== parse 测试 ==========

    @Test
    fun `parse should identify CONTACT role`() {
        val result = IdentityPrefixHelper.parse("${IdentityPrefixHelper.PREFIX_CONTACT}你好")
        assertEquals(IdentityPrefixHelper.IdentityRole.CONTACT, result.role)
        assertEquals("你好", result.content)
    }

    @Test
    fun `parse should identify USER role`() {
        val result = IdentityPrefixHelper.parse("${IdentityPrefixHelper.PREFIX_USER}你好")
        assertEquals(IdentityPrefixHelper.IdentityRole.USER, result.role)
        assertEquals("你好", result.content)
    }

    @Test
    fun `parse should identify LEGACY role for no prefix`() {
        val result = IdentityPrefixHelper.parse("你好")
        assertEquals(IdentityPrefixHelper.IdentityRole.LEGACY, result.role)
        assertEquals("你好", result.content)
    }

    @Test
    fun `parse should handle empty content`() {
        val result = IdentityPrefixHelper.parse("")
        assertEquals(IdentityPrefixHelper.IdentityRole.LEGACY, result.role)
        assertEquals("", result.content)
    }

    @Test
    fun `parse should handle multiline content with prefix`() {
        val content = "${IdentityPrefixHelper.PREFIX_CONTACT}第一行\n第二行"
        val result = IdentityPrefixHelper.parse(content)
        assertEquals(IdentityPrefixHelper.IdentityRole.CONTACT, result.role)
        assertEquals("第一行\n第二行", result.content)
    }

    // ========== stripAllPrefixes 测试 ==========

    @Test
    fun `stripAllPrefixes should remove single CONTACT prefix`() {
        val result = IdentityPrefixHelper.stripAllPrefixes(
            "${IdentityPrefixHelper.PREFIX_CONTACT}你好"
        )
        assertEquals("你好", result)
    }

    @Test
    fun `stripAllPrefixes should remove single USER prefix`() {
        val result = IdentityPrefixHelper.stripAllPrefixes(
            "${IdentityPrefixHelper.PREFIX_USER}你好"
        )
        assertEquals("你好", result)
    }

    @Test
    fun `stripAllPrefixes should remove double same prefix`() {
        val result = IdentityPrefixHelper.stripAllPrefixes(
            "${IdentityPrefixHelper.PREFIX_CONTACT}${IdentityPrefixHelper.PREFIX_CONTACT}你好"
        )
        assertEquals("你好", result)
    }

    @Test
    fun `stripAllPrefixes should remove triple same prefix`() {
        val result = IdentityPrefixHelper.stripAllPrefixes(
            "${IdentityPrefixHelper.PREFIX_CONTACT}${IdentityPrefixHelper.PREFIX_CONTACT}${IdentityPrefixHelper.PREFIX_CONTACT}你好"
        )
        assertEquals("你好", result)
    }

    @Test
    fun `stripAllPrefixes should remove mixed prefixes`() {
        val result = IdentityPrefixHelper.stripAllPrefixes(
            "${IdentityPrefixHelper.PREFIX_CONTACT}${IdentityPrefixHelper.PREFIX_USER}你好"
        )
        assertEquals("你好", result)
    }

    @Test
    fun `stripAllPrefixes should handle no prefix`() {
        val result = IdentityPrefixHelper.stripAllPrefixes("你好")
        assertEquals("你好", result)
    }

    @Test
    fun `stripAllPrefixes should handle empty string`() {
        val result = IdentityPrefixHelper.stripAllPrefixes("")
        assertEquals("", result)
    }

    // ========== getPrefixByRole 测试 ==========

    @Test
    fun `getPrefixByRole should return CONTACT prefix for CONTACT role`() {
        val result = IdentityPrefixHelper.getPrefixByRole(
            IdentityPrefixHelper.IdentityRole.CONTACT
        )
        assertEquals(IdentityPrefixHelper.PREFIX_CONTACT, result)
    }

    @Test
    fun `getPrefixByRole should return USER prefix for USER role`() {
        val result = IdentityPrefixHelper.getPrefixByRole(
            IdentityPrefixHelper.IdentityRole.USER
        )
        assertEquals(IdentityPrefixHelper.PREFIX_USER, result)
    }

    @Test
    fun `getPrefixByRole should return empty string for LEGACY role`() {
        val result = IdentityPrefixHelper.getPrefixByRole(
            IdentityPrefixHelper.IdentityRole.LEGACY
        )
        assertEquals("", result)
    }

    // ========== rebuildWithPrefix 测试 ==========

    @Test
    fun `rebuildWithPrefix should add CONTACT prefix`() {
        val result = IdentityPrefixHelper.rebuildWithPrefix(
            IdentityPrefixHelper.IdentityRole.CONTACT,
            "再见"
        )
        assertEquals("${IdentityPrefixHelper.PREFIX_CONTACT}再见", result)
    }

    @Test
    fun `rebuildWithPrefix should add USER prefix`() {
        val result = IdentityPrefixHelper.rebuildWithPrefix(
            IdentityPrefixHelper.IdentityRole.USER,
            "再见"
        )
        assertEquals("${IdentityPrefixHelper.PREFIX_USER}再见", result)
    }

    @Test
    fun `rebuildWithPrefix should not add prefix for LEGACY`() {
        val result = IdentityPrefixHelper.rebuildWithPrefix(
            IdentityPrefixHelper.IdentityRole.LEGACY,
            "你好"
        )
        assertEquals("你好", result)
    }

    @Test
    fun `rebuildWithPrefix should handle empty content`() {
        val result = IdentityPrefixHelper.rebuildWithPrefix(
            IdentityPrefixHelper.IdentityRole.CONTACT,
            ""
        )
        assertEquals(IdentityPrefixHelper.PREFIX_CONTACT, result)
    }

    // ========== hasPrefix 测试 ==========

    @Test
    fun `hasPrefix should return true for CONTACT prefix`() {
        val result = IdentityPrefixHelper.hasPrefix(
            "${IdentityPrefixHelper.PREFIX_CONTACT}你好"
        )
        assertTrue(result)
    }

    @Test
    fun `hasPrefix should return true for USER prefix`() {
        val result = IdentityPrefixHelper.hasPrefix(
            "${IdentityPrefixHelper.PREFIX_USER}你好"
        )
        assertTrue(result)
    }

    @Test
    fun `hasPrefix should return false for no prefix`() {
        val result = IdentityPrefixHelper.hasPrefix("你好")
        assertFalse(result)
    }

    @Test
    fun `hasPrefix should return false for empty string`() {
        val result = IdentityPrefixHelper.hasPrefix("")
        assertFalse(result)
    }

    // ========== IdentityRole 测试 ==========

    @Test
    fun `IdentityRole CONTACT should have correct displayName`() {
        assertEquals("对方", IdentityPrefixHelper.IdentityRole.CONTACT.displayName)
    }

    @Test
    fun `IdentityRole USER should have correct displayName`() {
        assertEquals("我", IdentityPrefixHelper.IdentityRole.USER.displayName)
    }

    @Test
    fun `IdentityRole LEGACY should have correct displayName`() {
        assertEquals("历史", IdentityPrefixHelper.IdentityRole.LEGACY.displayName)
    }

    // ========== 边界情况测试 ==========

    @Test
    fun `should handle content that looks like prefix but is not`() {
        // 内容中包含类似前缀的文字，但不是真正的前缀
        val content = "我说【对方说】这个词"
        val result = IdentityPrefixHelper.parse(content)
        assertEquals(IdentityPrefixHelper.IdentityRole.LEGACY, result.role)
        assertEquals(content, result.content)
    }

    @Test
    fun `should handle prefix in middle of content`() {
        // 前缀出现在内容中间
        val content = "你好${IdentityPrefixHelper.PREFIX_CONTACT}世界"
        val result = IdentityPrefixHelper.parse(content)
        assertEquals(IdentityPrefixHelper.IdentityRole.LEGACY, result.role)
        assertEquals(content, result.content)
    }

    @Test
    fun `should handle special characters in content`() {
        val content = "你好！@#$%^&*()_+{}|:\"<>?"
        val result = IdentityPrefixHelper.addPrefix(content, ActionType.ANALYZE)
        assertEquals("${IdentityPrefixHelper.PREFIX_CONTACT}$content", result)
    }

    @Test
    fun `should handle emoji in content`() {
        val content = "你好😀🎉"
        val result = IdentityPrefixHelper.addPrefix(content, ActionType.ANALYZE)
        assertEquals("${IdentityPrefixHelper.PREFIX_CONTACT}$content", result)
    }

    // ========== 完整流程测试 ==========

    @Test
    fun `full flow - add prefix then parse should return original content`() {
        val original = "测试内容"
        val prefixed = IdentityPrefixHelper.addPrefix(original, ActionType.ANALYZE)
        val parsed = IdentityPrefixHelper.parse(prefixed)
        
        assertEquals(IdentityPrefixHelper.IdentityRole.CONTACT, parsed.role)
        assertEquals(original, parsed.content)
    }

    @Test
    fun `full flow - edit and rebuild should preserve role`() {
        // 模拟编辑对话流程
        val original = "${IdentityPrefixHelper.PREFIX_CONTACT}原始内容"
        
        // 1. 解析获取角色和内容
        val parsed = IdentityPrefixHelper.parse(original)
        assertEquals(IdentityPrefixHelper.IdentityRole.CONTACT, parsed.role)
        assertEquals("原始内容", parsed.content)
        
        // 2. 用户编辑内容
        val editedContent = "编辑后的内容"
        
        // 3. 重建前缀
        val rebuilt = IdentityPrefixHelper.rebuildWithPrefix(parsed.role, editedContent)
        assertEquals("${IdentityPrefixHelper.PREFIX_CONTACT}编辑后的内容", rebuilt)
    }
}
