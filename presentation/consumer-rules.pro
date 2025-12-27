# Consumer rules for presentation module
# These rules are applied to consumers of this library

# Keep all theme classes
-keep class com.empathy.ai.presentation.theme.** { *; }
-keep class com.empathy.ai.presentation.theme.ThemeKt { *; }
-keep class com.empathy.ai.presentation.theme.AdaptiveDimensionsKt { *; }
-keep class com.empathy.ai.presentation.theme.SemanticColorsKt { *; }

# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }
