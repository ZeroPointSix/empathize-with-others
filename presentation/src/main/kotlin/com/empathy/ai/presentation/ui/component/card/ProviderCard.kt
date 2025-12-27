package com.empathy.ai.presentation.ui.component.card

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * AI 服务商卡片组件
 *
 * 用于展示 AI 服务商的详细信息
 *
 * @param provider 服务商信息
 * @param onEdit 编辑回调
 * @param onDelete 删除回调
 * @param onSetDefault 设置为默认回调
 * @param modifier 修饰符
 */
@Composable
fun ProviderCard(
    provider: AiProvider,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensions.cardElevation),
        colors = CardDefaults.cardColors(
            containerColor = if (provider.isDefault) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.cardPadding)
        ) {
            // 头部：名称和操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 服务商名称
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                    ) {
                        Text(
                            text = provider.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (provider.isDefault) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        
                        // 默认标签
                        if (provider.isDefault) {
                            AssistChip(
                                onClick = { },
                                label = { Text("默认") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "默认",
                                        modifier = Modifier.size(dimensions.iconSizeSmall)
                                    )
                                }
                            )
                        }
                    }
                }
                
                // 操作按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimensions.spacingXSmall)
                ) {
                    // 设置为默认按钮
                    if (!provider.isDefault) {
                        IconButton(onClick = onSetDefault) {
                            Icon(
                                imageVector = Icons.Outlined.StarBorder,
                                contentDescription = "设为默认",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // 编辑按钮
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = if (provider.isDefault) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    
                    // 删除按钮
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            
            // API 端点
            Text(
                text = "API 端点",
                style = MaterialTheme.typography.labelSmall,
                color = if (provider.isDefault) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            Spacer(modifier = Modifier.height(dimensions.spacingXSmall))
            Text(
                text = provider.baseUrl,
                style = MaterialTheme.typography.bodySmall,
                color = if (provider.isDefault) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            
            // 模型信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "模型数量",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (provider.isDefault) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                    Spacer(modifier = Modifier.height(dimensions.spacingXSmall))
                    Text(
                        text = "${provider.models.size} 个模型",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (provider.isDefault) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "默认模型",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (provider.isDefault) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                    Spacer(modifier = Modifier.height(dimensions.spacingXSmall))
                    Text(
                        text = provider.getDefaultModel()?.displayName 
                            ?: provider.defaultModelId,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (provider.isDefault) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "默认服务商", showBackground = true)
@Composable
private fun ProviderCardDefaultPreview() {
    EmpathyTheme {
        ProviderCard(
            provider = AiProvider(
                id = "1",
                name = "OpenAI",
                baseUrl = "https://api.openai.com/v1",
                apiKey = "sk-test123",
                models = listOf(
                    AiModel(id = "gpt-4", displayName = "GPT-4"),
                    AiModel(id = "gpt-3.5-turbo", displayName = "GPT-3.5 Turbo")
                ),
                defaultModelId = "gpt-4",
                isDefault = true
            ),
            onEdit = {},
            onDelete = {},
            onSetDefault = {}
        )
    }
}

@Preview(name = "普通服务商", showBackground = true)
@Composable
private fun ProviderCardNormalPreview() {
    EmpathyTheme {
        ProviderCard(
            provider = AiProvider(
                id = "2",
                name = "DeepSeek",
                baseUrl = "https://api.deepseek.com/v1",
                apiKey = "sk-test456",
                models = listOf(
                    AiModel(id = "deepseek-chat", displayName = "DeepSeek Chat"),
                    AiModel(id = "deepseek-coder", displayName = "DeepSeek Coder")
                ),
                defaultModelId = "deepseek-chat",
                isDefault = false
            ),
            onEdit = {},
            onDelete = {},
            onSetDefault = {}
        )
    }
}

@Preview(name = "单个模型", showBackground = true)
@Composable
private fun ProviderCardSingleModelPreview() {
    EmpathyTheme {
        ProviderCard(
            provider = AiProvider(
                id = "3",
                name = "自定义服务",
                baseUrl = "https://custom-api.example.com/v1",
                apiKey = "sk-custom789",
                models = listOf(
                    AiModel(id = "custom-model")
                ),
                defaultModelId = "custom-model",
                isDefault = false
            ),
            onEdit = {},
            onDelete = {},
            onSetDefault = {}
        )
    }
}

@Preview(name = "长 URL", showBackground = true)
@Composable
private fun ProviderCardLongUrlPreview() {
    EmpathyTheme {
        ProviderCard(
            provider = AiProvider(
                id = "4",
                name = "Google",
                baseUrl = "https://generativelanguage.googleapis.com/v1beta/models",
                apiKey = "sk-google123",
                models = listOf(
                    AiModel(id = "gemini-pro", displayName = "Gemini Pro")
                ),
                defaultModelId = "gemini-pro",
                isDefault = false
            ),
            onEdit = {},
            onDelete = {},
            onSetDefault = {}
        )
    }
}

@Preview(name = "深色模式", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProviderCardDarkPreview() {
    EmpathyTheme {
        ProviderCard(
            provider = AiProvider(
                id = "5",
                name = "OpenAI",
                baseUrl = "https://api.openai.com/v1",
                apiKey = "sk-test123",
                models = listOf(
                    AiModel(id = "gpt-4", displayName = "GPT-4")
                ),
                defaultModelId = "gpt-4",
                isDefault = true
            ),
            onEdit = {},
            onDelete = {},
            onSetDefault = {}
        )
    }
}
