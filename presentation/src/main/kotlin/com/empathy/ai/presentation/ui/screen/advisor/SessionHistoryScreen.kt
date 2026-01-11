package com.empathy.ai.presentation.ui.screen.advisor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.theme.iOSTextTertiary
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
 * - 长按会话显示操作菜单（BUG-00060新增）
 * - 重命名会话（BUG-00060新增）
 * - 置顶/取消置顶会话（BUG-00060新增）
 *
 * ## 关联文档
 * - PRD-00029: AI军师UI架构优化需求
 * - TDD-00029: AI军师UI架构优化技术设计
 * - FD-00029: AI军师UI架构优化功能设计
 * - BUG-00060: 会话管理增强需求
 *
 * ## 页面布局
 * ```
 * ┌─────────────────────────────────────┐
 * │ [<]      会话历史           [新建]  │  ← iOS导航栏
 * ├─────────────────────────────────────┤
 * │ 与 张三 的对话                      │  ← 分组标题
 * ├─────────────────────────────────────┤
 * │ [📌💬] 关于工作安排的讨论   昨天 > │  ← 置顶会话
 * │        最后一条消息预览...          │
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
    val dimensions = AdaptiveDimensions.current

    // BUG-00060: 重命名对话框
    if (uiState.showRenameDialog && uiState.sessionToRename != null) {
        RenameSessionDialog(
            currentTitle = uiState.sessionToRename!!.title,
            onConfirm = { newTitle ->
                viewModel.renameSession(uiState.sessionToRename!!.id, newTitle)
                viewModel.hideRenameDialog()
            },
            onDismiss = { viewModel.hideRenameDialog() }
        )
    }

    Scaffold(
        containerColor = iOSBackground,
        topBar = {
            // iOS风格导航栏
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "会话历史",
                        fontSize = dimensions.fontSizeTitle,  // BUG-00055: 使用响应式字体
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
                            fontSize = dimensions.fontSizeTitle  // BUG-00055: 使用响应式字体
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
                fontSize = dimensions.fontSizeCaption,  // BUG-00055: 使用响应式字体
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
                        onSessionClick = onNavigateToChat,
                        onRenameSession = { session -> viewModel.showRenameDialog(session) },
                        onTogglePin = { session -> 
                            viewModel.togglePinSession(session.id, !session.isPinned) 
                        },
                        onDeleteSession = { session -> viewModel.deleteSession(session.id) }
                    )
                }
            }
        }
    }
}

/**
 * 会话列表
 *
 * BUG-00060: 支持长按操作菜单
 */
@Composable
private fun SessionList(
    sessions: List<AiAdvisorSession>,
    onSessionClick: (sessionId: String) -> Unit,
    onRenameSession: (AiAdvisorSession) -> Unit,
    onTogglePin: (AiAdvisorSession) -> Unit,
    onDeleteSession: (AiAdvisorSession) -> Unit
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
                onClick = { onSessionClick(session.id) },
                onRename = { onRenameSession(session) },
                onTogglePin = { onTogglePin(session) },
                onDelete = { onDeleteSession(session) }
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
 * - 置顶图标: 蓝色图钉（BUG-00060新增）
 *
 * BUG-00060: 支持长按显示操作菜单
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionListItem(
    session: AiAdvisorSession,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val dimensions = AdaptiveDimensions.current
    var showMenu by remember { mutableStateOf(false) }
    
    Column {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { showMenu = true }
                    )
                    .background(iOSCardBackground)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 会话图标（带置顶标识）
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

                // BUG-00060: 置顶图标
                if (session.isPinned) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "已置顶",
                        tint = iOSBlue,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(if (session.isPinned) 8.dp else 12.dp))

                // 信息区域
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = session.title,
                            fontSize = dimensions.fontSizeSubtitle,  // BUG-00055: 使用响应式字体
                            fontWeight = FontWeight.Medium,
                            color = iOSTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatRelativeTime(session.updatedAt),
                            fontSize = dimensions.fontSizeCaption,  // BUG-00055: 使用响应式字体
                            color = iOSTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 消息数量预览
                    Text(
                        text = if (session.messageCount > 0) "${session.messageCount}条消息" else "暂无消息",
                        fontSize = dimensions.fontSizeCaption,  // BUG-00055: 使用响应式字体
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
                    tint = iOSTextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // BUG-00060: 长按菜单
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("重命名") },
                    onClick = {
                        showMenu = false
                        onRename()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (session.isPinned) "取消置顶" else "置顶") },
                    onClick = {
                        showMenu = false
                        onTogglePin()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (session.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = null
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("删除", color = Color.Red) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = Color.Red
                        )
                    }
                )
            }
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
    val dimensions = AdaptiveDimensions.current
    
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
                fontSize = dimensions.fontSizeTitle,  // BUG-00055: 使用响应式字体
                color = iOSTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "点击下方按钮开始新对话",
                fontSize = dimensions.fontSizeSubtitle,  // BUG-00055: 使用响应式字体
                color = iOSTextSecondary.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onCreateNewSession) {
                Text(
                    text = "发起新对话",
                    color = iOSBlue,
                    fontSize = dimensions.fontSizeTitle,  // BUG-00055: 使用响应式字体
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

/**
 * BUG-00060: 重命名会话对话框
 *
 * @param currentTitle 当前会话标题
 * @param onConfirm 确认回调，传入新标题
 * @param onDismiss 取消回调
 */
@Composable
private fun RenameSessionDialog(
    currentTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newTitle by remember { mutableStateOf(currentTitle) }
    val dimensions = AdaptiveDimensions.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "重命名会话",
                fontSize = dimensions.fontSizeTitle,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("会话名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newTitle) },
                enabled = newTitle.isNotBlank()
            ) {
                Text("确定", color = iOSBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = iOSTextSecondary)
            }
        }
    )
}
