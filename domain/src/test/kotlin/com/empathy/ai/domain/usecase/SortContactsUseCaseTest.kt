package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.ContactSortOption
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SortContactsUseCaseTest {

    private lateinit var useCase: SortContactsUseCase

    @Before
    fun setup() {
        useCase = SortContactsUseCase()
    }

    @Test
    fun `按姓名排序应为升序`() {
        val contacts = listOf(
            ContactProfile(id = "1", name = "Bob", targetGoal = "A"),
            ContactProfile(id = "2", name = "Alice", targetGoal = "B"),
            ContactProfile(id = "3", name = "Charlie", targetGoal = "C")
        )

        val result = useCase(contacts, ContactSortOption.NAME)

        assertEquals(listOf("Alice", "Bob", "Charlie"), result.map { it.name })
    }

    @Test
    fun `按最近互动排序应为最新在前`() {
        val contacts = listOf(
            ContactProfile(id = "1", name = "Alice", targetGoal = "A", lastInteractionDate = "2026-01-10"),
            ContactProfile(id = "2", name = "Bob", targetGoal = "B", lastInteractionDate = "2026-01-12"),
            ContactProfile(id = "3", name = "Charlie", targetGoal = "C", lastInteractionDate = null)
        )

        val result = useCase(contacts, ContactSortOption.LAST_INTERACTION)

        assertEquals(listOf("Bob", "Alice", "Charlie"), result.map { it.name })
    }

    @Test
    fun `按关系分数排序应为高分在前`() {
        val contacts = listOf(
            ContactProfile(id = "1", name = "Alice", targetGoal = "A", relationshipScore = 20),
            ContactProfile(id = "2", name = "Bob", targetGoal = "B", relationshipScore = 80),
            ContactProfile(id = "3", name = "Charlie", targetGoal = "C", relationshipScore = 50)
        )

        val result = useCase(contacts, ContactSortOption.RELATIONSHIP_SCORE)

        assertEquals(listOf("Bob", "Charlie", "Alice"), result.map { it.name })
    }
}
