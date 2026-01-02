package com.empathy.ai.build

/**
 * 版本变更类型
 * 定义版本号递增的级别
 */
enum class VersionBump {
    /** 主版本号递增 - 不兼容的API修改 */
    MAJOR,
    /** 次版本号递增 - 向下兼容的功能性新增 */
    MINOR,
    /** 修订号递增 - 向下兼容的问题修正 */
    PATCH,
    /** 不影响版本号 */
    NONE
}

/**
 * 提交类型枚举
 * 基于 Conventional Commits 规范定义
 *
 * @property prefix 提交前缀
 * @property versionBump 版本变更类型
 * @property description 类型描述
 * @property emoji 类型对应的emoji图标
 * 
 * @see TDD-00024 4.1.3 CommitType枚举
 * @see https://www.conventionalcommits.org/
 */
enum class CommitType(
    val prefix: String,
    val versionBump: VersionBump,
    val description: String,
    val emoji: String
) {
    // ========== Major版本递增 ==========
    
    /** 破坏性变更 - 功能类 */
    BREAKING_CHANGE(
        prefix = "feat!",
        versionBump = VersionBump.MAJOR,
        description = "破坏性变更（功能）",
        emoji = "💥"
    ),
    
    /** 破坏性变更 - 修复类 */
    BREAKING_FIX(
        prefix = "fix!",
        versionBump = VersionBump.MAJOR,
        description = "破坏性变更（修复）",
        emoji = "💥"
    ),
    
    // ========== Minor版本递增 ==========
    
    /** 新功能 */
    FEATURE(
        prefix = "feat",
        versionBump = VersionBump.MINOR,
        description = "新功能",
        emoji = "✨"
    ),
    
    // ========== Patch版本递增 ==========
    
    /** Bug修复 */
    FIX(
        prefix = "fix",
        versionBump = VersionBump.PATCH,
        description = "Bug修复",
        emoji = "🐛"
    ),
    
    /** 性能优化 */
    PERF(
        prefix = "perf",
        versionBump = VersionBump.PATCH,
        description = "性能优化",
        emoji = "⚡"
    ),
    
    // ========== 不影响版本号 ==========
    
    /** 文档更新 */
    DOCS(
        prefix = "docs",
        versionBump = VersionBump.NONE,
        description = "文档更新",
        emoji = "📝"
    ),
    
    /** 代码格式 */
    STYLE(
        prefix = "style",
        versionBump = VersionBump.NONE,
        description = "代码格式",
        emoji = "💄"
    ),
    
    /** 代码重构 */
    REFACTOR(
        prefix = "refactor",
        versionBump = VersionBump.NONE,
        description = "代码重构",
        emoji = "♻️"
    ),
    
    /** 测试相关 */
    TEST(
        prefix = "test",
        versionBump = VersionBump.NONE,
        description = "测试相关",
        emoji = "✅"
    ),
    
    /** 构建/工具 */
    CHORE(
        prefix = "chore",
        versionBump = VersionBump.NONE,
        description = "构建/工具",
        emoji = "🔧"
    ),
    
    /** CI配置 */
    CI(
        prefix = "ci",
        versionBump = VersionBump.NONE,
        description = "CI配置",
        emoji = "👷"
    ),
    
    /** 构建系统 */
    BUILD(
        prefix = "build",
        versionBump = VersionBump.NONE,
        description = "构建系统",
        emoji = "📦"
    ),
    
    /** 依赖更新 */
    DEPS(
        prefix = "deps",
        versionBump = VersionBump.NONE,
        description = "依赖更新",
        emoji = "⬆️"
    ),
    
    /** 回滚 */
    REVERT(
        prefix = "revert",
        versionBump = VersionBump.NONE,
        description = "回滚",
        emoji = "⏪"
    );
    
    companion object {
        /**
         * 从提交消息解析提交类型
         * 支持格式: type: message, type(scope): message, type!: message
         * 
         * @param message 提交消息
         * @return 匹配的提交类型，未匹配返回null
         */
        fun fromMessage(message: String): CommitType? {
            val lowerMessage = message.lowercase().trim()
            
            // 优先匹配破坏性变更（带!的类型）
            if (lowerMessage.startsWith("feat!") || 
                lowerMessage.contains("feat(") && lowerMessage.contains(")!:")) {
                return BREAKING_CHANGE
            }
            if (lowerMessage.startsWith("fix!") ||
                lowerMessage.contains("fix(") && lowerMessage.contains(")!:")) {
                return BREAKING_FIX
            }
            
            // 匹配普通类型
            return values().find { type ->
                // 匹配 "type:" 或 "type(scope):"
                lowerMessage.startsWith("${type.prefix}:") ||
                lowerMessage.startsWith("${type.prefix}(")
            }
        }
        
        /**
         * 从前缀字符串获取提交类型
         */
        fun fromPrefix(prefix: String): CommitType? {
            val lowerPrefix = prefix.lowercase().trim()
            return values().find { it.prefix.lowercase() == lowerPrefix }
        }
        
        /**
         * 获取所有会影响版本号的提交类型
         */
        fun getVersionBumpTypes(): List<CommitType> {
            return values().filter { it.versionBump != VersionBump.NONE }
        }
        
        /**
         * 获取指定版本变更级别的所有提交类型
         */
        fun getByVersionBump(bump: VersionBump): List<CommitType> {
            return values().filter { it.versionBump == bump }
        }
    }
}
