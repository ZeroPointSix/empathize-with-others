package com.empathy.ai.presentation.ui.screen.advisor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.theme.iOSSeparator
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.viewmodel.AiAdvisorUiState
import com.empathy.ai.presentation.viewmodel.AiAdvisorViewModel

/**
 * AI军师主界面（iOS风格）
 *
 * ## 业务职责
 * 作为AI军师功能的入口页面，展示联系人列表与每个联系人的最近会话。
 * 用户点击联系人后进入AI军师对话界面进行深度分析。
 *
 * ## 关联文档
 * - PRD-00026: AI军师对话功能需求
 * - TDD-00026: AI军师对话功能技术设计
 * - FD-00026: AI军师对话功能设计
 *
 * ## 页面布局
 * ```
 * ┌─────────────────────────────────────┐
 * │  [图标] AI军师                      │  ← iOS大标题风格
 * │  选择联系人，开始与AI军师对话        │
 * ├─────────────────────────────────────┤
 * │                                     │
 * │  ┌───────────────────────────────┐ │
 * │  │  联系人A  │ 消息数(3)      >  │ │  ← 卡片式列表
 *  │  ├───────────────────────────────┤ │
 *  │  │  联系人B  │ 点击开始对话    >  │ │
 *  │  └───────────────────────────────┘ │
 * │                                     │
 * └─────────────────────────────────────┘
 * ```
 *
 * ## 设计决策
 * - 采用iOS设计风格，与应用整体保持一致
 * - 使用卡片式联系人列表，区分度更高
 * - 最近会话标题+消息计数，直观展示对话状态
 * - 空状态友好引导，提升首次使用体验
 *
 * @param onNavigateToChat 点击联系人后的导航回调，参数为联系人ID
 * @param viewModel 注入的ViewModel，通过hiltViewModel()自动获取
 * @see AiAdvisorViewModel 管理页面状态
 * @see AiAdvisorChatScreen 对话详情页面
 */
@Composable
fun AiAdvisorScreen(
    onNavigateToChat: (String) -> Unit,
    viewModel: AiAdvisorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBackground)
    ) {
        // iOS大标题
        IOSLargeTitle(title = "AI军师")

        // 内容区域
        AiAdvisorContent(
            uiState = uiState,
            onContactClick = onNavigateToChat
        )
    }
}

@Composable
private fun IOSLargeTitle(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(iOSCardBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Psychology,
                contentDescription = null,
                tint = iOSPurple,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = iOSTextPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "选择联系人，开始与AI军师对话",
            fontSize = 15.sp,
            color = iOSTextSecondary
        )
    }
}

@Composable
private fun AiAdvisorContent(
    uiState: AiAdvisorUiState,
    onContactClick: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = iOSBlue
                )
            }
            uiState.error != null -> {
                ErrorState(
                    message = uiState.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.contacts.isEmpty() -> {
                EmptyState(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                ContactListWithSessions(
                    contacts = uiState.contacts,
                    recentSessions = uiState.recentSessions,
                    onContactClick = onContactClick
                )
            }
        }
    }
}

@Composable
private fun ContactListWithSessions(
    contacts: List<ContactProfile>,
    recentSessions: Map<String, AiAdvisorSession?>,
    onContactClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Text(
                text = "联系人",
                fontSize = 13.sp,
                color = iOSTextSecondary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 8.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    contacts.forEachIndexed { index, contact ->
                        ContactItem(
                            contact = contact,
                            recentSession = recentSessions[contact.id],
                            onClick = { onContactClick(contact.id) }
                        )
                        if (index < contacts.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 72.dp)
                                    .height(0.5.dp)
                                    .background(iOSSeparator)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactItem(
    contact: ContactProfile,
    recentSession: AiAdvisorSession?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
            color = iOSPurple.copy(alpha = 0.15f)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                tint = iOSPurple
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Contact info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = iOSTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = recentSession?.title ?: "点击开始对话",
                fontSize = 14.sp,
                color = iOSTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Message count badge
        if (recentSession != null && recentSession.messageCount > 0) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iOSBlue
            ) {
                Text(
                    text = "${recentSession.messageCount}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Arrow
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = iOSTextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = iOSPurple.copy(alpha = 0.1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.Psychology,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                tint = iOSPurple.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "暂无联系人",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = iOSTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "请先添加联系人，然后与AI军师对话",
            fontSize = 15.sp,
            color = iOSTextSecondary
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "加载失败",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFF3B30)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 15.sp,
            color = iOSTextSecondary
        )
    }
}
