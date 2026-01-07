package com.empathy.ai.presentation.ui.screen.advisor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.viewmodel.SessionHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 会话历史页面
 *
 * 🔴 必须参考原型: 文档/开发文档/UI-原型/PRD29/ai-advisor-home-ios.html
 *
 * ## 业务职责
 * 显示当前联系人的所有历史会话，支持：
 * - 查看历史会话列表
 * - 点击会话进入对话界面
 * - 新建会话
 *
 * ## 关联文档
 * - PRD-00029: AI军师UI架构优化需求
 * - TDD-00029: AI军师UI架构优化技术设计
 * - FD-00029: AI军师UI架构优化功能设计
 *
 * ## 页面布局
 * ```
 * ┌─────────────────────────────────────┐
 * │ [<]      会话历史           [新建]  │  ← iOS导航栏
 * ├─────────────────────────────────────┤
 * │ 与 张三 的对话                      │  ← 分组标题
 * ├─────────────────────────────────────┤
 * │ [💬] 关于工作安排的讨论      昨天 > │  ← 会话列表项
 * │      最后一条消息预览...            │
 * ├─────────────────────────────────────┤
 * │ [💬] 周末计划              3天前 > │
 * │      最后一条消息预览...            │
 * └─────────────────────────────────────┘
 * ```
 *
 * @param contactId 联系人ID
 * @param onNavigateBack 返回按钮点击回调
 * @param onNavigateToChat 点击会话后的导航回调
 * @param onCreateNewSession 新建会话按钮点击回调
 * @param viewModel 注入的ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    contactId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (sessionId: String) -> Unit,
    onCreateNewSession: () -> Unit,
    viewModel: SessionHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = iOSBackground,
        topBar = {
            // iOS风格导航栏
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "会话历史",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iOSTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = iOSBlue
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onCreateNewSession) {
                        Text(
                            text = "新建",
                            color = iOSBlue,
                            fontSize = 17.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = iOSCardBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 分组标题
            Text(
                text = "与 ${uiState.contactName} 的对话",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                fontSize = 13.sp,
                color = iOSTextSecondary,
                fontWeight = FontWeight.Normal
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = iOSBlue)
                    }
                }
                uiState.isEmpty -> {
                    EmptySessionsView(onCreateNewSession = onCreateNewSession)
                }
                else -> {
                    SessionList(
                        sessions = uiState.sessions,
                        onSessionClick = onNavigateToChat
                    )
                }
            }
        }
    }
}

/**
 * 会话列表
 */
@Composable
private fun SessionList(
    sessions: List<AiAdvisorSession>,
    onSessionClick: (sessionId: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = sessions,
            key = { it.id }
        ) { session ->
            SessionListItem(
                session = session,
                onClick = { onSessionClick(session.id) }
            )
        }
    }
}

/**
 * 会话列表项
 *
 * 🔴 必须参考原型样式：
 * - 图标: 24dp, 蓝色聊天气泡
 * - 标题: 15sp, 黑色
 * - 时间: 13sp, 灰色
 * - 预览: 13sp, 灰色, 单行截断
 * - 右箭头: 灰色
 */
@Composable
private fun SessionListItem(
    session: AiAdvisorSession,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(iOSCardBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 会话图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iOSBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = iOSBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 信息区域
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = iOSTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatRelativeTime(session.updatedAt),
                        fontSize = 13.sp,
                        color = iOSTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 最后消息预览
                Text(
                    text = session.title,
                    fontSize = 13.sp,
                    color = iOSTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 右箭头
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFC7C7CC),
                modifier = Modifier.size(20.dp)
            )
        }

        // 分隔线
        HorizontalDivider(
            modifier = Modifier.padding(start = 68.dp),
            color = Color(0xFFE5E5EA),
            thickness = 0.5.dp
        )
    }
}

/**
 * 空状态视图
 */
@Composable
private fun EmptySessionsView(onCreateNewSession: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 空状态图标
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iOSBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = iOSBlue,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "暂无历史会话",
                fontSize = 17.sp,
                color = iOSTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "点击下方按钮开始新对话",
                fontSize = 15.sp,
                color = iOSTextSecondary.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onCreateNewSession) {
                Text(
                    text = "发起新对话",
                    color = iOSBlue,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 格式化相对时间
 *
 * 显示规则：
 * - 1分钟内: "刚刚"
 * - 1小时内: "X分钟前"
 * - 今天: "今天 HH:mm"
 * - 昨天: "昨天"
 * - 本周: "周X"
 * - 更早: "MM/dd"
 */
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "刚刚"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
        diff < 24 * 60 * 60 * 1000 -> {
            val calendar = Calendar.getInstance()
            val todayStart = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            if (timestamp >= todayStart) {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                "今天 ${sdf.format(Date(timestamp))}"
            } else {
                "昨天"
            }
        }
        diff < 2 * 24 * 60 * 60 * 1000 -> "昨天"
        diff < 7 * 24 * 60 * 60 * 1000 -> {
            val days = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
            val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
            days[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        }
        else -> {
            val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
