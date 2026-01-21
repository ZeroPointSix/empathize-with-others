package com.empathy.ai.presentation.ui.component.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.presentation.theme.AvatarColors
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * iOS风格身份名片卡
 *
 * 设计原则：
 * 1. 白色大圆角卡片容器
 * 2. 头像居中，带白色描边和双层投影
 * 3. 姓名使用SF Pro Display Bold风格，字号加大
 * 4. 相识天数设计为精致的灰色小胶囊标签
 * 5. 编辑入口改为卡片右上角的灰色"编辑"文字按钮
 *
 * @param contact 联系人信息
 * @param daysSinceFirstMet 相识天数
 * @param onEditClick 编辑按钮点击回调
 * @param modifier 修饰符
 */
@Composable
fun IdentityCard(
    contact: ContactProfile,
    daysSinceFirstMet: Int,
    onEditClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // 右上角编辑按钮
            if (onEditClick != null) {
                TextButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "编辑",
                        fontSize = 15.sp,
                        color = iOSTextSecondary
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 头像 - 带白色描边和双层投影
                AvatarWithShadow(
                    avatarUrl = contact.avatarUrl,
                    name = contact.name,
                    avatarColorSeed = contact.avatarColorSeed
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 姓名 - SF Pro Display Bold风格
                Text(
                    text = contact.name,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = iOSTextPrimary,
                    letterSpacing = (-0.5).sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 相识天数 - 精致的灰色小胶囊标签
                DaysKnownChip(days = daysSinceFirstMet)

                Spacer(modifier = Modifier.height(12.dp))

                // 联系方式
                val contactInfoText = contact.contactInfo?.takeIf { it.isNotBlank() } ?: "占位文本"
                Text(
                    text = "联系方式",
                    fontSize = 12.sp,
                    color = iOSTextSecondary
                )
                Text(
                    text = contactInfoText,
                    fontSize = 14.sp,
                    color = iOSTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 带投影的头像组件
 */
@Composable
private fun AvatarWithShadow(
    avatarUrl: String?,
    name: String,
    avatarColorSeed: Int,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = AvatarColors.getColorPairBySeed(avatarColorSeed)
    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.15f)
            )
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .border(3.dp, Color.White, CircleShape)
                .padding(3.dp)
        ) {
            if (!avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "头像",
                    modifier = Modifier
                        .size(94.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 默认头像
                Box(
                    modifier = Modifier
                        .size(94.dp)
                        .background(
                            color = backgroundColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }
    }
}

/**
 * 相识天数胶囊标签
 */
@Composable
private fun DaysKnownChip(
    days: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF2F2F7)
    ) {
        Text(
            text = "已相识 $days 天",
            fontSize = 12.sp,
            color = iOSTextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

// ==================== Previews ====================

@Preview(name = "身份名片卡", showBackground = true)
@Composable
private fun IdentityCardPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            IdentityCard(
                contact = ContactProfile(
                    id = "1",
                    name = "小明",
                    targetGoal = "建立良好关系",
                    avatarUrl = "",
                    relationshipScore = 85
                ),
                daysSinceFirstMet = 105,
                onEditClick = {}
            )
        }
    }
}

@Preview(name = "身份名片卡-无编辑", showBackground = true)
@Composable
private fun IdentityCardNoEditPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            IdentityCard(
                contact = ContactProfile(
                    id = "2",
                    name = "新朋友",
                    targetGoal = "认识新朋友",
                    avatarUrl = "",
                    relationshipScore = 50
                ),
                daysSinceFirstMet = 1
            )
        }
    }
}
