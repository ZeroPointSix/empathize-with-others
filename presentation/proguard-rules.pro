# Add project specific ProGuard rules here.
# Keep all Compose-related classes
-keep class com.empathy.ai.presentation.theme.** { *; }
-keep class androidx.compose.** { *; }

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
