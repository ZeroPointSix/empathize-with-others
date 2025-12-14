# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ==================== 记忆系统 ProGuard 规则 ====================

# 保留领域模型（用于JSON序列化/反序列化）
-keep class com.empathy.ai.domain.model.Fact { *; }
-keep class com.empathy.ai.domain.model.FactSource { *; }
-keep class com.empathy.ai.domain.model.ConversationLog { *; }
-keep class com.empathy.ai.domain.model.DailySummary { *; }
-keep class com.empathy.ai.domain.model.KeyEvent { *; }
-keep class com.empathy.ai.domain.model.TagUpdate { *; }
-keep class com.empathy.ai.domain.model.RelationshipTrend { *; }
-keep class com.empathy.ai.domain.model.RelationshipLevel { *; }

# 保留数据库实体（Room需要）
-keep class com.empathy.ai.data.local.entity.ConversationLogEntity { *; }
-keep class com.empathy.ai.data.local.entity.DailySummaryEntity { *; }
-keep class com.empathy.ai.data.local.entity.FailedSummaryTaskEntity { *; }

# 保留AI响应模型（Moshi JSON解析需要）
-keep class com.empathy.ai.data.remote.model.AiSummaryResponse { *; }
-keepclassmembers class com.empathy.ai.data.remote.model.AiSummaryResponse { *; }

# 保留类型转换器（Room需要）
-keep class com.empathy.ai.data.local.converter.FactListConverter { *; }

# 保留枚举值（防止被混淆）
-keepclassmembers enum com.empathy.ai.domain.model.FactSource { *; }
-keepclassmembers enum com.empathy.ai.domain.model.RelationshipTrend { *; }
-keepclassmembers enum com.empathy.ai.domain.model.RelationshipLevel { *; }

# Moshi 规则
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class **JsonAdapter {
    <init>(...);
    <fields>;
}
-keepnames @com.squareup.moshi.JsonClass class *

# Room 规则
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**