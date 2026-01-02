package com.empathy.ai.build

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * SemanticVersion 单元测试
 * 
 * @see TDD-00024 6.1 单元测试
 */
class SemanticVersionTest {
    
    // ========== 解析测试 ==========
    
    @Test
    fun `parse valid version string`() {
        val version = SemanticVersion.parse("1.2.3")
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
        assertNull(version.prerelease)
        assertNull(version.build)
    }
    
    @Test
    fun `parse version with prerelease`() {
        val version = SemanticVersion.parse("1.2.3-beta.1")
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
        assertEquals("beta.1", version.prerelease)
        assertNull(version.build)
    }
    
    @Test
    fun `parse version with build metadata`() {
        val version = SemanticVersion.parse("1.2.3+20251231")
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
        assertNull(version.prerelease)
        assertEquals("20251231", version.build)
    }
    
    @Test
    fun `parse version with prerelease and build`() {
        val version = SemanticVersion.parse("1.2.3-beta.1+20251231")
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
        assertEquals("beta.1", version.prerelease)
        assertEquals("20251231", version.build)
    }
    
    @Test
    fun `parse version with whitespace`() {
        val version = SemanticVersion.parse("  1.2.3  ")
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
    }
    
    @Test
    fun `parse zero version`() {
        val version = SemanticVersion.parse("0.0.0")
        assertEquals(0, version.major)
        assertEquals(0, version.minor)
        assertEquals(0, version.patch)
    }
    
    @Test
    fun `parse large version numbers`() {
        val version = SemanticVersion.parse("100.200.300")
        assertEquals(100, version.major)
        assertEquals(200, version.minor)
        assertEquals(300, version.patch)
    }
    
    @Test
    fun `parse invalid version throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.parse("invalid")
        }
    }
    
    @Test
    fun `parse incomplete version throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.parse("1.2")
        }
    }
    
    @Test
    fun `parse negative version throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.parse("-1.2.3")
        }
    }
    
    @Test
    fun `parseOrNull returns null for invalid version`() {
        assertNull(SemanticVersion.parseOrNull("invalid"))
    }
    
    @Test
    fun `parseOrNull returns version for valid input`() {
        val version = SemanticVersion.parseOrNull("1.2.3")
        assertNotNull(version)
        assertEquals(1, version.major)
    }
    
    // ========== 版本递增测试 ==========
    
    @Test
    fun `bump major version`() {
        val version = SemanticVersion(1, 2, 3)
        val bumped = version.bumpMajor()
        assertEquals(SemanticVersion(2, 0, 0), bumped)
    }
    
    @Test
    fun `bump major clears prerelease and build`() {
        val version = SemanticVersion(1, 2, 3, "beta", "123")
        val bumped = version.bumpMajor()
        assertNull(bumped.prerelease)
        assertNull(bumped.build)
    }
    
    @Test
    fun `bump minor version`() {
        val version = SemanticVersion(1, 2, 3)
        val bumped = version.bumpMinor()
        assertEquals(SemanticVersion(1, 3, 0), bumped)
    }
    
    @Test
    fun `bump minor clears prerelease and build`() {
        val version = SemanticVersion(1, 2, 3, "beta", "123")
        val bumped = version.bumpMinor()
        assertNull(bumped.prerelease)
        assertNull(bumped.build)
    }
    
    @Test
    fun `bump patch version`() {
        val version = SemanticVersion(1, 2, 3)
        val bumped = version.bumpPatch()
        assertEquals(SemanticVersion(1, 2, 4), bumped)
    }
    
    @Test
    fun `bump patch clears prerelease and build`() {
        val version = SemanticVersion(1, 2, 3, "beta", "123")
        val bumped = version.bumpPatch()
        assertNull(bumped.prerelease)
        assertNull(bumped.build)
    }
    
    // ========== versionCode 测试 ==========
    
    @Test
    fun `toVersionCode calculates correctly`() {
        val version = SemanticVersion(1, 2, 3)
        assertEquals(10203, version.toVersionCode())
    }
    
    @Test
    fun `toVersionCode for zero version`() {
        val version = SemanticVersion(0, 0, 0)
        assertEquals(0, version.toVersionCode())
    }
    
    @Test
    fun `toVersionCode for large version`() {
        val version = SemanticVersion(10, 20, 30)
        assertEquals(102030, version.toVersionCode())
    }
    
    @Test
    fun `fromVersionCode reverses correctly`() {
        val original = SemanticVersion(1, 2, 3)
        val code = original.toVersionCode()
        val restored = SemanticVersion.fromVersionCode(code)
        assertEquals(original.major, restored.major)
        assertEquals(original.minor, restored.minor)
        assertEquals(original.patch, restored.patch)
    }
    
    // ========== 字符串转换测试 ==========
    
    @Test
    fun `toString formats correctly`() {
        val version = SemanticVersion(1, 2, 3)
        assertEquals("1.2.3", version.toString())
    }
    
    @Test
    fun `toString with prerelease`() {
        val version = SemanticVersion(1, 2, 3, prerelease = "beta.1")
        assertEquals("1.2.3-beta.1", version.toString())
    }
    
    @Test
    fun `toString with build`() {
        val version = SemanticVersion(1, 2, 3, build = "20251231")
        assertEquals("1.2.3+20251231", version.toString())
    }
    
    @Test
    fun `toString with prerelease and build`() {
        val version = SemanticVersion(1, 2, 3, "beta.1", "20251231")
        assertEquals("1.2.3-beta.1+20251231", version.toString())
    }
    
    @Test
    fun `toShortString ignores prerelease and build`() {
        val version = SemanticVersion(1, 2, 3, "beta.1", "20251231")
        assertEquals("1.2.3", version.toShortString())
    }
    
    // ========== 比较测试 ==========
    
    @Test
    fun `compare major versions`() {
        val v1 = SemanticVersion(1, 0, 0)
        val v2 = SemanticVersion(2, 0, 0)
        assertTrue(v1 < v2)
    }
    
    @Test
    fun `compare minor versions`() {
        val v1 = SemanticVersion(1, 1, 0)
        val v2 = SemanticVersion(1, 2, 0)
        assertTrue(v1 < v2)
    }
    
    @Test
    fun `compare patch versions`() {
        val v1 = SemanticVersion(1, 1, 1)
        val v2 = SemanticVersion(1, 1, 2)
        assertTrue(v1 < v2)
    }
    
    @Test
    fun `prerelease version is less than release`() {
        val prerelease = SemanticVersion(1, 0, 0, prerelease = "beta")
        val release = SemanticVersion(1, 0, 0)
        assertTrue(prerelease < release)
    }
    
    @Test
    fun `equal versions compare as equal`() {
        val v1 = SemanticVersion(1, 2, 3)
        val v2 = SemanticVersion(1, 2, 3)
        assertEquals(0, v1.compareTo(v2))
    }
    
    // ========== 边界测试 ==========
    
    @Test
    fun `constructor rejects negative major`() {
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion(-1, 0, 0)
        }
    }
    
    @Test
    fun `constructor rejects negative minor`() {
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion(0, -1, 0)
        }
    }
    
    @Test
    fun `constructor rejects negative patch`() {
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion(0, 0, -1)
        }
    }
    
    @Test
    fun `DEFAULT version is 1_0_0`() {
        assertEquals(SemanticVersion(1, 0, 0), SemanticVersion.DEFAULT)
    }
    
    // ========== 辅助方法测试 ==========
    
    @Test
    fun `withPrerelease sets prerelease`() {
        val version = SemanticVersion(1, 0, 0)
        val withPre = version.withPrerelease("beta")
        assertEquals("beta", withPre.prerelease)
    }
    
    @Test
    fun `withBuild sets build`() {
        val version = SemanticVersion(1, 0, 0)
        val withBuild = version.withBuild("123")
        assertEquals("123", withBuild.build)
    }
}
