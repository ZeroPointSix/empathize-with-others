package com.empathy.ai.build

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * VersionCalculator 单元测试
 * 
 * @see TDD-00024 6.1 单元测试
 */
class VersionCalculatorTest {
    
    private val calculator = VersionCalculator()
    
    // ========== 版本计算测试 ==========
    
    @Test
    fun `empty commits returns same version`() {
        val current = SemanticVersion(1, 2, 3)
        val next = calculator.calculateNextVersion(current, emptyList())
        assertEquals(current, next)
    }
    
    @Test
    fun `breaking change bumps major`() {
        val current = SemanticVersion(1, 2, 3)
        val commits = listOf(
            ParsedCommit(CommitType.BREAKING_CHANGE, subject = "破坏性变更", isBreaking = true)
        )
        val next = calculator.calculateNextVersion(current, commits)
        assertEquals(SemanticVersion(2, 0, 0), next)
    }
    
    @Test
    fun `breaking fix bumps major`() {
        val current = SemanticVersion(1, 2, 3)
        val commits = listOf(
            ParsedCommit(CommitType.BREAKING_FIX, subject = "破坏性修复", isBreaking = true)
        )
        val next = calculator.calculateNextVersion(current, commits)
        assertEquals(SemanticVersion(2, 0, 0), next)
    }
    
    @Test
    fun `feature bumps minor`() {
        val current = SemanticVersion(1, 2, 3)
        val commits = listOf(
            ParsedCommit(CommitType.FEATURE, subject = "新功能")
        )
        val next = calculator.calculateNextVersion(current, commits)
        assertEquals(SemanticVersion(1, 3, 0), next)
    }
    
    @Test
    fun `fix bumps patch`() {
        val current = SemanticVersion(1, 2, 3)
        val commits = listOf(
            ParsedCommit(CommitType.FIX, subject = "修复问题")
        )
        val next = calculator.calculateNextVersion(current, commits)
        assertEquals(SemanticVersion(1, 2, 4), next)
    }
    
    @Test
    fun `perf bumps patch`() {
        val current = SemanticVersion(1, 2, 3)
        val commits = listOf(
            ParsedCommit(CommitType.PERF, subject = "性能优化")
        )
        val next = calculator.calculateNextVersion(current, commits)
        assertEquals(SemanticVersion(1, 2, 4), next)
    }
    
    @Test
    fun `docs does not bump version`() {
        val current = SemanticVersion(1, 2, 3)
        val commits = listOf(
            ParsedCommit(CommitType.DOCS, subject = "更新文档")
        )
        val next = calculator.calculateNextVersion(current, commits)
        assertEquals(current, next)
    }
    
    @Test
    fun `chore does not bump version`() {
        val current = SemanticVersion(1, 2, 3)
        val commits = listOf(
            ParsedCommit(CommitType.CHORE, subject = "更新依赖")
        )
        val next = calculator.calculateNextVersion(current, commits)
        assertEquals(current, next)
    }
    
    // ========== 优先级测试 ==========
    
    @Test
    fun `highest bump wins - major over minor`() {
        val current = SemanticVersion(1, 2, 3)
        val commits = listOf(
            ParsedCommit(CommitType.FEATURE, subject = "新功能"),
            ParsedCommit(CommitType.BREAKING_CHANGE, subject = "破坏性变更", isBreaking = true)
        )
        val next = calculator.calculateNextVersion(current, commits)
        assertEquals(SemanticVersion(2, 0, 0), next)
    }
    
    @Test
    fun `highest bump wins - minor over patch`() {
        val current = SemanticVersion(1, 2, 3)
        val commits = listOf(
            ParsedCommit(CommitType.FIX, subject = "修复问题"),
            ParsedCommit(CommitType.FEATURE, subject = "新功能")
        )
        val next = calculator.calculateNextVersion(current, commits)
        assertEquals(SemanticVersion(1, 3, 0), next)
    }
    
    @Test
    fun `highest bump wins - patch over none`() {
        val current = SemanticVersion(1, 2, 3)
        val commits = listOf(
            ParsedCommit(CommitType.DOCS, subject = "更新文档"),
            ParsedCommit(CommitType.FIX, subject = "修复问题")
        )
        val next = calculator.calculateNextVersion(current, commits)
        assertEquals(SemanticVersion(1, 2, 4), next)
    }
    
    @Test
    fun `isBreaking flag triggers major bump`() {
        val current = SemanticVersion(1, 2, 3)
        val commits = listOf(
            ParsedCommit(CommitType.FIX, subject = "修复问题", isBreaking = true)
        )
        val next = calculator.calculateNextVersion(current, commits)
        assertEquals(SemanticVersion(2, 0, 0), next)
    }
    
    // ========== determineHighestBump 测试 ==========
    
    @Test
    fun `determineHighestBump returns NONE for empty list`() {
        val bump = calculator.determineHighestBump(emptyList())
        assertEquals(VersionBump.NONE, bump)
    }
    
    @Test
    fun `determineHighestBump returns MAJOR for breaking`() {
        val commits = listOf(
            ParsedCommit(CommitType.FEATURE, subject = "新功能", isBreaking = true)
        )
        val bump = calculator.determineHighestBump(commits)
        assertEquals(VersionBump.MAJOR, bump)
    }
    
    @Test
    fun `determineHighestBump returns MINOR for feature`() {
        val commits = listOf(
            ParsedCommit(CommitType.FEATURE, subject = "新功能")
        )
        val bump = calculator.determineHighestBump(commits)
        assertEquals(VersionBump.MINOR, bump)
    }
    
    @Test
    fun `determineHighestBump returns PATCH for fix`() {
        val commits = listOf(
            ParsedCommit(CommitType.FIX, subject = "修复")
        )
        val bump = calculator.determineHighestBump(commits)
        assertEquals(VersionBump.PATCH, bump)
    }
    
    // ========== 变更日志生成测试 ==========
    
    @Test
    fun `generateChangelog for empty commits`() {
        val changelog = calculator.generateChangelog(emptyList())
        assertEquals("无变更记录", changelog)
    }
    
    @Test
    fun `generateChangelog includes features`() {
        val commits = listOf(
            ParsedCommit(CommitType.FEATURE, subject = "添加登录功能")
        )
        val changelog = calculator.generateChangelog(commits)
        assertTrue(changelog.contains("✨ 新功能"))
        assertTrue(changelog.contains("添加登录功能"))
    }
    
    @Test
    fun `generateChangelog includes fixes`() {
        val commits = listOf(
            ParsedCommit(CommitType.FIX, subject = "修复崩溃问题")
        )
        val changelog = calculator.generateChangelog(commits)
        assertTrue(changelog.contains("🐛 Bug修复"))
        assertTrue(changelog.contains("修复崩溃问题"))
    }
    
    @Test
    fun `generateChangelog includes breaking changes first`() {
        val commits = listOf(
            ParsedCommit(CommitType.FEATURE, subject = "新功能"),
            ParsedCommit(CommitType.BREAKING_CHANGE, subject = "破坏性变更", isBreaking = true)
        )
        val changelog = calculator.generateChangelog(commits)
        val breakingIndex = changelog.indexOf("⚠️ 破坏性变更")
        val featureIndex = changelog.indexOf("✨ 新功能")
        assertTrue(breakingIndex < featureIndex, "破坏性变更应该在新功能之前")
    }
    
    @Test
    fun `generateChangelog includes scope`() {
        val commits = listOf(
            ParsedCommit(CommitType.FIX, scope = "ui", subject = "修复按钮")
        )
        val changelog = calculator.generateChangelog(commits)
        assertTrue(changelog.contains("**ui**"))
    }
    
    @Test
    fun `generateChangelog includes performance`() {
        val commits = listOf(
            ParsedCommit(CommitType.PERF, subject = "优化查询")
        )
        val changelog = calculator.generateChangelog(commits)
        assertTrue(changelog.contains("⚡ 性能优化"))
    }
    
    @Test
    fun `generateChangelog includes docs`() {
        val commits = listOf(
            ParsedCommit(CommitType.DOCS, subject = "更新README")
        )
        val changelog = calculator.generateChangelog(commits)
        assertTrue(changelog.contains("📝 文档更新"))
    }
    
    @Test
    fun `generateChangelog includes refactor`() {
        val commits = listOf(
            ParsedCommit(CommitType.REFACTOR, subject = "重构代码")
        )
        val changelog = calculator.generateChangelog(commits)
        assertTrue(changelog.contains("♻️ 代码重构"))
    }
    
    // ========== 摘要生成测试 ==========
    
    @Test
    fun `generateSummary for empty commits`() {
        val summary = calculator.generateSummary(emptyList())
        assertEquals("无变更", summary)
    }
    
    @Test
    fun `generateSummary counts features`() {
        val commits = listOf(
            ParsedCommit(CommitType.FEATURE, subject = "功能1"),
            ParsedCommit(CommitType.FEATURE, subject = "功能2")
        )
        val summary = calculator.generateSummary(commits)
        assertTrue(summary.contains("2 个新功能"))
    }
    
    @Test
    fun `generateSummary counts fixes`() {
        val commits = listOf(
            ParsedCommit(CommitType.FIX, subject = "修复1")
        )
        val summary = calculator.generateSummary(commits)
        assertTrue(summary.contains("1 个Bug修复"))
    }
    
    @Test
    fun `generateSummary counts breaking changes`() {
        val commits = listOf(
            ParsedCommit(CommitType.BREAKING_CHANGE, subject = "破坏性", isBreaking = true)
        )
        val summary = calculator.generateSummary(commits)
        assertTrue(summary.contains("1 个破坏性变更"))
    }
    
    // ========== 分析测试 ==========
    
    @Test
    fun `analyzeCommits returns correct counts`() {
        val commits = listOf(
            ParsedCommit(CommitType.FEATURE, subject = "功能1"),
            ParsedCommit(CommitType.FEATURE, subject = "功能2"),
            ParsedCommit(CommitType.FIX, subject = "修复1"),
            ParsedCommit(CommitType.DOCS, subject = "文档1"),
            ParsedCommit(CommitType.BREAKING_CHANGE, subject = "破坏性", isBreaking = true)
        )
        
        val analysis = calculator.analyzeCommits(commits)
        
        assertEquals(5, analysis.totalCount)
        assertEquals(1, analysis.breakingCount)
        assertEquals(2, analysis.featureCount)
        assertEquals(1, analysis.fixCount)
        assertEquals(1, analysis.docsCount)
        assertEquals(VersionBump.MAJOR, analysis.suggestedBump)
    }
    
    @Test
    fun `analyzeCommits extracts scopes`() {
        val commits = listOf(
            ParsedCommit(CommitType.FIX, scope = "ui", subject = "修复1"),
            ParsedCommit(CommitType.FIX, scope = "api", subject = "修复2"),
            ParsedCommit(CommitType.FIX, scope = "ui", subject = "修复3")  // 重复scope
        )
        
        val analysis = calculator.analyzeCommits(commits)
        
        assertEquals(2, analysis.scopes.size)
        assertTrue(analysis.scopes.contains("ui"))
        assertTrue(analysis.scopes.contains("api"))
    }
}
