package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.ContactSearchHistoryRepository
import javax.inject.Inject

/**
 * 清空联系人搜索历史
 */
class ClearContactSearchHistoryUseCase @Inject constructor(
    private val repository: ContactSearchHistoryRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.clearHistory()
    }
}
