package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.repository.UserProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户画像上下文构建器
 *
 * 负责构建包含用户画像信息的AI分析上下文，
 * 支持智能筛选相关的画像信息以提供更精准的个性化建议。
 */
@Singleton
class UserProfileContextBuilder @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) {
    
    companion object {
        // 关键词匹配规则 - 工作场景
        private val WORK_KEYWORDS = listOf("工作", "职场", "同事", "老板", "领导", "下属", "项目", "加班")
        
        // 关键词匹配规则 - 社交场景
        private val SOCIAL_KEYWORDS = listOf("朋友", "聚会", "派对", "社交", "活动", "聚餐")
        
        // 关键词匹配规则 - 家庭场景
        private val FAMILY_KEYWORDS = listOf("家人", "父母", "爸妈", "孩子", "家庭", "亲戚")
        
        // 关键词匹配规则 - 情感场景
        private val ROMANCE_KEYWORDS = listOf("约会", "恋爱", "对象", "男朋友", "女朋友", "暧昧", "表白")
        
        // 关键词匹配规则 - 学习场景
        private val LEARNING_KEYWORDS = listOf("学习", "考试", "课程", "培训", "读书")
        
        // 关键词匹配规则 - 运动场景
        private val SPORTS_KEYWORDS = listOf("运动", "健身", "跑步", "游泳", "球赛")
        
        // 关键词匹配规则 - 旅行场景
        private val TRAVEL_KEYWORDS = listOf("旅行", "旅游", "出差", "度假", "景点")
        
        // 情感关键词
        private val EMOTION_HAPPY_KEYWORDS = listOf("开心", "高兴", "快乐", "兴奋", "激动")
        private val EMOTION_SAD_KEYWORDS = listOf("难过", "伤心", "失落", "沮丧", "郁闷")
        private val EMOTION_ANGRY_KEYWORDS = listOf("生气", "愤怒", "恼火", "烦躁")
        private val EMOTION_ANXIOUS_KEYWORDS = listOf("焦虑", "紧张", "担心", "害怕", "不安")
        
        // 场景关键词
        private val MEETING_KEYWORDS = listOf("会议", "开会", "讨论", "汇报")
        private val SPEECH_KEYWORDS = listOf("演讲", "发言", "分享", "主持")
        private val NEGOTIATION_KEYWORDS = listOf("谈判", "协商", "商量", "沟通")
        private val INTERVIEW_KEYWORDS = listOf("面试", "应聘", "求职")
        private val DINING_KEYWORDS = listOf("聚餐", "吃饭", "饭局", "宴会")
        private val MOVIE_KEYWORDS = listOf("看电影", "电影院", "影片")
    }
    
    /**
     * 构建包含用户画像的分析上下文
     *
     * @param contact 联系人画像
     * @param userInput 用户输入的聊天内容
     * @return 构建结果，成功返回上下文字符串，失败返回异常
     */
    suspend fun buildAnalysisContext(
        contact: ContactProfile,
        userInput: String
    ): Result<String> {
        return try {
            val userProfileResult = userProfileRepository.getUserProfile()
            
            // 获取用户画像失败时，降级为不含用户画像的上下文
            val userProfile = if (userProfileResult.isSuccess) {
                userProfileResult.getOrThrow()
            } else {
                null
            }
            
            val context = buildString {
                // 1. 用户画像信息（优先级最高）
                if (userProfile != null && !userProfile.isEmpty()) {
                    val filteredProfile = filterRelevantProfileInfo(userProfile, userInput)
                    if (!filteredProfile.isEmpty()) {
                        appendLine("【用户画像（你的特点）】")
                        appendUserProfileSection(this, filteredProfile)
                        appendLine()
                    }
                }
                
                // 2. 联系人信息
                appendLine("【联系人信息】")
                appendContactSection(this, contact)
                appendLine()
                
                // 3. 用户输入的聊天记录
                appendLine("【用户提供的聊天记录】")
                appendLine(userInput)
            }
            
            Result.success(context)
        } catch (e: Exception) {
            Result.failure(Exception("构建分析上下文失败: ${e.message}", e))
        }
    }
    
    /**
     * 根据对话内容智能筛选相关的用户画像信息
     *
     * 使用多层匹配策略：
     * 1. 关键词匹配 - 基础场景匹配
     * 2. 情感分析匹配 - 情绪相关维度
     * 3. 场景匹配 - 特定场景维度
     *
     * @param profile 完整的用户画像
     * @param chatContent 聊天内容
     * @return 筛选后的用户画像
     */
    fun filterRelevantProfileInfo(
        profile: UserProfile,
        chatContent: String
    ): UserProfile {
        val relevantDimensions = mutableSetOf<String>()
        val chatContentLower = chatContent.lowercase()
        
        // 1. 关键词匹配 - 基础场景
        applyKeywordMatching(chatContentLower, relevantDimensions)
        
        // 2. 情感分析匹配
        applyEmotionMatching(chatContentLower, relevantDimensions)
        
        // 3. 场景匹配
        applyScenarioMatching(chatContentLower, relevantDimensions)
        
        // 如果没有匹配到特定维度，返回完整画像
        if (relevantDimensions.isEmpty()) {
            return profile
        }
        
        // 返回筛选后的画像
        return profile.copy(
            personalityTraits = if ("personalityTraits" in relevantDimensions)
                profile.personalityTraits else emptyList(),
            values = if ("values" in relevantDimensions)
                profile.values else emptyList(),
            interests = if ("interests" in relevantDimensions)
                profile.interests else emptyList(),
            communicationStyle = if ("communicationStyle" in relevantDimensions)
                profile.communicationStyle else emptyList(),
            socialPreferences = if ("socialPreferences" in relevantDimensions)
                profile.socialPreferences else emptyList()
            // 自定义维度保持不变，因为无法预测其相关性
        )
    }
    
    /**
     * 应用关键词匹配规则
     */
    private fun applyKeywordMatching(
        chatContent: String,
        relevantDimensions: MutableSet<String>
    ) {
        // 工作场景 -> values, communicationStyle
        if (WORK_KEYWORDS.any { chatContent.contains(it) }) {
            relevantDimensions.addAll(listOf("values", "communicationStyle"))
        }
        
        // 社交场景 -> socialPreferences, interests
        if (SOCIAL_KEYWORDS.any { chatContent.contains(it) }) {
            relevantDimensions.addAll(listOf("socialPreferences", "interests"))
        }
        
        // 家庭场景 -> values, personalityTraits
        if (FAMILY_KEYWORDS.any { chatContent.contains(it) }) {
            relevantDimensions.addAll(listOf("values", "personalityTraits"))
        }
        
        // 情感场景 -> interests, communicationStyle, personalityTraits
        if (ROMANCE_KEYWORDS.any { chatContent.contains(it) }) {
            relevantDimensions.addAll(listOf("interests", "communicationStyle", "personalityTraits"))
        }
        
        // 学习场景 -> interests, personalityTraits
        if (LEARNING_KEYWORDS.any { chatContent.contains(it) }) {
            relevantDimensions.addAll(listOf("interests", "personalityTraits"))
        }
        
        // 运动场景 -> interests, socialPreferences
        if (SPORTS_KEYWORDS.any { chatContent.contains(it) }) {
            relevantDimensions.addAll(listOf("interests", "socialPreferences"))
        }
        
        // 旅行场景 -> interests, socialPreferences
        if (TRAVEL_KEYWORDS.any { chatContent.contains(it) }) {
            relevantDimensions.addAll(listOf("interests", "socialPreferences"))
        }
    }
    
    /**
     * 应用情感分析匹配规则
     */
    private fun applyEmotionMatching(
        chatContent: String,
        relevantDimensions: MutableSet<String>
    ) {
        // 开心情绪 -> personalityTraits, interests
        if (EMOTION_HAPPY_KEYWORDS.any { chatContent.contains(it) }) {
            relevantDimensions.addAll(listOf("personalityTraits", "interests"))
        }
        
        // 难过情绪 -> personalityTraits, values
        if (EMOTION_SAD_KEYWORDS.any { chatContent.contains(it) }) {
            relevantDimensions.addAll(listOf("personalityTraits", "values"))
        }
        
        // 生气情绪 -> personalityTraits, communicationStyle
        if (EMOTION_ANGRY_KEYWORDS.any { chatContent.contains(it) }) {
            relevantDimensions.addAll(listOf("personalityTraits", "communicationStyle"))
        }
        
        // 焦虑情绪 -> personalityTraits, socialPreferences
        if (EMOTION_ANXIOUS_KEYWORDS.any { chatContent.contains(it) }) {
            relevantDimensions.addAll(listOf("personalityTraits", "socialPreferences"))
        }
    }
    
    /**
     * 应用场景匹配规则
     */
    private fun applyScenarioMatching(
        chatContent: String,
        relevantDimensions: MutableSet<String>
    ) {
        // 会议场景 -> communicationStyle, values
        if (MEETING_KEYWORDS.any { chatContent.contains(it) }) {
            relevantDimensions.addAll(listOf("communicationStyle", "values"))
        }
        
        // 演讲场景 -> communicationStyle, personalityTraits
        if (SPEECH_KEYWORDS.any { chatContent.contains(it) }) {
            relevantDimensions.addAll(listOf("communicationStyle", "personalityTraits"))
        }
        
        // 谈判场景 -> communicationStyle, values
        if (NEGOTIATION_KEYWORDS.any { chatContent.contains(it) }) {
            relevantDimensions.addAll(listOf("communicationStyle", "values"))
        }
        
        // 面试场景 -> communicationStyle, values, personalityTraits
        if (INTERVIEW_KEYWORDS.any { chatContent.contains(it) }) {
            relevantDimensions.addAll(listOf("communicationStyle", "values", "personalityTraits"))
        }
        
        // 聚餐场景 -> socialPreferences, interests
        if (DINING_KEYWORDS.any { chatContent.contains(it) }) {
            relevantDimensions.addAll(listOf("socialPreferences", "interests"))
        }
        
        // 看电影场景 -> interests, socialPreferences
        if (MOVIE_KEYWORDS.any { chatContent.contains(it) }) {
            relevantDimensions.addAll(listOf("interests", "socialPreferences"))
        }
    }
    
    /**
     * 追加用户画像信息到StringBuilder
     */
    private fun appendUserProfileSection(sb: StringBuilder, profile: UserProfile) {
        with(sb) {
            if (profile.personalityTraits.isNotEmpty()) {
                appendLine("- 性格特点: ${profile.personalityTraits.joinToString("、")}")
            }
            if (profile.values.isNotEmpty()) {
                appendLine("- 价值观: ${profile.values.joinToString("、")}")
            }
            if (profile.interests.isNotEmpty()) {
                appendLine("- 兴趣爱好: ${profile.interests.joinToString("、")}")
            }
            if (profile.communicationStyle.isNotEmpty()) {
                appendLine("- 沟通风格: ${profile.communicationStyle.joinToString("、")}")
            }
            if (profile.socialPreferences.isNotEmpty()) {
                appendLine("- 社交偏好: ${profile.socialPreferences.joinToString("、")}")
            }
            // 自定义维度
            profile.customDimensions.forEach { (dimension, tags) ->
                if (tags.isNotEmpty()) {
                    appendLine("- $dimension: ${tags.joinToString("、")}")
                }
            }
        }
    }
    
    /**
     * 追加联系人信息到StringBuilder
     */
    private fun appendContactSection(sb: StringBuilder, contact: ContactProfile) {
        with(sb) {
            appendLine("- 联系人: ${contact.name}")
            
            if (contact.targetGoal.isNotBlank()) {
                appendLine("- 攻略目标: ${contact.targetGoal}")
            }
            
            if (contact.facts.isNotEmpty()) {
                appendLine("- 联系人特点:")
                contact.facts.forEach { fact ->
                    appendLine("  - ${fact.key}: ${fact.value}")
                }
            }
            
            // 添加关系等级信息
            appendLine("- 关系等级: ${contact.getRelationshipLevel().name}")
            appendLine("- 关系分数: ${contact.relationshipScore}")
        }
    }
}
