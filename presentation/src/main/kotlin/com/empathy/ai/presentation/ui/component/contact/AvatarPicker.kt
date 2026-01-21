package com.empathy.ai.presentation.ui.component.contact

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.AvatarColors
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.yalantis.ucrop.UCrop
import java.io.File


/**
 * 头像选择器组件
 *
 * 技术要点:
 * - 圆形头像区域（100dp）
 * - 无头像时显示姓名首字 + 随机背景色
 * - 有头像时显示图片 + 编辑图标覆盖层
 * - 支持相册/拍照选择与裁剪
 *
 * @param avatarUri 头像URI（null表示无头像）
 * @param displayName 联系人姓名（用于默认头像首字）
 * @param avatarColorSeed 默认头像颜色索引
 * @param onAvatarChange 头像变更回调
 * @param modifier 修饰符
 *
 * @see TDD-00020 7.2 AvatarPicker头像选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPicker(
    avatarUri: Uri?,
    displayName: String,
    avatarColorSeed: Int,
    onAvatarChange: (Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dimensions = AdaptiveDimensions.current
    val avatarSize = dimensions.avatarSizeLarge
    val (backgroundColor, textColor) = AvatarColors.getColorPairBySeed(avatarColorSeed)
    val displayChar = displayName.trim().take(1).ifBlank { "?" }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showPicker by remember { mutableStateOf(false) }
    // Keep the camera output uri to hand off to UCrop after capture.
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = result.data?.let { UCrop.getOutput(it) }
            if (resultUri != null) {
                val persistedUri = persistAvatarToFiles(context, resultUri)
                if (persistedUri != null) {
                    onAvatarChange(persistedUri)
                } else {
                    showToast(context, "头像保存失败，已使用临时文件")
                    onAvatarChange(resultUri)
                }
            } else {
                showToast(context, "图片处理失败")
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val error = result.data?.let { UCrop.getError(it) }
            showToast(context, error?.message ?: "图片处理失败")
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            startCrop(context, uri, cropLauncher)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { startCrop(context, it, cropLauncher) }
        } else {
            showToast(context, "拍照失败，请重试")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera(context, takePictureLauncher) { pendingCameraUri = it }
        } else {
            showToast(context, "相机权限被拒绝")
        }
    }

    Box(
        modifier = modifier
            .size(avatarSize)
            .clickable { showPicker = true },
        contentAlignment = Alignment.Center
    ) {
        if (avatarUri != null) {
            AsyncImage(
                model = avatarUri,
                contentDescription = "头像",
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .background(
                        color = Color.Black.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑头像",
                    tint = Color.White,
                    modifier = Modifier.size(dimensions.iconSizeLarge - 8.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .background(color = backgroundColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayChar,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "添加头像",
                tint = iOSBlue,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(dimensions.iconSizeMedium)
                    .background(
                        color = Color.White.copy(alpha = 0.85f),
                        shape = CircleShape
                    )
                    .padding(4.dp)
            )
        }
    }

    if (showPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                AvatarSourceItem(text = "拍照") {
                    showPicker = false
                    if (ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        launchCamera(context, takePictureLauncher) { pendingCameraUri = it }
                    } else {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                }
                AvatarSourceItem(text = "从相册选择") {
                    showPicker = false
                    pickMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                AvatarSourceItem(text = "取消") {
                    showPicker = false
                }
            }
        }
    }
}

@Composable
private fun AvatarSourceItem(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontSize = 16.sp,
        color = iOSTextSecondary,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    )
}

private fun launchCamera(
    context: Context,
    launcher: ActivityResultLauncher<Uri>,
    onUriReady: (Uri) -> Unit
) {
    val uri = createCameraImageUri(context)
    onUriReady(uri)
    launcher.launch(uri)
}

private fun startCrop(
    context: Context,
    sourceUri: Uri,
    launcher: ActivityResultLauncher<Intent>
) {
    // UCrop needs a writable destination Uri; use app cache to avoid storage permissions.
    val destinationUri = createCropImageUri(context)
    val options = UCrop.Options().apply {
        setCircleDimmedLayer(true)
        setFreeStyleCropEnabled(false)
        setHideBottomControls(true)
        setCompressionFormat(Bitmap.CompressFormat.JPEG)
        setCompressionQuality(90)
    }

    val intent = UCrop.of(sourceUri, destinationUri)
        .withAspectRatio(1f, 1f)
        .withMaxResultSize(1024, 1024)
        .withOptions(options)
        .getIntent(context)
    launcher.launch(intent)
}

private fun createCameraImageUri(context: Context): Uri {
    val file = createAvatarTempFile(context, "camera")
    val authority = "${context.packageName}.fileprovider"
    return FileProvider.getUriForFile(context, authority, file)
}

private fun createCropImageUri(context: Context): Uri {
    val file = createAvatarTempFile(context, "crop")
    return Uri.fromFile(file)
}

private fun createAvatarTempFile(context: Context, prefix: String): File {
    val dir = File(context.cacheDir, "avatars").apply { mkdirs() }
    return File(dir, "$prefix-${System.currentTimeMillis()}.jpg")
}

private fun persistAvatarToFiles(context: Context, sourceUri: Uri): Uri? {
    return try {
        val dir = File(context.filesDir, "avatars").apply { mkdirs() }
        val targetFile = File(dir, "avatar-${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null
        Uri.fromFile(targetFile)
    } catch (e: Exception) {
        null
    }
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

@Preview(name = "头像选择器-无头像", showBackground = true)
@Composable
private fun AvatarPickerEmptyPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AvatarPicker(
                avatarUri = null,
                displayName = "张三",
                avatarColorSeed = 0,
                onAvatarChange = {}
            )
        }
    }
}
