package com.empathy.ai.build

/**
 * 版本计算器
 * 根据Git提交历史计算新版本号
 * 
 * @see TDD-00024 4.2.2 VersionCalculator版本计算器
 */
class VersionCalculator {
    
    /**
     * 计算新版本号
     * 根据提交列表中最高优先级的变更类型决定版本递增方式
     * 
     * @param currentVersion 当前版本
     * @param commits 自上次发布以来的提交列表
     * @return 新版本号
     */
    fun calculateNextVersion(
        currentVersion: SemanticVersion,
        commits: List<ParsedCommit>
    ): SemanticVersion {
        if (commits.isEmpty()) {
            return currentVersion
        }
        
        // 确定最高优先级的版本变更类型
        val highestBump = determineHighestBump(commits)
        
        return when (highestBump) {
            VersionBump.MAJOR -> currentVersion.bumpMajor()
            VersionBump.MINOR -> currentVersion.bumpMinor()
            VersionBump.PATCH -> currentVersion.bumpPatch()
            VersionBump.NONE -> currentVersion
        }
    }
    
    /**
     * 确定最高优先级的版本变更类型
     */
    fun determineHighestBump(commits: List<ParsedCommit>): VersionBump {
        if (commits.isEmpty()) return VersionBump.NONE
        
        // 检查是否有破坏性变更
        if (commits.any { it.isBreaking }) {
            return VersionBump.MAJOR
        }
        
        // 按优先级排序：MAJOR > MINOR > PATCH > NONE
        return commits
            .map { it.type.versionBump }
            .minByOrNull { it.ordinal }  // ordinal越小优先级越高
            ?: VersionBump.NONE
    }
    
    /**
     * 生成变更日志
     * 按提交类型分组，生成格式化的变更日志
     * 
     * @param commits 提交列表
     * @return 格式化的变更日志（Markdown格式）
     */
    fun generateChangelog(commits: List<ParsedCommit>): String {
        if (commits.isEmpty()) {
            return "无变更记录"
        }
        
        val grouped = commits.groupBy { it.type }
        
        return buildString {
            // 破坏性变更（最重要，放在最前面）
            val breaking = commits.filter { it.isBreaking }
            if (breaking.isNotEmpty()) {
                appendLine("### ⚠️ 破坏性变更")
                breaking.forEach { commit ->
                    appendLine("- ${commit.subject}")
                }
                appendLine()
            }
            
            // 新功能
            grouped[CommitType.FEATURE]?.let { features ->
                appendLine("### ✨ 新功能")
                features.filterNot { it.isBreaking }.forEach { commit ->
                    val scope = commit.scope?.let { "**$it**: " } ?: ""
                    appendLine("- $scope${commit.subject}")
                }
                appendLine()
            }
            
            // Bug修复
            grouped[CommitType.FIX]?.let { fixes ->
                appendLine("### 🐛 Bug修复")
                fixes.filterNot { it.isBreaking }.forEach { commit ->
                    val scope = commit.scope?.let { "**$it**: " } ?: ""
                    appendLine("- $scope${commit.subject}")
                }
                appendLine()
            }
            
            // 性能优化
            grouped[CommitType.PERF]?.let { perfs ->
                appendLine("### ⚡ 性能优化")
                perfs.forEach { commit ->
                    val scope = commit.scope?.let { "**$it**: " } ?: ""
                    appendLine("- $scope${commit.subject}")
                }
                appendLine()
            }
            
            // 代码重构
            grouped[CommitType.REFACTOR]?.let { refactors ->
                appendLine("### ♻️ 代码重构")
                refactors.forEach { commit ->
                    val scope = commit.scope?.let { "**$it**: " } ?: ""
                    appendLine("- $scope${commit.subject}")
                }
                appendLine()
            }
            
            // 文档更新
            grouped[CommitType.DOCS]?.let { docs ->
                appendLine("### 📝 文档更新")
                docs.forEach { commit ->
                    val scope = commit.scope?.let { "**$it**: " } ?: ""
                    appendLine("- $scope${commit.subject}")
                }
                appendLine()
            }
            
            // 其他变更（测试、构建、CI等）
            val otherTypes = listOf(
                CommitType.TEST, CommitType.CHORE, CommitType.CI, 
                CommitType.BUILD, CommitType.DEPS, CommitType.STYLE
            )
            val others = otherTypes.flatMap { grouped[it] ?: emptyList() }
            if (others.isNotEmpty()) {
                appendLine("### 🔧 其他变更")
                others.forEach { commit ->
                    val scope = commit.scope?.let { "**$it**: " } ?: ""
                    appendLine("- ${commit.type.emoji} $scope${commit.subject}")
                }
                appendLine()
            }
        }.trimEnd()
    }
    
    /**
     * 生成简短的变更摘要
     * 用于版本发布说明的简短描述
     */
    fun generateSummary(commits: List<ParsedCommit>): String {
        if (commits.isEmpty()) {
            return "无变更"
        }
        
        val stats = mutableListOf<String>()
        
        val breaking = commits.count { it.isBreaking }
        if (breaking > 0) {
            stats.add("$breaking 个破坏性变更")
        }
        
        val features = commits.count { it.type == CommitType.FEATURE && !it.isBreaking }
        if (features > 0) {
            stats.add("$features 个新功能")
        }
        
        val fixes = commits.count { it.type == CommitType.FIX && !it.isBreaking }
        if (fixes > 0) {
            stats.add("$fixes 个Bug修复")
        }
        
        val perfs = commits.count { it.type == CommitType.PERF }
        if (perfs > 0) {
            stats.add("$perfs 个性能优化")
        }
        
        val others = commits.size - breaking - features - fixes - perfs
        if (others > 0) {
            stats.add("$others 个其他变更")
        }
        
        return stats.joinToString("，")
    }
    
    /**
     * 分析提交统计信息
     */
    fun analyzeCommits(commits: List<ParsedCommit>): CommitAnalysis {
        return CommitAnalysis(
            totalCount = commits.size,
            breakingCount = commits.count { it.isBreaking },
            featureCount = commits.count { it.type == CommitType.FEATURE },
            fixCount = commits.count { it.type == CommitType.FIX },
            perfCount = commits.count { it.type == CommitType.PERF },
            docsCount = commits.count { it.type == CommitType.DOCS },
            refactorCount = commits.count { it.type == CommitType.REFACTOR },
            otherCount = commits.count { 
                it.type !in listOf(
                    CommitType.FEATURE, CommitType.FIX, CommitType.PERF,
                    CommitType.DOCS, CommitType.REFACTOR,
                    CommitType.BREAKING_CHANGE, CommitType.BREAKING_FIX
                )
            },
            scopes = commits.mapNotNull { it.scope }.distinct(),
            suggestedBump = determineHighestBump(commits)
        )
    }
}

/**
 * 提交分析结果
 */
data class CommitAnalysis(
    val totalCount: Int,
    val breakingCount: Int,
    val featureCount: Int,
    val fixCount: Int,
    val perfCount: Int,
    val docsCount: Int,
    val refactorCount: Int,
    val otherCount: Int,
    val scopes: List<String>,
    val suggestedBump: VersionBump
) {
    /**
     * 格式化输出分析结果
     */
    override fun toString(): String = buildString {
        appendLine("提交分析结果:")
        appendLine("  总提交数: $totalCount")
        if (breakingCount > 0) appendLine("  破坏性变更: $breakingCount")
        if (featureCount > 0) appendLine("  新功能: $featureCount")
        if (fixCount > 0) appendLine("  Bug修复: $fixCount")
        if (perfCount > 0) appendLine("  性能优化: $perfCount")
        if (docsCount > 0) appendLine("  文档更新: $docsCount")
        if (refactorCount > 0) appendLine("  代码重构: $refactorCount")
        if (otherCount > 0) appendLine("  其他: $otherCount")
        if (scopes.isNotEmpty()) appendLine("  影响范围: ${scopes.joinToString(", ")}")
        appendLine("  建议版本变更: $suggestedBump")
    }
}
