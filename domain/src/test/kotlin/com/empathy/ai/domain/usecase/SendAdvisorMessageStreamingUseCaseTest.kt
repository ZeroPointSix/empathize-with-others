package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.AiAdvisorMessageBlock
import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.AiStreamChunk
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.MessageBlockStatus
import com.empathy.ai.domain.model.MessageBlockType
import com.empathy.ai.domain.model.MessageType
import com.empathy.ai.domain.model.SendStatus
import com.empathy.ai.domain.model.StreamingState
import com.empathy.ai.domain.model.TokenUsage
import com.empathy.ai.domain.repository.AiAdvisorRepository
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.util.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * SendAdvisorMessageStreamingUseCase流式发送用例测试
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 * @see FD-00030 AI军师Markdown渲染与会话隔离功能设计
 */
class SendAdvisorMessageStreamingUseCaseTest {

    private lateinit var useCase: SendAdvisorMessageStreamingUseCase
    private lateinit var aiAdvisorRepository: AiAdvisorRepository
    private lateinit var aiRepository: AiRepository
    private lateinit var contactRepository: ContactRepository
    private lateinit var aiProviderRepository: AiProviderRepository
    private lateinit var brainTagRepository: BrainTagRepository  // FD-00030: 新增标签仓库
    private lateinit var logger: Logger  // CR-001: 新增日志记录器

    private val testContactId = "contact-123"
    private val testSessionId = "session-456"
    private val testMessage = "Hello, AI!"

    private val testProvider = AiProvider(
        id = "provider-1",
        name = "Test Provider",
        baseUrl = "https://api.test.com",
        apiKey = "test-key",
        models = listOf(AiModel(id = "test-model", displayName = "Test Model")),
        defaultModelId = "test-model",
        isDefault = true
    )

    @Before
    fun setup() {
        aiAdvisorRepository = mockk(relaxed = true)
        aiRepository = mockk(relaxed = true)
        contactRepository = mockk(relaxed = true)
        aiProviderRepository = mockk(relaxed = true)
        brainTagRepository = mockk(relaxed = true)  // FD-00030: 初始化标签仓库Mock
        logger = mockk(relaxed = true)  // CR-001: 初始化日志记录器Mock

        useCase = SendAdvisorMessageStreamingUseCase(
            aiAdvisorRepository,
            aiRepository,
            contactRepository,
            aiProviderRepository,
            brainTagRepository,  // FD-00030: 新增依赖
            logger  // CR-001: 新增依赖
        )

        // 默认mock配置
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { contactRepository.getProfile(any()) } returns Result.success(null)
        coEvery { brainTagRepository.getTagsForContact(any()) } returns flowOf(emptyList())  // FD-00030: 标签Mock
        coEvery { aiAdvisorRepository.getConversationsBySessionWithLimit(any(), any()) } returns Result.success(emptyList())  // FD-00030: 会话隔离
        coEvery { aiAdvisorRepository.saveMessage(any()) } returns Result.success(Unit)
        coEvery { aiAdvisorRepository.saveBlock(any()) } returns Result.success(Unit)
        coEvery { aiAdvisorRepository.updateBlockContent(any(), any(), any()) } returns Result.success(Unit)
        coEvery { aiAdvisorRepository.updateMessageStatus(any(), any()) } returns Result.success(Unit)
    }

    @Test
    fun `流式响应正常完成时应发射Started和Completed状态`() = runTest {
        // Given
        coEvery { aiRepository.generateTextStream(any(), any(), any()) } returns flowOf(
            AiStreamChunk.Started,
            AiStreamChunk.TextDelta("Hello"),
            AiStreamChunk.TextDelta(" World"),
            AiStreamChunk.Complete("Hello World", null)
        )

        // When
        val result = useCase(testContactId, testSessionId, testMessage).toList()

        // Then
        assertTrue(result.first() is StreamingState.Started)
        assertTrue(result.any { it is StreamingState.TextUpdate })
        assertTrue(result.last() is StreamingState.Completed)
    }

    @Test
    fun `流式响应应正确累积文本内容`() = runTest {
        // Given
        coEvery { aiRepository.generateTextStream(any(), any(), any()) } returns flowOf(
            AiStreamChunk.Started,
            AiStreamChunk.TextDelta("Hello"),
            AiStreamChunk.TextDelta(" "),
            AiStreamChunk.TextDelta("World"),
            AiStreamChunk.Complete("Hello World", null)
        )

        // When
        val result = useCase(testContactId, testSessionId, testMessage).toList()

        // Then
        val textUpdates = result.filterIsInstance<StreamingState.TextUpdate>()
        assertEquals("Hello", textUpdates[0].content)
        assertEquals("Hello ", textUpdates[1].content)
        assertEquals("Hello World", textUpdates[2].content)
    }

    @Test
    fun `思考过程应创建THINKING类型Block`() = runTest {
        // Given
        coEvery { aiRepository.generateTextStream(any(), any(), any()) } returns flowOf(
            AiStreamChunk.Started,
            AiStreamChunk.ThinkingDelta("Let me think..."),
            AiStreamChunk.ThinkingComplete("Let me think...", 1000L),
            AiStreamChunk.TextDelta("Answer"),
            AiStreamChunk.Complete("Answer", null)
        )

        // When
        useCase(testContactId, testSessionId, testMessage).toList()

        // Then - FD-00030: saveBlock被调用多次（THINKING block + MAIN_TEXT block）
        coVerify(atLeast = 2) { aiAdvisorRepository.saveBlock(any()) }
        // 验证至少有一个THINKING类型的block
        coVerify { aiAdvisorRepository.saveBlock(match { it.type == MessageBlockType.THINKING }) }
    }

    @Test
    fun `思考过程应发射ThinkingUpdate状态`() = runTest {
        // Given
        coEvery { aiRepository.generateTextStream(any(), any(), any()) } returns flowOf(
            AiStreamChunk.Started,
            AiStreamChunk.ThinkingDelta("Thinking..."),
            AiStreamChunk.ThinkingComplete("Thinking...", 1000L),
            AiStreamChunk.TextDelta("Result"),
            AiStreamChunk.Complete("Result", null)
        )

        // When
        val result = useCase(testContactId, testSessionId, testMessage).toList()

        // Then
        val thinkingUpdates = result.filterIsInstance<StreamingState.ThinkingUpdate>()
        assertTrue(thinkingUpdates.isNotEmpty())
        assertEquals("Thinking...", thinkingUpdates.first().content)
    }

    @Test
    fun `SSE连接失败时应发射Error状态`() = runTest {
        // Given
        val error = IOException("Connection failed")
        coEvery { aiRepository.generateTextStream(any(), any(), any()) } returns flowOf(
            AiStreamChunk.Started,
            AiStreamChunk.Error(error)
        )

        // When
        val result = useCase(testContactId, testSessionId, testMessage).toList()

        // Then
        assertTrue(result.last() is StreamingState.Error)
        assertEquals(error, (result.last() as StreamingState.Error).error)
    }

    @Test
    fun `SSE连接失败时应更新消息状态为FAILED`() = runTest {
        // Given
        coEvery { aiRepository.generateTextStream(any(), any(), any()) } returns flowOf(
            AiStreamChunk.Started,
            AiStreamChunk.Error(IOException("Error"))
        )

        // When
        useCase(testContactId, testSessionId, testMessage).toList()

        // Then - FD-00030: API变更为updateMessageContentAndStatus
        coVerify { aiAdvisorRepository.updateMessageContentAndStatus(any(), eq("[AI响应失败: Error]"), eq(SendStatus.FAILED)) }
    }

    @Test
    fun `完成时应更新消息状态为SUCCESS`() = runTest {
        // Given
        coEvery { aiRepository.generateTextStream(any(), any(), any()) } returns flowOf(
            AiStreamChunk.Started,
            AiStreamChunk.TextDelta("Response"),
            AiStreamChunk.Complete("Response", null)
        )

        // When
        useCase(testContactId, testSessionId, testMessage).toList()

        // Then - FD-00030: API变更为updateMessageContentAndStatus
        coVerify { aiAdvisorRepository.updateMessageContentAndStatus(any(), eq("Response"), eq(SendStatus.SUCCESS)) }
    }

    @Test
    fun `完成时应更新Block状态为SUCCESS`() = runTest {
        // Given
        coEvery { aiRepository.generateTextStream(any(), any(), any()) } returns flowOf(
            AiStreamChunk.Started,
            AiStreamChunk.TextDelta("Response"),
            AiStreamChunk.Complete("Response", null)
        )

        // When
        useCase(testContactId, testSessionId, testMessage).toList()

        // Then
        coVerify { aiAdvisorRepository.updateBlockContent(any(), any(), MessageBlockStatus.SUCCESS) }
    }

    @Test
    fun `应保存用户消息`() = runTest {
        // Given
        coEvery { aiRepository.generateTextStream(any(), any(), any()) } returns flowOf(
            AiStreamChunk.Started,
            AiStreamChunk.Complete("Response", null)
        )

        // When
        useCase(testContactId, testSessionId, testMessage).toList()

        // Then - FD-00030: saveMessage被调用2次（用户消息+AI初始消息）
        coVerify(atLeast = 2) { aiAdvisorRepository.saveMessage(any()) }
        // 验证至少有一次是用户消息
        coVerify { aiAdvisorRepository.saveMessage(match { it.messageType == MessageType.USER }) }
    }

    @Test
    fun `未配置默认Provider时应发射Error状态`() = runTest {
        // Given
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(null)

        // When
        val result = useCase(testContactId, testSessionId, testMessage).toList()

        // Then
        assertTrue(result.last() is StreamingState.Error)
    }

    @Test
    fun `获取Provider失败时应发射Error状态`() = runTest {
        // Given
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.failure(Exception("Provider error"))

        // When
        val result = useCase(testContactId, testSessionId, testMessage).toList()

        // Then
        assertTrue(result.last() is StreamingState.Error)
    }

    @Test
    fun `Complete应包含Token使用统计`() = runTest {
        // Given
        val usage = TokenUsage(100, 50, 150)
        coEvery { aiRepository.generateTextStream(any(), any(), any()) } returns flowOf(
            AiStreamChunk.Started,
            AiStreamChunk.TextDelta("Response"),
            AiStreamChunk.Complete("Response", usage)
        )

        // When
        val result = useCase(testContactId, testSessionId, testMessage).toList()

        // Then
        val completed = result.last() as StreamingState.Completed
        assertEquals(usage, completed.usage)
    }
}
