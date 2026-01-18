package com.empathy.ai.presentation.ui.screen.aiconfig

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.empathy.ai.domain.model.ProxyType
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSGreen
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.ui.component.ios.IOSFormField
import com.empathy.ai.presentation.ui.component.ios.IOSNavigationBar
import com.empathy.ai.presentation.ui.component.ios.IOSSegmentedControl
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsSection
import com.empathy.ai.presentation.ui.component.ios.IOSSwitch

/**
 * iOS风格代理设置对话框
 *
 * 全屏对话框，用于配置网络代理
 *
 * 设计规格（TD-00025）:
 * - 全屏iOS风格对话框
 * - IOSNavigationBar（取消、标题、保存）
 * - 代理启用开关
 * - 服务器地址和端口输入
 * - 用户名密码认证区域
 * - 代理测试功能
 *
 * @param uiState UI状态
 * @param onEvent 事件处理
 * @param onDismiss 关闭对话框
 *
 * @see TD-00025 Phase 5: 网络代理实现
 */
@Composable
fun ProxySettingsDialog(
    uiState: AiConfigUiState,
    onEvent: (AiConfigUiEvent) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        ProxySettingsContent(
            uiState = uiState,
            onEvent = onEvent,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun ProxySettingsContent(
    uiState: AiConfigUiState,
    onEvent: (AiConfigUiEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val dimensions = AdaptiveDimensions.current
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = iOSBackground
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 导航栏
            IOSNavigationBar(
                title = "网络代理",
                onCancel = onDismiss,
                onDone = { onEvent(AiConfigUiEvent.SaveProxyConfig) },
                isDoneEnabled = uiState.isProxyConfigValid
            )

            // 内容区域
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(dimensions.spacingMedium)
            ) {
                // 代理开关
                IOSSettingsSection(title = "代理设置") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensions.spacingMedium, vertical = dimensions.spacingMedium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "启用代理",
                                fontSize = dimensions.fontSizeTitle,
                                color = iOSTextPrimary
                            )
                            Text(
                                text = if (uiState.proxyEnabled) "已启用" else "已禁用",
                                fontSize = dimensions.fontSizeCaption,
                                color = iOSTextSecondary
                            )
                        }
                        IOSSwitch(
                            checked = uiState.proxyEnabled,
                            onCheckedChange = { onEvent(AiConfigUiEvent.UpdateProxyEnabled(it)) }
                        )
                    }
                }

                if (uiState.proxyEnabled) {
                    Spacer(modifier = Modifier.height(dimensions.spacingMedium))

                    // 代理类型
                    IOSSettingsSection(title = "代理类型") {
                        ProxyTypeSelector(
                            selectedType = uiState.proxyType,
                            onTypeSelected = { onEvent(AiConfigUiEvent.UpdateProxyType(it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacingMedium))

                    // 服务器配置
                    IOSSettingsSection(title = "服务器配置") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = iOSCardBackground,
                            shape = RoundedCornerShape(dimensions.cornerRadiusMedium)
                        ) {
                            Column {
                                IOSFormField(
                                    label = "地址",
                                    value = uiState.proxyHost,
                                    onValueChange = { onEvent(AiConfigUiEvent.UpdateProxyHost(it)) },
                                    placeholder = "proxy.example.invalid"
                                )
                                IOSFormField(
                                    label = "端口",
                                    value = if (uiState.proxyPort > 0) uiState.proxyPort.toString() else "",
                                    onValueChange = { 
                                        val port = it.toIntOrNull() ?: 0
                                        onEvent(AiConfigUiEvent.UpdateProxyPort(port))
                                    },
                                    placeholder = "8080",
                                    keyboardType = KeyboardType.Number,
                                    showDivider = false
                                )
                            }
                        }

                        // 错误提示
                        if (uiState.proxyHostError != null) {
                            Text(
                                text = uiState.proxyHostError,
                                fontSize = dimensions.fontSizeCaption,
                                color = Color(0xFFFF3B30),
                                modifier = Modifier.padding(
                                    start = dimensions.spacingMedium,
                                    top = dimensions.spacingSmall
                                )
                            )
                        }
                        if (uiState.proxyPortError != null) {
                            Text(
                                text = uiState.proxyPortError,
                                fontSize = dimensions.fontSizeCaption,
                                color = Color(0xFFFF3B30),
                                modifier = Modifier.padding(
                                    start = dimensions.spacingMedium,
                                    top = dimensions.spacingSmall
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacingMedium))

                    // 认证信息（可选）
                    IOSSettingsSection(title = "认证信息（可选）") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = iOSCardBackground,
                            shape = RoundedCornerShape(dimensions.cornerRadiusMedium)
                        ) {
                            Column {
                                IOSFormField(
                                    label = "用户名",
                                    value = uiState.proxyUsername,
                                    onValueChange = { onEvent(AiConfigUiEvent.UpdateProxyUsername(it)) },
                                    placeholder = "可选"
                                )
                                IOSFormField(
                                    label = "密码",
                                    value = uiState.proxyPassword,
                                    onValueChange = { onEvent(AiConfigUiEvent.UpdateProxyPassword(it)) },
                                    placeholder = "可选",
                                    isPassword = true,
                                    showDivider = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacingLarge))

                    // 测试连接按钮
                    ProxyTestButton(
                        isLoading = uiState.isTestingProxy,
                        testResult = uiState.proxyTestResult,
                        onTest = { onEvent(AiConfigUiEvent.TestProxyConnection) }
                    )
                }
            }
        }
    }
}

/**
 * 代理类型选择器
 */
@Composable
private fun ProxyTypeSelector(
    selectedType: ProxyType,
    onTypeSelected: (ProxyType) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    val types = listOf(ProxyType.HTTP, ProxyType.HTTPS, ProxyType.SOCKS4, ProxyType.SOCKS5)
    val selectedIndex = types.indexOf(selectedType)

    IOSSegmentedControl(
        tabs = types.map { it.getDisplayName() },
        selectedIndex = selectedIndex,
        onTabSelected = { index -> onTypeSelected(types[index]) },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.spacingMedium)
    )
}

/**
 * 代理测试按钮
 */
@Composable
private fun ProxyTestButton(
    isLoading: Boolean,
    testResult: ProxyTestResult?,
    onTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(
            onClick = onTest,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.iosButtonHeight),
            shape = RoundedCornerShape(dimensions.cornerRadiusSmall)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(dimensions.iconSizeMedium),
                    color = iOSBlue,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(dimensions.spacingSmall))
            }
            Text(
                text = if (isLoading) "测试中..." else "测试连接",
                fontSize = dimensions.fontSizeSubtitle
            )
        }

        // 测试结果
        testResult?.let { result ->
            Spacer(modifier = Modifier.height(dimensions.spacingMedium))
            
            when (result) {
                is ProxyTestResult.Success -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(iOSGreen, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                        Text(
                            text = "连接成功，延迟 ${result.latencyMs}ms",
                            fontSize = dimensions.fontSizeBody,
                            color = iOSGreen
                        )
                    }
                }
                is ProxyTestResult.Failure -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFFF3B30), RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                        Text(
                            text = result.message,
                            fontSize = dimensions.fontSizeBody,
                            color = Color(0xFFFF3B30)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "代理设置对话框 - 禁用", showBackground = true)
@Composable
private fun ProxySettingsDialogDisabledPreview() {
    EmpathyTheme {
        ProxySettingsContent(
            uiState = AiConfigUiState(
                proxyEnabled = false
            ),
            onEvent = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "代理设置对话框 - 启用", showBackground = true)
@Composable
private fun ProxySettingsDialogEnabledPreview() {
    EmpathyTheme {
        ProxySettingsContent(
            uiState = AiConfigUiState(
                proxyEnabled = true,
                proxyType = ProxyType.HTTP,
                proxyHost = "proxy.example.invalid",
                proxyPort = 8080
            ),
            onEvent = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "代理设置对话框 - 测试成功", showBackground = true)
@Composable
private fun ProxySettingsDialogSuccessPreview() {
    EmpathyTheme {
        ProxySettingsContent(
            uiState = AiConfigUiState(
                proxyEnabled = true,
                proxyType = ProxyType.SOCKS5,
                proxyHost = "192.0.2.1",
                proxyPort = 1080,
                proxyTestResult = ProxyTestResult.Success(latencyMs = 45)
            ),
            onEvent = {},
            onDismiss = {}
        )
    }
}
