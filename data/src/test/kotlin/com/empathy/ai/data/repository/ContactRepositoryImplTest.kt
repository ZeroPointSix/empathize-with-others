package com.empathy.ai.data.repository

import com.empathy.ai.data.local.dao.ContactDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * ContactRepositoryImpl单元测试
 *
 * 覆盖联系方式与头像更新逻辑。
 */
class ContactRepositoryImplTest {

    private lateinit var contactDao: ContactDao
    private lateinit var contactRepository: ContactRepositoryImpl

    @Before
    fun setup() {
        contactDao = mockk()
        contactRepository = ContactRepositoryImpl(contactDao)
    }

    @Test
    fun `更新联系方式_返回受影响行数`() = runTest {
        // Given
        val contactId = "contact-1"
        val contactInfo = "123456"
        coEvery { contactDao.updateContactInfo(contactId, contactInfo) } returns 1

        // When
        val rows = contactRepository.updateContactInfo(contactId, contactInfo)

        // Then
        assertEquals(1, rows)
        coVerify { contactDao.updateContactInfo(contactId, contactInfo) }
    }

    @Test
    fun `更新头像_返回受影响行数`() = runTest {
        // Given
        val contactId = "contact-2"
        val avatarUrl = "file:///tmp/avatar.jpg"
        val avatarColorSeed = 3
        coEvery { contactDao.updateAvatar(contactId, avatarUrl, avatarColorSeed) } returns 1

        // When
        val rows = contactRepository.updateAvatar(contactId, avatarUrl, avatarColorSeed)

        // Then
        assertEquals(1, rows)
        coVerify { contactDao.updateAvatar(contactId, avatarUrl, avatarColorSeed) }
    }
}
