package com.empathy.ai.presentation.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextHighlightTest {

    private val highlightStyle = SpanStyle(
        fontWeight = FontWeight.SemiBold,
        background = Color(0xFF00FF00)
    )

    @Test
    fun buildHighlightedText_blankQuery_returnsPlainText() {
        val result = buildHighlightedText(
            text = "hello",
            query = "",
            highlightStyle = highlightStyle
        )

        assertEquals("hello", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun buildHighlightedText_caseInsensitive_matchesAndHighlights() {
        val result = buildHighlightedText(
            text = "张三",
            query = "张",
            highlightStyle = highlightStyle
        )

        assertEquals("张三", result.text)
        assertEquals(1, result.spanStyles.size)
        val range = result.spanStyles.first()
        assertEquals(0, range.start)
        assertEquals(1, range.end)
        assertEquals(highlightStyle, range.item)
    }

    @Test
    fun buildHighlightedText_caseInsensitiveAscii_matchesAndHighlights() {
        val result = buildHighlightedText(
            text = "Alice",
            query = "aL",
            highlightStyle = highlightStyle
        )

        assertEquals("Alice", result.text)
        assertEquals(1, result.spanStyles.size)
        val range = result.spanStyles.first()
        assertEquals(0, range.start)
        assertEquals(2, range.end)
        assertEquals(highlightStyle, range.item)
    }

    @Test
    fun buildHighlightedText_noMatch_returnsPlainText() {
        val result = buildHighlightedText(
            text = "hello",
            query = "xyz",
            highlightStyle = highlightStyle
        )

        assertEquals("hello", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun buildHighlightedText_multipleMatches_returnsAllRanges() {
        val result = buildHighlightedText(
            text = "bananana",
            query = "na",
            highlightStyle = highlightStyle
        )

        val ranges = result.spanStyles.map { it.start to it.end }
        assertEquals(listOf(2 to 4, 4 to 6, 6 to 8), ranges)
    }

    @Test
    fun searchHighlightAlpha_returnsExpectedValues() {
        assertEquals(0.35f, searchHighlightAlpha(true), 0.0001f)
        assertEquals(0.2f, searchHighlightAlpha(false), 0.0001f)
    }

    @Test
    fun createSearchHighlightStyle_usesBaseColorWithAlpha() {
        val baseColor = Color(0xFF112233)
        val style = createSearchHighlightStyle(isDarkTheme = true, baseColor = baseColor)

        assertEquals(FontWeight.SemiBold, style.fontWeight)
        assertEquals(baseColor.copy(alpha = 0.35f), style.background)
    }
}
