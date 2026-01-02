package com.empathy.ai.build

import java.io.File

/**
 * 解析后的提交信息
 * 
 * @property type 提交类型
 * @property scope 影响范围（可选）
 * @property subject 提交主题
 * @property isBreaking 是否为破坏性变更
 * @property hash 提交哈希（短格式）
 */
data class ParsedCommit(
    val type: CommitType,
    val scope: String? = null,
    val subject: String,
    val isBreaking: Boolean = false,
    val hash: String? = null
) {
    /**
     * 格式化为变更日志条目
     */
    fun toChangelogEntry(): String {
        val scopeStr = scope?.let { "($it)" } ?: ""
        val breakingMark = if (isBreaking) " ⚠️" else ""
        return "- ${type.emoji} $scopeStr$subject$breakingMark"
    }
}

/**
 * Git提交信息解析器
 * 解析自上次发布以来的所有提交，提取变更类型
 * 
 * @property projectDir 项目根目录
 * 
 * @see TDD-00024 4.2.1 CommitParser提交解析器
 */
class CommitParser(private val projectDir: File) {
    
    companion object {
        /** 版本标签前缀 */
        private const val TAG_PREFIX = "v"
        
        /** Conventional Commits 正则表达式 */
        // 支持空括号: feat(): message 或 feat(scope): message
        private val COMMIT_REGEX = """^(\w+)(?:\(([^)]*)\))?(!)?: (.+)$""".toRegex()
        
        /** 提交哈希正则（用于从git log输出中提取） */
        private val HASH_REGEX = """^([a-f0-9]+)\s+(.+)$""".toRegex()
    }
    
    /**
     * 解析自上次标签以来的所有提交
     * @return 解析后的提交列表
     */
    fun parseCommitsSinceLastTag(): List<ParsedCommit> {
        val lastTag = getLastTag()
        val commits = if (lastTag != null) {
            getCommitsSince(lastTag)
        } else {
            // 没有标签时，获取所有提交
            getAllCommits()
        }
        return commits.mapNotNull { parseCommitLine(it) }
    }
    
    /**
     * 解析指定范围内的提交
     * @param fromRef 起始引用（标签或提交哈希）
     * @param toRef 结束引用，默认为HEAD
     */
    fun parseCommitsBetween(fromRef: String, toRef: String = "HEAD"): List<ParsedCommit> {
        val commits = getCommitsBetween(fromRef, toRef)
        return commits.mapNotNull { parseCommitLine(it) }
    }
    
    /**
     * 获取最近的版本标签
     * @return 最近的标签名，如果没有标签则返回null
     */
    fun getLastTag(): String? {
        return try {
            val process = ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            
            if (exitCode == 0 && output.isNotEmpty()) output else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 获取所有版本标签（按版本号排序）
     */
    fun getAllTags(): List<String> {
        return try {
            val process = ProcessBuilder("git", "tag", "-l", "${TAG_PREFIX}*", "--sort=-v:refname")
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readLines()
            process.waitFor()
            
            output.filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 获取指定标签之后的所有提交
     */
    private fun getCommitsSince(tag: String): List<String> {
        return try {
            val process = ProcessBuilder(
                "git", "log", "$tag..HEAD", "--oneline", "--no-merges"
            )
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readLines()
            process.waitFor()
            
            output.filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 获取两个引用之间的提交
     */
    private fun getCommitsBetween(fromRef: String, toRef: String): List<String> {
        return try {
            val process = ProcessBuilder(
                "git", "log", "$fromRef..$toRef", "--oneline", "--no-merges"
            )
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readLines()
            process.waitFor()
            
            output.filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 获取所有提交（当没有标签时使用）
     */
    private fun getAllCommits(): List<String> {
        return try {
            val process = ProcessBuilder(
                "git", "log", "--oneline", "--no-merges", "-n", "100"  // 限制100条
            )
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readLines()
            process.waitFor()
            
            output.filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 解析单行git log输出
     * 格式: "abc1234 type(scope): message"
     */
    private fun parseCommitLine(line: String): ParsedCommit? {
        val hashMatch = HASH_REGEX.find(line)
        val (hash, message) = if (hashMatch != null) {
            hashMatch.groupValues[1] to hashMatch.groupValues[2]
        } else {
            null to line
        }
        
        return parseCommitMessage(message.trim())?.copy(hash = hash)
    }
    
    /**
     * 解析提交消息
     * 支持 Conventional Commits 格式: type(scope)!: message
     * 
     * @param message 提交消息（不含哈希）
     * @return 解析后的提交信息，格式不匹配返回null
     */
    fun parseCommitMessage(message: String): ParsedCommit? {
        val match = COMMIT_REGEX.find(message.trim()) ?: return null
        
        val typeStr = match.groupValues[1]
        val scope = match.groupValues[2].takeIf { it.isNotEmpty() }
        val isBreaking = match.groupValues[3] == "!"
        val subject = match.groupValues[4].trim()
        
        // 确定提交类型
        val type = when {
            isBreaking && typeStr.lowercase() == "feat" -> CommitType.BREAKING_CHANGE
            isBreaking && typeStr.lowercase() == "fix" -> CommitType.BREAKING_FIX
            isBreaking -> CommitType.fromPrefix(typeStr) ?: return null
            else -> CommitType.fromPrefix(typeStr) ?: return null
        }
        
        return ParsedCommit(
            type = type,
            scope = scope,
            subject = subject,
            isBreaking = isBreaking
        )
    }
    
    /**
     * 检查Git是否可用
     */
    fun isGitAvailable(): Boolean {
        return try {
            val process = ProcessBuilder("git", "--version")
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查当前目录是否为Git仓库
     */
    fun isGitRepository(): Boolean {
        return try {
            val process = ProcessBuilder("git", "rev-parse", "--git-dir")
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取当前分支名
     */
    fun getCurrentBranch(): String? {
        return try {
            val process = ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0) output else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 获取当前提交的短哈希
     */
    fun getCurrentCommitHash(): String? {
        return try {
            val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0) output else null
        } catch (e: Exception) {
            null
        }
    }
}
