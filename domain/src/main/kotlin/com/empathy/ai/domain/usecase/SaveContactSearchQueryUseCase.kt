package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.ContactSearchHistoryRepository
import javax.inject.Inject

/**
 * 保存联系人搜索关键词
 */
class SaveContactSearchQueryUseCase @Inject constructor(
    private val repository: ContactSearchHistoryRepository
) {
    suspend operator fun invoke(query: String): Result<List<String>> {
        return repository.saveQuery(query)
    }
}
