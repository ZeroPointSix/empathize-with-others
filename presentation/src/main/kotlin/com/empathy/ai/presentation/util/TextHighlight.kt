package com.empathy.ai.presentation.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

fun buildHighlightedText(
    text: String,
    query: String,
    highlightStyle: SpanStyle
): AnnotatedString {
    if (query.isBlank() || text.isEmpty()) {
        return AnnotatedString(text)
    }

    var currentIndex = 0

    return buildAnnotatedString {
        while (currentIndex < text.length) {
            val matchIndex = text.indexOf(query, currentIndex, ignoreCase = true)
            if (matchIndex == -1) {
                append(text.substring(currentIndex))
                break
            }

            if (matchIndex > currentIndex) {
                append(text.substring(currentIndex, matchIndex))
            }

            withStyle(highlightStyle) {
                append(text.substring(matchIndex, matchIndex + query.length))
            }

            currentIndex = matchIndex + query.length
        }
    }
}

fun searchHighlightAlpha(isDarkTheme: Boolean): Float {
    return if (isDarkTheme) 0.35f else 0.2f
}

fun createSearchHighlightStyle(
    isDarkTheme: Boolean,
    baseColor: Color
): SpanStyle {
    return SpanStyle(
        fontWeight = FontWeight.SemiBold,
        background = baseColor.copy(alpha = searchHighlightAlpha(isDarkTheme))
    )
}
