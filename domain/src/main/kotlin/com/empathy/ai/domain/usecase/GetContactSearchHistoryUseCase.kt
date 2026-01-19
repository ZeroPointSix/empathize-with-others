package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.ContactSearchHistoryRepository
import javax.inject.Inject

/**
 * 获取联系人搜索历史
 */
class GetContactSearchHistoryUseCase @Inject constructor(
    private val repository: ContactSearchHistoryRepository
) {
    suspend operator fun invoke(): Result<List<String>> {
        return repository.getHistory()
    }
}
