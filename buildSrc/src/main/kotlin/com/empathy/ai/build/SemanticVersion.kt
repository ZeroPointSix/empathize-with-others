package com.empathy.ai.build

/**
 * 语义化版本数据类
 * 遵循 Semantic Versioning 2.0.0 规范
 *
 * @property major 主版本号 - 不兼容的API修改
 * @property minor 次版本号 - 向下兼容的功能性新增
 * @property patch 修订号 - 向下兼容的问题修正
 * @property prerelease 预发布标识 - dev/alpha/beta/rc
 * @property build 构建元数据 - 时间戳或Git SHA
 * 
 * @see TDD-00024 4.1.1 SemanticVersion数据类
 */
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val prerelease: String? = null,
    val build: String? = null
) : Comparable<SemanticVersion> {
    
    init {
        require(major >= 0) { "Major version must be non-negative: $major" }
        require(minor >= 0) { "Minor version must be non-negative: $minor" }
        require(patch >= 0) { "Patch version must be non-negative: $patch" }
    }
    
    /**
     * 递增主版本号，重置次版本号和修订号
     */
    fun bumpMajor(): SemanticVersion = copy(
        major = major + 1, 
        minor = 0, 
        patch = 0,
        prerelease = null,
        build = null
    )
    
    /**
     * 递增次版本号，重置修订号
     */
    fun bumpMinor(): SemanticVersion = copy(
        minor = minor + 1, 
        patch = 0,
        prerelease = null,
        build = null
    )
    
    /**
     * 递增修订号
     */
    fun bumpPatch(): SemanticVersion = copy(
        patch = patch + 1,
        prerelease = null,
        build = null
    )
    
    /**
     * 设置预发布标识
     */
    fun withPrerelease(prerelease: String?): SemanticVersion = copy(prerelease = prerelease)
    
    /**
     * 设置构建元数据
     */
    fun withBuild(build: String?): SemanticVersion = copy(build = build)
    
    /**
     * 转换为字符串格式
     * 格式: major.minor.patch[-prerelease][+build]
     */
    override fun toString(): String {
        val base = "$major.$minor.$patch"
        val pre = prerelease?.let { "-$it" } ?: ""
        val bld = build?.let { "+$it" } ?: ""
        return "$base$pre$bld"
    }
    
    /**
     * 转换为简短字符串（不含预发布和构建信息）
     */
    fun toShortString(): String = "$major.$minor.$patch"
    
    /**
     * 计算versionCode
     * 公式: major*10000 + minor*100 + patch
     * 
     * 支持范围: 0.0.0 ~ 214.74.83 (Int.MAX_VALUE = 2147483647)
     */
    fun toVersionCode(): Int {
        val code = major * 10000 + minor * 100 + patch
        require(code >= 0) { "Version code overflow: $this -> $code" }
        return code
    }
    
    /**
     * 比较版本号大小
     * 按照语义化版本规范：major > minor > patch > prerelease
     */
    override fun compareTo(other: SemanticVersion): Int {
        // 比较主版本号
        if (major != other.major) return major.compareTo(other.major)
        // 比较次版本号
        if (minor != other.minor) return minor.compareTo(other.minor)
        // 比较修订号
        if (patch != other.patch) return patch.compareTo(other.patch)
        // 比较预发布标识（有预发布标识的版本低于无预发布标识的版本）
        return when {
            prerelease == null && other.prerelease == null -> 0
            prerelease == null -> 1  // 无预发布 > 有预发布
            other.prerelease == null -> -1
            else -> prerelease.compareTo(other.prerelease)
        }
    }
    
    companion object {
        /**
         * 默认初始版本
         */
        val DEFAULT = SemanticVersion(1, 0, 0)
        
        /**
         * 版本号正则表达式
         * 支持格式: 1.2.3, 1.2.3-beta.1, 1.2.3+20251231, 1.2.3-beta.1+20251231
         */
        private val VERSION_REGEX = """^(\d+)\.(\d+)\.(\d+)(?:-([a-zA-Z0-9.-]+))?(?:\+([a-zA-Z0-9.-]+))?$""".toRegex()
        
        /**
         * 从字符串解析版本号
         * @param version 版本字符串，如 "1.2.3-beta.1+20251231"
         * @return SemanticVersion实例
         * @throws IllegalArgumentException 版本格式无效时抛出
         */
        fun parse(version: String): SemanticVersion {
            val trimmed = version.trim()
            val match = VERSION_REGEX.matchEntire(trimmed)
                ?: throw IllegalArgumentException("Invalid version format: '$version'. Expected format: major.minor.patch[-prerelease][+build]")
            
            return SemanticVersion(
                major = match.groupValues[1].toIntOrNull() 
                    ?: throw IllegalArgumentException("Invalid major version: ${match.groupValues[1]}"),
                minor = match.groupValues[2].toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid minor version: ${match.groupValues[2]}"),
                patch = match.groupValues[3].toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid patch version: ${match.groupValues[3]}"),
                prerelease = match.groupValues[4].takeIf { it.isNotEmpty() },
                build = match.groupValues[5].takeIf { it.isNotEmpty() }
            )
        }
        
        /**
         * 安全解析版本号，失败时返回null
         */
        fun parseOrNull(version: String): SemanticVersion? {
            return try {
                parse(version)
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * 从versionCode反向计算版本号
         * 注意：无法恢复预发布和构建信息
         */
        fun fromVersionCode(code: Int): SemanticVersion {
            require(code >= 0) { "Version code must be non-negative: $code" }
            val major = code / 10000
            val minor = (code % 10000) / 100
            val patch = code % 100
            return SemanticVersion(major, minor, patch)
        }
    }
}
