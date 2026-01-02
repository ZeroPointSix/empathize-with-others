package com.empathy.ai.build

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * CommitParser 单元测试
 * 
 * @see TDD-00024 6.1 单元测试
 */
class CommitParserTest {
    
    private val parser = CommitParser(File("."))
    
    // ========== 提交消息解析测试 ==========
    
    @Test
    fun `parse feat commit`() {
        val commit = parser.parseCommitMessage("feat: 添加新功能")
        
        assertNotNull(commit)
        assertEquals(CommitType.FEATURE, commit.type)
        assertEquals("添加新功能", commit.subject)
        assertFalse(commit.isBreaking)
        assertNull(commit.scope)
    }
    
    @Test
    fun `parse fix commit`() {
        val commit = parser.parseCommitMessage("fix: 修复按钮样式")
        
        assertNotNull(commit)
        assertEquals(CommitType.FIX, commit.type)
        assertEquals("修复按钮样式", commit.subject)
        assertFalse(commit.isBreaking)
    }
    
    @Test
    fun `parse breaking change commit with exclamation`() {
        val commit = parser.parseCommitMessage("feat!: 破坏性变更")
        
        assertNotNull(commit)
        assertEquals(CommitType.BREAKING_CHANGE, commit.type)
        assertEquals("破坏性变更", commit.subject)
        assertTrue(commit.isBreaking)
    }
    
    @Test
    fun `parse breaking fix commit`() {
        val commit = parser.parseCommitMessage("fix!: 破坏性修复")
        
        assertNotNull(commit)
        assertEquals(CommitType.BREAKING_FIX, commit.type)
        assertTrue(commit.isBreaking)
    }
    
    @Test
    fun `parse commit with scope`() {
        val commit = parser.parseCommitMessage("fix(ui): 修复按钮样式")
        
        assertNotNull(commit)
        assertEquals(CommitType.FIX, commit.type)
        assertEquals("ui", commit.scope)
        assertEquals("修复按钮样式", commit.subject)
    }
    
    @Test
    fun `parse commit with scope and breaking`() {
        val commit = parser.parseCommitMessage("feat(api)!: 重构API接口")
        
        assertNotNull(commit)
        assertEquals(CommitType.BREAKING_CHANGE, commit.type)
        assertEquals("api", commit.scope)
        assertEquals("重构API接口", commit.subject)
        assertTrue(commit.isBreaking)
    }
    
    @Test
    fun `parse docs commit`() {
        val commit = parser.parseCommitMessage("docs: 更新README")
        
        assertNotNull(commit)
        assertEquals(CommitType.DOCS, commit.type)
        assertEquals("更新README", commit.subject)
    }
    
    @Test
    fun `parse style commit`() {
        val commit = parser.parseCommitMessage("style: 格式化代码")
        
        assertNotNull(commit)
        assertEquals(CommitType.STYLE, commit.type)
    }
    
    @Test
    fun `parse refactor commit`() {
        val commit = parser.parseCommitMessage("refactor: 重构登录模块")
        
        assertNotNull(commit)
        assertEquals(CommitType.REFACTOR, commit.type)
    }
    
    @Test
    fun `parse test commit`() {
        val commit = parser.parseCommitMessage("test: 添加单元测试")
        
        assertNotNull(commit)
        assertEquals(CommitType.TEST, commit.type)
    }
    
    @Test
    fun `parse chore commit`() {
        val commit = parser.parseCommitMessage("chore: 更新依赖")
        
        assertNotNull(commit)
        assertEquals(CommitType.CHORE, commit.type)
    }
    
    @Test
    fun `parse ci commit`() {
        val commit = parser.parseCommitMessage("ci: 配置GitHub Actions")
        
        assertNotNull(commit)
        assertEquals(CommitType.CI, commit.type)
    }
    
    @Test
    fun `parse perf commit`() {
        val commit = parser.parseCommitMessage("perf: 优化查询性能")
        
        assertNotNull(commit)
        assertEquals(CommitType.PERF, commit.type)
    }
    
    @Test
    fun `parse build commit`() {
        val commit = parser.parseCommitMessage("build: 更新Gradle配置")
        
        assertNotNull(commit)
        assertEquals(CommitType.BUILD, commit.type)
    }
    
    @Test
    fun `parse deps commit`() {
        val commit = parser.parseCommitMessage("deps: 升级Kotlin版本")
        
        assertNotNull(commit)
        assertEquals(CommitType.DEPS, commit.type)
    }
    
    @Test
    fun `parse revert commit`() {
        val commit = parser.parseCommitMessage("revert: 回滚上次提交")
        
        assertNotNull(commit)
        assertEquals(CommitType.REVERT, commit.type)
    }
    
    // ========== 无效格式测试 ==========
    
    @Test
    fun `parse invalid commit returns null`() {
        val commit = parser.parseCommitMessage("随便写的提交信息")
        assertNull(commit)
    }
    
    @Test
    fun `parse commit without colon returns null`() {
        val commit = parser.parseCommitMessage("feat 添加新功能")
        assertNull(commit)
    }
    
    @Test
    fun `parse empty message returns null`() {
        val commit = parser.parseCommitMessage("")
        assertNull(commit)
    }
    
    @Test
    fun `parse whitespace only returns null`() {
        val commit = parser.parseCommitMessage("   ")
        assertNull(commit)
    }
    
    @Test
    fun `parse unknown type returns null`() {
        val commit = parser.parseCommitMessage("unknown: 未知类型")
        assertNull(commit)
    }
    
    // ========== 边界情况测试 ==========
    
    @Test
    fun `parse commit with extra spaces`() {
        val commit = parser.parseCommitMessage("  feat:   添加新功能  ")
        
        assertNotNull(commit)
        assertEquals(CommitType.FEATURE, commit.type)
        assertEquals("添加新功能", commit.subject)
    }
    
    @Test
    fun `parse commit with empty scope`() {
        val commit = parser.parseCommitMessage("feat(): 添加新功能")
        
        assertNotNull(commit)
        assertNull(commit.scope)  // 空括号应该被忽略
    }
    
    @Test
    fun `parse commit with complex scope`() {
        val commit = parser.parseCommitMessage("feat(ui/button): 添加按钮组件")
        
        assertNotNull(commit)
        assertEquals("ui/button", commit.scope)
    }
    
    @Test
    fun `parse commit with special characters in subject`() {
        val commit = parser.parseCommitMessage("fix: 修复 #123 问题 (紧急)")
        
        assertNotNull(commit)
        assertEquals("修复 #123 问题 (紧急)", commit.subject)
    }
    
    // ========== toChangelogEntry 测试 ==========
    
    @Test
    fun `toChangelogEntry formats correctly`() {
        val commit = ParsedCommit(
            type = CommitType.FEATURE,
            subject = "添加新功能"
        )
        
        val entry = commit.toChangelogEntry()
        assertTrue(entry.contains("✨"))
        assertTrue(entry.contains("添加新功能"))
    }
    
    @Test
    fun `toChangelogEntry with scope`() {
        val commit = ParsedCommit(
            type = CommitType.FIX,
            scope = "ui",
            subject = "修复按钮"
        )
        
        val entry = commit.toChangelogEntry()
        assertTrue(entry.contains("(ui)"))
    }
    
    @Test
    fun `toChangelogEntry with breaking mark`() {
        val commit = ParsedCommit(
            type = CommitType.BREAKING_CHANGE,
            subject = "破坏性变更",
            isBreaking = true
        )
        
        val entry = commit.toChangelogEntry()
        assertTrue(entry.contains("⚠️"))
    }
}
