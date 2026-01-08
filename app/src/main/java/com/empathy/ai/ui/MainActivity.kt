package com.empathy.ai.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.empathy.ai.presentation.navigation.NavGraph
import com.empathy.ai.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 应用主Activity
 * 
 * 职责:
 * 1. 设置Compose内容
 * 2. 应用主题
 * 3. 初始化导航
 * 4. 依赖注入入口
 * 
 * 注意: 此Activity必须放在app模块（Application模块）中，
 * 因为Hilt的@AndroidEntryPoint注解需要AGP的字节码转换支持，
 * 而字节码转换只在Application模块中生效。
 * 
 * 主题说明: 使用app模块本地的AppTheme而非presentation模块的EmpathyTheme，
 * 以解决多模块架构下ThemeKt类在运行时无法被找到的问题。
 * 
 * 开发者模式: DeveloperModeViewModel使用Activity作为ViewModelStoreOwner，
 * 在SettingsScreen中通过hiltViewModel()获取同一个实例
 * @see BUG-00050 开发者模式导航时意外退出
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            // 应用主题 - 使用app模块本地的AppTheme
            AppTheme {
                // Surface容器,提供背景色
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 创建导航控制器
                    val navController = rememberNavController()
                    
                    // 导航图
                    NavGraph(
                        navController = navController
                    )
                }
            }
        }
    }
}
