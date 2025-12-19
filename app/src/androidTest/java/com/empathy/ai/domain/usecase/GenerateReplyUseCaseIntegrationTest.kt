package com.empathy.ai.domain.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.usecase.GenerateReplyUseCase
import com.empathy.ai.domain.util.IdentityPrefixHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * GenerateReplyUseCase集成测试
 * 
 * 测试回复生成功能的端到端流程，特别验证数据保存的完整性
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class GenerateReplyUseCaseIntegrationTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var generateReplyUseCase: GenerateReplyUseCase
    
    @Inject
    lateinit var conversationRepository: ConversationRepository
    
    private val testContactId = "1"
    private val testMessage = "你好，最近怎么样？"
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun `端到端测试：回复模式数据应该保存到数据库`() = runTest {
        // Given - 确保数据库是干净的
        conversationRepository.deleteByContactId(testContactId)
        
        // When - 执行回复生成
        val result = generateReplyUseCase(testContactId, testMessage)
        
        // Then - 验证AI回复生成成功
        assertTrue(result.isSuccess, "生成回复应该成功")
        val replyResult = result.getOrThrow()
        assertNotNull(replyResult.suggestedReply, "回复建议不应为空")
        assertFalse(replyResult.suggestedReply.isBlank(), "回复建议不应为空白")
        
        // 验证数据库中保存了完整的对话记录
        val conversations = conversationRepository.getConversationsByContact(testContactId).getOrThrow()
        assertEquals(1, conversations.size, "应该保存一条对话记录")
        
        val conversation = conversations.first()
        
        // 验证用户输入
        val expectedUserInput = IdentityPrefixHelper.addPrefix(testMessage, ActionType.REPLY)
        assertEquals(expectedUserInput, conversation.userInput, "用户输入应该正确保存")
        
        // 验证AI回复
        assertNotNull(conversation.aiResponse, "AI回复应该不为空")
        assertTrue(conversation.aiResponse!!.contains("【回复建议】"), "AI回复应该包含标题")
        assertTrue(conversation.aiResponse!!.contains(replyResult.suggestedReply), "AI回复应该包含建议内容")
        
        // 验证时间戳
        assertTrue(conversation.timestamp > 0, "时间戳应该有效")
        
        // 验证未总结状态
        assertFalse(conversation.isSummarized, "对话记录应该未总结")
    }
    
    @Test
    fun `多次回复应该按时间倒序排列`() = runTest {
        // Given - 确保数据库是干净的
        conversationRepository.deleteByContactId(testContactId)
        
        // When - 执行多次回复生成
        val message1 = "第一条消息"
        val message2 = "第二条消息"
        val message3 = "第三条消息"
        
        generateReplyUseCase(testContactId, message1)
        Thread.sleep(100) // 确保时间戳不同
        generateReplyUseCase(testContactId, message2)
        Thread.sleep(100)
        generateReplyUseCase(testContactId, message3)
        
        // Then - 验证数据库中保存了多条对话记录
        val conversations = conversationRepository.getConversationsByContact(testContactId).getOrThrow()
        assertEquals(3, conversations.size, "应该保存三条对话记录")
        
        // 验证按时间倒序排列（最新的在前）
        assertTrue(conversations[0].timestamp > conversations[1].timestamp, "第一条应该是最新的")
        assertTrue(conversations[1].timestamp > conversations[2].timestamp, "第二条应该比第三条新")
        
        // 验证每条记录都有完整的用户输入和AI回复
        conversations.forEachIndexed { index, conversation ->
            val expectedMessage = when (index) {
                0 -> message3 // 最新的消息
                1 -> message2
                2 -> message1
                else -> ""
            }
            val expectedUserInput = IdentityPrefixHelper.addPrefix(expectedMessage, ActionType.REPLY)
            assertEquals(expectedUserInput, conversation.userInput, "第${index+1}条用户输入应该正确")
            assertNotNull(conversation.aiResponse, "第${index+1}条AI回复应该不为空")
            assertTrue(conversation.aiResponse!!.contains("【回复建议】"), "第${index+1}条AI回复应该包含标题")
        }
    }
    
    @Test
    fun `不同联系人的回复应该分别保存`() = runTest {
        // Given - 两个不同的联系人
        val contactId1 = "1"
        val contactId2 = "2"
        val message = "测试消息"
        
        // 确保数据库是干净的
        conversationRepository.deleteByContactId(contactId1)
        conversationRepository.deleteByContactId(contactId2)
        
        // When - 为两个联系人分别生成回复
        generateReplyUseCase(contactId1, message)
        generateReplyUseCase(contactId2, message)
        
        // Then - 验证每个联系人都有各自的对话记录
        val conversations1 = conversationRepository.getConversationsByContact(contactId1).getOrThrow()
        val conversations2 = conversationRepository.getConversationsByContact(contactId2).getOrThrow()
        
        assertEquals(1, conversations1.size, "联系人1应该有一条对话记录")
        assertEquals(1, conversations2.size, "联系人2应该有一条对话记录")
        
        // 验证contactId正确
        assertEquals(contactId1, conversations1.first().contactId)
        assertEquals(contactId2, conversations2.first().contactId)
        
        // 验证内容相同但独立保存
        assertEquals(conversations1.first().userInput, conversations2.first().userInput)
        assertNotNull(conversations1.first().aiResponse)
        assertNotNull(conversations2.first().aiResponse)
    }
    
    @Test
    fun `空消息应该也能保存`() = runTest {
        // Given
        val emptyMessage = ""
        val whitespaceMessage = "   "
        
        // 确保数据库是干净的
        conversationRepository.deleteByContactId(testContactId)
        
        // When
        val result1 = generateReplyUseCase(testContactId, emptyMessage)
        val result2 = generateReplyUseCase(testContactId, whitespaceMessage)
        
        // Then
        assertTrue(result1.isSuccess, "空消息应该也能生成回复")
        assertTrue(result2.isSuccess, "空白消息应该也能生成回复")
        
        val conversations = conversationRepository.getConversationsByContact(testContactId).getOrThrow()
        assertEquals(2, conversations.size, "应该保存两条对话记录")
        
        // 验证空消息也被正确保存
        val expectedEmptyInput = IdentityPrefixHelper.addPrefix(emptyMessage, ActionType.REPLY)
        val expectedWhitespaceInput = IdentityPrefixHelper.addPrefix(whitespaceMessage, ActionType.REPLY)
        assertEquals(expectedEmptyInput, conversations[1].userInput) // 旧的消息
        assertEquals(expectedWhitespaceInput, conversations[0].userInput) // 新的消息
        
        // 验证AI回复不为空
        assertNotNull(conversations[0].aiResponse)
        assertNotNull(conversations[1].aiResponse)
    }
    
    @Test
    fun `特殊字符消息应该正确保存`() = runTest {
        // Given
        val specialMessage = "包含特殊字符的消息：!@#$%^&*()_+-=[]{}|;':\",./<>?"
        
        // 确保数据库是干净的
        conversationRepository.deleteByContactId(testContactId)
        
        // When
        val result = generateReplyUseCase(testContactId, specialMessage)
        
        // Then
        assertTrue(result.isSuccess, "特殊字符消息应该能生成回复")
        
        val conversations = conversationRepository.getConversationsByContact(testContactId).getOrThrow()
        assertEquals(1, conversations.size, "应该保存一条对话记录")
        
        // 验证特殊字符被正确保存
        val expectedUserInput = IdentityPrefixHelper.addPrefix(specialMessage, ActionType.REPLY)
        assertEquals(expectedUserInput, conversations.first().userInput)
        assertNotNull(conversations.first().aiResponse)
    }
    
    @Test
    fun `长消息应该正确保存`() = runTest {
        // Given
        val longMessage = "这是一个很长的消息。".repeat(100) // 重复100次，约2000字符
        
        // 确保数据库是干净的
        conversationRepository.deleteByContactId(testContactId)
        
        // When
        val result = generateReplyUseCase(testContactId, longMessage)
        
        // Then
        assertTrue(result.isSuccess, "长消息应该能生成回复")
        
        val conversations = conversationRepository.getConversationsByContact(testContactId).getOrThrow()
        assertEquals(1, conversations.size, "应该保存一条对话记录")
        
        // 验证长消息被正确保存
        val expectedUserInput = IdentityPrefixHelper.addPrefix(longMessage, ActionType.REPLY)
        assertEquals(expectedUserInput, conversations.first().userInput)
        assertNotNull(conversations.first().aiResponse)
    }
}