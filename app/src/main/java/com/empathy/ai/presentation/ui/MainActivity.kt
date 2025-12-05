package com.empathy.ai.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.empathy.ai.presentation.navigation.NavGraph
import com.empathy.ai.presentation.theme.EmpathyTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 应用主Activity
 * 
 * 职责:
 * 1. 设置Compose内容
 * 2. 应用主题
 * 3. 初始化导航
 * 4. 依赖注入入口
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            // 应用主题
            EmpathyTheme {
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
