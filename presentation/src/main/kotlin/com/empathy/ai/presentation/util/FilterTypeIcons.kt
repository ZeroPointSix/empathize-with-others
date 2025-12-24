package com.empathy.ai.presentation.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.empathy.ai.domain.model.FilterType

/**
 * FilterType的图标扩展
 *
 * 在Presentation层提供FilterType的图标映射，
 * 保持Domain层不依赖Compose。
 *
 * @see TDD-00017 Clean Architecture模块化改造技术设计
 */
val FilterType.icon: ImageVector
    get() = when (this) {
        FilterType.ALL -> Icons.Default.FilterList
        FilterType.AI_SUMMARY -> Icons.Default.AutoAwesome
        FilterType.CONFLICT -> Icons.Default.Warning
        FilterType.DATE -> Icons.Default.Restaurant
        FilterType.SWEET -> Icons.Default.Favorite
    }
