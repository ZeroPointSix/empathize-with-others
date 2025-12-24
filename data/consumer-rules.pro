# Data模块的ProGuard规则
# 这些规则会被包含到使用此模块的应用中

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Moshi
-keep class com.empathy.ai.data.remote.model.** { *; }
-keepclassmembers class com.empathy.ai.data.remote.model.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
