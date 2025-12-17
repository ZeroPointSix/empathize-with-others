package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.repository.ContactRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * GetAllContactsUseCase 单元测试
 *
 * 测试覆盖：
 * 1. 成功获取联系人列表
 * 2. 空列表的情况
 * 3. Flow响应式数据流
 */
class GetAllContactsUseCaseTest {

    private lateinit var contactRepository: ContactRepository
    private lateinit var useCase: GetAllContactsUseCase

    @Before
    fun setup() {
        contactRepository = mockk()
        useCase = GetAllContactsUseCase(contactRepository)
    }

    @Test
    fun `should return contact list flow when repository provides data`() = runTest {
        // Given
        val expectedContacts = listOf(
            ContactProfile(
                id = "contact_1",
                name = "联系人1",
                targetGoal = "目标1",
                contextDepth = 10,
                facts = mapOf("age" to "25")
            ),
            ContactProfile(
                id = "contact_2",
                name = "联系人2",
                targetGoal = "目标2",
                contextDepth = 8,
                facts = mapOf("age" to "30")
            )
        )

        every {
            contactRepository.getAllProfiles()
        } returns flowOf(expectedContacts)

        // When
        val result = useCase().toList()

        // Then
        assertEquals(1, result.size) // Flow只发射一次
        assertEquals(expectedContacts, result.first())
        assertEquals(2, result.first().size)
    }

    @Test
    fun `should return empty list flow when repository provides empty data`() = runTest {
        // Given
        val emptyContacts = emptyList<ContactProfile>()

        every {
            contactRepository.getAllProfiles()
        } returns flowOf(emptyContacts)

        // When
        val result = useCase().toList()

        // Then
        assertEquals(1, result.size)
        assertTrue(result.first().isEmpty())
    }

    @Test
    fun `should return contact list with single contact`() = runTest {
        // Given
        val singleContact = ContactProfile(
            id = "contact_1",
            name = "单人联系人",
            targetGoal = "测试目标",
            contextDepth = 5,
            facts = emptyMap()
        )

        every {
            contactRepository.getAllProfiles()
        } returns flowOf(listOf(singleContact))

        // When
        val result = useCase().toList()

        // Then
        assertEquals(1, result.size)
        assertEquals(1, result.first().size)
        assertEquals(singleContact, result.first().first())
    }
}