package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * EmotionType 枚举测试
 *
 * 测试内容：
 * - 枚举值完整性
 * - Emoji映射
 * - 显示名称
 * - fromText方法
 */
class EmotionTypeTest {
    
    @Test
    fun `all emotion types should have emoji`() {
        EmotionType.entries.forEach { type ->
            assertNotNull("${type.name} should have emoji", type.emoji)
            assert(type.emoji.isNotEmpty()) { "${type.name} emoji should not be empty" }
        }
    }
    
    @Test
    fun `all emotion types should have display name`() {
        EmotionType.entries.forEach { type ->
            assertNotNull("${type.name} should have displayName", type.displayName)
            assert(type.displayName.isNotEmpty()) { "${type.name} displayName should not be empty" }
        }
    }
    
    @Test
    fun `SWEET should have correct emoji and name`() {
        assertEquals("🥰", EmotionType.SWEET.emoji)
        assertEquals("甜蜜", EmotionType.SWEET.displayName)
    }
    
    @Test
    fun `CONFLICT should have correct emoji and name`() {
        assertEquals("😤", EmotionType.CONFLICT.emoji)
        assertEquals("冲突", EmotionType.CONFLICT.displayName)
    }
    
    @Test
    fun `GIFT should have correct emoji and name`() {
        assertEquals("🎁", EmotionType.GIFT.emoji)
        assertEquals("礼物", EmotionType.GIFT.displayName)
    }
    
    @Test
    fun `DATE should have correct emoji and name`() {
        assertEquals("💑", EmotionType.DATE.emoji)
        assertEquals("约会", EmotionType.DATE.displayName)
    }
    
    @Test
    fun `DEEP_TALK should have correct emoji and name`() {
        assertEquals("💭", EmotionType.DEEP_TALK.emoji)
        assertEquals("深谈", EmotionType.DEEP_TALK.displayName)
    }
    
    @Test
    fun `NEUTRAL should have correct emoji and name`() {
        assertEquals("😐", EmotionType.NEUTRAL.emoji)
        assertEquals("平淡", EmotionType.NEUTRAL.displayName)
    }
    
    @Test
    fun `fromText should detect sweet keywords`() {
        assertEquals(EmotionType.SWEET, EmotionType.fromText("我爱你"))
        assertEquals(EmotionType.SWEET, EmotionType.fromText("今天很开心"))
        assertEquals(EmotionType.SWEET, EmotionType.fromText("好幸福啊"))
        assertEquals(EmotionType.SWEET, EmotionType.fromText("喜欢和你在一起"))
    }
    
    @Test
    fun `fromText should detect conflict keywords`() {
        assertEquals(EmotionType.CONFLICT, EmotionType.fromText("我们吵架了"))
        assertEquals(EmotionType.CONFLICT, EmotionType.fromText("有点生气"))
        assertEquals(EmotionType.CONFLICT, EmotionType.fromText("发生了矛盾"))
        assertEquals(EmotionType.CONFLICT, EmotionType.fromText("不开心"))
    }
    
    @Test
    fun `fromText should detect gift keywords`() {
        assertEquals(EmotionType.GIFT, EmotionType.fromText("送了礼物"))
        assertEquals(EmotionType.GIFT, EmotionType.fromText("收到惊喜"))
        assertEquals(EmotionType.GIFT, EmotionType.fromText("生日快乐"))
        assertEquals(EmotionType.GIFT, EmotionType.fromText("纪念日"))
    }
    
    @Test
    fun `fromText should detect date keywords`() {
        assertEquals(EmotionType.DATE, EmotionType.fromText("一起约会"))
        assertEquals(EmotionType.DATE, EmotionType.fromText("去看电影"))
        assertEquals(EmotionType.DATE, EmotionType.fromText("出去吃饭"))
        assertEquals(EmotionType.DATE, EmotionType.fromText("一起旅行"))
    }
    
    @Test
    fun `fromText should detect deep talk keywords`() {
        assertEquals(EmotionType.DEEP_TALK, EmotionType.fromText("聊了很久"))
        assertEquals(EmotionType.DEEP_TALK, EmotionType.fromText("谈心"))
        assertEquals(EmotionType.DEEP_TALK, EmotionType.fromText("讨论未来"))
        assertEquals(EmotionType.DEEP_TALK, EmotionType.fromText("深入交流"))
    }
    
    @Test
    fun `fromText should return NEUTRAL for unknown text`() {
        assertEquals(EmotionType.NEUTRAL, EmotionType.fromText("今天天气不错"))
        assertEquals(EmotionType.NEUTRAL, EmotionType.fromText(""))
        assertEquals(EmotionType.NEUTRAL, EmotionType.fromText("普通的一天"))
    }
    
    @Test
    fun `emotion types count should be 6`() {
        assertEquals(6, EmotionType.entries.size)
    }
}
