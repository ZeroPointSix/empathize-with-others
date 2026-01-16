package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.ActionType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * IdentityPrefixHelper 单元测试
 *
 * 覆盖身份前缀的添加、解析与清理逻辑。
 */
class IdentityPrefixHelperTest {

    @Test
    fun `addPrefix应根据动作类型添加正确前缀并去重`() {
        // Given
        val raw = "你好"

        // When
        val analyze = IdentityPrefixHelper.addPrefix(raw, ActionType.ANALYZE)
        assertEquals("${IdentityPrefixHelper.PREFIX_CONTACT}你好", analyze)

        val check = IdentityPrefixHelper.addPrefix(raw, ActionType.CHECK)
        assertEquals("${IdentityPrefixHelper.PREFIX_USER}你好", check)

        val swapped = IdentityPrefixHelper.addPrefix(
            "${IdentityPrefixHelper.PREFIX_USER}你好",
            ActionType.ANALYZE
        )

        // Then
        assertEquals("${IdentityPrefixHelper.PREFIX_CONTACT}你好", swapped)
    }

    @Test
    fun `parse应正确识别身份并返回纯文本`() {
        // Given
        val contact = IdentityPrefixHelper.parse("${IdentityPrefixHelper.PREFIX_CONTACT}早安")

        // When
        assertEquals(IdentityPrefixHelper.IdentityRole.CONTACT, contact.role)
        assertEquals("早安", contact.content)

        val user = IdentityPrefixHelper.parse("${IdentityPrefixHelper.PREFIX_USER}我马上到")
        assertEquals(IdentityPrefixHelper.IdentityRole.USER, user.role)
        assertEquals("我马上到", user.content)

        val legacy = IdentityPrefixHelper.parse("这是一条旧数据")

        // Then
        assertEquals(IdentityPrefixHelper.IdentityRole.LEGACY, legacy.role)
        assertEquals("这是一条旧数据", legacy.content)
    }

    @Test
    fun `stripAllPrefixes应移除多重前缀`() {
        // Given
        val content = "${IdentityPrefixHelper.PREFIX_CONTACT}${IdentityPrefixHelper.PREFIX_USER}你好"

        // When
        val stripped = IdentityPrefixHelper.stripAllPrefixes(content)

        // Then
        assertEquals("你好", stripped)
    }
}
