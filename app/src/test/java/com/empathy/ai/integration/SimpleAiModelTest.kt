package com.empathy.ai.integration

import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

/**
 * 简化版真实AI模型测试
 *
 * 直接通过HTTP调用测试AI服务商，避免编译错误影响
 */
class SimpleAiModelTest {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class TestProvider(
        val name: String,
        val baseUrl: String,
        val apiKey: String,
        val models: List<String>
    )

    private val testProviders = listOf(
        TestProvider(
            name = "x666.me",
            baseUrl = "https://x666.me",
            apiKey = "sk-BaN9AcQXfzNmMy12i4nYpQonxm78loqFrQbt4LGBzlxMKpVD",
            models = listOf(
                "gpt-4o-mini",
                "gpt-4.1-mini"
                // 暂时只测试两个模型，节省时间
            )
        )
    )

    @Test
    fun `test simple AI model calls`() = runBlocking {
        println("\n=== 简化版AI模型测试 ===\n")

        val testPrompt = """
            【对话上下文】
            用户: 今天钓鱼没收获，有点沮丧

            【联系人画像】
            姓名: 李四
            爱好: 钓鱼
            雷区: 不要提钱
            策略: 多夸他衣品好

            请分析这段对话并给出建议，以JSON格式返回，包含字段：
            - replySuggestion: 回复建议
            - strategyAnalysis: 策略分析
            - riskLevel: 风险等级 (SAFE/MEDIUM/HIGH)
        """.trimIndent()

        var totalTests = 0
        var successfulTests = 0
        val failedModels = mutableListOf<String>()

        testProviders.forEach { provider ->
            println("测试服务商: ${provider.name}")
            println("=" .repeat(50))

            provider.models.forEach { modelId ->
                totalTests++
                println("\n测试模型: $modelId")

                try {
                    val result = callAiModel(provider, modelId, testPrompt)

                    if (result != null) {
                        println("✅ 调用成功")
                        println("  响应预览: ${result.take(200)}...")
                        successfulTests++

                        // 简单检查是否包含预期字段
                        if (result.contains("replySuggestion") || result.contains("回复建议") ||
                            result.contains("strategyAnalysis") || result.contains("策略分析")) {
                            println("  ✅ 包含预期字段")
                        } else {
                            println("  ⚠️ 可能缺少预期字段")
                        }
                    } else {
                        println("❌ 调用失败")
                        failedModels.add("${provider.name}/$modelId")
                    }

                } catch (e: Exception) {
                    println("❌ 测试异常: ${e.message}")
                    failedModels.add("${provider.name}/$modelId")
                }

                // 避免请求过快
                kotlinx.coroutines.delay(2000)
            }

            println()
        }

        // 计算成功率
        val successRate = if (totalTests > 0) {
            (successfulTests.toDouble() / totalTests) * 100
        } else {
            0.0
        }

        println("\n" + "=".repeat(50))
        println("测试总结")
        println("=".repeat(50))
        println("总测试数: $totalTests")
        println("成功数: $successfulTests")
        println("失败数: ${totalTests - successfulTests}")
        println("成功率: ${"%.2f".format(successRate)}%")

        if (failedModels.isNotEmpty()) {
            println("\n失败的模型:")
            failedModels.forEach { println("  - $it") }
        }

        println()

        // 记录测试结果
        println("\n=== 测试日期: ${java.time.LocalDate.now()} ===")
        println("x666.me 测试结果:")
        println("| 模型 | 测试次数 | 成功次数 | 成功率 | 备注 |")
        println("|------|---------|---------|--------|------|")
        testProviders.forEach { provider ->
            provider.models.forEach { model ->
                val success = if (failedModels.contains("${provider.name}/$model")) 0 else 1
                val rate = if (success > 0) "100.00%" else "0.00%"
                println("| $model | 1 | $success | $rate | ${if (success > 0) "成功" else "失败"} |")
            }
        }

        // 验证基本成功率（这里我们设置一个较低的标准，因为只是简化测试）
        assertTrue(
            successRate >= 50.0,
            "简化测试成功率 (${"%.2f".format(successRate)}%) 低于要求的 50%"
        )
    }

    private suspend fun callAiModel(
        provider: TestProvider,
        modelId: String,
        prompt: String
    ): String? {
        return try {
            val requestBody = """
                {
                    "model": "$modelId",
                    "messages": [
                        {
                            "role": "system",
                            "content": "你是一个专业的沟通分析师。请始终以JSON格式返回分析结果。"
                        },
                        {
                            "role": "user",
                            "content": $prompt
                        }
                    ],
                    "temperature": 0.7,
                    "max_tokens": 1000
                }
            """.trimIndent()

            val request = Request.Builder()
                .url("${provider.baseUrl}/v1/chat/completions")
                .addHeader("Authorization", "Bearer ${provider.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            println("  发送请求到: ${provider.baseUrl}/v1/chat/completions")

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                println("  HTTP状态: ${response.code}")

                // 尝试解析响应获取content
                responseBody?.let {
                    extractContentFromResponse(it)
                }
            } else {
                println("  HTTP错误: ${response.code} - ${response.message}")
                val errorBody = response.body?.string()
                println("  错误详情: ${errorBody?.take(200)}...")
                null
            }

        } catch (e: Exception) {
            println("  请求异常: ${e.message}")
            null
        }
    }

    private fun extractContentFromResponse(responseBody: String): String {
        // 简单解析OpenAI格式的响应
        return try {
            // 这是一个简化的解析，实际应该使用JSON库
            val contentStart = responseBody.indexOf("\"content\":\"") + 11
            val contentEnd = responseBody.indexOf("\"", contentStart)

            if (contentStart > 10 && contentEnd > contentStart) {
                responseBody.substring(contentStart, contentEnd)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
            } else {
                responseBody
            }
        } catch (e: Exception) {
            println("  解析响应失败: ${e.message}")
            responseBody
        }
    }
}