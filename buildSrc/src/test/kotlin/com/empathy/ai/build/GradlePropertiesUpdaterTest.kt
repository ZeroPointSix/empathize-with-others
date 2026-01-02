package com.empathy.ai.build

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * GradlePropertiesUpdater 单元测试
 * 
 * @see TDD-00024 6.1 单元测试
 */
class GradlePropertiesUpdaterTest {
    
    private lateinit var tempDir: File
    private lateinit var updater: GradlePropertiesUpdater
    
    @Before
    fun setUp() {
        tempDir = createTempDir("gradle-properties-updater-test")
        updater = GradlePropertiesUpdater()
    }
    
    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }
    
    // ========== supports 测试 ==========
    
    @Test
    fun `supports returns true for gradle_properties file`() {
        val file = File(tempDir, "gradle.properties")
        file.writeText("key=value")
        
        assertTrue(updater.supports(file))
    }
    
    @Test
    fun `supports returns false for non-existent file`() {
        val file = File(tempDir, "gradle.properties")
        
        assertFalse(updater.supports(file))
    }
    
    @Test
    fun `supports returns false for other files`() {
        val file = File(tempDir, "build.gradle.kts")
        file.writeText("plugins {}")
        
        assertFalse(updater.supports(file))
    }
    
    // ========== updateVersion 测试 ==========
    
    @Test
    fun `updateVersion updates APP_VERSION_NAME`() {
        val file = createPropertiesFile("APP_VERSION_NAME=1.0.0")
        
        val result = updater.updateVersion(file, SemanticVersion(2, 0, 0))
        
        assertTrue(result)
        assertTrue(file.readText().contains("APP_VERSION_NAME=2.0.0"))
    }
    
    @Test
    fun `updateVersion updates APP_VERSION_CODE`() {
        val file = createPropertiesFile("""
            APP_VERSION_NAME=1.0.0
            APP_VERSION_CODE=10000
        """.trimIndent())
        
        val result = updater.updateVersion(file, SemanticVersion(2, 1, 0))
        
        assertTrue(result)
        assertTrue(file.readText().contains("APP_VERSION_CODE=20100"))
    }
    
    @Test
    fun `updateVersion updates VERSION key`() {
        val file = createPropertiesFile("VERSION=1.0.0")
        
        val result = updater.updateVersion(file, SemanticVersion(1, 1, 0))
        
        assertTrue(result)
        assertTrue(file.readText().contains("VERSION=1.1.0"))
    }
    
    @Test
    fun `updateVersion updates LIB_VERSION key`() {
        val file = createPropertiesFile("LIB_VERSION=1.0.0")
        
        val result = updater.updateVersion(file, SemanticVersion(1, 0, 1))
        
        assertTrue(result)
        assertTrue(file.readText().contains("LIB_VERSION=1.0.1"))
    }
    
    @Test
    fun `updateVersion updates multiple version keys`() {
        val file = createPropertiesFile("""
            APP_VERSION_NAME=1.0.0
            APP_VERSION_CODE=10000
            VERSION=1.0.0
        """.trimIndent())
        
        val result = updater.updateVersion(file, SemanticVersion(2, 0, 0))
        
        assertTrue(result)
        val content = file.readText()
        assertTrue(content.contains("APP_VERSION_NAME=2.0.0"))
        assertTrue(content.contains("APP_VERSION_CODE=20000"))
        assertTrue(content.contains("VERSION=2.0.0"))
    }
    
    @Test
    fun `updateVersion returns false when no version keys found`() {
        val file = createPropertiesFile("OTHER_KEY=value")
        
        val result = updater.updateVersion(file, SemanticVersion(1, 0, 0))
        
        assertFalse(result)
    }
    
    @Test
    fun `updateVersion returns false for unsupported file`() {
        val file = File(tempDir, "other.properties")
        file.writeText("APP_VERSION_NAME=1.0.0")
        
        val result = updater.updateVersion(file, SemanticVersion(2, 0, 0))
        
        assertFalse(result)
    }
    
    @Test
    fun `updateVersion preserves other properties`() {
        val file = createPropertiesFile("""
            org.gradle.jvmargs=-Xmx2g
            APP_VERSION_NAME=1.0.0
            kotlin.code.style=official
        """.trimIndent())
        
        updater.updateVersion(file, SemanticVersion(2, 0, 0))
        
        val content = file.readText()
        assertTrue(content.contains("org.gradle.jvmargs=-Xmx2g"))
        assertTrue(content.contains("kotlin.code.style=official"))
    }
    
    // ========== readVersion 测试 ==========
    
    @Test
    fun `readVersion reads APP_VERSION_NAME`() {
        val file = createPropertiesFile("APP_VERSION_NAME=1.2.3")
        
        val version = updater.readVersion(file)
        
        assertNotNull(version)
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
    }
    
    @Test
    fun `readVersion reads VERSION when APP_VERSION_NAME missing`() {
        val file = createPropertiesFile("VERSION=2.0.0")
        
        val version = updater.readVersion(file)
        
        assertNotNull(version)
        assertEquals(2, version.major)
    }
    
    @Test
    fun `readVersion reads LIB_VERSION when others missing`() {
        val file = createPropertiesFile("LIB_VERSION=3.0.0")
        
        val version = updater.readVersion(file)
        
        assertNotNull(version)
        assertEquals(3, version.major)
    }
    
    @Test
    fun `readVersion returns null when no version keys found`() {
        val file = createPropertiesFile("OTHER_KEY=value")
        
        val version = updater.readVersion(file)
        
        assertNull(version)
    }
    
    @Test
    fun `readVersion returns null for invalid version format`() {
        val file = createPropertiesFile("APP_VERSION_NAME=invalid")
        
        val version = updater.readVersion(file)
        
        assertNull(version)
    }
    
    @Test
    fun `readVersion returns null for unsupported file`() {
        val file = File(tempDir, "other.properties")
        file.writeText("APP_VERSION_NAME=1.0.0")
        
        val version = updater.readVersion(file)
        
        assertNull(version)
    }
    
    // ========== updateVersionWithStage 测试 ==========
    
    @Test
    fun `updateVersionWithStage updates all fields`() {
        val file = createPropertiesFile("""
            APP_VERSION_NAME=1.0.0
            APP_VERSION_CODE=10000
            APP_RELEASE_STAGE=dev
        """.trimIndent())
        
        val result = updater.updateVersionWithStage(
            file, 
            SemanticVersion(2, 0, 0), 
            ReleaseStage.PRODUCTION
        )
        
        assertTrue(result)
        val content = file.readText()
        assertTrue(content.contains("APP_VERSION_NAME=2.0.0"))
        assertTrue(content.contains("APP_VERSION_CODE=20000"))
        assertTrue(content.contains("APP_RELEASE_STAGE=production"))
    }
    
    @Test
    fun `updateVersionWithStage updates stage to beta`() {
        val file = createPropertiesFile("""
            APP_VERSION_NAME=1.0.0
            APP_RELEASE_STAGE=dev
        """.trimIndent())
        
        updater.updateVersionWithStage(file, SemanticVersion(1, 0, 0), ReleaseStage.BETA)
        
        assertTrue(file.readText().contains("APP_RELEASE_STAGE=beta"))
    }
    
    @Test
    fun `updateVersionWithStage returns false for unsupported file`() {
        val file = File(tempDir, "other.properties")
        file.writeText("APP_VERSION_NAME=1.0.0")
        
        val result = updater.updateVersionWithStage(
            file, 
            SemanticVersion(2, 0, 0), 
            ReleaseStage.PRODUCTION
        )
        
        assertFalse(result)
    }
    
    // ========== readStage 测试 ==========
    
    @Test
    fun `readStage reads correct stage`() {
        val file = createPropertiesFile("APP_RELEASE_STAGE=beta")
        
        val stage = updater.readStage(file)
        
        assertEquals(ReleaseStage.BETA, stage)
    }
    
    @Test
    fun `readStage returns default when key missing`() {
        val file = createPropertiesFile("OTHER_KEY=value")
        
        val stage = updater.readStage(file)
        
        assertEquals(ReleaseStage.DEFAULT, stage)
    }
    
    @Test
    fun `readStage returns default for unsupported file`() {
        val file = File(tempDir, "other.properties")
        file.writeText("APP_RELEASE_STAGE=beta")
        
        val stage = updater.readStage(file)
        
        assertEquals(ReleaseStage.DEFAULT, stage)
    }
    
    @Test
    fun `readStage handles case insensitive stage names`() {
        val file = createPropertiesFile("APP_RELEASE_STAGE=PRODUCTION")
        
        val stage = updater.readStage(file)
        
        assertEquals(ReleaseStage.PRODUCTION, stage)
    }
    
    // ========== 边界测试 ==========
    
    @Test
    fun `updateVersion handles version with prerelease`() {
        val file = createPropertiesFile("APP_VERSION_NAME=1.0.0")
        
        val version = SemanticVersion(2, 0, 0, prerelease = "beta.1")
        updater.updateVersion(file, version)
        
        assertTrue(file.readText().contains("APP_VERSION_NAME=2.0.0-beta.1"))
    }
    
    @Test
    fun `updateVersion handles version with build metadata`() {
        val file = createPropertiesFile("APP_VERSION_NAME=1.0.0")
        
        val version = SemanticVersion(2, 0, 0, build = "20260101")
        updater.updateVersion(file, version)
        
        assertTrue(file.readText().contains("APP_VERSION_NAME=2.0.0+20260101"))
    }
    
    @Test
    fun `updateVersion handles empty file`() {
        val file = createPropertiesFile("")
        
        val result = updater.updateVersion(file, SemanticVersion(1, 0, 0))
        
        assertFalse(result)
    }
    
    @Test
    fun `updateVersion handles file with comments`() {
        val file = createPropertiesFile("""
            # This is a comment
            APP_VERSION_NAME=1.0.0
            # Another comment
        """.trimIndent())
        
        updater.updateVersion(file, SemanticVersion(2, 0, 0))
        
        val content = file.readText()
        assertTrue(content.contains("# This is a comment"))
        assertTrue(content.contains("APP_VERSION_NAME=2.0.0"))
    }
    
    // ========== 辅助方法 ==========
    
    private fun createPropertiesFile(content: String): File {
        val file = File(tempDir, "gradle.properties")
        file.writeText(content)
        return file
    }
}
