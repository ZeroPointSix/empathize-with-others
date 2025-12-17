package com.empathy.ai.integration

import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.ConversationContextConfig
import com.empathy.ai.domain.model.MessageSender
import com.empathy.ai.domain.model.TimestampedMessage
import com.empathy.ai.domain.util.ConversationContextBuilder
import com.empathy.ai.domain.util.IdentityPrefixHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 身份前缀集成测试
 *
 * 测试身份识别系统的完整流程：
 * - 【帮我分析】→ 存储 → UI显示
 * - 【帮我检查】→ AI分析 → 不存储
 * - 编辑对话 → 保存 → 前缀保留
 * - 历史数据兼容性
 *
 * @see PRD-00008 输入内容身份识别与双向对话历史需求
 * @see TDD-00008 输入内容身份识别与双向对话历史技术设计
 */
class IdentityPrefixIntegrationTest {

    private lateinit var conversationContextBuilder: ConversationContextBuilder

    @Before
    fun setup() {
        conversationContextBuilder = ConversationContextBuilder()
    }

    // ========== 【帮我分析】完整流程测试 ==========

    @Test
    fun `ANALYZE flow - should add CONTACT prefix and store`() {
        // Given: 用户输入对方说的内容
        val userInput = "你怎么才回消息？"
        
        // When: 点击【帮我分析】，系统添加前缀
        val prefixedInput = IdentityPrefixHelper.addPrefix(userInput, ActionType.ANALYZE)
        
        // Then: 应该添加【对方说】前缀
        assertEquals("${IdentityPrefixHelper.PREFIX_CONTACT}你怎么才回消息？", prefixedInput)
        
        // And: 解析后应该识别为 CONTACT 角色
        val parsed = IdentityPrefixHelper.parse(prefixedInput)
        assertEquals(IdentityPrefixHelper.IdentityRole.CONTACT, parsed.role)
        assertEquals(userInput, parsed.content)
    }

    @Test
    fun `ANALYZE flow - multiline input should be handled correctly`() {
        // Given: 用户粘贴多行对话
        val userInput = """
            你怎么才回消息？
            我等了你好久
        """.trimIndent()
        
        // When: 点击【帮我分析】
        val prefixedInput = IdentityPrefixHelper.addPrefix(userInput, ActionType.ANALYZE)
        
        // Then: 整体添加前缀
        assertTrue(prefixedInput.startsWith(IdentityPrefixHelper.PREFIX_CONTACT))
        
        // And: 解析后内容完整
        val parsed = IdentityPrefixHelper.parse(prefixedInput)
        assertEquals(userInput, parsed.content)
    }

    // ========== 【帮我检查】完整流程测试 ==========

    @Test
    fun `CHECK flow - should add USER prefix but not store`() {
        // Given: 用户输入要发送的草稿
        val draft = "刚才在忙"
        
        // When: 点击【帮我检查】，系统添加前缀
        val prefixedDraft = IdentityPrefixHelper.addPrefix(draft, ActionType.CHECK)
        
        // Then: 应该添加【我正在回复】前缀
        assertEquals("${IdentityPrefixHelper.PREFIX_USER}刚才在忙", prefixedDraft)
        
        // And: 解析后应该识别为 USER 角色
        val parsed = IdentityPrefixHelper.parse(prefixedDraft)
        assertEquals(IdentityPrefixHelper.IdentityRole.USER, parsed.role)
        assertEquals(draft, parsed.content)
    }

    // ========== 编辑对话完整流程测试 ==========

    @Test
    fun `edit flow - should preserve role after editing`() {
        // Given: 存储的对话记录（带前缀）
        val storedContent = "${IdentityPrefixHelper.PREFIX_CONTACT}原始内容"
        
        // When: 用户打开编辑对话框
        val parsed = IdentityPrefixHelper.parse(storedContent)
        
        // Then: 编辑框显示纯文本
        assertEquals("原始内容", parsed.content)
        assertEquals(IdentityPrefixHelper.IdentityRole.CONTACT, parsed.role)
        
        // When: 用户修改内容并保存
        val editedContent = "修改后的内容"
        val rebuilt = IdentityPrefixHelper.rebuildWithPrefix(parsed.role, editedContent)
        
        // Then: 保存后保留原始角色
        assertEquals("${IdentityPrefixHelper.PREFIX_CONTACT}修改后的内容", rebuilt)
    }

    @Test
    fun `edit flow - LEGACY data should remain without prefix`() {
        // Given: 旧数据（无前缀）
        val legacyContent = "这是旧数据"
        
        // When: 用户编辑
        val parsed = IdentityPrefixHelper.parse(legacyContent)
        assertEquals(IdentityPrefixHelper.IdentityRole.LEGACY, parsed.role)
        
        // And: 修改后保存
        val editedContent = "修改后的旧数据"
        val rebuilt = IdentityPrefixHelper.rebuildWithPrefix(parsed.role, editedContent)
        
        // Then: 保持无前缀
        assertEquals("修改后的旧数据", rebuilt)
        assertFalse(IdentityPrefixHelper.hasPrefix(rebuilt))
    }

    // ========== 历史上下文构建测试 ==========

    @Test
    fun `history context - should preserve identity prefixes`() {
        // Given: 带身份前缀的历史消息
        val messages = listOf(
            TimestampedMessage(
                content = "${IdentityPrefixHelper.PREFIX_CONTACT}早安",
                timestamp = System.currentTimeMillis() - 3600000,
                sender = MessageSender.ME
            ),
            TimestampedMessage(
                content = "${IdentityPrefixHelper.PREFIX_USER}早呀",
                timestamp = System.currentTimeMillis() - 3500000,
                sender = MessageSender.ME
            ),
            TimestampedMessage(
                content = "${IdentityPrefixHelper.PREFIX_CONTACT}今天天气真好",
                timestamp = System.currentTimeMillis(),
                sender = MessageSender.ME
            )
        )
        
        // When: 构建历史上下文
        val context = conversationContextBuilder.buildHistoryContext(messages)
        
        // Then: 上下文中应该包含身份前缀
        assertTrue(context.contains(IdentityPrefixHelper.PREFIX_CONTACT))
        assertTrue(context.contains(IdentityPrefixHelper.PREFIX_USER))
    }

    @Test
    fun `history context - should handle mixed data with and without prefix`() {
        // Given: 混合数据（新旧数据混合）
        val messages = listOf(
            TimestampedMessage(
                content = "旧数据，无前缀",  // 旧数据
                timestamp = System.currentTimeMillis() - 7200000,
                sender = MessageSender.ME
            ),
            TimestampedMessage(
                content = "${IdentityPrefixHelper.PREFIX_CONTACT}新数据，有前缀",  // 新数据
                timestamp = System.currentTimeMillis(),
                sender = MessageSender.ME
            )
        )
        
        // When: 构建历史上下文
        val context = conversationContextBuilder.buildHistoryContext(messages)
        
        // Then: 两种数据都应该正确显示
        assertTrue(context.contains("旧数据，无前缀"))
        assertTrue(context.contains("${IdentityPrefixHelper.PREFIX_CONTACT}新数据，有前缀"))
    }

    // ========== 数据兼容性测试 ==========

    @Test
    fun `compatibility - LEGACY data should display correctly`() {
        // Given: 无前缀的旧数据
        val legacyData = "这是一条旧的对话记录"
        
        // When: 解析
        val parsed = IdentityPrefixHelper.parse(legacyData)
        
        // Then: 应该识别为 LEGACY
        assertEquals(IdentityPrefixHelper.IdentityRole.LEGACY, parsed.role)
        assertEquals("历史", parsed.role.displayName)
        assertEquals(legacyData, parsed.content)
    }

    @Test
    fun `compatibility - new data with CONTACT prefix should display correctly`() {
        // Given: 带 CONTACT 前缀的新数据
        val newData = "${IdentityPrefixHelper.PREFIX_CONTACT}你好"
        
        // When: 解析
        val parsed = IdentityPrefixHelper.parse(newData)
        
        // Then: 应该识别为 CONTACT
        assertEquals(IdentityPrefixHelper.IdentityRole.CONTACT, parsed.role)
        assertEquals("对方", parsed.role.displayName)
        assertEquals("你好", parsed.content)
    }

    @Test
    fun `compatibility - new data with USER prefix should display correctly`() {
        // Given: 带 USER 前缀的新数据
        val newData = "${IdentityPrefixHelper.PREFIX_USER}你好"
        
        // When: 解析
        val parsed = IdentityPrefixHelper.parse(newData)
        
        // Then: 应该识别为 USER
        assertEquals(IdentityPrefixHelper.IdentityRole.USER, parsed.role)
        assertEquals("我", parsed.role.displayName)
        assertEquals("你好", parsed.content)
    }

    // ========== 防双重前缀测试 ==========

    @Test
    fun `double prefix prevention - should not add duplicate prefix`() {
        // Given: 用户从截图 OCR 复制了带前缀的内容
        val ocrContent = "${IdentityPrefixHelper.PREFIX_CONTACT}真的吗？"
        
        // When: 再次添加前缀
        val result = IdentityPrefixHelper.addPrefix(ocrContent, ActionType.ANALYZE)
        
        // Then: 不应该有双重前缀
        assertEquals("${IdentityPrefixHelper.PREFIX_CONTACT}真的吗？", result)
    }

    @Test
    fun `double prefix prevention - should handle triple prefix`() {
        // Given: 极端情况，三重前缀
        val triplePrefix = "${IdentityPrefixHelper.PREFIX_CONTACT}${IdentityPrefixHelper.PREFIX_CONTACT}${IdentityPrefixHelper.PREFIX_CONTACT}内容"
        
        // When: 添加前缀
        val result = IdentityPrefixHelper.addPrefix(triplePrefix, ActionType.ANALYZE)
        
        // Then: 应该只有一个前缀
        assertEquals("${IdentityPrefixHelper.PREFIX_CONTACT}内容", result)
    }

    @Test
    fun `double prefix prevention - should replace wrong prefix`() {
        // Given: 用户复制了带错误前缀的内容
        val wrongPrefix = "${IdentityPrefixHelper.PREFIX_USER}对方说的话"
        
        // When: 使用 ANALYZE 添加前缀
        val result = IdentityPrefixHelper.addPrefix(wrongPrefix, ActionType.ANALYZE)
        
        // Then: 应该替换为正确的前缀
        assertEquals("${IdentityPrefixHelper.PREFIX_CONTACT}对方说的话", result)
    }

    // ========== UI 渲染测试 ==========

    @Test
    fun `UI rendering - three roles should have different display names`() {
        assertEquals("对方", IdentityPrefixHelper.IdentityRole.CONTACT.displayName)
        assertEquals("我", IdentityPrefixHelper.IdentityRole.USER.displayName)
        assertEquals("历史", IdentityPrefixHelper.IdentityRole.LEGACY.displayName)
    }

    @Test
    fun `UI rendering - parsed content should not contain prefix`() {
        // Given: 带前缀的内容
        val content = "${IdentityPrefixHelper.PREFIX_CONTACT}显示给用户的内容"
        
        // When: 解析
        val parsed = IdentityPrefixHelper.parse(content)
        
        // Then: 纯内容不应该包含前缀
        assertFalse(parsed.content.contains("【对方说】"))
        assertFalse(parsed.content.contains("【我正在回复】"))
        assertEquals("显示给用户的内容", parsed.content)
    }

    // ========== 完整业务流程测试 ==========

    @Test
    fun `full business flow - analyze then check conversation`() {
        // 模拟完整的对话流程
        
        // Step 1: 用户粘贴对方的消息，点击【帮我分析】
        val contactMessage = "你怎么才回消息？"
        val analyzedContent = IdentityPrefixHelper.addPrefix(contactMessage, ActionType.ANALYZE)
        assertEquals("${IdentityPrefixHelper.PREFIX_CONTACT}你怎么才回消息？", analyzedContent)
        
        // Step 2: 用户写好回复，点击【帮我检查】
        val myDraft = "刚才在开会，抱歉让你久等了"
        val checkedContent = IdentityPrefixHelper.addPrefix(myDraft, ActionType.CHECK)
        assertEquals("${IdentityPrefixHelper.PREFIX_USER}刚才在开会，抱歉让你久等了", checkedContent)
        
        // Step 3: 验证两条消息的角色不同
        val parsedContact = IdentityPrefixHelper.parse(analyzedContent)
        val parsedUser = IdentityPrefixHelper.parse(checkedContent)
        
        assertEquals(IdentityPrefixHelper.IdentityRole.CONTACT, parsedContact.role)
        assertEquals(IdentityPrefixHelper.IdentityRole.USER, parsedUser.role)
        
        // Step 4: 验证 UI 显示的内容不含前缀
        assertEquals(contactMessage, parsedContact.content)
        assertEquals(myDraft, parsedUser.content)
    }
}
