package com.empathy.ai.presentation.ui.component.input

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 联系人搜索栏组件
 *
 * 功能：
 * 1. 搜索输入框（带防抖）
 * 2. 实时更新搜索查询
 * 3. 显示搜索结果计数
 * 4. 提供清除和关闭按钮
 *
 * @param searchQuery 当前搜索查询文本
 * @param resultCount 搜索结果数量
 * @param onQueryChange 查询文本变化回调
 * @param onSearchClose 关闭搜索回调
 * @param modifier 修饰符
 */
@Composable
fun ContactSearchBar(
    searchQuery: String,
    resultCount: Int,
    onQueryChange: (String) -> Unit,
    onSearchClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 关闭按钮
            IconButton(onClick = {
                keyboardController?.hide()
                onSearchClose()
            }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭搜索",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // 搜索输入框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("搜索联系人") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索图标",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "清除搜索",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                    }
                ),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            
            // 结果计数显示
            if (searchQuery.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$resultCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "空搜索", showBackground = true)
@Composable
private fun ContactSearchBarEmptyPreview() {
    EmpathyTheme {
        ContactSearchBar(
            searchQuery = "",
            resultCount = 0,
            onQueryChange = {},
            onSearchClose = {}
        )
    }
}

@Preview(name = "有搜索文本", showBackground = true)
@Composable
private fun ContactSearchBarWithQueryPreview() {
    EmpathyTheme {
        ContactSearchBar(
            searchQuery = "张三",
            resultCount = 5,
            onQueryChange = {},
            onSearchClose = {}
        )
    }
}

@Preview(name = "无结果", showBackground = true)
@Composable
private fun ContactSearchBarNoResultsPreview() {
    EmpathyTheme {
        ContactSearchBar(
            searchQuery = "不存在的联系人",
            resultCount = 0,
            onQueryChange = {},
            onSearchClose = {}
        )
    }
}

@Preview(name = "深色模式", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContactSearchBarDarkPreview() {
    EmpathyTheme {
        ContactSearchBar(
            searchQuery = "李四",
            resultCount = 3,
            onQueryChange = {},
            onSearchClose = {}
        )
    }
}
