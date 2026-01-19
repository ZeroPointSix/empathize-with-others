package com.empathy.ai.domain.model

import com.empathy.ai.domain.util.MemoryConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fact 领域模型单元测试
 *
 * 业务背景 (PRD-00003):
 *   事实记录是联系人画像的核心组成部分，用于存储关于联系人的结构化信息
 *   如职业、爱好、生日、重要纪念日等关键信息
 *
 * 设计决策:
 *   - 使用UUID作为唯一标识符，支持离线创建和冲突避免
 *   - 提供工厂方法创建Fact，自动生成ID
 *   - 支持过期机制（90天）和新鲜度判断（7天），用于AI分析时的数据筛选
 *
 * 数据来源:
 *   - MANUAL: 用户手动添加的事实（如"职业: 产品经理"）
 *   - AI_INFERRED: AI从对话中推断的事实（如从对话内容推断"喜欢旅行"）
 *
 * 任务追踪: FD-00003/核心功能实现
 */
class FactTest {

    @Test
    fun `创建Fact成功`() {
        val fact = Fact(
            key = "职业",
            value = "产品经理",
            timestamp = System.currentTimeMillis(),
            source = FactSource.MANUAL
        )

        assertEquals("职业", fact.key)
        assertEquals("产品经理", fact.value)
        assertEquals(FactSource.MANUAL, fact.source)
        assertTrue(fact.timestamp > 0)
        assertTrue(fact.id.isNotBlank())
    }

    @Test
    fun `Fact应该有唯一的默认id`() {
        // Given & When: 创建两个相同内容的Fact
        val fact1 = Fact(key = "兴趣", value = "音乐", timestamp = 1L, source = FactSource.MANUAL)
        val fact2 = Fact(key = "兴趣", value = "音乐", timestamp = 1L, source = FactSource.MANUAL)

        // Then: id应该不同
        assertNotEquals(fact1.id, fact2.id)
    }

    @Test
    fun `Fact可以指定自定义id`() {
        // Given & When
        val customId = "custom-id-123"
        val fact = Fact(
            id = customId,
            key = "职业",
            value = "产品经理",
            timestamp = System.currentTimeMillis(),
            source = FactSource.MANUAL
        )

        // Then
        assertEquals(customId, fact.id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `创建Fact时id为空抛出异常`() {
        Fact(
            id = "",
            key = "职业",
            value = "产品经理",
            timestamp = System.currentTimeMillis(),
            source = FactSource.MANUAL
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `创建Fact时id为空白抛出异常`() {
        Fact(
            id = "   ",
            key = "职业",
            value = "产品经理",
            timestamp = System.currentTimeMillis(),
            source = FactSource.MANUAL
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `创建Fact时key为空抛出异常`() {
        Fact(
            key = "",
            value = "产品经理",
            timestamp = System.currentTimeMillis(),
            source = FactSource.MANUAL
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `创建Fact时key为空白抛出异常`() {
        Fact(
            key = "   ",
            value = "产品经理",
            timestamp = System.currentTimeMillis(),
            source = FactSource.MANUAL
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `创建Fact时value为空抛出异常`() {
        Fact(
            key = "职业",
            value = "",
            timestamp = System.currentTimeMillis(),
            source = FactSource.MANUAL
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `创建Fact时timestamp为0抛出异常`() {
        Fact(
            key = "职业",
            value = "产品经理",
            timestamp = 0,
            source = FactSource.MANUAL
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `创建Fact时timestamp为负数抛出异常`() {
        Fact(
            key = "职业",
            value = "产品经理",
            timestamp = -1,
            source = FactSource.MANUAL
        )
    }

    @Test
    fun `isExpired返回true当超过90天`() {
        val now = System.currentTimeMillis()
        val expiredTimestamp = now - (MemoryConstants.EXPIRY_DAYS + 1) * MemoryConstants.ONE_DAY_MILLIS

        val fact = Fact(
            key = "职业",
            value = "产品经理",
            timestamp = expiredTimestamp,
            source = FactSource.MANUAL
        )

        assertTrue(fact.isExpired(now))
    }

    @Test
    fun `isExpired返回false当未超过90天`() {
        val now = System.currentTimeMillis()
        val recentTimestamp = now - (MemoryConstants.EXPIRY_DAYS - 1) * MemoryConstants.ONE_DAY_MILLIS

        val fact = Fact(
            key = "职业",
            value = "产品经理",
            timestamp = recentTimestamp,
            source = FactSource.MANUAL
        )

        assertFalse(fact.isExpired(now))
    }

    @Test
    fun `isRecent返回true当在7天内`() {
        val now = System.currentTimeMillis()
        val recentTimestamp = now - (MemoryConstants.RECENT_DAYS - 1) * MemoryConstants.ONE_DAY_MILLIS

        val fact = Fact(
            key = "职业",
            value = "产品经理",
            timestamp = recentTimestamp,
            source = FactSource.MANUAL
        )

        assertTrue(fact.isRecent(now))
    }

    @Test
    fun `isRecent返回false当超过7天`() {
        val now = System.currentTimeMillis()
        val oldTimestamp = now - (MemoryConstants.RECENT_DAYS + 1) * MemoryConstants.ONE_DAY_MILLIS

        val fact = Fact(
            key = "职业",
            value = "产品经理",
            timestamp = oldTimestamp,
            source = FactSource.MANUAL
        )

        assertFalse(fact.isRecent(now))
    }

    @Test
    fun `formatDate返回正确格式的日期字符串`() {
        val fact = Fact(
            key = "职业",
            value = "产品经理",
            timestamp = System.currentTimeMillis(),
            source = FactSource.MANUAL
        )

        val dateString = fact.formatDate()
        assertNotNull(dateString)
        assertTrue(dateString.isNotBlank())
        // 验证日期格式 yyyy-MM-dd
        assertTrue(dateString.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun `FactSource枚举值正确`() {
        assertEquals(2, FactSource.values().size)
        assertNotNull(FactSource.MANUAL)
        assertNotNull(FactSource.AI_INFERRED)
    }

    @Test
    fun `Fact的copy方法正确工作`() {
        val original = Fact(
            key = "职业",
            value = "产品经理",
            timestamp = System.currentTimeMillis(),
            source = FactSource.MANUAL
        )

        val copied = original.copy(value = "工程师")

        assertEquals("职业", copied.key)
        assertEquals("工程师", copied.value)
        assertEquals(original.timestamp, copied.timestamp)
        assertEquals(original.source, copied.source)
    }

    @Test
    fun `copyWithEdit应更新编辑追踪字段`() {
        val original = Fact(
            id = "fact-1",
            key = "职业",
            value = "产品经理",
            timestamp = 1_000L,
            source = FactSource.MANUAL
        )

        val edited = original.copyWithEdit("工作", "高级产品经理")

        assertEquals("工作", edited.key)
        assertEquals("高级产品经理", edited.value)
        assertTrue(edited.isUserModified)
        assertEquals("职业", edited.originalKey)
        assertEquals("产品经理", edited.originalValue)
        assertTrue(edited.lastModifiedTime >= original.timestamp)
    }

    @Test
    fun `hasChanges应正确判断是否有变化`() {
        val fact = Fact(
            id = "fact-1",
            key = "职业",
            value = "产品经理",
            timestamp = 1_000L,
            source = FactSource.MANUAL
        )

        assertFalse(fact.hasChanges("职业", "产品经理"))
        assertTrue(fact.hasChanges("工作", "产品经理"))
        assertTrue(fact.hasChanges("职业", "高级产品经理"))
    }

    @Test
    fun `Fact的equals方法正确工作`() {
        val timestamp = System.currentTimeMillis()
        val id = "same-id"
        val fact1 = Fact(id, "职业", "产品经理", timestamp, FactSource.MANUAL)
        val fact2 = Fact(id, "职业", "产品经理", timestamp, FactSource.MANUAL)

        assertEquals(fact1, fact2)
    }

    @Test
    fun `不同id的Fact不相等`() {
        val timestamp = System.currentTimeMillis()
        val fact1 = Fact(key = "职业", value = "产品经理", timestamp = timestamp, source = FactSource.MANUAL)
        val fact2 = Fact(key = "职业", value = "产品经理", timestamp = timestamp, source = FactSource.MANUAL)

        // 即使内容相同，id不同也不相等
        assertNotEquals(fact1, fact2)
    }

    @Test
    fun `不同内容的Fact不相等`() {
        val timestamp = System.currentTimeMillis()
        val id = "same-id"
        val fact1 = Fact(id, "职业", "产品经理", timestamp, FactSource.MANUAL)
        val fact2 = Fact(id, "职业", "工程师", timestamp, FactSource.MANUAL)

        assertFalse(fact1 == fact2)
    }
}
